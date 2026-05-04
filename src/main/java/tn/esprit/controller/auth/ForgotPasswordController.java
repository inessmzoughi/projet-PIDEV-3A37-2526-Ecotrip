package tn.esprit.controller.auth;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.Auth_User.ForgetPassword.PasswordResetService;

import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    /* ─── Step 1: email ─── */
    @FXML private VBox      step1Box;
    @FXML private TextField emailField;
    @FXML private Label     errEmail;
    @FXML private Button    btnSendCode;

    /* ─── Step 2: token + new password ─── */
    @FXML private VBox          step2Box;
    @FXML private TextField     tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         errToken;
    @FXML private Label         errNewPassword;
    @FXML private Label         errConfirmPassword;
    @FXML private Button        btnResetPassword;

    /* ─── Feedback ─── */
    @FXML private VBox  successBox;
    @FXML private Label successDetail;
    @FXML private Label errorLabel;
    @FXML private VBox notFoundBox;

    private final PasswordResetService service = new PasswordResetService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Start on step 1
        showStep(1);
    }

    /* ════════════════════════════════════════════════
       STEP 1 — Send code
    ════════════════════════════════════════════════ */

    @FXML
    private void onSendCode() {
        clearErrors();
        String email = emailField.getText().trim();

        if (!email.contains("@")) {
            showFieldError(errEmail, "Veuillez entrer une adresse email valide.");
            return;
        }

        btnSendCode.setDisable(true);
        btnSendCode.setText("⏳  Vérification...");

        Thread t = new Thread(() -> {
            try {
                service.requestReset(email);

                // Email found + sent
                javafx.application.Platform.runLater(() -> {
                    successBox.setVisible(true);
                    successBox.setManaged(true);
                    successDetail.setText(
                            "Un code a été envoyé à " + email
                                    + ". Vérifiez votre boite mail.");
                    showStep(2);
                    btnSendCode.setText("📨  Envoyer le code");
                    btnSendCode.setDisable(false);
                });

            } catch (IllegalArgumentException e) {
                // Email not found in DB
                javafx.application.Platform.runLater(() -> {
                    showEmailNotFound(email);
                    btnSendCode.setText("📨  Envoyer le code");
                    btnSendCode.setDisable(false);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Erreur d'envoi : " + e.getMessage()
                            + "\nVérifiez votre connexion internet.");
                    btnSendCode.setText("📨  Envoyer le code");
                    btnSendCode.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }
    private void showEmailNotFound(String email) {
        // Hide any previous success
        successBox.setVisible(false);
        successBox.setManaged(false);

        // Show the not-found error box
        errorLabel.setText(
                "❌  Aucun compte trouvé pour « " + email + " ».\n\n"
                        + "Vérifiez l'adresse saisie ou créez un nouveau compte."
        );
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Show the register button
        notFoundBox.setVisible(true);
        notFoundBox.setManaged(true);
    }
    /* ════════════════════════════════════════════════
       STEP 2 — Reset password
    ════════════════════════════════════════════════ */

    @FXML
    private void onResetPassword() {
        clearErrors();

        String token    = tokenField.getText().trim();
        String newPass  = newPasswordField.getText();
        String confirm  = confirmPasswordField.getText();

        boolean ok = true;

        if (token.isEmpty()) {
            showFieldError(errToken, "Veuillez coller le code reçu par email.");
            ok = false;
        }
        if (newPass.length() < 6) {
            showFieldError(errNewPassword, "Minimum 6 caractères.");
            ok = false;
        }
        if (!newPass.equals(confirm)) {
            showFieldError(errConfirmPassword,
                    "Les mots de passe ne correspondent pas.");
            ok = false;
        }
        if (!ok) return;

        btnResetPassword.setDisable(true);
        btnResetPassword.setText("⏳  Réinitialisation...");

        Thread t = new Thread(() -> {
            try {
                service.resetPassword(token, newPass);

                Platform.runLater(() -> {
                    // Success — show message then redirect to login after 2 seconds
                    showSuccess(
                            "✅ Mot de passe réinitialisé avec succès !",
                            "Vous allez être redirigé vers la connexion...");

                    // Navigate to login after 2 seconds
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() ->
                                SceneManager.navigateTo(Routes.LOGIN));
                    }).start();
                });

            } catch (IllegalArgumentException e) {
                // Token invalid/expired or validation error
                Platform.runLater(() -> {
                    if (e.getMessage().contains("expiré")
                            || e.getMessage().contains("invalide")) {
                        showFieldError(errToken, e.getMessage());
                    } else {
                        showError(e.getMessage());
                    }
                    btnResetPassword.setText("🔒  Réinitialiser le mot de passe");
                    btnResetPassword.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Erreur : " + e.getMessage());
                    btnResetPassword.setText("🔒  Réinitialiser le mot de passe");
                    btnResetPassword.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onResendCode() {
        // Go back to step 1 so user can request a new code
        clearErrors();
        successBox.setVisible(false);
        successBox.setManaged(false);
        tokenField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showStep(1);
    }

    /* ════════════════════════════════════════════════
       NAVIGATION
    ════════════════════════════════════════════════ */

    @FXML
    private void onBackToLogin() {
        SceneManager.navigateTo(Routes.LOGIN);
    }

    /* ════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════ */

    private void showStep(int step) {
        step1Box.setVisible(step == 1); step1Box.setManaged(step == 1);
        step2Box.setVisible(step == 2); step2Box.setManaged(step == 2);
    }

    private void showSuccess(String title, String detail) {
        successBox.setVisible(true);   successBox.setManaged(true);
        successDetail.setText(title + "\n" + detail);
        errorLabel.setVisible(false);  errorLabel.setManaged(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);  errorLabel.setManaged(true);
        successBox.setVisible(false); successBox.setManaged(false);
    }

    private void showFieldError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true); label.setManaged(true);
    }

    private void clearErrors() {
        errorLabel.setVisible(false);   errorLabel.setManaged(false);
        notFoundBox.setVisible(false);  notFoundBox.setManaged(false);  // ← add this line
        for (Label l : new Label[]{errEmail, errToken,
                errNewPassword, errConfirmPassword}) {
            l.setVisible(false); l.setManaged(false);
        }
    }
    @FXML
    private void onGoToRegister() {
        SceneManager.navigateTo(Routes.REGISTER);
    }
}