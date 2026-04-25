package tn.esprit.services.Auth_User.FaceRecognition;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.opencv.core.Mat;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.FaceDescriptorUtil;

public class FaceGate {

    private final WebcamService           webcam       = new WebcamService();
    private final FaceVerificationService  verification = new FaceVerificationService();
    private boolean result = false;

    /**
     * Full gate check — call this from every protected route.
     *
     * Returns:
     *   PASS         → allow navigation (enrolled + verified, or not enrolled)
     *   FAIL         → block navigation (enrolled but verification failed)
     *   NOT_ENROLLED → allow navigation but caller should show nudge
     */
    public enum GateResult { PASS, FAIL, NOT_ENROLLED }

    public static GateResult check() {
        if (!isEnrolled()) {
            return GateResult.NOT_ENROLLED;
        }
        boolean verified = verify();
        return verified ? GateResult.PASS : GateResult.FAIL;
    }
    /**
     * Check if current user has enrolled their face.
     * Mirrors PHP: $user->getFaceDescriptor() !== null
     */
    public static boolean isEnrolled() {
        String desc = SessionManager.getInstance()
                .getCurrentUser().getFaceDescriptor();
        return FaceDescriptorUtil.isEnrolled(desc);
    }

    /**
     * Show face verification dialog.
     * Mirrors PHP FaceGateController — blocks until verified or cancelled.
     *
     * @return true = verified (allow navigation), false = rejected or cancelled
     */
    public static boolean verify() {
        return new FaceGate().showDialog();
    }

    private boolean showDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        // ── UI components ─────────────────────────────────────────────────────

        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(400);
        cameraView.setFitHeight(270);
        cameraView.setPreserveRatio(true);
        cameraView.setStyle(
                "-fx-background-color:#0f172a;"
                        + "-fx-background-radius:12;"
        );

        Label titleLabel = new Label("🔐  Vérification faciale");
        titleLabel.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#0f172a;"
        );

        Label subLabel = new Label(
                "Positionnez-vous face à la caméra puis cliquez Vérifier.");
        subLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#475569;");
        subLabel.setWrapText(true);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;");
        statusLabel.setWrapText(true);

        Button btnVerify = new Button("✅  Vérifier mon identité");
        btnVerify.setStyle(
                "-fx-background-color:#1a5f2a;-fx-text-fill:white;"
                        + "-fx-font-weight:bold;-fx-background-radius:10;"
                        + "-fx-font-size:13px;-fx-padding:10 24;"
                        + "-fx-cursor:hand;-fx-border-width:0;"
        );

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(
                "-fx-background-color:#f1f5f9;-fx-text-fill:#475569;"
                        + "-fx-background-radius:10;-fx-font-size:13px;"
                        + "-fx-padding:10 20;-fx-cursor:hand;-fx-border-width:0;"
        );

        HBox buttons = new HBox(12, btnVerify, btnCancel);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(16,
                titleLabel, subLabel, cameraView,
                statusLabel, buttons
        );
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setMaxWidth(460);
        root.setStyle(
                "-fx-background-color:white;"
                        + "-fx-background-radius:20;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),30,0,0,8);"
        );

        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.55);");

        // ── Start camera stream ───────────────────────────────────────────────

        boolean cameraStarted = webcam.start();
        if (!cameraStarted) {
            statusLabel.setText("❌ Impossible d'accéder à la caméra.");
        }

        final long[] lastTime = {0};
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now - lastTime[0] < 66_000_000) return; // ~15fps
                lastTime[0] = now;
                Mat frame = webcam.grabFrame();
                if (frame == null) return;
                WritableImage wi = webcam.matToWritableImage(frame);
                cameraView.setImage(wi);
            }
        };
        if (cameraStarted) timer.start();

        // ── Verify button action ──────────────────────────────────────────────

        btnVerify.setOnAction(e -> {
            Mat frame = webcam.grabFrame();
            if (frame == null) {
                statusLabel.setText("❌ Impossible de capturer une image. Réessayez.");
                return;
            }

            // Disable UI during API call
            btnVerify.setDisable(true);
            btnVerify.setText("⏳  Envoi à l'API HuggingFace...");
            statusLabel.setText("Analyse en cours...");

            // Run API call off UI thread — takes 2–5 seconds
            Thread t = new Thread(() -> {
                try {
                    boolean ok = verification.verifyCurrentUser(frame);

                    Platform.runLater(() -> {
                        if (ok) {
                            // Face matched — close dialog and allow navigation
                            result = true;
                            timer.stop();
                            webcam.stop();
                            dialog.close();
                        } else {
                            statusLabel.setText(
                                    "❌ Visage non reconnu. "
                                            + "Repositionnez-vous et réessayez."
                            );
                            btnVerify.setDisable(false);
                            btnVerify.setText("✅  Vérifier mon identité");
                        }
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Erreur : " + ex.getMessage());
                        btnVerify.setDisable(false);
                        btnVerify.setText("✅  Vérifier mon identité");
                    });
                }
            });
            t.setDaemon(true);
            t.start();
        });

        // ── Cancel action ─────────────────────────────────────────────────────

// In showDialog() — replace the btnCancel action:
        btnCancel.setOnAction(e -> {
            result = false;
            timer.stop();
            webcam.stop();
            dialog.close();

            // Show alert so user knows they're blocked
            // (only if they were enrolled — not if they just dismissed)
            if (isEnrolled()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Accès refusé");
                    alert.setHeaderText("Vérification faciale annulée");
                    alert.setContentText(
                            "Vous devez valider votre identité pour accéder à cette section.\n"
                                    + "Allez dans Mon Compte pour gérer votre reconnaissance faciale."
                    );
                    alert.showAndWait();
                });
            }
        });

        // Stop camera if dialog is force-closed
        dialog.setOnCloseRequest(e -> {
            timer.stop();
            webcam.stop();
        });

        dialog.setScene(new Scene(overlay, 500, 500));
        dialog.showAndWait(); // blocks until dialog closes
        return result;
    }
}