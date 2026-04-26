package ui;

import dao.RecordsDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.IssuanceRecord;
import model.ScreeningHistoryRecord;

import java.time.LocalDate;
import java.util.List;

public class DonorHistoryViewController {
    private final RecordsDAO recordsDAO = new RecordsDAO();
    private int donorId;
    private String donorName;

    @FXML private Label donorNameLabel;
    @FXML private TableView donationTable;
    @FXML private TableColumn donationBagColumn;
    @FXML private TableColumn donationBloodTypeColumn;
    @FXML private TableColumn donationCollectionColumn;
    @FXML private TableColumn donationStatusColumn;
    @FXML private TableColumn donationExpiryColumn;

    @FXML private TableView screeningTable;
    @FXML private TableColumn screeningIdColumn;
    @FXML private TableColumn screeningDateColumn;
    @FXML private TableColumn screeningCollectionColumn;
    @FXML private TableColumn screeningStatusColumn;
    @FXML private TableColumn screeningByColumn;

    @FXML private TableView issuanceTable;
    @FXML private TableColumn issuanceBagColumn;
    @FXML private TableColumn issuancePatientColumn;
    @FXML private TableColumn issuanceHospitalColumn;
    @FXML private TableColumn issuanceDateColumn;
    @FXML private TableColumn issuanceCrossmatchColumn;

    @FXML
    public void initialize() {
        donationBagColumn.setCellValueFactory(new PropertyValueFactory<>("bagId"));
        donationBloodTypeColumn.setCellValueFactory(new PropertyValueFactory<>("bloodType"));
        donationCollectionColumn.setCellValueFactory(new PropertyValueFactory<>("dateCollected"));
        donationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        donationExpiryColumn.setCellValueFactory(new PropertyValueFactory<>("dateExpiry"));

        screeningIdColumn.setCellValueFactory(new PropertyValueFactory<>("screeningId"));
        screeningDateColumn.setCellValueFactory(new PropertyValueFactory<>("screeningDate"));
        screeningCollectionColumn.setCellValueFactory(new PropertyValueFactory<>("collectionDate"));
        screeningStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        screeningByColumn.setCellValueFactory(new PropertyValueFactory<>("screenedBy"));

        issuanceBagColumn.setCellValueFactory(new PropertyValueFactory<>("bagId"));
        issuancePatientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        issuanceHospitalColumn.setCellValueFactory(new PropertyValueFactory<>("requestHospital"));
        issuanceDateColumn.setCellValueFactory(new PropertyValueFactory<>("issuedAt"));
        issuanceCrossmatchColumn.setCellValueFactory(new PropertyValueFactory<>("crossmatchStatus"));

        configureTable(donationTable);
        configureTable(screeningTable);
        configureTable(issuanceTable);
    }

    public void loadDonorHistory(int donorId, String donorName) {
        this.donorId = donorId;
        this.donorName = donorName;
        if (donorNameLabel != null) {
            donorNameLabel.setText(donorName);
        }
        refreshHistory();
    }

    @FXML
    private void refreshHistory() {
        if (donorId <= 0) {
            return;
        }
        try {
            var screenings = recordsDAO.fetchDonorScreenings(donorId);
            screeningTable.setItems(FXCollections.observableArrayList(screenings));

            var issuances = recordsDAO.fetchDonorIssuances(donorId);
            issuanceTable.setItems(FXCollections.observableArrayList(issuances));

            var donations = recordsDAO.fetchDonorBloodBags(donorId);
            donationTable.setItems(FXCollections.observableArrayList(donations));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T> void configureTable(TableView<T> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(54);
        table.setPlaceholder(new Label("No records available."));
    }
}