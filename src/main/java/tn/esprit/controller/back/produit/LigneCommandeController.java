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
import tn.esprit.models.produit.LigneCommande;
import tn.esprit.services.produit.LigneCommandeService;
import tn.esprit.database.Base;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class LigneCommandeController implements Initializable {

    @FXML private Label statTotal, statTotalCA, statQte, statCommandes;

    @FXML private VBox formPanel;
    @FXML private Label formPanelTitle;

    @FXML private ComboBox<Integer> commandeCombo;
    @FXML private ComboBox<Integer> productCombo;
    @FXML private TextField quantiteField;
    @FXML private TextField unitpriceField;
    @FXML private TextField subtotalField;

    @FXML private TextField searchField;
    @FXML private ComboBox<Integer> filterCommande;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<LigneCommande> tableView;
    @FXML private TableColumn<LigneCommande, Integer> colIndex;
    @FXML private TableColumn<LigneCommande, Integer> colCommande;
    @FXML private TableColumn<LigneCommande, Integer> colProduct;
    @FXML private TableColumn<LigneCommande, Integer> colQuantite;
    @FXML private TableColumn<LigneCommande, Double> colUnitprice;
    @FXML private TableColumn<LigneCommande, Double> colSubtotal;
    @FXML private TableColumn<LigneCommande, Void> colActions;

    @FXML private Label badgeCount;

    private final LigneCommandeService service = new LigneCommandeService();
    private List<LigneCommande> allData = new ArrayList<>();
    private Integer editingId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…",
                "Commande (croissant)",
                "Subtotal (croissant)",
                "Quantité (croissante)"
        ));
        sortCombo.getSelectionModel().selectFirst();

        subtotalField.setEditable(false);

        quantiteField.textProperty().addListener((obs, oldVal, newVal) -> recalculerSubtotal());
        unitpriceField.textProperty().addListener((obs, oldVal, newVal) -> recalculerSubtotal());

        setupColumns();
        loadCombos();
        loadData();
    }

    private void recalculerSubtotal() {
        try {
            int qte = Integer.parseInt(quantiteField.getText().trim());
            double prix = Double.parseDouble(unitpriceField.getText().trim());

            if (qte < 0 || prix < 0) {
                subtotalField.clear();
                return;
            }

            subtotalField.setText(String.format(Locale.US, "%.2f", qte * prix));
        } catch (Exception e) {
            subtotalField.clear();
        }
    }

    private void loadCombos() {
        loadCommandeCombo();
        loadProduitCombo();
    }

    private void loadCommandeCombo() {
        List<Integer> commandeIds = loadIds("commandes", "id");
        commandeCombo.setItems(FXCollections.observableArrayList(commandeIds));
    }

    private void loadProduitCombo() {
        List<Integer> produitIds = loadIds("produits", "id");
        productCombo.setItems(FXCollections.observableArrayList(produitIds));
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
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement",
                    "Impossible de lire les lignes de commande : " + e.getMessage());
            allData = new ArrayList<>();
        }

        List<Integer> commandeIds = allData.stream()
                .map(LigneCommande::getCommandeId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        filterCommande.setItems(FXCollections.observableArrayList(commandeIds));
        filterCommande.setPromptText("Toutes les commandes");

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

        colCommande.setCellValueFactory(new PropertyValueFactory<>("commandeId"));
        colCommande.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "Commande #" + v);
            }
        });

        colProduct.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colProduct.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "Produit #" + v);
            }
        });

        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colQuantite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v + " unité(s)");
            }
        });

        colUnitprice.setCellValueFactory(new PropertyValueFactory<>("unitprice"));
        colUnitprice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    return;
                }
                setText(String.format(Locale.US, "%.2f DT", v));
                if (!getStyleClass().contains("td-price")) {
                    getStyleClass().add("td-price");
                }
            }
        });

        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colSubtotal.setCellFactory(col -> new TableCell<>() {
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

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    LigneCommande ligne = getCurrentRowItem();
                    if (ligne != null) {
                        openEdit(ligne);
                    }
                });

                delBtn.setOnAction(e -> {
                    LigneCommande ligne = getCurrentRowItem();
                    if (ligne != null) {
                        confirmDelete(ligne);
                    }
                });
            }

            private LigneCommande getCurrentRowItem() {
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
        Integer commande = filterCommande.getValue();
        String sort = sortCombo.getValue();

        List<LigneCommande> filtered = allData.stream()
                .filter(l ->
                        q.isEmpty()
                                || String.valueOf(l.getId()).contains(q)
                                || String.valueOf(l.getCommandeId()).contains(q)
                                || String.valueOf(l.getProductId()).contains(q)
                                || String.valueOf(l.getQuantite()).contains(q)
                                || String.valueOf(l.getSubtotal()).contains(q)
                )
                .filter(l -> commande == null || Objects.equals(l.getCommandeId(), commande))
                .collect(Collectors.toList());

        if ("Commande (croissant)".equals(sort)) {
            filtered.sort(Comparator.comparingInt(LigneCommande::getCommandeId));
        } else if ("Subtotal (croissant)".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(LigneCommande::getSubtotal));
        } else if ("Quantité (croissante)".equals(sort)) {
            filtered.sort(Comparator.comparingInt(LigneCommande::getQuantite));
        }

        badgeCount.setText(String.valueOf(filtered.size()));
        tableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));

        double ca = allData.stream().mapToDouble(LigneCommande::getSubtotal).sum();
        statTotalCA.setText(String.format(Locale.US, "%.2f DT", ca));

        double qteAvg = allData.stream().mapToInt(LigneCommande::getQuantite).average().orElse(0);
        statQte.setText(allData.isEmpty() ? "—" : String.format(Locale.US, "%.1f", qteAvg));

        long nbCommandes = allData.stream().map(LigneCommande::getCommandeId).distinct().count();
        statCommandes.setText(String.valueOf(nbCommandes));
    }

    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("Nouvelle Ligne de Commande");
        commandeCombo.getSelectionModel().clearSelection();
        productCombo.getSelectionModel().clearSelection();
        quantiteField.clear();
        unitpriceField.clear();
        subtotalField.clear();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(LigneCommande l) {
        editingId = l.getId();
        formPanelTitle.setText("Modifier la Ligne de Commande");
        commandeCombo.setValue(l.getCommandeId());
        productCombo.setValue(l.getProductId());
        quantiteField.setText(String.valueOf(l.getQuantite()));
        unitpriceField.setText(String.valueOf(l.getUnitprice()));
        subtotalField.setText(String.format(Locale.US, "%.2f", l.getSubtotal()));
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        if (commandeCombo.getValue() == null
                || productCombo.getValue() == null
                || quantiteField.getText().trim().isEmpty()
                || unitpriceField.getText().trim().isEmpty()) {

            showAlert(Alert.AlertType.WARNING, "Champs manquants",
                    "Commande, produit, quantité et prix unitaire sont obligatoires.");
            return;
        }

        try {
            int commandeId = commandeCombo.getValue();
            int productId = productCombo.getValue();
            int quantite = Integer.parseInt(quantiteField.getText().trim());
            double unitprice = Double.parseDouble(unitpriceField.getText().trim());

            if (quantite <= 0) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                        "La quantité doit être strictement positive.");
                return;
            }

            if (unitprice < 0) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                        "Le prix unitaire ne peut pas être négatif.");
                return;
            }

            double subtotal = quantite * unitprice;

            LigneCommande l;
            if (editingId != null) {
                l = new LigneCommande(editingId, commandeId, productId, quantite, unitprice, subtotal);
                service.update(l);
            } else {
                l = new LigneCommande(commandeId, productId, quantite, unitprice, subtotal);
                service.create(l);
            }

            onCloseForm();
            loadData();

        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                    "Quantité et prix unitaire doivent être numériques.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage());
        }
    }

    private void confirmDelete(LigneCommande l) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la ligne ?");
        alert.setHeaderText("Supprimer la ligne #" + l.getId() + " ?");
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
                service.delete(l.getId());
                loadData();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur SQL",
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
    @FXML private void onNavProduits() { navigateTo("Product.fxml", "Produits"); }
    @FXML private void onNavLignesCommande() { navigateTo("LigneCommande.fxml", "Lignes de commande"); }
    @FXML private void onNavPaiements() { navigateTo("Payment.fxml", "Paiements"); }
    @FXML private void onNavCategoriesProduit() { navigateTo("ProductCategory.fxml", "Catégories Produit"); }
    @FXML private void onNavUtilisateurs() { navigateTo("Utilisateurs.fxml", "Utilisateurs"); }
    @FXML private void onLogout() { System.exit(0); }

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
            showAlert(Alert.AlertType.ERROR, "Erreur navigation",
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