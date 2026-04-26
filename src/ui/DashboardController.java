package ui;

import dao.DohInventoryDAO;
import dao.DohDonorDAO;
import dao.RecordsDAO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.BloodBagRecord;
import model.IssuanceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {
    private static final List<String> BLOOD_TYPES = Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy");

    private final DohInventoryDAO inventoryDAO = new DohInventoryDAO();
    private final DohDonorDAO donorDAO = new DohDonorDAO();
    private final RecordsDAO recordsDAO = new RecordsDAO();
    private Timeline clockTimeline;

    @FXML private Label currentDateTimeLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label totalBagsLabel;
    @FXML private Label availableBagsLabel;
    @FXML private Label totalDonorsLabel;
    @FXML private Label pendingTtiLabel;
    @FXML private BarChart<String, Number> stockChart;
    @FXML private PieChart distributionChart;
    @FXML private Label activityCountLabel;
    @FXML private VBox activityList;

    @FXML
    public void initialize() {
        configureCharts();
        startClock();
        refreshDashboard();
    }

    private void configureCharts() {
        stockChart.setLegendVisible(false);
        stockChart.setAnimated(false);
        stockChart.setVerticalGridLinesVisible(false);
        stockChart.setHorizontalGridLinesVisible(true);

        distributionChart.setLegendVisible(true);
        distributionChart.setAnimated(false);
    }

    private void startClock() {
        updateClock();
        clockTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateClock()));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void updateClock() {
        currentDateTimeLabel.setText(DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        currentDateLabel.setText(DATE_FORMATTER.format(LocalDateTime.now()));
    }

    private void refreshDashboard() {
        List<BloodBagRecord> inventory = inventoryDAO.fetchInventory();
        Map<String, Long> availableCounts = calculateAvailableCounts(inventory);
        List<IssuanceRecord> issuances = recordsDAO.fetchRecentIssuances();
        int totalDonors = donorDAO.getTotalDonorCount();

        totalBagsLabel.setText(String.valueOf(inventory.size()));
        availableBagsLabel.setText(String.valueOf(availableCounts.values().stream().mapToLong(Long::longValue).sum()));
        totalDonorsLabel.setText(String.valueOf(totalDonors));
        pendingTtiLabel.setText(String.valueOf(inventory.stream()
                .filter(record -> "QUARANTINE".equalsIgnoreCase(record.getEffectiveStatus()))
                .count()));

        populateStockChart(availableCounts);
        populateDistributionChart(inventory);
        populateActivityList(inventory, issuances);
    }

    private Map<String, Long> calculateAvailableCounts(List<BloodBagRecord> inventory) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String bloodType : BLOOD_TYPES) {
            counts.put(bloodType, 0L);
        }
        for (BloodBagRecord record : inventory) {
            if ("AVAILABLE".equalsIgnoreCase(record.getEffectiveStatus())) {
                counts.put(record.getBloodType(), counts.getOrDefault(record.getBloodType(), 0L) + 1);
            }
        }
        return counts;
    }

    private void populateStockChart(Map<String, Long> availableCounts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Long> entry : availableCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        stockChart.getData().setAll(series);
    }

    private void populateDistributionChart(List<BloodBagRecord> inventory) {
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (String bloodType : BLOOD_TYPES) {
            typeCounts.put(bloodType, 0L);
        }
        for (BloodBagRecord record : inventory) {
            typeCounts.put(record.getBloodType(), typeCounts.getOrDefault(record.getBloodType(), 0L) + 1);
        }

        var pieData = FXCollections.<PieChart.Data>observableArrayList();
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
        }
        distributionChart.setData(pieData);
    }

    private void populateActivityList(List<BloodBagRecord> inventory, List<IssuanceRecord> issuances) {
        activityList.getChildren().clear();

        var allActivities = new java.util.ArrayList<java.util.AbstractMap.SimpleEntry<String, String>>();

        inventory.stream()
                .filter(r -> "QUARANTINE".equalsIgnoreCase(r.getEffectiveStatus()))
                .sorted(Comparator.comparing(BloodBagRecord::getDateCollected, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .forEach(r -> allActivities.add(new java.util.AbstractMap.SimpleEntry<>("SCREENED", r.getBagId() + " - " + r.getDonorName())));

        issuances.stream()
                .limit(4)
                .forEach(i -> allActivities.add(new java.util.AbstractMap.SimpleEntry<>("ISSUED", i.getBagId() + " to " + i.getPatientName())));

        allActivities.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        activityCountLabel.setText(allActivities.size() + " entries");

        if (allActivities.isEmpty()) {
            Label empty = new Label("No recent activity.");
            empty.getStyleClass().add("empty-state");
            activityList.getChildren().add(empty);
            return;
        }

        allActivities.stream()
                .limit(6)
                .forEach(entry -> activityList.getChildren().add(buildActivityItem(entry.getKey(), entry.getValue())));
    }

    private HBox buildActivityItem(String type, String description) {
        HBox row = new HBox(12);
        row.getStyleClass().add("critical-action-item");

        Region accentBar = new Region();
        accentBar.getStyleClass().add("critical-action-accent");
        if ("ISSUED".equals(type)) {
            accentBar.setStyle("-fx-background-color: #22C55E;");
        } else if ("SCREENED".equals(type)) {
            accentBar.setStyle("-fx-background-color: #F59E0B;");
        }

        VBox details = new VBox(3);
        Label title = new Label(description);
        title.getStyleClass().add("critical-action-title");

        Label meta = new Label(type);
        meta.getStyleClass().add("critical-action-meta");
        details.getChildren().addAll(title, meta);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label statusPill = new Label(type);
        statusPill.getStyleClass().add("status-pill");
        if ("ISSUED".equals(type)) {
            statusPill.getStyleClass().add("status-pill-success");
        } else {
            statusPill.getStyleClass().add("status-pill-warning");
        }

        row.getChildren().addAll(accentBar, details, statusPill);
        return row;
    }
}