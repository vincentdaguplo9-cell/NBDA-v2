package dao;

import database.DBConnection;
import model.AuditLogRecord;
import model.BloodBagRecord;
import model.DonorRecord;
import model.IssuanceRecord;
import model.ScreeningHistoryRecord;

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
        String sql = "SELECT donor_id, first_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
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
        String sql = "SELECT donor_id, first_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
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
            throw new RuntimeException("Failed to load donors page.", e);
        }
        return records;
    }

    private DonorRecord parseDonor(ResultSet rs) throws SQLException {
        return new DonorRecord(
                rs.getInt("donor_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("sex"),
                toLocalDate(rs.getDate("birth_date")),
                rs.getString("blood_type"),
                rs.getString("barangay"),
                rs.getString("contact_no"),
                toLocalDate(rs.getDate("last_successful_donation"))
        );
    }

    public boolean updateDonor(DonorRecord donor) {
        String sql = "UPDATE donors SET first_name = ?, last_name = ?, sex = ?, birth_date = ?, blood_type = ?, " +
                "barangay = ?, contact_no = ?, last_successful_donation = ? WHERE donor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donor.getFirstName());
            ps.setString(2, donor.getLastName());
            ps.setString(3, donor.getSex());
            ps.setDate(4, toDate(donor.getBirthDate()));
            ps.setString(5, donor.getBloodType());
            ps.setString(6, donor.getBarangay());
            ps.setString(7, donor.getContactNo());
            ps.setDate(8, toDate(donor.getLastSuccessfulDonation()));
            ps.setInt(9, donor.getDonorId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update donor profile.", e);
        }
    }

    public List<ScreeningHistoryRecord> fetchScreenings() {
        List<ScreeningHistoryRecord> records = new ArrayList<>();
        String sql = "SELECT ds.screening_id, ds.screening_date, ds.intended_collection_date, ds.screening_status, ds.decision_reason, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
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
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
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

    public List<IssuanceRecord> fetchIssuanceLog() {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, bi.blood_type, bi.collection_date, bi.issued_at, bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
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
            throw new RuntimeException("Failed to load issuance records.", e);
        }
        return records;
    }

    public List<IssuanceRecord> fetchIssuanceLogPage(int page, int pageSize) {
        List<IssuanceRecord> records = new ArrayList<>();
        int offset = page * pageSize;
        String sql = "SELECT bi.bag_id, bi.blood_type, bi.collection_date, bi.issued_at, bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
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
        String sql = "SELECT al.event_time, al.action_type, al.entity_type, al.entity_id, al.details, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS actor " +
                "FROM audit_log al LEFT JOIN users u ON u.user_id = al.user_id " +
                "ORDER BY al.event_time DESC, al.audit_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(parseAudit(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load audit logs.", e);
        }
        return records;
    }

    public List<AuditLogRecord> fetchAuditLogsPage(int page, int pageSize) {
        List<AuditLogRecord> records = new ArrayList<>();
        int offset = page * pageSize;
        String sql = "SELECT al.event_time, al.action_type, al.entity_type, al.entity_id, al.details, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS actor " +
                "FROM audit_log al LEFT JOIN users u ON u.user_id = al.user_id " +
                "ORDER BY al.event_time DESC, al.audit_id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(parseAudit(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load audit page.", e);
        }
        return records;
    }

    private AuditLogRecord parseAudit(ResultSet rs) throws SQLException {
        return new AuditLogRecord(
                toLocalDateTime(rs.getTimestamp("event_time")),
                rs.getString("actor"),
                rs.getString("action_type"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                rs.getString("details")
        );
    }

    public List<IssuanceRecord> fetchRecentIssuances() {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, bi.blood_type, bi.collection_date, bi.issued_at, bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS issued_by " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "LEFT JOIN users u ON u.user_id = bi.issued_by " +
                "WHERE bi.issued_at IS NOT NULL " +
                "ORDER BY bi.issued_at DESC LIMIT 20";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(new IssuanceRecord(
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
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load recent issuances.", e);
        }
        return records;
    }

    public List<DonorRecord> searchDonors(String query) {
        List<DonorRecord> records = new ArrayList<>();
        String sql = "SELECT donor_id, first_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
                "FROM donors WHERE LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR contact_no LIKE ? " +
                "ORDER BY last_name, first_name, donor_id DESC";
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new DonorRecord(
                            rs.getInt("donor_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("sex"),
                            toLocalDate(rs.getDate("birth_date")),
                            rs.getString("blood_type"),
                            rs.getString("barangay"),
                            rs.getString("contact_no"),
                            toLocalDate(rs.getDate("last_successful_donation"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search donors.", e);
        }
        return records;
    }

    public List<ScreeningHistoryRecord> searchScreenings(String query) {
        List<ScreeningHistoryRecord> records = new ArrayList<>();
        String sql = "SELECT ds.screening_id, ds.screening_date, ds.intended_collection_date, ds.screening_status, ds.decision_reason, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
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
                    records.add(new ScreeningHistoryRecord(
                            rs.getInt("screening_id"),
                            rs.getString("donor_name"),
                            toLocalDate(rs.getDate("screening_date")),
                            toLocalDate(rs.getDate("intended_collection_date")),
                            rs.getString("screening_status"),
                            rs.getString("decision_reason"),
                            rs.getString("screened_by")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search screenings.", e);
        }
        return records;
    }

    public List<IssuanceRecord> searchIssuances(String query) {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, bi.blood_type, bi.collection_date, bi.issued_at, bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
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
                    records.add(new IssuanceRecord(
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
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search issuances.", e);
        }
        return records;
    }

    public List<DonorRecord> fetchDonorHistory(int donorId) {
        List<DonorRecord> records = new ArrayList<>();
        String sql = "SELECT donor_id, first_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
                "FROM donors WHERE donor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    records.add(new DonorRecord(
                            rs.getInt("donor_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("sex"),
                            toLocalDate(rs.getDate("birth_date")),
                            rs.getString("blood_type"),
                            rs.getString("barangay"),
                            rs.getString("contact_no"),
                            toLocalDate(rs.getDate("last_successful_donation"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor history.", e);
        }
        return records;
    }

    public List<ScreeningHistoryRecord> fetchDonorScreenings(int donorId) {
        List<ScreeningHistoryRecord> records = new ArrayList<>();
        String sql = "SELECT ds.screening_id, ds.screening_date, ds.intended_collection_date, ds.screening_status, ds.decision_reason, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS screened_by " +
                "FROM donor_screening ds " +
                "INNER JOIN donors d ON d.donor_id = ds.donor_id " +
                "LEFT JOIN users u ON u.user_id = ds.screened_by " +
                "WHERE ds.donor_id = ? " +
                "ORDER BY ds.screening_date DESC, ds.screening_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new ScreeningHistoryRecord(
                            rs.getInt("screening_id"),
                            rs.getString("donor_name"),
                            toLocalDate(rs.getDate("screening_date")),
                            toLocalDate(rs.getDate("intended_collection_date")),
                            rs.getString("screening_status"),
                            rs.getString("decision_reason"),
                            rs.getString("screened_by")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor screenings.", e);
        }
        return records;
    }

    public List<IssuanceRecord> fetchDonorIssuances(int donorId) {
        List<IssuanceRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, bi.blood_type, bi.collection_date, bi.issued_at, bi.issue_patient_name, bi.request_hospital, " +
                "bi.requesting_physician, bi.blood_request_no, bi.crossmatch_status, " +
                "TRIM(CONCAT_WS(' ', d.first_name, d.last_name)) AS donor_name, " +
                "TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS issued_by " +
                "FROM blood_inventory bi " +
                "INNER JOIN donors d ON d.donor_id = bi.donor_id " +
                "LEFT JOIN users u ON u.user_id = bi.issued_by " +
                "WHERE bi.donor_id = ? " +
                "ORDER BY bi.collection_date DESC, bi.bag_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new IssuanceRecord(
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
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor issuances.", e);
        }
        return records;
    }

    public List<BloodBagRecord> fetchDonorBloodBags(int donorId) {
        List<BloodBagRecord> records = new ArrayList<>();
        String sql = "SELECT bi.bag_id, bi.donor_id, bi.blood_type, bi.collection_date, bi.expiry_date, " +
                "bi.tti_overall_status, bi.inventory_status AS status " +
                "FROM blood_inventory bi " +
                "WHERE bi.donor_id = ? " +
                "ORDER BY bi.collection_date DESC, bi.bag_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new BloodBagRecord(
                            rs.getString("bag_id"),
                            rs.getInt("donor_id"),
                            null,
                            null,
                            rs.getString("blood_type"),
                            toLocalDate(rs.getDate("collection_date")),
                            toLocalDate(rs.getDate("expiry_date")),
                            rs.getString("tti_overall_status"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor blood bags.", e);
        }
        return records;
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Date toDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }
}
