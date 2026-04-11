package tn.esprit.controller.back.user;

import tn.esprit.models.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.repository.UserRepository;
import tn.esprit.session.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UserShowController {

    @FXML private Label detailUsername;
    @FXML private Label detailEmail;
    @FXML private Label detailAddress;
    @FXML private Label detailTelephone;
    @FXML private Label detailRole;
    @FXML private Label detailVerified;

    private final UserRepository userRepository = new UserRepository();
    private User user;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        user = SessionManager.getInstance().getPendingUser();
        if (user == null) {
            SceneManager.navigateTo(Routes.ADMIN_USERS);
            return;
        }

        detailUsername.setText(user.getUsername());
        detailEmail.setText(user.getEmail());
        detailAddress.setText(user.getAddress()   != null ? user.getAddress()   : "—");
        detailTelephone.setText(user.getTelephone() != null ? user.getTelephone() : "—");

        // Role badge
        detailRole.setText(user.getRoles().getLabel());
        detailRole.getStyleClass().add(
                user.getRoles().name().equals("ADMIN") ? "badge-admin" : "badge-user");

        // Verified badge
        detailVerified.setText(user.isVerified() ? "Oui" : "Non");
        detailVerified.getStyleClass().add(
                user.isVerified() ? "badge-verified-yes" : "badge-verified-no");
    }

    @FXML private void handleEdit() {
        SessionManager.getInstance().setPendingUser(user);
        SceneManager.navigateTo(Routes.ADMIN_USER_EDIT);
    }

    @FXML private void handleList() {
        SceneManager.navigateTo(Routes.ADMIN_USERS);
    }

    @FXML private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'utilisateur");
        confirm.setHeaderText("Supprimer " + user.getUsername() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                userRepository.delete(user.getId());
                SessionManager.getInstance().setFlashMessage(
                        "Utilisateur " + user.getUsername() + " supprimé.");
                SceneManager.navigateTo(Routes.ADMIN_USERS);
            }
        });
    }
}