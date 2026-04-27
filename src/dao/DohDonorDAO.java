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
        if (donor == null || screening == null || screening.getCollectionDate() == null) {
            return RegistrationResult.error("Missing donor details or screening data.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ExistingDonor existingDonor = findExistingDonor(conn, donor);
                LocalDate previousDonation = mostRecentDonation(
                        existingDonor == null ? null : existingDonor.lastSuccessfulDonation,
                        donor.getLastSuccessfulDonation()
                );

                int donorId;
                if (existingDonor == null) {
                    donorId = insertDonor(conn, donor, previousDonation);
                } else {
                    donorId = existingDonor.id;
                    updateDonor(conn, donorId, donor, previousDonation);
                }

                ScreeningDecision decision = evaluateEligibility(donor, screening, previousDonation);
                int screeningId = insertScreening(conn, donorId, screening, screenedBy, decision);

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

        String externalId = donor.getExternalCardId();
        String externalSource = donor.getExternalSource();
        if (externalId == null || externalId.isBlank() || "NONE".equals(externalSource)) {
            reasons.add("Valid PRC/DOH ID required - please register with external card ID.");
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
        } else if (age > 60 && previousDonation == null) {
            reasons.add("Donors aged 61 to 65 require prior successful donation history as regular donors.");
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
            reasons.add("Pulse rate must be within the acceptable donor range.");
        }

        if (previousDonation != null) {
            LocalDate nextAllowedDate = previousDonation.plusDays(90);
            if (nextAllowedDate.isAfter(screening.getCollectionDate())) {
                reasons.add("Whole blood donation requires a 3-month interval. Earliest next donation date is " + nextAllowedDate + ".");
                nextEligibleDate = laterDate(nextEligibleDate, nextAllowedDate);
            }
        }

        if (!screening.isHadMeal()) {
            reasons.add("Donor must have eaten before donation.");
        }

        if (screening.getSleptHours() < 8.0) {
            reasons.add("Donor should have at least 8 hours of sleep and rest before donation.");
        }

        if (screening.isAlcoholInLast24h()) {
            reasons.add("Alcohol intake within the last 24 hours requires temporary deferral.");
        }

        if (screening.isHasFeverCoughColds()) {
            reasons.add("Fever, cough, or colds require temporary deferral.");
        }

        if (screening.isHadTattooOrPiercingLast12Months()) {
            reasons.add("Tattoo, piercing, acupuncture, or similar needle procedure within the last 12 months requires deferral.");
            nextEligibleDate = laterDate(nextEligibleDate, screening.getCollectionDate().plusYears(1));
        }

        if (screening.isHadRecentOperation()) {
            reasons.add("Recent medical operation requires physician clearance before donation.");
        }

        if ("FEMALE".equalsIgnoreCase(donor.getSex()) && screening.isCurrentlyPregnant()) {
            reasons.add("Pregnancy requires temporary deferral.");
        }

        if (reasons.isEmpty()) {
            return ScreeningDecision.eligible(
                    "Passed age, weight, donation interval, and readiness screening.",
                    screening.getCollectionDate().plusDays(90)
            );
        }

        StringBuilder message = new StringBuilder("Donor deferred: ");
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) {
                message.append(' ');
            }
            message.append(reasons.get(i));
        }
        if (nextEligibleDate != null) {
            message.append(" Suggested re-screening date: ").append(nextEligibleDate).append('.');
        }
        return ScreeningDecision.deferred(message.toString(), nextEligibleDate);
    }

    private ExistingDonor findExistingDonor(Connection conn, DohDonor donor) throws SQLException {
        String sql = "SELECT donor_id, last_successful_donation FROM donors " +
                "WHERE LOWER(first_name) = LOWER(?) AND LOWER(last_name) = LOWER(?) " +
                "AND birth_date = ? AND contact_no = ? " +
                "ORDER BY donor_id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donor.getFirstName());
            ps.setString(2, donor.getLastName());
            ps.setDate(3, Date.valueOf(donor.getBirthdate()));
            ps.setString(4, donor.getContact());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date lastDonation = rs.getDate("last_successful_donation");
                    return new ExistingDonor(
                            rs.getInt("donor_id"),
                            lastDonation == null ? null : lastDonation.toLocalDate()
                    );
                }
            }
        }
        return null;
    }

    public DohDonor findDonorBySearch(String query) {
        String sql = "SELECT donor_id, first_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation " +
                "FROM donors " +
                "WHERE LOWER(first_name) LIKE LOWER(?) OR LOWER(last_name) LIKE LOWER(?) OR contact_no LIKE ? " +
                "ORDER BY donor_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DohDonor(
                            rs.getInt("donor_id"),
                            rs.getString("external_card_id"),
                            rs.getString("external_source"),
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

    private int insertDonor(Connection conn, DohDonor donor, LocalDate lastSuccessfulDonation) throws SQLException {
        String sql = "INSERT INTO donors (external_card_id, external_source, first_name, middle_name, last_name, sex, birth_date, blood_type, barangay, contact_no, last_successful_donation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, donor.getExternalCardId());
            ps.setString(2, donor.getExternalSource());
            ps.setString(3, donor.getFirstName());
            ps.setString(4, donor.getMiddleName());
            ps.setString(5, donor.getLastName());
            ps.setString(6, donor.getSex());
            ps.setDate(7, Date.valueOf(donor.getBirthdate()));
            ps.setString(8, donor.getBloodType());
            ps.setString(9, donor.getBarangay());
            ps.setString(10, donor.getContact());
            ps.setDate(11, lastSuccessfulDonation != null ? Date.valueOf(lastSuccessfulDonation) : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Unable to create donor record.");
    }

    private void updateDonor(Connection conn, int donorId, DohDonor donor, LocalDate lastSuccessfulDonation) throws SQLException {
        String sql = "UPDATE donors SET first_name = ?, last_name = ?, sex = ?, birth_date = ?, blood_type = ?, barangay = ?, " +
                "contact_no = ?, last_successful_donation = ? WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donor.getFirstName());
            ps.setString(2, donor.getLastName());
            ps.setString(3, donor.getSex());
            ps.setDate(4, Date.valueOf(donor.getBirthdate()));
            ps.setString(5, donor.getBloodType());
            ps.setString(6, donor.getBarangay());
            ps.setString(7, donor.getContact());
            ps.setDate(8, lastSuccessfulDonation == null ? null : Date.valueOf(lastSuccessfulDonation));
            ps.setInt(9, donorId);
            ps.executeUpdate();
        }
    }

    private int insertScreening(Connection conn, int donorId, ScreeningInput screening, int screenedBy,
                                ScreeningDecision decision) throws SQLException {
        String sql = "INSERT INTO donor_screening (" +
                "donor_id, screened_by, screening_date, intended_collection_date, weight_kg, blood_pressure, systolic_bp, diastolic_bp, " +
                "pulse_bpm, temperature_c, hemoglobin_g_dl, slept_hours, guardian_consent_provided, had_meal, alcohol_in_last_24h, " +
                "has_fever_cough_colds, had_tattoo_or_piercing_last_12m, had_recent_operation, currently_pregnant, " +
                "screening_status, decision_reason, next_eligible_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, donorId);
            ps.setInt(2, screenedBy);
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.setDate(4, Date.valueOf(screening.getCollectionDate()));
            ps.setDouble(5, screening.getWeightKg());
            ps.setString(6, screening.getBloodPressureDisplay());
            ps.setInt(7, screening.getSystolicBp());
            ps.setInt(8, screening.getDiastolicBp());
            ps.setInt(9, screening.getPulseBpm());
            ps.setDouble(10, screening.getTemperatureC());
            ps.setDouble(11, screening.getHemoglobinGdl());
            ps.setDouble(12, screening.getSleptHours());
            ps.setBoolean(13, screening.isGuardianConsentProvided());
            ps.setBoolean(14, screening.isHadMeal());
            ps.setBoolean(15, screening.isAlcoholInLast24h());
            ps.setBoolean(16, screening.isHasFeverCoughColds());
            ps.setBoolean(17, screening.isHadTattooOrPiercingLast12Months());
            ps.setBoolean(18, screening.isHadRecentOperation());
            ps.setBoolean(19, screening.isCurrentlyPregnant());
            ps.setString(20, decision.status);
            ps.setString(21, decision.message);
            ps.setDate(22, decision.nextEligibleDate == null ? null : Date.valueOf(decision.nextEligibleDate));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Unable to store donor screening.");
    }

    private void insertBag(Connection conn, String bagId, int donorId, int screeningId, String bloodType, LocalDate collectionDate, int volumeMl)
            throws SQLException {
        String sql = "INSERT INTO blood_inventory (" +
                "bag_id, donor_id, screening_id, blood_type, component, collection_volume_ml, collection_date, expiry_date, " +
                "tti_hiv, tti_hbv, tti_hcv, tti_syphilis, tti_malaria, tti_overall_status, inventory_status) " +
                "VALUES (?, ?, ?, ?, 'WHOLE_BLOOD', ?, ?, ?, 'PENDING', 'PENDING', 'PENDING', 'PENDING', 'PENDING', 'PENDING', 'QUARANTINE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bagId);
            ps.setInt(2, donorId);
            ps.setInt(3, screeningId);
            ps.setString(4, bloodType);
            ps.setInt(5, volumeMl);
            ps.setDate(6, Date.valueOf(collectionDate));
            ps.setDate(7, Date.valueOf(collectionDate.plusDays(35)));
            ps.executeUpdate();
        }
    }

    private void updateLastSuccessfulDonation(Connection conn, int donorId, LocalDate collectionDate) throws SQLException {
        String sql = "UPDATE donors SET last_successful_donation = ? WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(collectionDate));
            ps.setInt(2, donorId);
            ps.executeUpdate();
        }
    }

    private String nextBagId(Connection conn, LocalDate collectionDate) throws SQLException {
        String yearCode = String.format("%02d", collectionDate.getYear() % 100);
        String prefix = FACILITY_CODE + "-" + yearCode + "-";
        String sql = "SELECT bag_id FROM blood_inventory WHERE bag_id LIKE ? ORDER BY bag_id DESC LIMIT 1";
        int nextSequence = 1;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String bagId = rs.getString("bag_id");
                    String[] parts = bagId.split("-");
                    if (parts.length == 3) {
                        nextSequence = Integer.parseInt(parts[2]) + 1;
                    }
                }
            }
        }
        return prefix + String.format("%06d", nextSequence);
    }

    private LocalDate mostRecentDonation(LocalDate databaseDate, LocalDate formDate) {
        if (databaseDate == null) {
            return formDate;
        }
        if (formDate == null || databaseDate.isAfter(formDate)) {
            return databaseDate;
        }
        return formDate;
    }

    private LocalDate laterDate(LocalDate current, LocalDate candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
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

    public int getTotalDonorCount() {
        String sql = "SELECT COUNT(*) FROM donors";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count donors.", e);
        }
        return 0;
    }

    private static class ExistingDonor {
        private final int id;
        private final LocalDate lastSuccessfulDonation;

        private ExistingDonor(int id, LocalDate lastSuccessfulDonation) {
            this.id = id;
            this.lastSuccessfulDonation = lastSuccessfulDonation;
        }
    }

    private static class ScreeningDecision {
        private final boolean eligible;
        private final String status;
        private final String message;
        private final LocalDate nextEligibleDate;

        private ScreeningDecision(boolean eligible, String status, String message, LocalDate nextEligibleDate) {
            this.eligible = eligible;
            this.status = status;
            this.message = message;
            this.nextEligibleDate = nextEligibleDate;
        }

        private static ScreeningDecision eligible(String message, LocalDate nextEligibleDate) {
            return new ScreeningDecision(true, "ELIGIBLE", message, nextEligibleDate);
        }

        private static ScreeningDecision deferred(String message, LocalDate nextEligibleDate) {
            return new ScreeningDecision(false, "TEMPORARILY_DEFERRED", message, nextEligibleDate);
        }
    }
}
