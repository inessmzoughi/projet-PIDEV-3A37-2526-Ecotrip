package org.example.controller;

import org.example.models.Equipement;
import org.example.services.Equipement_service;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour Equipements.fxml
 * Adapté au vrai modèle Equipement (id, nom, description)
 * et à Equipement_service (MySQL).
 *
 * Les champs icon, categorie et premium n'existent pas en base —
 * ils sont gérés uniquement en mémoire/UI pour l'affichage.
 */
public class EquipementsController implements Initializable {

    /* ─── Injections FXML ─── */
    @FXML private Label statTotal, statCats, statPremium;

    @FXML private HBox  catTabsPane;

    @FXML private VBox      formPanel;
    @FXML private Label     formPanelTitle;
    @FXML private TextField nomField;
    @FXML private TextArea  descField;        // description (seul champ texte libre en base)

    @FXML private TextField            searchField;

    @FXML private ToggleButton toggleCards, toggleList;

    @FXML private FlowPane  cardGrid;
    @FXML private VBox      tableCard;

    @FXML private TableView<Equipement>            tableView;
    @FXML private TableColumn<Equipement, Integer> colIndex;
    @FXML private TableColumn<Equipement, String>  colNom, colDesc;
    @FXML private TableColumn<Equipement, Void>    colActions;

    @FXML private Label badgeCount;

    /* ─── Service ─── */
    private final Equipement_service service = new Equipement_service();

    /* ─── État ─── */
    private List<Equipement> allData;
    private Integer editingId  = null;
    private boolean cardViewOn = false;

    /* ══════════════════════════════════════════
       INITIALIZE
       ══════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadData();
    }

    /* ══════════════════════════════════════════
       CHARGEMENT MYSQL
       ══════════════════════════════════════════ */
    private void loadData() {
        try {
            allData = service.getAll();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement",
                    "Impossible de lire les équipements : " + e.getMessage());
            allData = List.of();
        }
        renderAll();
        updateStats();
    }

    /* ══════════════════════════════════════════
       COLONNES
       ══════════════════════════════════════════ */
    private void setupColumns() {
        colNom .setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Index
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer i, boolean e) {
                super.updateItem(i, e);
                setText(e ? null : String.valueOf(getIndex() + 1));
                getStyleClass().add("td-index");
            }
        });

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
       RENDU
       ══════════════════════════════════════════ */
    private void renderAll() {
        String q = searchField.getText().toLowerCase().trim();

        List<Equipement> filtered = allData.stream()
                .filter(eq -> q.isEmpty()
                        || eq.getNom().toLowerCase().contains(q)
                        || eq.getDescription().toLowerCase().contains(q))
                .collect(Collectors.toList());

        badgeCount.setText(String.valueOf(filtered.size()));

        // Tableau
        tableView.setItems(FXCollections.observableArrayList(filtered));

        // Grille de cartes
        cardGrid.getChildren().clear();
        for (Equipement eq : filtered) {
            cardGrid.getChildren().add(buildEqCard(eq));
        }
    }

    private VBox buildEqCard(Equipement eq) {
        VBox card = new VBox(8);
        card.getStyleClass().add("eq-card");
        card.setPrefWidth(220);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-border-color:#38a169 transparent transparent transparent;"
                + "-fx-border-width:3 0 0 0;");

        Label iconLbl = new Label("🔧");
        iconLbl.setStyle("-fx-font-size:32;");

        Label nameLbl = new Label(eq.getNom());
        nameLbl.getStyleClass().add("eq-card-name");

        String descText = (eq.getDescription() == null || eq.getDescription().isEmpty())
                ? "Aucune description." : eq.getDescription();
        Label descLbl = new Label(descText);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        HBox actions = new HBox(8);
        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("btn-edit");
        editBtn.setOnAction(e -> openEdit(eq));
        Button delBtn = new Button("🗑️");
        delBtn.getStyleClass().add("btn-del");
        delBtn.setOnAction(e -> confirmDelete(eq));
        actions.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(iconLbl, nameLbl, descLbl, actions);
        return card;
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));
        // statCats et statPremium n'ont pas d'équivalent en base — on affiche "—"
        if (statCats    != null) statCats   .setText("—");
        if (statPremium != null) statPremium.setText("—");
    }

    /* ══════════════════════════════════════════
       TOGGLE VUE
       ══════════════════════════════════════════ */
    @FXML
    private void onToggleView() {
        cardViewOn = toggleCards.isSelected();
        cardGrid .setVisible(cardViewOn);
        cardGrid .setManaged(cardViewOn);
        tableCard.setVisible(!cardViewOn);
        tableCard.setManaged(!cardViewOn);
        toggleCards.getStyleClass().remove("vt-btn-active");
        toggleList .getStyleClass().remove("vt-btn-active");
        if (cardViewOn) toggleCards.getStyleClass().add("vt-btn-active");
        else            toggleList .getStyleClass().add("vt-btn-active");
    }

    /* ══════════════════════════════════════════
       FORMULAIRE
       ══════════════════════════════════════════ */
    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("🛎️ Nouvel Équipement");
        nomField.clear();
        descField.clear();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(Equipement eq) {
        editingId = eq.getId();
        formPanelTitle.setText("✏️ Modifier l'Équipement");
        nomField .setText(eq.getNom());
        descField.setText(eq.getDescription());
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        String nom  = nomField.getText().trim();
        String desc = descField.getText().trim();

        if (nom.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Le nom est obligatoire.");
            return;
        }

        try {
            if (editingId != null) {
                Equipement e = new Equipement(editingId, nom, desc);
                service.modifier(e);
            } else {
                Equipement e = new Equipement(0, nom, desc);
                service.ajouter(e);
            }
            onCloseForm();
            loadData();
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage());
        }
    }

    private void confirmDelete(Equipement eq) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer l'équipement ?");
        alert.setHeaderText("🗑️  Supprimer « " + eq.getNom() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/ecotrip.css").toExternalForm());
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.supprimer(eq.getId());
                loadData();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur SQL",
                        "Suppression impossible : " + ex.getMessage());
            }
        });
    }

    /* ══════════════════════════════════════════
       RECHERCHE
       ══════════════════════════════════════════ */
    @FXML private void onSearch() { renderAll(); }

    /* ══════════════════════════════════════════
       NAVIGATION
       ══════════════════════════════════════════ */
    @FXML private void onNavHebergements() { navigateTo("ListHebergements.fxml",      "Hébergements"); }
    @FXML private void onNavChambres()     { navigateTo("Chambres.fxml",              "Chambres"); }
    @FXML private void onNavCategories()   { navigateTo("CategoriesHebergement.fxml", "Catégories"); }
    @FXML private void onLogout()          { System.exit(0); }

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
                getClass().getResource("/css/ecotrip.css").toExternalForm());
        a.showAndWait();
    }
}