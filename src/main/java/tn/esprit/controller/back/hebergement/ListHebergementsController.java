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
    @FXML private Label            errAdresse, errLabelEco, errImage, errLatitude, errLongitude;
    @FXML private Button           submitBtn;
    @FXML private FlowPane         equipementsCheckboxPane;
    @FXML private Label            errPropietaire;

    /* ─── Table ─── */
    @FXML private TextField              searchField;
    @FXML private ComboBox<String>       sortCombo;
    @FXML private TableView<Hebergement> tableView;
    @FXML private TableColumn<Hebergement, String>  colNom, colVille, colLabelEco;
    @FXML private TableColumn<Hebergement, Integer> colNbEtoiles, colActif;
    @FXML private TableColumn<Hebergement, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;
    @FXML private HBox  paginationBar;

    /* ─── State ─── */
    private final Hebergement_service           service              = new Hebergement_service();
    private final CategorieH_service            categorieService     = new CategorieH_service();
    private final Equipement_service            equipementService    = new Equipement_service();
    private final HebergementEquipement_service hebergementEqService = new HebergementEquipement_service();
    private List<Hebergement> allData;
    private List<Equipement>  allEquipements       = new ArrayList<>();
    private final List<CheckBox> equipementCheckboxes = new ArrayList<>();
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

    /* ─── Validation en temps réel ─── */
    @FXML private void validateNom()      { setFieldError(nomField,      errNom,      nomField.getText().trim().isEmpty()); }
    @FXML private void validateVille()    { setFieldError(villeField,    errVille,    villeField.getText().trim().isEmpty()); }
    @FXML private void validateAdresse()  { setFieldError(adresseField,  errAdresse,  adresseField.getText().trim().isEmpty()); }
    @FXML private void validateLabelEco() { setFieldError(labelEcoField, errLabelEco, labelEcoField.getText().trim().isEmpty()); }
    @FXML private void validateImage()    { setFieldError(imagePrincipaleField, errImage, imagePrincipaleField.getText().trim().isEmpty()); }

    @FXML private void validateNbEtoiles() {
        setFieldError(nbEtoilesField, errNbEtoiles, !nbEtoilesField.getText().trim().matches("[1-5]"));
    }

    @FXML private void updateCounter() {
        charCount.setText(descriptionField.getText().length() + " / 500 caractères");
    }

    @FXML private void validateLatitude() {
        try {
            double lat = Double.parseDouble(latitudeField.getText().trim());
            // Tunisie : latitude entre 30.2 et 37.5
            setFieldError(latitudeField, errLatitude, lat < 30.2 || lat > 37.5);
        } catch (NumberFormatException e) {
            setFieldError(latitudeField, errLatitude, true);
        }
    }

    @FXML private void validateLongitude() {
        try {
            double lng = Double.parseDouble(longitudeField.getText().trim());
            // Tunisie : longitude entre 7.5 et 11.6
            setFieldError(longitudeField, errLongitude, lng < 7.5 || lng > 11.6);
        } catch (NumberFormatException e) {
            setFieldError(longitudeField, errLongitude, true);
        }
    }

    /* ─── Validation globale ─── */
    private boolean validateAll() {
        boolean ok = true;

        // Nom
        if (nomField.getText().trim().isEmpty()) {
            setFieldError(nomField, errNom, true); ok = false;
        } else setFieldError(nomField, errNom, false);

        // Ville
        if (villeField.getText().trim().isEmpty()) {
            setFieldError(villeField, errVille, true); ok = false;
        } else setFieldError(villeField, errVille, false);

        // Adresse
        if (adresseField.getText().trim().isEmpty()) {
            setFieldError(adresseField, errAdresse, true); ok = false;
        } else setFieldError(adresseField, errAdresse, false);

        // Nb Étoiles
        if (!nbEtoilesField.getText().trim().matches("[1-5]")) {
            setFieldError(nbEtoilesField, errNbEtoiles, true); ok = false;
        } else setFieldError(nbEtoilesField, errNbEtoiles, false);

        // Catégorie
        if (categorieCombo.getValue() == null || categorieCombo.getValue().isEmpty()) {
            errCategorie.setVisible(true); errCategorie.setManaged(true); ok = false;
        } else { errCategorie.setVisible(false); errCategorie.setManaged(false); }

        // Label Éco
        if (labelEcoField.getText().trim().isEmpty()) {
            setFieldError(labelEcoField, errLabelEco, true); ok = false;
        } else setFieldError(labelEcoField, errLabelEco, false);

        // Image principale
        if (imagePrincipaleField.getText().trim().isEmpty()) {
            setFieldError(imagePrincipaleField, errImage, true); ok = false;
        } else setFieldError(imagePrincipaleField, errImage, false);

        // Propriétaire
        if (propietaireCombo.getValue() == null || propietaireCombo.getValue().equals("— Aucun —")) {
            errPropietaire.setVisible(true); errPropietaire.setManaged(true); ok = false;
        } else { errPropietaire.setVisible(false); errPropietaire.setManaged(false); }

        // Description
        if (descriptionField.getText().trim().isEmpty()) {
            if (!descriptionField.getStyleClass().contains("form-input-error"))
                descriptionField.getStyleClass().add("form-input-error");
            ok = false;
        } else {
            descriptionField.getStyleClass().remove("form-input-error");
        }

        // Latitude (Tunisie : 30.2 ~ 37.5)
        try {
            double lat = Double.parseDouble(latitudeField.getText().trim());
            if (lat < 30.2 || lat > 37.5) {
                setFieldError(latitudeField, errLatitude, true); ok = false;
            } else setFieldError(latitudeField, errLatitude, false);
        } catch (NumberFormatException e) {
            setFieldError(latitudeField, errLatitude, true); ok = false;
        }

        // Longitude (Tunisie : 7.5 ~ 11.6)
        try {
            double lng = Double.parseDouble(longitudeField.getText().trim());
            if (lng < 7.5 || lng > 11.6) {
                setFieldError(longitudeField, errLongitude, true); ok = false;
            } else setFieldError(longitudeField, errLongitude, false);
        } catch (NumberFormatException e) {
            setFieldError(longitudeField, errLongitude, true); ok = false;
        }

        return ok;
    }

    /* ─── Submit ─── */
    @FXML
    private void onSubmit() {
        if (!validateAll()) return;

        double lat = 0.0, lng = 0.0;
        try { lat = Double.parseDouble(latitudeField.getText().trim());  } catch (NumberFormatException ignored) {}
        try { lng = Double.parseDouble(longitudeField.getText().trim()); } catch (NumberFormatException ignored) {}

        int categorieId   = categorieMap.getOrDefault(categorieCombo.getValue(), 1);
        int propietaireId = propietaireMap.getOrDefault(
                propietaireCombo.getValue() != null ? propietaireCombo.getValue() : "— Aucun —", 0);
        int actif = "Actif".equals(actifCombo.getValue()) ? 1 : 0;

        List<Integer> selectedEqIds = equipementCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Integer) cb.getUserData())
                .collect(Collectors.toList());

        try {
            int hebergementId;
            if (hebergementEnEdition == null) {
                hebergementId = service.ajouter(new Hebergement(0,
                        nomField.getText().trim(),
                        descriptionField.getText().trim(),
                        adresseField.getText().trim(),
                        villeField.getText().trim(),
                        Integer.parseInt(nbEtoilesField.getText().trim()),
                        imagePrincipaleField.getText().trim(),
                        labelEcoField.getText().trim(),
                        lat, lng, actif, categorieId, propietaireId));
                showToast("✅ Hébergement ajouté !");
            } else {
                hebergementEnEdition.setNom(nomField.getText().trim());
                hebergementEnEdition.setVille(villeField.getText().trim());
                hebergementEnEdition.setAdresse(adresseField.getText().trim());
                hebergementEnEdition.setNb_etoiles(Integer.parseInt(nbEtoilesField.getText().trim()));
                hebergementEnEdition.setCategorie_id(categorieId);
                hebergementEnEdition.setLabel_eco(labelEcoField.getText().trim());
                hebergementEnEdition.setImage_principale(imagePrincipaleField.getText().trim());
                hebergementEnEdition.setLatitude(lat);
                hebergementEnEdition.setLongitude(lng);
                hebergementEnEdition.setActif(actif);
                hebergementEnEdition.setDescription(descriptionField.getText().trim());
                hebergementEnEdition.setPropietaire_id(propietaireId);
                service.modifier(hebergementEnEdition);
                hebergementId = hebergementEnEdition.getId();
                showToast("💾 Hébergement modifié !");
            }
            hebergementEqService.sauvegarder(hebergementId, selectedEqIds);
            onReset();
            refreshAll();
        } catch (SQLException e) { showAlert("Erreur SQL", e.getMessage()); }
    }

    /* ─── Reset ─── */
    @FXML
    private void onReset() {
        hebergementEnEdition = null;
        nomField.clear(); villeField.clear(); adresseField.clear();
        nbEtoilesField.clear(); descriptionField.clear();
        labelEcoField.clear(); imagePrincipaleField.clear();
        latitudeField.clear(); longitudeField.clear();
        categorieCombo.setValue(null);
        propietaireCombo.getSelectionModel().selectFirst();
        actifCombo.getSelectionModel().selectFirst();

        // Reset erreurs
        setFieldError(nomField,      errNom,      false);
        setFieldError(villeField,    errVille,    false);
        setFieldError(adresseField,  errAdresse,  false);
        setFieldError(nbEtoilesField,errNbEtoiles,false);
        setFieldError(labelEcoField, errLabelEco, false);
        setFieldError(imagePrincipaleField, errImage, false);
        setFieldError(latitudeField, errLatitude, false);
        setFieldError(longitudeField,errLongitude,false);
        errCategorie.setVisible(false);   errCategorie.setManaged(false);
        errPropietaire.setVisible(false); errPropietaire.setManaged(false);
        descriptionField.getStyleClass().remove("form-input-error");

        charCount.setText("0 / 500 caractères");
        formIcon.setText("🏨");
        formTitle.setText("Nouvel Hébergement");
        formSubtitle.setText("Remplissez les informations ci-dessous.");
        submitBtn.setText("➕ Ajouter");
        equipementCheckboxes.forEach(cb -> cb.setSelected(false));
    }

    /* ─── Charger pour édition ─── */
    private void chargerPourEdition(Hebergement h) {
        hebergementEnEdition = h;
        nomField.setText(h.getNom());
        villeField.setText(h.getVille());
        adresseField.setText(h.getAdresse()           != null ? h.getAdresse()           : "");
        labelEcoField.setText(h.getLabel_eco()         != null ? h.getLabel_eco()         : "");
        imagePrincipaleField.setText(h.getImage_principale() != null ? h.getImage_principale() : "");
        nbEtoilesField.setText(String.valueOf(h.getNb_etoiles()));
        latitudeField.setText(String.valueOf(h.getLatitude()));
        longitudeField.setText(String.valueOf(h.getLongitude()));
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
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colNbEtoiles.setCellValueFactory(new PropertyValueFactory<>("nb_etoiles"));
        colLabelEco.setCellValueFactory(new PropertyValueFactory<>("label_eco"));
        colActif.setCellValueFactory(new PropertyValueFactory<>("actif"));
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