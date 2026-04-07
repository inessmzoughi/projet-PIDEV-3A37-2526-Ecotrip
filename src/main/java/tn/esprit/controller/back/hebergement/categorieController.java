package tn.esprit.controller.back.hebergement;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import tn.esprit.models.Categorie_hebergement;
import tn.esprit.services.hebergement.CategorieH_service;

import java.sql.SQLException;

public class categorieController {

    @FXML
    private TableColumn<?, ?> ColID;

    @FXML
    private TableColumn<?, ?> ColNom;

    @FXML
    private TableColumn<?, ?> Coldescription;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnModifier;

    @FXML
    private Button btnSupprimer;

    @FXML
    private AnchorPane categorie_hContainer;

    @FXML
    private TextField tId;

    @FXML
    private TextField tName;

    @FXML
    private TableView<Categorie_hebergement> tableView;

    @FXML
    private TextField tdescription;

    @FXML
    void initialize() throws SQLException {
        tId.setVisible(false);
        chargerTableView();
        configurerTableViewClick();
    }

    private final CategorieH_service categorieService = new CategorieH_service();

    private void chargerTableView() throws SQLException {
        ObservableList<Categorie_hebergement> list = FXCollections.observableList(categorieService.getAll());
        tableView.setItems(list);
        ColID.setCellValueFactory(new PropertyValueFactory<>("id"));
        ColNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        Coldescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void configurerTableViewClick() {
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Categorie_hebergement selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    tId.setText(String.valueOf(selected.getId()));
                    tName.setText(selected.getNom());
                    tdescription.setText(selected.getDescription());
                }
            }
        });
    }

    @FXML
    void AjouterCategorie(ActionEvent event) {
        if (!verifierSaisie()) return;

        Categorie_hebergement c = new Categorie_hebergement(0, tName.getText(), tdescription.getText());
        try {
            categorieService.ajouter(c);
            afficherInfo("Catégorie ajoutée avec succès !");
            clearFields();
            chargerTableView();
        } catch (SQLException e) {
            afficherErreur("Erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    void ModifierCategorie(ActionEvent event) {
        if (tId.getText().isEmpty()) {
            afficherErreur("Veuillez sélectionner une catégorie à modifier.");
            return;
        }
        if (!verifierSaisie()) return;

        Categorie_hebergement c = new Categorie_hebergement(
                Integer.parseInt(tId.getText()), tName.getText(), tdescription.getText()
        );
        try {
            categorieService.modifier(c);
            afficherInfo("Catégorie modifiée avec succès !");
            clearFields();
            chargerTableView();
        } catch (SQLException e) {
            afficherErreur("Erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    void SupprimerCategorie(ActionEvent event) {
        if (tId.getText().isEmpty()) {
            afficherErreur("Veuillez sélectionner une catégorie à supprimer.");
            return;
        }
        try {
            categorieService.supprimer(Integer.parseInt(tId.getText()));
            afficherInfo("Catégorie supprimée avec succès !");
            clearFields();
            chargerTableView();
        } catch (SQLException e) {
            afficherErreur("Erreur SQL : " + e.getMessage());
        }
    }

    private boolean verifierSaisie() {
        if (tName.getText().isEmpty() || tdescription.getText().isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return false;
        }
        if (!tName.getText().matches("[a-zA-ZÀ-ÿ ]+")) {
            afficherErreur("Le nom ne peut contenir que des lettres.");
            return false;
        }
        return true;
    }

    private void afficherInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void afficherErreur(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }

    private void clearFields() {
        tId.clear();
        tName.clear();
        tdescription.clear();
    }
}
