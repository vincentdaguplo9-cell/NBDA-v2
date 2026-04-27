package ui;

import dao.AuditDAO;
import dao.RecordsDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.AuditLogRecord;
import model.DonorRecord;
import model.IssuanceRecord;
import model.ScreeningHistoryRecord;
import model.UserAccount;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// Records hub for donor masterlist, screening history, issuance logs, and audit review.
public class RecordsViewController {
    private static final List<String> BARANGAYS = Arrays.asList(
            "Agpangi", "Anislagan", "Atipolo", "Bato", "Borac", "Cabungaan", "Calumpang", "Catmon",
            "Haguikhikan", "Padre Inocentes Garcia", "Larrazabal", "Lico", "Lucsoon", "Mabini",
            "Mocpong", "P.I. Garcia", "Santissimo Rosario", "Santo Nino", "Talustusan", "Villa Caneja"
    );
    private static final int PAGE_SIZE = 15;

    private final RecordsDAO recordsDAO = new RecordsDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    @FXML private TableView<DonorRecord> donorTable;
    @FXML private TableColumn<DonorRecord, Integer> donorIdColumn;
    @FXML private TableColumn<DonorRecord, String> donorNameColumn;
    @FXML private TableColumn<DonorRecord, String> donorSexColumn;
    @FXML private TableColumn<DonorRecord, String> donorBloodTypeColumn;
    @FXML private TableColumn<DonorRecord, String> donorContactColumn;
    @FXML private TableColumn<DonorRecord, LocalDate> donorLastDonationColumn;

    @FXML private TableView<ScreeningHistoryRecord> screeningTable;
    @FXML private TableColumn<ScreeningHistoryRecord, Integer> screeningIdColumn;
    @FXML private TableColumn<ScreeningHistoryRecord, String> screeningDonorColumn;
    @FXML private TableColumn<ScreeningHistoryRecord, LocalDate> screeningDateColumn;
    @FXML private TableColumn<ScreeningHistoryRecord, LocalDate> screeningCollectionColumn;
    @FXML private TableColumn<ScreeningHistoryRecord, String> screeningStatusColumn;
    @FXML private TableColumn<ScreeningHistoryRecord, String> screeningByColumn;

    @FXML private TableView<IssuanceRecord> issuanceTable;
    @FXML private TableColumn<IssuanceRecord, String> issuanceBagColumn;
    @FXML private TableColumn<IssuanceRecord, String> issuancePatientColumn;
    @FXML private TableColumn<IssuanceRecord, String> issuanceHospitalColumn;
    @FXML private TableColumn<IssuanceRecord, String> issuanceRequestColumn;
    @FXML private TableColumn<IssuanceRecord, String> issuanceCrossmatchColumn;
    @FXML private TableColumn<IssuanceRecord, String> issuanceByColumn;

    @FXML private TableView<AuditLogRecord> auditTable;
    @FXML private TableColumn<AuditLogRecord, java.time.LocalDateTime> auditTimeColumn;
    @FXML private TableColumn<AuditLogRecord, String> auditActorColumn;
    @FXML private TableColumn<AuditLogRecord, String> auditActionColumn;
    @FXML private TableColumn<AuditLogRecord, String> auditEntityColumn;
    @FXML private TableColumn<AuditLogRecord, String> auditDetailsColumn;

    @FXML private Tab auditTab;
    @FXML private Button refreshButton;
    @FXML private Button editDonorButton;
    @FXML private TextField donorSearchField;
    @FXML private TextField screeningSearchField;
    @FXML private TextField issuanceSearchField;

    @FXML private Pagination donorPagination;
    @FXML private Pagination screeningPagination;
    @FXML private Pagination issuancePagination;
    @FXML private Pagination auditPagination;

    @FXML
    public void initialize() {
        donorIdColumn.setCellValueFactory(new PropertyValueFactory<>("donorId"));
        donorNameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        donorSexColumn.setCellValueFactory(new PropertyValueFactory<>("sex"));
        donorBloodTypeColumn.setCellValueFactory(new PropertyValueFactory<>("bloodType"));
        donorContactColumn.setCellValueFactory(new PropertyValueFactory<>("contactNo"));
        donorLastDonationColumn.setCellValueFactory(new PropertyValueFactory<>("lastSuccessfulDonation"));

        screeningIdColumn.setCellValueFactory(new PropertyValueFactory<>("screeningId"));
        screeningDonorColumn.setCellValueFactory(new PropertyValueFactory<>("donorName"));
        screeningDateColumn.setCellValueFactory(new PropertyValueFactory<>("screeningDate"));
        screeningCollectionColumn.setCellValueFactory(new PropertyValueFactory<>("collectionDate"));
        screeningStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        screeningByColumn.setCellValueFactory(new PropertyValueFactory<>("screenedBy"));

        issuanceBagColumn.setCellValueFactory(new PropertyValueFactory<>("bagId"));
        issuancePatientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        issuanceHospitalColumn.setCellValueFactory(new PropertyValueFactory<>("requestHospital"));
        issuanceRequestColumn.setCellValueFactory(new PropertyValueFactory<>("bloodRequestNo"));
        issuanceCrossmatchColumn.setCellValueFactory(new PropertyValueFactory<>("crossmatchStatus"));
        issuanceByColumn.setCellValueFactory(new PropertyValueFactory<>("issuedBy"));

        auditTimeColumn.setCellValueFactory(new PropertyValueFactory<>("eventTime"));
        auditActorColumn.setCellValueFactory(new PropertyValueFactory<>("actor"));
        auditActionColumn.setCellValueFactory(new PropertyValueFactory<>("actionType"));
        auditEntityColumn.setCellValueFactory(new PropertyValueFactory<>("entityType"));
        auditDetailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

        IconFactory.install(refreshButton, "LIST", "R");
        IconFactory.install(editDonorButton, "EDIT", "E");

        UserAccount user = UserSession.getCurrentUser();
        auditTab.setDisable(user == null || !user.isAdmin());
        auditTab.setClosable(false);

        configureTable(donorTable);
        configureTable(screeningTable);
        configureTable(issuanceTable);
        configureTable(auditTable);

        donorPagination.setPageCount(Math.max(1, (int) Math.ceil(recordsDAO.getDonorCount() / (double) PAGE_SIZE)));
        donorPagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> loadDonorPage(newVal.intValue()));

        screeningPagination.setPageCount(Math.max(1, (int) Math.ceil(recordsDAO.getScreeningCount() / (double) PAGE_SIZE)));
        screeningPagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> loadScreeningPage(newVal.intValue()));

        issuancePagination.setPageCount(Math.max(1, (int) Math.ceil(recordsDAO.getIssuanceCount() / (double) PAGE_SIZE)));
        issuancePagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> loadIssuancePage(newVal.intValue()));

        auditPagination.setPageCount(Math.max(1, (int) Math.ceil(recordsDAO.getAuditCount() / (double) PAGE_SIZE)));
        auditPagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> loadAuditPage(newVal.intValue()));

        refreshAll();
    }

    private void loadDonorPage(int page) {
        donorTable.setItems(FXCollections.observableArrayList(recordsDAO.fetchDonorsPage(page, PAGE_SIZE)));
    }

    private void loadScreeningPage(int page) {
        screeningTable.setItems(FXCollections.observableArrayList(recordsDAO.fetchScreeningsPage(page, PAGE_SIZE)));
    }

    private void loadIssuancePage(int page) {
        issuanceTable.setItems(FXCollections.observableArrayList(recordsDAO.fetchIssuanceLogPage(page, PAGE_SIZE)));
    }

    private void loadAuditPage(int page) {
        auditTable.setItems(FXCollections.observableArrayList(recordsDAO.fetchAuditLogsPage(page, PAGE_SIZE)));
    }

    @FXML
    private void refreshAll() {
        loadDonorPage(0);
        loadScreeningPage(0);
        loadIssuancePage(0);
        loadAuditPage(0);
    }

    @FXML
    private void searchDonors() {
        donorPagination.setDisable(true);
        String query = donorSearchField.getText() == null ? "" : donorSearchField.getText().trim();
        if (query.isEmpty()) {
            donorTable.setItems(FXCollections.observableArrayList(recordsDAO.fetchDonors()));
        } else {
            donorTable.setItems(FXCollections.observableArrayList(recordsDAO.searchDonors(query)));
        }
    }

    @FXML
    private void searchScreenings() {
        screeningPagination.setDisable(true);
        String query = screeningSearchField.getText() == null ? "" : screeningSearchField.getText().trim();
        if (query.isEmpty()) {
            screeningTable.setItems(FXCollections.observableArrayList(recordsDAO.fetchScreenings()));
        } else {
            screeningTable.setItems(FXCollections.observableArrayList(recordsDAO.searchScreenings(query)));
        }
    }

    @FXML
    private void searchIssuances() {
        issuancePagination.setDisable(true);
        String query = issuanceSearchField.getText() == null ? "" : issuanceSearchField.getText().trim();
        if (query.isEmpty()) {
            issuanceTable.setItems(FXCollections.observableArrayList(recordsDAO.fetchIssuanceLog()));
        } else {
            issuanceTable.setItems(FXCollections.observableArrayList(recordsDAO.searchIssuances(query)));
        }
    }

    @FXML
    private void onDonorDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            DonorRecord selected = donorTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openDonorHistory(selected);
            }
        }
    }

    private void openDonorHistory(DonorRecord donor) {
        try {
            FXMLLoader loader = new FXMLLoader(FxmlView.fxmlUrl("DonorHistoryView.fxml"));
            Parent root = loader.load();
            DonorHistoryViewController controller = loader.getController();
            controller.loadDonorHistory(donor.getDonorId(), donor.getDisplayName());

            Stage stage = new Stage();
            stage.initOwner(donorTable.getScene().getWindow());
            stage.setTitle("Donation History - " + donor.getDisplayName());
            stage.setScene(new Scene(root, 800, 600));
            stage.setMaximized(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open donor history: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void editSelectedDonor() {
        DonorRecord selected = donorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a donor profile to edit.").showAndWait();
            return;
        }

        DonorRecord updated = showEditDialog(selected);
        if (updated == null) {
            return;
        }

        if (!recordsDAO.updateDonor(updated)) {
            new Alert(Alert.AlertType.ERROR, "Failed to update donor profile.").showAndWait();
            return;
        }

        UserAccount user = UserSession.getCurrentUser();
        if (user != null) {
            auditDAO.log(user.getUserId(), "DONOR_UPDATED", "donors",
                    String.valueOf(updated.getDonorId()), "Donor profile edited from records screen.");
        }
        refreshAll();
        new Alert(Alert.AlertType.INFORMATION, "Donor profile updated.").showAndWait();
    }

    private DonorRecord showEditDialog(DonorRecord donor) {
        Dialog<DonorRecord> dialog = new Dialog<>();
        dialog.setTitle("Edit Donor");
        dialog.setHeaderText("Update donor masterlist details");

        TextField firstName = new TextField(donor.getFirstName());
        TextField lastName = new TextField(donor.getLastName());
        ComboBox<String> sex = new ComboBox<>(FXCollections.observableArrayList("MALE", "FEMALE"));
        sex.getSelectionModel().select(donor.getSex());
        DatePicker birthDate = new DatePicker(donor.getBirthDate());
        ComboBox<String> bloodType = new ComboBox<>(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        bloodType.getSelectionModel().select(donor.getBloodType());
        ComboBox<String> barangay = new ComboBox<>(FXCollections.observableArrayList(BARANGAYS));
        barangay.getSelectionModel().select(donor.getBarangay());
        TextField contact = new TextField(donor.getContactNo());
        DatePicker lastDonation = new DatePicker(donor.getLastSuccessfulDonation());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("First Name"), firstName);
        grid.addRow(1, new Label("Last Name"), lastName);
        grid.addRow(2, new Label("Sex"), sex);
        grid.addRow(3, new Label("Birthdate"), birthDate);
        grid.addRow(4, new Label("Blood Type"), bloodType);
        grid.addRow(5, new Label("Barangay"), barangay);
        grid.addRow(6, new Label("Contact"), contact);
        grid.addRow(7, new Label("Last Donation"), lastDonation);

        ButtonType saveButtonType = new ButtonType("Save Donor", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            if (blank(firstName.getText()) || blank(lastName.getText()) || sex.getValue() == null || birthDate.getValue() == null
                    || bloodType.getValue() == null || barangay.getValue() == null || !isValidMobile(contact.getText())) {
                new Alert(Alert.AlertType.WARNING, "Complete all required donor fields with a valid Philippine mobile number.").showAndWait();
                return null;
            }
            return new DonorRecord(
                    donor.getDonorId(),
                    donor.getExternalCardId(),
                    donor.getExternalSource(),
                    normalizeName(firstName.getText()),
                    "",
                    normalizeName(lastName.getText()),
                    sex.getValue(),
                    birthDate.getValue(),
                    bloodType.getValue(),
                    barangay.getValue(),
                    normalizeContact(contact.getText()),
                    lastDonation.getValue()
            );
        });
        return dialog.showAndWait().orElse(null);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidMobile(String value) {
        return normalizeContact(value).matches("09\\d{9}");
    }

    private String normalizeContact(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.startsWith("63") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String[] parts = raw.trim().replaceAll("\\s+", " ").split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1).toLowerCase());
            }
        }
        return out.toString();
    }

    private <T> void configureTable(TableView<T> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(54);
        table.setPlaceholder(new Label("No records available."));
    }
}
