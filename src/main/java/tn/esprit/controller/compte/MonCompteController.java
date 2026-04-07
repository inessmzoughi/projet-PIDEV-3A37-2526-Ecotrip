package tn.esprit.controller.compte;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import tn.esprit.models.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.session.SessionManager;

public class MonCompteController {

    @FXML private Label greetingLabel;
    @FXML private Label avatarInitials;
    @FXML private Circle avatarCircle;
    @FXML private Label emailValue;
    @FXML private Label usernameValue;
    @FXML private Label addressValue;
    @FXML private Label phoneValue;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();

        // Greeting
        greetingLabel.setText("Bonjour, " + user.getUsername() + " 👋");

        // Avatar initials
        String initials = String.valueOf(user.getUsername().charAt(0)).toUpperCase()
                + String.valueOf(user.getUsername().charAt(0)).toUpperCase();
        avatarInitials.setText(initials);

        // Info fields
        emailValue.setText(user.getEmail());
        usernameValue.setText(user.getUsername());
        addressValue.setText(
                user.getAddress() != null && !user.getAddress().isEmpty()
                        ? user.getAddress() : "Non renseignée"
        );
        phoneValue.setText(
                user.getTelephone() != null && !user.getTelephone().isEmpty()
                        ? user.getTelephone() : "Non renseigné"
        );
    }

    @FXML
    private void handleEditProfile() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        SceneManager.navigateTo(
                isAdmin ? Routes.ADMIN_UPDATE_ACCOUNT : Routes.FRONT_UPDATE_ACCOUNT
        );
    }

    @FXML
    private void handleChangePassword() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        SceneManager.navigateTo(
                isAdmin ? Routes.ADMIN_CHANGE_PASSWORD : Routes.FRONT_CHANGE_PASSWORD
        );
    }
}