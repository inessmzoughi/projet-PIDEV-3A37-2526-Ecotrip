package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.esprit.models.produit.Commande;
import tn.esprit.models.produit.LigneCommande;
import tn.esprit.models.produit.Product;
import tn.esprit.services.produit.CommandeService;
import tn.esprit.services.produit.LigneCommandeService;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.CartManager;
import tn.esprit.models.cart.CartItem;

import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    @FXML
    private VBox itemsContainer;
    @FXML private VBox        emptyState;
    @FXML private VBox        summaryBox;
    @FXML private HBox contentBox;
    @FXML private Label labelTotal;
    @FXML private Label       labelCount;
    @FXML private RadioButton rbCarte;
    @FXML private RadioButton rbPaypal;
    @FXML private RadioButton rbCash;
    @FXML private Button      btnPayer;

    private final CommandeService commandeService      = new CommandeService();
    private final LigneCommandeService ligneCommandeService = new LigneCommandeService();
    private final CartManager cart                 = CartManager.getInstance();

    // Groupe radio pour le mode de paiement
    private final ToggleGroup paymentGroup = new ToggleGroup();

    private static final String GREEN_DARK = "#2d5a1b";
    private static final String GREEN_MED  = "#4a7c3f";
    private static final String WHITE      = "#ffffff";
    private static final String BORDER     = "#e0e0e0";
    private static final String GREY       = "#666666";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Lier les RadioButtons au même groupe
        rbCarte.setToggleGroup(paymentGroup);
        rbPaypal.setToggleGroup(paymentGroup);
        rbCash.setToggleGroup(paymentGroup);
        rbCarte.setSelected(true);

        refreshCart();
    }
    // Replace refreshCart() with this updated version:
    private void refreshCart() {
        itemsContainer.getChildren().clear();

        boolean hasProducts     = !cart.getProductItems().isEmpty();
        boolean hasReservations = !cart.getReservationItems().isEmpty();
        boolean empty           = !hasProducts && !hasReservations;

        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        contentBox.setVisible(!empty);
        contentBox.setManaged(!empty);

        if (empty) return;

        // ── Reservation items ────────────────────────────
        if (hasReservations) {
            Label sectionLabel = new Label("🏨  Réservations");
            sectionLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#2d5a1b;");
            itemsContainer.getChildren().add(sectionLabel);

            for (CartItem item : cart.getReservationItems()) {
                itemsContainer.getChildren().add(buildReservationRow(item));
            }
        }

        // ── Product items (existing) ─────────────────────
        if (hasProducts) {
            Label sectionLabel = new Label("🛍  Produits");
            sectionLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#2d5a1b;"
                    + (hasReservations ? "-fx-padding:16 0 0 0;" : ""));
            itemsContainer.getChildren().add(sectionLabel);

            for (Map.Entry<Product, Integer> entry : cart.getProductItems().entrySet()) {
                itemsContainer.getChildren().add(buildItemRow(entry.getKey(), entry.getValue()));
            }
        }

        updateSummary();
    }

    // Add this new method for reservation rows:
    private HBox buildReservationRow(CartItem item) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: #c8e6c9;" +  // green tint border for reservations
                        "-fx-border-width: 1;"
        );
        row.setEffect(new DropShadow(6, Color.web("#00000015")));

        // Icon based on type
        String icon = switch (item.getType()) {
            case HEBERGEMENT -> "🏨";
            case ACTIVITY    -> "🧭";
            case TRANSPORT   -> "🚌";
        };
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nom = new Label(item.getLabel());
        nom.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");

        String dateInfo = item.getDateFrom() != null
                ? item.getDateFrom() + " → " + item.getDateTo()
                + (item.getNights() > 0 ? " (" + item.getNights() + " nuits)" : "")
                : "";
        Label dates = new Label(dateInfo);
        dates.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GREY + ";");

        Label guests = new Label("👥 " + item.getNumberOfPersons() + " personne(s)");
        guests.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GREY + ";");

        info.getChildren().addAll(nom, dates, guests);

        // Total
        Label total = new Label(String.format("%.2f TND", item.getTotalPrice()));
        total.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_MED + "; -fx-min-width: 100px;");
        total.setAlignment(Pos.CENTER_RIGHT);

        // Delete
        Button btnDel = new Button("🗑");
        btnDel.setStyle(
                "-fx-background-color: #ffebee;" +
                        "-fx-text-fill: #c62828;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;"
        );
        btnDel.setOnAction(e -> {
            cart.removeReservationItem(item);
            refreshCart();
        });

        row.getChildren().addAll(iconLabel, info, total, btnDel);
        return row;
    }

    // Update updateSummary() to show grand total:
    private void updateSummary() {
        labelTotal.setText(String.format("%.2f TND", cart.getTotal()));
        labelCount.setText(String.valueOf(cart.getCount()));
    }

    private HBox buildItemRow(Product p, int qty) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;"
        );
        row.setEffect(new DropShadow(6, Color.web("#00000015")));

        // Icône
        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size: 28px;");

        // Infos produit
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nom = new Label(p.getNom());
        nom.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + GREEN_DARK + ";"
        );
        Label prixUnit = new Label(String.format("%.2f TND / unité", p.getPrix()));
        prixUnit.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GREY + ";");
        info.getChildren().addAll(nom, prixUnit);

        // Contrôle quantité
        HBox qtyBox = new HBox(8);
        qtyBox.setAlignment(Pos.CENTER);

        String qtyBtnStyle =
                "-fx-background-color: #f0f0f0;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 32px;" +
                        "-fx-min-height: 32px;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;";

        Button btnMinus = new Button("−");
        btnMinus.setStyle(qtyBtnStyle);

        Label qtyLabel = new Label(String.valueOf(qty));
        qtyLabel.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 30px;"
        );
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
        qtyBox.getChildren().addAll(btnMinus, qtyLabel, btnPlus);

        // Sous-total
        Label sousTotal = new Label(String.format("%.2f TND", p.getPrix() * qty));
        sousTotal.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + GREEN_MED + ";" +
                        "-fx-min-width: 100px;"
        );
        sousTotal.setAlignment(Pos.CENTER_RIGHT);

        // Bouton supprimer
        Button btnDel = new Button("🗑");
        btnDel.setStyle(
                "-fx-background-color: #ffebee;" +
                        "-fx-text-fill: #c62828;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;"
        );
        btnDel.setOnAction(e -> {
            cart.removeProduct(p);
            refreshCart();
        });

        row.getChildren().addAll(icon, info, qtyBox, sousTotal, btnDel);
        return row;
    }


    @FXML
    private void onConfirmer() {
        if (cart.getProductItems().isEmpty() && cart.getReservationItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "Panier vide", "Ajoutez des produits avant de confirmer.");
            return;
        }

        // Mode de paiement sélectionné
        String mode = rbCarte.isSelected() ? "CARTE"
                : rbPaypal.isSelected() ? "PAYPAL"
                : "CASH";

        try {
            // ── Pour chaque produit du panier → une Commande + une LigneCommande ──
            for (Map.Entry<Product, Integer> entry : cart.getProductItems().entrySet()) {
                Product p   = entry.getKey();
                int     qty = entry.getValue();
                double  sousTotal = p.getPrix() * qty;

                // 1. Créer la Commande
                Commande commande = new Commande(
                        SessionManager.getInstance().getCurrentUser().getId(),  // TODO : remplacer par l'ID utilisateur connecté
                        p.getId(),
                        qty,
                        p.getPrix(),
                        sousTotal,
                        new Date()
                );
                commandeService.create(commande);

                // 2. Récupérer l'ID généré de la commande
                // (on lit la dernière insérée pour cet user+produit)
                int commandeId = getLastInsertedCommandeId(1, p.getId());

                // 3. Créer la LigneCommande
                LigneCommande ligne = new LigneCommande(
                        commandeId,
                        p.getId(),
                        qty,
                        p.getPrix(),
                        sousTotal
                );
                ligneCommandeService.create(ligne);
            }

            // 4. Vider le panier
            double total = cart.getTotal();
            cart.clear();

            showAlert(Alert.AlertType.INFORMATION,
                    "Commande confirmée ✅",
                    "Votre commande a été enregistrée avec succès !\n" +
                            "Mode de paiement : " + mode + "\n" +
                            "Total : " + String.format("%.2f TND", total));

            refreshCart();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "Erreur", "Une erreur s'est produite : " + e.getMessage());
        }
    }

    // Récupère l'ID de la dernière commande insérée pour un user+produit
    private int getLastInsertedCommandeId(int userId, int produitId) throws SQLException {
        return commandeService.read().stream()
                .filter(c -> c.getIdUser() == userId && c.getProduitId() == produitId)
                .mapToInt(Commande::getId)
                .max()
                .orElse(-1);
    }

    @FXML
    private void onVider() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vider le panier");
        confirm.setHeaderText("Êtes-vous sûr de vouloir vider le panier ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                cart.clear();
                refreshCart();
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}