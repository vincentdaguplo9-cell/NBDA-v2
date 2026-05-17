package model;

import java.time.LocalDateTime;

// Required details before a bag can be issued to a patient or facility.
public class IssueRequestInput {
    private final String bagId;
    private final String patientName;
    private final String patientHospitalNo;
    private final String requestHospital;
    private final String requestingPhysician;
    private final String bloodRequestNo;
    private final String crossmatchStatus;
    private final String issueNotes;
    private final LocalDateTime issuedAt;
    private final int issuedBy;

    // Full constructor with all fields
    public IssueRequestInput(String bagId, String patientName, String patientHospitalNo, String requestHospital,
                               String requestingPhysician, String bloodRequestNo, String crossmatchStatus,
                               String issueNotes, LocalDateTime issuedAt, int issuedBy) {
        this.bagId = bagId;
        this.patientName = patientName;
        this.patientHospitalNo = patientHospitalNo;
        this.requestHospital = requestHospital;
        this.requestingPhysician = requestingPhysician;
        this.bloodRequestNo = bloodRequestNo;
        this.crossmatchStatus = crossmatchStatus;
        this.issueNotes = issueNotes;
        this.issuedAt = issuedAt;
        this.issuedBy = issuedBy;
    }

    // Secondary constructor without bagId, issuedAt and issuedBy (for dialog use)
    public IssueRequestInput(String patientName, String patientHospitalNo, String requestHospital,
                              String requestingPhysician, String bloodRequestNo, String crossmatchStatus,
                              String issueNotes) {
        this(null, patientName, patientHospitalNo, requestHospital, requestingPhysician, 
             bloodRequestNo, crossmatchStatus, issueNotes, null, 0);
    }

    public String getBagId() {
        return bagId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientHospitalNo() {
        return patientHospitalNo;
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

    public String getIssueNotes() {
        return issueNotes;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public int getIssuedBy() {
        return issuedBy;
    }
}
