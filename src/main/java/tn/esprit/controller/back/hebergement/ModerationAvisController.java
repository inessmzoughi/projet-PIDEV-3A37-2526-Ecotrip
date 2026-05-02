package tn.esprit.controller.back.hebergement;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.models.hebergements.Avis;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.AvisHebergement_service;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ModerationAvisController implements Initializable {

    @FXML private VBox  avisContainer;
    @FXML private Label lblBadge;

    private final AvisHebergement_service avisService
            = new AvisHebergement_service();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerAvisEnAttente();
    }

    /* ─── Navigation retour ─── */
    @FXML
    private void onRetourHebergements() {
        SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS );
    }

    private void chargerAvisEnAttente() {
        try {
            List<Avis> list = avisService.getEnAttente();
            lblBadge.setText(String.valueOf(list.size()));
            avisContainer.getChildren().clear();

            if (list.isEmpty()) {
                Label empty = new Label("✅ Aucun avis en attente de modération.");
                empty.setStyle("-fx-font-size:14px; -fx-text-fill:#64748b;"
                        + "-fx-padding:40;");
                avisContainer.getChildren().add(empty);
                return;
            }

            for (Avis av : list)
                avisContainer.getChildren().add(buildModerationCard(av));

        } catch (SQLException e) {
            showAlert("Erreur : " + e.getMessage());
        }
    }

    private VBox buildModerationCard(Avis av) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color:white;"
                + "-fx-background-radius:12;"
                + "-fx-border-color:#fde68a;"
                + "-fx-border-radius:12;"
                + "-fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        // ── Header ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("⏳ EN ATTENTE");
        badge.setStyle("-fx-background-color:#fef9c3; -fx-text-fill:#854d0e;"
                + "-fx-font-size:10px; -fx-font-weight:bold;"
                + "-fx-background-radius:10; -fx-padding:3 8 3 8;");

        Label info = new Label(av.getUsername());
        info.setStyle("-fx-font-weight:bold; -fx-font-size:13px;"
                + "-fx-text-fill:#0f172a;");

        Label date = new Label();
        if (av.getCreatedAt() != null)
            date.setText(av.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        date.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(badge, info, spacer, date);

        // ── Commentaire ──
        Label texte = new Label(av.getCommentaire());
        texte.setWrapText(true);
        texte.setStyle("-fx-font-size:13px; -fx-text-fill:#334155;"
                + "-fx-background-color:#f8fafc;"
                + "-fx-background-radius:8; -fx-padding:10;");
        texte.setMaxWidth(Double.MAX_VALUE);

        // ── Image (si présente) ──
        String imagePath = av.getImagePath();
        if (imagePath != null && !imagePath.isBlank()) {
            HBox imageBox = buildImageBox(imagePath);
            card.getChildren().addAll(header, texte, imageBox);
        } else {
            card.getChildren().addAll(header, texte);
        }

        // ── Boutons ──
        Button approuver = new Button("✅  Approuver");
        approuver.setStyle("-fx-background-color:#22c55e; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:8;"
                + "-fx-cursor:hand; -fx-padding:8 24 8 24;"
                + "-fx-border-width:0;");
        approuver.setOnAction(e -> {
            try {
                avisService.approuver(av.getId());
                navigerVersHebergements();
            } catch (SQLException ex) {
                showAlert("Erreur : " + ex.getMessage());
            }
        });

        Button rejeter = new Button("❌  Rejeter");
        rejeter.setStyle("-fx-background-color:#ef4444; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:8;"
                + "-fx-cursor:hand; -fx-padding:8 24 8 24;"
                + "-fx-border-width:0;");
        rejeter.setOnAction(e -> {
            try {
                avisService.rejeter(av.getId());
                navigerVersHebergements();
            } catch (SQLException ex) {
                showAlert("Erreur : " + ex.getMessage());
            }
        });

        HBox actions = new HBox(10, approuver, rejeter);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().add(actions);
        return card;
    }

    /* ─── Construire la zone image ─── */
    private HBox buildImageBox(String imagePath) {
        HBox imageBox = new HBox();
        imageBox.setAlignment(Pos.CENTER_LEFT);

        try {
            Image img;
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                img = new Image(imagePath, 260, 180, true, true, true);
            } else if (imagePath.startsWith("file:")) {
                // Already a file URI
                img = new Image(imagePath, 260, 180, true, true, true);
            } else {
                // Absolute or relative path → convert to proper URI
                java.io.File file = new java.io.File(imagePath);
                String uri = file.toURI().toString(); // produces file:///... correctly
                img = new Image(uri, 260, 180, true, true, true);
            }

            // Check the image actually loaded
            if (img.isError()) {
                throw new Exception("Load error: " +
                        (img.getException() != null ? img.getException().getMessage() : imagePath));
            }

            ImageView iv = new ImageView(img);
            iv.setFitWidth(260);
            iv.setFitHeight(180);
            iv.setPreserveRatio(true);
            iv.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),6,0,0,2);");

            // Conteneur image avec coins arrondis simulés via clip ou border
            VBox imgContainer = new VBox(iv);
            imgContainer.setStyle("-fx-background-color:#f1f5f9;"
                    + "-fx-background-radius:8;"
                    + "-fx-border-color:#e2e8f0;"
                    + "-fx-border-radius:8;"
                    + "-fx-border-width:1;"
                    + "-fx-padding:4;");
            imgContainer.setAlignment(Pos.CENTER);

            // Label indicatif
            Label imgLabel = new Label("📷 Photo jointe");
            imgLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;"
                    + "-fx-font-style:italic; -fx-padding:4 0 0 0;");

            VBox wrapper = new VBox(4, imgContainer, imgLabel);
            wrapper.setAlignment(Pos.CENTER_LEFT);

            imageBox.getChildren().add(wrapper);

        } catch (Exception ex) {
            // Si l'image ne peut pas être chargée, afficher un placeholder
            Label placeholder = new Label("🖼️  Image non disponible\n" + imagePath);
            placeholder.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;"
                    + "-fx-background-color:#f8fafc;"
                    + "-fx-background-radius:8;"
                    + "-fx-border-color:#e2e8f0;"
                    + "-fx-border-radius:8;"
                    + "-fx-border-width:1;"
                    + "-fx-padding:12;");
            placeholder.setWrapText(true);
            imageBox.getChildren().add(placeholder);
        }

        return imageBox;
    }

    /* ─── Navigation vers liste hébergements après action ─── */
    private void navigerVersHebergements() {
        SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS );
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(msg);
        a.showAndWait();
    }
}