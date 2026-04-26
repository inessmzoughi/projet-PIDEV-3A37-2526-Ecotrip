package tn.esprit.controller.back.hebergement;

import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import javafx.concurrent.Worker;
import tn.esprit.models.hebergements.Categorie_hebergement;
import tn.esprit.models.hebergements.Equipement;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
    @FXML private WebView          mapView;
    @FXML private TextField prixField;

    /* ─── Table ─── */
    @FXML private TextField              searchField;
    @FXML private ComboBox<String>       sortCombo;
    @FXML private TableView<Hebergement> tableView;
    @FXML private TableColumn<Hebergement, String>  colNom, colVille, colLabelEco;
    @FXML private TableColumn<Hebergement, Integer> colNbEtoiles, colActif;
    @FXML private TableColumn<Hebergement, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;

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
    private final GeminiService geminiService = new GeminiService();

    /* ─── Pont Java ↔ JavaScript ─── */
    public class JavaConnector {
        public void onMapClick(double lat, double lng) {
            javafx.application.Platform.runLater(() -> {
                latitudeField.setText(String.valueOf(lat).replace(",", "."));
                longitudeField.setText(String.valueOf(lng).replace(",", "."));
                validateLatitude();
                validateLongitude();
            });
        }
    }
    private final JavaConnector javaConnector = new JavaConnector();

    /* ─── Initialize ─── */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…","Nom (A→Z)","Étoiles (croissant)","Ville (A→Z)"));
        sortCombo.getSelectionModel().selectFirst();
        actifCombo.setItems(FXCollections.observableArrayList("Actif","Inactif"));
        actifCombo.getSelectionModel().selectFirst();
        loadPropietaires();
        loadCategories();
        loadEquipements();
        setupColumns();
        loadData();
        refreshAll();
        initMap();
    }

    /* ─── Carte ─── */
    private void initMap() {
        WebEngine engine = mapView.getEngine();

        String css = getClass().getResource("/leaflet/leaflet.css").toExternalForm();
        String js  = getClass().getResource("/leaflet/leaflet.js").toExternalForm();

        String html = "<!DOCTYPE html><html><head>"
                + "<meta charset='utf-8'/>"
                + "<link rel='stylesheet' href='" + css + "'/>"
                + "<script src='" + js + "'></script>"
                + "<style>*{margin:0;padding:0;box-sizing:border-box;}"
                + "html,body,#map{width:100%;height:100vh;}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map').setView([33.8869,9.5375],6);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',"
                + "{attribution:'© OpenStreetMap'}).addTo(map);"
                + "var marker=null;"
                + "function setMarker(lat,lng,nom,ville){"
                + "  if(marker)map.removeLayer(marker);"
                + "  marker=L.marker([lat,lng]).addTo(map)"
                + "    .bindPopup('<b>'+nom+'</b><br>'+ville).openPopup();"
                + "  map.setView([lat,lng],13);}"
                + "map.on('click',function(e){"
                + "  if(window.javaConnector)"
                + "    window.javaConnector.onMapClick(e.latlng.lat,e.latlng.lng);});"
                + "setTimeout(function(){ map.invalidateSize(); }, 300);"
                + "setInterval(function(){ map.invalidateSize(); }, 1000);"
                + "</script></body></html>";

        engine.loadContent(html);

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaConnector", javaConnector);
            }
        });
    }

    private void updateMapMarker() {
        try {
            double lat = Double.parseDouble(latitudeField.getText().trim());
            double lng = Double.parseDouble(longitudeField.getText().trim());
            String nom   = nomField.getText().trim().replace("'", "\\'");
            String ville = villeField.getText().trim().replace("'", "\\'");
            mapView.getEngine().executeScript(
                    "setMarker(" + lat + "," + lng + ",'" + nom + "','" + ville + "')");
        } catch (NumberFormatException ignored) {}
    }
    private void showMapPopup(Hebergement h) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.DECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(submitBtn.getScene().getWindow());
        popup.setTitle(h.getNom() + " — " + h.getVille());

        WebView miniMap = new WebView();
        miniMap.setPrefSize(500, 380);

        String css = getClass().getResource("/leaflet/leaflet.css").toExternalForm();
        String js  = getClass().getResource("/leaflet/leaflet.js").toExternalForm();

        double lat = h.getLatitude();
        double lng = h.getLongitude();
        String nom   = h.getNom().replace("'", "\\'");
        String ville = h.getVille().replace("'", "\\'");

        String html = "<!DOCTYPE html><html><head>"
                + "<meta charset='utf-8'/>"
                + "<link rel='stylesheet' href='" + css + "'/>"
                + "<script src='" + js + "'></script>"
                + "<style>*{margin:0;padding:0;box-sizing:border-box;}"
                + "html,body,#map{width:100%;height:100%;}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map').setView([" + lat + "," + lng + "],14);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',"
                + "{attribution:'OpenStreetMap'}).addTo(map);"
                + "L.marker([" + lat + "," + lng + "]).addTo(map)"
                + ".bindPopup('<b>" + nom + "</b><br>" + ville + "').openPopup();"
                + "setTimeout(function(){ map.invalidateSize(); }, 300);"
                + "setInterval(function(){ map.invalidateSize(); }, 1000);"
                + "</script></body></html>";

        miniMap.getEngine().loadContent(html);

        // Infos sous la carte
        Label lblNom   = new Label(h.getNom());
        lblNom.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label lblVille = new Label("📍 " + h.getVille() + "  |  " + h.getNb_etoiles() + " étoiles");
        lblVille.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        Label lblCoords = new Label("Lat: " + lat + "  —  Lng: " + lng);
        lblCoords.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8; -fx-font-style:italic;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle(
                "-fx-background-color:#0ea5e9; -fx-text-fill:white; -fx-font-weight:bold;" +
                        "-fx-background-radius:8; -fx-padding:8 32 8 32; -fx-cursor:hand;" +
                        "-fx-font-size:13px; -fx-border-width:0;");
        closeBtn.setOnAction(e -> popup.close());

        VBox info = new VBox(4, lblNom, lblVille, lblCoords);
        info.setPadding(new Insets(12, 16, 4, 16));

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 16, 16, 16));

        VBox root = new VBox(miniMap, info, footer);
        root.setStyle("-fx-background-color:white;");

        popup.setScene(new Scene(root));
        popup.setResizable(false);

        popup.setOnShown(e -> {
            Stage owner = (Stage) submitBtn.getScene().getWindow();
            popup.setX(owner.getX() + (owner.getWidth()  - popup.getWidth())  / 2);
            popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
        });

        popup.showAndWait();
    }

    /* ─── Chargement propriétaires ─── */
    private void loadPropietaires() {
        try {
            propietaireMap.clear();
            propietaireMap.putAll(service.getPropietairesMap());
            propietaireCombo.setItems(
                    FXCollections.observableArrayList(propietaireMap.keySet()));
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

    /* ─── Chargement équipements ─── */
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

    /* ─── Validations ─── */
    @FXML private void validateNom()      { setFieldError(nomField,      errNom,      nomField.getText().trim().isEmpty()); }
    @FXML private void validateVille()    { setFieldError(villeField,    errVille,    villeField.getText().trim().isEmpty()); }
    @FXML private void validateAdresse()  { setFieldError(adresseField,  errAdresse,  adresseField.getText().trim().isEmpty()); }
    @FXML private void validateLabelEco() { setFieldError(labelEcoField, errLabelEco, labelEcoField.getText().trim().isEmpty()); }
    @FXML private void validateImage()    { setFieldError(imagePrincipaleField, errImage, imagePrincipaleField.getText().trim().isEmpty()); }

    @FXML private void validateNbEtoiles() {
        setFieldError(nbEtoilesField, errNbEtoiles,
                !nbEtoilesField.getText().trim().matches("[1-5]"));
    }

    @FXML private void updateCounter() {
        charCount.setText(descriptionField.getText().length() + " / 500 caractères");
    }

    @FXML private void validateLatitude() {
        try {
            double lat = Double.parseDouble(latitudeField.getText().trim());
            setFieldError(latitudeField, errLatitude, lat < 30.2 || lat > 37.5);
            if (lat >= 30.2 && lat <= 37.5) updateMapMarker();
        } catch (NumberFormatException e) {
            setFieldError(latitudeField, errLatitude, true);
        }
    }

    @FXML private void validateLongitude() {
        try {
            double lng = Double.parseDouble(longitudeField.getText().trim());
            setFieldError(longitudeField, errLongitude, lng < 7.5 || lng > 11.6);
            if (lng >= 7.5 && lng <= 11.6) updateMapMarker();
        } catch (NumberFormatException e) {
            setFieldError(longitudeField, errLongitude, true);
        }
    }

    @FXML private void validateCategorie() {
        boolean error = categorieCombo.getValue() == null
                || categorieCombo.getValue().isEmpty();
        errCategorie.setVisible(error);
        errCategorie.setManaged(error);
        if (!error) categorieCombo.getStyleClass().remove("form-input-error");
        else if (!categorieCombo.getStyleClass().contains("form-input-error"))
            categorieCombo.getStyleClass().add("form-input-error");
    }

    @FXML private void validatePropietaire() {
        boolean error = propietaireCombo.getValue() == null
                || propietaireCombo.getValue().equals("— Aucun —");
        errPropietaire.setVisible(error);
        errPropietaire.setManaged(error);
        if (!error) propietaireCombo.getStyleClass().remove("form-input-error");
        else if (!propietaireCombo.getStyleClass().contains("form-input-error"))
            propietaireCombo.getStyleClass().add("form-input-error");
    }

    @FXML private void validateActif() {}

    /* ─── Validation globale ─── */
    private boolean validateAll() {
        boolean ok = true;
        if (nomField.getText().trim().isEmpty()) {
            setFieldError(nomField, errNom, true); ok = false;
        } else setFieldError(nomField, errNom, false);
        if (villeField.getText().trim().isEmpty()) {
            setFieldError(villeField, errVille, true); ok = false;
        } else setFieldError(villeField, errVille, false);
        if (adresseField.getText().trim().isEmpty()) {
            setFieldError(adresseField, errAdresse, true); ok = false;
        } else setFieldError(adresseField, errAdresse, false);
        if (!nbEtoilesField.getText().trim().matches("[1-5]")) {
            setFieldError(nbEtoilesField, errNbEtoiles, true); ok = false;
        } else setFieldError(nbEtoilesField, errNbEtoiles, false);
        if (categorieCombo.getValue() == null || categorieCombo.getValue().isEmpty()) {
            errCategorie.setVisible(true); errCategorie.setManaged(true);
            if (!categorieCombo.getStyleClass().contains("form-input-error"))
                categorieCombo.getStyleClass().add("form-input-error");
            ok = false;
        } else {
            errCategorie.setVisible(false); errCategorie.setManaged(false);
            categorieCombo.getStyleClass().remove("form-input-error");
        }
        if (labelEcoField.getText().trim().isEmpty()) {
            setFieldError(labelEcoField, errLabelEco, true); ok = false;
        } else setFieldError(labelEcoField, errLabelEco, false);
        if (imagePrincipaleField.getText().trim().isEmpty()) {
            setFieldError(imagePrincipaleField, errImage, true); ok = false;
        } else setFieldError(imagePrincipaleField, errImage, false);
        if (propietaireCombo.getValue() == null
                || propietaireCombo.getValue().equals("— Aucun —")) {
            errPropietaire.setVisible(true); errPropietaire.setManaged(true);
            if (!propietaireCombo.getStyleClass().contains("form-input-error"))
                propietaireCombo.getStyleClass().add("form-input-error");
            ok = false;
        } else {
            errPropietaire.setVisible(false); errPropietaire.setManaged(false);
            propietaireCombo.getStyleClass().remove("form-input-error");
        }
        if (descriptionField.getText().trim().isEmpty()) {
            if (!descriptionField.getStyleClass().contains("form-input-error"))
                descriptionField.getStyleClass().add("form-input-error");
            ok = false;
        } else descriptionField.getStyleClass().remove("form-input-error");
        try {
            double lat = Double.parseDouble(latitudeField.getText().trim());
            if (lat < 30.2 || lat > 37.5) {
                setFieldError(latitudeField, errLatitude, true); ok = false;
            } else setFieldError(latitudeField, errLatitude, false);
        } catch (NumberFormatException e) {
            setFieldError(latitudeField, errLatitude, true); ok = false;
        }
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
        try { lat = Double.parseDouble(latitudeField.getText().trim()); } catch (NumberFormatException ignored) {}
        try { lng = Double.parseDouble(longitudeField.getText().trim()); } catch (NumberFormatException ignored) {}
        int categorieId   = categorieMap.getOrDefault(categorieCombo.getValue(), 1);
        int propietaireId = propietaireMap.getOrDefault(
                propietaireCombo.getValue() != null ? propietaireCombo.getValue() : "— Aucun —", 0);
        int actif = "Actif".equals(actifCombo.getValue()) ? 1 : 0;
        Hebergement candidat = new Hebergement(
                hebergementEnEdition != null ? hebergementEnEdition.getId() : 0,
                nomField.getText().trim(), descriptionField.getText().trim(),
                adresseField.getText().trim(), villeField.getText().trim(),
                Integer.parseInt(nbEtoilesField.getText().trim()),
                imagePrincipaleField.getText().trim(), labelEcoField.getText().trim(),
                lat, lng, actif, categorieId, propietaireId);
        try {
            if (service.existsExact(candidat)) {
                showSuccessPopup("Un hébergement identique existe déjà !", "⚠️");
                return;
            }
        } catch (SQLException e) { showAlert("Erreur SQL", e.getMessage()); return; }
        List<Integer> selectedEqIds = equipementCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Integer) cb.getUserData())
                .collect(Collectors.toList());
        try {
            int hebergementId;
            if (hebergementEnEdition == null) {
                hebergementId = service.ajouterAvecId(new Hebergement(0,
                        nomField.getText().trim(), descriptionField.getText().trim(),
                        adresseField.getText().trim(), villeField.getText().trim(),
                        Integer.parseInt(nbEtoilesField.getText().trim()),
                        imagePrincipaleField.getText().trim(), labelEcoField.getText().trim(),
                        lat, lng, actif, categorieId, propietaireId));
                showSuccessPopup("Hébergement ajouté avec succès !", "✅");
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
                showSuccessPopup("Hébergement modifié avec succès !", "💾");
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
        setFieldError(nomField,      errNom,      false);
        setFieldError(villeField,    errVille,    false);
        setFieldError(adresseField,  errAdresse,  false);
        setFieldError(nbEtoilesField,errNbEtoiles,false);
        setFieldError(labelEcoField, errLabelEco, false);
        setFieldError(imagePrincipaleField, errImage, false);
        setFieldError(latitudeField, errLatitude, false);
        setFieldError(longitudeField,errLongitude,false);
        errCategorie.setVisible(false);   errCategorie.setManaged(false);
        categorieCombo.getStyleClass().remove("form-input-error");
        errPropietaire.setVisible(false); errPropietaire.setManaged(false);
        propietaireCombo.getStyleClass().remove("form-input-error");
        descriptionField.getStyleClass().remove("form-input-error");
        charCount.setText("0 / 500 caractères");
        formIcon.setText("🏨");
        formTitle.setText("Nouvel Hébergement");
        formSubtitle.setText("Remplissez les informations ci-dessous.");
        submitBtn.setText("➕ Ajouter");
        equipementCheckboxes.forEach(cb -> cb.setSelected(false));
        prixField.clear();   // ← ajouter dans onReset()
    }
    /* ─── Suggestion IA (Gemini) ─── */
    @FXML
    /* ─── Helper commun : vérifie les 4 champs requis ─── */
    private boolean checkFieldsForIA() {
        String nom        = nomField.getText().trim();
        String ville      = villeField.getText().trim();
        String etoilesStr = nbEtoilesField.getText().trim();
        String categorie  = categorieCombo.getValue();

        if (nom.isEmpty() || ville.isEmpty()
                || !etoilesStr.matches("[1-5]") || categorie == null) {
            showSuccessPopup(
                    "Remplissez d'abord :\nNom · Ville · Étoiles · Catégorie", "⚠️");
            return false;
        }
        return true;
    }

    /* ─── Bouton Description : ✨ Suggérer ─── */
    @FXML
    private void onSuggestDescription() {
        if (!checkFieldsForIA()) return;

        String nom      = nomField.getText().trim();
        String ville    = villeField.getText().trim();
        int    etoiles  = Integer.parseInt(nbEtoilesField.getText().trim());
        String categorie = categorieCombo.getValue();

        descriptionField.setPromptText("⏳ Génération en cours…");
        descriptionField.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                String desc = geminiService.suggestDescription(
                        nom, ville, etoiles, categorie);
                javafx.application.Platform.runLater(() -> {
                    descriptionField.setText(desc);
                    descriptionField.setDisable(false);
                    descriptionField.setPromptText("Décrivez l'hébergement…");
                    updateCounter();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    descriptionField.setDisable(false);
                    descriptionField.setPromptText("Décrivez l'hébergement…");
                    handleGeminiError(e);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /* ─── Bouton Prix : ✨ Suggérer ─── */
    @FXML
    private void onSuggestPrix() {
        if (!checkFieldsForIA()) return;

        String nom       = nomField.getText().trim();
        String ville     = villeField.getText().trim();
        int    etoiles   = Integer.parseInt(nbEtoilesField.getText().trim());
        String categorie = categorieCombo.getValue();

        prixField.setPromptText("⏳ Calcul…");
        prixField.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                int prix = geminiService.suggestPrix(
                        nom, ville, etoiles, categorie);
                javafx.application.Platform.runLater(() -> {
                    prixField.setText(String.valueOf(prix));
                    prixField.setDisable(false);
                    prixField.setPromptText("Ex : 250");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    prixField.setDisable(false);
                    prixField.setPromptText("Ex : 250");
                    handleGeminiError(e);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /* ─── Gestion erreurs Gemini lisibles ─── */
    private void handleGeminiError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
        if (msg.contains("RESOURCE_EXHAUSTED")) {
            showSuccessPopup("Quota Gemini dépassé.\nAttendez 1 minute et réessayez.", "⚠️");
        } else if (msg.contains("404")) {
            showSuccessPopup("Modèle Gemini introuvable.", "⚠️");
        } else if (msg.contains("API_KEY")) {
            showSuccessPopup("Clé API Gemini invalide.", "⚠️");
        } else {
            showAlert("Erreur Gemini", msg);
        }
    }
    /* ─── Charger pour édition ─── */
    private void chargerPourEdition(Hebergement h) {
        hebergementEnEdition = h;
        nomField.setText(h.getNom());
        villeField.setText(h.getVille());
        adresseField.setText(h.getAdresse()                != null ? h.getAdresse()            : "");
        labelEcoField.setText(h.getLabel_eco()             != null ? h.getLabel_eco()          : "");
        imagePrincipaleField.setText(h.getImage_principale() != null ? h.getImage_principale() : "");
        nbEtoilesField.setText(String.valueOf(h.getNb_etoiles()));
        latitudeField.setText(String.valueOf(h.getLatitude()));
        longitudeField.setText(String.valueOf(h.getLongitude()));
        descriptionField.setText(h.getDescription()        != null ? h.getDescription()        : "");
        categorieMap.forEach((nom, id)   -> { if (id == h.getCategorie_id())   categorieCombo.setValue(nom); });
        propietaireMap.forEach((nom, id) -> { if (id == h.getPropietaire_id()) propietaireCombo.setValue(nom); });
        if (h.getPropietaire_id() == 0) propietaireCombo.setValue("— Aucun —");
        actifCombo.setValue(h.getActif() == 1 ? "Actif" : "Inactif");
        categorieCombo.getStyleClass().remove("form-input-error");
        errCategorie.setVisible(false); errCategorie.setManaged(false);
        propietaireCombo.getStyleClass().remove("form-input-error");
        errPropietaire.setVisible(false); errPropietaire.setManaged(false);
        updateCounter();
        formIcon.setText("✏️");
        formTitle.setText("Modifier l'Hébergement");
        formSubtitle.setText("Mettez à jour les informations.");
        submitBtn.setText("💾 Enregistrer");
        nomField.requestFocus();
        updateMapMarker();
        try {
            List<Equipement> existing    = hebergementEqService.getEquipementsByHebergement(h.getId());
            List<Integer>    existingIds = existing.stream().map(Equipement::getId).toList();
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
        try {
            statTotal.setText(String.valueOf(service.countTotal()));
            statEtoiles.setText(String.format("%.1f ⭐", service.avgEtoiles()));
            statActif.setText(service.countActifs() + " actifs");
        } catch (SQLException e) {
            statTotal.setText("—"); statEtoiles.setText("—"); statActif.setText("—");
        }
    }

    private void renderTable() {
        String query = searchField.getText().toLowerCase().trim();
        String sort  = sortCombo.getValue();
        List<Hebergement> filtered = allData.stream()
                .filter(h -> query.isEmpty()
                        || h.getNom().toLowerCase().contains(query)
                        || h.getVille().toLowerCase().contains(query))
                .collect(Collectors.toList());
        if ("Nom (A→Z)".equals(sort))
            filtered.sort(Comparator.comparing(Hebergement::getNom));
        else if ("Étoiles (croissant)".equals(sort))
            filtered.sort(Comparator.comparingInt(Hebergement::getNb_etoiles));
        else if ("Ville (A→Z)".equals(sort))
            filtered.sort(Comparator.comparing(Hebergement::getVille));
        badgeCount.setText(String.valueOf(filtered.size()));
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        if (currentPage > totalPages) currentPage = 1;
        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);
        tableView.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
        pagInfo.setText(total == 0 ? ""
                : "Affichage " + (from + 1) + "–" + to + " sur " + total);
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
                Label chip = new Label("📍 " + item);
                chip.getStyleClass().add("td-city");
                setGraphic(chip); setText(null);
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
            private final Button mapBtn  = new Button("📍");
            private final Button editBtn = new Button("✏ Modifier");
            private final Button delBtn  = new Button("🗑 Supprimer");
            private final HBox   box     = new HBox(5, mapBtn, editBtn, delBtn);
            {
                // Style bouton carte — cercle bleu
                mapBtn.setStyle(
                        "-fx-background-color:#0ea5e9; -fx-text-fill:white;" +
                                "-fx-background-radius:50%; -fx-min-width:28px; -fx-min-height:28px;" +
                                "-fx-max-width:28px; -fx-max-height:28px;" +
                                "-fx-font-size:12px; -fx-cursor:hand; -fx-padding:0;");

                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(0, 0, 0, 2));

                mapBtn.setOnAction(e -> showMapPopup(
                        getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> chargerPourEdition(
                        getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> confirmDelete(
                        getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
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
                if (hebergementEnEdition != null
                        && hebergementEnEdition.getId() == h.getId()) onReset();
                refreshAll();
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    /* ─── Navigation ─── */
    @FXML private void onSearch()       { currentPage = 1; renderTable(); }
    @FXML private void onSort()         { currentPage = 1; renderTable(); }
    @FXML private void onNavDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    /* ─── Helpers ─── */
    private void setFieldError(TextField field, Label errLabel, boolean hasError) {
        if (hasError) {
            if (!field.getStyleClass().contains("form-input-error"))
                field.getStyleClass().add("form-input-error");
            errLabel.setVisible(true); errLabel.setManaged(true);
        } else {
            field.getStyleClass().remove("form-input-error");
            errLabel.setVisible(false); errLabel.setManaged(false);
        }
    }

    private void showSuccessPopup(String message, String iconText) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(submitBtn.getScene().getWindow());
        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size:40px;");
        Label msg = new Label(message);
        msg.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        msg.setWrapText(true);
        msg.setMaxWidth(260);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        String btnColor = iconText.equals("⚠️") ? "#d97706" : "#38a169";
        Button closeBtn = new Button("OK");
        closeBtn.setStyle(
                "-fx-background-color:" + btnColor + "; -fx-text-fill:white;"
                        + "-fx-font-weight:bold; -fx-background-radius:8;"
                        + "-fx-padding:9 48 9 48; -fx-cursor:hand;"
                        + "-fx-border-width:0; -fx-font-size:13px;");
        closeBtn.setOnAction(e -> popup.close());
        VBox box = new VBox(14, icon, msg, closeBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32, 36, 28, 36));
        box.setPrefWidth(320);
        box.setStyle(
                "-fx-background-color:white; -fx-background-radius:14;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),20,0,0,5);"
                        + "-fx-border-color:#e2e8f0; -fx-border-radius:14; -fx-border-width:1;");
        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.setOnShown(e -> {
            Stage owner = (Stage) submitBtn.getScene().getWindow();
            popup.setX(owner.getX() + (owner.getWidth()  - popup.getWidth())  / 2);
            popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
        });
        popup.showAndWait();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}