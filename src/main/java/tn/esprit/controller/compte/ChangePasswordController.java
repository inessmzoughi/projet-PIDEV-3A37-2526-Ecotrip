package tn.esprit.controller.compte;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import tn.esprit.models.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.repository.UserRepository;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.PasswordUtil;

public class ChangePasswordController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label currentPasswordError;
    @FXML private Label newPasswordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label successLabel;

    private final UserRepository userRepository = new UserRepository();

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
        }
    }

    @FXML
    private void handleSave() {
        clearErrors();

        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        boolean valid = true;

        if (current.isEmpty()) {
            showFieldError(currentPasswordError, "Le mot de passe actuel est requis.");
            valid = false;
        }
        if (newPass.length() < 6) {
            showFieldError(newPasswordError, "Minimum 6 caractères.");
            valid = false;
        }
        if (!newPass.equals(confirm)) {
            showFieldError(confirmPasswordError, "Les mots de passe ne correspondent pas.");
            valid = false;
        }
        if (!valid) return;

        User user = SessionManager.getInstance().getCurrentUser();

        // Verify current password
        if (!PasswordUtil.verify(current, user.getPassword())) {
            showFieldError(currentPasswordError, "Mot de passe actuel incorrect.");
            return;
        }

        // Hash and save new password
        user.setPassword(PasswordUtil.hash(newPass));
        try {
            userRepository.updatePassword(user);
            SessionManager.getInstance().setCurrentUser(user);
            showSuccess("Mot de passe modifié avec succès.");
            clearFields();
        } catch (Exception e) {
            showFieldError(currentPasswordError, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        SceneManager.navigateTo(
                isAdmin ? Routes.ADMIN_MON_COMPTE : Routes.FRONT_MON_COMPTE
        );
    }

    private void clearFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void showSuccess(String msg) {
        successLabel.setText("✅ " + msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private void showFieldError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        for (Label l : new Label[]{
                currentPasswordError, newPasswordError,
                confirmPasswordError, successLabel}) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }
}