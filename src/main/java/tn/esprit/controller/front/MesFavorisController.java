package tn.esprit.controller.front;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.controller.front.modals.ActivityDetailController;
import tn.esprit.controller.front.modals.ActivityReservationController;
import tn.esprit.controller.front.modals.ActivityScheduleController;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.models.hebergements.Equipement;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityFavoriteService;
import tn.esprit.services.activity.ActivityImageService;
import tn.esprit.services.activity.ActivityMapService;
import tn.esprit.services.activity.ActivityService;
import tn.esprit.services.hebergement.CategorieH_service;
import tn.esprit.services.hebergement.FavoriHebergement_service;
import tn.esprit.services.hebergement.HebergementEquipement_service;
import tn.esprit.services.hebergement.Hebergement_service;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.CartManager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MesFavorisController implements Initializable {

    // ── Recherche & compteur ──────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     resultCountLabel;

    // ── Onglets ───────────────────────────────────────────────────────────────
    @FXML private Button tabAll;
    @FXML private Button tabHebergements;
    @FXML private Button tabActivites;

    // ── Section hébergements ──────────────────────────────────────────────────
    @FXML private VBox     sectionHebergements;
    @FXML private FlowPane favorisContainer;
    @FXML private Label    hebCountBadge;
    @FXML private VBox     emptyHebergements;

    // ── Séparateur ────────────────────────────────────────────────────────────
    @FXML private HBox sectionDivider;

    // ── Section activités ─────────────────────────────────────────────────────
    @FXML private VBox     sectionActivites;
    @FXML private FlowPane favoritesGrid;
    @FXML private Label    actCountBadge;
    @FXML private VBox     emptyState;

    // ── Empty global ──────────────────────────────────────────────────────────
    @FXML private VBox emptyGlobal;

    // ── Pagination ────────────────────────────────────────────────────────────
    @FXML private HBox   paginationBar;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label  pagInfo;

    private static final int    PER_PAGE    = 6;
    private              int    currentPage = 1;
    private List<Object> filteredData       = new ArrayList<>();

    // ── Services ──────────────────────────────────────────────────────────────
    private final ActivityService           activityService = new ActivityService();
    private final ActivityFavoriteService   actFavService   = new ActivityFavoriteService();
    private final FavoriHebergement_service hebFavService   = new FavoriHebergement_service();
    private final Hebergement_service       hebService      = new Hebergement_service();

    private static final String UPLOADS_DIR = "uploads/hebergements/";

    // ── Données ───────────────────────────────────────────────────────────────
    private List<Activity>    allFavActivities   = new ArrayList<>();
    private List<Hebergement> allFavHebergements = new ArrayList<>();

    // ── Onglet actif ──────────────────────────────────────────────────────────
    private String activeTab = "all";

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        javafx.application.Platform.runLater(() -> {
            if (searchField != null)
                searchField.textProperty().addListener((obs, o, n) -> { currentPage = 1; render(); });
            loadAll();
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CHARGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    private void loadAll() { loadActivities(); loadHebergements(); render(); }

    private void loadActivities() {
        try {
            Set<Integer> ids = actFavService.getFavoriteIds();
            allFavActivities = activityService.afficherAll().stream()
                    .filter(Activity::isActive)
                    .filter(a -> ids.contains(a.getId()))
                    .sorted(Comparator.comparing(Activity::getTitle, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        } catch (Exception e) { allFavActivities = new ArrayList<>(); }
    }

    private void loadHebergements() {
        try {
            int userId = SessionManager.getInstance().isLoggedIn()
                    ? SessionManager.getInstance().getCurrentUser().getId() : 0;
            allFavHebergements = hebFavService.getFavoris(userId);
        } catch (Exception e) { allFavHebergements = new ArrayList<>(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ONGLETS
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void onTabAll()          { currentPage = 1; switchTab("all"); }
    @FXML private void onTabHebergements() { currentPage = 1; switchTab("heb"); }
    @FXML private void onTabActivites()    { currentPage = 1; switchTab("act"); }

    private void switchTab(String tab) {
        activeTab = tab;
        setTabActive(tabAll,          "all".equals(tab));
        setTabActive(tabHebergements, "heb".equals(tab));
        setTabActive(tabActivites,    "act".equals(tab));
        render();
    }

    private void setTabActive(Button btn, boolean active) {
        if (btn == null) return;
        btn.getStyleClass().removeAll("favs-tab-active");
        if (active) btn.getStyleClass().add("favs-tab-active");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RENDU PRINCIPAL
    // ══════════════════════════════════════════════════════════════════════════

    private void render() {
        String search = (searchField == null || searchField.getText() == null)
                ? "" : searchField.getText().trim().toLowerCase();

        List<Hebergement> filteredHeb = allFavHebergements.stream()
                .filter(h -> search.isEmpty()
                        || safeLower(h.getNom()).contains(search)
                        || safeLower(h.getVille()).contains(search)
                        || safeLower(h.getDescription()).contains(search))
                .collect(Collectors.toList());

        List<Activity> filteredAct = allFavActivities.stream()
                .filter(a -> search.isEmpty()
                        || safeLower(a.getTitle()).contains(search)
                        || safeLower(a.getLocation()).contains(search)
                        || safeLower(a.getDescription()).contains(search))
                .collect(Collectors.toList());

        boolean showHeb = !"act".equals(activeTab);
        boolean showAct = !"heb".equals(activeTab);

        filteredData = new ArrayList<>();
        if (showHeb) filteredData.addAll(filteredHeb);
        if (showAct) filteredData.addAll(filteredAct);

        setBadge(hebCountBadge, filteredHeb.size());
        setBadge(actCountBadge, filteredAct.size());

        setVisible(sectionHebergements, showHeb);
        setVisible(sectionActivites,    showAct);
        setVisible(emptyHebergements,   showHeb && filteredHeb.isEmpty());
        setVisible(emptyState,          showAct && filteredAct.isEmpty());

        if (favorisContainer != null) setVisible(favorisContainer, showHeb && !filteredHeb.isEmpty());
        if (favoritesGrid    != null) setVisible(favoritesGrid,    showAct && !filteredAct.isEmpty());

        setVisible(sectionDivider, showHeb && showAct && !filteredHeb.isEmpty() && !filteredAct.isEmpty());

        boolean totallyEmpty = (!showHeb || filteredHeb.isEmpty()) && (!showAct || filteredAct.isEmpty());
        setVisible(emptyGlobal, totallyEmpty);

        int total = (showHeb ? filteredHeb.size() : 0) + (showAct ? filteredAct.size() : 0);
        if (resultCountLabel != null)
            resultCountLabel.setText(total + " favori" + (total > 1 ? "s" : ""));

        int totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PER_PAGE));
        currentPage = Math.min(currentPage, totalPages);
        renderPage();

        boolean showPagination = totalPages > 1;
        setVisible(paginationBar, showPagination);
        if (paginationBar != null) paginationBar.setManaged(showPagination);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PAGINATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void onPrev() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML private void onNext() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PER_PAGE));
        if (currentPage < totalPages) { currentPage++; renderPage(); }
    }

    private void renderPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PER_PAGE));
        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, filteredData.size());

        if (favorisContainer != null) favorisContainer.getChildren().clear();
        if (favoritesGrid    != null) favoritesGrid.getChildren().clear();

        for (int i = from; i < to; i++) {
            Object item = filteredData.get(i);
            if (item instanceof Hebergement h && favorisContainer != null)
                favorisContainer.getChildren().add(buildHebCard(h));
            else if (item instanceof Activity a && favoritesGrid != null)
                favoritesGrid.getChildren().add(buildActivityCard(a));
        }

        if (pagInfo != null) pagInfo.setText("Page " + currentPage + " / " + totalPages);
        if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRE BOUTON
    // ══════════════════════════════════════════════════════════════════════════

    private Button makeBtn(String text, String bg, String fg, String border) {
        Button btn = new Button(text);
        String borderStyle = border != null
                ? "-fx-border-color:" + border + ";-fx-border-radius:8;-fx-border-width:1.5;"
                : "-fx-border-width:0;";
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";"
                + "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:8;"
                + "-fx-padding:7 16 7 16;-fx-cursor:hand;" + borderStyle);
        btn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARTE HÉBERGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildHebCard(Hebergement h) {
        VBox card = new VBox(0);
        card.getStyleClass().add("heb-card");
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.getChildren().add(buildHebImageZone(h));

        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 18, 18));

        if (h.getNb_etoiles() > 0) {
            Label stars = new Label("★".repeat(h.getNb_etoiles())
                    + "☆".repeat(Math.max(0, 5 - h.getNb_etoiles())));
            stars.getStyleClass().add("heb-card-stars");
            body.getChildren().add(stars);
        }

        Label nom = new Label(h.getNom());
        nom.getStyleClass().add("heb-card-nom");
        nom.setWrapText(true);
        body.getChildren().add(nom);

        Label ville = new Label("📍  " + safeText(h.getVille()));
        ville.getStyleClass().add("heb-card-ville");
        body.getChildren().add(ville);

        if (h.getDescription() != null && !h.getDescription().isBlank()) {
            String raw  = h.getDescription();
            String text = raw.length() > 90 ? raw.substring(0, 90) + "…" : raw;
            Label desc  = new Label(text);
            desc.getStyleClass().add("heb-card-desc");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        body.getChildren().add(spacer);

        Button removeBtn = makeBtn("Retirer",     "#fee2e2", "#dc2626", null);
        Button shareBtn  = makeBtn("🔗 Partager", "transparent", "#2d5016", "#2d5016");
        Button detailBtn = makeBtn("Voir détail", "#2d5016", "white", null);

        removeBtn.setOnAction(ev -> {
            try {
                int uid = SessionManager.getInstance().isLoggedIn()
                        ? SessionManager.getInstance().getCurrentUser().getId() : 0;
                hebFavService.supprimer(uid, h.getId());
                loadHebergements();
                render();
            } catch (SQLException e) { e.printStackTrace(); }
        });
        shareBtn.setOnAction(ev  -> showQrPopup(h));
        detailBtn.setOnAction(ev -> openHebergementDetail(h));

        FlowPane btnRow = new FlowPane(8, 8);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));
        btnRow.getChildren().addAll(removeBtn, shareBtn, detailBtn);

        body.getChildren().add(btnRow);
        card.getChildren().add(body);
        card.setOnMouseClicked(ev -> openHebergementDetail(h));
        return card;
    }

    private void openHebergementDetail(Hebergement h) {
        try {
            tn.esprit.controller.front.HebergementDetailController ctrl =
                    SceneManager.navigateToAndGetController(Routes.HEBERGEMENT_DETAIL);
            ctrl.setHebergement(h);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private StackPane buildHebImageZone(Hebergement h) {
        VBox imgBox = new VBox();
        imgBox.getStyleClass().add("heb-card-img");
        imgBox.setPrefHeight(200); imgBox.setMinHeight(200); imgBox.setMaxHeight(200);
        imgBox.setAlignment(Pos.CENTER);

        Image image = loadHebImage(h.getImage_principale());
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(320); iv.setFitHeight(200);
            iv.setPreserveRatio(false); iv.setSmooth(true);
            imgBox.getChildren().add(iv);
        } else {
            Label fallback = new Label("🏨");
            fallback.setStyle("-fx-font-size:52;");
            imgBox.getChildren().add(fallback);
        }

        StackPane stack = new StackPane(imgBox);

        if (h.getLabel_eco() != null && !h.getLabel_eco().isBlank()) {
            Label ecoBadge = new Label(h.getLabel_eco());
            ecoBadge.getStyleClass().add("heb-card-eco-badge");
            stack.getChildren().add(ecoBadge);
            StackPane.setAlignment(ecoBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(ecoBadge, new Insets(14, 14, 0, 0));
        }

        Label favBadge = new Label("♥ Favori");
        favBadge.getStyleClass().add("activity-favorite-badge");
        stack.getChildren().add(favBadge);
        StackPane.setAlignment(favBadge, Pos.TOP_LEFT);
        StackPane.setMargin(favBadge, new Insets(14, 0, 0, 14));
        return stack;
    }

    private Image loadHebImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Image img = new Image(imagePath, 320, 200, false, true, true);
                return img.isError() ? null : img;
            }
            File absFile = new File(imagePath);
            if (absFile.exists())
                return new Image(absFile.toURI().toString(), 320, 200, false, true);
            String fileName  = new File(imagePath).getName();
            File uploadFile  = new File(UPLOADS_DIR + fileName);
            if (uploadFile.exists())
                return new Image(uploadFile.toURI().toString(), 320, 200, false, true);
            URL resource = getClass().getResource("/images/" + fileName);
            if (resource != null)
                return new Image(resource.toExternalForm(), 320, 200, false, true);
        } catch (Exception e) {
            System.err.println("[MesFavoris] Image heb non chargée : " + imagePath);
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARTE ACTIVITÉ
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildActivityCard(Activity activity) {
        VBox card = new VBox(0);
        card.getStyleClass().add("heb-card");
        card.setPrefWidth(320); card.setMaxWidth(320);
        card.getChildren().add(buildActivityImageZone(activity));

        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 18, 18));

        if (activity.getCategory() != null) {
            String icon = activity.getCategory().getIcon() != null
                    && !activity.getCategory().getIcon().isBlank()
                    ? activity.getCategory().getIcon() : "";
            Label cat = new Label((icon.isBlank() ? "" : icon + "  ") + activity.getCategory().getName());
            cat.getStyleClass().add("heb-card-category");
            body.getChildren().add(cat);
        }

        Label title = new Label(activity.getTitle());
        title.getStyleClass().add("heb-card-nom"); title.setWrapText(true);
        body.getChildren().add(title);

        Label location = new Label("📍  " + safeText(activity.getLocation()));
        location.getStyleClass().add("heb-card-ville");
        body.getChildren().add(location);

        if (activity.getDescription() != null && !activity.getDescription().isBlank()) {
            String raw  = activity.getDescription();
            String text = raw.length() > 90 ? raw.substring(0, 90) + "…" : raw;
            Label desc  = new Label(text);
            desc.getStyleClass().add("heb-card-desc"); desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        FlowPane meta = new FlowPane(8, 6);
        Label dur  = new Label("⏱  " + formatDuration(activity.getDurationMinutes()));
        dur.getStyleClass().add("heb-card-equipement");
        Label part = new Label("👥  " + activity.getMaxParticipants() + " pers. max");
        part.getStyleClass().add("heb-card-equipement");
        meta.getChildren().addAll(dur, part);
        if (ActivityMapService.hasValidCoordinates(activity)) {
            Label map = new Label("🗺  Carte disponible");
            map.getStyleClass().add("heb-card-equipement");
            meta.getChildren().add(map);
        }
        body.getChildren().add(meta);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        body.getChildren().add(spacer);

        Label price = new Label(String.format("%.0f TND", activity.getPrice()));
        price.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2d5016;");
        body.getChildren().add(price);

        Button removeBtn = makeBtn("Retirer",     "#fee2e2", "#dc2626", null);
        Button detailBtn = makeBtn("Voir détail", "#2d5016", "white",   null);

        removeBtn.setOnAction(ev -> { actFavService.toggleFavorite(activity.getId()); loadActivities(); render(); });
        detailBtn.setOnAction(ev -> openActivityDetail(activity));

        FlowPane btnRow = new FlowPane(8, 8);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));
        btnRow.getChildren().addAll(removeBtn, detailBtn);
        body.getChildren().add(btnRow);
        card.getChildren().add(body);
        card.setOnMouseClicked(ev -> openActivityDetail(activity));
        return card;
    }

    private StackPane buildActivityImageZone(Activity activity) {
        StackPane imageZone = new StackPane();
        imageZone.getStyleClass().add("heb-card-img");
        imageZone.setPrefHeight(200); imageZone.setMinHeight(200); imageZone.setMaxHeight(200);

        Image image = ActivityImageService.loadImage(getClass(), activity.getImage(), 320, 200);
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(320); iv.setFitHeight(200);
            iv.setPreserveRatio(false); iv.setSmooth(true);
            Rectangle clip = new Rectangle(320, 200);
            clip.setArcWidth(28); clip.setArcHeight(28);
            clip.widthProperty().bind(imageZone.widthProperty());
            clip.heightProperty().bind(imageZone.heightProperty());
            iv.setClip(clip);
            imageZone.getChildren().add(iv);
        } else {
            VBox fallback = new VBox(8);
            fallback.setAlignment(Pos.CENTER);
            fallback.getStyleClass().add("activity-card-image-fallback");
            Label icon = new Label(activity.getCategory() != null
                    && activity.getCategory().getIcon() != null
                    ? activity.getCategory().getIcon() : "🌿");
            icon.getStyleClass().add("activity-card-image-icon");
            Label t = new Label(activity.getTitle());
            t.getStyleClass().add("activity-card-image-text"); t.setWrapText(true);
            fallback.getChildren().addAll(icon, t);
            imageZone.getChildren().add(fallback);
        }

        Label favBadge = new Label("♥ Favori");
        favBadge.getStyleClass().add("activity-favorite-badge");
        imageZone.getChildren().add(favBadge);
        StackPane.setAlignment(favBadge, Pos.TOP_LEFT);
        StackPane.setMargin(favBadge, new Insets(14, 0, 0, 14));
        return imageZone;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION & MODALS
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void handleContact() { SceneManager.navigateTo(Routes.ACTIVITES); }
    @FXML private void onRetour()      { SceneManager.navigateTo(Routes.HEBERGEMENTS); }

    private void openActivityDetail(Activity activity) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/ActivityDetailModal.fxml"));
            StackPane detailOverlay = loader.load();
            StackPane oc = ensureOverlayContainer();
            ActivityDetailController ctrl = loader.getController();
            ctrl.setActivity(activity);
            ctrl.setFavoriteService(actFavService);
            ctrl.setOverlayRoot(detailOverlay);
            ctrl.setOnScheduleRequested(() -> { oc.getChildren().remove(detailOverlay); openScheduleModal(activity, oc); });
            ctrl.setOnReserveRequested(() -> { oc.getChildren().remove(detailOverlay); openReservationModal(activity, oc, null); });
            showOverlay(oc, detailOverlay);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openScheduleModal(Activity activity, StackPane oc) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/ActivityScheduleModal.fxml"));
            StackPane overlay = loader.load();
            ActivityScheduleController ctrl = loader.getController();
            ctrl.setActivity(activity);
            ctrl.setOverlayRoot(overlay);
            ctrl.setOnReserveRequested(schedule -> { oc.getChildren().remove(overlay); openReservationModal(activity, oc, schedule); });
            showOverlay(oc, overlay);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openReservationModal(Activity activity, StackPane oc, ActivitySchedule schedule) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/ActivityReservationModal.fxml"));
            StackPane overlay = loader.load();
            ActivityReservationController ctrl = loader.getController();
            if (schedule != null) ctrl.setPreselectedSchedule(schedule);
            ctrl.setActivity(activity);
            ctrl.setOverlayRoot(overlay);
            ctrl.setOnCartUpdated(() -> {
                if (!oc.getChildren().isEmpty()) oc.getChildren().get(0).setEffect(null);
            });
            showOverlay(oc, overlay);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // QR CODE POPUP
    // ══════════════════════════════════════════════════════════════════════════

    private void showQrPopup(Hebergement h) {

        Stage loadingStage = new Stage();
        loadingStage.initStyle(StageStyle.TRANSPARENT);
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        Label loadingLabel = new Label("⏳  Génération du lien en cours…");
        loadingLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#2d5016;-fx-font-weight:bold;");
        VBox loadingBox = new VBox(loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(28));
        loadingBox.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),16,0,0,4);");
        loadingStage.setScene(new Scene(loadingBox, Color.TRANSPARENT));

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return generateHebHtmlPage(h);
            }
        };

        task.setOnRunning(e -> {
            javafx.scene.Node anchor = favorisContainer != null ? favorisContainer : favoritesGrid;
            if (anchor != null && anchor.getScene() != null) {
                Stage owner = (Stage) anchor.getScene().getWindow();
                loadingStage.setX(owner.getX() + (owner.getWidth()  - 280) / 2);
                loadingStage.setY(owner.getY() + (owner.getHeight() - 100) / 2);
            }
            loadingStage.show();
        });

        task.setOnSucceeded(e -> {
            loadingStage.close();
            String telegraphUrl = task.getValue();
            Image qrImage = generateQrCode(telegraphUrl, 260);
            if (qrImage == null) {
                new Alert(Alert.AlertType.ERROR, "Impossible de générer le QR code.").showAndWait();
                return;
            }
            buildAndShowQrPopup(h, telegraphUrl, qrImage);
        });

        task.setOnFailed(e -> {
            loadingStage.close();
            new Alert(Alert.AlertType.ERROR,
                    "Erreur Telegraph : " + task.getException().getMessage()).showAndWait();
        });

        new Thread(task).start();
    }

    private void buildAndShowQrPopup(Hebergement h, String telegraphUrl, Image qrImage) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 14, 20));
        header.setStyle("-fx-background-color:#2d5016;-fx-background-radius:16 16 0 0;");
        Label hTitle = new Label("🔗  Partager l'hébergement");
        hTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.18);-fx-text-fill:white;"
                + "-fx-font-size:13px;-fx-background-radius:20;-fx-border-width:0;"
                + "-fx-min-width:28;-fx-min-height:28;-fx-cursor:hand;");
        closeBtn.setOnAction(e -> popup.close());
        header.getChildren().addAll(hTitle, hSpacer, closeBtn);

        // Infos
        Label nomLbl = new Label(safeText(h.getNom()));
        nomLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1a2e1a;");
        nomLbl.setWrapText(true);

        Label villeLbl = new Label("📍  " + safeText(h.getVille()));
        villeLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;");

        // Badges
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER);
        if (h.getNb_etoiles() > 0) {
            Label stars = new Label("★".repeat(h.getNb_etoiles()));
            stars.setStyle("-fx-font-size:16px;-fx-text-fill:#f59e0b;");
            badges.getChildren().add(stars);
        }
        if (h.getLabel_eco() != null && !h.getLabel_eco().isBlank()) {
            Label eco = new Label("🌿 " + h.getLabel_eco());
            eco.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#166534;"
                    + "-fx-font-size:11px;-fx-font-weight:bold;"
                    + "-fx-background-radius:20;-fx-padding:3 10 3 10;");
            badges.getChildren().add(eco);
        }

        // QR
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(220); qrView.setFitHeight(220); qrView.setPreserveRatio(true);
        qrView.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),10,0,0,3);");

        Label hint = new Label("Scannez pour ouvrir la fiche complète dans votre navigateur");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");
        hint.setWrapText(true);
        hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // ─────────────────────────────────────────────────────────────────────
        // FIX : "Ouvrir dans le navigateur" — utilise openBrowser() au lieu de
        //        java.awt.Desktop qui échoue dans les apps JavaFX car AWT et FX
        //        tournent sur des threads différents et Desktop peut ne pas être
        //        supporté selon la JVM / le système.
        // ─────────────────────────────────────────────────────────────────────
        Button openBtn = new Button("🌐 Ouvrir dans le navigateur");
        openBtn.setStyle("-fx-background-color:#2d5016;-fx-text-fill:white;"
                + "-fx-font-size:12px;-fx-font-weight:bold;"
                + "-fx-background-radius:8;-fx-border-width:0;"
                + "-fx-padding:8 20 8 20;-fx-cursor:hand;");
        openBtn.setOnAction(e -> openBrowser(telegraphUrl));

        // Body
        VBox body = new VBox(12);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(20, 28, 20, 28));
        body.setStyle("-fx-background-color:white;");
        body.getChildren().addAll(nomLbl, villeLbl, badges, qrView, hint, openBtn);

        // Footer
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#475569;"
                + "-fx-font-size:13px;-fx-font-weight:bold;"
                + "-fx-background-radius:10;-fx-border-width:0;"
                + "-fx-padding:10 48 10 48;-fx-cursor:hand;");
        fermerBtn.setOnAction(e -> popup.close());
        HBox footer = new HBox(fermerBtn);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 28, 18, 28));
        footer.setStyle("-fx-background-color:white;-fx-background-radius:0 0 16 16;");

        VBox root = new VBox(header, body, footer);
        root.setStyle("-fx-background-color:white;-fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.28),32,0,0,8);");
        root.setPrefWidth(360);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);

        popup.setOnShown(e -> {
            javafx.scene.Node anchor = favorisContainer != null ? favorisContainer : favoritesGrid;
            if (anchor != null && anchor.getScene() != null) {
                Stage owner = (Stage) anchor.getScene().getWindow();
                popup.setX(owner.getX() + (owner.getWidth()  - popup.getWidth())  / 2);
                popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
            }
        });
        popup.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OUVERTURE NAVIGATEUR — CROSS-PLATFORM
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ouvre une URL dans le navigateur par défaut du système, de façon fiable
     * sur Windows, macOS et Linux, sans dépendre de java.awt.Desktop (qui peut
     * échouer dans une app JavaFX pure).
     *
     * Stratégie en cascade :
     *  1. java.awt.Desktop si supporté (fonctionne sur la plupart des Windows/macOS).
     *  2. Runtime.exec() avec la commande native adaptée au système.
     *  3. Afficher une alerte de secours avec l'URL copiable.
     */
    private void openBrowser(String url) {
        // Stratégie 1 : java.awt.Desktop (doit être appelé sur le thread AWT,
        // pas le thread JavaFX — on utilise donc SwingUtilities.invokeLater)
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        desktop.browse(new java.net.URI(url));
                        return;
                    } catch (Exception ex) {
                        System.err.println("[Browser] Desktop.browse() échoué : " + ex.getMessage());
                    }
                    // Repli Runtime si Desktop a échoué depuis AWT thread
                    openBrowserViaRuntime(url);
                });
                return;
            }
        }

        // Stratégie 2 : commande système native
        openBrowserViaRuntime(url);
    }

    /**
     * Lance le navigateur via Runtime.exec() selon l'OS détecté.
     * Appelé depuis un thread non-JavaFX ; toute alerte UI est renvoyée
     * sur le JavaFX Application Thread.
     */
    private void openBrowserViaRuntime(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                // Windows : "rundll32 url.dll,FileProtocolHandler <url>"
                // ou plus simplement "cmd /c start <url>"
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                // Linux / Unix — xdg-open est le standard freedesktop
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception ex) {
            System.err.println("[Browser] Runtime.exec() échoué : " + ex.getMessage());
            // Stratégie 3 : alerte de secours avec l'URL copiable
            javafx.application.Platform.runLater(() -> showCopyUrlAlert(url));
        }
    }

    /**
     * Alerte de dernier recours : affiche l'URL pour que l'utilisateur la copie.
     */
    private void showCopyUrlAlert(String url) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ouvrir dans le navigateur");
        alert.setHeaderText("Impossible d'ouvrir le navigateur automatiquement.");
        alert.setContentText("Copiez ce lien et collez-le dans votre navigateur :\n\n" + url);
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GÉNÉRATION PAGE TELEGRAPH
    // ══════════════════════════════════════════════════════════════════════════

    private String generateHebHtmlPage(Hebergement h) throws Exception {

        String categorieNom = "";
        try {
            CategorieH_service catService = new CategorieH_service();
            String n = catService.getNomById(h.getCategorie_id());
            if (n != null) categorieNom = n;
        } catch (Exception ignored) {}

        List<String> equipNoms = new ArrayList<>();
        try {
            HebergementEquipement_service equipService = new HebergementEquipement_service();
            List<Equipement> equips = equipService.getEquipementsByHebergement(h.getId());
            for (Equipement eq : equips) equipNoms.add(eq.getNom());
        } catch (Exception ignored) {}

        String contentJson = buildTelegraphContent(h, categorieNom, equipNoms);
        String title       = "EcoTrip - " + safeText(h.getNom());
        return uploadToTelegraph(title, contentJson);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTION CONTENU TELEGRAPH
    // ══════════════════════════════════════════════════════════════════════════

    private String buildTelegraphContent(Hebergement h, String categorieNom, List<String> equipNoms) {
        StringBuilder json = new StringBuilder("[");

        String imgSrc = safeText(h.getImage_principale());
        if (imgSrc.startsWith("http://") || imgSrc.startsWith("https://")) {
            json.append("{\"tag\":\"figure\",\"children\":[")
                    .append("{\"tag\":\"img\",\"attrs\":{\"src\":\"").append(escJson(imgSrc)).append("\"}}")
                    .append("]},");
        }

        json.append("{\"tag\":\"h3\",\"children\":[\"").append(escJson(safeText(h.getNom()))).append("\"]},");
        json.append("{\"tag\":\"p\",\"children\":[\"📍 ").append(escJson(safeText(h.getVille()))).append("\"]},");

        if (h.getAdresse() != null && !h.getAdresse().isBlank())
            json.append("{\"tag\":\"p\",\"children\":[\"🏠 ").append(escJson(h.getAdresse())).append("\"]},");

        if (h.getNb_etoiles() > 0)
            json.append("{\"tag\":\"p\",\"children\":[\"⭐ ")
                    .append("★".repeat(h.getNb_etoiles()))
                    .append(" (").append(h.getNb_etoiles()).append("/5)\"]},");

        if (h.getLabel_eco() != null && !h.getLabel_eco().isBlank())
            json.append("{\"tag\":\"p\",\"children\":[\"🌿 Label Éco : ").append(escJson(h.getLabel_eco())).append("\"]},");

        if (!categorieNom.isBlank())
            json.append("{\"tag\":\"p\",\"children\":[\"🏷 Catégorie : ").append(escJson(categorieNom)).append("\"]},");

        json.append("{\"tag\":\"hr\"},");

        if (h.getDescription() != null && !h.getDescription().isBlank()) {
            json.append("{\"tag\":\"h4\",\"children\":[\"Description\"]},");
            json.append("{\"tag\":\"p\",\"children\":[\"").append(escJson(h.getDescription())).append("\"]},");
            json.append("{\"tag\":\"hr\"},");
        }

        if (!equipNoms.isEmpty()) {
            json.append("{\"tag\":\"h4\",\"children\":[\"Équipements & Services\"]},");
            json.append("{\"tag\":\"ul\",\"children\":[");
            for (int i = 0; i < equipNoms.size(); i++) {
                json.append("{\"tag\":\"li\",\"children\":[\"").append(escJson(equipNoms.get(i))).append("\"]}");
                if (i < equipNoms.size() - 1) json.append(",");
            }
            json.append("]},");
            json.append("{\"tag\":\"hr\"},");
        }

        json.append("{\"tag\":\"p\",\"children\":[\"— Généré par EcoTrip —\"]}");
        json.append("]");
        return json.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPLOAD TELEGRAPH
    // ══════════════════════════════════════════════════════════════════════════

    private String uploadToTelegraph(String titre, String contentJson) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

        java.net.http.HttpRequest accountReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.telegra.ph/createAccount"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers
                        .ofString("short_name=EcoTrip&author_name=EcoTrip+App"))
                .build();
        String accountJson = client.send(accountReq,
                java.net.http.HttpResponse.BodyHandlers.ofString()).body();
        String token = extractJsonValue(accountJson, "access_token");
        if (token == null || token.isBlank())
            throw new Exception("Telegraph createAccount échoué : " + accountJson);

        String pageTitle = titre.length() > 50 ? titre.substring(0, 50) : titre;
        String body = "access_token=" + urlEncode(token)
                + "&title="   + urlEncode(pageTitle)
                + "&content=" + urlEncode(contentJson)
                + "&return_content=false";

        java.net.http.HttpRequest pageReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.telegra.ph/createPage"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        String pageJson = client.send(pageReq,
                java.net.http.HttpResponse.BodyHandlers.ofString()).body();

        String url = extractJsonValue(pageJson, "url");
        if (url == null || url.isBlank())
            throw new Exception("Telegraph createPage échoué : " + pageJson);

        return url;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GÉNÉRATION QR CODE
    // ══════════════════════════════════════════════════════════════════════════

    private Image generateQrCode(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage buffered = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    buffered.setRGB(x, y, matrix.get(x, y) ? 0xFF1a2e1a : 0xFFFFFFFF);
            return SwingFXUtils.toFXImage(buffered, null);
        } catch (WriterException e) {
            System.err.println("[QR] Erreur : " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CHARGEMENT IMAGE EN BYTES
    // ══════════════════════════════════════════════════════════════════════════

    private byte[] loadImageAsBytes(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                try (InputStream in = new java.net.URL(imagePath).openStream()) {
                    return in.readAllBytes();
                }
            }
            File abs = new File(imagePath);
            if (abs.exists()) return Files.readAllBytes(abs.toPath());
            String fileName   = new File(imagePath).getName();
            File   uploadFile = new File(UPLOADS_DIR + fileName);
            if (uploadFile.exists()) return Files.readAllBytes(uploadFile.toPath());
            URL res = getClass().getResource("/images/" + fileName);
            if (res != null) {
                try (InputStream in = res.openStream()) { return in.readAllBytes(); }
            }
        } catch (Exception e) {
            System.err.println("[HTML] Image non chargée : " + imagePath);
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════════

    private StackPane ensureOverlayContainer() {
        Parent rootNode = (favoritesGrid != null && favoritesGrid.getScene() != null)
                ? favoritesGrid.getScene().getRoot() : favorisContainer.getScene().getRoot();
        if (rootNode instanceof StackPane sp) return sp;
        StackPane oc = new StackPane();
        Scene scene  = rootNode.getScene();
        oc.getChildren().add(rootNode);
        scene.setRoot(oc);
        return oc;
    }

    private void showOverlay(StackPane oc, StackPane overlay) {
        oc.getChildren().add(overlay);
        if (!oc.getChildren().isEmpty()) oc.getChildren().get(0).setEffect(new GaussianBlur(8));
    }

    private void setVisible(Region node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible); node.setManaged(visible);
    }

    private void setBadge(Label badge, int count) {
        if (badge == null) return;
        badge.setText(String.valueOf(count));
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " min";
        int hh = minutes / 60, mm = minutes % 60;
        return mm == 0 ? hh + "h" : hh + "h" + mm + "min";
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", " ");
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String safeLower(String v) { return v == null ? "" : v.toLowerCase(); }
    private String safeText(String v)  { return v == null ? "" : v; }
}