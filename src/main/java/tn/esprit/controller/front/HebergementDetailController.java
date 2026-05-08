package tn.esprit.controller.front;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.models.hebergements.*;
import tn.esprit.models.Auth_User.User;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.*;
import tn.esprit.services.hebergement.AvisModeratorService.ModerationResult;
import tn.esprit.services.hebergement.AvisModeratorService.Decision;
import tn.esprit.session.SessionManager;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class HebergementDetailController implements Initializable {

    /* ─── FXML ─── */
    @FXML private ImageView  imageView;
    @FXML private ImageView  photoPreview;
    @FXML private StackPane  imageStack;
    @FXML private Label      lblNom, lblVille, lblEtoiles, lblDescription;
    @FXML private Label      lblCategorie, lblVilleSidebar, lblAdresse;
    @FXML private Label      lblLikeCount, lblAvisCount;
    @FXML private Button     btnLike, btnRetour, btnReserver;
    @FXML private Button     btnDislike;
    @FXML private TextArea   commentaireField;
    @FXML private Button     btnEnvoyerAvis, btnChoisirPhoto;
    @FXML private Label      lblPhotoChoisie;
    @FXML private VBox       avisContainer, formAvisBox;
    @FXML private Label      errCommentaire;
    @FXML private FlowPane   equipementsPane;
    @FXML private HBox       prixBox;
    @FXML private StackPane  galerieMainPane;
    @FXML private ImageView  galerieMainView;
    @FXML private Button     galeriePrev;
    @FXML private Button     galerieNext;
    @FXML private Label      galerieCounter;
    @FXML private Label      galerieLegend;
    @FXML private HBox       galerieThumbs;

    /* ─── Services ─── */
    private final LikeHebergement_service       likeService       = new LikeHebergement_service();
    private final AvisHebergement_service       avisService       = new AvisHebergement_service();
    private final CategorieH_service            categorieService  = new CategorieH_service();
    private final HebergementEquipement_service equipService      = new HebergementEquipement_service();
    private final Chambre_service               chambreService    = new Chambre_service();
    private final CloudinaryService             cloudinaryService = CloudinaryService.getInstance();
    // ── IA Moderator ────────────────────────────────────────────────────────
    private final AvisModeratorService          moderatorService  = AvisModeratorService.getInstance();

    /* ─── State ─── */
    private Hebergement hebergement;
    private User        currentUser;
    private boolean     isLiked    = false;
    private boolean     isDisliked = false;
    private File        selectedPhoto = null;
    private Avis        editingAvis   = null;
    private final HebergementImage_service galerieService = new HebergementImage_service();
    private List<HebergementImage>         galerieImages  = new ArrayList<>();
    private int                            galerieIndex   = 0;

    private static final String UPLOADS_AVIS = "uploads/avis/";
    private static final String UPLOADS_HEB  = "uploads/hebergements/";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            btnLike.setDisable(true);
            formAvisBox.setDisable(true);
            commentaireField.setPromptText("Connectez-vous pour laisser un avis…");
        }
        new File(UPLOADS_AVIS).mkdirs();
    }

    public void setHebergement(Hebergement h) {
        this.hebergement = h;
        afficherDetails();
        chargerEquipements();
        chargerGalerie();
        chargerLikes();
        chargerAvis();
        chargerPrix();
    }

    @FXML private void onRetour() { SceneManager.navigateTo(Routes.HEBERGEMENTS); }

    @FXML
    private void onReserver() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/HebergementReservationModal.fxml"));
            StackPane overlay = loader.load();
            tn.esprit.controller.front.modals.HebergementReservationController ctrl
                    = loader.getController();
            ctrl.setHebergement(hebergement);
            ctrl.setOverlayRoot(overlay);

            Scene scene = btnReserver.getScene();
            Parent rootNode = scene.getRoot();
            StackPane container;
            if (rootNode instanceof StackPane) {
                container = (StackPane) rootNode;
            } else {
                container = new StackPane(rootNode);
                scene.setRoot(container);
            }
            container.getChildren().add(overlay);
            if (!container.getChildren().isEmpty())
                container.getChildren().get(0)
                        .setEffect(new javafx.scene.effect.GaussianBlur(8));
            ctrl.setOnCartUpdated(() ->
                    container.getChildren().get(0).setEffect(null));
        } catch (Exception e) { e.printStackTrace(); }
    }

    /* ─── Afficher infos ─── */
    private void afficherDetails() {
        lblNom.setText(hebergement.getNom());
        lblVille.setText("📍  " + hebergement.getVille());
        lblVilleSidebar.setText(hebergement.getVille());
        lblAdresse.setText(hebergement.getAdresse() != null ? hebergement.getAdresse() : "");
        lblEtoiles.setText("★".repeat(hebergement.getNb_etoiles()));
        lblDescription.setText(hebergement.getDescription() != null
                ? hebergement.getDescription() : "");
        try {
            Categorie_hebergement cat = categorieService.getById(hebergement.getCategorie_id());
            if (cat != null) lblCategorie.setText(cat.getNom().toUpperCase());
        } catch (SQLException ignored) {}

        Image img = loadImage(hebergement.getImage_principale());
        if (img != null) {
            imageView.setImage(img);
            if (imageStack != null) imageStack.getChildren().remove(imageView);
            if (imageStack != null) imageStack.getChildren().add(0, imageView);
        }
    }

    /* ─── Équipements ─── */
    private void chargerEquipements() {
        try {
            List<Equipement> list = equipService.getEquipementsByHebergement(hebergement.getId());
            equipementsPane.getChildren().clear();
            if (list != null)
                for (Equipement eq : list) {
                    Label tag = new Label(eq.getNom());
                    tag.getStyleClass().add("heb-card-equipement");
                    equipementsPane.getChildren().add(tag);
                }
        } catch (SQLException ignored) {}
    }

    /* ─── Prix minimum ─── */
    private void chargerPrix() {
        try {
            List<Chambre> chambres = chambreService.getByHebergement(hebergement.getId());
            Double min = chambres.stream()
                    .mapToDouble(Chambre::getPrix_par_nuit).min()
                    .stream().boxed().findFirst().orElse(null);
            prixBox.getChildren().clear();
            if (min != null) {
                Label from   = new Label("À partir de");
                from.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
                Label amount = new Label(String.format("%.0f", min));
                amount.getStyleClass().add("heb-card-price-amount");
                Label cur    = new Label("TND");
                cur.getStyleClass().add("heb-card-price-currency");
                Label unit   = new Label("/ nuit");
                unit.getStyleClass().add("heb-card-price-unit");
                prixBox.getChildren().addAll(from, amount, cur, unit);
            } else {
                Label req = new Label("Prix sur demande");
                req.getStyleClass().add("heb-card-price-request");
                prixBox.getChildren().add(req);
            }
        } catch (SQLException ignored) {}
    }

    /* ══════════════════════════════════════════════════════
       LIKES
       ══════════════════════════════════════════════════════ */
    private void chargerLikes() {
        try {
            int count = likeService.countLikes(hebergement.getId());
            lblLikeCount.setText(String.valueOf(count));
            if (currentUser != null) {
                isLiked    = likeService.isLiked(currentUser.getId(), hebergement.getId());
                isDisliked = likeService.isDisliked(currentUser.getId(), hebergement.getId());
            }
            updateLikeDislikeBtns();
        } catch (SQLException e) { lblLikeCount.setText("0"); }
    }

    @FXML
    private void onLike() {
        if (currentUser == null) return;
        try {
            if (isDisliked) {
                likeService.removeDislike(currentUser.getId(), hebergement.getId());
                isDisliked = false;
            }
            isLiked = likeService.toggleLike(currentUser.getId(), hebergement.getId());
            lblLikeCount.setText(String.valueOf(likeService.countLikes(hebergement.getId())));
            updateLikeDislikeBtns();
        } catch (SQLException e) { showAlert("Erreur like : " + e.getMessage()); }
    }

    @FXML
    private void onDislike() {
        if (currentUser == null) return;
        try {
            if (isLiked) {
                likeService.toggleLike(currentUser.getId(), hebergement.getId());
                isLiked = false;
                lblLikeCount.setText(String.valueOf(likeService.countLikes(hebergement.getId())));
            }
            isDisliked = likeService.toggleDislike(currentUser.getId(), hebergement.getId());
            updateLikeDislikeBtns();
        } catch (SQLException e) { showAlert("Erreur dislike : " + e.getMessage()); }
    }

    private void updateLikeDislikeBtns() {
        String count = lblLikeCount.getText();
        if (isLiked) {
            btnLike.setText("❤️  " + count);
            btnLike.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#e53e3e;"
                    + "-fx-font-weight:bold; -fx-background-radius:20;"
                    + "-fx-border-color:#fca5a5; -fx-border-radius:20;"
                    + "-fx-cursor:hand; -fx-padding:8 20 8 20;");
        } else {
            btnLike.setText("🤍  " + count);
            btnLike.setStyle("-fx-background-color:#f8fafc; -fx-text-fill:#64748b;"
                    + "-fx-font-weight:bold; -fx-background-radius:20;"
                    + "-fx-border-color:#e2e8f0; -fx-border-radius:20;"
                    + "-fx-cursor:hand; -fx-padding:8 20 8 20;");
        }
        if (isDisliked) {
            btnDislike.setText("👎  Pas pour moi");
            btnDislike.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#92400e;"
                    + "-fx-font-weight:bold; -fx-background-radius:20;"
                    + "-fx-border-color:#fcd34d; -fx-border-radius:20;"
                    + "-fx-cursor:hand; -fx-padding:8 20 8 20;");
        } else {
            btnDislike.setText("👎  Pas pour moi");
            btnDislike.setStyle("-fx-background-color:#f8fafc; -fx-text-fill:#94a3b8;"
                    + "-fx-font-weight:bold; -fx-background-radius:20;"
                    + "-fx-border-color:#e2e8f0; -fx-border-radius:20;"
                    + "-fx-cursor:hand; -fx-padding:8 20 8 20;");
        }
    }

    /* ══════════════════════════════════════════════════════
       PHOTO
       ══════════════════════════════════════════════════════ */
    @FXML
    private void onChoisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = fc.showOpenDialog(btnChoisirPhoto.getScene().getWindow());
        if (file != null) {
            selectedPhoto = file;
            lblPhotoChoisie.setText(file.getName());
            try {
                Image preview = new Image(file.toURI().toString(), 220, 130, true, true);
                photoPreview.setImage(preview);
                photoPreview.setVisible(true);
                photoPreview.setManaged(true);
            } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unused")
    private String savePhoto(File src) {
        try {
            String ext      = src.getName().contains(".")
                    ? src.getName().substring(src.getName().lastIndexOf(".")) : ".jpg";
            String fileName = UUID.randomUUID() + ext;
            File   dest     = new File(UPLOADS_AVIS + fileName);
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getPath();
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde photo : " + e.getMessage());
            return null;
        }
    }

    /* ══════════════════════════════════════════════════════
       AVIS  ── avec MODÉRATION IA ──
       ══════════════════════════════════════════════════════ */

    private void chargerAvis() {
        try {
            List<Avis> list = avisService.getApprouvesByHebergement(hebergement.getId());
            int total = avisService.countApprouves(hebergement.getId());
            lblAvisCount.setText(total + " avis");
            avisContainer.getChildren().clear();
            for (Avis av : list)
                avisContainer.getChildren().add(buildAvisCard(av));
        } catch (SQLException e) { showAlert("Erreur chargement avis : " + e.getMessage()); }
    }

    /**
     * Soumission d'un avis avec modération IA automatique.
     *
     * Flux :
     *  1. Validation basique (longueur)
     *  2. Upload photo si présente
     *  3. Appel Claude → décision APPROUVE / REJETE / EN_ATTENTE
     *  4. Sauvegarde avec le bon statut
     *  5. Popup résultat adapté à la décision
     */
    @FXML
    private void onEnvoyerAvis() {
        if (currentUser == null) return;
        String texte = commentaireField.getText().trim();

        // ── Validation basique ────────────────────────────────────────────────
        if (texte.length() < 5)   { showErr("⚠ Minimum 5 caractères."); return; }
        if (texte.length() > 500) { showErr("⚠ Maximum 500 caractères."); return; }
        hideErr();

        // ── Désactiver le bouton pendant le traitement ────────────────────────
        btnEnvoyerAvis.setDisable(true);
        btnEnvoyerAvis.setText("🤖  Analyse en cours…");

        // ── Upload photo (synchrone, déjà sur FX thread) ─────────────────────
        final String[] photoPathHolder = {null};
        if (selectedPhoto != null) {
            try {
                photoPathHolder[0] = cloudinaryService.uploadAvisImage(selectedPhoto);
            } catch (Exception e) {
                btnEnvoyerAvis.setDisable(false);
                btnEnvoyerAvis.setText(editingAvis != null
                        ? "✏️  Mettre à jour l'avis" : "📤  Publier mon avis");
                showAlert("Erreur upload photo : " + e.getMessage());
                return;
            }
        }

        // ── Modération IA en arrière-plan (évite de bloquer le FX thread) ─────
        final String texteAAnalyser = texte;
        javafx.concurrent.Task<ModerationResult> moderationTask =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected ModerationResult call() {
                        return moderatorService.moderer(texteAAnalyser);
                    }
                };

        moderationTask.setOnSucceeded(event -> {
            ModerationResult result = moderationTask.getValue();
            System.out.println("[AvisModerator] Décision : " + result.decision()
                    + " — " + result.reason());

            try {
                String statut = switch (result.decision()) {
                    case APPROUVE  -> "APPROUVE";
                    case REJETE    -> "REJETE";
                    case EN_ATTENTE -> "EN_ATTENTE";
                };

                if (editingAvis != null) {
                    // ── Édition d'un avis existant ───────────────────────────
                    editingAvis.setCommentaire(texteAAnalyser);
                    editingAvis.setStatut(statut);
                    if (photoPathHolder[0] != null)
                        editingAvis.setImagePath(photoPathHolder[0]);
                    avisService.modifier(editingAvis);
                    editingAvis = null;
                } else {
                    // ── Nouvel avis ──────────────────────────────────────────
                    Avis avis = new Avis(
                            currentUser.getId(),
                            hebergement.getId(),
                            texteAAnalyser,
                            photoPathHolder[0]);
                    avis.setStatut(statut);
                    avisService.ajouter(avis);
                }

                // ── Reset formulaire ─────────────────────────────────────────
                commentaireField.clear();
                selectedPhoto = null;
                lblPhotoChoisie.setText("Aucune photo choisie");
                photoPreview.setVisible(false);
                photoPreview.setManaged(false);

                // ── Popup adapté à la décision ───────────────────────────────
                showModerationPopup(result.decision(), result.reason());

                // Recharger seulement si approuvé (sinon rien de nouveau à afficher)
                if (result.decision() == Decision.APPROUVE) chargerAvis();

            } catch (SQLException e) {
                showAlert("Erreur sauvegarde : " + e.getMessage());
            } finally {
                btnEnvoyerAvis.setDisable(false);
                btnEnvoyerAvis.setText("📤  Publier mon avis");
            }
        });

        moderationTask.setOnFailed(event -> {
            btnEnvoyerAvis.setDisable(false);
            btnEnvoyerAvis.setText("📤  Publier mon avis");
            showAlert("Erreur modération : " + moderationTask.getException().getMessage());
        });

        new Thread(moderationTask).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POPUP RÉSULTAT MODÉRATION — 3 variantes selon la décision IA
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Affiche un popup dont le contenu varie selon la décision de l'IA :
     *
     *  APPROUVE   → ✅ vert  « Votre avis est publié ! »
     *  REJETE     → ❌ rouge « Votre avis a été rejeté (raison) »
     *  EN_ATTENTE → ⏳ bleu  « En attente de validation manuelle »
     */
    private void showModerationPopup(Decision decision, String raison) {
        Stage popup = new Stage();
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initOwner(commentaireField.getScene().getWindow());

        // ── Contenu selon décision ────────────────────────────────────────────
        String emoji, titre, detail, btnColor;
        switch (decision) {
            case APPROUVE -> {
                emoji    = "✅";
                titre    = "Avis publié !";
                detail   = "Votre avis est visible par tous les voyageurs. Merci pour votre retour !";
                btnColor = "#16a34a";
            }
            case REJETE -> {
                emoji    = "❌";
                titre    = "Avis non publié";
                detail   = "Votre avis n'a pas pu être publié.\n"
                        + "Raison détectée : " + raison + "\n\n"
                        + "Veuillez reformuler votre commentaire de manière respectueuse.";
                btnColor = "#dc2626";
            }
            default -> { // EN_ATTENTE
                emoji    = "⏳";
                titre    = "En attente de validation";
                detail   = "Votre avis a été soumis et sera vérifié par notre équipe "
                        + "avant d'être publié. Merci de votre patience !";
                btnColor = "#0ea5e9";
            }
        }

        // ── UI ────────────────────────────────────────────────────────────────
        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size:44px;");

        Label titreLbl = new Label(titre);
        titreLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label detailLbl = new Label(detail);
        detailLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#475569; -fx-text-alignment:center;");
        detailLbl.setWrapText(true);
        detailLbl.setMaxWidth(260);
        detailLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Badge IA
        Label iaBadge = new Label("🤖 Modéré automatiquement par IA");
        iaBadge.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#64748b;"
                + "-fx-font-size:10px; -fx-background-radius:20;"
                + "-fx-padding:3 10 3 10;");

        Button ok = new Button("OK");
        ok.setStyle("-fx-background-color:" + btnColor + "; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:8;"
                + "-fx-padding:9 48 9 48; -fx-cursor:hand;"
                + "-fx-border-width:0; -fx-font-size:13px;");
        ok.setOnAction(e -> popup.close());

        VBox box = new VBox(12, iconLbl, titreLbl, detailLbl, iaBadge, ok);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32, 36, 28, 36));
        box.setPrefWidth(340);
        box.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),20,0,0,5);"
                + "-fx-border-color:#e2e8f0; -fx-border-radius:14; -fx-border-width:1;");

        Scene scene = new Scene(box);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.setOnShown(e -> {
            Stage owner = (Stage) commentaireField.getScene().getWindow();
            popup.setX(owner.getX() + (owner.getWidth()  - popup.getWidth())  / 2);
            popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
        });
        popup.showAndWait();
    }

    /* ─── Préparer l'édition d'un avis ─── */
    private void startEditing(Avis av) {
        editingAvis = av;
        commentaireField.setText(av.getCommentaire());
        btnEnvoyerAvis.setText("✏️  Mettre à jour l'avis");
        commentaireField.requestFocus();
    }

    /* ─── Build carte avis ─── */
    private VBox buildAvisCard(Avis av) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:white;"
                + "-fx-background-radius:12;"
                + "-fx-border-color:#e2e8f0;"
                + "-fx-border-radius:12;"
                + "-fx-border-width:1;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        String initiale = (av.getUsername() != null && !av.getUsername().isEmpty())
                ? av.getUsername().substring(0, 1).toUpperCase() : "?";
        Label avatar = new Label(initiale);
        avatar.setStyle("-fx-background-color:#2d6a4f; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-font-size:13px;"
                + "-fx-background-radius:50%;"
                + "-fx-min-width:34px; -fx-min-height:34px;"
                + "-fx-max-width:34px; -fx-max-height:34px;"
                + "-fx-alignment:center;");

        Label username = new Label(av.getUsername() != null ? av.getUsername() : "Utilisateur");
        username.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#0f172a;");

        Label date = new Label();
        if (av.getCreatedAt() != null)
            date.setText(av.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        date.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(avatar, username, spacer, date);

        Label texte = new Label(av.getCommentaire());
        texte.setWrapText(true);
        texte.setStyle("-fx-font-size:13px; -fx-text-fill:#334155; -fx-line-spacing:3;");

        card.getChildren().addAll(header, texte);

        if (av.getImagePath() != null && !av.getImagePath().isBlank()) {
            try {
                File f = new File(av.getImagePath());
                Image img = f.exists()
                        ? new Image(f.toURI().toString(), 300, 200, true, true)
                        : new Image(av.getImagePath(), 300, 200, true, true, true);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(300);
                    iv.setFitHeight(200);
                    iv.setPreserveRatio(true);
                    card.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        if (currentUser != null && av.getUserId() == currentUser.getId()) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            Button editBtn = new Button("✏️ Modifier");
            editBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#2d6a4f;"
                    + "-fx-font-size:11px; -fx-cursor:hand;"
                    + "-fx-border-color:#2d6a4f; -fx-border-radius:6;"
                    + "-fx-background-radius:6; -fx-padding:4 10 4 10;");
            editBtn.setOnAction(e -> startEditing(av));

            Button delBtn = new Button("🗑 Supprimer");
            delBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#e53e3e;"
                    + "-fx-font-size:11px; -fx-cursor:hand;"
                    + "-fx-border-color:#fca5a5; -fx-border-radius:6;"
                    + "-fx-background-radius:6; -fx-padding:4 10 4 10;");
            delBtn.setOnAction(e -> {
                try {
                    avisService.supprimer(av.getId());
                    chargerAvis();
                } catch (SQLException ex) {
                    showAlert("Erreur suppression : " + ex.getMessage());
                }
            });

            actions.getChildren().addAll(editBtn, delBtn);
            card.getChildren().add(actions);
        }
        return card;
    }

    /* ─── Image helpers ─── */
    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                Image img = new Image(path, 1200, 340, false, true, true);
                return img.isError() ? null : img;
            }
            File f = new File(path);
            if (f.exists()) return new Image(f.toURI().toString(), 1200, 340, false, true);
            File uf = new File(UPLOADS_HEB + new File(path).getName());
            if (uf.exists()) return new Image(uf.toURI().toString(), 1200, 340, false, true);
            URL res = getClass().getResource("/images/" + new File(path).getName());
            if (res != null) return new Image(res.toExternalForm(), 1200, 340, false, true);
        } catch (Exception ignored) {}
        return null;
    }

    /* ─── Helpers UI ─── */
    private void showErr(String msg) {
        errCommentaire.setText(msg);
        errCommentaire.setVisible(true);
        errCommentaire.setManaged(true);
    }
    private void hideErr() {
        errCommentaire.setVisible(false);
        errCommentaire.setManaged(false);
    }
    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(msg);
        a.showAndWait();
    }

    /* ══════════════════════════════════════════════════════
       GALERIE  ── FIX : rechargement propre depuis la DB ──
       ══════════════════════════════════════════════════════ */

    /**
     * FIX : on vide toujours la liste avant de recharger depuis la DB.
     * Cela évite que l'ancienne liste (après suppression/modification)
     * reste en mémoire et pointe vers des images qui n'existent plus.
     */
    private void chargerGalerie() {
        // FIX 1 — toujours repartir d'une liste vide
        galerieImages = new ArrayList<>();

        try {
            galerieImages = galerieService.getByHebergement(hebergement.getId());
        } catch (SQLException ignored) {
            galerieImages = new ArrayList<>();
        }

        if (galerieImages.isEmpty()) {
            galerieMainPane.setVisible(false);
            galerieMainPane.setManaged(false);
            return;
        }

        galerieMainPane.setVisible(true);
        galerieMainPane.setManaged(true);
        galerieIndex = 0;

        galerieThumbs.getChildren().clear();
        for (int i = 0; i < galerieImages.size(); i++) {
            final int idx = i;
            final String url = galerieImages.get(i).getUrl();

            StackPane thumbPane = new StackPane();
            thumbPane.setPrefSize(100, 70);
            thumbPane.setStyle("-fx-border-color:#e2e8f0; -fx-border-radius:8;"
                    + "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:2;"
                    + "-fx-background-color:#f1f5f9;");

            try {
                // FIX 2 — chargement asynchrone (true en dernier paramètre)
                Image thumbImg = new Image(url, 100, 70, true, true, true);
                ImageView thumb = new ImageView();
                thumb.setFitWidth(100);
                thumb.setFitHeight(70);
                thumb.setPreserveRatio(true);
                thumb.setSmooth(true);

                // FIX 3 — écouter la fin du chargement pour gérer les erreurs
                thumbImg.errorProperty().addListener((obs, old, err) -> {
                    if (err) {
                        System.err.println("[Galerie] Miniature introuvable : " + url);
                    }
                });
                thumbImg.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !thumbImg.isError()) {
                        thumb.setImage(thumbImg);
                    }
                });
                // Si déjà chargée (cache) on l'affiche directement
                if (!thumbImg.isError() && thumbImg.getProgress() >= 1.0) {
                    thumb.setImage(thumbImg);
                }
                thumbPane.getChildren().add(thumb);
            } catch (Exception ignored) {}

            thumbPane.setOnMouseClicked(e -> selectGalerieImage(idx));
            thumbPane.setOnMouseEntered(e ->
                    thumbPane.setStyle("-fx-border-color:#2d6a4f; -fx-border-radius:8;"
                            + "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:2;"
                            + "-fx-background-color:#f1f5f9;"));
            thumbPane.setOnMouseExited(e -> {
                boolean active = (galerieIndex == idx);
                thumbPane.setStyle("-fx-border-color:" + (active ? "#2d6a4f" : "#e2e8f0")
                        + "; -fx-border-radius:8; -fx-background-radius:8;"
                        + "-fx-cursor:hand; -fx-border-width:2;"
                        + "-fx-background-color:#f1f5f9;");
            });

            galerieThumbs.getChildren().add(thumbPane);
        }

        selectGalerieImage(0);
    }

    private void selectGalerieImage(int idx) {
        if (galerieImages == null || galerieImages.isEmpty()) return;
        galerieIndex = idx;
        HebergementImage img = galerieImages.get(idx);

        // FIX 4 — chargement asynchrone de la grande image (true = background loading)
        try {
            Image mainImg = new Image(img.getUrl(), 700, 320, true, true, true);

            // FIX 5 — listener d'erreur pour debug et fallback propre
            mainImg.errorProperty().addListener((obs, old, err) -> {
                if (err) {
                    System.err.println("[Galerie] Erreur chargement image principale : " + img.getUrl());
                    javafx.application.Platform.runLater(() -> galerieMainView.setImage(null));
                }
            });

            // FIX 6 — afficher dès que le chargement est terminé
            mainImg.progressProperty().addListener((obs, old, progress) -> {
                if (progress.doubleValue() >= 1.0 && !mainImg.isError()) {
                    javafx.application.Platform.runLater(() -> galerieMainView.setImage(mainImg));
                }
            });

            // Si déjà en cache, affichage immédiat
            if (!mainImg.isError() && mainImg.getProgress() >= 1.0) {
                galerieMainView.setImage(mainImg);
            } else if (!mainImg.isError()) {
                // Image en cours de chargement : vider l'ancienne image pour éviter l'affichage résiduel
                galerieMainView.setImage(null);
            }
        } catch (Exception e) {
            System.err.println("[Galerie] Exception chargement image : " + e.getMessage());
            galerieMainView.setImage(null);
        }

        // Compteur
        galerieCounter.setText((idx + 1) + " / " + galerieImages.size());

        // Légende
        if (img.getLegende() != null && !img.getLegende().isBlank()) {
            galerieLegend.setText("📝  " + img.getLegende());
            galerieLegend.setVisible(true);
            galerieLegend.setManaged(true);
        } else {
            galerieLegend.setVisible(false);
            galerieLegend.setManaged(false);
        }

        // Surbrillance miniature active
        for (int i = 0; i < galerieThumbs.getChildren().size(); i++) {
            StackPane p = (StackPane) galerieThumbs.getChildren().get(i);
            p.setStyle("-fx-border-color:" + (i == idx ? "#2d6a4f" : "#e2e8f0")
                    + "; -fx-border-radius:8; -fx-background-radius:8;"
                    + "-fx-cursor:hand; -fx-border-width:2;"
                    + "-fx-background-color:#f1f5f9;");
        }
    }

    @FXML
    private void onGaleriePrev() {
        if (galerieImages == null || galerieImages.isEmpty()) return;
        selectGalerieImage((galerieIndex - 1 + galerieImages.size()) % galerieImages.size());
    }

    @FXML
    private void onGalerieNext() {
        if (galerieImages == null || galerieImages.isEmpty()) return;
        selectGalerieImage((galerieIndex + 1) % galerieImages.size());
    }
}