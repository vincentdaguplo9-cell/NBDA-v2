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
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
                "d.barangay, " +
                "bi.blood_type, bi.collection_date, " +
                "bi.expiry_date, bi.tti_overall_status AS tti_status, bi.inventory_status AS status " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "ORDER BY bi.collection_date DESC, bi.bag_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(new BloodBagRecord(
                        rs.getString("bag_id"),
                        rs.getInt("donor_id"),
                        rs.getString("donor_name"),
                        rs.getString("barangay"),
                        rs.getString("blood_type"),
                        rs.getDate("collection_date").toLocalDate(),
                        rs.getDate("expiry_date").toLocalDate(),
                        rs.getString("tti_status"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load inventory.", e);
        }
        return records;
    }

    public DashboardSummary fetchDashboardSummary() {
        refreshExpiredBags();
        try (Connection conn = DBConnection.getConnection()) {
            int totalBags = countTotalBags(conn);
            int expiringSoon = countExpiringSoon(conn);
            List<String> criticalLines = criticalStockLines(conn);
            return new DashboardSummary(totalBags, expiringSoon, criticalLines);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load dashboard metrics.", e);
        }
    }

    public InventoryActionResult releaseBagAfterScreening(String bagId, TtiScreeningInput screeningInput, int userId) {
        refreshExpiredBags();
        String selectSql = "SELECT inventory_status, expiry_date FROM blood_inventory WHERE bag_id = ?";
        String updateSql = "UPDATE blood_inventory SET tti_hiv = ?, tti_hbv = ?, tti_hcv = ?, tti_syphilis = ?, " +
                "tti_malaria = ?, tti_overall_status = ?, tti_tested_at = ?, tti_tested_by = ?, tti_test_kit = ?, " +
                "inventory_status = ?, disposition_notes = ? WHERE bag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, bagId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    return InventoryActionResult.failure("Selected bag was not found.");
                }
                String status = rs.getString("inventory_status");
                LocalDate expiry = rs.getDate("expiry_date").toLocalDate();
                if (expiry.isBefore(LocalDate.now()) || "EXPIRED".equalsIgnoreCase(status)) {
                    return InventoryActionResult.failure("Expired blood cannot be released.");
                }
                if (!"QUARANTINE".equalsIgnoreCase(status)) {
                    return InventoryActionResult.failure("Only quarantined bags can be processed for release.");
                }
            }

            boolean allNonReactive = screeningInput.allNonReactive();
            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setString(1, screeningInput.getHiv());
                update.setString(2, screeningInput.getHbv());
                update.setString(3, screeningInput.getHcv());
                update.setString(4, screeningInput.getSyphilis());
                update.setString(5, screeningInput.getMalaria());
                update.setString(6, allNonReactive ? "CLEARED" : "REACTIVE");
                update.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));
                update.setInt(8, userId);
                update.setString(9, screeningInput.getTestKit());
                update.setString(10, allNonReactive ? "AVAILABLE" : "DISCARDED");
                update.setString(11, screeningInput.getRemarks());
                update.setString(12, bagId);
                update.executeUpdate();
            }

            insertAudit(conn, userId, allNonReactive ? "TTI_RELEASED" : "TTI_REACTIVE",
                    "blood_inventory", bagId,
                    allNonReactive ? "Bag released after non-reactive TTI screening." : "Bag discarded after reactive TTI screening.");

            if (allNonReactive) {
                return InventoryActionResult.success("Bag " + bagId + " cleared and moved to AVAILABLE stock.");
            }
            return InventoryActionResult.success("Reactive TTI result recorded. Bag " + bagId + " moved to DISCARDED.");
        } catch (SQLException e) {
            return InventoryActionResult.failure("Release failed: " + e.getMessage());
        }
    }

    public InventoryActionResult issueBag(String bagId, IssueRequestInput request, int userId) {
        refreshExpiredBags();
        String selectSql = "SELECT inventory_status, tti_overall_status, expiry_date FROM blood_inventory WHERE bag_id = ?";
        String updateSql = "UPDATE blood_inventory SET inventory_status = ?, crossmatch_status = ?, issue_patient_name = ?, " +
                "patient_hospital_no = ?, request_hospital = ?, requesting_physician = ?, blood_request_no = ?, " +
                "issued_at = NOW(), issued_by = ?, issue_notes = ? WHERE bag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, bagId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    return InventoryActionResult.failure("Selected bag was not found.");
                }
                String status = rs.getString("inventory_status");
                String ttiStatus = rs.getString("tti_overall_status");
                LocalDate expiry = rs.getDate("expiry_date").toLocalDate();
                if (expiry.isBefore(LocalDate.now()) || "EXPIRED".equalsIgnoreCase(status)) {
                    return InventoryActionResult.failure("Expired blood cannot be issued.");
                }
                if (!"AVAILABLE".equalsIgnoreCase(status) || !"CLEARED".equalsIgnoreCase(ttiStatus)) {
                    if ("AVAILABLE".equalsIgnoreCase(status) && "DISCARDED".equalsIgnoreCase(ttiStatus)) {
                        return InventoryActionResult.failure("Bag was discarded due to reactive TTI result.");
                    }
                    return InventoryActionResult.failure("Only AVAILABLE bags with cleared TTI screening can be issued.");
                }
            }

            if (request == null) {
                return InventoryActionResult.failure("Issuance details are required.");
            }
            if (blank(request.getPatientName()) || blank(request.getRequestHospital())
                    || blank(request.getRequestingPhysician()) || blank(request.getBloodRequestNo())) {
                return InventoryActionResult.failure("Patient, hospital, physician, and blood request number are required.");
            }
            if (!"COMPATIBLE".equalsIgnoreCase(request.getCrossmatchStatus())
                    && !"NOT_REQUIRED".equalsIgnoreCase(request.getCrossmatchStatus())) {
                return InventoryActionResult.failure("Only COMPATIBLE or NOT_REQUIRED requests can proceed to issuance.");
            }

            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setString(1, "ISSUED");
                update.setString(2, request.getCrossmatchStatus());
                update.setString(3, request.getPatientName());
                update.setString(4, request.getPatientHospitalNo());
                update.setString(5, request.getRequestHospital());
                update.setString(6, request.getRequestingPhysician());
                update.setString(7, request.getBloodRequestNo());
                update.setInt(8, userId);
                update.setString(9, request.getIssueNotes());
                update.setString(10, bagId);
                update.executeUpdate();
            }
            insertAudit(conn, userId, "BAG_ISSUED", "blood_inventory", bagId,
                    "Issued to " + request.getPatientName() + " for " + request.getRequestHospital()
                            + " under request " + request.getBloodRequestNo() + ".");
            return InventoryActionResult.success("Bag " + bagId + " marked as ISSUED.");
        } catch (SQLException e) {
            return InventoryActionResult.failure("Issuance failed: " + e.getMessage());
        }
    }

    public void refreshExpiredBags() {
        String sql = "UPDATE blood_inventory SET inventory_status = 'EXPIRED' " +
                "WHERE expiry_date < ? AND inventory_status NOT IN ('ISSUED', 'DISCARDED', 'EXPIRED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update expired inventory.", e);
        }
    }

    private int countTotalBags(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM blood_inventory";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countExpiringSoon(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM blood_inventory WHERE expiry_date BETWEEN ? AND ? " +
                "AND inventory_status NOT IN ('ISSUED', 'DISCARDED', 'EXPIRED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setDate(2, Date.valueOf(LocalDate.now().plusDays(7)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<String> criticalStockLines(Connection conn) throws SQLException {
        Map<String, Integer> availableCounts = new LinkedHashMap<>();
        for (String bloodType : BLOOD_TYPES) {
            availableCounts.put(bloodType, 0);
        }

        String sql = "SELECT blood_type, COUNT(*) AS bag_count FROM blood_inventory " +
                "WHERE inventory_status = 'AVAILABLE' AND tti_overall_status = 'CLEARED' GROUP BY blood_type";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                availableCounts.put(rs.getString("blood_type"), rs.getInt("bag_count"));
            }
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : availableCounts.entrySet()) {
            if (entry.getValue() < 5) {
                lines.add(entry.getKey() + " - " + entry.getValue() + " bag(s) available");
            }
        }
        if (lines.isEmpty()) {
            lines.add("All blood types are above the critical threshold.");
        }
        return lines;
    }

    private void insertAudit(Connection conn, int userId, String actionType, String entityType, String entityId, String details)
            throws SQLException {
        String sql = "INSERT INTO audit_log (user_id, action_type, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, entityType);
            ps.setString(4, entityId);
            ps.setString(5, details);
            ps.executeUpdate();
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
