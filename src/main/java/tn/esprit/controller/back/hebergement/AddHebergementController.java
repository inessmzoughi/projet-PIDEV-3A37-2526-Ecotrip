package tn.esprit.controller.back.hebergement;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.models.Categorie_hebergement;
import tn.esprit.models.Hebergement;
import tn.esprit.database.Base;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.CategorieH_service;
import tn.esprit.services.hebergement.Hebergement_service;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class AddHebergementController implements Initializable {

    @FXML private TextField        nomField;
    @FXML private TextField        villeField;
    @FXML private TextField        adresseField;
    @FXML private TextField        nbEtoilesField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private TextField        labelEcoField;
    @FXML private TextField        imagePrincipaleField;  // ✅ ajouté
    @FXML private TextField        latitudeField;
    @FXML private TextField        longitudeField;
    @FXML private ComboBox<String> propietaireCombo;
    @FXML private ComboBox<String> actifCombo;            // ✅ ajouté
    @FXML private TextArea         descriptionField;

    @FXML private Label errNom;
    @FXML private Label errVille;
    @FXML private Label errNbEtoiles;
    @FXML private Label errCategorie;
    @FXML private Label errPropietaire;
    @FXML private Label charCount;

    private final Hebergement_service service          = new Hebergement_service();
    private final CategorieH_service  categorieService = new CategorieH_service();

    private final Map<String, Integer> categorieMap   = new LinkedHashMap<>();
    private final Map<String, Integer> propietaireMap = new LinkedHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ✅ actif
        actifCombo.setItems(FXCollections.observableArrayList("Actif", "Inactif"));
        actifCombo.getSelectionModel().selectFirst();

        loadCategories();
        loadPropietaires();
        updateCounter();
    }

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
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de charger les catégories : " + e.getMessage()).show();
        }
    }

    private void loadPropietaires() {
        try {
            java.sql.Connection conn = Base.getInstance().getConnection();
            java.sql.ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT id, username FROM user");
            propietaireMap.clear();
            List<String> noms = new ArrayList<>();
            noms.add("— Aucun —");
            propietaireMap.put("— Aucun —", 0);
            while (rs.next()) {
                String username = rs.getString("username");
                propietaireMap.put(username, rs.getInt("id"));
                noms.add(username);
            }
            propietaireCombo.setItems(FXCollections.observableArrayList(noms));
            propietaireCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de charger les propriétaires : " + e.getMessage()).show();
        }
    }

    /* ══════ VALIDATION ══════ */
    @FXML private void validateNom()       { setFieldError(nomField, errNom, nomField.getText().trim().isEmpty()); }
    @FXML private void validateVille()     { setFieldError(villeField, errVille, villeField.getText().trim().isEmpty()); }
    @FXML private void validateNbEtoiles() { setFieldError(nbEtoilesField, errNbEtoiles, !nbEtoilesField.getText().trim().matches("[1-5]")); }
    @FXML private void updateCounter()     { charCount.setText(descriptionField.getText().length() + " / 500 caractères"); }

    private boolean validateAll() {
        boolean ok = true;
        if (nomField.getText().trim().isEmpty()) {
            setFieldError(nomField, errNom, true); ok = false;
        } else setFieldError(nomField, errNom, false);

        if (villeField.getText().trim().isEmpty()) {
            setFieldError(villeField, errVille, true); ok = false;
        } else setFieldError(villeField, errVille, false);

        if (!nbEtoilesField.getText().trim().matches("[1-5]")) {
            setFieldError(nbEtoilesField, errNbEtoiles, true); ok = false;
        } else setFieldError(nbEtoilesField, errNbEtoiles, false);

        if (categorieCombo.getValue() == null || categorieCombo.getValue().isEmpty()) {
            errCategorie.setVisible(true); errCategorie.setManaged(true); ok = false;
        } else { errCategorie.setVisible(false); errCategorie.setManaged(false); }

        return ok;
    }

    /* ══════ SUBMIT ══════ */
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

        Hebergement h = new Hebergement(
                0,
                nomField.getText().trim(),
                descriptionField.getText().trim(),
                adresseField         != null ? adresseField.getText().trim()           : "",
                villeField.getText().trim(),
                Integer.parseInt(nbEtoilesField.getText().trim()),
                imagePrincipaleField != null ? imagePrincipaleField.getText().trim()   : "",
                labelEcoField        != null ? labelEcoField.getText().trim()          : "",
                lat, lng,
                actif,
                categorieId,
                propietaireId
        );

        try {
            service.ajouter(h);
            navigateToList();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur SQL : " + e.getMessage()).show();
        }
    }

    /* ══════ NAVIGATION ══════ */
    @FXML private void onNavHebergements() { navigateToList(); }
    @FXML private void onLogout()          { System.exit(0); }
    @FXML private void onNavDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    private void navigateToList() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/back/hebergement/ListHebergements.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle("EcoTrip Admin — Hébergements");
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    /* ══════ HELPERS ══════ */
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
}