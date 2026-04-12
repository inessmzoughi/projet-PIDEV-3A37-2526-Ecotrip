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
import tn.esprit.models.produit.ProductCategory;
import tn.esprit.services.produit.ProductCategoryService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProductCategoryController implements Initializable {

    @FXML private Label statTotal, statAvecDesc, statSansDesc;
    @FXML private VBox formPanel;
    @FXML private Label formPanelTitle;

    @FXML private TextField nomField;
    @FXML private ComboBox<String> descriptionCombo;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<ProductCategory> tableView;
    @FXML private TableColumn<ProductCategory, Integer> colIndex;
    @FXML private TableColumn<ProductCategory, String> colNom;
    @FXML private TableColumn<ProductCategory, String> colDescription;
    @FXML private TableColumn<ProductCategory, Void> colActions;

    @FXML private Label badgeCount;

    private static final String OPT_LOUER = "À louer";
    private static final String OPT_VENDRE = "À vendre";

    private final ProductCategoryService service = new ProductCategoryService();
    private List<ProductCategory> allData = new ArrayList<>();
    private Integer editingId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        descriptionCombo.setItems(FXCollections.observableArrayList(
                OPT_LOUER,
                OPT_VENDRE
        ));
        descriptionCombo.setEditable(true);
        descriptionCombo.setPromptText("Description...");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…",
                "Nom (A → Z)",
                "Nom (Z → A)",
                "Avec description",
                "Sans description"
        ));
        sortCombo.getSelectionModel().selectFirst();

        setupColumns();
        loadData();
    }

    private void loadData() {
        try {
            allData = service.read();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur de chargement",
                    "Impossible de lire les catégories : " + e.getMessage());
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
            }
        });

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label chip = new Label("🏷 " + value);
                chip.getStyleClass().addAll("chip", "chip-green");
                setGraphic(chip);
                setText(null);
            }
        });

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                String desc = value == null ? "" : value.trim();

                if (desc.isEmpty()) {
                    Label chip = new Label("—");
                    chip.getStyleClass().addAll("chip", "chip-gray");
                    setGraphic(chip);
                    setText(null);
                    return;
                }

                Label chip = new Label(desc);

                if (OPT_LOUER.equalsIgnoreCase(desc)) {
                    chip.getStyleClass().addAll("chip", "chip-blue");
                } else if (OPT_VENDRE.equalsIgnoreCase(desc)) {
                    chip.getStyleClass().addAll("chip", "chip-orange");
                } else {
                    chip.getStyleClass().addAll("chip", "chip-gray");
                }

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
                    ProductCategory category = getCurrentRowItem();
                    if (category != null) {
                        openEdit(category);
                    }
                });

                delBtn.setOnAction(e -> {
                    ProductCategory category = getCurrentRowItem();
                    if (category != null) {
                        confirmDelete(category);
                    }
                });
            }

            private ProductCategory getCurrentRowItem() {
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
        String sort = sortCombo.getValue();

        List<ProductCategory> filtered = allData.stream()
                .filter(c ->
                        q.isEmpty()
                                || (c.getNom() != null && c.getNom().toLowerCase().contains(q))
                                || (c.getDescription() != null && c.getDescription().toLowerCase().contains(q))
                )
                .collect(Collectors.toList());

        if ("Nom (A → Z)".equals(sort)) {
            filtered.sort(Comparator.comparing(
                    c -> c.getNom() == null ? "" : c.getNom().toLowerCase()
            ));
        } else if ("Nom (Z → A)".equals(sort)) {
            filtered.sort(Comparator.comparing(
                    (ProductCategory c) -> c.getNom() == null ? "" : c.getNom().toLowerCase()
            ).reversed());
        } else if ("Avec description".equals(sort)) {
            filtered = filtered.stream()
                    .filter(c -> c.getDescription() != null && !c.getDescription().trim().isEmpty())
                    .collect(Collectors.toList());
        } else if ("Sans description".equals(sort)) {
            filtered = filtered.stream()
                    .filter(c -> c.getDescription() == null || c.getDescription().trim().isEmpty())
                    .collect(Collectors.toList());
        }

        badgeCount.setText(String.valueOf(filtered.size()));
        tableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));

        long avecDesc = allData.stream()
                .filter(c -> c.getDescription() != null && !c.getDescription().trim().isEmpty())
                .count();

        statAvecDesc.setText(String.valueOf(avecDesc));
        statSansDesc.setText(String.valueOf(allData.size() - avecDesc));
    }

    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("Nouvelle Catégorie");
        nomField.clear();
        descriptionCombo.getSelectionModel().clearSelection();
        descriptionCombo.getEditor().clear();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(ProductCategory c) {
        editingId = c.getId();
        formPanelTitle.setText("Modifier la Catégorie");
        nomField.setText(c.getNom());

        if (c.getDescription() != null) {
            descriptionCombo.setValue(c.getDescription());
            descriptionCombo.getEditor().setText(c.getDescription());
        } else {
            descriptionCombo.getSelectionModel().clearSelection();
            descriptionCombo.getEditor().clear();
        }

        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        String nom = nomField.getText() == null ? "" : nomField.getText().trim();

        String description = null;
        if (descriptionCombo.getEditor() != null) {
            description = descriptionCombo.getEditor().getText();
        }
        if ((description == null || description.trim().isEmpty()) && descriptionCombo.getValue() != null) {
            description = descriptionCombo.getValue();
        }

        // ── Validation 1 : nom obligatoire ──────────────────────────────────────
        if (nom.isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "Champ manquant",
                    "Le nom de la catégorie est obligatoire.");
            return;
        }

        // ── Validation 2 : nom = lettres uniquement (minuscules, majuscules, accents) ──
        if (!nom.matches("[a-zA-ZÀ-ÿ ]+")) {
            showAlert(Alert.AlertType.WARNING,
                    "Nom invalide",
                    "Le nom ne doit contenir que des lettres (sans chiffres ni caractères spéciaux).");
            return;
        }

        // ── Normalisation description ────────────────────────────────────────────
        if (description != null) {
            description = description.trim();
            if (description.isEmpty()) {
                description = null;
            }
        }

        // ── Validation 3 : description obligatoire et valeur valide uniquement ────
        if (description == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Champ manquant",
                    "La description est obligatoire.");
            return;
        }

        if (!OPT_LOUER.equalsIgnoreCase(description)
                && !OPT_VENDRE.equalsIgnoreCase(description)) {
            showAlert(Alert.AlertType.WARNING,
                    "Description invalide",
                    "La description doit être « À louer » ou « À vendre » uniquement.");
            return;
        }

        try {
            if (editingId != null) {
                service.update(new ProductCategory(editingId, nom, description));
            } else {
                service.create(new ProductCategory(nom, description));
            }

            onCloseForm();
            loadData();

        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage());
        }
    }

    private void confirmDelete(ProductCategory c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la catégorie ?");
        alert.setHeaderText("Supprimer « " + c.getNom() + " » ?");
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
                service.delete(c.getId());
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
    @FXML private void onNavProduits() { navigateTo("Product.fxml", "Produits"); }
    @FXML private void onNavCommandes() { navigateTo("Commande.fxml", "Commandes"); }
    @FXML private void onNavLignesCommande() { navigateTo("LigneCommande.fxml", "Lignes de commande"); }
    @FXML private void onNavPaiements() { navigateTo("Payment.fxml", "Paiements"); }
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