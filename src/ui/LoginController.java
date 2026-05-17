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

    @FXML
    public void initialize() {
        // Initialization logic here (CSS handles the background)
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
            Alert alert = new Alert(Alert.AlertType.WARNING, result.getMessage());
            alert.setTitle("Login Failed");
            alert.setHeaderText(null);
            if (loginButton != null && loginButton.getScene() != null && loginButton.getScene().getWindow() != null) {
                alert.initOwner(loginButton.getScene().getWindow());
            }
            alert.showAndWait();
            return;
        }

        UserSession.login(result.getUser());
        try {
            FXMLLoader loader = new FXMLLoader(FxmlView.fxmlUrl("AppShell.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(FxmlView.stylesheet("style.css"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("Press ESC to exit fullscreen");
            stage.setTitle("NBDA - Naval Blood Donation Archive");
        } catch (IOException e) {
            throw new RuntimeException("Failed to open application shell.", e);
        }
    }
}