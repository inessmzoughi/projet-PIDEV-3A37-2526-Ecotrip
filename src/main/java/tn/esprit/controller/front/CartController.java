package tn.esprit.controller.front;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import tn.esprit.models.produit.Commande;
import tn.esprit.models.produit.LigneCommande;
import tn.esprit.models.produit.Product;
import tn.esprit.services.produit.CommandeService;
import tn.esprit.services.produit.LigneCommandeService;
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
    @FXML private Button      btnExporterPDF;   // ✅ ajouté

    // ── Services ───────────────────────────────────────────────────────────────
    private final CommandeService      commandeService      = new CommandeService();
    private final LigneCommandeService ligneCommandeService = new LigneCommandeService();
    private final CartManager          cart                 = CartManager.getInstance();
    private final ReservationService   reservationService   = new ReservationService();

    // ── Radio groupe paiement ──────────────────────────────────────────────────
    private final ToggleGroup paymentGroup = new ToggleGroup();

    // ── Constantes couleurs UI ─────────────────────────────────────────────────
    private static final String GREEN_DARK = "#2d5a1b";
    private static final String GREEN_MED  = "#4a7c3f";
    private static final String WHITE      = "#ffffff";
    private static final String BORDER     = "#e0e0e0";
    private static final String GREY       = "#666666";

    // ── Constantes couleurs PDF (java.awt.Color) ───────────────────────────────
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
        rbCarte.setToggleGroup(paymentGroup);
        rbPaypal.setToggleGroup(paymentGroup);
        rbCash.setToggleGroup(paymentGroup);
        rbCarte.setSelected(true);
        refreshCart();
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

        if (empty) return;

        // ── Réservations ──────────────────────────────────────────────────────
        if (hasReservations) {
            Label sectionLabel = new Label("🏨  Réservations");
            sectionLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + GREEN_DARK + ";");
            itemsContainer.getChildren().add(sectionLabel);
            for (CartItem item : cart.getReservationItems()) {
                itemsContainer.getChildren().add(buildReservationRow(item));
            }
        }

        // ── Produits ──────────────────────────────────────────────────────────
        if (hasProducts) {
            Label sectionLabel = new Label("🛍  Produits");
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
    //  BUILD ROWS
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildReservationRow(CartItem item) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(
                "-fx-background-color:" + WHITE + ";" +
                        "-fx-background-radius:12;-fx-border-radius:12;" +
                        "-fx-border-color:#c8e6c9;-fx-border-width:1;"
        );
        row.setEffect(new DropShadow(6, Color.web("#00000015")));

        String icon = switch (item.getType()) {
            case HEBERGEMENT -> "🏨";
            case ACTIVITY    -> "🧭";
            case TRANSPORT   -> "🚌";
        };
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size:28px;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nom = new Label(item.getLabel());
        nom.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_DARK + ";");

        String dateInfo = item.getDateFrom() != null
                ? item.getDateFrom() + " → " + item.getDateTo()
                + (item.getNights() > 0 ? " (" + item.getNights() + " nuits)" : "")
                : "";
        Label dates  = new Label(dateInfo);
        dates.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREY + ";");
        Label guests = new Label("👥 " + item.getNumberOfPersons() + " personne(s)");
        guests.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREY + ";");
        info.getChildren().addAll(nom, dates, guests);

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
        row.setStyle(
                "-fx-background-color:" + WHITE + ";" +
                        "-fx-background-radius:12;-fx-border-radius:12;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;"
        );
        row.setEffect(new DropShadow(6, Color.web("#00000015")));

        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size:28px;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nom = new Label(p.getNom());
        nom.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_DARK + ";");
        Label prixUnit = new Label(String.format("%.2f TND / unité", p.getPrix()));
        prixUnit.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREY + ";");
        info.getChildren().addAll(nom, prixUnit);

        String qtyBtnStyle =
                "-fx-background-color:#f0f0f0;-fx-font-size:16px;-fx-font-weight:bold;" +
                        "-fx-min-width:32px;-fx-min-height:32px;-fx-background-radius:6;-fx-cursor:hand;";

        Button btnMinus = new Button("−");
        btnMinus.setStyle(qtyBtnStyle);
        Label qtyLabel = new Label(String.valueOf(qty));
        qtyLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-min-width:30px;");
        qtyLabel.setAlignment(Pos.CENTER);
        Button btnPlus = new Button("+");
        btnPlus.setStyle(qtyBtnStyle);

        btnMinus.setOnAction(e -> { cart.updateQuantity(p, cart.getProductItems().getOrDefault(p, 1) - 1); refreshCart(); });
        btnPlus.setOnAction(e  -> { cart.updateQuantity(p, cart.getProductItems().getOrDefault(p, 1) + 1); refreshCart(); });

        HBox qtyBox = new HBox(8, btnMinus, qtyLabel, btnPlus);
        qtyBox.setAlignment(Pos.CENTER);

        Label sousTotal = new Label(String.format("%.2f TND", p.getPrix() * qty));
        sousTotal.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_MED + ";-fx-min-width:100px;");
        sousTotal.setAlignment(Pos.CENTER_RIGHT);

        Button btnDel = buildDeleteButton();
        btnDel.setOnAction(e -> { cart.removeProduct(p); refreshCart(); });

        row.getChildren().addAll(icon, info, qtyBox, sousTotal, btnDel);
        return row;
    }

    private Button buildDeleteButton() {
        Button btn = new Button("🗑");
        btn.setStyle(
                "-fx-background-color:#ffebee;-fx-text-fill:#c62828;" +
                        "-fx-font-size:14px;-fx-background-radius:6;-fx-cursor:hand;"
        );
        return btn;
    }

    private void updateSummary() {
        labelTotal.setText(String.format("%.2f TND", cart.getTotal()));
        labelCount.setText(String.valueOf(cart.getCount()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CONFIRMER COMMANDE
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void onConfirmer() {
        if (cart.getProductItems().isEmpty() && cart.getReservationItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Panier vide", "Ajoutez des articles avant de confirmer.");
            return;
        }

        String mode = rbCarte.isSelected() ? "CARTE" : rbPaypal.isSelected() ? "PAYPAL" : "CASH";

        try {
            if (!cart.getReservationItems().isEmpty()) {
                reservationService.finalizeAllReservations(cart.getReservationItems());
            }

            for (Map.Entry<Product, Integer> entry : cart.getProductItems().entrySet()) {
                Product p   = entry.getKey();
                int     qty = entry.getValue();
                double  st  = p.getPrix() * qty;
                Commande commande = new Commande(1, p.getId(), qty, p.getPrix(), st, new Date());
                commandeService.create(commande);
                int cid = getLastInsertedCommandeId(1, p.getId());
                ligneCommandeService.create(new LigneCommande(cid, p.getId(), qty, p.getPrix(), st));
            }

            double total = cart.getTotal();
            cart.clear();

            showAlert(Alert.AlertType.INFORMATION, "Commande confirmée ✅",
                    "Votre commande a été enregistrée !\nMode : " + mode
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

    // ══════════════════════════════════════════════════════════════════════════
    //  VIDER PANIER
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORT PDF  ✅
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void onExporterPDF() {
        if (cart.getProductItems().isEmpty() && cart.getReservationItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Panier vide", "Le panier est vide, rien à exporter.");
            return;
        }

        // Boîte de dialogue pour choisir le fichier de destination
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

            // ── Polices ──────────────────────────────────────────────────────
            Font fTitle     = new Font(Font.HELVETICA, 22, Font.BOLD,   PDF_GREEN_DARK);
            Font fSubTitle  = new Font(Font.HELVETICA, 10, Font.NORMAL, PDF_GREY);
            Font fSection   = new Font(Font.HELVETICA, 13, Font.BOLD,   PDF_GREEN_DARK);
            Font fTHeader   = new Font(Font.HELVETICA, 10, Font.BOLD,   PDF_WHITE);
            Font fCell      = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(50, 50, 50));
            Font fSubtotal  = new Font(Font.HELVETICA, 11, Font.BOLD,   PDF_GREEN_DARK);
            Font fGrandTotal= new Font(Font.HELVETICA, 16, Font.BOLD,   PDF_GREEN_DARK);
            Font fMode      = new Font(Font.HELVETICA, 10, Font.ITALIC, PDF_GREY);

            // ── En-tête ───────────────────────────────────────────────────────
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

            // ── Section Réservations ──────────────────────────────────────────
            if (!cart.getReservationItems().isEmpty()) {
                Paragraph secRes = new Paragraph("Réservations", fSection);
                secRes.setSpacingBefore(6);
                secRes.setSpacingAfter(6);
                doc.add(secRes);

                PdfPTable tRes = new PdfPTable(new float[]{3f, 2f, 2f, 1.5f, 1.8f});
                tRes.setWidthPercentage(100);
                tRes.setSpacingAfter(8);

                addTableHeader(tRes, fTHeader,
                        "Désignation", "Date début", "Date fin", "Personnes", "Total (TND)");

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
                addSubTotalRow(tRes, fSubtotal, 4, String.format("Sous-total : %.2f TND", sousTotal));
                doc.add(tRes);
            }

            // ── Section Produits ──────────────────────────────────────────────
            if (!cart.getProductItems().isEmpty()) {
                Paragraph secProd = new Paragraph("Produits", fSection);
                secProd.setSpacingBefore(12);
                secProd.setSpacingAfter(6);
                doc.add(secProd);

                PdfPTable tProd = new PdfPTable(new float[]{3.5f, 2f, 1.2f, 2f});
                tProd.setWidthPercentage(100);
                tProd.setSpacingAfter(8);

                addTableHeader(tProd, fTHeader,
                        "Produit", "Prix unit. (TND)", "Qté", "Sous-total (TND)");

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
                addSubTotalRow(tProd, fSubtotal, 3, String.format("Sous-total : %.2f TND", sousTotal));
                doc.add(tProd);
            }

            // ── Grand total ───────────────────────────────────────────────────
            doc.add(buildSeparatorTable(PDF_GREEN_DARK));
            doc.add(new Paragraph(" "));

            Paragraph grandTotal = new Paragraph(
                    String.format("TOTAL GÉNÉRAL : %.2f TND", cart.getTotal()), fGrandTotal);
            grandTotal.setAlignment(Element.ALIGN_RIGHT);
            doc.add(grandTotal);

            // ── Mode de paiement ──────────────────────────────────────────────
            String mode = rbCarte.isSelected() ? "Carte bancaire"
                    : rbPaypal.isSelected() ? "PayPal" : "Cash";
            Paragraph modePara = new Paragraph("Mode de paiement : " + mode, fMode);
            modePara.setAlignment(Element.ALIGN_RIGHT);
            modePara.setSpacingBefore(4);
            doc.add(modePara);

            // ── Pied de page ──────────────────────────────────────────────────
            doc.add(new Paragraph(" "));
            doc.add(buildSeparatorTable(new java.awt.Color(200, 200, 200)));
            Paragraph footer = new Paragraph("Merci pour votre confiance — ESPRIT Eco-Tourism", fSubTitle);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(6);
            doc.add(footer);

            doc.close();

            showAlert(Alert.AlertType.INFORMATION, "Export réussi ✅",
                    "Facture enregistrée :\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur export PDF", ex.getMessage());
        }
    }

    // ── Helpers PDF ────────────────────────────────────────────────────────────

    /** Ligne de séparation colorée */
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

    /** Ajoute une ligne d'en-tête verte à un tableau */
    private void addTableHeader(PdfPTable table, Font font, String... cols) {
        for (String col : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(col, font));
            cell.setBackgroundColor(PDF_GREEN_DARK);
            cell.setPadding(8);
            cell.setBorderColor(PDF_BORDER);
            table.addCell(cell);
        }
    }

    /** Ajoute une ligne de données avec couleur alternée */
    private void addRow(PdfPTable table, Font font, java.awt.Color bg, String... values) {
        for (String val : values) {
            PdfPCell cell = new PdfPCell(new Phrase(val, font));
            cell.setBackgroundColor(bg);
            cell.setPadding(7);
            cell.setBorderColor(PDF_BORDER);
            table.addCell(cell);
        }
    }

    /** Ajoute une ligne de sous-total fusionnée + montant */
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

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITAIRE ALERT
    // ══════════════════════════════════════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}