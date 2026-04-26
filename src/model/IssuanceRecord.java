package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Issued bag traceability row for hospital release logs.
public class IssuanceRecord {
    private final String bagId;
    private final String donorName;
    private final String bloodType;
    private final LocalDate collectionDate;
    private final LocalDateTime issuedAt;
    private final String patientName;
    private final String requestHospital;
    private final String requestingPhysician;
    private final String bloodRequestNo;
    private final String crossmatchStatus;
    private final String issuedBy;

    public IssuanceRecord(String bagId, String donorName, String bloodType, LocalDate collectionDate,
                          LocalDateTime issuedAt, String patientName, String requestHospital,
                          String requestingPhysician, String bloodRequestNo, String crossmatchStatus, String issuedBy) {
        this.bagId = bagId;
        this.donorName = donorName;
        this.bloodType = bloodType;
        this.collectionDate = collectionDate;
        this.issuedAt = issuedAt;
        this.patientName = patientName;
        this.requestHospital = requestHospital;
        this.requestingPhysician = requestingPhysician;
        this.bloodRequestNo = bloodRequestNo;
        this.crossmatchStatus = crossmatchStatus;
        this.issuedBy = issuedBy;
    }

    public String getBagId() {
        return bagId;
    }

    public String getDonorName() {
        return donorName;
    }

    public String getBloodType() {
        return bloodType;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getRequestHospital() {
        return requestHospital;
    }

    public String getRequestingPhysician() {
        return requestingPhysician;
    }

    public String getBloodRequestNo() {
        return bloodRequestNo;
    }

    public String getCrossmatchStatus() {
        return crossmatchStatus;
    }

    public String getIssuedBy() {
        return issuedBy;
    }
}
