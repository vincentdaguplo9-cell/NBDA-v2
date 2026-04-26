package model;

// Required details before a bag can be issued to a patient or facility.
public class IssueRequestInput {
    private final String patientName;
    private final String patientHospitalNo;
    private final String requestHospital;
    private final String requestingPhysician;
    private final String bloodRequestNo;
    private final String crossmatchStatus;
    private final String issueNotes;

    public IssueRequestInput(String patientName, String patientHospitalNo, String requestHospital,
                             String requestingPhysician, String bloodRequestNo, String crossmatchStatus,
                             String issueNotes) {
        this.patientName = patientName;
        this.patientHospitalNo = patientHospitalNo;
        this.requestHospital = requestHospital;
        this.requestingPhysician = requestingPhysician;
        this.bloodRequestNo = bloodRequestNo;
        this.crossmatchStatus = crossmatchStatus;
        this.issueNotes = issueNotes;
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
}
