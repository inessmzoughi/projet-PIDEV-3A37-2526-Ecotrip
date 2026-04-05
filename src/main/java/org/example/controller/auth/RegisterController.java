package org.example.controller.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.exception.EmailAlreadyExistsException;
import org.example.models.User;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.services.Auth_User.AuthService;
import org.example.session.SessionManager;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleRegister() {
        String username  = usernameField.getText().trim();
        String email     = emailField.getText().trim();
        String password  = passwordField.getText();
        String confirm   = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            errorLabel.setText("Username or Email address is required.");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            errorLabel.setText("Passwords do not match.");
            return;
        }

        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            errorLabel.setText("Password must be at least 8 characters.");
            return;
        }

        try {
            User user = authService.register(username, email, password);
            SessionManager.getInstance().setCurrentUser(user);
            SceneManager.navigateTo(Routes.ADMIN_DASHBOARD);
        } catch (EmailAlreadyExistsException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        SceneManager.navigateTo(Routes.LOGIN);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}