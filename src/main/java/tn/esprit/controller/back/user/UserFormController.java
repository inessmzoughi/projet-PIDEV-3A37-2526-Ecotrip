package tn.esprit.controller.back.user;

import tn.esprit.models.User;
import tn.esprit.models.enums.Role;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.repository.UserRepository;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.PasswordUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UserFormController {

    @FXML private Label         formTitleLabel;
    @FXML private TextField     emailField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         passwordHint;
    @FXML private TextField     addressField;
    @FXML private TextField     telephoneField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private CheckBox      isVerifiedCheck;
    @FXML private Button        saveButton;
    @FXML private Button        viewButton;
    @FXML private Label         successLabel;
    @FXML private Label         errorLabel;
    @FXML private Label         emailError;
    @FXML private Label         usernameError;
    @FXML private Label         passwordError;

    private final UserRepository userRepository = new UserRepository();
    private User editingUser; // null = create mode, non-null = edit mode

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        roleCombo.getItems().addAll(Role.values());
        roleCombo.setValue(Role.ROLE_USER);

        editingUser = SessionManager.getInstance().getPendingUser();

        if (editingUser != null) {
            // EDIT MODE
            formTitleLabel.setText("✏  Modifier " + editingUser.getUsername());
            saveButton.setText("💾  Enregistrer les modifications");
            passwordHint.setVisible(true);
            passwordHint.setManaged(true);
            viewButton.setVisible(true);
            viewButton.setManaged(true);
            populateFields(editingUser);
        } else {
            // CREATE MODE
            formTitleLabel.setText("➕  Créer un nouvel utilisateur");
            saveButton.setText("💾  Créer");
        }
    }

    private void populateFields(User user) {
        emailField.setText(user.getEmail());
        usernameField.setText(user.getUsername());
        if (user.getAddress()   != null) addressField.setText(user.getAddress());
        if (user.getTelephone() != null) telephoneField.setText(user.getTelephone());
        roleCombo.setValue(user.getRoles());
        isVerifiedCheck.setSelected(user.isVerified());
    }

    @FXML
    private void handleSave() {
        clearErrors();
        if (!validate()) return;

        if (editingUser == null) {
            createUser();
        } else {
            updateUser();
        }
    }

    private void createUser() {
        User user = new User();
        user.setEmail(emailField.getText().trim());
        user.setUsername(usernameField.getText().trim());
        user.setPassword(PasswordUtil.hash(passwordField.getText()));
        user.setAddress(addressField.getText().trim());
        user.setTelephone(telephoneField.getText().trim());
        user.setRoles(roleCombo.getValue());
        user.setIsVerified(isVerifiedCheck.isSelected());

        try {
            userRepository.save(user);
            SessionManager.getInstance().setFlashMessage(
                    "Utilisateur " + user.getUsername() + " créé avec succès.");
            SceneManager.navigateTo(Routes.ADMIN_USERS);
        } catch (Exception e) {
            showError("Erreur lors de la création : " + e.getMessage());
        }
    }

    private void updateUser() {
        editingUser.setEmail(emailField.getText().trim());
        editingUser.setUsername(usernameField.getText().trim());
        editingUser.setAddress(addressField.getText().trim());
        editingUser.setTelephone(telephoneField.getText().trim());
        editingUser.setRoles(roleCombo.getValue());
        editingUser.setIsVerified(isVerifiedCheck.isSelected());

        // Only update password if a new one was entered
        String newPass = passwordField.getText();
        if (!newPass.isEmpty()) {
            editingUser.setPassword(PasswordUtil.hash(newPass));
            userRepository.updatePassword(editingUser);
        }

        try {
            userRepository.update(editingUser);
            SessionManager.getInstance().setFlashMessage(
                    "Utilisateur " + editingUser.getUsername() + " mis à jour.");
            SceneManager.navigateTo(Routes.ADMIN_USERS);
        } catch (Exception e) {
            showError("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        SceneManager.navigateTo(Routes.ADMIN_USERS);
    }

    @FXML
    private void handleView() {
        if (editingUser != null) {
            SessionManager.getInstance().setPendingUser(editingUser);
            SceneManager.navigateTo(Routes.ADMIN_USER_SHOW);
        }
    }

    private boolean validate() {
        boolean valid = true;
        String email    = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (!email.contains("@")) {
            showFieldError(emailError, "Email invalide."); valid = false;
        }
        if (username.isEmpty()) {
            showFieldError(usernameError, "Nom d'utilisateur requis."); valid = false;
        }
        // Password required only on create
        if (editingUser == null && password.isEmpty()) {
            showFieldError(passwordError, "Mot de passe requis."); valid = false;
        }
        if (!password.isEmpty() && password.length() < 6) {
            showFieldError(passwordError, "Minimum 6 caractères."); valid = false;
        }
        return valid;
    }

    private void showError(String msg) {
        errorLabel.setText("❌ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showFieldError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        for (Label l : new Label[]{errorLabel, successLabel, emailError, usernameError, passwordError}) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }
}