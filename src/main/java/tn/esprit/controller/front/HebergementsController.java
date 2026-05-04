package tn.esprit.controller.front;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.controller.front.modals.HebergementReservationController;
import tn.esprit.models.Auth_User.User;
import tn.esprit.models.hebergements.Categorie_hebergement;
import tn.esprit.models.hebergements.Chambre;
import tn.esprit.models.hebergements.Equipement;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.CategorieH_service;
import tn.esprit.services.hebergement.Chambre_service;
import tn.esprit.services.hebergement.FavoriHebergement_service;
import tn.esprit.services.hebergement.HebergementEquipement_service;
import tn.esprit.services.hebergement.Hebergement_service;
import tn.esprit.services.hebergement.LikeHebergement_service;
import tn.esprit.session.SessionManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.fxml.FXMLLoader;
import tn.esprit.utils.CartManager;

public class HebergementsController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortSelect;
    @FXML private ComboBox<String> filterVille;
    @FXML private ComboBox<String> filterEtoiles;
    @FXML private FlowPane         cardsPane;
    @FXML private Label            resultCount;
    @FXML private VBox             emptyState;

    /* ── Pagination UI ── */
    @FXML private HBox   paginationBar;
    @FXML private Label  pagInfo;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private final Hebergement_service           service           = new Hebergement_service();
    private final CategorieH_service            categorieService  = new CategorieH_service();
    private final Chambre_service               chambreService    = new Chambre_service();
    private final HebergementEquipement_service equipementService = new HebergementEquipement_service();
    private final LikeHebergement_service       likeService       = new LikeHebergement_service();
    private final FavoriHebergement_service     favoriService     = new FavoriHebergement_service();

    private List<Hebergement> allData;
    private List<Hebergement> filteredData;

    private static final int    PER_PAGE    = 6;
    private static final String UPLOADS_DIR = "uploads/hebergements/";

    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
        setupFilters();
    }

    /* ─────────────── DATA ─────────────── */

    private void loadData() {
        try { allData = service.getAll(); }
        catch (SQLException e) { allData = List.of(); }
        filteredData = allData;
        renderPage();
    }

    /* ─────────────── FILTERS ─────────────── */

    private void setupFilters() {
        filterVille.getItems().add("villes");
        if (allData != null) {
            allData.stream()
                    .map(Hebergement::getVille)
                    .filter(v -> v != null && !v.isBlank())
                    .distinct().sorted()
                    .forEach(filterVille.getItems()::add);
        }
        filterVille.getSelectionModel().selectFirst();

        filterEtoiles.getItems().addAll(
                "etoiles", "★ 1", "★★ 2", "★★★ 3", "★★★★ 4", "★★★★★ 5");
        filterEtoiles.getSelectionModel().selectFirst();
        sortSelect.getSelectionModel().selectFirst();

        searchField.setOnKeyReleased(e -> applyFilters());
        filterVille.setOnAction(e -> applyFilters());
        filterEtoiles.setOnAction(e -> applyFilters());
        sortSelect.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String query  = searchField.getText()  == null ? "" : searchField.getText().toLowerCase().trim();
        String ville  = filterVille.getValue();
        String etoile = filterEtoiles.getValue();
        String sort   = sortSelect.getValue();

        filteredData = allData.stream()
                .filter(h -> query.isEmpty()
                        || h.getNom().toLowerCase().contains(query)
                        || h.getVille().toLowerCase().contains(query)
                        || (h.getDescription() != null && h.getDescription().toLowerCase().contains(query)))
                .filter(h -> ville  == null || ville.equals("villes")  || h.getVille().equals(ville))
                .filter(h -> {
                    if (etoile == null || etoile.equals("etoiles")) return true;
                    String nb = etoile.replaceAll("[^0-9]", "");
                    return !nb.isEmpty() && h.getNb_etoiles() == Integer.parseInt(nb);
                })
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Name (A-Z)" -> filteredData.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
            case "Name (Z-A)" -> filteredData.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
            case "Best Rated" -> filteredData.sort((a, b) -> b.getNb_etoiles() - a.getNb_etoiles());
            case "City (A-Z)" -> filteredData.sort((a, b) -> a.getVille().compareToIgnoreCase(b.getVille()));
            default -> {}
        }

        currentPage = 1;
        renderPage();
    }

    /* ─────────────── PAGINATION ─────────────── */

    private void renderPage() {
        cardsPane.getChildren().clear();

        boolean isEmpty = filteredData == null || filteredData.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        int total = isEmpty ? 0 : filteredData.size();
        resultCount.setText(total + " Accommodation" + (total > 1 ? "s" : "") + " Found");

        if (isEmpty) {
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            return;
        }

        int totalPages = (int) Math.ceil((double) total / PER_PAGE);
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);

        filteredData.subList(from, to).forEach(h -> cardsPane.getChildren().add(buildCard(h)));

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);

        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
    }

    @FXML private void onPrev() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML private void onNext() {
        int totalPages = (int) Math.ceil((double) filteredData.size() / PER_PAGE);
        if (currentPage < totalPages) { currentPage++; renderPage(); }
    }

    /* ─────────────── BUILD CARD ─────────────── */

    private VBox buildCard(Hebergement h) {
        VBox card = new VBox(0);
        card.getStyleClass().add("heb-card");

        // Zone image
        card.getChildren().add(buildImageZone(h));

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(20, 22, 22, 22));

        // Étoiles
        if (h.getNb_etoiles() > 0) {
            Label stars = new Label("★".repeat(h.getNb_etoiles()));
            stars.getStyleClass().add("heb-card-stars");
            body.getChildren().add(stars);
        }

        // Catégorie
        try {
            Categorie_hebergement cat = categorieService.getById(h.getCategorie_id());
            if (cat != null && cat.getNom() != null) {
                Label catLabel = new Label(cat.getNom().toUpperCase());
                catLabel.getStyleClass().add("heb-card-category");
                body.getChildren().add(catLabel);
            }
        } catch (SQLException ignored) {}

        // Nom
        Label nom = new Label(h.getNom());
        nom.getStyleClass().add("heb-card-nom");
        nom.setWrapText(true);
        body.getChildren().add(nom);

        // Ville
        Label ville = new Label("📍  " + h.getVille());
        ville.getStyleClass().add("heb-card-ville");
        body.getChildren().add(ville);

        // Description
        if (h.getDescription() != null && !h.getDescription().isBlank()) {
            String raw  = h.getDescription();
            String text = raw.length() > 100 ? raw.substring(0, 100) + "…" : raw;
            Label desc  = new Label(text);
            desc.getStyleClass().add("heb-card-desc");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        // Équipements
        try {
            List<Equipement> equipements = equipementService.getEquipementsByHebergement(h.getId());
            if (equipements != null && !equipements.isEmpty()) {
                FlowPane equips = new FlowPane(6, 6);
                int max = Math.min(3, equipements.size());
                for (int i = 0; i < max; i++) {
                    Label eq = new Label(equipements.get(i).getNom());
                    eq.getStyleClass().add("heb-card-equipement");
                    equips.getChildren().add(eq);
                }
                if (equipements.size() > 3) {
                    Label more = new Label("+" + (equipements.size() - 3) + " more");
                    more.getStyleClass().add("heb-card-equipement");
                    equips.getChildren().add(more);
                }
                body.getChildren().add(equips);
            }
        } catch (SQLException ignored) {}

        // Prix minimum
        HBox priceBox = new HBox(5);
        priceBox.getStyleClass().add("heb-card-price-box");
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setMaxWidth(Double.MAX_VALUE);
        try {
            List<Chambre> chambres = chambreService.getByHebergement(h.getId());
            Double minPrice = chambres.stream()
                    .mapToDouble(Chambre::getPrix_par_nuit).min()
                    .stream().boxed().findFirst().orElse(null);
            if (minPrice != null) {
                Label amount   = new Label(String.format("%.0f", minPrice));
                Label currency = new Label("TND");
                Label unit     = new Label("/ night");
                amount.getStyleClass().add("heb-card-price-amount");
                currency.getStyleClass().add("heb-card-price-currency");
                unit.getStyleClass().add("heb-card-price-unit");
                priceBox.getChildren().addAll(amount, currency, unit);
            } else {
                Label req = new Label("Price on request");
                req.getStyleClass().add("heb-card-price-request");
                priceBox.getChildren().add(req);
            }
        } catch (SQLException e) {
            Label req = new Label("Price on request");
            req.getStyleClass().add("heb-card-price-request");
            priceBox.getChildren().add(req);
        }
        body.getChildren().add(priceBox);

        // ── Pourcentage de likes ──
        try {
            double pct = likeService.getLikePercentage(h.getId());
            int    nb  = likeService.countLikes(h.getId());

            String emoji = pct >= 75 ? "🔥" : pct >= 40 ? "❤️" : "🤍";
            Label lblPct = new Label(emoji + "  " + nb + " personnes ont aimé  ("
                    + String.format("%.0f", pct) + "%)");
            lblPct.setStyle("-fx-font-size:11px; -fx-text-fill:#2d6a4f; -fx-font-weight:bold;");

            body.getChildren().addAll(lblPct, buildProgressBar(pct));
        } catch (SQLException ignored) {}

        // Bouton Voir détails
        Button btnDetail = new Button("👁 Voir détails");
        btnDetail.getStyleClass().add("heb-card-btn");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setOnAction(e -> ouvrirDetail(h));
        body.getChildren().add(btnDetail);

        // Bouton Réserver
        Button btn = new Button("Réserver");
        btn.getStyleClass().add("heb-card-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> onReserver(h));
        body.getChildren().add(btn);

        card.getChildren().add(body);
        return card;
    }

    /* ─────────────── BARRE DE PROGRESSION LIKES ─────────────── */

    private StackPane buildProgressBar(double pct) {
        HBox bg = new HBox();
        bg.setStyle("-fx-background-color:#e2e8f0; -fx-background-radius:6;");
        bg.setPrefHeight(7);
        bg.setMaxWidth(Double.MAX_VALUE);

        HBox fill = new HBox();
        fill.setPrefHeight(7);
        fill.setPrefWidth(0);

        String color = pct >= 75 ? "#16a34a"
                : pct >= 40 ? "#52b788"
                :             "#86efac";
        fill.setStyle("-fx-background-color:" + color + "; -fx-background-radius:6;");

        StackPane bar = new StackPane(bg, fill);
        bar.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        bar.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0 && fill.getPrefWidth() == 0) {
                double targetW = newW.doubleValue() * pct / 100.0;
                Timeline anim = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(fill.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(900),
                                new KeyValue(fill.prefWidthProperty(),
                                        targetW, Interpolator.EASE_OUT))
                );
                anim.play();
            }
        });

        return bar;
    }

    /* ─────────────── IMAGE ZONE ─────────────── */

    private StackPane buildImageZone(Hebergement h) {
        VBox imgBox = new VBox();
        imgBox.getStyleClass().add("heb-card-img");
        imgBox.setPrefHeight(200);
        imgBox.setMinHeight(200);
        imgBox.setMaxHeight(200);
        imgBox.setAlignment(Pos.CENTER);

        Image image = loadImage(h.getImage_principale());
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(320);
            iv.setFitHeight(200);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            imgBox.getChildren().add(iv);
        } else {
            Label fallback = new Label("🏨");
            fallback.setStyle("-fx-font-size:52;");
            imgBox.getChildren().add(fallback);
        }

        StackPane stack = new StackPane(imgBox);

        // ── Eco badge ──
        if (h.getLabel_eco() != null && !h.getLabel_eco().isBlank()) {
            Label ecoBadge = new Label(h.getLabel_eco());
            ecoBadge.getStyleClass().add("heb-card-eco-badge");
            stack.getChildren().add(ecoBadge);
            StackPane.setAlignment(ecoBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(ecoBadge, new Insets(14, 14, 0, 0));
        }

        // ── Bouton favori ──
        Button btnFavori = new Button("🤍");
        btnFavori.setStyle("-fx-background-color:rgba(255,255,255,0.85);"
                + "-fx-background-radius:50%;"
                + "-fx-min-width:34px; -fx-min-height:34px;"
                + "-fx-max-width:34px; -fx-max-height:34px;"
                + "-fx-font-size:15px; -fx-cursor:hand;"
                + "-fx-border-width:0; -fx-padding:0;");
        stack.getChildren().add(btnFavori);
        StackPane.setAlignment(btnFavori, Pos.TOP_LEFT);
        StackPane.setMargin(btnFavori, new Insets(10, 0, 0, 10));

        // ✅ Fix : relire le user depuis la session à chaque carte
        User user = SessionManager.getInstance().getCurrentUser();

        boolean[] fav = { false };
        if (user != null) {
            try {
                fav[0] = favoriService.isFavori(user.getId(), h.getId());
                btnFavori.setText(fav[0] ? "❤️" : "🤍");
            } catch (SQLException ignored) {}
        }

        // Toggle au clic
        btnFavori.setOnAction(e -> {
            // ✅ Fix : relire le user au moment du clic aussi
            User u = SessionManager.getInstance().getCurrentUser();
            if (u == null) return;
            try {
                fav[0] = favoriService.toggle(u.getId(), h.getId());
                btnFavori.setText(fav[0] ? "❤️" : "🤍");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        return stack;
    }

    /* ─────────────── IMAGE LOADER ─────────────── */

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Image img = new Image(imagePath, 320, 200, false, true, true);
                return img.isError() ? null : img;
            }
            File absFile = new File(imagePath);
            if (absFile.exists()) return new Image(absFile.toURI().toString(), 320, 200, false, true);
            String fileName = new File(imagePath).getName();
            File uploadFile = new File(UPLOADS_DIR + fileName);
            if (uploadFile.exists()) return new Image(uploadFile.toURI().toString(), 320, 200, false, true);
            URL resource = getClass().getResource("/images/" + fileName);
            if (resource != null) return new Image(resource.toExternalForm(), 320, 200, false, true);
        } catch (Exception e) {
            System.err.println("Image non chargée : " + imagePath);
        }
        return null;
    }

    /* ─────────────── HANDLERS ─────────────── */

    private void onReserver(Hebergement h) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/HebergementReservationModal.fxml"));
            StackPane modalOverlay = loader.load();

            HebergementReservationController ctrl = loader.getController();
            ctrl.setHebergement(h);
            ctrl.setOverlayRoot(modalOverlay);

            Parent rootNode = cardsPane.getScene().getRoot();
            StackPane overlayContainer;
            if (rootNode instanceof StackPane) {
                overlayContainer = (StackPane) rootNode;
            } else {
                overlayContainer = new StackPane();
                Scene scene = rootNode.getScene();
                overlayContainer.getChildren().add(rootNode);
                scene.setRoot(overlayContainer);
            }

            overlayContainer.getChildren().add(modalOverlay);
            if (!overlayContainer.getChildren().isEmpty())
                overlayContainer.getChildren().get(0)
                        .setEffect(new javafx.scene.effect.GaussianBlur(8));

            ctrl.setOnCartUpdated(() -> {
                System.out.println("Cart: " + CartManager.getInstance().getCount());
                overlayContainer.getChildren().get(0).setEffect(null);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @FXML
    private void onReset() {
        searchField.clear();
        sortSelect.getSelectionModel().selectFirst();
        filterVille.getSelectionModel().selectFirst();
        filterEtoiles.getSelectionModel().selectFirst();
        filteredData = allData;
        currentPage  = 1;
        renderPage();
    }
}