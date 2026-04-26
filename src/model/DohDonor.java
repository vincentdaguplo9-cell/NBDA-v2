package model;

import java.time.LocalDate;

// DOH-aligned donor record used by the new FXML workflow.
public class DohDonor {
    private Integer id;
    private String firstName;
    private String lastName;
    private String sex;
    private LocalDate birthdate;
    private String bloodType;
    private String barangay;
    private String contact;
    private LocalDate lastSuccessfulDonation;

    public DohDonor(Integer id, String firstName, String lastName, String sex, LocalDate birthdate, String bloodType,
                    String barangay, String contact, LocalDate lastSuccessfulDonation) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = sex;
        this.birthdate = birthdate;
        this.bloodType = bloodType;
        this.barangay = barangay;
        this.contact = contact;
        this.lastSuccessfulDonation = lastSuccessfulDonation;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        StringBuilder out = new StringBuilder(firstName == null ? "" : firstName);
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

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getBarangay() {
        return barangay;
    }

    public String getContact() {
        return contact;
    }

    public LocalDate getLastSuccessfulDonation() {
        return lastSuccessfulDonation;
    }

    public void setLastSuccessfulDonation(LocalDate lastSuccessfulDonation) {
        this.lastSuccessfulDonation = lastSuccessfulDonation;
    }
}