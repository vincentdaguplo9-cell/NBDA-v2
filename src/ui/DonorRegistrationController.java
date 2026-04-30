package ui;

import dao.DohDonorDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import model.DohDonor;
import model.RegistrationResult;
import model.ScreeningInput;
import model.UserAccount;
import ui.UserSession;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DonorRegistrationController {
    private static final List<String> BARANGAYS = Arrays.asList(
            "Agpangi", "Anislagan", "Atipolo", "Bato", "Borac", "Cabungaan", "Calumpang", "Catmon",
            "Haguikhikan", "Padre Inocentes Garcia", "Larrazabal", "Lico", "Lucsoon", "Mabini",
            "Mocpong", "P.I. Garcia", "Santissimo Rosario", "Santo Nino", "Talustusan", "Villa Caneja"
    );
    private static final List<String> ID_TYPES = Arrays.asList(
            "National ID", "Driver's License", "Passport", "PhilHealth", "Student ID", "Other"
    );
    private static final Pattern BP_PATTERN = Pattern.compile("^(\\d{2,3})\\s*/\\s*(\\d{2,3})$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    private final DohDonorDAO donorDAO = new DohDonorDAO();

    @FXML private TextField firstNameField;
    @FXML private TextField middleNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> sexCombo;
    @FXML private DatePicker birthdatePicker;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private ComboBox<String> barangayCombo;
    @FXML private TextField contactField;
    @FXML private TextField weightField;
    @FXML private TextField bpField;
    @FXML private TextField pulseField;
    @FXML private TextField hemoglobinField;
    @FXML private CheckBox guardianConsentCheckBox;
    @FXML private CheckBox hadMealCheckBox;
    @FXML private CheckBox alcoholCheckBox;
    @FXML private CheckBox feverCheckBox;
    @FXML private CheckBox tattooCheckBox;
    @FXML private CheckBox operationCheckBox;
    @FXML private Label resultLabel;
    @FXML private Label eligibilityStatusLabel;
    @FXML private Button saveButton;

    @FXML private DatePicker lastDonationPicker;
    @FXML private ToggleButton firstTimeDonorToggle;
    @FXML private ToggleButton returningDonorToggle;
    @FXML private ToggleGroup donorTypeGroup;
    @FXML private DatePicker collectionDatePicker;
    @FXML private ComboBox<String> authIdTypeCombo;
    @FXML private TextField temperatureField;
    @FXML private TextField volumeField;
    @FXML private Button bpPresetBtn;
    @FXML private Button volumePresetBtn;
    @FXML private Label waitTimeValueLabel;
    @FXML private Label waitTimeHintLabel;
    @FXML private Label ageLabel;
    @FXML private TextField sleptHoursField;

    @FXML
    public void initialize() {
        bloodTypeCombo.setItems(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        sexCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE"));
        barangayCombo.setItems(FXCollections.observableArrayList(BARANGAYS));
        authIdTypeCombo.setItems(FXCollections.observableArrayList(ID_TYPES));

        collectionDatePicker.setValue(LocalDate.now());

        donorTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                returningDonorToggle.setSelected(true);
            }
            toggleFirstTimeDonor();
        });

        setupInputRestrictions();

        saveButton.setGraphic(IconFactory.createIcon("USER_PLUS", "+", "#38BDF8", 15));
        saveButton.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);

        bpPresetBtn.setOnAction(e -> bpField.setText("120/80"));
        volumePresetBtn.setOnAction(e -> volumeField.setText("450"));

        birthdatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateAgeLabel());

        clearForm();
    }

    private void updateAgeLabel() {
        if (birthdatePicker.getValue() != null) {
            int age = Period.between(birthdatePicker.getValue(), LocalDate.now()).getYears();
            ageLabel.setText(String.valueOf(age));
        } else {
            ageLabel.setText("");
        }
    }

    private void setupInputRestrictions() {
        firstNameField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToLetters(e, firstNameField));
        lastNameField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToLetters(e, lastNameField));
        contactField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToContact(e, contactField));
        weightField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, weightField));
        bpField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToBP(e, bpField));
        pulseField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, pulseField));
        hemoglobinField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToDecimal(e, hemoglobinField));
    }

    @FXML
    private void toggleFirstTimeDonor() {
        boolean isFirstTime = firstTimeDonorToggle.isSelected();
        lastDonationPicker.setDisable(isFirstTime);
        if (isFirstTime) {
            lastDonationPicker.setValue(null);
        }
    }

    @FXML
    private void clearForm() {
        firstNameField.clear();
        middleNameField.clear();
        lastNameField.clear();
        sexCombo.getSelectionModel().clearSelection();
        birthdatePicker.setValue(null);
        bloodTypeCombo.getSelectionModel().clearSelection();
        barangayCombo.getSelectionModel().clearSelection();
        contactField.clear();
        weightField.clear();
        bpField.clear();
        pulseField.clear();
        hemoglobinField.clear();
        temperatureField.clear();
        volumeField.clear();
        sleptHoursField.clear();
        collectionDatePicker.setValue(LocalDate.now());
        authIdTypeCombo.getSelectionModel().clearSelection();
        lastDonationPicker.setValue(null);
        returningDonorToggle.setSelected(true);
        lastDonationPicker.setDisable(false);
        guardianConsentCheckBox.setSelected(false);
        hadMealCheckBox.setSelected(false);
        alcoholCheckBox.setSelected(false);
        feverCheckBox.setSelected(false);
        tattooCheckBox.setSelected(false);
        operationCheckBox.setSelected(false);
        resultLabel.setText("Awaiting validation...");
        eligibilityStatusLabel.setText("Verify");
        ageLabel.setText("");
        waitTimeValueLabel.setText("Ready");
        waitTimeHintLabel.setText("Eligible to donate");
    }

    @FXML
    private void saveDonor() {
        try {
            String firstName = firstNameField.getText();
            String lastName = lastNameField.getText();
            String sex = sexCombo.getValue();
            LocalDate birthdate = birthdatePicker.getValue();
            String bloodType = bloodTypeCombo.getValue();
            String barangay = barangayCombo.getValue();
            String contactNo = contactField.getText();
            LocalDate collectionDate = collectionDatePicker.getValue();
            String idType = authIdTypeCombo.getValue();

            if (firstName == null || firstName.isBlank() ||
                lastName == null || lastName.isBlank() ||
                sex == null ||
                birthdate == null ||
                bloodType == null ||
                barangay == null ||
                contactNo == null || contactNo.isBlank() ||
                collectionDate == null ||
                idType == null || idType.isBlank()) {
                resultLabel.setText("Please complete all required fields (*).");
                setEligibilityStatus("Incomplete", "status-pill");
                return;
            }

            Double weight = parseDouble(weightField.getText(), "Weight must be a valid number.");
            Integer pulse = parseInteger(pulseField.getText(), "Pulse must be a valid number.");
            Double hemoglobin = parseDouble(hemoglobinField.getText(), "Hemoglobin must be a valid number.");
            Double temperature = parseDouble(temperatureField.getText(), "Temperature must be a valid number.");

            if (weight == null || pulse == null || hemoglobin == null || temperature == null) {
                return;
            }

            BloodPressure bp = parseBloodPressure(bpField.getText());
            if (bp == null) {
                showValidation("Blood pressure must be in format: systolic/diastolic (e.g., 120/80)");
                return;
            }

            boolean isFirstTime = firstTimeDonorToggle.isSelected();

            DohDonor donor = new DohDonor();
            donor.setFirstName(firstName);
            donor.setMiddleName(middleNameField.getText());
            donor.setLastName(lastName);
            donor.setSex(sex);
            donor.setBirthdate(birthdate);
            donor.setBloodType(bloodType);
            donor.setBarangay(barangay);
            donor.setContact(contactNo);
            donor.setLastSuccessfulDonation(lastDonationPicker.getValue());

            ScreeningInput screening = new ScreeningInput(
                    collectionDate,
                    volumeField.getText().isEmpty() ? 450 : Integer.parseInt(volumeField.getText()),
                    weight,
                    bp.systolic,
                    bp.diastolic,
                    pulse,
                    temperature,
                    hemoglobin,
                    sleptHoursField.getText().isEmpty() ? 8.0 : Double.parseDouble(sleptHoursField.getText()),
                    guardianConsentCheckBox.isSelected(),
                    hadMealCheckBox.isSelected(),
                    !alcoholCheckBox.isSelected(),
                    !feverCheckBox.isSelected(),
                    !tattooCheckBox.isSelected(),
                    !operationCheckBox.isSelected(),
                    false,
                    idType,
                    isFirstTime
            );

            UserAccount currentUser = UserSession.getCurrentUser();
            if (currentUser == null) {
                resultLabel.setText("No user logged in. Please restart the application.");
                return;
            }

            RegistrationResult result = donorDAO.registerDonation(donor, screening, currentUser.getUserId());

            if (result.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Registration Successful", result.getMessage());
                setEligibilityStatus("Accepted", "status-pill");
                clearForm();
            } else if (result.isDeferred()) {
                showAlert(Alert.AlertType.WARNING, "Donor Deferred", result.getMessage());
                setEligibilityStatus("Deferred", "status-pill");
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", result.getMessage());
                setEligibilityStatus("Error", "status-pill");
            }

        } catch (Exception e) {
            resultLabel.setText("Error: " + e.getMessage());
            setEligibilityStatus("Error", "status-pill");
            e.printStackTrace();
        }
    }

    private void setEligibilityStatus(String label, String styleClass) {
        eligibilityStatusLabel.setText(label);
        eligibilityStatusLabel.getStyleClass().removeAll("status-pill");
        eligibilityStatusLabel.getStyleClass().add(styleClass);
    }

    private void restrictToLetters(KeyEvent e, TextField field) {
        String text = e.getCharacter();
        if (!text.matches("[a-zA-Z\\s\\-']")) {
            e.consume();
        }
    }

    private void restrictToNumbers(KeyEvent e, TextField field) {
        String text = e.getCharacter();
        if (!text.matches("\\d")) {
            e.consume();
        }
    }

    private void restrictToContact(KeyEvent e, TextField field) {
        String text = e.getCharacter();
        String current = field.getText();
        if (!text.matches("\\d") || current.length() >= 11) {
            e.consume();
        }
    }

    private void restrictToBP(KeyEvent e, TextField field) {
        String text = e.getCharacter();
        String current = field.getText();
        if (!text.matches("[\\d/]") || current.length() > 7) {
            e.consume();
        }
    }

    private void restrictToDecimal(KeyEvent e, TextField field) {
        String text = e.getCharacter();
        String current = field.getText();
        if (text.equals(".")) {
            if (current.contains(".")) {
                e.consume();
            }
        } else if (!text.matches("\\d")) {
            e.consume();
        }
    }

    private Double parseDouble(String raw, String errorMessage) {
        try {
            return Double.parseDouble(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ex) {
            showValidation(errorMessage);
            return null;
        }
    }

    private Integer parseInteger(String raw, String errorMessage) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ex) {
            showValidation(errorMessage);
            return null;
        }
    }

    private BloodPressure parseBloodPressure(String raw) {
        String text = raw == null ? "" : raw.trim();
        Matcher matcher = BP_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        return new BloodPressure(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private void showValidation(String message) {
        resultLabel.setText(message);
        setEligibilityStatus("Review required", "status-pill");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (saveButton != null && saveButton.getScene() != null && saveButton.getScene().getWindow() != null) {
            alert.initOwner(saveButton.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private static class BloodPressure {
        private final int systolic;
        private final int diastolic;

        private BloodPressure(int systolic, int diastolic) {
            this.systolic = systolic;
            this.diastolic = diastolic;
        }
    }
}
