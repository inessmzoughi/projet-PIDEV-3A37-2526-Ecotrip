package tn.esprit.controller.back.hebergement;

import tn.esprit.models.Categorie_hebergement;
import tn.esprit.models.Equipement;
import tn.esprit.models.Hebergement;
import tn.esprit.database.Base;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.CategorieH_service;
import tn.esprit.services.hebergement.Equipement_service;
import tn.esprit.services.hebergement.HebergementEquipement_service;
import tn.esprit.services.hebergement.Hebergement_service;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ListHebergementsController implements Initializable {

    /* ─── Stats ─── */
    @FXML private Label statTotal, statEtoiles, statActif;

    /* ─── Formulaire ─── */
    @FXML private Label            formIcon, formTitle, formSubtitle;
    @FXML private TextField        nomField, villeField, adresseField, nbEtoilesField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private TextField        labelEcoField, imagePrincipaleField, latitudeField, longitudeField;
    @FXML private ComboBox<String> propietaireCombo, actifCombo;
    @FXML private TextArea         descriptionField;
    @FXML private Label            errNom, errVille, errNbEtoiles, errCategorie, charCount;
    @FXML private Button           submitBtn;
    @FXML private FlowPane         equipementsCheckboxPane;
    @FXML private Label errPropietaire;


    /* ─── Table ─── */
    @FXML private TextField              searchField;
    @FXML private ComboBox<String>       sortCombo;
    @FXML private TableView<Hebergement> tableView;
    @FXML private TableColumn<Hebergement, Integer> colIndex;
    @FXML private TableColumn<Hebergement, String>  colNom, colVille, colLabelEco;
    @FXML private TableColumn<Hebergement, Integer> colNbEtoiles, colActif;
    @FXML private TableColumn<Hebergement, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;
    @FXML
    private HBox paginationBar;

    /* ─── State ─── */
    private final Hebergement_service            service                    = new Hebergement_service();
    private final CategorieH_service             categorieService           = new CategorieH_service();
    private final Equipement_service             equipementService          = new Equipement_service();
    private final HebergementEquipement_service  hebergementEqService       = new HebergementEquipement_service();
    private List<Hebergement> allData;
    private List<Equipement>  allEquipements                                = new ArrayList<>();
    private final List<CheckBox> equipementCheckboxes                       = new ArrayList<>();
    private Hebergement hebergementEnEdition = null;
    private static final int PER_PAGE = 6;
    private int currentPage = 1;
    private final Map<String, Integer> propietaireMap = new LinkedHashMap<>();
    private final Map<String, Integer> categorieMap   = new LinkedHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.setItems(FXCollections.observableArrayList("Trier par…","Nom (A→Z)","Étoiles (croissant)","Ville (A→Z)"));
        sortCombo.getSelectionModel().selectFirst();
        actifCombo.setItems(FXCollections.observableArrayList("Actif","Inactif"));
        actifCombo.getSelectionModel().selectFirst();
        loadPropietaires();
        loadCategories();
        loadEquipements();
        setupColumns();
        loadData();
        refreshAll();
    }

    /* ─── Chargement propriétaires ─── */
    private void loadPropietaires() {
        try {
            java.sql.Connection conn = Base.getInstance().getConnection();
            java.sql.ResultSet rs = conn.createStatement().executeQuery("SELECT id, username FROM user");
            propietaireMap.clear();
            List<String> noms = new ArrayList<>();
            noms.add("— Aucun —");
            propietaireMap.put("— Aucun —", 0);
            while (rs.next()) {
                String u = rs.getString("username");
                propietaireMap.put(u, rs.getInt("id"));
                noms.add(u);
            }
            propietaireCombo.setItems(FXCollections.observableArrayList(noms));
            propietaireCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Chargement catégories ─── */
    private void loadCategories() {
        try {
            List<Categorie_hebergement> cats = categorieService.getAll();
            categorieMap.clear();
            List<String> noms = new ArrayList<>();
            for (Categorie_hebergement c : cats) {
                categorieMap.put(c.getNom(), c.getId());
                noms.add(c.getNom());
            }
            categorieCombo.setItems(FXCollections.observableArrayList(noms));
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Chargement équipements (checkboxes) ─── */
    private void loadEquipements() {
        try {
            allEquipements = equipementService.getAll();
            equipementsCheckboxPane.getChildren().clear();
            equipementCheckboxes.clear();
            for (Equipement eq : allEquipements) {
                CheckBox cb = new CheckBox(eq.getNom());
                cb.setUserData(eq.getId());
                cb.getStyleClass().add("eq-checkbox");
                equipementCheckboxes.add(cb);
                equipementsCheckboxPane.getChildren().add(cb);
            }
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Validation ─── */
    @FXML private void validateNom()       { setFieldError(nomField, errNom, nomField.getText().trim().isEmpty()); }
    @FXML private void validateVille()     { setFieldError(villeField, errVille, villeField.getText().trim().isEmpty()); }
    @FXML private void validateNbEtoiles() { setFieldError(nbEtoilesField, errNbEtoiles, !nbEtoilesField.getText().trim().matches("[1-5]")); }
    @FXML private void updateCounter()     { charCount.setText(descriptionField.getText().length() + " / 500 caractères"); }

    private boolean validateAll() {
        boolean ok = true;
        if (nomField.getText().trim().isEmpty())               { setFieldError(nomField, errNom, true);             ok = false; } else setFieldError(nomField, errNom, false);
        if (villeField.getText().trim().isEmpty())             { setFieldError(villeField, errVille, true);         ok = false; } else setFieldError(villeField, errVille, false);
        if (!nbEtoilesField.getText().trim().matches("[1-5]")) { setFieldError(nbEtoilesField, errNbEtoiles, true); ok = false; } else setFieldError(nbEtoilesField, errNbEtoiles, false);
        if (categorieCombo.getValue() == null || categorieCombo.getValue().isEmpty()) {
            errCategorie.setVisible(true); errCategorie.setManaged(true); ok = false;
        } else { errCategorie.setVisible(false); errCategorie.setManaged(false); }
        return ok;
    }

    /* ─── Submit ─── */
    @FXML
    private void onSubmit() {
        if (!validateAll()) return;
        String adresse    = adresseField          != null ? adresseField.getText().trim()          : "";
        String labelEco   = labelEcoField          != null ? labelEcoField.getText().trim()          : "";
        String imagePrinc = imagePrincipaleField   != null ? imagePrincipaleField.getText().trim()   : "";
        double lat = 0.0, lng = 0.0;
        try { lat = Double.parseDouble(latitudeField.getText().trim());  } catch (NumberFormatException ignored) {}
        try { lng = Double.parseDouble(longitudeField.getText().trim()); } catch (NumberFormatException ignored) {}
        int categorieId   = categorieMap.getOrDefault(categorieCombo.getValue(), 1);
        int propietaireId = propietaireMap.getOrDefault(propietaireCombo.getValue() != null ? propietaireCombo.getValue() : "— Aucun —", 0);
        int actif         = "Actif".equals(actifCombo.getValue()) ? 1 : 0;

        // IDs des équipements cochés
        List<Integer> selectedEqIds = equipementCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Integer) cb.getUserData())
                .collect(Collectors.toList());

        try {
            int hebergementId;
            if (hebergementEnEdition == null) {
                hebergementId = service.ajouter(new Hebergement(0,
                        nomField.getText().trim(), descriptionField.getText().trim(),
                        adresse, villeField.getText().trim(),
                        Integer.parseInt(nbEtoilesField.getText().trim()),
                        imagePrinc, labelEco, lat, lng, actif, categorieId, propietaireId));
                showToast("✅ Hébergement ajouté !");
            } else {
                hebergementEnEdition.setNom(nomField.getText().trim());
                hebergementEnEdition.setVille(villeField.getText().trim());
                hebergementEnEdition.setAdresse(adresse);
                hebergementEnEdition.setNb_etoiles(Integer.parseInt(nbEtoilesField.getText().trim()));
                hebergementEnEdition.setCategorie_id(categorieId);
                hebergementEnEdition.setLabel_eco(labelEco);
                hebergementEnEdition.setImage_principale(imagePrinc);
                hebergementEnEdition.setLatitude(lat);
                hebergementEnEdition.setLongitude(lng);
                hebergementEnEdition.setActif(actif);
                hebergementEnEdition.setDescription(descriptionField.getText().trim());
                hebergementEnEdition.setPropietaire_id(propietaireId);
                service.modifier(hebergementEnEdition);
                hebergementId = hebergementEnEdition.getId();
                showToast("💾 Hébergement modifié !");
            }
            // Sauvegarder les équipements
            hebergementEqService.sauvegarder(hebergementId, selectedEqIds);
            onReset();
            refreshAll();
        } catch (SQLException e) { showAlert("Erreur SQL", e.getMessage()); }
    }

    /* ─── Reset ─── */
    @FXML
    private void onReset() {
        hebergementEnEdition = null;
        nomField.clear(); villeField.clear(); nbEtoilesField.clear(); descriptionField.clear();
        if (adresseField          != null) adresseField.clear();
        if (labelEcoField          != null) labelEcoField.clear();
        if (imagePrincipaleField   != null) imagePrincipaleField.clear();
        if (latitudeField          != null) latitudeField.clear();
        if (longitudeField         != null) longitudeField.clear();
        categorieCombo.setValue(null);
        propietaireCombo.getSelectionModel().selectFirst();
        actifCombo.getSelectionModel().selectFirst();
        setFieldError(nomField, errNom, false);
        setFieldError(villeField, errVille, false);
        setFieldError(nbEtoilesField, errNbEtoiles, false);
        errCategorie.setVisible(false); errCategorie.setManaged(false);
        charCount.setText("0 / 500 caractères");
        formIcon.setText("🏨");
        formTitle.setText("Nouvel Hébergement");
        formSubtitle.setText("Remplissez les informations ci-dessous.");
        submitBtn.setText("➕ Ajouter");
        // Décocher tous les équipements
        equipementCheckboxes.forEach(cb -> cb.setSelected(false));
    }

    /* ─── Charger pour édition ─── */
    private void chargerPourEdition(Hebergement h) {
        hebergementEnEdition = h;
        nomField.setText(h.getNom());
        villeField.setText(h.getVille());
        nbEtoilesField.setText(String.valueOf(h.getNb_etoiles()));
        if (adresseField          != null) adresseField.setText(h.getAdresse()          != null ? h.getAdresse()          : "");
        if (labelEcoField          != null) labelEcoField.setText(h.getLabel_eco()        != null ? h.getLabel_eco()        : "");
        if (imagePrincipaleField   != null) imagePrincipaleField.setText(h.getImage_principale() != null ? h.getImage_principale() : "");
        if (latitudeField          != null) latitudeField.setText(String.valueOf(h.getLatitude()));
        if (longitudeField         != null) longitudeField.setText(String.valueOf(h.getLongitude()));
        descriptionField.setText(h.getDescription() != null ? h.getDescription() : "");
        categorieMap.forEach((nom, id)   -> { if (id == h.getCategorie_id())   categorieCombo.setValue(nom); });
        propietaireMap.forEach((nom, id) -> { if (id == h.getPropietaire_id()) propietaireCombo.setValue(nom); });
        if (h.getPropietaire_id() == 0) propietaireCombo.setValue("— Aucun —");
        actifCombo.setValue(h.getActif() == 1 ? "Actif" : "Inactif");
        updateCounter();
        formIcon.setText("✏️");
        formTitle.setText("Modifier l'Hébergement");
        formSubtitle.setText("Mettez à jour les informations.");
        submitBtn.setText("💾 Enregistrer");
        nomField.requestFocus();

        // Cocher les équipements existants
        try {
            List<Equipement> existing    = hebergementEqService.getEquipementsByHebergement(h.getId());
            List<Integer>    existingIds = existing.stream().map(Equipement::getId).collect(Collectors.toList());
            equipementCheckboxes.forEach(cb -> cb.setSelected(existingIds.contains((Integer) cb.getUserData())));
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Data ─── */
    private void loadData() {
        try { allData = service.getAll(); }
        catch (SQLException e) { allData = new ArrayList<>(); showAlert("Erreur", e.getMessage()); }
    }

    private void refreshAll() { loadData(); updateStats(); renderTable(); }

    private void updateStats() {
        int total = allData.size();
        statTotal.setText(String.valueOf(total));
        if (total == 0) { statEtoiles.setText("—"); statActif.setText("—"); return; }
        statEtoiles.setText(String.format("%.1f ⭐", allData.stream().mapToInt(Hebergement::getNb_etoiles).average().orElse(0)));
        statActif.setText(allData.stream().filter(h -> h.getActif() == 1).count() + " actifs");
    }

    private void renderTable() {
        String query = searchField.getText().toLowerCase().trim();
        String sort  = sortCombo.getValue();
        List<Hebergement> filtered = allData.stream()
                .filter(h -> query.isEmpty() || h.getNom().toLowerCase().contains(query) || h.getVille().toLowerCase().contains(query))
                .collect(Collectors.toList());
        if ("Nom (A→Z)".equals(sort))                filtered.sort(Comparator.comparing(Hebergement::getNom));
        else if ("Étoiles (croissant)".equals(sort)) filtered.sort(Comparator.comparingInt(Hebergement::getNb_etoiles));
        else if ("Ville (A→Z)".equals(sort))         filtered.sort(Comparator.comparing(Hebergement::getVille));
        badgeCount.setText(String.valueOf(filtered.size()));
        int total = filtered.size(), totalPages = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        if (currentPage > totalPages) currentPage = 1;
        int from = (currentPage - 1) * PER_PAGE, to = Math.min(from + PER_PAGE, total);
        tableView.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
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

    private void setupColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colNbEtoiles.setCellValueFactory(new PropertyValueFactory<>("nb_etoiles"));
        colLabelEco.setCellValueFactory(new PropertyValueFactory<>("label_eco"));
        colActif.setCellValueFactory(new PropertyValueFactory<>("actif"));
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                setText(String.valueOf(getTableView().getItems().indexOf(getTableRow().getItem()) + 1 + (currentPage - 1) * PER_PAGE));
                getStyleClass().add("td-index");
            }
        });
        colVille.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label chip = new Label("📍 " + item); chip.getStyleClass().add("td-city"); setGraphic(chip); setText(null);
            }
        });
        colNbEtoiles.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "⭐".repeat(Math.max(0, item)));
            }
        });
        colActif.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item == 1 ? "✅ Actif" : "❌ Inactif");
                badge.getStyleClass().add(item == 1 ? "badge-actif" : "badge-inactif");
                setGraphic(badge); setText(null);
            }
        });
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button delBtn  = new Button("🗑️ Supprimer");
            private final HBox   box     = new HBox(8, editBtn, delBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> chargerPourEdition(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e  -> confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : box); }
        });
    }

    private void confirmDelete(Hebergement h) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer « " + h.getNom() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            try {
                hebergementEqService.supprimerParHebergement(h.getId());
                service.supprimer(h.getId());
                if (hebergementEnEdition != null && hebergementEnEdition.getId() == h.getId()) onReset();
                refreshAll();
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    /* ─── Navigation ─── */
    @FXML private void onSearch()          { currentPage = 1; renderTable(); }
    @FXML private void onSort()            { currentPage = 1; renderTable(); }
    @FXML private void onNavHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void onNavChambres()     { SceneManager.navigateTo(Routes.ADMIN_CHAMBRES); }
    @FXML private void onNavEquipements()  { SceneManager.navigateTo(Routes.ADMIN_EQUIPEMENTS); }
    @FXML private void onNavCategories()   { SceneManager.navigateTo(Routes.ADMIN_CATEGORIES_HEBERGEMENT); }
    @FXML private void onNavDashboard()    { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    /* ─── Helpers ─── */
    private void setFieldError(TextField field, Label errLabel, boolean hasError) {
        if (hasError) {
            if (!field.getStyleClass().contains("form-input-error")) field.getStyleClass().add("form-input-error");
            errLabel.setVisible(true); errLabel.setManaged(true);
        } else {
            field.getStyleClass().remove("form-input-error");
            errLabel.setVisible(false); errLabel.setManaged(false);
        }
    }

    private void showToast(String msg) {
        pagInfo.setText(msg);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(this::renderTable);
        }).start();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}