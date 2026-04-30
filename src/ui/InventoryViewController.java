package ui;

import dao.DohInventoryDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import model.BloodBagRecord;
import model.InventoryActionResult;
import model.IssueRequestInput;
import model.TtiScreeningInput;
import model.UserAccount;

import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import javafx.stage.FileChooser;

// Inventory controller with pagination-based archive browsing.
public class InventoryViewController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final int PAGE_SIZE = 10;

    private final DohInventoryDAO inventoryDAO = new DohInventoryDAO();
    private final ObservableList<BloodBagRecord> inventoryItems = FXCollections.observableArrayList();
    private final FilteredList<BloodBagRecord> filteredInventory = new FilteredList<>(inventoryItems, item -> true);
    private final ObservableList<BloodBagRecord> pagedInventory = FXCollections.observableArrayList();

    @FXML private TableView<BloodBagRecord> inventoryTable;
    @FXML private TableColumn<BloodBagRecord, String> bagIdColumn;
    @FXML private TableColumn<BloodBagRecord, String> barangayColumn;
    @FXML private TableColumn<BloodBagRecord, String> donorColumn;
    @FXML private TableColumn<BloodBagRecord, String> bloodTypeColumn;
    @FXML private TableColumn<BloodBagRecord, LocalDate> collectedColumn;
    @FXML private TableColumn<BloodBagRecord, LocalDate> expiryColumn;
    @FXML private TableColumn<BloodBagRecord, String> statusColumn;
    @FXML private Label pageSummaryLabel;
    @FXML private Pagination inventoryPagination;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button refreshButton;
    @FXML private Button releaseIssueButton;
    @FXML private ComboBox<String> bloodTypeFilterCombo;
    @FXML private ComboBox<String> barangayFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Button clearFilterButton;
    @FXML private Button exportCsvButton;

    private static final List<String> BARANGAYS = Arrays.asList(
            "Agpangi", "Anislagan", "Atipolo", "Bato", "Borac", "Cabungaan", "Calumpang", "Catmon",
            "Haguikhikan", "Padre Inocentes Garcia", "Larrazabal", "Lico", "Lucsoon", "Mabini",
            "Mocpong", "P.I. Garcia", "Santissimo Rosario", "Santo Nino", "Talustusan", "Villa Caneja"
    );

    @FXML
    public void initialize() {
        bagIdColumn.setCellValueFactory(new PropertyValueFactory<>("bagId"));
        barangayColumn.setCellValueFactory(new PropertyValueFactory<>("barangay"));
        donorColumn.setCellValueFactory(new PropertyValueFactory<>("donorName"));
        bloodTypeColumn.setCellValueFactory(new PropertyValueFactory<>("bloodType"));
        collectedColumn.setCellValueFactory(new PropertyValueFactory<>("dateCollected"));
        expiryColumn.setCellValueFactory(new PropertyValueFactory<>("dateExpiry"));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEffectiveStatus()));

        bagIdColumn.setCellFactory(centeredCellFactory(this::safeText));
        barangayColumn.setCellFactory(centeredCellFactory(this::safeText));
        donorColumn.setCellFactory(centeredCellFactory(this::safeText));
        bloodTypeColumn.setCellFactory(buildBloodTypeCellFactory());
        collectedColumn.setCellFactory(centeredCellFactory(this::formatDate));
        expiryColumn.setCellFactory(centeredCellFactory(this::formatDate));
        statusColumn.setCellFactory(buildStatusCellFactory());

        inventoryTable.setItems(pagedInventory);
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        inventoryTable.setFixedCellSize(45);
        inventoryTable.setPlaceholder(buildEmptyState("No blood bags match the current filters."));

        inventoryPagination.setMaxPageIndicatorCount(6);
        inventoryPagination.currentPageIndexProperty().addListener((obs, oldValue, newValue) -> loadPage(newValue.intValue()));
        inventoryPagination.skinProperty().addListener((obs, oldSkin, newSkin) -> hidePaginationPageInfo());
        Platform.runLater(this::hidePaginationPageInfo);

        IconFactory.install(refreshButton, "LIST", "R");
        IconFactory.install(releaseIssueButton, "TINT", "B");
        IconFactory.install(searchButton, "SEARCH", "S");

        bloodTypeFilterCombo.setItems(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        barangayFilterCombo.setItems(FXCollections.observableArrayList(BARANGAYS));
        statusFilterCombo.setItems(FXCollections.observableArrayList("AVAILABLE", "QUARANTINE", "ISSUED", "EXPIRED", "DISCARDED"));

        bloodTypeFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyAllFilters());
        barangayFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyAllFilters());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyAllFilters());

        refreshInventory();
    }

    @FXML
    private void clearFilters() {
        bloodTypeFilterCombo.setValue(null);
        barangayFilterCombo.setValue(null);
        statusFilterCombo.setValue(null);
        searchField.clear();
        applyAllFilters();
    }

    private void applyAllFilters() {
        String searchText = searchField.getText();
        String bloodType = bloodTypeFilterCombo.getValue();
        String barangay = barangayFilterCombo.getValue();
        String status = statusFilterCombo.getValue();

        filteredInventory.setPredicate(record -> {
            if (searchText != null && !searchText.trim().isEmpty()) {
                String query = searchText.trim().toLowerCase();
                if (!contains(record.getBagId(), query)
                        && !contains(record.getBarangay(), query)
                        && !contains(record.getDonorName(), query)
                        && !contains(record.getBloodType(), query)
                        && !contains(record.getEffectiveStatus(), query)) {
                    return false;
                }
            }

            if (bloodType != null && !bloodType.isEmpty()) {
                if (!bloodType.equalsIgnoreCase(record.getBloodType())) {
                    return false;
                }
            }

            if (barangay != null && !barangay.isEmpty()) {
                if (!barangay.equalsIgnoreCase(record.getBarangay())) {
                    return false;
                }
            }

            if (status != null && !status.isEmpty()) {
                if (!status.equalsIgnoreCase(record.getEffectiveStatus())) {
                    return false;
                }
            }

            return true;
        });

        rebuildPagination(0);
    }

    @FXML
    private void searchInventory() {
        applyAllFilters();
    }

    @FXML
    private void refreshInventory() {
        inventoryItems.setAll(inventoryDAO.fetchInventory());
        applyAllFilters();
    }

    @FXML
    private void handleReleaseOrIssue() {
        BloodBagRecord selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Select a blood bag first.");
            alert.initOwner(inventoryTable.getScene().getWindow());
            alert.showAndWait();
            return;
        }

        String effectiveStatus = selected.getEffectiveStatus();
        if ("EXPIRED".equalsIgnoreCase(effectiveStatus)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Expired blood cannot be released or issued.");
            alert.initOwner(inventoryTable.getScene().getWindow());
            alert.showAndWait();
            return;
        }

        InventoryActionResult result;
        if ("QUARANTINE".equalsIgnoreCase(effectiveStatus)) {
            result = processTtiRelease(selected.getBagId());
        } else if ("AVAILABLE".equalsIgnoreCase(effectiveStatus)) {
            result = processIssue(selected.getBagId());
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "This bag is already " + effectiveStatus + ".");
            alert.initOwner(inventoryTable.getScene().getWindow());
            alert.showAndWait();
            return;
        }

        Alert.AlertType alertType = result.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(alertType, result.getMessage());
        alert.initOwner(inventoryTable.getScene().getWindow());
        alert.showAndWait();
        refreshInventory();
    }

    private void rebuildPagination(int preferredIndex) {
        int totalItems = filteredInventory.size();
        int pageCount = Math.max(1, (int) Math.ceil(totalItems / (double) PAGE_SIZE));
        int safeIndex = Math.min(Math.max(preferredIndex, 0), pageCount - 1);

        inventoryPagination.setDisable(totalItems <= PAGE_SIZE);
        inventoryPagination.setPageCount(pageCount);
        if (inventoryPagination.getCurrentPageIndex() != safeIndex) {
            inventoryPagination.setCurrentPageIndex(safeIndex);
        }
        loadPage(safeIndex);
    }

    private void loadPage(int pageIndex) {
        List<BloodBagRecord> filtered = new ArrayList<>(filteredInventory);
        if (filtered.isEmpty()) {
            pagedInventory.clear();
            pageSummaryLabel.setText("Showing 0-0 of 0 units");
            return;
        }

        int fromIndex = pageIndex * PAGE_SIZE;
        if (fromIndex >= filtered.size()) {
            fromIndex = Math.max(0, (inventoryPagination.getPageCount() - 1) * PAGE_SIZE);
        }
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filtered.size());

        pagedInventory.setAll(filtered.subList(fromIndex, toIndex));
        pageSummaryLabel.setText("Showing " + (fromIndex + 1) + "-" + toIndex + " of " + filtered.size() + " units");
    }

    private Optional<TtiScreeningInput> showTtiDialog(String bagId) {
        Dialog<TtiScreeningInput> dialog = new Dialog<>();
        dialog.setTitle("TTI Screening");
        dialog.setHeaderText("Encode mandatory TTI screening for bag " + bagId);
        dialog.initOwner(inventoryTable.getScene().getWindow());

        ComboBox<String> hiv = ttiCombo();
        ComboBox<String> hbv = ttiCombo();
        ComboBox<String> hcv = ttiCombo();
        ComboBox<String> syphilis = ttiCombo();
        ComboBox<String> malaria = ttiCombo();
        TextField testKit = new TextField("Rapid donor screening kit");
        TextArea remarks = new TextArea();
        remarks.setPromptText("Optional remarks or discard note");
        remarks.setWrapText(true);
        remarks.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("HIV"), hiv);
        grid.addRow(1, new Label("Hepatitis B"), hbv);
        grid.addRow(2, new Label("Hepatitis C"), hcv);
        grid.addRow(3, new Label("Syphilis"), syphilis);
        grid.addRow(4, new Label("Malaria"), malaria);
        grid.addRow(5, new Label("Test Kit / Method"), testKit);
        grid.addRow(6, new Label("Remarks"), remarks);

        ButtonType saveButtonType = new ButtonType("Save Screening", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            if (normalizeRemarks(testKit.getText()) == null) {
                Alert warn = new Alert(Alert.AlertType.WARNING, "Test kit or method is required.");
                warn.initOwner(inventoryTable.getScene().getWindow());
                warn.showAndWait();
                return null;
            }
            return new TtiScreeningInput(
                    hiv.getValue(),
                    hbv.getValue(),
                    hcv.getValue(),
                    syphilis.getValue(),
                    malaria.getValue(),
                    normalizeRemarks(testKit.getText()),
                    normalizeRemarks(remarks.getText())
            );
        });
        return dialog.showAndWait();
    }

    private Optional<IssueRequestInput> showIssueDialog(String bagId) {
        Dialog<IssueRequestInput> dialog = new Dialog<>();
        dialog.setTitle("Issue Blood Bag");
        dialog.setHeaderText("Encode release details for bag " + bagId);
        dialog.initOwner(inventoryTable.getScene().getWindow());

        TextField patientName = new TextField();
        TextField patientNo = new TextField();
        TextField hospital = new TextField();
        TextField physician = new TextField();
        TextField requestNo = new TextField();
        ComboBox<String> crossmatch = new ComboBox<>(FXCollections.observableArrayList(
                "COMPATIBLE", "NOT_REQUIRED", "INCOMPATIBLE", "PENDING"
        ));
        crossmatch.getSelectionModel().select("COMPATIBLE");
        TextArea notes = new TextArea();
        notes.setPromptText("Issue notes or release remarks");
        notes.setWrapText(true);
        notes.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("Patient Name"), patientName);
        grid.addRow(1, new Label("Patient Hospital No"), patientNo);
        grid.addRow(2, new Label("Request Hospital"), hospital);
        grid.addRow(3, new Label("Requesting Physician"), physician);
        grid.addRow(4, new Label("Blood Request No"), requestNo);
        grid.addRow(5, new Label("Crossmatch"), crossmatch);
        grid.addRow(6, new Label("Notes"), notes);

        ButtonType issueButton = new ButtonType("Issue Bag", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(issueButton, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != issueButton) {
                return null;
            }
            return new IssueRequestInput(
                    normalizeRemarks(patientName.getText()),
                    normalizeRemarks(patientNo.getText()),
                    normalizeRemarks(hospital.getText()),
                    normalizeRemarks(physician.getText()),
                    normalizeRemarks(requestNo.getText()),
                    crossmatch.getValue(),
                    normalizeRemarks(notes.getText())
            );
        });
        return dialog.showAndWait();
    }

    private InventoryActionResult processTtiRelease(String bagId) {
        UserAccount user = UserSession.getCurrentUser();
        if (user == null) {
            return InventoryActionResult.failure("You must be logged in to encode TTI screening.");
        }
        Optional<TtiScreeningInput> input = showTtiDialog(bagId);
        if (input.isEmpty()) {
            return InventoryActionResult.failure("TTI screening was canceled.");
        }
        // Get the actual TTI input from Optional and add testedAt and testedBy
        TtiScreeningInput tti = input.get();
        TtiScreeningInput ttiWithUser = new TtiScreeningInput(
                tti.getHiv(), tti.getHbv(), tti.getHcv(), 
                tti.getSyphilis(), tti.getMalaria(), tti.getTestKit(), 
                tti.getRemarks(), java.time.LocalDateTime.now(), user.getUserId());
        return inventoryDAO.releaseBag(bagId, ttiWithUser);
    }

    private InventoryActionResult processIssue(String bagId) {
        UserAccount user = UserSession.getCurrentUser();
        if (user == null) {
            return InventoryActionResult.failure("You must be logged in to issue blood bags.");
        }
        Optional<IssueRequestInput> request = showIssueDialog(bagId);
        if (request.isEmpty()) {
            return InventoryActionResult.failure("Issuance was canceled.");
        }
        // Get the actual IssueRequestInput from Optional and add bagId, issuedAt and issuedBy
        IssueRequestInput req = request.get();
        IssueRequestInput reqWithUser = new IssueRequestInput(
                bagId, req.getPatientName(), req.getPatientHospitalNo(),
                req.getRequestHospital(), req.getRequestingPhysician(), 
                req.getBloodRequestNo(), req.getCrossmatchStatus(), 
                req.getIssueNotes(), java.time.LocalDateTime.now(), user.getUserId());
        return inventoryDAO.issueBag(reqWithUser);
    }

    private ComboBox<String> ttiCombo() {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("NON_REACTIVE", "REACTIVE"));
        combo.getSelectionModel().select("NON_REACTIVE");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private Callback<TableColumn<BloodBagRecord, String>, TableCell<BloodBagRecord, String>> buildBloodTypeCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(null);
                setText(null);
                if (empty || item == null || item.isBlank()) {
                    return;
                }
                Label label = new Label(item);
                label.getStyleClass().add("blood-type-display");
                setGraphic(label);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private Callback<TableColumn<BloodBagRecord, String>, TableCell<BloodBagRecord, String>> buildStatusCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                setAlignment(Pos.CENTER);
                if (empty || item == null || item.isBlank()) {
                    return;
                }

                Circle dot = new Circle(5, statusColor(item));
                Label label = new Label(item);
                label.getStyleClass().add("status-cell-label");
                HBox wrapper = new HBox(8, dot, label);
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private <T> Callback<TableColumn<BloodBagRecord, T>, TableCell<BloodBagRecord, T>> centeredCellFactory(Function<T, String> formatter) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(null);
                setText(empty || item == null ? null : formatter.apply(item));
            }
        };
    }

    private Color statusColor(String status) {
        if ("AVAILABLE".equalsIgnoreCase(status)) {
            return Color.web("#22C55E");
        }
        if ("QUARANTINE".equalsIgnoreCase(status)) {
            return Color.web("#F59E0B");
        }
        return Color.web("#E11D48");
    }

    private Label buildEmptyState(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        return label;
    }

    private void hidePaginationPageInfo() {
        Node pageInfo = inventoryPagination.lookup(".page-information");
        if (pageInfo != null) {
            pageInfo.setVisible(false);
            pageInfo.setManaged(false);
        }
    }

    private String normalizeRemarks(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        return text.isEmpty() ? null : text;
    }

    @FXML
    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Inventory to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("blood_inventory_" + LocalDate.now().toString() + ".csv");

        java.io.File file = fileChooser.showSaveDialog(inventoryTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Bag ID,Blood Type,Status,Collected Date,Expiry Date,Donor Name,Barangay\n");

            for (BloodBagRecord record : filteredInventory) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        record.getBagId(),
                        record.getBloodType(),
                        record.getEffectiveStatus(),
                        record.getDateCollected() != null ? record.getDateCollected().toString() : "",
                        record.getDateExpiry() != null ? record.getDateExpiry().toString() : "",
                        record.getDonorName(),
                        record.getBarangay()
                ));
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Inventory exported successfully to " + file.getName());
            alert.initOwner(inventoryTable.getScene().getWindow());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export: " + e.getMessage());
            alert.initOwner(inventoryTable.getScene().getWindow());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
