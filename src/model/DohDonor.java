package model;

import java.time.LocalDate;

// DOH-aligned donor record for the new workflow with External ID mapping.
public class DohDonor {
    private String id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String sex;
    private LocalDate birthdate;
    private String bloodType;
    private String barangay;
    private String contact;
    private LocalDate lastSuccessfulDonation;
    private String externalCardId;
    private String externalSource;

    public DohDonor() {}

    public DohDonor(String id, String firstName, String middleName,
                    String lastName, String sex, LocalDate birthdate, String bloodType,
                    String barangay, String contact, LocalDate lastSuccessfulDonation) {
        this.id = id;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.sex = sex;
        this.birthdate = birthdate;
        this.bloodType = bloodType;
        this.barangay = barangay;
        this.contact = contact;
        this.lastSuccessfulDonation = lastSuccessfulDonation;
    }

    public String getExternalCardId() {
        return externalCardId;
    }

    public void setExternalCardId(String externalCardId) {
        this.externalCardId = externalCardId;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDate getLastSuccessfulDonation() {
        return lastSuccessfulDonation;
    }

    public void setLastSuccessfulDonation(LocalDate lastSuccessfulDonation) {
        this.lastSuccessfulDonation = lastSuccessfulDonation;
    }
}