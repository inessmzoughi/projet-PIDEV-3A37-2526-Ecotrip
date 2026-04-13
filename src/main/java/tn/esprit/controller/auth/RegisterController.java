package tn.esprit.controller.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.esprit.exception.EmailAlreadyExistsException;
import tn.esprit.models.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.Auth_User.AuthService;
import tn.esprit.session.SessionManager;

import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         errorLabel;

    private final AuthService authService = new AuthService();

    // RFC 5322-inspired pattern — catches the vast majority of invalid addresses
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        // 1. Empty field check
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // 2. Email format check
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email address (e.g. name@example.com).");
            return;
        }

        // 3. Password length check
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }
        if (username.length() < 8) {
            showError("Username must be at least 8 characters.");
            return;
        }
        // 4. Password match check
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }


        try {
            User user = authService.register(username, email, password);
            SessionManager.getInstance().setCurrentUser(user);
            SceneManager.navigateTo(Routes.ADMIN_DASHBOARD);
        } catch (EmailAlreadyExistsException e) {
            showError("An account with this email already exists.");
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