package tn.esprit.controller.compte;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.esprit.models.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.repository.UserRepository;
import tn.esprit.session.SessionManager;

public class UpdateAccountController {

    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private Label successLabel;
    @FXML private Label emailError;
    @FXML private Label usernameError;

    private final UserRepository userRepository = new UserRepository();

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }
        populateFields();
    }

    private void populateFields() {
        User user = SessionManager.getInstance().getCurrentUser();
        emailField.setText(user.getEmail());
        usernameField.setText(user.getUsername());
        if (user.getAddress() != null) addressField.setText(user.getAddress());
        if (user.getTelephone()   != null) phoneField.setText(user.getTelephone());
    }

    @FXML
    private void handleSave() {
        clearErrors();

        String email    = emailField.getText().trim();
        String username = usernameField.getText().trim();

        if (email.isEmpty()) { showFieldError(emailError, "L'email est requis."); return; }
        if (username.isEmpty()) { showFieldError(usernameError, "Le nom est requis."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        user.setEmail(email);
        user.setUsername(username);
        user.setAddress(addressField.getText().trim());
        user.setTelephone(phoneField.getText().trim());

        try {
            userRepository.update(user);
            // Refresh session with updated user
            SessionManager.getInstance().setCurrentUser(user);
            showSuccess("Vos informations ont été mises à jour.");
        } catch (Exception e) {
            showSuccess("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    @FXML
    private void handleSelectPhoto() {
        // Will implement with FileChooser later
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choisir une photo");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png")
        );
        // chooser.showOpenDialog(primaryStage) — needs stage reference
        // Implement fully when integrating image upload
    }

    @FXML
    private void handleCancel() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        SceneManager.navigateTo(
                isAdmin ? Routes.ADMIN_MON_COMPTE : Routes.FRONT_MON_COMPTE
        );
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
        for (Label l : new Label[]{emailError, usernameError, successLabel}) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }
}