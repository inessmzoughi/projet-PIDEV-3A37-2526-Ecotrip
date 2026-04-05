package org.example.controller.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.exception.AuthException;
import org.example.models.User;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.services.Auth_User.AuthService;
import org.example.session.SessionManager;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    private final AuthService authService=new AuthService();

    @FXML
    public void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Fill all the fields");
            return;
        }
        try {
            User user = authService.login(email, password);
            SessionManager.getInstance().setCurrentUser(user);
            SessionManager.getInstance().redirectAfterLogin();
        } catch (AuthException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        SceneManager.navigateTo(Routes.REGISTER);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
