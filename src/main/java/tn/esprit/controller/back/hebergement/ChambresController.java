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
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ChambresController implements Initializable {

    @FXML private Label statTotal, statPrix, statCap, statHeb;
    @FXML private VBox  formPanel;
    @FXML private Label formPanelTitle;
    @FXML private TextField             numeroField, prixField, capField;
    @FXML private TextArea              descField;
    @FXML private ComboBox<String>      typeCombo, dispoCombo, filterType, sortCombo;
    @FXML private ComboBox<Hebergement> hebCombo;
    @FXML private Label errNumero, errHeb, errPrix, errCap;
    @FXML private TextField        searchField;
    @FXML private TableView<Chambre>            tableView;
    @FXML private TableColumn<Chambre, String>  colNumero, colHeb, colType, colDesc;
    @FXML private TableColumn<Chambre, Double>  colPrix;
    @FXML private TableColumn<Chambre, Integer> colCap, colDispo;
    @FXML private TableColumn<Chambre, Void>    colActions;
    @FXML private Label badgeCount;

    /* ─── Pagination (même style ListHebergements) ─── */
    @FXML private Label pagInfo;
    @FXML private HBox  pagButtons;

    private static final int PER_PAGE = 6;
    private int currentPage = 1;
    private List<Chambre> filteredData = new ArrayList<>();

    private final Chambre_service     service    = new Chambre_service();
    private final Hebergement_service hebService = new Hebergement_service();
    private List<Chambre>     allData;
    private List<Hebergement> allHeb;
    private Integer editingId = null;

    private static final String[] TYPES = {"Simple", "Double", "Suite", "Familiale"};
    private static final Map<String, String> TYPE_STYLE = Map.of(
            "Simple", "chip-gray", "Double", "chip-blue",
            "Suite",  "chip-purple", "Familiale", "chip-green");

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

    private void loadHebergements() {
        try { allHeb = hebService.getAll(); } catch (SQLException e) { allHeb = List.of(); }
        hebCombo.setItems(FXCollections.observableArrayList(allHeb));
        hebCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Hebergement h)   { return h == null ? "" : h.getNom(); }
            @Override public Hebergement fromString(String s) { return null; }
        });
    }

    private void loadData() {
        try { allData = service.getAll(); }
        catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); allData = List.of(); }
        currentPage = 1;
        renderTable();
        updateStats();
    }

    /* ── Validation temps réel ── */
    @FXML private void validateNumero() {
        String v = numeroField.getText().trim();
        if (v.isEmpty()) setFieldError(numeroField, errNumero, "Le numéro est requis.");
        else if (!v.matches("\\d+")) setFieldError(numeroField, errNumero, "Doit être un entier (ex: 101).");
        else clearFieldError(numeroField, errNumero);
    }
    @FXML private void validatePrix() {
        try {
            double p = Double.parseDouble(prixField.getText().trim());
            if (p <= 0) setFieldError(prixField, errPrix, "Le prix doit être strictement positif.");
            else clearFieldError(prixField, errPrix);
        } catch (NumberFormatException e) { setFieldError(prixField, errPrix, "Nombre valide requis."); }
    }
    @FXML private void validateCap() {
        try {
            int c = Integer.parseInt(capField.getText().trim());
            if (c < 1 || c > 4) setFieldError(capField, errCap, "La capacité doit être entre 1 et 4.");
            else clearFieldError(capField, errCap);
        } catch (NumberFormatException e) { setFieldError(capField, errCap, "Entier entre 1 et 4 requis."); }
    }

    private boolean validateAll() {
        boolean ok = true;
        String numero = numeroField.getText().trim();
        if (numero.isEmpty())            { setFieldError(numeroField, errNumero, "Le numéro est requis."); ok = false; }
        else if (!numero.matches("\\d+")){ setFieldError(numeroField, errNumero, "Doit être un entier (ex: 101)."); ok = false; }
        else clearFieldError(numeroField, errNumero);

        if (hebCombo.getValue() == null) { setComboError(hebCombo, errHeb, "Sélectionner un hébergement."); ok = false; }
        else clearComboError(hebCombo, errHeb);

        try {
            double p = Double.parseDouble(prixField.getText().trim());
            if (p <= 0) { setFieldError(prixField, errPrix, "Le prix doit être strictement positif."); ok = false; }
            else clearFieldError(prixField, errPrix);
        } catch (NumberFormatException e) { setFieldError(prixField, errPrix, "Nombre valide requis."); ok = false; }

        try {
            int c = Integer.parseInt(capField.getText().trim());
            if (c < 1 || c > 4) { setFieldError(capField, errCap, "La capacité doit être entre 1 et 4."); ok = false; }
            else clearFieldError(capField, errCap);
        } catch (NumberFormatException e) { setFieldError(capField, errCap, "Entier entre 1 et 4 requis."); ok = false; }

        return ok;
    }

    @FXML private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("🛏️ Nouvelle Chambre");
        numeroField.clear(); prixField.clear(); capField.clear(); descField.clear();
        typeCombo.getSelectionModel().select("Double");
        dispoCombo.getSelectionModel().selectFirst();
        hebCombo.getSelectionModel().clearSelection();
        resetAllErrors();
        formPanel.setVisible(true); formPanel.setManaged(true);
    }

    @FXML private void onCloseForm() {
        formPanel.setVisible(false); formPanel.setManaged(false);
        editingId = null; resetAllErrors();
    }

    private void openEdit(Chambre c) {
        editingId = c.getId();
        formPanelTitle.setText("✏️ Modifier la Chambre");
        numeroField.setText(c.getNumero());
        prixField.setText(String.valueOf((int) c.getPrix_par_nuit()));
        capField.setText(String.valueOf(c.getCapacite()));
        descField.setText(c.getDescription());
        typeCombo.setValue(c.getType());
        dispoCombo.setValue(c.getDisponible() == 1 ? "Disponible" : "Indisponible");
        allHeb.stream().filter(h -> h.getId() == c.getHebergement_id()).findFirst()
                .ifPresent(h -> hebCombo.setValue(h));
        resetAllErrors();
        formPanel.setVisible(true); formPanel.setManaged(true);
    }

    @FXML private void onSave() {
        if (!validateAll()) return;
        try {
            int    hebId  = hebCombo.getValue().getId();
            double prix   = Double.parseDouble(prixField.getText().trim());
            int    cap    = Integer.parseInt(capField.getText().trim());
            int    dispo  = "Disponible".equals(dispoCombo.getValue()) ? 1 : 0;
            String desc   = descField.getText().trim();
            String type   = typeCombo.getValue();
            String numero = numeroField.getText().trim();
            if (editingId != null)
                service.modifier(new Chambre(editingId, hebId, dispo, desc, prix, cap, type, numero));
            else
                service.ajouter(new Chambre(0, hebId, dispo, desc, prix, cap, type, numero));
            onCloseForm(); loadData();
        } catch (SQLException ex) { showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage()); }
    }

    private void confirmDelete(Chambre c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer la chambre « " + c.getNumero() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try { service.supprimer(c.getId()); loadData(); }
            catch (SQLException ex) { showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage()); }
        });
    }

    private void setupColumns() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colPrix  .setCellValueFactory(new PropertyValueFactory<>("prix_par_nuit"));
        colCap   .setCellValueFactory(new PropertyValueFactory<>("capacite"));
        colType  .setCellValueFactory(new PropertyValueFactory<>("type"));
        colDesc  .setCellValueFactory(new PropertyValueFactory<>("description"));

        colHeb.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                if (e || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                Chambre c = (Chambre) getTableRow().getItem();
                setText("📍 " + allHeb.stream().filter(h -> h.getId() == c.getHebergement_id())
                        .map(Hebergement::getNom).findFirst().orElse("—"));
            }
        });
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                chip.getStyleClass().addAll("chip", TYPE_STYLE.getOrDefault(item, "chip-gray"));
                setGraphic(chip); setText(null);
            }
        });
        colPrix.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                setText(e || v == null ? null : (int) Math.round(v) + " DT");
            }
        });
        colCap.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean e) {
                super.updateItem(v, e);
                setText(e || v == null ? null : v + " pers.");
            }
        });
        colDispo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean e) {
                super.updateItem(v, e);
                if (e || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Chambre c = (Chambre) getTableRow().getItem();
                Label chip = new Label(c.getDisponible() == 1 ? "✅ Disponible" : "❌ Indisponible");
                chip.getStyleClass().add(c.getDisponible() == 1 ? "chip-green" : "chip-gray");
                setGraphic(chip); setText(null);
            }
        });
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button delBtn  = new Button("🗑️");
            private final HBox   box     = new HBox(8, editBtn, delBtn);
            { editBtn.getStyleClass().add("btn-edit"); delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> openEdit(getTableView().getItems().get(getIndex())));
                delBtn .setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(Void i, boolean e) { super.updateItem(i, e); setGraphic(e ? null : box); }
        });
    }

    /* ── Rendu + Pagination style ListHebergements ── */
    private void renderTable() {
        String q    = searchField.getText().toLowerCase().trim();
        String type = filterType.getValue();
        String sort = sortCombo.getValue();

        filteredData = allData.stream()
                .filter(c -> q.isEmpty() || c.getNumero().toLowerCase().contains(q)
                        || hebName(c.getHebergement_id()).toLowerCase().contains(q)
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(q)))
                .filter(c -> type == null || type.isEmpty() || c.getType().equals(type))
                .collect(Collectors.toList());

        if ("Numéro".equals(sort))         filteredData.sort(Comparator.comparing(Chambre::getNumero));
        else if ("Prix (croissant)".equals(sort)) filteredData.sort(Comparator.comparingDouble(Chambre::getPrix_par_nuit));
        else if ("Capacité (croissante)".equals(sort)) filteredData.sort(Comparator.comparingInt(Chambre::getCapacite));

        badgeCount.setText(String.valueOf(filteredData.size()));

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
            b.setOnAction(e -> { currentPage = pn; renderTable(); });
            pagButtons.getChildren().add(b);
        }
    }

    private void updateStats() {
        try {
            statTotal.setText(String.valueOf(service.countTotal()));
            statPrix.setText((int) Math.round(service.avgPrix()) + " DT");
            statCap.setText(String.valueOf((int) Math.round(service.avgCapacite())));
            statHeb.setText(String.valueOf(service.countHebergementsDistincts()));
        } catch (SQLException e) {
            statTotal.setText("—"); statPrix.setText("—");
            statCap.setText("—");   statHeb.setText("—");
        }
    }

    private String hebName(int hebId) {
        return allHeb.stream().filter(h -> h.getId() == hebId).map(Hebergement::getNom).findFirst().orElse("—");
    }

    @FXML private void onSearch() { currentPage = 1; renderTable(); }
    @FXML private void onNavHebergements() { onCloseForm(); SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void onNavDashboard()    { onCloseForm(); SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    private void setFieldError(TextField f, Label l, String msg) {
        if (!f.getStyleClass().contains("form-input-error")) f.getStyleClass().add("form-input-error");
        l.setText(msg); l.setVisible(true); l.setManaged(true);
    }
    private void clearFieldError(TextField f, Label l) {
        f.getStyleClass().remove("form-input-error"); l.setVisible(false); l.setManaged(false);
    }
    private void setComboError(ComboBox<?> c, Label l, String msg) {
        if (!c.getStyleClass().contains("form-input-error")) c.getStyleClass().add("form-input-error");
        l.setText(msg); l.setVisible(true); l.setManaged(true);
    }
    private void clearComboError(ComboBox<?> c, Label l) {
        c.getStyleClass().remove("form-input-error"); l.setVisible(false); l.setManaged(false);
    }
    private void resetAllErrors() {
        clearFieldError(numeroField, errNumero); clearComboError(hebCombo, errHeb);
        clearFieldError(prixField, errPrix);     clearFieldError(capField, errCap);
    }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        new Alert(type, msg, ButtonType.OK) {{ setTitle(title); }}.showAndWait();
    }
}