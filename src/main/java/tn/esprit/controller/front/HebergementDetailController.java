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
import tn.esprit.models.hebergements.Avis;
import tn.esprit.models.hebergements.Categorie_hebergement;
import tn.esprit.models.hebergements.Chambre;
import tn.esprit.models.hebergements.Equipement;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.models.User;
import tn.esprit.services.hebergement.AvisHebergement_service;
import tn.esprit.services.hebergement.CategorieH_service;
import tn.esprit.services.hebergement.Chambre_service;
import tn.esprit.services.hebergement.HebergementEquipement_service;
import tn.esprit.services.hebergement.LikeHebergement_service;
import tn.esprit.session.SessionManager;
import tn.esprit.services.hebergement.CloudinaryService;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
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
    @FXML private Button btnDislike;
    @FXML private TextArea   commentaireField;
    @FXML private Button     btnEnvoyerAvis, btnChoisirPhoto;
    @FXML private Label      lblPhotoChoisie;
    @FXML private VBox       avisContainer, formAvisBox;
    @FXML private Label      errCommentaire;
    @FXML private FlowPane   equipementsPane;
    @FXML private HBox       prixBox;

    /* ─── Services ─── */
    private final LikeHebergement_service     likeService       = new LikeHebergement_service();
    private final AvisHebergement_service     avisService       = new AvisHebergement_service();
    private final CategorieH_service          categorieService  = new CategorieH_service();
    private final HebergementEquipement_service equipService    = new HebergementEquipement_service();
    private final Chambre_service             chambreService    = new Chambre_service();
    private final CloudinaryService           cloudinaryService = CloudinaryService.getInstance();

    /* ─── State ─── */
    private Hebergement hebergement;
    private User        currentUser;
    private boolean isLiked    = false;
    private boolean isDisliked = false;
    private File        selectedPhoto = null;   // photo choisie pour le nouvel avis
    private Avis        editingAvis   = null;   // avis en cours d'édition

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
        // Assurer que le dossier uploads/avis existe
        new File(UPLOADS_AVIS).mkdirs();
    }

    /* ══════════════════════════════════════════════════════
       Appelé depuis HebergementsController (même stage)
       ══════════════════════════════════════════════════════ */
    public void setHebergement(Hebergement h) {
        this.hebergement = h;
        afficherDetails();
        chargerEquipements();
        chargerLikes();
        chargerAvis();
        chargerPrix();
    }

    /* ─── Retour à la liste (même stage, même scène) ─── */
    @FXML
    private void onRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/Hebergements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            Scene scene = btnRetour.getScene();
            scene.setRoot(root);           // ✅ change uniquement le root, même fenêtre
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ─── Réserver (déléguer au controller existant) ─── */
    /* ─── Réserver ─── */
    @FXML
    private void onReserver(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/HebergementReservationModal.fxml"));
            StackPane overlay = loader.load();
            tn.esprit.controller.front.modals.HebergementReservationController ctrl
                    = loader.getController();
            ctrl.setHebergement(hebergement);
            ctrl.setOverlayRoot(overlay);

            // ✅ Get scene from event source — never null here
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.Scene scene = source.getScene();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ─── Afficher infos ─── */
    private void afficherDetails() {
        lblNom.setText(hebergement.getNom());
        lblVille.setText("📍  " + hebergement.getVille());
        lblVilleSidebar.setText(hebergement.getVille());
        lblAdresse.setText(hebergement.getAdresse() != null
                ? hebergement.getAdresse() : "");
        lblEtoiles.setText("★".repeat(hebergement.getNb_etoiles()));
        lblDescription.setText(hebergement.getDescription() != null
                ? hebergement.getDescription() : "");

        // Catégorie
        try {
            Categorie_hebergement cat = categorieService.getById(hebergement.getCategorie_id());
            if (cat != null) lblCategorie.setText(cat.getNom().toUpperCase());
        } catch (SQLException ignored) {}

        // Image principale
        Image img = loadImage(hebergement.getImage_principale());
        if (img != null) {
            imageView.setImage(img);
            // Mettre l'imageView derrière l'overlay
            if (imageStack != null) imageStack.getChildren().remove(imageView);
            if (imageStack != null) imageStack.getChildren().add(0, imageView);
        }
    }

    /* ─── Équipements ─── */
    private void chargerEquipements() {
        try {
            List<Equipement> list = equipService.getEquipementsByHebergement(hebergement.getId());
            equipementsPane.getChildren().clear();
            if (list != null) {
                for (Equipement eq : list) {
                    Label tag = new Label(eq.getNom());
                    tag.getStyleClass().add("heb-card-equipement");
                    equipementsPane.getChildren().add(tag);
                }
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
                Label from = new Label("À partir de");
                from.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
                Label amount = new Label(String.format("%.0f", min));
                amount.getStyleClass().add("heb-card-price-amount");
                Label cur = new Label("TND");
                cur.getStyleClass().add("heb-card-price-currency");
                Label unit = new Label("/ nuit");
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
        } catch (SQLException e) {
            lblLikeCount.setText("0");
        }
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
        } catch (SQLException e) {
            showAlert("Erreur like : " + e.getMessage());
        }
    }

    @FXML
    private void onDislike() {
        if (currentUser == null) return;
        try {
            if (isLiked) {
                likeService.toggleLike(currentUser.getId(), hebergement.getId()); // removes like
                isLiked = false;
                lblLikeCount.setText(String.valueOf(likeService.countLikes(hebergement.getId())));
            }
            isDisliked = likeService.toggleDislike(currentUser.getId(), hebergement.getId());
            updateLikeDislikeBtns();
        } catch (SQLException e) {
            showAlert("Erreur dislike : " + e.getMessage());
        }
    }

    private void updateLikeDislikeBtns() {
        String count = lblLikeCount.getText();
        // Like button
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
        // Dislike button
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
            // Prévisualisation
            try {
                Image preview = new Image(file.toURI().toString(), 220, 130, true, true);
                photoPreview.setImage(preview);
                photoPreview.setVisible(true);
                photoPreview.setManaged(true);
            } catch (Exception ignored) {}
        }
    }

    /* ── Copie la photo dans uploads/avis/ et retourne le chemin ── */
    private String savePhoto(File src) {
        try {
            String ext      = src.getName().contains(".")
                    ? src.getName().substring(src.getName().lastIndexOf(".")) : ".jpg";
            String fileName = UUID.randomUUID().toString() + ext;
            File   dest     = new File(UPLOADS_AVIS + fileName);
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getPath();
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde photo : " + e.getMessage());
            return null;
        }
    }

    /* ══════════════════════════════════════════════════════
       AVIS
       ══════════════════════════════════════════════════════ */
    /* ─── Charger SEULEMENT les avis APPROUVÉS ─── */
    private void chargerAvis() {
        try {
            List<Avis> list = avisService.getApprouvesByHebergement(
                    hebergement.getId());
            int total = avisService.countApprouves(hebergement.getId());
            lblAvisCount.setText(total + " avis");
            avisContainer.getChildren().clear();
            for (Avis av : list)
                avisContainer.getChildren().add(buildAvisCard(av));
        } catch (SQLException e) {
            showAlert("Erreur chargement avis : " + e.getMessage());
        }
    }

    /* ─── Publier avis → EN_ATTENTE ─── */
    @FXML
    private void onEnvoyerAvis() {
        if (currentUser == null) return;
        String texte = commentaireField.getText().trim();

        if (texte.length() < 5) { showErr("⚠ Minimum 5 caractères."); return; }
        if (texte.length() > 500) { showErr("⚠ Maximum 500 caractères."); return; }
        hideErr();

        try {
            String photoPath = null;
            if (selectedPhoto != null) {
                try {
                    photoPath = cloudinaryService.uploadAvisImage(selectedPhoto);
                } catch (Exception e) {
                    showAlert("Erreur upload photo : " + e.getMessage());
                    return;
                }
            }

            if (editingAvis != null) {
                editingAvis.setCommentaire(texte);
                if (photoPath != null) editingAvis.setImagePath(photoPath);
                avisService.modifier(editingAvis); // remet EN_ATTENTE
                editingAvis = null;
                btnEnvoyerAvis.setText("📤  Publier mon avis");
            } else {
                Avis avis = new Avis(
                        currentUser.getId(),
                        hebergement.getId(),
                        texte, photoPath);
                avisService.ajouter(avis);
            }

            // Reset formulaire
            commentaireField.clear();
            selectedPhoto = null;
            lblPhotoChoisie.setText("Aucune photo choisie");
            photoPreview.setVisible(false);
            photoPreview.setManaged(false);

            // ✅ Message EN_ATTENTE au lieu de recharger
            showEnAttentePopup();

        } catch (SQLException e) {
            showAlert("Erreur : " + e.getMessage());
        }
    }

    /* ─── Popup "en attente de validation" ─── */
    private void showEnAttentePopup() {
        Stage popup = new Stage();
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initOwner(commentaireField.getScene().getWindow());

        Label icon = new Label("⏳");
        icon.setStyle("-fx-font-size:40px;");

        Label msg = new Label("Votre avis a été soumis !\nIl sera visible après validation par l'administrateur.");
        msg.setStyle("-fx-font-size:13px; -fx-font-weight:bold;"
                + "-fx-text-fill:#0f172a; -fx-text-alignment:center;");
        msg.setWrapText(true);
        msg.setMaxWidth(260);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button ok = new Button("OK");
        ok.setStyle("-fx-background-color:#0ea5e9; -fx-text-fill:white;"
                + "-fx-font-weight:bold; -fx-background-radius:8;"
                + "-fx-padding:9 48 9 48; -fx-cursor:hand;"
                + "-fx-border-width:0; -fx-font-size:13px;");
        ok.setOnAction(e -> { popup.close(); chargerAvis(); });

        VBox box = new VBox(14, icon, msg, ok);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32, 36, 28, 36));
        box.setPrefWidth(320);
        box.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),20,0,0,5);"
                + "-fx-border-color:#e2e8f0; -fx-border-radius:14; -fx-border-width:1;");

        javafx.scene.Scene scene = new javafx.scene.Scene(box);
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
        // scroll vers le formulaire
        commentaireField.getParent().getParent()
                .fireEvent(new javafx.scene.input.ScrollEvent(
                        javafx.scene.input.ScrollEvent.SCROLL,
                        0, 0, 0, 0, false, false, false, false,
                        true, false, 0, -300, 0, -300,
                        javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE,
                        0, javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.LINES,
                        3, 3, null));
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

        // ── Header : avatar + username + date ──
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

        // ── Texte ──
        Label texte = new Label(av.getCommentaire());
        texte.setWrapText(true);
        texte.setStyle("-fx-font-size:13px; -fx-text-fill:#334155; -fx-line-spacing:3;");

        card.getChildren().addAll(header, texte);

        // ── Photo jointe ──
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
                    iv.setStyle("-fx-border-radius:8; -fx-background-radius:8;");
                    card.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        // ── Actions (seulement si c'est l'avis du user connecté) ──
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
}