package tn.esprit.controller.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.models.produit.Product;
import tn.esprit.services.produit.ProductService;

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

    private final ProductService productService = new ProductService();
    private List<Product> allProducts = new ArrayList<>();

    private static final int PAGE_SIZE = 9;
    private int currentPage = 1;
    private List<Product> filteredProducts = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            allProducts = productService.read();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Remplir les catégories dans le ComboBox (à adapter si vous avez un CategoryService)
        filterCategory.getItems().add("All Categories");
        filterCategory.setValue("All Categories");

        sortSelect.setValue("Sort by");

        // Listeners pour filtrage en temps réel
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        sortSelect.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterCategory.valueProperty().addListener((obs, o, n) -> applyFilters());

        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String category = filterCategory.getValue();
        String sort = sortSelect.getValue();

        filteredProducts = allProducts.stream()
                .filter(p -> p.getNom().toLowerCase().contains(search))
                .collect(Collectors.toList());

        // Tri
        if ("Name (A-Z)".equals(sort)) {
            filteredProducts.sort(Comparator.comparing(Product::getNom));
        } else if ("Name (Z-A)".equals(sort)) {
            filteredProducts.sort(Comparator.comparing(Product::getNom).reversed());
        } else if ("Price (Low-High)".equals(sort)) {
            filteredProducts.sort(Comparator.comparingDouble(Product::getPrix));
        } else if ("Price (High-Low)".equals(sort)) {
            filteredProducts.sort(Comparator.comparingDouble(Product::getPrix).reversed());
        }

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
        int to = Math.min(from + PAGE_SIZE, filteredProducts.size());

        for (Product p : filteredProducts.subList(from, to)) {
            cardsPane.getChildren().add(buildCard(p));
        }

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
        btnPrev.setDisable(currentPage == 1);
        btnNext.setDisable(currentPage == totalPages);
    }

    private VBox buildCard(Product p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("prod-card");
        card.setPrefWidth(260);

        Label nom = new Label(p.getNom());
        nom.getStyleClass().add("prod-card-name");

        Label prix = new Label(String.format("%.2f TND", p.getPrix()));
        prix.getStyleClass().add("prod-card-price");

        Label stock = new Label("Stock : " + p.getStock());
        stock.getStyleClass().add("prod-card-stock");

        card.getChildren().addAll(nom, prix, stock);
        return card;
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