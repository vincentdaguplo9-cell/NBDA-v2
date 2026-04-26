package ui;

import dao.AuthDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import model.LoginResult;

import java.io.IOException;

public class LoginController {
    private final AuthDAO authDAO = new AuthDAO();

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private CheckBox showPasswordCheck;
    @FXML private ImageView brandBg;

    @FXML
    public void initialize() {
        try {
            Image bg = new Image(getClass().getResourceAsStream("/assets/img/bg.jpg"));
            brandBg.setImage(bg);
        } catch (Exception e) {
            System.err.println("Failed to load background: " + e.getMessage());
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        if (showPasswordCheck.isSelected()) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
        }
    }

    @FXML
    private void handleLogin() {
        statusLabel.setText("");
        String password = showPasswordCheck.isSelected() ? passwordTextField.getText() : passwordField.getText();
        LoginResult result = authDAO.login(usernameField.getText(), password);
        if (!result.isSuccess()) {
            statusLabel.setText(result.getMessage());
            new Alert(Alert.AlertType.WARNING, result.getMessage()).showAndWait();
            return;
        }

        UserSession.login(result.getUser());
        try {
            FXMLLoader loader = new FXMLLoader(FxmlView.fxmlUrl("AppShell.fxml"));
            Parent root = loader.load();
            double width = Screen.getPrimary().getVisualBounds().getWidth();
            double height = Screen.getPrimary().getVisualBounds().getHeight();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(FxmlView.stylesheet("nbda-modern.css"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setTitle("NBDA - Naval Blood Donation Archive");
        } catch (IOException e) {
            throw new RuntimeException("Failed to open application shell.", e);
        }
    }
}