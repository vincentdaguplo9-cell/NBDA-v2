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

public class RecordDonationController {
    private static final List<String> BARANGAYS = Arrays.asList(
            "Agpangi", "Anislagan", "Atipolo", "Bato", "Borac", "Cabungaan", "Calumpang", "Catmon",
            "Haguikhikan", "Padre Inocentes Garcia", "Larrazabal", "Lico", "Lucsoon", "Mabini",
            "Mocpong", "P.I. Garcia", "Santissimo Rosario", "Santo Nino", "Talustusan", "Villa Caneja"
    );
    private static final Pattern BP_PATTERN = Pattern.compile("^(\\d{2,3})\\s*/\\s*(\\d{2,3})$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final DohDonorDAO donorDAO = new DohDonorDAO();
    private Integer selectedDonorId = null;

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label donorInfoLabel;
    @FXML private Label eligibilityStatusLabel;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private ComboBox<String> barangayCombo;

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

    @FXML private CheckBox hadMealCheckBox;
    @FXML private CheckBox alcoholCheckBox;
    @FXML private CheckBox feverCheckBox;
    @FXML private CheckBox tattooCheckBox;

    @FXML private Label resultLabel;
    @FXML private Label eligibilityStatusLabel2;
    @FXML private Button saveButton;

    @FXML
    public void initialize() {
        bloodTypeCombo.setItems(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        barangayCombo.setItems(FXCollections.observableArrayList(BARANGAYS));
        collectionDatePicker.setValue(LocalDate.now());

        setupInputRestrictions();

        collectionDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateWaitTimeCalculation());
    }

    private void setupInputRestrictions() {
        firstNameField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToLetters(e, firstNameField));
        lastNameField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToLetters(e, lastNameField));
        volumeField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, volumeField));
        weightField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, weightField));
        bpField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToBP(e, bpField));
        pulseField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToNumbers(e, pulseField));
        temperatureField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToDecimal(e, temperatureField));
        hemoglobinField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToDecimal(e, hemoglobinField));
        sleptHoursField.addEventFilter(KeyEvent.KEY_TYPED, e -> restrictToDecimal(e, sleptHoursField));
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
    private void searchDonor() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            donorInfoLabel.setText("Enter name or contact number to search");
            donorInfoLabel.setStyle("-fx-text-fill: #64748B;");
            eligibilityStatusLabel.setText("Status will appear after search");
            return;
        }

        DohDonor donor = donorDAO.findDonorBySearch(query);
        if (donor != null) {
            selectedDonorId = donor.getId();
            firstNameField.setText(donor.getFirstName());
            lastNameField.setText(donor.getLastName());
            bloodTypeCombo.setValue(donor.getBloodType());
            barangayCombo.setValue(donor.getBarangay());

            donorInfoLabel.setText("Found: " + donor.getDisplayName() + " | " + donor.getBloodType());
            donorInfoLabel.setStyle("-fx-text-fill: #22C55E;");

            if (donor.getLastSuccessfulDonation() != null) {
                LocalDate nextEligible = donor.getLastSuccessfulDonation().plusDays(90);
                if (nextEligible.isAfter(LocalDate.now())) {
                    long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), nextEligible);
                    eligibilityStatusLabel.setText("Last donation: " + donor.getLastSuccessfulDonation().format(DATE_FORMATTER) + " | Wait " + daysRemaining + " more days");
                    eligibilityStatusLabel.setStyle("-fx-text-fill: #F59E0B;");
                    setEligibilityStatus("Deferred", "status-pill-warning");
                } else {
                    eligibilityStatusLabel.setText("Last donation: " + donor.getLastSuccessfulDonation().format(DATE_FORMATTER) + " | Ready to donate!");
                    eligibilityStatusLabel.setStyle("-fx-text-fill: #22C55E;");
                    setEligibilityStatus("Eligible", "status-pill-success");
                }
            } else {
                eligibilityStatusLabel.setText("No previous donations on record");
                eligibilityStatusLabel.setStyle("-fx-text-fill: #22C55E;");
                setEligibilityStatus("New Donor", "status-pill-success");
            }
            updateWaitTimeCalculation();
            resultLabel.setText("Donor found. Verify medical screening and submit.");
        } else {
            selectedDonorId = null;
            donorInfoLabel.setText("No donor found with that search term");
            donorInfoLabel.setStyle("-fx-text-fill: #EF4444;");
            eligibilityStatusLabel.setText("Please check the name or contact and try again");
            eligibilityStatusLabel.setStyle("-fx-text-fill: #EF4444;");
            setEligibilityStatus("Not Found", "status-pill-danger");
            clearDonorFields();
        }
    }

    @FXML
    private void recordDonation() {
        if (selectedDonorId == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please search and select a donor first.");
            alert.initOwner(saveButton.getScene().getWindow());
            alert.showAndWait();
            return;
        }

        resultLabel.setText("Running eligibility verification...");
        setEligibilityStatus("Verifying", "status-pill-neutral");

        LocalDate collectionDate = collectionDatePicker.getValue();

        if (collectionDate == null) {
            showValidation("Collection date is required.");
            return;
        }

        if (collectionDate.isAfter(LocalDate.now())) {
            showValidation("Collection date cannot be in the future.");
            return;
        }

        Double weight = parseDouble(weightField.getText(), "Weight must be a valid number.");
        if (weight == null) return;

        BloodPressure bloodPressure = parseBloodPressure(bpField.getText());
        if (bloodPressure == null) {
            showValidation("Blood pressure must be encoded like 120/80.");
            return;
        }

        Integer pulse = parseInteger(pulseField.getText(), "Pulse must be a whole number.");
        if (pulse == null) return;

        Double temperature = parseDouble(temperatureField.getText(), "Temperature must be a valid value.");
        if (temperature == null) return;

        Double hemoglobin = parseDouble(hemoglobinField.getText(), "Hemoglobin must be a valid value.");
        if (hemoglobin == null) return;

        Double sleptHours = parseDouble(sleptHoursField.getText(), "Sleep hours must be a valid number.");
        if (sleptHours == null) return;

        Integer volume = parseVolume(volumeField.getText());
        if (volume == null) return;

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
                false,
                hadMealCheckBox != null && hadMealCheckBox.isSelected(),
                alcoholCheckBox != null && !alcoholCheckBox.isSelected(),
                feverCheckBox != null && !feverCheckBox.isSelected(),
                tattooCheckBox != null && !tattooCheckBox.isSelected(),
                false,
                false
        );

        UserAccount currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            showValidation("You must be logged in to record donations.");
            return;
        }

        DohDonor donor = new DohDonor(
                selectedDonorId,
                firstNameField.getText(),
                lastNameField.getText(),
                null,
                null,
                null,
                barangayCombo.getValue(),
                null,
                null
        );

        RegistrationResult result = donorDAO.registerDonation(donor, screening, currentUser.getUserId());
        Alert.AlertType alertType = result.isSuccess()
                ? Alert.AlertType.INFORMATION
                : (result.isDeferred() ? Alert.AlertType.WARNING : Alert.AlertType.ERROR);
        Alert alert = new Alert(alertType, result.getMessage());
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();

        if (result.isSuccess()) {
            clearForm();
        } else {
            resultLabel.setText(result.isDeferred()
                    ? "Deferred: " + result.getMessage()
                    : "Failed: " + result.getMessage());
            setEligibilityStatus(result.isDeferred() ? "Deferred" : "Failed",
                    result.isDeferred() ? "status-pill-warning" : "status-pill-danger");
        }
    }

    @FXML
    private void clearForm() {
        searchField.clear();
        firstNameField.clear();
        lastNameField.clear();
        bloodTypeCombo.getSelectionModel().clearSelection();
        barangayCombo.getSelectionModel().clearSelection();
        collectionDatePicker.setValue(LocalDate.now());
        weightField.clear();
        bpField.clear();
        pulseField.clear();
        temperatureField.clear();
        hemoglobinField.clear();
        sleptHoursField.clear();
        hadMealCheckBox.setSelected(true);
        alcoholCheckBox.setSelected(true);
        feverCheckBox.setSelected(true);
        tattooCheckBox.setSelected(true);
        selectedDonorId = null;
        donorInfoLabel.setText("Search for a returning donor above");
        donorInfoLabel.setStyle("-fx-text-fill: #64748B;");
        eligibilityStatusLabel.setText("Status will appear after search");
        resultLabel.setText("Find a returning donor to begin");
        setEligibilityStatus("Awaiting", "status-pill-neutral");
    }

    private void clearDonorFields() {
        firstNameField.clear();
        lastNameField.clear();
        bloodTypeCombo.getSelectionModel().clearSelection();
        barangayCombo.getSelectionModel().clearSelection();
    }

    private void updateWaitTimeCalculation() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            waitTimeValueLabel.setText("--");
            waitTimeHintLabel.setText("Search donor first");
            return;
        }

        DohDonor donor = donorDAO.findDonorBySearch(query);
        if (donor != null && donor.getLastSuccessfulDonation() != null) {
            LocalDate nextEligible = donor.getLastSuccessfulDonation().plusDays(90);
            if (nextEligible.isAfter(LocalDate.now())) {
                long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), nextEligible);
                waitTimeValueLabel.setText("Wait " + daysRemaining + " days");
                waitTimeHintLabel.setText("Next eligible: " + nextEligible.format(DATE_FORMATTER));
            } else {
                waitTimeValueLabel.setText("Ready");
                waitTimeHintLabel.setText("Can donate now");
            }
        } else {
            waitTimeValueLabel.setText("Ready");
            waitTimeHintLabel.setText("No previous donation restriction");
        }
    }

    private void showValidation(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();
    }

    private void setEligibilityStatus(String text, String styleClass) {
        eligibilityStatusLabel2.setText(text);
        eligibilityStatusLabel2.getStyleClass().clear();
        eligibilityStatusLabel2.getStyleClass().add(styleClass);
    }

    private Double parseDouble(String text, String errorMsg) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            showValidation(errorMsg);
            return null;
        }
    }

    private Integer parseInteger(String text, String errorMsg) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            showValidation(errorMsg);
            return null;
        }
    }

    private BloodPressure parseBloodPressure(String text) {
        try {
            Matcher matcher = BP_PATTERN.matcher(text.trim());
            if (matcher.matches()) {
                return new BloodPressure(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
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

    private static class BloodPressure {
        final int systolic;
        final int diastolic;
        BloodPressure(int systolic, int diastolic) {
            this.systolic = systolic;
            this.diastolic = diastolic;
        }
    }
}