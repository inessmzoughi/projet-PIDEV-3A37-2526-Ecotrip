package tn.esprit.controller.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.esprit.models.produit.Product;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.TranslationService;
import tn.esprit.utils.CartManager;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ProductDetailController implements Initializable {

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private Label             lblNom;
    @FXML private Label             lblPrix;
    @FXML private Label             lblPrixLabel;
    @FXML private Label             lblStock;
    @FXML private Label             lblStatut;
    @FXML private Label             lblAnalyseIA;
    @FXML private Label             lblRecoTitle;
    @FXML private VBox              loadingBox;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label             lblLoadingText;
    @FXML private Label             lblAiConseil;
    @FXML private Button            btnAddToCart;
    @FXML private Button            btnBack;
    @FXML private VBox              recommendationsSection;
    @FXML private FlowPane          recommendedCardsBox;

    // ── Language selector ─────────────────────────────────────────────────────
    @FXML private ComboBox<String> languageSelect;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String API_KEY    = "";
    private static final String API_URL    = "";
    private static final String GREEN_DARK = "";
    private static final String GREEN_MED  = "";

    // ── State ─────────────────────────────────────────────────────────────────
    private Product       currentProduct;
    private List<Product> allProducts;

    // ── Translation ───────────────────────────────────────────────────────────
    private String currentLang = "fr";
    private final Map<String, Map<String, String>> translationCache = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ── Language selector setup ───────────────────────────────────────────
        languageSelect.getItems().addAll("🇫🇷 Français", "🇬🇧 English", "🇪🇸 Español");
        languageSelect.setValue("🇫🇷 Français");
        languageSelect.valueProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            if      (n.contains("English")) currentLang = "en";
            else if (n.contains("Español")) currentLang = "es";
            else                            currentLang = "fr";
            // Re-apply translations to static labels
            applyTranslations();
            // Re-populate product info with translated text
            if (currentProduct != null) populateProductInfo();
        });
    }

    /**
     * Called by ProductsController after loading this FXML via SceneManager.
     */
    public void initData(Product p, List<Product> allProducts) {
        this.currentProduct = p;
        this.allProducts    = allProducts;
        applyTranslations();
        populateProductInfo();
        loadAIRecommendations();
    }

    // ── Translation ───────────────────────────────────────────────────────────

    /** Translate with cache. Returns original if lang == fr. */
    private String t(String text) {
        if (text == null || text.isEmpty() || currentLang.equals("fr")) return text;
        translationCache.putIfAbsent(text, new HashMap<>());
        if (translationCache.get(text).containsKey(currentLang)) {
            return translationCache.get(text).get(currentLang);
        }
        String translated = TranslationService.translate(text, currentLang);
        translationCache.get(text).put(currentLang, translated);
        return translated;
    }

    /** Update all static UI labels based on current language. */
    private void applyTranslations() {
        btnBack.setText("← " + t("Retour aux produits"));
        lblPrixLabel.setText(t("Prix"));
        lblAnalyseIA.setText(t("Analyse IA"));
        lblRecoTitle.setText(t("Produits similaires recommandés"));
        lblLoadingText.setText("⏳ " + t("Recherche de produits similaires..."));
        btnAddToCart.setText("🛒  " + t("Ajouter au panier"));
    }

    // ── Back button ───────────────────────────────────────────────────────────

    @FXML
    private void onBack() {
        SceneManager.navigateTo(Routes.FRONT_PRODUCTS);
    }

    // ── Add to cart ───────────────────────────────────────────────────────────

    @FXML
    private void onAddToCart() {
        CartManager.getInstance().addProduct(currentProduct);
        btnAddToCart.setText("✓  " + t("Ajouté au panier !"));
        btnAddToCart.setStyle(
                "-fx-background-color:#4CAF50;-fx-text-fill:white;" +
                        "-fx-font-size:14px;-fx-font-weight:bold;" +
                        "-fx-padding:11 0 11 0;-fx-background-radius:10;-fx-cursor:hand;"
        );
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                btnAddToCart.setText("🛒  " + t("Ajouter au panier"));
                btnAddToCart.setStyle(
                        "-fx-background-color:" + GREEN_DARK + ";-fx-text-fill:white;" +
                                "-fx-font-size:14px;-fx-font-weight:bold;" +
                                "-fx-padding:11 0 11 0;-fx-background-radius:10;-fx-cursor:hand;"
                );
            });
        }).start();
    }

    // ── Product info ──────────────────────────────────────────────────────────

    private void populateProductInfo() {
        // Translate product name
        lblNom.setText(t(currentProduct.getNom()));
        lblPrix.setText(String.format("%.2f TND", currentProduct.getPrix()));

        int stock = currentProduct.getStock();
        lblStock.setText(stock + " " + t("unités disponibles"));

        if (stock <= 10) {
            lblStatut.setText("⚠  " + t("Stock faible"));
            lblStatut.setStyle(
                    "-fx-background-color:#fff3cd;-fx-text-fill:#856404;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-padding:4 12 4 12;-fx-background-radius:10;"
            );
        } else {
            lblStatut.setText("✔  " + t("Disponible"));
            lblStatut.setStyle(
                    "-fx-background-color:#d4edda;-fx-text-fill:#155724;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-padding:4 12 4 12;-fx-background-radius:10;"
            );
        }
    }

    // ── AI Recommendations ────────────────────────────────────────────────────

    private void loadAIRecommendations() {
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        lblAiConseil.setVisible(false);
        lblAiConseil.setManaged(false);
        recommendationsSection.setVisible(false);
        recommendationsSection.setManaged(false);

        new Thread(() -> {
            List<Product> candidates = getCandidates();

            String        conseil;
            List<Product> recommended;

            if (candidates.isEmpty()) {
                recommended = new ArrayList<>();
                conseil     = t("Aucun produit similaire trouvé dans cette gamme de prix.");
            } else {
                String        json = callGroqAPI(candidates);
                List<Integer> ids  = parseRecommendedIds(json);
                conseil            = parseConseil(json);
                recommended        = findProducts(ids, candidates);

                if (recommended.isEmpty()) {
                    recommended = candidates.subList(0, Math.min(3, candidates.size()));
                }
            }

            final List<Product> finalRec     = recommended;
            final String        finalConseil = conseil;

            Platform.runLater(() -> {
                loadingBox.setVisible(false);
                loadingBox.setManaged(false);

                if (finalConseil != null && !finalConseil.isBlank()) {
                    lblAiConseil.setText("💡 " + finalConseil);
                    lblAiConseil.setVisible(true);
                    lblAiConseil.setManaged(true);
                }

                if (!finalRec.isEmpty()) {
                    recommendedCardsBox.getChildren().clear();
                    for (Product p : finalRec) {
                        recommendedCardsBox.getChildren().add(buildMiniCard(p));
                    }
                    recommendationsSection.setVisible(true);
                    recommendationsSection.setManaged(true);
                }
            });
        }).start();
    }

    /**
     * Pre-filter: exclude current product + keep only ±30 TND price range.
     */
    private List<Product> getCandidates() {
        List<Product> result = new ArrayList<>();
        if (allProducts == null) return result;
        double ref = currentProduct.getPrix();
        for (Product p : allProducts) {
            if (p.getId() == currentProduct.getId()) continue;
            if (p.getNom().equalsIgnoreCase(currentProduct.getNom())) continue;
            if (Math.abs(p.getPrix() - ref) <= 30.0) result.add(p);
        }
        return result;
    }

    // ── Groq API call ─────────────────────────────────────────────────────────

    private String callGroqAPI(List<Product> candidates) {
        try {
            String jsonBody = "{"
                    + "\"model\": \"llama-3.1-8b-instant\","
                    + "\"messages\": [{\"role\": \"user\", \"content\": \""
                    + escapeJson(buildPrompt(candidates)) + "\"}]"
                    + "}";

            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Groq status : " + response.statusCode());
            return extractContent(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String buildPrompt(List<Product> candidates) {
        StringBuilder prodList = new StringBuilder();
        for (Product p : candidates) {
            prodList.append("ID:").append(p.getId())
                    .append(" Nom:").append(p.getNom())
                    .append(" Prix:").append(String.format("%.2f", p.getPrix()))
                    .append(" TND\n");
        }
        return "Tu es un assistant de recommandation produit pour EcoTrip.\n\n"
                + "Produit affiché (NE PAS recommander celui-ci) :\n"
                + "ID:" + currentProduct.getId()
                + " Nom:" + currentProduct.getNom()
                + " Prix:" + String.format("%.2f", currentProduct.getPrix()) + " TND\n\n"
                + "Produits candidats (prix déjà filtré à ±30 TND) :\n"
                + prodList + "\n"
                + "RÈGLES STRICTES :\n"
                + "1. Recommande 2 à 3 produits UNIQUEMENT parmi les candidats ci-dessus\n"
                + "2. N'invente aucun produit\n"
                + "3. Ne recommande JAMAIS le produit affiché (ID:" + currentProduct.getId() + ")\n"
                + "4. Réponds UNIQUEMENT en JSON valide, sans texte avant ou après\n\n"
                + "Format JSON EXACT :\n"
                + "{\"ids\":[ID1,ID2],\"conseil\":\"Une phrase de conseil en français.\"}";
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private List<Integer> parseRecommendedIds(String content) {
        List<Integer> ids = new ArrayList<>();
        try {
            int jsonStart = content.indexOf('{');
            int jsonEnd   = content.lastIndexOf('}');
            if (jsonStart == -1 || jsonEnd == -1) return ids;
            String jsonStr = content.substring(jsonStart, jsonEnd + 1);

            int idsStart = jsonStr.indexOf("\"ids\"");
            if (idsStart == -1) return ids;
            int arrStart = jsonStr.indexOf('[', idsStart);
            int arrEnd   = jsonStr.indexOf(']', arrStart);
            if (arrStart == -1 || arrEnd == -1) return ids;

            for (String part : jsonStr.substring(arrStart + 1, arrEnd).split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) ids.add(Integer.parseInt(trimmed));
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing IDs : " + e.getMessage());
        }
        return ids;
    }

    private String parseConseil(String content) {
        try {
            int jsonStart = content.indexOf('{');
            int jsonEnd   = content.lastIndexOf('}');
            if (jsonStart == -1 || jsonEnd == -1) return null;
            String jsonStr = content.substring(jsonStart, jsonEnd + 1);

            String marker = "\"conseil\":\"";
            int start = jsonStr.indexOf(marker);
            if (start == -1) { marker = "\"conseil\": \""; start = jsonStr.indexOf(marker); }
            if (start == -1) return null;
            start += marker.length();

            StringBuilder sb = new StringBuilder();
            for (int i = start; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                if (c == '\\' && i + 1 < jsonStr.length()) {
                    char next = jsonStr.charAt(i + 1);
                    if      (next == '"') { sb.append('"'); i++; }
                    else if (next == 'n') { sb.append(' '); i++; }
                    else                  { sb.append(c); }
                } else if (c == '"') { break; }
                else { sb.append(c); }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    private List<Product> findProducts(List<Integer> ids, List<Product> candidates) {
        List<Product> result = new ArrayList<>();
        for (int id : ids) {
            for (Product p : candidates) {
                if (p.getId() == id) { result.add(p); break; }
            }
        }
        return result;
    }

    // ── Mini recommendation card ──────────────────────────────────────────────

    private VBox buildMiniCard(Product p) {
        VBox card = new VBox();
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setSpacing(0);

        final String styleNormal =
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-border-color:#e0e0e0;-fx-border-radius:12;-fx-border-width:1;-fx-cursor:hand;";
        final String styleHover =
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-border-color:#2d5a1b;-fx-border-radius:12;-fx-border-width:2;-fx-cursor:hand;";

        card.setStyle(styleNormal);
        card.setEffect(new DropShadow(8, Color.web("#00000015")));
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e  -> card.setStyle(styleNormal));

        // Click on card → open detail for that recommended product
        card.setOnMouseClicked(e -> {
            ProductDetailController ctrl =
                    SceneManager.navigateToAndGetController(Routes.FRONT_PRODUCT_DETAIL);
            if (ctrl != null) ctrl.initData(p, allProducts);
        });

        // Header
        HBox header = new HBox();
        header.setPrefHeight(58);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 12, 0, 12));
        header.setStyle("-fx-background-color:" + GREEN_DARK + ";-fx-background-radius:12 12 0 0;");
        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size:26px;");
        header.getChildren().add(icon);

        // Body
        VBox body = new VBox(7);
        body.setPadding(new Insets(11, 13, 13, 13));

        // Translate product name in card
        Label nom = new Label(t(p.getNom()));
        nom.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_DARK + ";");
        nom.setWrapText(true);

        Separator sep = new Separator();

        HBox priceRow = new HBox(5);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label prix = new Label(String.format("💰 %.2f TND", p.getPrix()));
        prix.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_MED + ";");
        priceRow.getChildren().add(prix);

        HBox stockRow = new HBox(5);
        stockRow.setAlignment(Pos.CENTER_LEFT);
        Label stock = new Label("📦 " + t("Stock") + " : " + p.getStock());
        stock.setStyle("-fx-font-size:11px;-fx-text-fill:#666;");
        stockRow.getChildren().add(stock);
        if (p.getStock() <= 10) {
            Label low = new Label("⚠ " + t("Stock faible"));
            low.setStyle(
                    "-fx-background-color:#fff3cd;-fx-text-fill:#856404;" +
                            "-fx-font-size:10px;-fx-padding:2 6 2 6;-fx-background-radius:8;"
            );
            stockRow.getChildren().add(low);
        }

        Button btnPanier = new Button("🛒  " + t("Ajouter au panier"));
        btnPanier.setMaxWidth(Double.MAX_VALUE);
        final String btnStyle =
                "-fx-background-color:" + GREEN_DARK + ";-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:7 0 7 0;-fx-background-radius:8;-fx-cursor:hand;";
        final String btnHover =
                "-fx-background-color:" + GREEN_MED + ";-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:7 0 7 0;-fx-background-radius:8;-fx-cursor:hand;";
        btnPanier.setStyle(btnStyle);
        btnPanier.setOnMouseEntered(e -> btnPanier.setStyle(btnHover));
        btnPanier.setOnMouseExited(e  -> btnPanier.setStyle(btnStyle));
        btnPanier.setOnAction(e -> {
            e.consume(); // prevent card click from firing
            CartManager.getInstance().addProduct(p);
            btnPanier.setText("✓ " + t("Ajouté !"));
            btnPanier.setStyle(
                    "-fx-background-color:#4CAF50;-fx-text-fill:white;" +
                            "-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-padding:7 0 7 0;-fx-background-radius:8;"
            );
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    btnPanier.setText("🛒  " + t("Ajouter au panier"));
                    btnPanier.setStyle(btnStyle);
                });
            }).start();
        });

        body.getChildren().addAll(nom, sep, priceRow, stockRow, btnPanier);
        card.getChildren().addAll(header, body);
        return card;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String extractContent(String json) {
        try {
            String marker = "\"content\":\"";
            int start = json.indexOf(marker);
            if (start == -1) { marker = "\"content\": \""; start = json.indexOf(marker); }
            if (start == -1) return "";
            start += marker.length();
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if      (next == '"')  { sb.append('"');  i++; }
                    else if (next == 'n')  { sb.append('\n'); i++; }
                    else if (next == 't')  { sb.append('\t'); i++; }
                    else if (next == '\\') { sb.append('\\'); i++; }
                    else                   { sb.append(c); }
                } else if (c == '"') { break; }
                else { sb.append(c); }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}