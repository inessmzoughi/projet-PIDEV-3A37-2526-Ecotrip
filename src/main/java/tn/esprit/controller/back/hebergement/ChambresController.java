package tn.esprit.controller.back.hebergement;

import javafx.scene.layout.VBox;
import tn.esprit.models.Chambre;
import tn.esprit.models.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.Chambre_service;
import tn.esprit.services.hebergement.Hebergement_service;
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
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour Chambres.fxml
 * Adapté au vrai modèle Chambre (id, numero, type, capacite, prix_par_nuit,
 * description, disponible, hebergement_id) et à Chambre_service / Hebergement_service (MySQL).
 */
public class ChambresController implements Initializable {

    /* ─── Injections FXML ─── */
    @FXML private Label statTotal, statPrix, statCap, statHeb;

    @FXML private VBox formPanel;
    @FXML private Label     formPanelTitle;

    // Champs du formulaire — calqués sur les vraies colonnes de Chambre
    @FXML private TextField            numeroField;      // numero
    @FXML private TextField            prixField;        // prix_par_nuit
    @FXML private TextField            capField;         // capacite
    @FXML private TextArea             descField;        // description
    @FXML private ComboBox<String>     typeCombo;        // type
    @FXML private ComboBox<Hebergement> hebCombo;        // hebergement_id
    @FXML private ComboBox<String>     dispoCombo;       // disponible (0/1)

    @FXML private TextField            searchField;
    @FXML private ComboBox<String>     filterType;
    @FXML private ComboBox<String>     sortCombo;

    @FXML private TableView<Chambre>            tableView;
    @FXML private TableColumn<Chambre, Integer> colIndex;
    @FXML private TableColumn<Chambre, String>  colNumero;   // remplace colNom
    @FXML private TableColumn<Chambre, String>  colHeb;
    @FXML private TableColumn<Chambre, String>  colType;
    @FXML private TableColumn<Chambre, String>  colDesc;
    @FXML private TableColumn<Chambre, Double>  colPrix;
    @FXML private TableColumn<Chambre, Integer> colCap;
    @FXML private TableColumn<Chambre, Integer> colDispo;
    @FXML private TableColumn<Chambre, Void>    colActions;

    @FXML private Label badgeCount;

    /* ─── Services ─── */
    private final Chambre_service     service    = new Chambre_service();
    private final Hebergement_service hebService = new Hebergement_service();

    /* ─── État ─── */
    private List<Chambre>      allData;
    private List<Hebergement>  allHeb;
    private Integer editingId = null;

    private static final String[] TYPES = {"Simple", "Double", "Suite", "Familiale"};
    private static final Map<String, String> TYPE_STYLE = Map.of(
            "Simple",    "chip-gray",
            "Double",    "chip-blue",
            "Suite",     "chip-purple",
            "Familiale", "chip-green"
    );

    /* ══════════════════════════════════════════
       INITIALIZE
       ══════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.setItems(FXCollections.observableArrayList(TYPES));
        typeCombo.getSelectionModel().select("Double");

        dispoCombo.setItems(FXCollections.observableArrayList("Disponible", "Indisponible"));
        dispoCombo.getSelectionModel().selectFirst();

        filterType.setItems(FXCollections.observableArrayList("", "Simple", "Double", "Suite", "Familiale"));
        filterType.setPromptText("Tous les types");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…", "Numéro (A→Z)", "Prix (croissant)", "Capacité (croissante)"));
        sortCombo.getSelectionModel().selectFirst();

        loadHebergements();
        setupColumns();
        loadData();
    }

    /* ══════════════════════════════════════════
       CHARGEMENT MYSQL
       ══════════════════════════════════════════ */
    private void loadHebergements() {
        try {
            allHeb = hebService.getAll();
        } catch (SQLException e) {
            allHeb = List.of();
        }
        hebCombo.setItems(FXCollections.observableArrayList(allHeb));
        hebCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Hebergement h)    { return h == null ? "" : h.getNom(); }
            @Override public Hebergement fromString(String s)  { return null; }
        });
    }

    private void loadData() {
        try {
            allData = service.getAll();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement",
                    "Impossible de lire les chambres : " + e.getMessage());
            allData = List.of();
        }
        renderTable();
        updateStats();
    }

    /* ══════════════════════════════════════════
       COLONNES
       ══════════════════════════════════════════ */
    private void setupColumns() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colPrix  .setCellValueFactory(new PropertyValueFactory<>("prix_par_nuit"));
        colCap   .setCellValueFactory(new PropertyValueFactory<>("capacite"));
        colType  .setCellValueFactory(new PropertyValueFactory<>("type"));

        // Index
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer i, boolean e) {
                super.updateItem(i, e);
                setText(e ? null : String.valueOf(getIndex() + 1));
                getStyleClass().add("td-index");
            }
        });

        // Hébergement lié
        colHeb.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                if (e || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                Chambre c = (Chambre) getTableRow().getItem();
                String name = allHeb.stream()
                        .filter(h -> h.getId() == c.getHebergement_id())
                        .map(Hebergement::getNom).findFirst().orElse("—");
                setText("📍 " + name);
            }
        });

        // Type avec chip
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                chip.getStyleClass().addAll("chip", TYPE_STYLE.getOrDefault(item, "chip-gray"));
                setGraphic(chip);
                setText(null);
            }
        });

        // Prix
        colPrix.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); return; }
                setText((int) Math.round(v) + " DT");
                getStyleClass().add("td-price");
            }
        });

        // Capacité
        colCap.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean e) {
                super.updateItem(v, e);
                setText(e || v == null ? null : v + " pers.");
            }
        });

        // Disponibilité
        colDispo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean e) {
                super.updateItem(v, e);
                if (e || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Chambre c = (Chambre) getTableRow().getItem();
                Label chip = new Label(c.getDisponible() == 1 ? "✅ Disponible" : "❌ Indisponible");
                chip.getStyleClass().add(c.getDisponible() == 1 ? "chip-green" : "chip-gray");
                setGraphic(chip);
                setText(null);
            }
        });

        // Description
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button delBtn  = new Button("🗑️");
            private final HBox   box     = new HBox(8, editBtn, delBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn .getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> openEdit(getTableView().getItems().get(getIndex())));
                delBtn .setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void i, boolean e) {
                super.updateItem(i, e);
                setGraphic(e ? null : box);
            }
        });
    }

    /* ══════════════════════════════════════════
       RENDU TABLEAU
       ══════════════════════════════════════════ */
    private void renderTable() {
        String q    = searchField.getText().toLowerCase().trim();
        String type = filterType.getValue();
        String sort = sortCombo.getValue();

        List<Chambre> filtered = allData.stream()
                .filter(c -> q.isEmpty()
                        || c.getNumero().toLowerCase().contains(q)
                        || hebName(c.getHebergement_id()).toLowerCase().contains(q)
                        || c.getDescription().toLowerCase().contains(q))
                .filter(c -> type == null || type.isEmpty() || c.getType().equals(type))
                .collect(Collectors.toList());

        if ("Numéro (A→Z)".equals(sort))
            filtered.sort(Comparator.comparing(Chambre::getNumero));
        else if ("Prix (croissant)".equals(sort))
            filtered.sort(Comparator.comparingDouble(Chambre::getPrix_par_nuit));
        else if ("Capacité (croissante)".equals(sort))
            filtered.sort(Comparator.comparingInt(Chambre::getCapacite));

        badgeCount.setText(String.valueOf(filtered.size()));
        tableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));
        statPrix.setText(allData.isEmpty() ? "—" :
                (int) Math.round(allData.stream().mapToDouble(Chambre::getPrix_par_nuit).average().orElse(0)) + " DT");
        statCap.setText(allData.isEmpty() ? "—" :
                String.valueOf((int) Math.round(allData.stream().mapToInt(Chambre::getCapacite).average().orElse(0))));
        long uniqueHeb = allData.stream().map(Chambre::getHebergement_id).distinct().count();
        statHeb.setText(String.valueOf(uniqueHeb));
    }

    private String hebName(int hebId) {
        return allHeb.stream()
                .filter(h -> h.getId() == hebId)
                .map(Hebergement::getNom).findFirst().orElse("—");
    }

    /* ══════════════════════════════════════════
       FORMULAIRE
       ══════════════════════════════════════════ */
    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("🛏️ Nouvelle Chambre");
        numeroField.clear();
        prixField.clear();
        capField.clear();
        descField.clear();
        typeCombo.getSelectionModel().select("Double");
        dispoCombo.getSelectionModel().selectFirst();
        hebCombo.getSelectionModel().clearSelection();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(Chambre c) {
        editingId = c.getId();
        formPanelTitle.setText("✏️ Modifier la Chambre");
        numeroField.setText(c.getNumero());
        prixField  .setText(String.valueOf((int) c.getPrix_par_nuit()));
        capField   .setText(String.valueOf(c.getCapacite()));
        descField  .setText(c.getDescription());
        typeCombo  .setValue(c.getType());
        dispoCombo .setValue(c.getDisponible() == 1 ? "Disponible" : "Indisponible");
        allHeb.stream().filter(h -> h.getId() == c.getHebergement_id()).findFirst()
                .ifPresent(h -> hebCombo.setValue(h));
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        String numero = numeroField.getText().trim();
        if (numero.isEmpty() || prixField.getText().trim().isEmpty() || capField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Numéro, prix et capacité sont obligatoires.");
            return;
        }
        try {
            double prix  = Double.parseDouble(prixField.getText().trim());
            int    cap   = Integer.parseInt(capField.getText().trim());
            String desc  = descField.getText().trim();
            String type  = typeCombo.getValue();
            int    dispo = "Disponible".equals(dispoCombo.getValue()) ? 1 : 0;
            int    hebId = hebCombo.getValue() == null ? 0 : hebCombo.getValue().getId();

            if (editingId != null) {
                Chambre c = new Chambre(editingId, hebId, dispo, desc, prix, cap, type, numero);
                service.modifier(c);
            } else {
                Chambre c = new Chambre(0, hebId, dispo, desc, prix, cap, type, numero);
                service.ajouter(c);
            }
            onCloseForm();
            loadData();
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Valeur invalide", "Veuillez entrer des valeurs numériques valides.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage());
        }
    }

    private void confirmDelete(Chambre c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la chambre ?");
        alert.setHeaderText("🗑️  Supprimer la chambre « " + c.getNumero() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/ecotrip.css").toExternalForm());
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.supprimer(c.getId());
                loadData();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Suppression impossible : " + ex.getMessage());
            }
        });
    }

    /* ══════════════════════════════════════════
       SEARCH / FILTER
       ══════════════════════════════════════════ */
    @FXML private void onSearch() { renderTable(); }

    /* ══════════════════════════════════════════
       NAVIGATION
       ══════════════════════════════════════════ */
    @FXML private void onNavHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void onNavEquipements()  { navigateTo("Equipements.fxml",            "Équipements"); }
    @FXML private void onNavCategories()   { navigateTo("CategoriesHebergement.fxml",  "Catégories"); }
    @FXML private void onLogout()          { System.exit(0); }
    @FXML private void onNavDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    private void navigateTo(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/" + fxml));
            Stage  stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle("EcoTrip Admin — " + title);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    /* ══════════════════════════════════════════
       HELPERS
       ══════════════════════════════════════════ */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/ecotrip.css").toExternalForm());
        a.showAndWait();
    }
}