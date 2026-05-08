package tn.esprit.controller.front;

import tn.esprit.utils.MollieCheckoutWindow;
import tn.esprit.utils.MollieConfig;
import tn.esprit.utils.MolliePayment;
import tn.esprit.utils.MolliePaymentService;
import java.math.BigDecimal;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import tn.esprit.models.produit.Commande;
import tn.esprit.models.produit.LigneCommande;
import tn.esprit.models.produit.Product;
import tn.esprit.services.TranslationService;
import tn.esprit.services.produit.CommandeService;
import tn.esprit.services.produit.LigneCommandeService;
import tn.esprit.services.produit.ProductService;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.CartManager;
import tn.esprit.models.cart.CartItem;
import tn.esprit.services.ReservationService;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    // ── FXML Fields ────────────────────────────────────────────────────────────
    @FXML private VBox        itemsContainer;
    @FXML private VBox        emptyState;
    @FXML private VBox        summaryBox;
    @FXML private HBox        contentBox;
    @FXML private Label       labelTotal;
    @FXML private Label       labelCount;
    @FXML private RadioButton rbCarte;
    @FXML private RadioButton rbPaypal;
    @FXML private RadioButton rbCash;
    @FXML private Button      btnPayer;
    @FXML private Button      btnVider;
    @FXML private Button      btnExporterPDF;

    // ── Labels traduisibles ────────────────────────────────────────────────────
    @FXML private Label lblHeroTitle;
    @FXML private Label lblHeroSub;
    @FXML private Label lblRecap;
    @FXML private Label lblNbArticles;
    @FXML private Label lblTotal;
    @FXML private Label lblModePaiement;
    @FXML private Label lblEmptyTitle;
    @FXML private Label lblEmptySub;

    // ── Langue ────────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> languageSelect;
    private String currentLang = "fr";
    private final Map<String, Map<String, String>> translationCache = new HashMap<>();

    // ── Services ───────────────────────────────────────────────────────────────
    private final CommandeService      commandeService      = new CommandeService();
    private final LigneCommandeService ligneCommandeService = new LigneCommandeService();
    private final ProductService       productService       = new ProductService(); // ✅ AJOUT
    private final CartManager          cart                 = CartManager.getInstance();
    private final ReservationService   reservationService   = new ReservationService();

    // ── Radio groupe paiement ──────────────────────────────────────────────────
    private final ToggleGroup paymentGroup = new ToggleGroup();

    // ── Timer JavaFX (toutes les secondes) ────────────────────────────────────
    private Timeline uiTicker;

    // ── Notification banner ────────────────────────────────────────────────────
    private Label warningBanner;

    // ── Couleurs UI ────────────────────────────────────────────────────────────
    private static final String GREEN_DARK  = "#2d5a1b";
    private static final String GREEN_MED   = "#4a7c3f";
    private static final String WHITE       = "#ffffff";
    private static final String BORDER      = "#e0e0e0";
    private static final String GREY        = "#757575";
    private static final String WARN_BG     = "#fff3cd";
    private static final String WARN_BORDER = "#ffc107";
    private static final String WARN_TEXT   = "#856404";

    // ── Couleurs PDF ───────────────────────────────────────────────────────────
    private static final java.awt.Color PDF_GREEN_DARK  = new java.awt.Color(45,  90,  27);
    private static final java.awt.Color PDF_GREEN_MED   = new java.awt.Color(74, 124,  63);
    private static final java.awt.Color PDF_GREEN_LIGHT = new java.awt.Color(220, 237, 200);
    private static final java.awt.Color PDF_GREY        = new java.awt.Color(100, 100, 100);
    private static final java.awt.Color PDF_WHITE       = java.awt.Color.WHITE;
    private static final java.awt.Color PDF_BORDER      = new java.awt.Color(220, 220, 220);

    // ══════════════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ── 1. Lier les RadioButtons au ToggleGroup ────────────────────────────
        rbCash.setToggleGroup(paymentGroup);
        rbCarte.setToggleGroup(paymentGroup);
        rbPaypal.setToggleGroup(paymentGroup);

        // ── 2. FIX DEFINITIF : forcer le texte des RadioButtons en Java ────────
        rbCash.setText("Cash");
        rbCarte.setText("Par carte bancaire");
        rbPaypal.setText("PayPal");

        // ── 3. Sélectionner Cash par défaut ───────────────────────────────────
        rbCash.setSelected(true);

        // ── 4. Sélecteur de langue ────────────────────────────────────────────
        if (languageSelect != null) {
            languageSelect.getItems().addAll("FR Francais", "EN English", "ES Espanol");
            languageSelect.setValue("FR Francais");
            languageSelect.valueProperty().addListener((obs, o, n) -> {
                if (n == null) return;
                if      (n.contains("English")) currentLang = "en";
                else if (n.contains("Espanol")) currentLang = "es";
                else                            currentLang = "fr";
                applyTranslations();
                refreshCart();
            });
        }

        // ── 5. Callbacks d'expiration ─────────────────────────────────────────
        cart.setOnWarning(label -> showWarningBanner(
                "ATTENTION : L'article \"" + label + "\" sera supprime dans 1 minute !"));

        cart.setOnExpired(label -> {
            hideWarningBanner();
            refreshCart();
            showExpiryNotification(label);
        });

        // ── 6. Ticker UI (1 s) ────────────────────────────────────────────────
        uiTicker = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimerBadges()));
        uiTicker.setCycleCount(Timeline.INDEFINITE);
        uiTicker.play();

        // ── 7. Traductions + rendu ────────────────────────────────────────────
        applyTranslations();
        refreshCart();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRADUCTION
    // ══════════════════════════════════════════════════════════════════════════

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

    private void applyTranslations() {
        if (lblHeroTitle    != null) lblHeroTitle.setText("Mon Panier");
        if (lblHeroSub      != null) lblHeroSub.setText(t("Verifiez vos articles avant de proceder au paiement"));
        if (lblRecap        != null) lblRecap.setText(t("Recapitulatif"));
        if (lblNbArticles   != null) lblNbArticles.setText(t("Nombre d'articles") + " :");
        if (lblTotal        != null) lblTotal.setText(t("Total") + " :");
        if (lblModePaiement != null) lblModePaiement.setText(t("Mode de paiement"));
        if (lblEmptyTitle   != null) lblEmptyTitle.setText(t("Votre panier est vide"));
        if (lblEmptySub     != null) lblEmptySub.setText(t("Retournez a la boutique pour ajouter des produits"));

        if (btnPayer       != null) btnPayer.setText(t("Confirmer la commande"));
        if (btnVider       != null) btnVider.setText(t("Vider le panier"));
        if (btnExporterPDF != null) btnExporterPDF.setText(t("Exporter la commande en PDF"));

        if (rbCash   != null) rbCash.setText(t("Cash"));
        if (rbCarte  != null) rbCarte.setText(t("Par carte bancaire"));
        if (rbPaypal != null) rbPaypal.setText("PayPal");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REFRESH PANIER
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshCart() {
        itemsContainer.getChildren().clear();

        boolean hasProducts     = !cart.getProductItems().isEmpty();
        boolean hasReservations = !cart.getReservationItems().isEmpty();
        boolean empty           = !hasProducts && !hasReservations;

        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        contentBox.setVisible(!empty);
        contentBox.setManaged(!empty);

        if (empty) {
            hideWarningBanner();
            return;
        }

        if (warningBanner != null) {
            itemsContainer.getChildren().add(0, warningBanner);
        }

        // ── Réservations ──────────────────────────────────────────────────────
        if (hasReservations) {
            Label sectionLabel = new Label("Reservations");
            sectionLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + GREEN_DARK + ";");
            itemsContainer.getChildren().add(sectionLabel);
            for (CartItem item : cart.getReservationItems()) {
                itemsContainer.getChildren().add(buildReservationRow(item));
            }
        }

        // ── Produits ──────────────────────────────────────────────────────────
        if (hasProducts) {
            Label sectionLabel = new Label("Produits");
            sectionLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + GREEN_DARK + ";"
                    + (hasReservations ? "-fx-padding:16 0 0 0;" : ""));
            itemsContainer.getChildren().add(sectionLabel);
            for (Map.Entry<Product, Integer> entry : cart.getProductItems().entrySet()) {
                itemsContainer.getChildren().add(buildItemRow(entry.getKey(), entry.getValue()));
            }
        }

        updateSummary();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TIMER BADGES
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void refreshTimerBadges() {
        itemsContainer.lookupAll(".timer-badge").forEach(node -> {
            if (!(node instanceof Label badge)) return;
            Object ud = badge.getUserData();
            if (ud == null) return;

            long remaining;
            boolean warning;
            if (ud instanceof Product p) {
                remaining = cart.getRemainingMillis(p);
                warning   = cart.isWarning(p);
            } else if (ud instanceof CartItem ci) {
                remaining = cart.getRemainingMillis(ci);
                warning   = cart.isWarning(ci);
            } else return;

            badge.setText("Timer : " + CartManager.formatRemaining(remaining));

            if (warning) {
                badge.setStyle(
                        "-fx-background-color:" + WARN_BG + ";" +
                                "-fx-text-fill:" + WARN_TEXT + ";" +
                                "-fx-border-color:" + WARN_BORDER + ";" +
                                "-fx-border-width:1; -fx-border-radius:4;" +
                                "-fx-background-radius:4; -fx-padding:2 8; -fx-font-size:11px;" +
                                "-fx-font-weight:bold;");
            } else {
                badge.setStyle(
                        "-fx-background-color:#e8f5e9;" +
                                "-fx-text-fill:#388e3c;" +
                                "-fx-border-color:#a5d6a7;" +
                                "-fx-border-width:1; -fx-border-radius:4;" +
                                "-fx-background-radius:4; -fx-padding:2 8; -fx-font-size:11px;");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BUILD ROWS
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildReservationRow(CartItem item) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));

        boolean warn = cart.isWarning(item);
        row.setStyle(
                "-fx-background-color:" + (warn ? WARN_BG : WHITE) + ";" +
                        "-fx-background-radius:12;-fx-border-radius:12;" +
                        "-fx-border-color:" + (warn ? WARN_BORDER : "#c8e6c9") + ";-fx-border-width:1;"
        );
        row.setEffect(new DropShadow(6, Color.web("#00000015")));

        String iconText = switch (item.getType()) {
            case HEBERGEMENT -> "[Hotel]";
            case ACTIVITY    -> "[Activite]";
            case TRANSPORT   -> "[Transport]";
        };
        Label iconLabel = new Label(iconText);
        iconLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + GREEN_DARK + ";");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nom = new Label(item.getLabel());
        nom.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_DARK + ";");

        String dateInfo = item.getDateFrom() != null
                ? item.getDateFrom() + " -> " + item.getDateTo()
                + (item.getNights() > 0 ? " (" + item.getNights() + " nuits)" : "")
                : "";
        Label dates  = new Label(dateInfo);
        dates.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREY + ";");
        Label guests = new Label(item.getNumberOfPersons() + " personne(s)");
        guests.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREY + ";");

        Label timerBadge = buildTimerBadge(item, warn);
        info.getChildren().addAll(nom, dates, guests, timerBadge);

        Label total = new Label(String.format("%.2f TND", item.getTotalPrice()));
        total.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_MED + ";-fx-min-width:100px;");
        total.setAlignment(Pos.CENTER_RIGHT);

        Button btnDel = buildDeleteButton();
        btnDel.setOnAction(e -> { cart.removeReservationItem(item); refreshCart(); });

        row.getChildren().addAll(iconLabel, info, total, btnDel);
        return row;
    }

    private HBox buildItemRow(Product p, int qty) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));

        boolean warn = cart.isWarning(p);
        row.setStyle(
                "-fx-background-color:" + (warn ? WARN_BG : WHITE) + ";" +
                        "-fx-background-radius:12;-fx-border-radius:12;" +
                        "-fx-border-color:" + (warn ? WARN_BORDER : BORDER) + ";-fx-border-width:1;"
        );
        row.setEffect(new DropShadow(6, Color.web("#00000015")));

        Label icon = new Label("[Produit]");
        icon.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_DARK + ";");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nom = new Label(p.getNom());
        nom.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_DARK + ";");
        Label prixUnit = new Label(String.format("%.2f TND / unite", p.getPrix()));
        prixUnit.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREY + ";");

        Label timerBadge = buildTimerBadge(p, warn);
        info.getChildren().addAll(nom, prixUnit, timerBadge);

        String qtyBtnStyle =
                "-fx-background-color:#f0f0f0;-fx-font-size:16px;-fx-font-weight:bold;" +
                        "-fx-min-width:32px;-fx-min-height:32px;-fx-background-radius:6;-fx-cursor:hand;";

        Button btnMinus = new Button("-");
        btnMinus.setStyle(qtyBtnStyle);
        Label qtyLabel = new Label(String.valueOf(qty));
        qtyLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-min-width:30px;");
        qtyLabel.setAlignment(Pos.CENTER);
        Button btnPlus = new Button("+");
        btnPlus.setStyle(qtyBtnStyle);

        btnMinus.setOnAction(e -> {
            cart.updateQuantity(p, cart.getProductItems().getOrDefault(p, 1) - 1);
            refreshCart();
        });
        btnPlus.setOnAction(e -> {
            cart.updateQuantity(p, cart.getProductItems().getOrDefault(p, 1) + 1);
            refreshCart();
        });

        HBox qtyBox = new HBox(8, btnMinus, qtyLabel, btnPlus);
        qtyBox.setAlignment(Pos.CENTER);

        Label sousTotal = new Label(String.format("%.2f TND", p.getPrix() * qty));
        sousTotal.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_MED
                + ";-fx-min-width:100px;");
        sousTotal.setAlignment(Pos.CENTER_RIGHT);

        Button btnDel = buildDeleteButton();
        btnDel.setOnAction(e -> { cart.removeProduct(p); refreshCart(); });

        row.getChildren().addAll(icon, info, qtyBox, sousTotal, btnDel);
        return row;
    }

    private Label buildTimerBadge(Product p, boolean warn) {
        long remaining = cart.getRemainingMillis(p);
        Label badge = new Label("Timer : " + CartManager.formatRemaining(remaining));
        badge.getStyleClass().add("timer-badge");
        badge.setUserData(p);
        badge.setStyle(warn
                ? "-fx-background-color:" + WARN_BG + ";-fx-text-fill:" + WARN_TEXT + ";"
                + "-fx-border-color:" + WARN_BORDER + ";-fx-border-width:1;-fx-border-radius:4;"
                + "-fx-background-radius:4;-fx-padding:2 8;-fx-font-size:11px;-fx-font-weight:bold;"
                : "-fx-background-color:#e8f5e9;-fx-text-fill:#388e3c;"
                + "-fx-border-color:#a5d6a7;-fx-border-width:1;-fx-border-radius:4;"
                + "-fx-background-radius:4;-fx-padding:2 8;-fx-font-size:11px;");
        return badge;
    }

    private Label buildTimerBadge(CartItem item, boolean warn) {
        long remaining = cart.getRemainingMillis(item);
        Label badge = new Label("Timer : " + CartManager.formatRemaining(remaining));
        badge.getStyleClass().add("timer-badge");
        badge.setUserData(item);
        badge.setStyle(warn
                ? "-fx-background-color:" + WARN_BG + ";-fx-text-fill:" + WARN_TEXT + ";"
                + "-fx-border-color:" + WARN_BORDER + ";-fx-border-width:1;-fx-border-radius:4;"
                + "-fx-background-radius:4;-fx-padding:2 8;-fx-font-size:11px;-fx-font-weight:bold;"
                : "-fx-background-color:#e8f5e9;-fx-text-fill:#388e3c;"
                + "-fx-border-color:#a5d6a7;-fx-border-width:1;-fx-border-radius:4;"
                + "-fx-background-radius:4;-fx-padding:2 8;-fx-font-size:11px;");
        return badge;
    }

    private Button buildDeleteButton() {
        Button btn = new Button("Supprimer");
        btn.setStyle(
                "-fx-background-color:#ffebee;-fx-text-fill:#c62828;" +
                        "-fx-font-size:12px;-fx-background-radius:6;-fx-cursor:hand;"
        );
        return btn;
    }

    private void updateSummary() {
        labelTotal.setText(String.format("%.2f TND", cart.getTotal()));
        labelCount.setText(String.valueOf(cart.getCount()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BANNER WARNING / EXPIRY
    // ══════════════════════════════════════════════════════════════════════════

    private void showWarningBanner(String message) {
        if (warningBanner == null) {
            warningBanner = new Label();
            warningBanner.setMaxWidth(Double.MAX_VALUE);
            warningBanner.setWrapText(true);
        }
        warningBanner.setText(message);
        warningBanner.setStyle(
                "-fx-background-color:" + WARN_BG + ";" +
                        "-fx-text-fill:" + WARN_TEXT + ";" +
                        "-fx-border-color:" + WARN_BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-padding:10 16;-fx-font-size:13px;-fx-font-weight:bold;");

        if (!itemsContainer.getChildren().contains(warningBanner)) {
            itemsContainer.getChildren().add(0, warningBanner);
        }
    }

    private void hideWarningBanner() {
        if (warningBanner != null) {
            itemsContainer.getChildren().remove(warningBanner);
        }
        warningBanner = null;
    }

    private void showExpiryNotification(String label) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Article expire");
        alert.setHeaderText(null);
        alert.setContentText(
                "\"" + label + "\" a ete retire de votre panier car la duree de reservation de 5 minutes est depassee.");
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CONFIRMER COMMANDE  ✅ MODIFIÉ : décrémentation du stock
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void onConfirmer() {
        if (cart.getProductItems().isEmpty() && cart.getReservationItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Panier vide", "Ajoutez des articles avant de confirmer.");
            return;
        }

        // ✅ Vérifier que le stock est suffisant avant de procéder
        for (Map.Entry<Product, Integer> entry : cart.getProductItems().entrySet()) {
            Product p   = entry.getKey();
            int     qty = entry.getValue();
            if (p.getStock() < qty) {
                showAlert(Alert.AlertType.WARNING,
                        "Stock insuffisant",
                        "Stock insuffisant pour \"" + p.getNom() + "\". "
                                + "Disponible : " + p.getStock() + ", demandé : " + qty);
                return;
            }
        }

        boolean isPaypal = rbPaypal.isSelected();

        try {
            if (!cart.getReservationItems().isEmpty()) {
                reservationService.finalizeAllReservations(cart.getReservationItems());
            }

            // ✅ Créer commandes + décrémenter le stock pour chaque produit
            for (Map.Entry<Product, Integer> entry : cart.getProductItems().entrySet()) {
                Product p   = entry.getKey();
                int     qty = entry.getValue();
                double  st  = p.getPrix() * qty;

                // Créer la commande
                Commande commande = new Commande(1, p.getId(), qty, p.getPrix(), st, new Date());
                commandeService.create(commande);
                int cid = getLastInsertedCommandeId(1, p.getId());
                ligneCommandeService.create(new LigneCommande(cid, p.getId(), qty, p.getPrix(), st));

                // ✅ Décrémenter le stock en base de données
                int nouveauStock = p.getStock() - qty;
                p.setStock(nouveauStock);
                productService.update(p);
            }

            double total = cart.getTotal();
            String numeroCommande = "CMD-" + System.currentTimeMillis();

            if (isPaypal) {
                try {
                    BigDecimal montantEUR = MollieConfig.convertStoreAmountToMollie(BigDecimal.valueOf(total));
                    MolliePaymentService mollieService = new MolliePaymentService();
                    MolliePayment molliePayment = mollieService.createPayment(
                            numeroCommande,
                            "Commande EcoTrip " + numeroCommande,
                            montantEUR, "");

                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Mode test Mollie");
                    info.setHeaderText("Comment simuler le paiement ?");
                    info.setContentText(
                            "Dans la fenetre qui va s'ouvrir :\n\n" +
                                    "1. Cliquez sur 'TEST CARDS' (bouton bleu a droite)\n" +
                                    "2. Choisissez une carte de test\n" +
                                    "3. Le paiement sera simule automatiquement");
                    info.showAndWait();

                    MollieCheckoutWindow.CheckoutResult result =
                            MollieCheckoutWindow.show(molliePayment, mollieService);

                    if (result.successful()) {
                        stopTicker();
                        cart.clear();
                        showAlert(Alert.AlertType.INFORMATION, "Paiement confirme",
                                "Paiement confirme via Mollie !\nCommande : " + numeroCommande
                                        + "\nTotal : " + String.format("%.2f TND", total));
                        refreshCart();
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Paiement non complete",
                                "Statut : " + result.status());
                    }
                    return;
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur Mollie", ex.getMessage());
                    return;
                }
            }

            String mode = rbCarte.isSelected() ? "Par carte bancaire" : "Cash";
            stopTicker();
            cart.clear();
            showAlert(Alert.AlertType.INFORMATION, "Commande confirmee",
                    "Votre commande a ete enregistree !\nMode : " + mode
                            + "\nTotal : " + String.format("%.2f TND", total));
            refreshCart();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private int getLastInsertedCommandeId(int userId, int produitId) throws SQLException {
        return commandeService.read().stream()
                .filter(c -> c.getIdUser() == userId && c.getProduitId() == produitId)
                .mapToInt(Commande::getId)
                .max()
                .orElse(-1);
    }

    private void stopTicker() {
        if (uiTicker != null) uiTicker.stop();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VIDER PANIER
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void onVider() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vider le panier");
        confirm.setHeaderText("Etes-vous sur de vouloir vider le panier ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                stopTicker();
                cart.clear();
                refreshCart();
                uiTicker.play();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORT PDF
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void onExporterPDF() {
        if (cart.getProductItems().isEmpty() && cart.getReservationItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Panier vide", "Le panier est vide, rien a exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer la facture PDF");
        fileChooser.setInitialFileName(
                "facture_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(btnExporterPDF.getScene().getWindow());
        if (file == null) return;

        try (FileOutputStream fos = new FileOutputStream(file)) {

            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, fos);
            doc.open();

            Font fTitle      = new Font(Font.HELVETICA, 22, Font.BOLD,   PDF_GREEN_DARK);
            Font fSubTitle   = new Font(Font.HELVETICA, 10, Font.NORMAL, PDF_GREY);
            Font fSection    = new Font(Font.HELVETICA, 13, Font.BOLD,   PDF_GREEN_DARK);
            Font fTHeader    = new Font(Font.HELVETICA, 10, Font.BOLD,   PDF_WHITE);
            Font fCell       = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(50, 50, 50));
            Font fSubtotal   = new Font(Font.HELVETICA, 11, Font.BOLD,   PDF_GREEN_DARK);
            Font fGrandTotal = new Font(Font.HELVETICA, 16, Font.BOLD,   PDF_GREEN_DARK);
            Font fMode       = new Font(Font.HELVETICA, 10, Font.ITALIC, PDF_GREY);

            Paragraph titre = new Paragraph("FACTURE / BON DE COMMANDE", fTitle);
            titre.setAlignment(Element.ALIGN_CENTER);
            doc.add(titre);

            String dateStr = new SimpleDateFormat("dd/MM/yyyy  HH:mm").format(new Date());
            Paragraph datePara = new Paragraph("Date : " + dateStr, fSubTitle);
            datePara.setAlignment(Element.ALIGN_CENTER);
            datePara.setSpacingBefore(4);
            doc.add(datePara);

            doc.add(buildSeparatorTable(PDF_GREEN_DARK));
            doc.add(new Paragraph(" "));

            if (!cart.getReservationItems().isEmpty()) {
                Paragraph secRes = new Paragraph("Reservations", fSection);
                secRes.setSpacingBefore(6);
                secRes.setSpacingAfter(6);
                doc.add(secRes);

                PdfPTable tRes = new PdfPTable(new float[]{3f, 2f, 2f, 1.5f, 1.8f});
                tRes.setWidthPercentage(100);
                tRes.setSpacingAfter(8);
                addTableHeader(tRes, fTHeader, "Designation", "Date debut", "Date fin", "Personnes", "Total (TND)");

                double sousTotal = 0;
                boolean alt = false;
                for (CartItem item : cart.getReservationItems()) {
                    java.awt.Color bg = alt ? new java.awt.Color(245, 251, 243) : PDF_WHITE;
                    addRow(tRes, fCell, bg,
                            item.getLabel(),
                            item.getDateFrom() != null ? item.getDateFrom().toString() : "-",
                            item.getDateTo()   != null ? item.getDateTo().toString()   : "-",
                            String.valueOf(item.getNumberOfPersons()),
                            String.format("%.2f", item.getTotalPrice()));
                    sousTotal += item.getTotalPrice();
                    alt = !alt;
                }
                addSubTotalRow(tRes, fSubtotal, 4, "Sous-total : " + String.format("%.2f TND", sousTotal));
                doc.add(tRes);
            }

            if (!cart.getProductItems().isEmpty()) {
                Paragraph secProd = new Paragraph("Produits", fSection);
                secProd.setSpacingBefore(12);
                secProd.setSpacingAfter(6);
                doc.add(secProd);

                PdfPTable tProd = new PdfPTable(new float[]{3.5f, 2f, 1.2f, 2f});
                tProd.setWidthPercentage(100);
                tProd.setSpacingAfter(8);
                addTableHeader(tProd, fTHeader, "Produit", "Prix unit. (TND)", "Qte", "Sous-total (TND)");

                double sousTotal = 0;
                boolean alt = false;
                for (Map.Entry<Product, Integer> entry : cart.getProductItems().entrySet()) {
                    Product p   = entry.getKey();
                    int     qty = entry.getValue();
                    double  st  = p.getPrix() * qty;
                    java.awt.Color bg = alt ? new java.awt.Color(245, 251, 243) : PDF_WHITE;
                    addRow(tProd, fCell, bg,
                            p.getNom(),
                            String.format("%.2f", p.getPrix()),
                            String.valueOf(qty),
                            String.format("%.2f", st));
                    sousTotal += st;
                    alt = !alt;
                }
                addSubTotalRow(tProd, fSubtotal, 3, "Sous-total : " + String.format("%.2f TND", sousTotal));
                doc.add(tProd);
            }

            doc.add(buildSeparatorTable(PDF_GREEN_DARK));
            doc.add(new Paragraph(" "));

            Paragraph grandTotal = new Paragraph(
                    "TOTAL GENERAL : " + String.format("%.2f TND", cart.getTotal()), fGrandTotal);
            grandTotal.setAlignment(Element.ALIGN_RIGHT);
            doc.add(grandTotal);

            String mode = rbCarte.isSelected() ? "Par carte bancaire"
                    : rbPaypal.isSelected() ? "PayPal" : "Cash";
            Paragraph modePara = new Paragraph("Mode de paiement : " + mode, fMode);
            modePara.setAlignment(Element.ALIGN_RIGHT);
            modePara.setSpacingBefore(4);
            doc.add(modePara);

            doc.add(new Paragraph(" "));
            doc.add(buildSeparatorTable(new java.awt.Color(200, 200, 200)));
            Paragraph footer = new Paragraph("Merci pour votre confiance - ESPRIT Eco-Tourism", fSubTitle);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(6);
            doc.add(footer);

            doc.close();

            showAlert(Alert.AlertType.INFORMATION, "Export reussi",
                    "Facture enregistree :\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur export PDF", ex.getMessage());
        }
    }

    // ── Helpers PDF ────────────────────────────────────────────────────────────

    private PdfPTable buildSeparatorTable(java.awt.Color color) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);
        t.setSpacingAfter(4);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(color);
        cell.setFixedHeight(3f);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        return t;
    }

    private void addTableHeader(PdfPTable table, Font font, String... cols) {
        for (String col : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(col, font));
            cell.setBackgroundColor(PDF_GREEN_DARK);
            cell.setPadding(8);
            cell.setBorderColor(PDF_BORDER);
            table.addCell(cell);
        }
    }

    private void addRow(PdfPTable table, Font font, java.awt.Color bg, String... values) {
        for (String val : values) {
            PdfPCell cell = new PdfPCell(new Phrase(val, font));
            cell.setBackgroundColor(bg);
            cell.setPadding(7);
            cell.setBorderColor(PDF_BORDER);
            table.addCell(cell);
        }
    }

    private void addSubTotalRow(PdfPTable table, Font font, int emptyColspan, String label) {
        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setColspan(emptyColspan);
        empty.setBorder(Rectangle.NO_BORDER);
        table.addCell(empty);
        PdfPCell stCell = new PdfPCell(new Phrase(label, font));
        stCell.setBackgroundColor(PDF_GREEN_LIGHT);
        stCell.setPadding(7);
        stCell.setBorderColor(PDF_BORDER);
        table.addCell(stCell);
    }

    // ── Alert ──────────────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}