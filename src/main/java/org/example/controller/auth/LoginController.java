package org.example.controller.auth;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.exception.AuthException;
import org.example.models.User;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.services.Auth_User.AuthService;
import org.example.session.SessionManager;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      rememberMe;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;
    @FXML private Button        loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        clearMessages();

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // Disable button while processing
        loginButton.setDisable(true);
        loginButton.setText("Connexion en cours...");

        try {
            User user = authService.login(email, password);
            SessionManager.getInstance().setCurrentUser(user);
            SessionManager.getInstance().redirectAfterLogin();
        } catch (AuthException e) {
            showError(e.getMessage());
        } finally {
            loginButton.setDisable(false);
            loginButton.setText("Se connecter");
        }
    }

    @FXML
    private void handleRegister() {
        SceneManager.navigateTo(Routes.REGISTER);
    }

    @FXML
    private void handleForgotPassword() {
        // You can implement this later
        showError("Fonctionnalité à venir.");
    }

    // Called from RegisterController after successful registration
    public void showSuccessMessage(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}