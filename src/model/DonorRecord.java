package model;

import java.time.LocalDate;

// Donor masterlist projection for the records screen.
public class DonorRecord {
    private final int donorId;
    private final String firstName;
    private final String lastName;
    private final String sex;
    private final LocalDate birthDate;
    private final String bloodType;
    private final String barangay;
    private final String contactNo;
    private final LocalDate lastSuccessfulDonation;

    public DonorRecord(int donorId, String firstName, String lastName, String sex,
                       LocalDate birthDate, String bloodType, String barangay, String contactNo,
                       LocalDate lastSuccessfulDonation) {
        this.donorId = donorId;
        this.firstName = firstName;
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

    public String getFirstName() {
        return firstName;
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