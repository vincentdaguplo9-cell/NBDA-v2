package model;

import java.time.LocalDate;

// Inventory projection for the new DOH-compliant blood archive view.
public class BloodBagRecord {
    private final String bagId;
    private final Integer donorId;
    private final String donorName;
    private final String barangay;
    private final String bloodType;
    private final LocalDate dateCollected;
    private final LocalDate dateExpiry;
    private final String ttiStatus;
    private final String status;

    public BloodBagRecord(String bagId, Integer donorId, String donorName, String barangay, String bloodType,
                          LocalDate dateCollected, LocalDate dateExpiry, String ttiStatus, String status) {
        this.bagId = bagId;
        this.donorId = donorId;
        this.donorName = donorName;
        this.barangay = barangay;
        this.bloodType = bloodType;
        this.dateCollected = dateCollected;
        this.dateExpiry = dateExpiry;
        this.ttiStatus = ttiStatus;
        this.status = status;
    }

    public String getBagId() {
        return bagId;
    }

    public Integer getDonorId() {
        return donorId;
    }

    public String getDonorName() {
        return donorName;
    }

    public String getBarangay() {
        return barangay;
    }

    public String getBloodType() {
        return bloodType;
    }

    public LocalDate getDateCollected() {
        return dateCollected;
    }

    public LocalDate getDateExpiry() {
        return dateExpiry;
    }

    public String getTtiStatus() {
        return ttiStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getEffectiveStatus() {
        if (dateExpiry != null && dateExpiry.isBefore(LocalDate.now()) && !"ISSUED".equalsIgnoreCase(status)
                && !"DISCARDED".equalsIgnoreCase(status)) {
            return "EXPIRED";
        }
        return status;
    }
}
