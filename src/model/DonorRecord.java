package model;

import java.time.LocalDate;

// Donor masterlist projection with External ID mapping.
public class DonorRecord {
    private final String donorId;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String sex;
    private final LocalDate birthDate;
    private final String bloodType;
    private final String barangay;
    private final String contactNo;
    private final String externalCardId;
    private final String idSource;
    private final LocalDate lastSuccessfulDonation;

    public DonorRecord(String donorId,
                       String firstName, String middleName, String lastName, String sex,
                       LocalDate birthDate, String bloodType, String barangay, String contactNo,
                       String externalCardId, String idSource,
                       LocalDate lastSuccessfulDonation) {
        this.donorId = donorId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.sex = sex;
        this.birthDate = birthDate;
        this.bloodType = bloodType;
        this.barangay = barangay;
        this.contactNo = contactNo;
        this.externalCardId = externalCardId;
        this.idSource = idSource;
        this.lastSuccessfulDonation = lastSuccessfulDonation;
    }

    public String getDonorId() {
        return donorId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getSex() {
        return sex;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getBarangay() {
        return barangay;
    }

    public String getContactNo() {
        return contactNo;
    }

    public String getExternalCardId() {
        return externalCardId;
    }

    public String getIdSource() {
        return idSource;
    }

    public LocalDate getLastSuccessfulDonation() {
        return lastSuccessfulDonation;
    }

    public String getDisplayName() {
        StringBuilder out = new StringBuilder(firstName == null ? "" : firstName);
        if (middleName != null && !middleName.isBlank()) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(middleName);
        }
        if (lastName != null && !lastName.isBlank()) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(lastName);
        }
        return out.toString().trim();
    }
}