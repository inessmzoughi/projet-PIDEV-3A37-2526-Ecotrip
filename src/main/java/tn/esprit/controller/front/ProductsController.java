package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.esprit.models.produit.Product;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.produit.ProductService;
import tn.esprit.services.TranslationService;
import tn.esprit.utils.CartManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ProductsController implements Initializable {
    private static final String PRODUCT_UPLOADS_DIR = "uploads/products/";


    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortSelect;
    @FXML private ComboBox<String> filterCategory;
    @FXML private Label            resultCount;
    @FXML private FlowPane         cardsPane;
    @FXML private VBox             emptyState;
    @FXML private HBox             paginationBar;
    @FXML private Button           btnPrev, btnNext;
    @FXML private Label            pagInfo;
    @FXML private Button           btnCart;

    @FXML private ComboBox<String> languageSelect;

    private final ProductService productService = new ProductService();
    private List<Product> allProducts      = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    private static final int PAGE_SIZE = 9;
    private int currentPage = 1;

    private String currentLang = "fr";
    private final Map<String, Map<String, String>> translationCache = new HashMap<>();

    private static final String GREEN_DARK = "#2d5a1b";
    private static final String GREEN_MED  = "#4a7c3f";
    private static final String WHITE      = "#ffffff";
    private static final String GREY_TEXT  = "#666666";
    private static final String BORDER     = "#e0e0e0";

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            allProducts = productService.read();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        filterCategory.getItems().add("All Categories");
        filterCategory.setValue("All Categories");
        sortSelect.setValue("Sort by");

        if (languageSelect != null) {
            languageSelect.getItems().addAll("🇫🇷 Français", "🇬🇧 English", "🇪🇸 Español");
            languageSelect.setValue("🇫🇷 Français");
            languageSelect.valueProperty().addListener((obs, o, n) -> {
                if (n == null) return;
                if      (n.contains("English"))  currentLang = "en";
                else if (n.contains("Español"))  currentLang = "es";
                else                             currentLang = "fr";
                applyFilters();
            });
        }

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        sortSelect.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterCategory.valueProperty().addListener((obs, o, n) -> applyFilters());

        applyFilters();
    }

    // ── Traduction avec cache ─────────────────────────────────────────────────

    private String getTranslated(String text) {
        if (text == null || text.isEmpty()) return text;
        if (currentLang.equals("fr")) return text;
        translationCache.putIfAbsent(text, new HashMap<>());
        if (translationCache.get(text).containsKey(currentLang)) {
            return translationCache.get(text).get(currentLang);
        }
        String translated = TranslationService.translate(text, currentLang);
        translationCache.get(text).put(currentLang, translated);
        return translated;
    }

    // ── Mise à jour des labels UI ─────────────────────────────────────────────

    private void updateUILanguage() {
        resultCount.setText(filteredProducts.size() + " " + getTranslated("Produit(s) trouvé(s)"));
        btnCart.setText("🛒  " + getTranslated("Mon Panier")
                + " (" + CartManager.getInstance().getCount() + ")");
        btnPrev.setText("← " + getTranslated("Précédent"));
        btnNext.setText(getTranslated("Suivant") + " →");
    }

    // ── Filtres ───────────────────────────────────────────────────────────────

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String sort   = sortSelect.getValue();

        filteredProducts = allProducts.stream()
                .filter(p -> p.getNom().toLowerCase().contains(search))
                .collect(Collectors.toList());

        if ("Name (A-Z)".equals(sort))
            filteredProducts.sort(Comparator.comparing(Product::getNom));
        else if ("Name (Z-A)".equals(sort))
            filteredProducts.sort(Comparator.comparing(Product::getNom).reversed());
        else if ("Price (Low-High)".equals(sort))
            filteredProducts.sort(Comparator.comparingDouble(Product::getPrix));
        else if ("Price (High-Low)".equals(sort))
            filteredProducts.sort(Comparator.comparingDouble(Product::getPrix).reversed());

        currentPage = 1;
        renderPage();
    }

    // ── Rendu de la page ──────────────────────────────────────────────────────

    private void renderPage() {
        cardsPane.getChildren().clear();

        boolean isEmpty = filteredProducts.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        updateUILanguage();

        if (isEmpty) {
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            return;
        }

        int totalPages = (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE);
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredProducts.size());
        List<Product> pageProducts = filteredProducts.subList(from, to);

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
        btnPrev.setDisable(currentPage == 1);
        btnNext.setDisable(currentPage == totalPages);

        if (!currentLang.equals("fr")) {
            Label loading = new Label("⏳ " + getTranslated("Chargement") + "...");
            loading.setStyle("-fx-font-size: 14px; -fx-text-fill: " + GREEN_DARK + ";");
            cardsPane.getChildren().add(loading);

            new Thread(() -> {
                for (Product p : pageProducts) getTranslated(p.getNom());
                javafx.application.Platform.runLater(() -> {
                    cardsPane.getChildren().clear();
                    for (Product p : pageProducts)
                        cardsPane.getChildren().add(buildCard(p));
                });
            }).start();
        } else {
            for (Product p : pageProducts)
                cardsPane.getChildren().add(buildCard(p));
        }
    }

    // ── Construction d'une carte produit ─────────────────────────────────────

    private VBox buildCard(Product p) {
        VBox card = new VBox();
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setSpacing(0);
        card.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
        );
        card.setEffect(new DropShadow(12, Color.web("#00000022")));

        final String styleNormal =
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";
        final String styleHover =
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: " + GREEN_MED + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-cursor: hand;";

        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e  -> card.setStyle(styleNormal));
        // Header image
        HBox header = buildImageHeader(p);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(16));

        Label nom = new Label(getTranslated(p.getNom()));
        nom.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
        nom.setWrapText(true);

        Separator sep = new Separator();

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label pIcon = new Label("💰");
        pIcon.setStyle("-fx-font-size: 14px;");
        Label prix = new Label(String.format("%.2f TND", p.getPrix()));
        prix.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_MED + ";");
        priceRow.getChildren().addAll(pIcon, prix);

        HBox stockRow = new HBox(8);
        stockRow.setAlignment(Pos.CENTER_LEFT);
        Label sIcon = new Label("📦");
        sIcon.setStyle("-fx-font-size: 13px;");
        Label stock = new Label(getTranslated("En stock") + " : " + p.getStock());
        stock.setStyle("-fx-font-size: 13px; -fx-text-fill: " + GREY_TEXT + ";");
        stockRow.getChildren().addAll(sIcon, stock);

        if (p.getStock() <= 10) {
            Label low = new Label("⚠ " + getTranslated("Stock faible"));
            low.setStyle(
                    "-fx-background-color: #fff3cd; -fx-text-fill: #856404;" +
                            "-fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-background-radius: 10;"
            );
            stockRow.getChildren().add(low);
        }

        // Bouton "Ajouter au panier"
        Button btnPanier = new Button("🛒  " + getTranslated("Ajouter au panier"));
        btnPanier.setMaxWidth(Double.MAX_VALUE);
        final String panierStyle =
                "-fx-background-color: " + GREEN_DARK + "; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 10 0 10 0; -fx-background-radius: 8; -fx-cursor: hand;";
        final String panierHover =
                "-fx-background-color: " + GREEN_MED + "; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 10 0 10 0; -fx-background-radius: 8; -fx-cursor: hand;";
        btnPanier.setStyle(panierStyle);
        btnPanier.setOnMouseEntered(e -> btnPanier.setStyle(panierHover));
        btnPanier.setOnMouseExited(e  -> btnPanier.setStyle(panierStyle));
        btnPanier.setOnAction(e -> addToCart(p, btnPanier, panierStyle));

        // Bouton "Détails"
        Button btnDetails = new Button("🔍  " + getTranslated("Détails"));
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        final String detailsStyle =
                "-fx-background-color: transparent; -fx-text-fill: " + GREEN_DARK + ";" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 9 0 9 0; -fx-background-radius: 8;" +
                        "-fx-border-color: " + GREEN_DARK + "; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 8; -fx-cursor: hand;";
        final String detailsHover =
                "-fx-background-color: #eaf3de; -fx-text-fill: " + GREEN_DARK + ";" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 9 0 9 0; -fx-background-radius: 8;" +
                        "-fx-border-color: " + GREEN_DARK + "; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 8; -fx-cursor: hand;";
        btnDetails.setStyle(detailsStyle);
        btnDetails.setOnMouseEntered(e -> btnDetails.setStyle(detailsHover));
        btnDetails.setOnMouseExited(e  -> btnDetails.setStyle(detailsStyle));
        btnDetails.setOnAction(e -> openDetail(p));

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(btnPanier,  Priority.ALWAYS);
        HBox.setHgrow(btnDetails, Priority.ALWAYS);
        btnRow.getChildren().addAll(btnPanier, btnDetails);

        body.getChildren().addAll(nom, sep, priceRow, stockRow, btnRow);
        card.getChildren().addAll(header, body);
        return card;
    }

    private HBox buildImageHeader(Product product) {
        HBox header = new HBox();
        header.setPrefHeight(170);
        header.setMinHeight(170);
        header.setMaxHeight(170);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 16, 0, 16));
        header.setStyle("-fx-background-color: " + GREEN_DARK + "; -fx-background-radius: 14 14 0 0;");

        Image image = loadImage(product.getImage());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(300);
            imageView.setFitHeight(170);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            header.getChildren().add(imageView);
            return header;
        }

        Label prodIcon = new Label("🛍");
        prodIcon.setStyle("-fx-font-size: 36px;");
        header.getChildren().add(prodIcon);
        return header;
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Image image = new Image(imagePath, 300, 170, false, true, true);
                return image.isError() ? null : image;
            }

            File absoluteFile = new File(imagePath);
            if (absoluteFile.exists()) {
                return new Image(absoluteFile.toURI().toString(), 300, 170, false, true);
            }

            String fileName = new File(imagePath).getName();
            File uploadFile = new File(PRODUCT_UPLOADS_DIR + fileName);
            if (uploadFile.exists()) {
                return new Image(uploadFile.toURI().toString(), 300, 170, false, true);
            }

            URL resource = getClass().getResource("/images/" + fileName);
            if (resource != null) {
                return new Image(resource.toExternalForm(), 300, 170, false, true);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    // ── ✅ CORRECTION PRINCIPALE ──────────────────────────────────────────────
    //
    // AVANT (bug) :
    //   FXMLLoader loader = new FXMLLoader(fxmlUrl);
    //   Parent root = loader.load();
    //   controller.initData(p, allProducts);
    //   btnCart.getScene().setRoot(root);   ← bypasse le shell → pas de navbar !
    //
    // APRÈS (corrigé) :
    //   SceneManager.navigateToAndGetController() appelle ensureFrontShell()
    //   en interne → le BorderPane shell (avec navbar en top) est garanti
    //   d'être dans la scène AVANT que le contenu soit injecté dans center.
    // ─────────────────────────────────────────────────────────────────────────

    private void openDetail(Product p) {
        ProductDetailController ctrl =
                SceneManager.navigateToAndGetController(Routes.FRONT_PRODUCT_DETAIL);
        if (ctrl != null) {
            ctrl.initData(p, allProducts);
        }
    }

    // ── Ajouter au panier ─────────────────────────────────────────────────────

    private void addToCart(Product p, Button btn, String originalStyle) {
        CartManager.getInstance().addProduct(p);
        btnCart.setText("🛒  " + getTranslated("Mon Panier")
                + " (" + CartManager.getInstance().getCount() + ")");
        btn.setText("✓  " + getTranslated("Ajouté !"));
        btn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 10 0 10 0; -fx-background-radius: 8;"
        );
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                btn.setText("🛒  " + getTranslated("Ajouter au panier"));
                btn.setStyle(originalStyle);
            });
        }).start();
    }

    // ── Handlers FXML ─────────────────────────────────────────────────────────

    @FXML private void onOpenCart() {}

    @FXML
    private void onReset() {
        searchField.clear();
        sortSelect.setValue("Sort by");
        filterCategory.setValue("All Categories");
        if (languageSelect != null) languageSelect.setValue("🇫🇷 Français");
        currentLang = "fr";
        applyFilters();
    }

    @FXML
    private void onPrev() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML
    private void onNext() {
        int totalPages = (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE);
        if (currentPage < totalPages) { currentPage++; renderPage(); }
    }
}

