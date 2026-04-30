package model;

import java.time.LocalDate;

// Inventory projection for DOH-compliant blood archive.
public class BloodBagRecord {
    private final String bagId;
    private final String donorId;
    private final String donorName;
    private final String barangay;
    private final String bloodType;
    private final LocalDate dateCollected;
    private final LocalDate dateExpiry;
    private final String inventoryStatus;
    private final String ttiStatus;

    public BloodBagRecord(String bagId, String donorId, String donorName, String barangay, String bloodType,
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

    public String getDonorId() {
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

    // Status getter - returns the effective status (considering expiry, TTI status, etc.)
    public String getStatus() {
        return getEffectiveStatus();
    }

    public String getEffectiveStatus() {
        // Priority: DISCARDED > REACTIVE (TTI) > EXPIRED > current status
        if ("DISCARDED".equalsIgnoreCase(inventoryStatus)) {
            return "DISCARDED";
        }
        if ("REACTIVE".equalsIgnoreCase(ttiStatus) && !"ISSUED".equalsIgnoreCase(inventoryStatus)) {
            return "REACTIVE";
        }
        if (dateExpiry != null && dateExpiry.isBefore(LocalDate.now()) 
                && !"ISSUED".equalsIgnoreCase(inventoryStatus)) {
            return "EXPIRED";
        }
        // Handle legacy status values
        if ("CLEARED".equalsIgnoreCase(inventoryStatus) || "AVAILABLE".equalsIgnoreCase(inventoryStatus)) {
            return "AVAILABLE";
        }
        return inventoryStatus != null ? inventoryStatus : "QUARANTINE";
    }
}
