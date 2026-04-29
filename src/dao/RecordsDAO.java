package dao;

import database.DBConnection;
import model.AuditLogRecord;
import model.BloodBagRecord;
import model.DonorRecord;
import model.IssuanceRecord;
import model.ScreeningHistoryRecord;
import ui.UserSession;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Read and update operations for donor history, screening logs, issued bags, and audit trails.
public class RecordsDAO {
    public List<DonorRecord> fetchDonors() {
        List<DonorRecord> records = new ArrayList<>();
        String sql = "SELECT donor_id, first_name, middle_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
                "FROM donors ORDER BY last_name, first_name, donor_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(parseDonor(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donors.", e);
        }
        return records;
    }

    public List<DonorRecord> fetchDonorsPage(int page, int pageSize) {
        List<DonorRecord> records = new ArrayList<>();
        int offset = page * pageSize;
        String sql = "SELECT donor_id, first_name, middle_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
                "FROM donors ORDER BY last_name, first_name, donor_id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseDonor(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor page.", e);
        }
        return records;
    }

    public List<ScreeningHistoryRecord> fetchScreenings() {
        List<ScreeningHistoryRecord> records = new ArrayList<>();
        String sql = "SELECT ds.screening_id, ds.screening_date, ds.intended_collection_date, ds.screening_status, ds.decision_reason, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS screened_by " +
                "FROM donor_screening ds " +
                "INNER JOIN donors d ON d.donor_id = ds.donor_id " +
                "LEFT JOIN users u ON u.user_id = ds.screened_by " +
                "ORDER BY ds.screening_date DESC, ds.screening_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(parseScreening(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load screening history.", e);
        }
        return records;
    }

    public List<ScreeningHistoryRecord> fetchScreeningsPage(int page, int pageSize) {
        List<ScreeningHistoryRecord> records = new ArrayList<>();
        int offset = page * pageSize;
        String sql = "SELECT ds.screening_id, ds.screening_date, ds.intended_collection_date, ds.screening_status, ds.decision_reason, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS screened_by " +
                "FROM donor_screening ds " +
                "INNER JOIN donors d ON d.donor_id = ds.donor_id " +
                "LEFT JOIN users u ON u.user_id = ds.screened_by " +
                "ORDER BY ds.screening_date DESC, ds.screening_id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseScreening(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load screening page.", e);
        }
        return records;
    }

    public List<IssuanceRecord> fetchIssuances() {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "bi.blood_type, bi.collection_date, bi.issued_at, " +
                "bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, " +
                "bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS issued_by " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "LEFT JOIN users u ON u.user_id = bi.issued_by " +
                "WHERE bi.issued_at IS NOT NULL " +
                "ORDER BY bi.issued_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(parseIssuance(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load issuances.", e);
        }
        return records;
    }

    public List<IssuanceRecord> fetchIssuancesPage(int page, int pageSize) {
        List<IssuanceRecord> records = new ArrayList<>();
        int offset = page * pageSize;
        String sql = "SELECT bi.bag_id, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "bi.blood_type, bi.collection_date, bi.issued_at, " +
                "bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, " +
                "bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS issued_by " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "LEFT JOIN users u ON u.user_id = bi.issued_by " +
                "WHERE bi.issued_at IS NOT NULL " +
                "ORDER BY bi.issued_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseIssuance(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load issuance page.", e);
        }
        return records;
    }

    public List<DonorRecord> searchDonors(String query) {
        List<DonorRecord> records = new ArrayList<>();
        String sql = "SELECT d.donor_id, d.first_name, d.middle_name, d.last_name, d.sex, d.birth_date, d.blood_type, d.barangay, d.contact_no, d.last_successful_donation " +
                "FROM donors d " +
                "WHERE LOWER(d.first_name) LIKE ? OR LOWER(d.last_name) LIKE ? OR d.contact_no LIKE ? " +
                "ORDER BY d.last_name, d.first_name, d.donor_id DESC";
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseDonor(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search donors.", e);
        }
        return records;
    }

    public List<DonorRecord> searchDonorsByStaff(String staffQuery) {
        List<DonorRecord> records = new ArrayList<>();
        String sql = "SELECT DISTINCT d.donor_id, d.first_name, d.middle_name, d.last_name, d.sex, d.birth_date, d.blood_type, d.barangay, d.contact_no, d.last_successful_donation " +
                "FROM donors d " +
                "INNER JOIN donor_screening ds ON d.donor_id = ds.donor_id " +
                "LEFT JOIN users u ON u.user_id = ds.screened_by " +
                "WHERE LOWER(u.staff_id) LIKE ? OR LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ? " +
                "ORDER BY d.last_name, d.first_name, d.donor_id DESC";
        String pattern = "%" + staffQuery.toLowerCase() + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseDonor(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search donors by staff.", e);
        }
        return records;
    }

    public List<ScreeningHistoryRecord> searchScreenings(String query) {
        List<ScreeningHistoryRecord> records = new ArrayList<>();
        String sql = "SELECT ds.screening_id, ds.screening_date, ds.intended_collection_date, ds.screening_status, ds.decision_reason, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS screened_by " +
                "FROM donor_screening ds " +
                "INNER JOIN donors d ON d.donor_id = ds.donor_id " +
                "LEFT JOIN users u ON u.user_id = ds.screened_by " +
                "WHERE LOWER(d.first_name) LIKE ? OR LOWER(d.last_name) LIKE ? OR LOWER(ds.screening_status) LIKE ? " +
                "ORDER BY ds.screening_date DESC, ds.screening_id DESC";
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseScreening(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search screenings.", e);
        }
        return records;
    }

    public List<IssuanceRecord> searchIssuances(String query) {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "bi.blood_type, bi.collection_date, bi.issued_at, " +
                "bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, " +
                "bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS issued_by " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "LEFT JOIN users u ON u.user_id = bi.issued_by " +
                "WHERE bi.issued_at IS NOT NULL AND (" +
                "LOWER(bi.bag_id) LIKE ? OR LOWER(bi.issue_patient_name) LIKE ? OR " +
                "LOWER(bi.request_hospital) LIKE ? OR LOWER(bi.blood_request_no) LIKE ?) " +
                "ORDER BY bi.issued_at DESC";
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseIssuance(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search issuances.", e);
        }
        return records;
    }

    public List<DonorRecord> fetchDonorHistory(String donorId) {
        List<DonorRecord> records = new ArrayList<>();
        String sql = "SELECT donor_id, first_name, middle_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
                "FROM donors WHERE donor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    records.add(parseDonor(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor history.", e);
        }
        return records;
    }

    public List<ScreeningHistoryRecord> fetchDonorScreenings(String donorId) {
        List<ScreeningHistoryRecord> records = new ArrayList<>();
        String sql = "SELECT ds.screening_id, ds.screening_date, ds.intended_collection_date, ds.screening_status, ds.decision_reason, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS screened_by " +
                "FROM donor_screening ds " +
                "INNER JOIN donors d ON d.donor_id = ds.donor_id " +
                "LEFT JOIN users u ON u.user_id = ds.screened_by " +
                "WHERE ds.donor_id = ? " +
                "ORDER BY ds.screening_date DESC, ds.screening_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseScreening(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor screenings.", e);
        }
        return records;
    }

    public List<BloodBagRecord> fetchDonorBloodBags(String donorId) {
        List<BloodBagRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, bi.donor_id, bi.blood_type, bi.collection_date, bi.expiry_date, " +
                "bi.tti_overall_status, bi.inventory_status, " +
                "d.first_name, d.middle_name, d.last_name, d.barangay " +
                "FROM blood_inventory bi " +
                "LEFT JOIN donors d ON bi.donor_id = d.donor_id " +
                "WHERE bi.donor_id = ? " +
                "ORDER BY bi.collection_date DESC, bi.bag_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String middleName = rs.getString("middle_name");
                    String lastName = rs.getString("last_name");
                    String donorName = buildDonorName(firstName, middleName, lastName);
                    
                    records.add(new BloodBagRecord(
                            rs.getString("bag_id"),
                            rs.getString("donor_id"),
                            donorName,
                            rs.getString("barangay"),
                            rs.getString("blood_type"),
                            toLocalDate(rs.getDate("collection_date")),
                            toLocalDate(rs.getDate("expiry_date")),
                            rs.getString("inventory_status"),  // param 8: inventoryStatus
                            rs.getString("tti_overall_status") // param 9: ttiStatus
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor blood bags.", e);
        }
        return records;
    }

    private String buildDonorName(String firstName, String middleName, String lastName) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            name.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(middleName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName);
        }
        return name.toString();
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    public int getDonorCount() {
        return countTable("donors");
    }

    public int getScreeningCount() {
        return countTable("donor_screening");
    }

    public int getIssuanceCount() {
        String sql = "SELECT COUNT(*) FROM blood_inventory WHERE issued_at IS NOT NULL";
        return countCustom(sql);
    }

    public int getAuditCount() {
        return countTable("audit_log");
    }

    private int countTable(String table) {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count " + table, e);
        }
        return 0;
    }

    private int countCustom(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count", e);
        }
        return 0;
    }

    private DonorRecord parseDonor(ResultSet rs) throws SQLException {
        return new DonorRecord(
                rs.getString("donor_id"),
                rs.getString("first_name"),
                rs.getString("middle_name") != null ? rs.getString("middle_name") : "",
                rs.getString("last_name"),
                rs.getString("sex"),
                toLocalDate(rs.getDate("birth_date")),
                rs.getString("blood_type"),
                rs.getString("barangay"),
                rs.getString("contact_no"),
                toLocalDate(rs.getDate("last_successful_donation"))
        );
    }

    private ScreeningHistoryRecord parseScreening(ResultSet rs) throws SQLException {
        return new ScreeningHistoryRecord(
                rs.getInt("screening_id"),
                rs.getString("donor_name"),
                toLocalDate(rs.getDate("screening_date")),
                toLocalDate(rs.getDate("intended_collection_date")),
                rs.getString("screening_status"),
                rs.getString("decision_reason"),
                rs.getString("screened_by")
        );
    }

    private IssuanceRecord parseIssuance(ResultSet rs) throws SQLException {
        return new IssuanceRecord(
                rs.getString("bag_id"),
                rs.getString("donor_name"),
                rs.getString("blood_type"),
                toLocalDate(rs.getDate("collection_date")),
                toLocalDateTime(rs.getTimestamp("issued_at")),
                rs.getString("issue_patient_name"),
                rs.getString("request_hospital"),
                rs.getString("requesting_physician"),
                rs.getString("blood_request_no"),
                rs.getString("crossmatch_status"),
                rs.getString("issued_by")
        );
    }

    public List<AuditLogRecord> fetchAuditLogs() {
        List<AuditLogRecord> records = new ArrayList<>();
        String sql = "SELECT user_id, action_type, entity_type, entity_id, details, event_time " +
                "FROM audit_log ORDER BY event_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(new AuditLogRecord(
                        toLocalDateTime(rs.getTimestamp("event_time")),
                        String.valueOf(rs.getInt("user_id")),
                        rs.getString("action_type"),
                        rs.getString("entity_type"),
                        rs.getString("entity_id"),
                        rs.getString("details")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load audit logs.", e);
        }
        return records;
    }

    public List<AuditLogRecord> fetchAuditLogsPage(int page, int pageSize) {
        List<AuditLogRecord> records = new ArrayList<>();
        int offset = page * pageSize;
        String sql = "SELECT user_id, action_type, entity_type, entity_id, details, event_time " +
                "FROM audit_log ORDER BY event_time DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new AuditLogRecord(
                            toLocalDateTime(rs.getTimestamp("event_time")),
                            String.valueOf(rs.getInt("user_id")),
                            rs.getString("action_type"),
                            rs.getString("entity_type"),
                            rs.getString("entity_id"),
                            rs.getString("details")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load audit page.", e);
        }
        return records;
    }

    // Fetch recent issuances for dashboard
    public List<IssuanceRecord> fetchRecentIssuances() {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "bi.blood_type, bi.collection_date, bi.issued_at, " +
                "bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, " +
                "bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS issued_by " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "LEFT JOIN users u ON u.user_id = bi.issued_by " +
                "WHERE bi.issued_at IS NOT NULL " +
                "ORDER BY bi.issued_at DESC LIMIT 10";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(parseIssuance(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load recent issuances.", e);
        }
        return records;
    }

    // Fetch issuances for a specific donor
    public List<IssuanceRecord> fetchDonorIssuances(String donorId) {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.middle_name, d.last_name)) AS donor_name, " +
                "bi.blood_type, bi.collection_date, bi.issued_at, " +
                "bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, " +
                "bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS issued_by " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "LEFT JOIN users u ON u.user_id = bi.issued_by " +
                "WHERE bi.donor_id = ? AND bi.issued_at IS NOT NULL " +
                "ORDER BY bi.issued_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseIssuance(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor issuances.", e);
        }
        return records;
    }

    // Alias for fetchIssuancesPage to match what RecordsViewController expects
    public List<IssuanceRecord> fetchIssuanceLogPage(int page, int pageSize) {
        return fetchIssuancesPage(page, pageSize);
    }

    // Update donor record
    public boolean updateDonor(DonorRecord updated) {
        String sql = "UPDATE donors SET first_name = ?, middle_name = ?, last_name = ?, " +
                "sex = ?, birth_date = ?, blood_type = ?, barangay = ?, contact_no = ?, " +
                "last_successful_donation = ? WHERE donor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, updated.getFirstName());
            ps.setString(2, updated.getMiddleName());
            ps.setString(3, updated.getLastName());
            ps.setString(4, updated.getSex());
            ps.setDate(5, toDate(updated.getBirthDate()));
            ps.setString(6, updated.getBloodType());
            ps.setString(7, updated.getBarangay());
            ps.setString(8, updated.getContactNo());
            ps.setDate(9, toDate(updated.getLastSuccessfulDonation()));
            ps.setString(10, updated.getDonorId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                String actor = String.valueOf(UserSession.getCurrentUser() != null ?
                        UserSession.getCurrentUser().getUserId() : "system");
                logAudit(actor, "UPDATE_DONOR", "donor", updated.getDonorId(),
                        "Updated donor: " + updated.getLastName() + ", " + updated.getFirstName());
            }
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update donor.", e);
        }
    }

    // Log an audit event
    private void logAudit(String actor, String actionType, String entityType, String entityId, String details) {
        String sql = "INSERT INTO audit_log (user_id, action_type, entity_type, entity_id, details, event_time) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actor);
            ps.setString(2, actionType);
            ps.setString(3, entityType);
            ps.setString(4, entityId != null ? entityId : "N/A");
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log audit: " + e.getMessage());
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Date toDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }
}
