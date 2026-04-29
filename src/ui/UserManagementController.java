package ui;

import dao.AuthDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import model.UserAccount;

import java.util.List;

public class UserManagementController {

    private final AuthDAO authDAO = new AuthDAO();

    @FXML private TableView<UserAccount> usersTable;
    @FXML private TableColumn<UserAccount, String> staffIdCol;
    @FXML private TableColumn<UserAccount, String> nameCol;
    @FXML private TableColumn<UserAccount, String> usernameCol;
    @FXML private TableColumn<UserAccount, String> roleCol;
    @FXML private TableColumn<UserAccount, String> statusCol;
    @FXML private TextField searchUserField;
    @FXML private Label activeUsersCount;
    @FXML private Label adminCount;
    @FXML private Button refreshButton;

    @FXML
    public void initialize() {
        setupTable();
        loadUsers();
        updateStats();
    }

    private javafx.stage.Window getParentWindow() {
        return usersTable.getScene() != null ? usersTable.getScene().getWindow() : null;
    }

    private void setupTable() {
        staffIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStaffId() != null ? data.getValue().getStaffId() : ""));
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDisplayName()));
        usernameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        roleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRole()));
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isActive() ? "Active" : "Inactive"));
    }

    private void loadUsers() {
        List<UserAccount> users = authDAO.getAllUsers();
        ObservableList<UserAccount> observableUsers = FXCollections.observableArrayList(users);
        usersTable.setItems(observableUsers);
    }

    @FXML
    private void refreshUsers() {
        loadUsers();
        updateStats();
    }

    private void updateStats() {
        if (activeUsersCount == null || adminCount == null) return;
        int active = 0;
        int admins = 0;
        for (UserAccount u : usersTable.getItems()) {
            if (u.isActive()) active++;
            if ("ADMIN".equalsIgnoreCase(u.getRole())) admins++;
        }
        activeUsersCount.setText(String.valueOf(active));
        adminCount.setText(String.valueOf(admins));
    }

    @FXML
    private void showAddUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(getParentWindow());
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create a new staff or admin account");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("STAFF", "ADMIN");
        roleComboBox.setValue("STAFF");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("First Name:"), 0, 2);
        grid.add(firstNameField, 1, 2);
        grid.add(new Label("Last Name:"), 0, 3);
        grid.add(lastNameField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleComboBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText();
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String role = roleComboBox.getValue();

                if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                    showAlert("Error", "All fields are required.", Alert.AlertType.ERROR);
                    return;
                }

                if (authDAO.usernameExists(username)) {
                    showAlert("Error", "Username already exists.", Alert.AlertType.ERROR);
                    return;
                }

                if (authDAO.createUser(username, password, firstName, lastName, role)) {
                    showAlert("Success", "User created successfully.", Alert.AlertType.INFORMATION);
                    refreshUsers();
                } else {
                    showAlert("Error", "Failed to create user.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showChangePasswordDialog(UserAccount user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(getParentWindow());
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Change password for " + user.getUsername());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();

        grid.add(new Label("New Password:"), 0, 0);
        grid.add(newPasswordField, 1, 0);
        grid.add(new Label("Confirm Password:"), 0, 1);
        grid.add(confirmPasswordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();

                if (newPassword.isEmpty()) {
                    showAlert("Error", "Password cannot be empty.", Alert.AlertType.ERROR);
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    showAlert("Error", "Passwords do not match.", Alert.AlertType.ERROR);
                    return;
                }

                if (authDAO.changePassword(user.getUserId(), newPassword)) {
                    showAlert("Success", "Password changed successfully.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to change password.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void deactivateUser(UserAccount user) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.initOwner(getParentWindow());
        confirmAlert.setTitle("Confirm Deactivation");
        confirmAlert.setHeaderText("Deactivate " + user.getUsername());
        confirmAlert.setContentText("Are you sure you want to deactivate this account?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (authDAO.deactivateUser(user.getUserId())) {
                    showAlert("Success", "User deactivated successfully.", Alert.AlertType.INFORMATION);
                    refreshUsers();
                } else {
                    showAlert("Error", "Failed to deactivate user.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.initOwner(getParentWindow());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
