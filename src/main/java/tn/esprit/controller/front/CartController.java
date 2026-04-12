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
import tn.esprit.utils.CartManager;

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

    private void refreshCart() {
        itemsContainer.getChildren().clear();
        Map<Product, Integer> items = cart.getItems();

        boolean empty = items.isEmpty();

        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        contentBox.setVisible(!empty);
        contentBox.setManaged(!empty);

        if (empty) return;

        for (Map.Entry<Product, Integer> entry : items.entrySet())
            itemsContainer.getChildren().add(buildItemRow(entry.getKey(), entry.getValue()));

        updateSummary();
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
            cart.updateQuantity(p, cart.getItems().getOrDefault(p, 1) - 1);
            refreshCart();
        });
        btnPlus.setOnAction(e -> {
            cart.updateQuantity(p, cart.getItems().getOrDefault(p, 1) + 1);
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

    private void updateSummary() {
        labelTotal.setText(String.format("%.2f TND", cart.getTotal()));
        labelCount.setText(String.valueOf(cart.getCount()));
    }

    @FXML
    private void onConfirmer() {
        if (cart.getItems().isEmpty()) {
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
            for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
                Product p   = entry.getKey();
                int     qty = entry.getValue();
                double  sousTotal = p.getPrix() * qty;

                // 1. Créer la Commande
                Commande commande = new Commande(
                        1,              // TODO : remplacer par l'ID utilisateur connecté
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