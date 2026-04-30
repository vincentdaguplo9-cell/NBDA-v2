package dao;

import database.DBConnection;
import model.DohDonor;
import model.RegistrationResult;
import model.ScreeningInput;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

// Donor registration and screening flow aligned to the single blood archive schema.
public class DohDonorDAO {
    private static final String FACILITY_CODE = "NVL01";
    private static final double MIN_WEIGHT_KG = 50.0;
    private static final double MIN_HEMOGLOBIN_GDL = 12.5;
    private static final double MAX_TEMPERATURE_C = 37.5;
    private static final int MIN_SYSTOLIC_BP = 90;
    private static final int MAX_SYSTOLIC_BP = 160;
    private static final int MIN_DIASTOLIC_BP = 60;
    private static final int MAX_DIASTOLIC_BP = 100;
    private static final int MIN_PULSE_BPM = 60;
    private static final int MAX_PULSE_BPM = 100;

    public RegistrationResult registerDonation(DohDonor donor, ScreeningInput screening, int screenedBy) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ExistingDonor existingDonor = findExistingDonor(conn, donor);
                LocalDate previousDonation = mostRecentDonation(
                        existingDonor == null ? null : existingDonor.lastSuccessfulDonation,
                        donor.getLastSuccessfulDonation()
                );

                String donorId;
                if (existingDonor == null) {
                    donorId = insertDonor(conn, donor, previousDonation);
                } else {
                    donorId = existingDonor.id;
                    updateDonor(conn, donorId, donor, previousDonation);
                }

                ScreeningDecision decision = evaluateEligibility(donor, screening, previousDonation);
                int screeningId = insertScreening(conn, donorId, screening, screenedBy, decision, screening.getAuthIdType());

                if (!decision.eligible) {
                    insertAudit(conn, screenedBy, "SCREENING_DEFERRED", "donor_screening",
                            String.valueOf(screeningId), decision.message);
                    conn.commit();
                    return RegistrationResult.deferred(decision.message);
                }

                String bagId = nextBagId(conn, screening.getCollectionDate());
                insertBag(conn, bagId, donorId, screeningId, donor.getBloodType(), screening.getCollectionDate(), screening.getCollectionVolumeMl());
                updateLastSuccessfulDonation(conn, donorId, screening.getCollectionDate());
                insertAudit(conn, screenedBy, "DONOR_ACCEPTED", "blood_inventory", bagId,
                        "Donor accepted and blood bag collected into quarantine.");

                conn.commit();
                return RegistrationResult.eligible(
                        "Donor accepted for whole blood donation. Bag " + bagId + " is now in QUARANTINE pending TTI screening.",
                        bagId
                );
            } catch (SQLException ex) {
                conn.rollback();
                return RegistrationResult.error("Registration failed: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            return RegistrationResult.error("Registration failed: " + ex.getMessage());
        }
    }

    private ScreeningDecision evaluateEligibility(DohDonor donor, ScreeningInput screening, LocalDate previousDonation) {
        List<String> reasons = new ArrayList<>();
        LocalDate nextEligibleDate = null;

        // Check if ID type was selected for authentication (transient auth model)
        // First-time donors are exempt from strict ID requirement (they may be in process of getting ID)
        String authIdType = screening.getAuthIdType();
        if (!screening.isFirstTimeDonor()) {
            if (authIdType == null || authIdType.isBlank() || "NONE".equals(authIdType)) {
                reasons.add("Valid ID must be presented and type selected for authentication.");
            }
        }

        int age = Period.between(donor.getBirthdate(), screening.getCollectionDate()).getYears();
        if (age < 16) {
            reasons.add("Donor must be at least 16 years old.");
            nextEligibleDate = donor.getBirthdate().plusYears(16);
        } else if (age < 18 && !screening.isGuardianConsentProvided()) {
            reasons.add("Donors aged 16 to 17 require written parent or guardian consent.");
            nextEligibleDate = screening.getCollectionDate();
        } else if (age > 65) {
            reasons.add("Donor must not be older than 65 years old.");
        } else if (age > 60 && previousDonation == null && !screening.isFirstTimeDonor()) {
            reasons.add("Donors aged 61 to 65 require prior successful donation history as regular donors.");
        } else if (screening.isFirstTimeDonor() && age >= 16 && age <= 65) {
            // First-time donor - accepted with note (not deferred)
            // They may need additional counseling but are not automatically deferred
            // This is informational only - do NOT add to reasons (which would cause deferral)
        }

        if (screening.getWeightKg() < MIN_WEIGHT_KG) {
            reasons.add("Weight must be at least 50 kg.");
        }

        if (screening.getHemoglobinGdl() < MIN_HEMOGLOBIN_GDL) {
            reasons.add("Hemoglobin must be at least 12.5 g/dL.");
        }

        if (screening.getTemperatureC() > MAX_TEMPERATURE_C) {
            reasons.add("Temperature must not exceed 37.5 C.");
        }

        if (screening.getSystolicBp() < MIN_SYSTOLIC_BP || screening.getSystolicBp() > MAX_SYSTOLIC_BP
                || screening.getDiastolicBp() < MIN_DIASTOLIC_BP || screening.getDiastolicBp() > MAX_DIASTOLIC_BP) {
            reasons.add("Blood pressure must be within the acceptable donor range.");
        }

        if (screening.getPulseBpm() < MIN_PULSE_BPM || screening.getPulseBpm() > MAX_PULSE_BPM) {
            reasons.add("Pulse rate must be between 50 and 100 bpm.");
        }

        if (screening.isAlcoholInLast24h()) {
            reasons.add("Alcohol intake within the last 24 hours requires temporary deferral.");
            if (nextEligibleDate == null || nextEligibleDate.isBefore(screening.getCollectionDate().plusDays(1))) {
                nextEligibleDate = screening.getCollectionDate().plusDays(1);
            }
        }

        if (screening.isHasFeverCoughColds()) {
            reasons.add("Fever, cough, or colds require temporary deferral.");
            if (nextEligibleDate == null || nextEligibleDate.isBefore(screening.getCollectionDate().plusDays(7))) {
                nextEligibleDate = screening.getCollectionDate().plusDays(7);
            }
        }

        if (screening.isHadTattooOrPiercingLast12Months()) {
            reasons.add("Tattoo, piercing, acupuncture, or similar needle procedure within the last 12 months requires deferral.");
            if (nextEligibleDate == null || nextEligibleDate.isBefore(screening.getCollectionDate().plusMonths(12))) {
                nextEligibleDate = screening.getCollectionDate().plusMonths(12);
            }
        }

        if (screening.isHadRecentOperation()) {
            reasons.add("Recent medical operation requires physician clearance before donation.");
            if (nextEligibleDate == null || nextEligibleDate.isBefore(screening.getCollectionDate().plusMonths(6))) {
                nextEligibleDate = screening.getCollectionDate().plusMonths(6);
            }
        }

        if ("FEMALE".equalsIgnoreCase(donor.getSex()) && screening.isCurrentlyPregnant()) {
            reasons.add("Pregnant donors are deferred until 6 weeks after delivery.");
            if (nextEligibleDate == null) {
                nextEligibleDate = screening.getCollectionDate().plusMonths(6);
            }
        }

        boolean eligible = reasons.isEmpty();
        String message = eligible ? "Eligible for donation." : String.join(" ", reasons);
        if (nextEligibleDate != null && !eligible) {
            message += " Suggested re-screening date: " + nextEligibleDate + ".";
        }

        // Truncate message to fit VARCHAR(1000)
        if (message.length() > 1000) {
            message = message.substring(0, 997) + "...";
        }

        return new ScreeningDecision(eligible, message, nextEligibleDate);
    }

    private static class ExistingDonor {
        final String id;
        final LocalDate lastSuccessfulDonation;

        ExistingDonor(String id, LocalDate lastSuccessfulDonation) {
            this.id = id;
            this.lastSuccessfulDonation = lastSuccessfulDonation;
        }
    }

    private static class ScreeningDecision {
        final boolean eligible;
        final String message;
        final LocalDate nextEligibleDate;

        ScreeningDecision(boolean eligible, String message, LocalDate nextEligibleDate) {
            this.eligible = eligible;
            this.message = message;
            this.nextEligibleDate = nextEligibleDate;
        }
    }

    private ExistingDonor findExistingDonor(Connection conn, DohDonor donor) throws SQLException {
        String sql = "SELECT donor_id, last_successful_donation FROM donors " +
                "WHERE LOWER(first_name) = LOWER(?) AND LOWER(middle_name) = LOWER(?) AND LOWER(last_name) = LOWER(?) " +
                "AND birth_date = ? AND contact_no = ? " +
                "ORDER BY donor_id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donor.getFirstName());
            ps.setString(2, donor.getMiddleName() != null ? donor.getMiddleName() : "");
            ps.setString(3, donor.getLastName());
            ps.setDate(4, Date.valueOf(donor.getBirthdate()));
            ps.setString(5, donor.getContact());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date lastDonation = rs.getDate("last_successful_donation");
                    return new ExistingDonor(
                            rs.getString("donor_id"),
                            lastDonation == null ? null : lastDonation.toLocalDate()
                    );
                }
            }
        }
        return null;
    }

    private String insertDonor(Connection conn, DohDonor donor, LocalDate lastSuccessfulDonation) throws SQLException {
        String donorId = generateDonorId(conn);
        String sql = "INSERT INTO donors (donor_id, first_name, middle_name, last_name, sex, birth_date, blood_type, barangay, contact_no, external_card_id, id_source, last_successful_donation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donorId);
            ps.setString(2, donor.getFirstName());
            ps.setString(3, donor.getMiddleName());
            ps.setString(4, donor.getLastName());
            ps.setString(5, donor.getSex());
            ps.setDate(6, Date.valueOf(donor.getBirthdate()));
            ps.setString(7, donor.getBloodType());
            ps.setString(8, donor.getBarangay());
            ps.setString(9, donor.getContact());
            ps.setString(10, donor.getExternalCardId());
            ps.setString(11, donor.getExternalSource());
            if (lastSuccessfulDonation == null) {
                ps.setNull(12, java.sql.Types.DATE);
            } else {
                ps.setDate(12, Date.valueOf(lastSuccessfulDonation));
            }
            ps.executeUpdate();
            return donorId;
        }
    }

    private String generateDonorId(Connection conn) throws SQLException {
        String yearPrefix = String.valueOf(Year.now().getValue());
        String idPrefix = "NBDA-" + yearPrefix + "-";
        
        String sql = "INSERT INTO donor_id_counter (id_prefix, last_number) VALUES (?, 1) " +
                "ON DUPLICATE KEY UPDATE last_number = last_number + 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPrefix);
            ps.executeUpdate();
        }
        
        String selectSql = "SELECT last_number FROM donor_id_counter WHERE id_prefix = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, idPrefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int nextNum = rs.getInt("last_number");
                    return idPrefix + String.format("%05d", nextNum);
                }
            }
        }
        throw new SQLException("Unable to generate donor ID.");
    }

    private void updateDonor(Connection conn, String donorId, DohDonor donor, LocalDate lastSuccessfulDonation) throws SQLException {
        String sql = "UPDATE donors SET first_name = ?, middle_name = ?, last_name = ?, sex = ?, birth_date = ?, blood_type = ?, barangay = ?, " +
                "contact_no = ?, external_card_id = ?, id_source = ?, last_successful_donation = ? WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donor.getFirstName());
            ps.setString(2, donor.getMiddleName());
            ps.setString(3, donor.getLastName());
            ps.setString(4, donor.getSex());
            ps.setDate(5, Date.valueOf(donor.getBirthdate()));
            ps.setString(6, donor.getBloodType());
            ps.setString(7, donor.getBarangay());
            ps.setString(8, donor.getContact());
            ps.setString(9, donor.getExternalCardId());
            ps.setString(10, donor.getExternalSource());
            if (lastSuccessfulDonation == null) {
                ps.setNull(11, java.sql.Types.DATE);
            } else {
                ps.setDate(11, Date.valueOf(lastSuccessfulDonation));
            }
            ps.setString(12, donorId);
            ps.executeUpdate();
        }
    }

    private int insertScreening(Connection conn, String donorId, ScreeningInput screening, int screenedBy, ScreeningDecision decision, String authIdType) throws SQLException {
        String sql = "INSERT INTO donor_screening (" +
                "donor_id, auth_id_type, screened_by, screening_date, intended_collection_date, weight_kg, blood_pressure, systolic_bp, diastolic_bp, " +
                "pulse_bpm, temperature_c, hemoglobin_g_dl, slept_hours, guardian_consent_provided, had_meal, alcohol_in_last_24h, " +
                "has_fever_cough_colds, had_tattoo_or_piercing_last_12m, had_recent_operation, currently_pregnant, " +
                "screening_status, decision_reason, next_eligible_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, donorId);
            // Handle null auth_id_type - default to 'OTHER' if not provided
            ps.setString(2, (authIdType == null || authIdType.isBlank()) ? "OTHER" : authIdType);
            ps.setInt(3, screenedBy);
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            ps.setDate(5, Date.valueOf(screening.getCollectionDate()));
            ps.setDouble(6, screening.getWeightKg());
            ps.setString(7, screening.getBloodPressureDisplay());
            ps.setInt(8, screening.getSystolicBp());
            ps.setInt(9, screening.getDiastolicBp());
            ps.setInt(10, screening.getPulseBpm());
            ps.setDouble(11, screening.getTemperatureC());
            ps.setDouble(12, screening.getHemoglobinGdl());
            ps.setDouble(13, screening.getSleptHours());
            ps.setBoolean(14, screening.isGuardianConsentProvided());
            ps.setBoolean(15, screening.isHadMeal());
            ps.setBoolean(16, screening.isAlcoholInLast24h());
            ps.setBoolean(17, screening.isHasFeverCoughColds());
            ps.setBoolean(18, screening.isHadTattooOrPiercingLast12Months());
            ps.setBoolean(19, screening.isHadRecentOperation());
            ps.setBoolean(20, screening.isCurrentlyPregnant());
            ps.setString(21, decision.eligible ? "ELIGIBLE" : "TEMPORARILY_DEFERRED");
            ps.setString(22, decision.message);
            ps.setDate(23, decision.nextEligibleDate == null ? null : Date.valueOf(decision.nextEligibleDate));
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert screening record.");
    }

    private String nextBagId(Connection conn, LocalDate collectionDate) throws SQLException {
        String datePart = collectionDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "BAG-" + datePart + "-";
        
        String sql = "INSERT INTO bag_id_counter (id_prefix, last_number) VALUES (?, 1) " +
                "ON DUPLICATE KEY UPDATE last_number = last_number + 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix);
            ps.executeUpdate();
        }
        
        String selectSql = "SELECT last_number FROM bag_id_counter WHERE id_prefix = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int nextNum = rs.getInt("last_number");
                    return prefix + String.format("%03d", nextNum);
                }
            }
        }
        throw new SQLException("Unable to generate bag ID.");
    }

    private void insertBag(Connection conn, String bagId, String donorId, int screeningId, String bloodType, LocalDate collectionDate, int volumeMl) throws SQLException {
        LocalDate expiryDate = collectionDate.plusDays(35);
        String sql = "INSERT INTO blood_inventory (bag_id, donor_id, screening_id, blood_type, collection_date, expiry_date, " +
                "collection_volume_ml, inventory_status) VALUES (?, ?, ?, ?, ?, ?, ?, 'QUARANTINE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bagId);
            ps.setString(2, donorId);
            ps.setInt(3, screeningId);
            ps.setString(4, bloodType);
            ps.setDate(5, Date.valueOf(collectionDate));
            ps.setDate(6, Date.valueOf(expiryDate));
            ps.setInt(7, volumeMl);
            ps.executeUpdate();
        }
    }

    private void updateLastSuccessfulDonation(Connection conn, String donorId, LocalDate date) throws SQLException {
        String sql = "UPDATE donors SET last_successful_donation = ? WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, donorId);
            ps.executeUpdate();
        }
    }

    private LocalDate mostRecentDonation(LocalDate d1, LocalDate d2) {
        if (d1 == null && d2 == null) return null;
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return d1.isAfter(d2) ? d1 : d2;
    }

    private void insertAudit(Connection conn, int userId, String actionType, String entityType, String entityId, String details) throws SQLException {
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

    public DohDonor findDonorBySearch(String query) {
        String sql = "SELECT donor_id, first_name, middle_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
                "FROM donors " +
                "WHERE donor_id = ? OR LOWER(first_name) LIKE LOWER(?) OR LOWER(last_name) LIKE LOWER(?) OR contact_no LIKE ? " +
                "ORDER BY CASE WHEN donor_id = ? THEN 1 ELSE 2 END, donor_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query);
            String searchPattern = "%" + query + "%";
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);
            ps.setString(5, query);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DohDonor(
                            rs.getString("donor_id"),
                            rs.getString("first_name"),
                            rs.getString("middle_name") != null ? rs.getString("middle_name") : "",
                            rs.getString("last_name"),
                            rs.getString("sex"),
                            rs.getDate("birth_date") != null ? rs.getDate("birth_date").toLocalDate() : null,
                            rs.getString("blood_type"),
                            rs.getString("barangay"),
                            rs.getString("contact_no"),
                            rs.getDate("last_successful_donation") != null ? rs.getDate("last_successful_donation").toLocalDate() : null
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search donor.", e);
        }
        return null;
    }

    public int getTotalDonorCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM donors";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
