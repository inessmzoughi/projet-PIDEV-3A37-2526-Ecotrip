package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.esprit.models.produit.Product;
import tn.esprit.services.produit.ProductService;
import tn.esprit.utils.CartManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ProductsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortSelect;
    @FXML private ComboBox<String> filterCategory;
    @FXML private Label resultCount;
    @FXML private FlowPane cardsPane;
    @FXML private VBox emptyState;
    @FXML private HBox paginationBar;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label pagInfo;
    @FXML private Button btnCart;

    private final ProductService productService = new ProductService();
    private List<Product> allProducts = new ArrayList<>();

    private static final int PAGE_SIZE = 9;
    private int currentPage = 1;
    private List<Product> filteredProducts = new ArrayList<>();

    private static final String GREEN_DARK  = "#2d5a1b";
    private static final String GREEN_MED   = "#4a7c3f";
    private static final String WHITE       = "#ffffff";
    private static final String GREY_TEXT   = "#666666";
    private static final String BORDER      = "#e0e0e0";

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

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        sortSelect.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterCategory.valueProperty().addListener((obs, o, n) -> applyFilters());

        applyFilters();
    }

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

    private void renderPage() {
        cardsPane.getChildren().clear();

        boolean isEmpty = filteredProducts.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        resultCount.setText(filteredProducts.size() + " Product(s) Found");

        if (isEmpty) {
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            return;
        }

        int totalPages = (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE);
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredProducts.size());

        for (Product p : filteredProducts.subList(from, to))
            cardsPane.getChildren().add(buildCard(p));

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
        btnPrev.setDisable(currentPage == 1);
        btnNext.setDisable(currentPage == totalPages);
    }

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

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: " + GREEN_MED + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
        ));

        // Header
        HBox header = new HBox();
        header.setPrefHeight(80);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 16, 0, 16));
        header.setStyle(
                "-fx-background-color: " + GREEN_DARK + ";" +
                        "-fx-background-radius: 14 14 0 0;"
        );
        Label prodIcon = new Label("🛍");
        prodIcon.setStyle("-fx-font-size: 36px;");
        header.getChildren().add(prodIcon);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(16));

        Label nom = new Label(p.getNom());
        nom.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + GREEN_DARK + ";"
        );
        nom.setWrapText(true);

        Separator sep = new Separator();

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label pIcon = new Label("💰");
        pIcon.setStyle("-fx-font-size: 14px;");
        Label prix = new Label(String.format("%.2f TND", p.getPrix()));
        prix.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + GREEN_MED + ";"
        );
        priceRow.getChildren().addAll(pIcon, prix);

        HBox stockRow = new HBox(8);
        stockRow.setAlignment(Pos.CENTER_LEFT);
        Label sIcon = new Label("📦");
        sIcon.setStyle("-fx-font-size: 13px;");
        Label stock = new Label("En stock : " + p.getStock());
        stock.setStyle("-fx-font-size: 13px; -fx-text-fill: " + GREY_TEXT + ";");
        stockRow.getChildren().addAll(sIcon, stock);

        if (p.getStock() <= 10) {
            Label low = new Label("⚠ Stock faible");
            low.setStyle(
                    "-fx-background-color: #fff3cd;" +
                            "-fx-text-fill: #856404;" +
                            "-fx-font-size: 11px;" +
                            "-fx-padding: 2 8 2 8;" +
                            "-fx-background-radius: 10;"
            );
            stockRow.getChildren().add(low);
        }

        // ✅ Bouton Ajouter au panier
        Button btn = new Button("🛒  Ajouter au panier");
        btn.setMaxWidth(Double.MAX_VALUE);
        String btnStyle =
                "-fx-background-color: " + GREEN_DARK + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 0 10 0;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;";
        String btnHover =
                "-fx-background-color: " + GREEN_MED + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 0 10 0;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;";
        btn.setStyle(btnStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(btnHover));
        btn.setOnMouseExited(e  -> btn.setStyle(btnStyle));
        btn.setOnAction(e -> addToCart(p, btn, btnStyle));

        body.getChildren().addAll(nom, sep, priceRow, stockRow, btn);
        card.getChildren().addAll(header, body);
        return card;
    }

    // ✅ Ajouter au panier avec feedback visuel
    private void addToCart(Product p, Button btn, String originalStyle) {
        CartManager.getInstance().addProduct(p);

        // ✅ Mettre à jour le compteur du bouton panier
        btnCart.setText("🛒  Mon Panier (" + CartManager.getInstance().getCount() + ")");

        // Feedback visuel
        btn.setText("✓  Ajouté !");
        btn.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 0 10 0;" +
                        "-fx-background-radius: 8;"
        );
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                btn.setText("🛒  Ajouter au panier");
                btn.setStyle(originalStyle);
            });
        }).start();
    }

    @FXML
    private void onOpenCart() {
        /*try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/views/front/produit/Cart.fxml")
            );
            javafx.scene.Parent root = loader.load();
            btnCart.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    @FXML
    private void onReset() {
        searchField.clear();
        sortSelect.setValue("Sort by");
        filterCategory.setValue("All Categories");
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