package ui;

import dao.DohDonorDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import model.DohDonor;
import model.RegistrationResult;
import model.ScreeningInput;
import model.UserAccount;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Donor registration + screening controller using Philippine public donor-readiness rules.
public class DonorRegistrationController {
    private static final List<String> BARANGAYS = Arrays.asList(
            "Agpangi", "Anislagan", "Atipolo", "Bato", "Borac", "Cabungaan", "Calumpang", "Catmon",
            "Haguikhikan", "Padre Inocentes Garcia", "Larrazabal", "Lico", "Lucsoon", "Mabini",
            "Mocpong", "P.I. Garcia", "Santissimo Rosario", "Santo Nino", "Talustusan", "Villa Caneja"
    );
    private static final Pattern BP_PATTERN = Pattern.compile("^(\\d{2,3})\\s*/\\s*(\\d{2,3})$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']+$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d*$");
    private static final Pattern CONTACT_PATTERN = Pattern.compile("^09\\d*$");

    private final DohDonorDAO donorDAO = new DohDonorDAO();

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> sexCombo;
    @FXML private DatePicker birthdatePicker;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private ComboBox<String> barangayCombo;
    @FXML private TextField contactField;
    @FXML private DatePicker lastDonationPicker;
    @FXML private DatePicker collectionDatePicker;
    @FXML private TextField volumeField;
    @FXML private TextField weightField;
    @FXML private TextField bpField;
    @FXML private TextField pulseField;
    @FXML private TextField temperatureField;
    @FXML private TextField hemoglobinField;
    @FXML private TextField sleptHoursField;
    @FXML private Label waitTimeValueLabel;
    @FXML private Label waitTimeHintLabel;
    @FXML private Label eligibilityStatusLabel;
    @FXML private CheckBox guardianConsentCheckBox;
    @FXML private CheckBox hadMealCheckBox;
    @FXML private CheckBox alcoholCheckBox;
    @FXML private CheckBox feverCheckBox;
    @FXML private CheckBox tattooCheckBox;
    @FXML private CheckBox operationCheckBox;
    @FXML private CheckBox pregnancyCheckBox;
    @FXML private Label resultLabel;
    @FXML private Button saveButton;
    @FXML private Label ageLabel;
    @FXML private Button volumePresetBtn;
    @FXML private Button bpPresetBtn;
    @FXML private TextField searchDonorField;
    @FXML private Button searchDonorBtn;

    @FXML
    public void initialize() {
        bloodTypeCombo.setItems(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        sexCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE"));
        barangayCombo.setItems(FXCollections.observableArrayList(BARANGAYS));
        collectionDatePicker.setValue(LocalDate.now());

        setupInputRestrictions();
        setupQuickFillButtons();
        setupAgeCalculation();

        saveButton.setGraphic(IconFactory.createIcon("USER_PLUS", "+", "#38BDF8", 15));
        saveButton.setContentDisplay(ContentDisplay.LEFT);

        if (pregnancyCheckBox != null) {
            pregnancyCheckBox.disableProperty().bind(sexCombo.valueProperty().isNotEqualTo("FEMALE"));
            sexCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (!"FEMALE".equalsIgnoreCase(newValue)) {
            if (pregnancyCheckBox != null) pregnancyCheckBox.setSelected(false);
                }
            });
        }

        lastDonationPicker.valueProperty().addListener((obs, oldValue, newValue) -> updateWaitTimeCalculation());
        collectionDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateWaitTimeCalculation());

        guardianConsentCheckBox.setSelected(false);
        resultLabel.setText("Complete the intake fields, then verify eligibility.");
        setEligibilityStatus("Awaiting verification", "status-pill-neutral");
        updateWaitTimeCalculation();
    }

    private void setupInputRestrictions() {
        firstNameField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToLetters(e, firstNameField));
        lastNameField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToLetters(e, lastNameField));
        contactField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToContact(e, contactField));
        volumeField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, volumeField));
        weightField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, weightField));
        bpField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToBP(e, bpField));
        pulseField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, pulseField));
        temperatureField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToDecimal(e, temperatureField));
        hemoglobinField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToDecimal(e, hemoglobinField));
        sleptHoursField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToDecimal(e, sleptHoursField));
    }

    private void setupQuickFillButtons() {
        volumePresetBtn.setOnAction(e -> volumeField.setText("450"));
        bpPresetBtn.setOnAction(e -> bpField.setText("120/80"));
    }

    private void setupAgeCalculation() {
        birthdatePicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                calculateAge(newValue);
            } else {
                ageLabel.setText("");
            }
        });
    }

    private void calculateAge(LocalDate birthdate) {
        LocalDate today = LocalDate.now();
        if (birthdate.isAfter(today)) {
            ageLabel.setText("Invalid date");
            return;
        }
        long years = ChronoUnit.YEARS.between(birthdate, today);
        ageLabel.setText(years + " years old");
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
        if (!text.matches("\\d") || (current.length() >= 11)) {
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
        if (!text.matches("[\\d.]") || current.contains(".")) {
            e.consume();
        }
    }

    @FXML
    private void saveDonor() {
        resultLabel.setText("Running donor verification...");
        setEligibilityStatus("Verifying", "status-pill-neutral");

        String firstName = normalizeNamePart(firstNameField.getText());
        String lastName = normalizeNamePart(lastNameField.getText());
        String sex = sexCombo.getValue();
        String bloodType = bloodTypeCombo.getValue();
        String barangay = barangayCombo.getValue();
        String contact = normalizeContact(contactField.getText());
        LocalDate birthdate = birthdatePicker.getValue();
        LocalDate lastDonation = lastDonationPicker.getValue();
        LocalDate collectionDate = collectionDatePicker.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || sex == null || birthdate == null || bloodType == null
                || barangay == null || contact == null || collectionDate == null) {
            showValidation("Complete all required donor profile fields.");
            return;
        }

        if (collectionDate.isAfter(LocalDate.now())) {
            showValidation("Collection date cannot be in the future.");
            return;
        }

        if (lastDonation != null && !lastDonation.isBefore(collectionDate)) {
            showValidation("Previous donation must be earlier than the collection date.");
            return;
        }

        if (collectionDate.isBefore(birthdate)) {
            showValidation("Collection date cannot be earlier than the donor birthdate.");
            return;
        }

        if (lastDonation != null && lastDonation.isBefore(birthdate)) {
            showValidation("Previous donation cannot be earlier than the donor birthdate.");
            return;
        }

        Integer volume = parseVolume(volumeField.getText());
        if (volume == null) {
            return;
        }

        Double weight = parseDouble(weightField.getText(), "Weight must be a valid number in kilograms.");
        if (weight == null) {
            return;
        }

        BloodPressure bloodPressure = parseBloodPressure(bpField.getText());
        if (bloodPressure == null) {
            showValidation("Blood pressure must be encoded like 120/80.");
            return;
        }

        Integer pulse = parseInteger(pulseField.getText(), "Pulse rate must be a whole number in beats per minute.");
        if (pulse == null) {
            return;
        }

        Double temperature = parseDouble(temperatureField.getText(), "Temperature must be a valid value in Celsius.");
        if (temperature == null) {
            return;
        }

        Double hemoglobin = parseDouble(hemoglobinField.getText(), "Hemoglobin must be a valid value in g/dL.");
        if (hemoglobin == null) {
            return;
        }

        Double sleptHours = parseDouble(sleptHoursField.getText(), "Sleep hours must be a valid number.");
        if (sleptHours == null) {
            return;
        }

        DohDonor donor = new DohDonor(
                null,
                firstName,
                lastName,
                sex,
                birthdate,
                bloodType,
                barangay,
                contact,
                lastDonation
        );

        ScreeningInput screening = new ScreeningInput(
                collectionDate,
                volume,
                weight,
                bloodPressure.systolic,
                bloodPressure.diastolic,
                pulse,
                temperature,
                hemoglobin,
                sleptHours,
                guardianConsentCheckBox != null && guardianConsentCheckBox.isSelected(),
                hadMealCheckBox != null && hadMealCheckBox.isSelected(),
                alcoholCheckBox != null && !alcoholCheckBox.isSelected(),
                feverCheckBox != null && !feverCheckBox.isSelected(),
                tattooCheckBox != null && !tattooCheckBox.isSelected(),
                operationCheckBox != null && !operationCheckBox.isSelected(),
                pregnancyCheckBox != null && pregnancyCheckBox.isSelected()
        );

        UserAccount currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            showValidation("You must be logged in to register and screen donors.");
            return;
        }

        RegistrationResult result = donorDAO.registerDonation(donor, screening, currentUser.getUserId());
        Alert.AlertType alertType = result.isSuccess()
                ? Alert.AlertType.INFORMATION
                : (result.isDeferred() ? Alert.AlertType.WARNING : Alert.AlertType.ERROR);
        Alert alert = new Alert(alertType, result.getMessage());
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();

        if (result.isSuccess()) {
            clearForm(result);
        } else {
            resultLabel.setText(result.isDeferred()
                    ? "Temporary deferral recorded. Review the screening alert for details."
                    : "Registration could not be completed. Review the screening alert.");
            setEligibilityStatus(result.isDeferred() ? "Deferred" : "Review required",
                    result.isDeferred() ? "status-pill-warning" : "status-pill-danger");
        }
    }

    private void clearForm(RegistrationResult result) {
        firstNameField.clear();
        lastNameField.clear();
        sexCombo.getSelectionModel().clearSelection();
        birthdatePicker.setValue(null);
        bloodTypeCombo.getSelectionModel().clearSelection();
        barangayCombo.getSelectionModel().clearSelection();
        contactField.clear();
        lastDonationPicker.setValue(null);
        collectionDatePicker.setValue(LocalDate.now());
        weightField.clear();
        bpField.clear();
        pulseField.clear();
        temperatureField.clear();
        hemoglobinField.clear();
        sleptHoursField.clear();
        if (guardianConsentCheckBox != null) guardianConsentCheckBox.setSelected(false);
        if (hadMealCheckBox != null) hadMealCheckBox.setSelected(true);
        if (alcoholCheckBox != null) alcoholCheckBox.setSelected(false);
        if (feverCheckBox != null) feverCheckBox.setSelected(false);
        if (tattooCheckBox != null) tattooCheckBox.setSelected(false);
        if (operationCheckBox != null) operationCheckBox.setSelected(false);
        if (pregnancyCheckBox != null) pregnancyCheckBox.setSelected(false);

        String bagId = result.getBagId() == null ? "" : result.getBagId();
        resultLabel.setText("Registered successfully. " + bagId + " is now in quarantine pending TTI clearance.");
        setEligibilityStatus("Eligible", "status-pill-success");
        updateWaitTimeCalculation();
    }

    private void updateWaitTimeCalculation() {
        LocalDate collectionDate = collectionDatePicker.getValue() == null ? LocalDate.now() : collectionDatePicker.getValue();
        LocalDate lastDonation = lastDonationPicker.getValue();

        if (lastDonation == null) {
            waitTimeValueLabel.setText("Ready today");
            waitTimeHintLabel.setText("No previous whole-blood interval is blocking this collection date.");
            return;
        }

        LocalDate nextEligibleDate = lastDonation.plusDays(90);
        if (!nextEligibleDate.isAfter(collectionDate)) {
            waitTimeValueLabel.setText("Ready today");
            waitTimeHintLabel.setText("The 90-day whole-blood interval has already been satisfied.");
            return;
        }

        long daysRemaining = ChronoUnit.DAYS.between(collectionDate, nextEligibleDate);
        waitTimeValueLabel.setText("Wait " + daysRemaining + " day(s)");
        waitTimeHintLabel.setText("Earliest next whole-blood donation date: " + DATE_FORMATTER.format(nextEligibleDate));
    }

    private String normalizeNamePart(String raw) {
        if (raw == null) {
            return "";
        }
        String[] parts = raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }

    private String normalizeOptionalNamePart(String raw) {
        return normalizeNamePart(raw);
    }

    private String normalizeContact(String raw) {
        if (raw == null) {
            showValidation("Contact number is required.");
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("63") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }
        if (!digits.matches("09\\d{9}")) {
            showValidation("Contact number must be a valid Philippine mobile number starting with 09.");
            return null;
        }
        return digits;
    }

    private Integer parseVolume(String raw) {
        try {
            String text = raw == null ? "" : raw.trim();
            if (text.isEmpty()) {
                return 450;
            }
            int vol = Integer.parseInt(text);
            if (vol < 100 || vol > 500) {
                showValidation("Blood volume must be between 100-500 mL.");
                return null;
            }
            return vol;
        } catch (NumberFormatException ex) {
            showValidation("Blood volume must be a valid number in mL.");
            return null;
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
        setEligibilityStatus("Review required", "status-pill-warning");
    }

    private void setEligibilityStatus(String label, String variantStyleClass) {
        eligibilityStatusLabel.setText(label);
        eligibilityStatusLabel.getStyleClass().removeAll(
                "status-pill-neutral",
                "status-pill-success",
                "status-pill-warning",
                "status-pill-danger"
        );
        if (!eligibilityStatusLabel.getStyleClass().contains("status-pill")) {
            eligibilityStatusLabel.getStyleClass().add("status-pill");
        }
        eligibilityStatusLabel.getStyleClass().add(variantStyleClass);
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
