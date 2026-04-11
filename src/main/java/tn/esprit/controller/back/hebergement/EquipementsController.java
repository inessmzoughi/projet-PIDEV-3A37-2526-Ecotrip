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
    @FXML private Label statTotal;
    @FXML private Label statAvecDesc;      // remplace statCats
    @FXML private Label statHebLies;       // remplace statPremium

    /* ─── Tabs ─── */
    @FXML private HBox catTabsPane;

    /* ─── Formulaire ─── */
    @FXML private VBox      formPanel;
    @FXML private Label     formPanelTitle;
    @FXML private TextField nomField;
    @FXML private TextArea  descField;
    @FXML private Label     errNom;

    /* ─── Barre de recherche ─── */
    @FXML private TextField searchField;

    /* ─── Toggle vue ─── */
    @FXML private ToggleButton toggleCards, toggleList;

    /* ─── Vues ─── */
    @FXML private FlowPane cardGrid;
    @FXML private VBox     tableCard;

    /* ─── Table ─── */
    @FXML private TableView<Equipement>            tableView;
    @FXML private TableColumn<Equipement, String>  colNom, colDesc;
    @FXML private TableColumn<Equipement, Void>    colActions;

    @FXML private Label badgeCount;

    /* ─── Pagination ─── */
    @FXML private Button            btnPrevPage;
    @FXML private Button            btnNextPage;
    @FXML private Label             lblPageInfo;
    @FXML private ComboBox<Integer> pageSizeCombo;

    private int currentPage = 0;
    private int pageSize    = 10;
    private List<Equipement> filteredData = new ArrayList<>();

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
        // Vue liste active par défaut
        cardGrid .setVisible(false);
        cardGrid .setManaged(false);
        tableCard.setVisible(true);
        tableCard.setManaged(true);

        // Pagination
        pageSizeCombo.setItems(FXCollections.observableArrayList(5, 10, 20, 50));
        pageSizeCombo.setValue(pageSize);
        pageSizeCombo.setOnAction(e -> {
            pageSize = pageSizeCombo.getValue();
            currentPage = 0;
            applyPage();
        });

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 0;
            renderAll();
        });

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
                    "Impossible de lire les \u00e9quipements : " + e.getMessage());
            allData = List.of();
        }
        currentPage = 0;
        renderAll();
        updateStats();
    }

    /* ══════════════════════════════════════════
       COLONNES — sans colonne ID
       ══════════════════════════════════════════ */
    private void setupColumns() {
        colNom .setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Colonne description avec texte tronqué proprement
        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText("\u2014");
                    setStyle("-fx-text-fill: #aaa;");
                } else {
                    setText(item.length() > 80 ? item.substring(0, 77) + "\u2026" : item);
                    setStyle("");
                }
            }
        });

        // Actions — boutons complets, non tronqués
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("\u270F\uFE0F Modifier");
            private final Button delBtn  = new Button("\uD83D\uDDD1\uFE0F Supprimer");
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
       RENDU + PAGINATION
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

        // Grille de cartes
        cardGrid.getChildren().clear();
        for (Equipement eq : filteredData) {
            cardGrid.getChildren().add(buildEqCard(eq));
        }

        applyPage();
    }

    private void applyPage() {
        int total      = filteredData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)           currentPage = 0;

        int from = currentPage * pageSize;
        int to   = Math.min(from + pageSize, total);

        tableView.setItems(FXCollections.observableArrayList(filteredData.subList(from, to)));

        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + totalPages
                + "  (" + total + " r\u00e9sultats)");
        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);
    }

    @FXML private void onPrevPage() {
        if (currentPage > 0) { currentPage--; applyPage(); }
    }

    @FXML private void onNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / pageSize));
        if (currentPage < totalPages - 1) { currentPage++; applyPage(); }
    }

    /* ══════════════════════════════════════════
       STATS RÉELLES
       ══════════════════════════════════════════ */
    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));

        try {
            int avecDesc = service.countAvecDescription();
            statAvecDesc.setText(String.valueOf(avecDesc));
        } catch (SQLException e) {
            statAvecDesc.setText("\u2014");
        }

        try {
            int hebLies = service.countHebergementsLies();
            statHebLies.setText(String.valueOf(hebLies));
        } catch (SQLException e) {
            statHebLies.setText("\u2014");
        }
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

        Label iconLbl = new Label("\uD83D\uDD27");
        iconLbl.setStyle("-fx-font-size:32;");

        Label nameLbl = new Label(eq.getNom());
        nameLbl.getStyleClass().add("eq-card-name");

        String descText = (eq.getDescription() == null || eq.getDescription().isBlank())
                ? "Aucune description." : eq.getDescription();
        Label descLbl = new Label(descText);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        Button editBtn = new Button("\u270F\uFE0F Modifier");
        editBtn.getStyleClass().add("btn-edit");
        editBtn.setOnAction(e -> openEdit(eq));

        Button delBtn = new Button("\uD83D\uDDD1\uFE0F Supprimer");
        delBtn.getStyleClass().add("btn-del");
        delBtn.setOnAction(e -> confirmDelete(eq));

        HBox actions = new HBox(8, editBtn, delBtn);
        card.getChildren().addAll(iconLbl, nameLbl, descLbl, actions);
        return card;
    }

    /* ══════════════════════════════════════════
       TOGGLE VUE
       ══════════════════════════════════════════ */
    @FXML
    private void onToggleCards() {
        cardViewOn = true;
        cardGrid .setVisible(true);
        cardGrid .setManaged(true);
        tableCard.setVisible(false);
        tableCard.setManaged(false);
        toggleCards.getStyleClass().add("vt-btn-active");
        toggleList .getStyleClass().remove("vt-btn-active");
    }

    @FXML
    private void onToggleList() {
        cardViewOn = false;
        cardGrid .setVisible(false);
        cardGrid .setManaged(false);
        tableCard.setVisible(true);
        tableCard.setManaged(true);
        toggleList .getStyleClass().add("vt-btn-active");
        toggleCards.getStyleClass().remove("vt-btn-active");
    }

    /* ══════════════════════════════════════════
       FORMULAIRE
       ══════════════════════════════════════════ */
    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("\uD83D\uDECE\uFE0F Nouvel \u00c9quipement");
        nomField.clear();
        descField.clear();
        errNom.setVisible(false);
        errNom.setManaged(false);
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
        formPanelTitle.setText("\u270F\uFE0F Modifier l\u2019\u00c9quipement");
        nomField .setText(eq.getNom());
        descField.setText(eq.getDescription() != null ? eq.getDescription() : "");
        errNom.setVisible(false);
        errNom.setManaged(false);
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        String nom  = nomField.getText().trim();
        String desc = descField.getText().trim();

        if (nom.isEmpty()) {
            errNom.setText("Le nom est obligatoire.");
            errNom.setVisible(true);
            errNom.setManaged(true);
            nomField.getStyleClass().add("form-input-error");
            return;
        }
        errNom.setVisible(false);
        errNom.setManaged(false);
        nomField.getStyleClass().remove("form-input-error");

        try {
            if (editingId != null) {
                service.modifier(new Equipement(editingId, nom, desc));
            } else {
                service.ajouter(new Equipement(0, nom, desc));
            }
            onCloseForm();
            loadData();
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage());
        }
    }

    private void confirmDelete(Equipement eq) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer l'\u00e9quipement ?");
        alert.setHeaderText("\uD83D\uDDD1\uFE0F  Supprimer \u00ab " + eq.getNom() + " \u00bb ?");
        alert.setContentText("Cette action est irr\u00e9versible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
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
       RECHERCHE (bouton fallback)
       ══════════════════════════════════════════ */
    @FXML private void onSearch() {
        currentPage = 0;
        renderAll();
    }

    /* ══════════════════════════════════════════
       NAVIGATION
       ══════════════════════════════════════════ */
    @FXML private void onNavHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void onNavChambres()     { navigateTo("Chambres.fxml",              "Chambres"); }
    @FXML private void onNavCategories()   { navigateTo("CategoriesHebergement.fxml", "Cat\u00e9gories"); }
    @FXML private void onLogout()          { System.exit(0); }
    @FXML private void onNavDashboard()    { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    private void navigateTo(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/" + fxml));
            Stage  stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle("EcoTrip Admin \u2014 " + title);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    /* ══════════════════════════════════════════
       HELPERS
       ══════════════════════════════════════════ */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
}