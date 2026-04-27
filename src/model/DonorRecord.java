package model;

import java.time.LocalDate;

// Donor masterlist projection with External ID mapping.
public class DonorRecord {
    private final int donorId;
    private final String externalCardId;
    private final String externalSource;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String sex;
    private final LocalDate birthDate;
    private final String bloodType;
    private final String barangay;
    private final String contactNo;
    private final LocalDate lastSuccessfulDonation;

    public DonorRecord(int donorId, String externalCardId, String externalSource,
                       String firstName, String middleName, String lastName, String sex,
                       LocalDate birthDate, String bloodType, String barangay, String contactNo,
                       LocalDate lastSuccessfulDonation) {
        this.donorId = donorId;
        this.externalCardId = externalCardId;
        this.externalSource = externalSource;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.sex = sex;
        this.birthDate = birthDate;
        this.bloodType = bloodType;
        this.barangay = barangay;
        this.contactNo = contactNo;
        this.lastSuccessfulDonation = lastSuccessfulDonation;
    }

    public int getDonorId() {
        return donorId;
    }

    public String getExternalCardId() {
        return externalCardId;
    }

    public String getExternalSource() {
        return externalSource;
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

    public LocalDate getLastSuccessfulDonation() {
        return lastSuccessfulDonation;
    }

    public String getDisplayName() {
        return (firstName + " " + lastName).trim();
    }
}