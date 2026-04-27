package model;

import java.time.LocalDate;

// Inventory projection for DOH-compliant blood archive.
public class BloodBagRecord {
    private final String bagId;
    private final Integer donorId;
    private final String donorName;
    private final String barangay;
    private final String bloodType;
    private final LocalDate dateCollected;
    private final LocalDate dateExpiry;
    private final String inventoryStatus;
    private final String ttiStatus;

    public BloodBagRecord(String bagId, Integer donorId, String donorName, String barangay, String bloodType,
                          LocalDate dateCollected, LocalDate dateExpiry, String inventoryStatus, String ttiStatus) {
        this.bagId = bagId;
        this.donorId = donorId;
        this.donorName = donorName;
        this.barangay = barangay;
        this.bloodType = bloodType;
        this.dateCollected = dateCollected;
        this.dateExpiry = dateExpiry;
        this.inventoryStatus = inventoryStatus;
        this.ttiStatus = ttiStatus;
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

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public String getTtiStatus() {
        return ttiStatus;
    }

    // Status getter for backward compatibility
    public String getStatus() {
        return inventoryStatus;
    }

    public String getEffectiveStatus() {
        if (dateExpiry != null && dateExpiry.isBefore(LocalDate.now()) && !"ISSUED".equalsIgnoreCase(inventoryStatus)
                && !"DISCARDED".equalsIgnoreCase(inventoryStatus)) {
            return "EXPIRED";
        }
        return inventoryStatus;
    }
}
