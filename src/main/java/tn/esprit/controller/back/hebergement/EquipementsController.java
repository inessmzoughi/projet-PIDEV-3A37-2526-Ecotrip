package tn.esprit.controller.back.hebergement;

import tn.esprit.models.Equipement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.Equipement_service;
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

public class EquipementsController implements Initializable {

    /* ─── Stats ─── */
    @FXML private Label statTotal, statAvecDesc, statHebLies;

    /* ─── Formulaire ─── */
    @FXML private VBox      formPanel;
    @FXML private Label     formPanelTitle;
    @FXML private TextField nomField;
    @FXML private TextArea  descField;
    @FXML private Label     errNom;

    /* ─── Recherche + toggle ─── */
    @FXML private TextField    searchField;
    @FXML private ToggleButton toggleCards, toggleList;

    /* ─── Vues ─── */
    @FXML private FlowPane cardGrid;
    @FXML private VBox     tableCard;

    /* ─── Table ─── */
    @FXML private TableView<Equipement>           tableView;
    @FXML private TableColumn<Equipement, String> colNom, colDesc;
    @FXML private TableColumn<Equipement, Void>   colActions;
    @FXML private Label badgeCount;

    /* ─── Pagination style ListHebergements ─── */
    @FXML private Label pagInfo;
    @FXML private HBox  pagButtons;

    private static final int PER_PAGE = 6;
    private int currentPage = 1;
    private List<Equipement> filteredData = new ArrayList<>();

    /* ─── Service + état ─── */
    private final Equipement_service service = new Equipement_service();
    private List<Equipement> allData;
    private Integer editingId  = null;

    /* ══════════════════════════════════════════
       INITIALIZE
       ══════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cardGrid .setVisible(false); cardGrid .setManaged(false);
        tableCard.setVisible(true);  tableCard.setManaged(true);

        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 1; renderAll(); });

        setupColumns();
        loadData();
    }

    /* ══════════════════════════════════════════
       CHARGEMENT
       ══════════════════════════════════════════ */
    private void loadData() {
        try { allData = service.getAll(); }
        catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement", e.getMessage());
            allData = List.of();
        }
        currentPage = 1;
        renderAll();
        updateStats();
    }

    /* ══════════════════════════════════════════
       COLONNES
       ══════════════════════════════════════════ */
    private void setupColumns() {
        colNom .setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText("—"); setStyle("-fx-text-fill:#aaa;");
                } else {
                    setText(item.length() > 80 ? item.substring(0, 77) + "…" : item);
                    setStyle("");
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button delBtn  = new Button("🗑️ Supprimer");
            private final HBox   box     = new HBox(8, editBtn, delBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn .getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> openEdit(getTableView().getItems().get(getIndex())));
                delBtn .setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void i, boolean e) {
                super.updateItem(i, e); setGraphic(e ? null : box);
            }
        });
    }

    /* ══════════════════════════════════════════
       RENDU + PAGINATION style ListHebergements
       ══════════════════════════════════════════ */
    private void renderAll() {
        String q = searchField.getText().toLowerCase().trim();

        filteredData = allData.stream()
                .filter(eq -> q.isEmpty()
                        || eq.getNom().toLowerCase().contains(q)
                        || (eq.getDescription() != null
                        && eq.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        badgeCount.setText(String.valueOf(filteredData.size()));

        // Grille de cartes (pas paginée)
        cardGrid.getChildren().clear();
        for (Equipement eq : filteredData) cardGrid.getChildren().add(buildEqCard(eq));

        // Table paginée
        int total      = filteredData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        if (currentPage > totalPages) currentPage = 1;

        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);

        tableView.setItems(FXCollections.observableArrayList(filteredData.subList(from, to)));

        pagInfo.setText(total == 0 ? "" : "Affichage " + (from + 1) + "–" + to + " sur " + total);

        pagButtons.getChildren().clear();
        for (int p = 1; p <= totalPages; p++) {
            final int pn = p;
            Button b = new Button(String.valueOf(p));
            b.getStyleClass().add("page-btn");
            if (p == currentPage) b.getStyleClass().add("page-btn-active");
            b.setOnAction(e -> { currentPage = pn; renderAll(); });
            pagButtons.getChildren().add(b);
        }
    }

    /* ══════════════════════════════════════════
       STATS
       ══════════════════════════════════════════ */
    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));
        try { statAvecDesc.setText(String.valueOf(service.countAvecDescription())); }
        catch (SQLException e) { statAvecDesc.setText("—"); }
        try { statHebLies.setText(String.valueOf(service.countHebergementsLies())); }
        catch (SQLException e) { statHebLies.setText("—"); }
    }

    /* ══════════════════════════════════════════
       CARTE
       ══════════════════════════════════════════ */
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

        String descText = (eq.getDescription() == null || eq.getDescription().isBlank())
                ? "Aucune description." : eq.getDescription();
        Label descLbl = new Label(descText);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("btn-edit");
        editBtn.setOnAction(e -> openEdit(eq));

        Button delBtn = new Button("🗑️ Supprimer");
        delBtn.getStyleClass().add("btn-del");
        delBtn.setOnAction(e -> confirmDelete(eq));

        card.getChildren().addAll(iconLbl, nameLbl, descLbl, new HBox(8, editBtn, delBtn));
        return card;
    }

    /* ══════════════════════════════════════════
       TOGGLE VUE
       ══════════════════════════════════════════ */
    @FXML private void onToggleCards() {
        cardGrid .setVisible(true);  cardGrid .setManaged(true);
        tableCard.setVisible(false); tableCard.setManaged(false);
        toggleCards.getStyleClass().add("vt-btn-active");
        toggleList .getStyleClass().remove("vt-btn-active");
    }

    @FXML private void onToggleList() {
        cardGrid .setVisible(false); cardGrid .setManaged(false);
        tableCard.setVisible(true);  tableCard.setManaged(true);
        toggleList .getStyleClass().add("vt-btn-active");
        toggleCards.getStyleClass().remove("vt-btn-active");
    }

    /* ══════════════════════════════════════════
       FORMULAIRE
       ══════════════════════════════════════════ */
    @FXML private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("🛎️ Nouvel Équipement");
        nomField.clear(); descField.clear();
        errNom.setVisible(false); errNom.setManaged(false);
        formPanel.setVisible(true); formPanel.setManaged(true);
    }

    @FXML private void onCloseForm() {
        formPanel.setVisible(false); formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(Equipement eq) {
        editingId = eq.getId();
        formPanelTitle.setText("✏️ Modifier l'Équipement");
        nomField .setText(eq.getNom());
        descField.setText(eq.getDescription() != null ? eq.getDescription() : "");
        errNom.setVisible(false); errNom.setManaged(false);
        formPanel.setVisible(true); formPanel.setManaged(true);
    }

    @FXML private void onSave() {
        String nom  = nomField.getText().trim();
        String desc = descField.getText().trim();

        if (nom.isEmpty()) {
            errNom.setText("Le nom est obligatoire.");
            errNom.setVisible(true); errNom.setManaged(true);
            if (!nomField.getStyleClass().contains("form-input-error"))
                nomField.getStyleClass().add("form-input-error");
            return;
        }
        errNom.setVisible(false); errNom.setManaged(false);
        nomField.getStyleClass().remove("form-input-error");

        try {
            if (editingId != null) service.modifier(new Equipement(editingId, nom, desc));
            else                   service.ajouter(new Equipement(0, nom, desc));
            onCloseForm();
            loadData();
        } catch (SQLException ex) { showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage()); }
    }

    private void confirmDelete(Equipement eq) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer l'équipement ?");
        alert.setHeaderText("🗑️  Supprimer « " + eq.getNom() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try { service.supprimer(eq.getId()); loadData(); }
            catch (SQLException ex) { showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage()); }
        });
    }

    /* ══════════════════════════════════════════
       NAVIGATION
       ══════════════════════════════════════════ */
    @FXML private void onSearch()          { currentPage = 1; renderAll(); }
    @FXML private void onNavHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void onNavChambres()     { SceneManager.navigateTo(Routes.ADMIN_CHAMBRES); }
    @FXML private void onNavCategories()   { SceneManager.navigateTo(Routes.ADMIN_CATEGORIES_HEBERGEMENT); }
    @FXML private void onLogout()          { System.exit(0); }
    @FXML private void onNavDashboard()    { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        new Alert(type, msg, ButtonType.OK) {{ setTitle(title); }}.showAndWait();
    }
}