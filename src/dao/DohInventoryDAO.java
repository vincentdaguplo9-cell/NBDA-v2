package dao;

import database.DBConnection;
import model.BloodBagRecord;
import model.DashboardSummary;
import model.InventoryActionResult;
import model.IssueRequestInput;
import model.TtiScreeningInput;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Inventory read/write layer for the blood bag quarantine, release, expiry, and issuance flow.
public class DohInventoryDAO {
    private static final String[] BLOOD_TYPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    public List<BloodBagRecord> fetchInventory() {
        refreshExpiredBags();
        List<BloodBagRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, bi.donor_id, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "d.barangay, " +
                "bi.blood_type, bi.collection_date, " +
                "bi.expiry_date, bi.inventory_status, bi.tti_overall_status " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "ORDER BY bi.collection_date DESC, bi.bag_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(new BloodBagRecord(
                        rs.getString("bag_id"),
                        rs.getString("donor_id"),
                        rs.getString("donor_name"),
                        rs.getString("barangay"),
                        rs.getString("blood_type"),
                        rs.getDate("collection_date").toLocalDate(),
                        rs.getDate("expiry_date").toLocalDate(),
                        rs.getString("inventory_status"),  // param 8: inventoryStatus
                        rs.getString("tti_overall_status")   // param 9: ttiStatus
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load inventory.", e);
        }
        return records;
    }

    public DashboardSummary fetchDashboardSummary() {
        refreshExpiredBags();
        // Count TOTAL bags
        int totalBags = 0;
        String totalSql = "SELECT COUNT(*) FROM blood_inventory";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(totalSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                totalBags = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count bags.", e);
        }

        // Count EXPIRED bags
        int expiryWarnings = 0;
        String expirySql = "SELECT COUNT(*) FROM blood_inventory WHERE expiry_date < ? " +
                "AND inventory_status NOT IN ('ISSUED', 'DISCARDED', 'EXPIRED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(expirySql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    expiryWarnings = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count expired bags.", e);
        }

        // Critical stock: blood types with < 5 AVAILABLE bags
        Map<String, Integer> availableByType = new LinkedHashMap<>();
        for (String bt : BLOOD_TYPES) {
            availableByType.put(bt, 0);
        }
        List<BloodBagRecord> inventory = fetchInventory();
        for (BloodBagRecord r : inventory) {
            if ("AVAILABLE".equalsIgnoreCase(r.getEffectiveStatus())) {
                availableByType.put(r.getBloodType(), availableByType.get(r.getBloodType()) + 1);
            }
        }
        List<String> criticalStockLines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : availableByType.entrySet()) {
            if (entry.getValue() < 5) {
                criticalStockLines.add(entry.getKey() + ": " + entry.getValue() + " units");
            }
        }

        return new DashboardSummary(totalBags, expiryWarnings, criticalStockLines);
    }

    public void refreshExpiredBags() {
        String sql = "UPDATE blood_inventory SET inventory_status = 'EXPIRED' " +
                "WHERE expiry_date < ? AND inventory_status NOT IN ('ISSUED', 'DISCARDED', 'EXPIRED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to refresh expired bags.", e);
        }
    }

    public InventoryActionResult releaseBag(String bagId, TtiScreeningInput tti) {
        // Check current status before releasing
        String selectSql = "SELECT inventory_status, tti_overall_status, expiry_date FROM blood_inventory WHERE bag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
            selectPs.setString(1, bagId);
            try (ResultSet rs = selectPs.executeQuery()) {
                if (!rs.next()) {
                    return InventoryActionResult.failure("Bag not found: " + bagId);
                }
                String status = rs.getString("inventory_status");
                LocalDate expiry = rs.getDate("expiry_date").toLocalDate();
                
                if (expiry.isBefore(LocalDate.now()) || "EXPIRED".equalsIgnoreCase(status)) {
                    return InventoryActionResult.failure("Bag is expired. Cannot release.");
                }
                if (!"QUARANTINE".equalsIgnoreCase(status)) {
                    return InventoryActionResult.failure("Bag must be in QUARANTINE to release. Current: " + status);
                }
            }
        } catch (SQLException e) {
            return InventoryActionResult.failure("Database error: " + e.getMessage());
        }

        // Determine overall TTI and Inventory status based on screening results
        boolean isCleared = tti.allNonReactive();
        String overallStatus = isCleared ? "CLEARED" : "REACTIVE";
        String inventoryStatus = isCleared ? "AVAILABLE" : "DISCARDED";

        String updateSql = "UPDATE blood_inventory SET " +
                "tti_hiv = ?, tti_hbv = ?, tti_hcv = ?, tti_syphilis = ?, tti_malaria = ?, " +
                "tti_overall_status = ?, tti_remarks = ?, " +
                "tti_tested_at = ?, tti_tested_by = ?, tti_test_kit = ?, " +
                "inventory_status = ? " +
                "WHERE bag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, tti.getHiv());
            ps.setString(2, tti.getHbv());
            ps.setString(3, tti.getHcv());
            ps.setString(4, tti.getSyphilis());
            ps.setString(5, tti.getMalaria());
            ps.setString(6, overallStatus);
            ps.setString(7, tti.getRemarks());
            ps.setTimestamp(8, Timestamp.valueOf(tti.getTestedAt()));
            ps.setInt(9, tti.getTestedBy());
            ps.setString(10, tti.getTestKit());
            ps.setString(11, inventoryStatus);
            ps.setString(12, bagId);
            ps.executeUpdate();
            
            if (isCleared) {
                return InventoryActionResult.success("Bag " + bagId + " screened CLEARED and released to AVAILABLE.");
            } else {
                return InventoryActionResult.success("Bag " + bagId + " screened REACTIVE and marked as DISCARDED.");
            }
        } catch (SQLException e) {
            return InventoryActionResult.failure("Failed to release bag: " + e.getMessage());
        }
    }

    public InventoryActionResult discardBag(String bagId, int userId) {
        String updateSql = "UPDATE blood_inventory SET inventory_status = 'DISCARDED' WHERE bag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, bagId);
            ps.executeUpdate();
            return InventoryActionResult.success("Bag " + bagId + " marked as DISCARDED.");
        } catch (SQLException e) {
            return InventoryActionResult.failure("Failed to discard bag: " + e.getMessage());
        }
    }

    public InventoryActionResult issueBag(IssueRequestInput request) {
        String selectSql = "SELECT inventory_status, tti_overall_status, expiry_date FROM blood_inventory WHERE bag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
            selectPs.setString(1, request.getBagId());
            try (ResultSet rs = selectPs.executeQuery()) {
                if (!rs.next()) {
                    return InventoryActionResult.failure("Bag not found: " + request.getBagId());
                }
                String status = rs.getString("inventory_status");
                String ttiStatus = rs.getString("tti_overall_status");
                
                if (rs.getDate("expiry_date").toLocalDate().isBefore(LocalDate.now()) || 
                    "EXPIRED".equalsIgnoreCase(status)) {
                    return InventoryActionResult.failure("Bag is expired. Cannot issue.");
                }
                if (!"AVAILABLE".equalsIgnoreCase(status) || !"CLEARED".equalsIgnoreCase(ttiStatus)) {
                    return InventoryActionResult.failure("Bag must be AVAILABLE with CLEARED TTI to issue. Current: " + status + ", TTI: " + ttiStatus);
                }
                if (!"COMPATIBLE".equalsIgnoreCase(request.getCrossmatchStatus())) {
                    return InventoryActionResult.failure("Crossmatch must be COMPATIBLE to issue. Current: " + request.getCrossmatchStatus());
                }
            }
        } catch (SQLException e) {
            return InventoryActionResult.failure("Database error: " + e.getMessage());
        }

        String updateSql = "UPDATE blood_inventory SET " +
                "issued_at = ?, issue_patient_name = ?, patient_hospital_no = ?, " +
                "request_hospital = ?, requesting_physician = ?, blood_request_no = ?, " +
                "crossmatch_status = ?, issued_by = ?, issue_notes = ?, " +
                "inventory_status = 'ISSUED' " +
                "WHERE bag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setTimestamp(1, Timestamp.valueOf(request.getIssuedAt()));
            ps.setString(2, request.getPatientName());
            ps.setString(3, request.getPatientHospitalNo());
            ps.setString(4, request.getRequestHospital());
            ps.setString(5, request.getRequestingPhysician());
            ps.setString(6, request.getBloodRequestNo());
            ps.setString(7, request.getCrossmatchStatus());
            ps.setInt(8, request.getIssuedBy());
            ps.setString(9, request.getIssueNotes());
            ps.setString(10, request.getBagId());
            ps.executeUpdate();
            return InventoryActionResult.success("Bag " + request.getBagId() + " issued successfully.");
        } catch (SQLException e) {
            return InventoryActionResult.failure("Failed to issue bag: " + e.getMessage());
        }
    }
}
