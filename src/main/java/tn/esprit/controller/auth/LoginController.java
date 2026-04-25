package tn.esprit.controller.auth;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.exception.AuthException;
import tn.esprit.models.Auth_User.GoogleUserInfo;
import tn.esprit.models.Auth_User.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.Auth_User.AuthService;
import tn.esprit.services.Auth_User.Google.GoogleAuthService;
import tn.esprit.services.Auth_User.Google.GoogleLoginService;
import tn.esprit.session.SessionManager;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;
    @FXML private Button        loginButton;
    @FXML private Button        btnGoogleLogin;

    private final AuthService       authService       = new AuthService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();
    private final GoogleLoginService googleLoginService = new GoogleLoginService();

    /* ─── Standard login (unchanged) ─── */

    @FXML
    private void handleLogin() {
        clearMessages();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

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

    /* ─── Google login ─── */

    @FXML
    private void handleGoogleLogin() {
        clearMessages();
        btnGoogleLogin.setDisable(true);
        showInfo("Ouverture du navigateur Google...");

        Thread t = new Thread(() -> {
            try {
                GoogleUserInfo googleInfo = googleAuthService.authenticate();

                if (googleInfo == null || googleInfo.getEmail() == null
                        || googleInfo.getEmail().isEmpty()) {
                    Platform.runLater(() -> {
                        showError("Impossible de récupérer les informations Google.");
                        btnGoogleLogin.setDisable(false);  // ← re-enable
                    });
                    return;
                }

                User user = googleLoginService.findOrCreateUser(googleInfo);
                Platform.runLater(() -> {
                    SessionManager.getInstance().setCurrentUser(user);
                    SessionManager.getInstance().redirectAfterLogin();
                    // No need to re-enable — we're navigating away
                });

            } catch (java.util.concurrent.TimeoutException e) {
                Platform.runLater(() -> {
                    showError("Délai dépassé. Veuillez réessayer.");
                    btnGoogleLogin.setDisable(false);  // ← re-enable
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    // Show the actual error message, not "Erreur Google: ..."
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("Address already in use")) {
                        showError("Le service Google est occupé. Réessayez dans 2 secondes.");
                    } else {
                        showError("Erreur Google : " + msg);
                    }
                    btnGoogleLogin.setDisable(false);  // ← re-enable
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /* ─── Other handlers ─── */

    @FXML
    private void handleRegister() {
        SceneManager.navigateTo(Routes.REGISTER);
    }

    @FXML
    public void onForgotPassword(ActionEvent actionEvent) {
        SceneManager.navigateTo(Routes.FORGET_PASSWORD);
    }

    public void showSuccessMessage(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    /* ─── Helpers ─── */

    private void showError(String message) {
        errorLabel.setText("❌  " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showInfo(String message) {
        successLabel.setText("⏳  " + message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}