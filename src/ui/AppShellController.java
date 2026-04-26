package ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;

import java.io.IOException;

public class AppShellController {
    @FXML private Button dashboardButton;
    @FXML private Button registrationButton;
    @FXML private Button recordDonationButton;
    @FXML private Button inventoryButton;
    @FXML private Button recordsButton;
    @FXML private Button logoutButton;
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        loadContent("Dashboard.fxml");
        activate(dashboardButton);
    }

    @FXML
    private void showRegistration() {
        loadContent("DonorRegistration.fxml");
        activate(registrationButton);
    }

    @FXML
    private void showRecordDonation() {
        loadContent("RecordDonation.fxml");
        activate(recordDonationButton);
    }

    @FXML
    private void showInventory() {
        loadContent("InventoryView.fxml");
        activate(inventoryButton);
    }

    @FXML
    private void showRecords() {
        loadContent("RecordsView.fxml");
        activate(recordsButton);
    }

    @FXML
    private void handleLogout() {
        UserSession.logout();
        try {
            FXMLLoader loader = new FXMLLoader(FxmlView.fxmlUrl("Login.fxml"));
            Parent view = loader.load();
            double width = Screen.getPrimary().getVisualBounds().getWidth();
            double height = Screen.getPrimary().getVisualBounds().getHeight();
            javafx.scene.Scene scene = new javafx.scene.Scene(view, width, height);
            scene.getStylesheets().add(FxmlView.stylesheet("nbda-modern.css"));
            javafx.stage.Stage stage = (javafx.stage.Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to return to login screen.", e);
        }
    }

    private void loadContent(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(FxmlView.fxmlUrl(fxml));
            Parent view = loader.load();
            if (view instanceof Region) {
                Region region = (Region) view;
                region.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                region.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }
            contentArea.getChildren().setAll(view);

            if (contentArea.getScene() != null) {
                contentArea.getScene().getStylesheets().clear();
                contentArea.getScene().getStylesheets().add(FxmlView.stylesheet("nbda-modern.css"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxml, e);
        }
    }

    private void activate(Button activeButton) {
        dashboardButton.getStyleClass().remove("nav-active");
        registrationButton.getStyleClass().remove("nav-active");
        recordDonationButton.getStyleClass().remove("nav-active");
        inventoryButton.getStyleClass().remove("nav-active");
        recordsButton.getStyleClass().remove("nav-active");
        activeButton.getStyleClass().add("nav-active");
    }
}