package tn.esprit.controller.back.transport;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.models.transport.Chauffeur;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.transport.ChauffeurService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ChauffeursController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statExperience;
    @FXML private Label statNote;
    @FXML private Label formTitle;
    @FXML private Button submitBtn;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField licenseField;
    @FXML private TextField experienceField;
    @FXML private TextField ratingField;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<Chauffeur> tableView;
    @FXML private TableColumn<Chauffeur, String> colNom;
    @FXML private TableColumn<Chauffeur, String> colPhone;
    @FXML private TableColumn<Chauffeur, String> colLicense;
    @FXML private TableColumn<Chauffeur, Integer> colExperience;
    @FXML private TableColumn<Chauffeur, Double> colRating;
    @FXML private TableColumn<Chauffeur, Void> colActions;
    @FXML private Label badgeCount;

    private final ChauffeurService service = new ChauffeurService();
    private List<Chauffeur> allData = new ArrayList<>();
    private Chauffeur editingChauffeur;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sortCombo.setItems(FXCollections.observableArrayList("Trier par...", "Nom (A-Z)", "Experience croissante", "Note croissante"));
        sortCombo.getSelectionModel().selectFirst();
        setupColumns();
        refreshAll();
    }

    private void refreshAll() {
        try {
            allData = service.afficherAll();
        } catch (SQLException e) {
            allData = new ArrayList<>();
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
        updateStats();
        renderTable();
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));
        statExperience.setText(allData.isEmpty() ? "—" : String.format("%.1f ans", allData.stream().mapToInt(Chauffeur::getExperience).average().orElse(0)));
        statNote.setText(allData.isEmpty() ? "—" : String.format("%.1f", allData.stream().mapToDouble(Chauffeur::getRating).average().orElse(0)));
    }

    private void renderTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<Chauffeur> filtered = allData.stream()
                .filter(c -> query.isEmpty()
                        || c.getFullName().toLowerCase().contains(query)
                        || c.getPhone().toLowerCase().contains(query)
                        || c.getLicenseNumber().toLowerCase().contains(query))
                .collect(Collectors.toList());

        String sort = sortCombo.getValue();
        if ("Nom (A-Z)".equals(sort)) filtered.sort(Comparator.comparing(Chauffeur::getFullName, String.CASE_INSENSITIVE_ORDER));
        else if ("Experience croissante".equals(sort)) filtered.sort(Comparator.comparingInt(Chauffeur::getExperience));
        else if ("Note croissante".equals(sort)) filtered.sort(Comparator.comparingDouble(Chauffeur::getRating));

        badgeCount.setText(String.valueOf(filtered.size()));
        tableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void setupColumns() {
        colNom.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getFullName()));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colLicense.setCellValueFactory(new PropertyValueFactory<>("licenseNumber"));
        colExperience.setCellValueFactory(new PropertyValueFactory<>("experience"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, editBtn, delBtn);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                editBtn.setOnAction(e -> loadForEdit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void onSubmit() {
        try {
            Chauffeur chauffeur = editingChauffeur == null ? new Chauffeur() : editingChauffeur;
            chauffeur.setFirstName(firstNameField.getText().trim());
            chauffeur.setLastName(lastNameField.getText().trim());
            chauffeur.setPhone(phoneField.getText().trim());
            chauffeur.setLicenseNumber(licenseField.getText().trim());
            chauffeur.setExperience(Integer.parseInt(experienceField.getText().trim()));
            chauffeur.setRating(Double.parseDouble(ratingField.getText().trim()));

            if (editingChauffeur == null) service.ajouter(chauffeur);
            else service.modifier(chauffeur);

            onReset();
            refreshAll();
        } catch (NumberFormatException e) {
            showAlert("Valeur invalide", "Experience doit etre un entier et note un decimal.", Alert.AlertType.WARNING);
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onReset() {
        editingChauffeur = null;
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        licenseField.clear();
        experienceField.clear();
        ratingField.clear();
        formTitle.setText("Nouveau Chauffeur");
        submitBtn.setText("Ajouter");
    }

    private void loadForEdit(Chauffeur chauffeur) {
        editingChauffeur = chauffeur;
        firstNameField.setText(chauffeur.getFirstName());
        lastNameField.setText(chauffeur.getLastName());
        phoneField.setText(chauffeur.getPhone());
        licenseField.setText(chauffeur.getLicenseNumber());
        experienceField.setText(String.valueOf(chauffeur.getExperience()));
        ratingField.setText(String.valueOf(chauffeur.getRating()));
        formTitle.setText("Modifier Chauffeur");
        submitBtn.setText("Enregistrer");
    }

    private void confirmDelete(Chauffeur chauffeur) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setHeaderText("Supprimer " + chauffeur.getFullName() + " ?");
        alert.setContentText("Cette action est irreversible.");
        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try {
                service.supprimer(chauffeur.getId());
                if (editingChauffeur != null && editingChauffeur.getId() == chauffeur.getId()) onReset();
                refreshAll();
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML private void onSearch() { renderTable(); }
    @FXML private void onSort() { renderTable(); }
    @FXML private void onNavTransports() { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void onNavChauffeurs() { SceneManager.navigateTo(Routes.ADMIN_CHAUFFEURS); }
    @FXML private void onNavCategories() { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT_CATEGORIES); }
    @FXML private void onNavDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
