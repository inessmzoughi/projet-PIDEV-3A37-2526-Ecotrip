package tn.esprit.controller.compte;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
<<<<<<< HEAD
import tn.esprit.models.Auth_User.User;
=======
>>>>>>> eea25a70bee82bd33125413eb8e90597aaad968e
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.Auth_User.UserService;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.PasswordUtil;
<<<<<<< HEAD
import javafx.animation.AnimationTimer;
import javafx.scene.image.WritableImage;
import org.opencv.core.Mat;
import tn.esprit.services.Auth_User.FaceRecognition.FaceEnrollmentService;   // new HF-based one
import tn.esprit.services.Auth_User.FaceRecognition.WebcamService;
import tn.esprit.utils.FaceDescriptorUtil;
=======

>>>>>>> eea25a70bee82bd33125413eb8e90597aaad968e
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MonCompteController implements Initializable {

    /* ─── Avatar / info ─── */
    @FXML private Circle    avatarCircle;
    @FXML private Label     avatarInitials;
    @FXML private ImageView avatarPhoto;       // overlaid on the circle when a photo exists
    @FXML private Label     profileUsername, profileEmail;
    @FXML private Label     roleValue, verifiedValue;
    @FXML private Label     addressValue, phoneValue;

    /* ─── Edit form ─── */
    @FXML private VBox      editFormPanel;
    @FXML private TextField usernameField, emailField, addressField, phoneField;
    @FXML private Label     errUsername, errEmail;
    @FXML private Label     editSuccessLabel;

    /* ─── Photo (edit form) ─── */
    @FXML private ImageView photoPreview;
    @FXML private Label     photoLabel;
    private String selectedPhotoPath = null;

    /* ─── Password form ─── */
    @FXML private VBox          passwordFormPanel;
    @FXML private PasswordField currentPasswordField, newPasswordField, confirmPasswordField;
    @FXML private Label         errCurrentPassword, errNewPassword, errConfirmPassword;
    @FXML private Label         passSuccessLabel;

    /* ─── Toast ─── */
    @FXML private Label toastLabel;

    private final UserService service = new UserService();
<<<<<<< HEAD
    @FXML private VBox      faceFormPanel;
    @FXML private Label     faceSuccessLabel, faceErrorLabel, faceStatusLabel;
    @FXML private javafx.scene.image.ImageView webcamView;
    @FXML private VBox      webcamPlaceholder;
    @FXML private Label     faceDetectedLabel;
    @FXML private Button    btnStartCamera, btnCaptureFace, btnStopCamera;

    private final WebcamService        webcamService     = new WebcamService();
    private final FaceEnrollmentService enrollmentService = new FaceEnrollmentService();
    private AnimationTimer cameraTimer;
=======

>>>>>>> eea25a70bee82bd33125413eb8e90597aaad968e
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }
        loadProfile();
        showPanel(editFormPanel);
        hidePanel(passwordFormPanel);
<<<<<<< HEAD
        if (SessionManager.getInstance().shouldOpenFacePanelOnLoad()) {
            SessionManager.getInstance().clearOpenFacePanel();
            showFaceForm(); // opens the face panel directly
        }
=======
>>>>>>> eea25a70bee82bd33125413eb8e90597aaad968e
    }

    /* ─── Load profile info ─── */
    private void loadProfile() {
        User user = SessionManager.getInstance().getCurrentUser();

        // ── Avatar: try photo first, fall back to initials ──
        String photoPath = user.getImage();
        if (photoPath != null && !photoPath.isEmpty()) {
            try {
                File f = new File(photoPath);
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString(), 104, 104, true, true);
                    avatarPhoto.setImage(img);
                    avatarPhoto.setVisible(true);
                    avatarPhoto.setManaged(true);
                    avatarInitials.setVisible(false);
                    avatarInitials.setManaged(false);
                    avatarCircle.setVisible(false);
                    avatarCircle.setManaged(false);
                } else {
                    showInitialsAvatar(user);
                }
            } catch (Exception e) {
                showInitialsAvatar(user);
            }
        } else {
            showInitialsAvatar(user);
        }

        profileUsername.setText(user.getUsername());
        profileEmail.setText(user.getEmail());

        addressValue.setText(
                user.getAddress() != null && !user.getAddress().isEmpty()
                        ? user.getAddress() : "Non renseignée");
        phoneValue.setText(
                user.getTelephone() != null && !user.getTelephone().isEmpty()
                        ? user.getTelephone() : "Non renseigné");

        // Role badge
        boolean isAdmin = user.getRoles().name().equals("ROLE_ADMIN");
        roleValue.setText(isAdmin ? "Admin" : "Utilisateur");
        roleValue.getStyleClass().removeAll("badge-admin", "badge-user");
        roleValue.getStyleClass().add(isAdmin ? "badge-admin" : "badge-user");

        // Verified badge
        verifiedValue.setText(user.isVerified() ? "✅ Vérifié" : "❌ Non vérifié");
        verifiedValue.getStyleClass().removeAll("badge-actif", "badge-inactif");
        verifiedValue.getStyleClass().add(user.isVerified() ? "badge-actif" : "badge-inactif");

        // Pre-fill edit form fields
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        addressField.setText(user.getAddress() != null ? user.getAddress() : "");
        phoneField.setText(user.getTelephone() != null ? user.getTelephone() : "");

        // Pre-fill photo preview in edit form
        selectedPhotoPath = photoPath;
        if (photoPath != null && !photoPath.isEmpty()) {
            try {
                File f = new File(photoPath);
                if (f.exists()) {
                    photoPreview.setImage(new Image(f.toURI().toString(), 72, 72, true, true));
                    photoLabel.setText("✅ " + f.getName());
                } else {
                    photoPreview.setImage(null);
                    photoLabel.setText("Aucune photo sélectionnée");
                }
            } catch (Exception ex) {
                photoPreview.setImage(null);
                photoLabel.setText("Aucune photo sélectionnée");
            }
        } else {
            photoPreview.setImage(null);
            photoLabel.setText("Aucune photo sélectionnée");
        }
    }

    /** Show circle + initials when no photo is available */
    private void showInitialsAvatar(User user) {
        avatarPhoto.setVisible(false);
        avatarPhoto.setManaged(false);
        avatarCircle.setVisible(true);
        avatarCircle.setManaged(true);
        avatarInitials.setVisible(true);
        avatarInitials.setManaged(true);

        String name = user.getUsername().trim();
        String[] parts = name.split("\\s+", 2);
        String initials = parts.length >= 2
                ? "" + Character.toUpperCase(parts[0].charAt(0)) + Character.toUpperCase(parts[1].charAt(0))
                : name.length() >= 2 ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();
        avatarInitials.setText(initials);
    }

    /* ─── Photo chooser ─── */
    @FXML
    private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );
        Stage stage = (Stage) usernameField.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPhotoPath = file.getAbsolutePath();
            photoPreview.setImage(new Image(file.toURI().toString(), 72, 72, true, true));
            photoLabel.setText("✅ " + file.getName());
        }
    }

    /* ─── Panel toggles ─── */
    @FXML public void showEditForm()     { showPanel(editFormPanel);    hidePanel(passwordFormPanel); clearEditErrors(); }
    @FXML public void showPasswordForm() { showPanel(passwordFormPanel); hidePanel(editFormPanel);    clearPasswordErrors(); }
    @FXML public void cancelEdit()       { showEditForm(); }
    @FXML public void cancelPassword()   { showPasswordForm(); }

    private void showPanel(VBox panel) { panel.setVisible(true);  panel.setManaged(true); }
    private void hidePanel(VBox panel) { panel.setVisible(false); panel.setManaged(false); }

    /* ─── Save profile ─── */
    @FXML
    private void handleSaveProfile() {
        clearEditErrors();
        boolean ok = true;

        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();

        if (username.isEmpty()) {
            showFieldError(errUsername, "Nom d'utilisateur requis."); ok = false;
        }
        if (email.isEmpty() || !email.contains("@")) {
            showFieldError(errEmail, "Email invalide."); ok = false;
        }
        if (!ok) return;

        User user = SessionManager.getInstance().getCurrentUser();
        try {
            service.updateUser(
                    user.getId(), username, email,
                    addressField.getText().trim(),
                    phoneField.getText().trim(),
                    user.getRoles().name(),
                    user.isVerified(),
                    null,           // no password change here
                    selectedPhotoPath
            );
            // Refresh session
            user.setUsername(username);
            user.setEmail(email);
            user.setAddress(addressField.getText().trim());
            user.setTelephone(phoneField.getText().trim());
            user.setImage(selectedPhotoPath);
            SessionManager.getInstance().setCurrentUser(user);

            loadProfile();
            showFieldSuccess(editSuccessLabel, "✅ Profil mis à jour avec succès.");
        } catch (RuntimeException e) {
            showFieldError(errUsername, "Erreur : " + e.getMessage());
        }
    }

    /* ─── Save password ─── */
    @FXML
    private void handleSavePassword() {
        clearPasswordErrors();
        boolean ok = true;

        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isEmpty()) {
            showFieldError(errCurrentPassword, "Mot de passe actuel requis."); ok = false;
        }
        if (newPass.length() < 6) {
            showFieldError(errNewPassword, "Minimum 6 caractères."); ok = false;
        }
        if (!newPass.equals(confirm)) {
            showFieldError(errConfirmPassword, "Les mots de passe ne correspondent pas."); ok = false;
        }
        if (!ok) return;

        User user = SessionManager.getInstance().getCurrentUser();
        if (!PasswordUtil.verify(current, user.getPassword())) {
            showFieldError(errCurrentPassword, "Mot de passe actuel incorrect.");
            return;
        }

        try {
            service.updateUser(
                    user.getId(), user.getUsername(), user.getEmail(),
                    user.getAddress(), user.getTelephone(),
                    user.getRoles().name(), user.isVerified(),
                    newPass,
                    user.getImage()
            );
            user.setPassword(PasswordUtil.hash(newPass));
            SessionManager.getInstance().setCurrentUser(user);

            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            showFieldSuccess(passSuccessLabel, "✅ Mot de passe modifié avec succès.");
        } catch (RuntimeException e) {
            showFieldError(errCurrentPassword, "Erreur : " + e.getMessage());
        }
    }

    /* ─── Validation ─── */
    @FXML private void validateUsername() {
        setInputError(usernameField, errUsername, usernameField.getText().trim().isEmpty());
    }
    @FXML private void validateEmail() {
        String e = emailField.getText().trim();
        setInputError(emailField, errEmail, e.isEmpty() || !e.contains("@"));
    }

    /* ─── Navigation ─── */
    @FXML private void onNavDashboard() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        SceneManager.navigateTo(isAdmin ? Routes.ADMIN_DASHBOARD : Routes.HOME);
    }

    /* ─── Helpers ─── */
    private void setInputError(Control field, Label errLabel, boolean hasError) {
        if (hasError) {
            if (!field.getStyleClass().contains("form-input-error"))
                field.getStyleClass().add("form-input-error");
            errLabel.setVisible(true); errLabel.setManaged(true);
        } else {
            field.getStyleClass().remove("form-input-error");
            errLabel.setVisible(false); errLabel.setManaged(false);
        }
    }

    private void showFieldError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true); label.setManaged(true);
    }

    private void showFieldSuccess(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true); label.setManaged(true);
        new Thread(() -> {
            try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> { label.setVisible(false); label.setManaged(false); });
        }).start();
    }

    private void clearEditErrors() {
        for (Label l : new Label[]{errUsername, errEmail, editSuccessLabel}) {
            l.setVisible(false); l.setManaged(false);
        }
        usernameField.getStyleClass().remove("form-input-error");
        emailField.getStyleClass().remove("form-input-error");
    }

    private void clearPasswordErrors() {
        for (Label l : new Label[]{errCurrentPassword, errNewPassword, errConfirmPassword, passSuccessLabel}) {
            l.setVisible(false); l.setManaged(false);
        }
    }
<<<<<<< HEAD

    // ── Panel toggle (same as before) ──
    @FXML public void showFaceForm() {
        showPanel(faceFormPanel);
        hidePanel(editFormPanel);
        hidePanel(passwordFormPanel);
        clearFaceMessages();
        updateFaceStatus();
    }
    @FXML public void cancelFace() {
        stopCamera();
        showPanel(editFormPanel);
        hidePanel(faceFormPanel);
    }

    private void updateFaceStatus() {
        String desc = SessionManager.getInstance().getCurrentUser().getFaceDescriptor();
        boolean enrolled = FaceDescriptorUtil.isEnrolled(desc);
        faceStatusLabel.setText(enrolled
                ? "✅ Visage enregistré — vous pouvez le mettre à jour."
                : "❌ Aucun visage enregistré.");
        faceStatusLabel.setStyle(enrolled
                ? "-fx-text-fill:#166534;" : "-fx-text-fill:#991b1b;");
    }

    // ── Camera lifecycle (same as before) ──
    @FXML public void onStartCamera() {
        if (!webcamService.start()) {
            showFaceError("Impossible d'accéder à la caméra.");
            return;
        }
        webcamView.setVisible(true);    webcamView.setManaged(true);
        webcamPlaceholder.setVisible(false); webcamPlaceholder.setManaged(false);
        btnStartCamera.setDisable(true);
        btnCaptureFace.setDisable(false);
        btnStopCamera.setDisable(false);

        final long[] lastFrameTime = {0};
        cameraTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now - lastFrameTime[0] < 66_000_000) return;
                lastFrameTime[0] = now;
                Mat frame = webcamService.grabFrame();
                if (frame == null) return;
                WritableImage wi = webcamService.matToWritableImage(frame);
                webcamView.setImage(wi);
            }
        };
        cameraTimer.start();
    }

    @FXML public void onStopCamera() { stopCamera(); }

    private void stopCamera() {
        if (cameraTimer != null) { cameraTimer.stop(); cameraTimer = null; }
        webcamService.stop();
        webcamView.setVisible(false);    webcamView.setManaged(false);
        webcamPlaceholder.setVisible(true); webcamPlaceholder.setManaged(true);
        btnStartCamera.setDisable(false);
        btnCaptureFace.setDisable(true);
        btnStopCamera.setDisable(true);
        faceDetectedLabel.setVisible(false); faceDetectedLabel.setManaged(false);
    }

    // ── Capture + enroll — now calls HuggingFace API ──
    @FXML public void onCaptureFace() {
        clearFaceMessages();
        Mat frame = webcamService.grabFrame();
        if (frame == null) {
            showFaceError("Impossible de capturer une image. Réessayez.");
            return;
        }

        btnCaptureFace.setDisable(true);
        btnCaptureFace.setText("⏳  Envoi à l'API...");

        // Run HuggingFace API call off UI thread — it can take 2-5 seconds
        Thread t = new Thread(() -> {
            try {
                boolean success = enrollmentService.enrollFace(frame);

                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        faceDetectedLabel.setVisible(true);
                        faceDetectedLabel.setManaged(true);
                        showFaceSuccess(
                                "✅ Visage enregistré avec succès ! "
                                        + "Vous pouvez maintenant utiliser la connexion faciale.");
                        updateFaceStatus();
                        stopCamera();
                    } else {
                        showFaceError(
                                "❌ L'API n'a pas pu extraire un visage. "
                                        + "Assurez-vous d'être bien face à la caméra et réessayez.");
                    }
                    btnCaptureFace.setText("📸  Capturer et enregistrer");
                    btnCaptureFace.setDisable(false);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showFaceError("Erreur API : " + e.getMessage());
                    btnCaptureFace.setText("📸  Capturer et enregistrer");
                    btnCaptureFace.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Helpers (same as before) ──
    private void showFaceSuccess(String msg) {
        faceSuccessLabel.setText(msg);
        faceSuccessLabel.setVisible(true); faceSuccessLabel.setManaged(true);
        faceErrorLabel.setVisible(false);  faceErrorLabel.setManaged(false);
    }
    private void showFaceError(String msg) {
        faceErrorLabel.setText(msg);
        faceErrorLabel.setVisible(true); faceErrorLabel.setManaged(true);
        faceSuccessLabel.setVisible(false); faceSuccessLabel.setManaged(false);
    }
    private void clearFaceMessages() {
        faceSuccessLabel.setVisible(false); faceSuccessLabel.setManaged(false);
        faceErrorLabel.setVisible(false);   faceErrorLabel.setManaged(false);
    }
=======
>>>>>>> eea25a70bee82bd33125413eb8e90597aaad968e
}