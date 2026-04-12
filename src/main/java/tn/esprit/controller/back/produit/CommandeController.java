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
import tn.esprit.models.produit.Commande;
import tn.esprit.services.produit.CommandeService;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class CommandeController implements Initializable {

    @FXML private Label statTotal, statTotal2, statQte, statClients;

    @FXML private VBox formPanel;
    @FXML private Label formPanelTitle;

    @FXML private ComboBox<Integer> userCombo;
    @FXML private ComboBox<Integer> produitCombo;
    @FXML private TextField quantiteField;
    @FXML private TextField prixUnitaireField;
    @FXML private TextField totalField;
    @FXML private DatePicker datePicker;

    @FXML private TextField searchField;
    @FXML private ComboBox<Integer> filterUser;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<Commande> tableView;
    @FXML private TableColumn<Commande, Integer> colIndex;
    @FXML private TableColumn<Commande, Integer> colUser;
    @FXML private TableColumn<Commande, Integer> colProduit;
    @FXML private TableColumn<Commande, Integer> colQuantite;
    @FXML private TableColumn<Commande, Double> colPrixUnitaire;
    @FXML private TableColumn<Commande, Double> colTotal;
    @FXML private TableColumn<Commande, Date> colDate;
    @FXML private TableColumn<Commande, Void> colActions;

    @FXML private Label badgeCount;

    private final CommandeService service = new CommandeService();
    private List<Commande> allData = new ArrayList<>();
    private Integer editingId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…",
                "Date (récent)",
                "Total (croissant)",
                "Quantité (croissante)"
        ));
        sortCombo.getSelectionModel().selectFirst();

        totalField.setEditable(false);

        quantiteField.textProperty().addListener((obs, oldVal, newVal) -> recalculerTotal());
        prixUnitaireField.textProperty().addListener((obs, oldVal, newVal) -> recalculerTotal());

        setupColumns();
        loadCombos();
        loadData();
    }

    private void recalculerTotal() {
        try {
            int qte = Integer.parseInt(quantiteField.getText().trim());
            double prix = Double.parseDouble(prixUnitaireField.getText().trim());

            if (qte < 0 || prix < 0) {
                totalField.clear();
                return;
            }

            totalField.setText(String.format(Locale.US, "%.2f", qte * prix));
        } catch (Exception e) {
            totalField.clear();
        }
    }

    private void loadCombos() {
        loadUserCombo();
        loadProduitCombo();
    }

    private void loadUserCombo() {
        List<Integer> userIds = loadIds("user", "id");
        userCombo.setItems(FXCollections.observableArrayList(userIds));
    }

    private void loadProduitCombo() {
        List<Integer> produitIds = loadIds("produits", "id");
        produitCombo.setItems(FXCollections.observableArrayList(produitIds));
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
                    "Impossible de lire les commandes : " + e.getMessage());
            allData = new ArrayList<>();
        }

        List<Integer> userIds = allData.stream()
                .map(Commande::getIdUser)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        filterUser.setItems(FXCollections.observableArrayList(userIds));
        filterUser.setPromptText("Tous les utilisateurs");

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

        colUser.setCellValueFactory(new PropertyValueFactory<>("idUser"));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "User #" + v);
            }
        });

        colProduit.setCellValueFactory(new PropertyValueFactory<>("produitId"));
        colProduit.setCellFactory(col -> new TableCell<>() {
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

        colPrixUnitaire.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        colPrixUnitaire.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format(Locale.US, "%.2f DT", v));
            }
        });

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format(Locale.US, "%.2f DT", v));
            }
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCommande"));
        colDate.setCellFactory(col -> new TableCell<>() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Date v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : sdf.format(v));
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    Commande cmd = getCurrentRowItem();
                    if (cmd != null) {
                        openEdit(cmd);
                    }
                });

                delBtn.setOnAction(e -> {
                    Commande cmd = getCurrentRowItem();
                    if (cmd != null) {
                        confirmDelete(cmd);
                    }
                });
            }

            private Commande getCurrentRowItem() {
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
        Integer user = filterUser.getValue();
        String sort = sortCombo.getValue();

        List<Commande> filtered = allData.stream()
                .filter(c ->
                        q.isEmpty()
                                || String.valueOf(c.getId()).contains(q)
                                || String.valueOf(c.getIdUser()).contains(q)
                                || String.valueOf(c.getProduitId()).contains(q)
                                || String.valueOf(c.getQuantite()).contains(q)
                                || String.valueOf(c.getTotal()).contains(q)
                )
                .filter(c -> user == null || Objects.equals(c.getIdUser(), user))
                .collect(Collectors.toList());

        if ("Date (récent)".equals(sort)) {
            filtered.sort(Comparator.comparing(
                    Commande::getDateCommande,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));
        } else if ("Total (croissant)".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(Commande::getTotal));
        } else if ("Quantité (croissante)".equals(sort)) {
            filtered.sort(Comparator.comparingInt(Commande::getQuantite));
        }

        badgeCount.setText(String.valueOf(filtered.size()));
        tableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));

        double ca = allData.stream().mapToDouble(Commande::getTotal).sum();
        statTotal2.setText(String.format(Locale.US, "%.2f DT", ca));

        double qteAvg = allData.stream().mapToInt(Commande::getQuantite).average().orElse(0);
        statQte.setText(allData.isEmpty() ? "—" : String.format(Locale.US, "%.1f", qteAvg));

        long clients = allData.stream().map(Commande::getIdUser).distinct().count();
        statClients.setText(String.valueOf(clients));
    }

    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("Nouvelle Commande");
        userCombo.getSelectionModel().clearSelection();
        produitCombo.getSelectionModel().clearSelection();
        quantiteField.clear();
        prixUnitaireField.clear();
        totalField.clear();
        datePicker.setValue(java.time.LocalDate.now());
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(Commande c) {
        editingId = c.getId();
        formPanelTitle.setText("Modifier la Commande");
        userCombo.setValue(c.getIdUser());
        produitCombo.setValue(c.getProduitId());
        quantiteField.setText(String.valueOf(c.getQuantite()));
        prixUnitaireField.setText(String.valueOf(c.getPrixUnitaire()));
        totalField.setText(String.format(Locale.US, "%.2f", c.getTotal()));

        if (c.getDateCommande() != null) {
            datePicker.setValue(
                    c.getDateCommande().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
            );
        } else {
            datePicker.setValue(java.time.LocalDate.now());
        }

        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        if (userCombo.getValue() == null
                || produitCombo.getValue() == null
                || quantiteField.getText().trim().isEmpty()
                || prixUnitaireField.getText().trim().isEmpty()
                || datePicker.getValue() == null) {

            showAlert(Alert.AlertType.WARNING, "Champs manquants",
                    "Utilisateur, produit, quantité, prix et date sont obligatoires.");
            return;
        }

        try {
            int idUser = userCombo.getValue();
            int idProduit = produitCombo.getValue();
            int quantite = Integer.parseInt(quantiteField.getText().trim());
            double prix = Double.parseDouble(prixUnitaireField.getText().trim());

            if (quantite <= 0) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                        "La quantité doit être strictement positive.");
                return;
            }

            if (prix < 0) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                        "Le prix unitaire ne peut pas être négatif.");
                return;
            }

            double total = quantite * prix;

            LocalDateTime dateTime = datePicker.getValue().atTime(LocalDateTime.now().toLocalTime());
            Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

            Commande c;
            if (editingId != null) {
                c = new Commande(editingId, idUser, idProduit, quantite, prix, total, date);
                service.update(c);
            } else {
                c = new Commande(idUser, idProduit, quantite, prix, total, date);
                service.create(c);
            }

            onCloseForm();
            loadData();

        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                    "Quantité et prix doivent être numériques.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage());
        }
    }

    private void confirmDelete(Commande c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la commande ?");
        alert.setHeaderText("Supprimer la commande #" + c.getId() + " ?");
        alert.setContentText("Cette action est irréversible.");

        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);

        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.delete(c.getId());
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
    @FXML private void onNavProduits() { navigateTo("Product.fxml", "Produits"); }
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
            showAlert(Alert.AlertType.ERROR, "Erreur navigation",
                    "Impossible d'ouvrir la page : " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
}