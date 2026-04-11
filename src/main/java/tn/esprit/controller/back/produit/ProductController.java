package tn.esprit.controller.back.produit;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.database.Base;
import tn.esprit.models.produit.Product;
import tn.esprit.services.produit.ProductService;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ProductController implements Initializable {

    @FXML private Label statTotal, statValeurStock, statPrixMoyen, statCategories;

    @FXML private VBox formPanel;
    @FXML private Label formPanelTitle;

    @FXML private TextField nomField;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private ComboBox<Integer> categoryCombo;
    @FXML private TextField imageField;

    @FXML private TextField searchField;
    @FXML private ComboBox<Integer> filterCategory;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<Product> tableView;
    @FXML private TableColumn<Product, Integer> colIndex;
    @FXML private TableColumn<Product, String> colNom;
    @FXML private TableColumn<Product, Double> colPrix;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Integer> colCategory;
    @FXML private TableColumn<Product, String> colImage;
    @FXML private TableColumn<Product, Void> colActions;

    @FXML private Label badgeCount;

    private final ProductService service = new ProductService();
    private List<Product> allData = new ArrayList<>();
    private Integer editingId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…",
                "Nom (A→Z)",
                "Prix (croissant)",
                "Stock (croissant)"
        ));
        sortCombo.getSelectionModel().selectFirst();

        setupColumns();
        loadCombos();
        loadData();
    }

    private void loadCombos() {
        List<Integer> categoryIds = loadIds("categorie", "id");
        categoryCombo.setItems(FXCollections.observableArrayList(categoryIds));
        filterCategory.setItems(FXCollections.observableArrayList(categoryIds));
        filterCategory.setPromptText("Toutes les catégories");
    }

    private List<Integer> loadIds(String table, String idColumn) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT " + idColumn + " FROM " + table + " ORDER BY " + idColumn;

        Connection cnx = Base.getInstance().getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getInt(idColumn));
            }

        } catch (SQLException e) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Erreur SQL",
                    "Impossible de charger les ids depuis " + table + " : " + e.getMessage()
            );
        }

        return ids;
    }

    private void loadData() {
        try {
            allData = service.read();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur de chargement",
                    "Impossible de lire les produits : " + e.getMessage());
            allData = new ArrayList<>();
        }

        renderTable();
        updateStats();
    }

    private void setupColumns() {
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                if (!getStyleClass().contains("td-index")) {
                    getStyleClass().add("td-index");
                }
            }
        });

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText("📦 " + v);
            }
        });

        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colPrix.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label chip = new Label(String.format(Locale.US, "%.2f DT", v));
                chip.getStyleClass().addAll("chip", "chip-green");
                setGraphic(chip);
                setText(null);
            }
        });

        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label chip = new Label(v + " unité(s)");
                chip.getStyleClass().addAll("chip", v <= 5 ? "chip-red" : "chip-blue");
                setGraphic(chip);
                setText(null);
            }
        });

        colCategory.setCellValueFactory(new PropertyValueFactory<>("productCategoryId"));
        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "🏷 Catégorie #" + v);
            }
        });

        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colImage.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);

                if (empty) {
                    setText(null);
                    return;
                }

                if (v == null || v.isBlank()) {
                    setText("—");
                    return;
                }

                String[] parts = v.replace("\\", "/").split("/");
                setText("🖼 " + parts[parts.length - 1]);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    Product product = getCurrentRowItem();
                    if (product != null) {
                        openEdit(product);
                    }
                });

                delBtn.setOnAction(e -> {
                    Product product = getCurrentRowItem();
                    if (product != null) {
                        confirmDelete(product);
                    }
                });
            }

            private Product getCurrentRowItem() {
                int index = getIndex();
                if (index >= 0 && index < getTableView().getItems().size()) {
                    return getTableView().getItems().get(index);
                }
                return null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void renderTable() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        Integer cat = filterCategory.getValue();
        String sort = sortCombo.getValue();

        List<Product> filtered = allData.stream()
                .filter(p ->
                        q.isEmpty()
                                || (p.getNom() != null && p.getNom().toLowerCase().contains(q))
                                || String.valueOf(p.getPrix()).contains(q)
                                || String.valueOf(p.getStock()).contains(q)
                                || String.valueOf(p.getProductCategoryId()).contains(q)
                )
                .filter(p -> cat == null || Objects.equals(p.getProductCategoryId(), cat))
                .toList();

        List<Product> sorted = new ArrayList<>(filtered);

        if ("Nom (A→Z)".equals(sort)) {
            sorted.sort(Comparator.comparing(Product::getNom, String.CASE_INSENSITIVE_ORDER));
        } else if ("Prix (croissant)".equals(sort)) {
            sorted.sort(Comparator.comparingDouble(Product::getPrix));
        } else if ("Stock (croissant)".equals(sort)) {
            sorted.sort(Comparator.comparingInt(Product::getStock));
        }

        badgeCount.setText(String.valueOf(sorted.size()));
        tableView.setItems(FXCollections.observableArrayList(sorted));
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));

        double valeurStock = allData.stream()
                .mapToDouble(p -> p.getPrix() * p.getStock())
                .sum();
        statValeurStock.setText(String.format(Locale.US, "%.2f DT", valeurStock));

        double prixMoyen = allData.stream()
                .mapToDouble(Product::getPrix)
                .average()
                .orElse(0);
        statPrixMoyen.setText(allData.isEmpty() ? "—" : String.format(Locale.US, "%.2f DT", prixMoyen));

        long nbCategories = allData.stream()
                .map(Product::getProductCategoryId)
                .distinct()
                .count();
        statCategories.setText(String.valueOf(nbCategories));
    }

    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("Nouveau Produit");
        nomField.clear();
        prixField.clear();
        stockField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        imageField.clear();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(Product p) {
        editingId = p.getId();
        formPanelTitle.setText("Modifier le Produit");
        nomField.setText(p.getNom());
        prixField.setText(String.valueOf(p.getPrix()));
        stockField.setText(String.valueOf(p.getStock()));
        categoryCombo.setValue(p.getProductCategoryId());
        imageField.setText(p.getImage() != null ? p.getImage() : "");
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        if (nomField.getText().trim().isEmpty()
                || prixField.getText().trim().isEmpty()
                || stockField.getText().trim().isEmpty()
                || categoryCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Champs manquants",
                    "Nom, prix, stock et catégorie sont obligatoires.");
            return;
        }

        try {
            String nom = nomField.getText().trim();
            double prix = Double.parseDouble(prixField.getText().trim());
            int stock = Integer.parseInt(stockField.getText().trim());
            int catId = categoryCombo.getValue();
            String image = imageField.getText() == null ? null : imageField.getText().trim();

            if (prix < 0 || stock < 0) {
                showAlert(Alert.AlertType.WARNING,
                        "Valeur invalide",
                        "Le prix et le stock doivent être positifs.");
                return;
            }

            if (image != null && image.isEmpty()) {
                image = null;
            }

            if (editingId != null) {
                service.update(new Product(editingId, nom, prix, stock, catId, image));
            } else {
                service.create(new Product(nom, prix, stock, catId, image));
            }

            onCloseForm();
            loadData();

        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING,
                    "Valeur invalide",
                    "Prix doit être un nombre décimal et stock un entier.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur SQL",
                    ex.getMessage());
        }
    }

    private void confirmDelete(Product p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le produit ?");
        alert.setHeaderText("Supprimer « " + p.getNom() + " » ?");
        alert.setContentText("Cette action est irréversible.");

        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);

        try {
            URL css = getClass().getResource("/css/ecotrip.css");
            if (css != null) {
                alert.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.delete(p.getId());
                loadData();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur SQL",
                        "Suppression impossible : " + ex.getMessage());
            }
        });
    }

    @FXML
    private void onSearch() {
        renderTable();
    }

    @FXML private void onNavHebergements() { navigateTo("ListHebergements.fxml", "Hébergements"); }
    @FXML private void onNavChambres() { navigateTo("Chambres.fxml", "Chambres"); }
    @FXML private void onNavEquipements() { navigateTo("Equipements.fxml", "Équipements"); }
    @FXML private void onNavCategories() { navigateTo("CategoriesHebergement.fxml", "Catégories"); }
    @FXML private void onNavCommandes() { navigateTo("Commande.fxml", "Commandes"); }
    @FXML private void onNavLignesCommande() { navigateTo("LigneCommande.fxml", "Lignes de commande"); }
    @FXML private void onNavPaiements() { navigateTo("Payment.fxml", "Paiements"); }
    @FXML private void onNavUtilisateurs() { navigateTo("Utilisateurs.fxml", "Utilisateurs"); }
    @FXML private void onNavCategoriesProduit() { navigateTo("ProductCategory.fxml", "Catégories produit"); }
    
    @FXML
    private void onLogout() {
        Stage stage = (Stage) tableView.getScene().getWindow();
        stage.close();
    }

    private void navigateTo(String fxml, String title) {
        try {
            URL location = getClass().getResource("/fxml/" + fxml);
            if (location == null) {
                showAlert(Alert.AlertType.INFORMATION,
                        "Page non disponible",
                        "La page « " + title + " » n'est pas encore implémentée.");
                return;
            }

            Parent root = FXMLLoader.load(location);
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle("EcoTrip Admin — " + title);

        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur navigation",
                    "Impossible d'ouvrir la page : " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);

        try {
            URL css = getClass().getResource("/css/ecotrip.css");
            if (css != null) {
                a.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        a.showAndWait();
    }
}