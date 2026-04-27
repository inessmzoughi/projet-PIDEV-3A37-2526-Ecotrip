package tn.esprit.controller.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.models.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.FavoriHebergement_service;
import tn.esprit.session.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class MesFavorisController implements Initializable {

    @FXML private FlowPane favorisContainer;
    @FXML private Label    lblCount;
    @FXML private VBox     emptyState;
    @FXML private Label resultCountLabel;

    private final FavoriHebergement_service favoriService = new FavoriHebergement_service();
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ✅ Fix 2 : toujours relire le user depuis la session à chaque ouverture
        currentUser = SessionManager.getInstance().getCurrentUser();
        chargerFavoris();
    }

    private void chargerFavoris() {
        favorisContainer.getChildren().clear();

        if (currentUser == null) {
            lblCount.setText("Connectez-vous pour voir vos favoris");
            resultCountLabel.setText("0 Favori Trouvé");
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        try {
            List<Hebergement> list = favoriService.getFavoris(currentUser.getId());

            lblCount.setText(list.size() + " hébergement(s) sauvegardé(s)");
            resultCountLabel.setText(list.size() + " Favori" + (list.size() > 1 ? "s" : "") + " Trouvé(s)");

            if (list.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                return;
            }

            emptyState.setVisible(false);
            emptyState.setManaged(false);

            for (Hebergement h : list)
                favorisContainer.getChildren().add(buildFavoriCard(h));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildFavoriCard(Hebergement h) {
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color:white;"
                + "-fx-background-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);"
                + "-fx-cursor:hand;");

        // ── Image ──
        StackPane imgStack = new StackPane();
        imgStack.setPrefHeight(180);
        imgStack.setMinHeight(180);
        imgStack.setMaxHeight(180);
        imgStack.setStyle("-fx-background-radius:14 14 0 0;");

        try {
            String path = h.getImage_principale();
            Image img = null;
            if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                img = new Image(path, 300, 180, false, true, true);
            }
            if (img != null && !img.isError()) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(300);
                iv.setFitHeight(180);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                imgStack.getChildren().add(iv);
            } else {
                Label fallback = new Label("🏨");
                fallback.setStyle("-fx-font-size:48px;"
                        + "-fx-background-color:#e8f5e9;"
                        + "-fx-min-width:300; -fx-min-height:180;"
                        + "-fx-alignment:center;");
                imgStack.getChildren().add(fallback);
            }
        } catch (Exception ignored) {
            Label fallback = new Label("🏨");
            fallback.setStyle("-fx-font-size:48px; -fx-background-color:#e8f5e9;"
                    + "-fx-min-width:300; -fx-min-height:180; -fx-alignment:center;");
            imgStack.getChildren().add(fallback);
        }

        // Badge étoiles sur image
        Label stars = new Label("★".repeat(h.getNb_etoiles()));
        stars.setStyle("-fx-background-color:rgba(0,0,0,0.45);"
                + "-fx-text-fill:#fbbf24;"
                + "-fx-font-size:11px;"
                + "-fx-background-radius:6;"
                + "-fx-padding:3 8 3 8;");
        imgStack.getChildren().add(stars);
        StackPane.setAlignment(stars, Pos.BOTTOM_LEFT);
        StackPane.setMargin(stars, new Insets(0, 0, 10, 10));

        // ── Corps ──
        VBox body = new VBox(6);
        body.setPadding(new Insets(16, 18, 18, 18));

        Label nom = new Label(h.getNom());
        nom.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        nom.setWrapText(true);

        Label ville = new Label("📍  " + h.getVille());
        ville.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        // Description courte
        if (h.getDescription() != null && !h.getDescription().isBlank()) {
            String desc = h.getDescription();
            if (desc.length() > 80) desc = desc.substring(0, 80) + "…";
            Label descLabel = new Label(desc);
            descLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
            descLabel.setWrapText(true);
            body.getChildren().addAll(nom, ville, descLabel);
        } else {
            body.getChildren().addAll(nom, ville);
        }

        // Séparateur
        Separator sep = new Separator();
        sep.setStyle("-fx-padding:4 0 4 0;");

        // ── Actions ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Button btnVoir = new Button("👁  Voir");
        btnVoir.setStyle("-fx-background-color:#2d6a4f;"
                + "-fx-text-fill:white;"
                + "-fx-font-weight:bold;"
                + "-fx-background-radius:8;"
                + "-fx-cursor:hand;"
                + "-fx-padding:7 20 7 20;"
                + "-fx-border-width:0;"
                + "-fx-font-size:12px;");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnVoir, Priority.ALWAYS);
        btnVoir.setOnAction(e -> ouvrirDetail(h));

        Button btnRetirer = new Button("🗑");
        btnRetirer.setStyle("-fx-background-color:#fee2e2;"
                + "-fx-text-fill:#e53e3e;"
                + "-fx-font-size:13px;"
                + "-fx-cursor:hand;"
                + "-fx-border-color:#fca5a5;"
                + "-fx-border-radius:8;"
                + "-fx-background-radius:8;"
                + "-fx-padding:7 12 7 12;");
        btnRetirer.setOnAction(e -> {
            try {
                favoriService.supprimer(currentUser.getId(), h.getId());
                chargerFavoris();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        actions.getChildren().addAll(btnVoir, btnRetirer);
        body.getChildren().addAll(sep, actions);
        card.getChildren().addAll(imgStack, body);
        return card;
    }

    private void ouvrirDetail(Hebergement h) {
        try {
            HebergementDetailController ctrl =
                    SceneManager.navigateToAndGetController(Routes.HEBERGEMENT_DETAIL);
            ctrl.setHebergement(h);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ Fix 1 : pas de bouton retour — le retour se fait via la navbar
    @FXML
    private void onRetour() {
        SceneManager.navigateTo(Routes.HEBERGEMENTS);
    }
}