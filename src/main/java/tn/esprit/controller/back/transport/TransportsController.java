package tn.esprit.controller.back.transport;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.models.transport.Chauffeur;
import tn.esprit.models.transport.Transport;
import tn.esprit.models.transport.TransportCategory;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.transport.ChauffeurService;
import tn.esprit.services.transport.TransportCategoryService;
import tn.esprit.services.transport.TransportService;

import java.net.URL;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TransportsController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statDisponible;
    @FXML private Label statPrixMoyen;

    @FXML private TextField typeField;
    @FXML private TextField capaciteField;
    @FXML private TextField emissionField;
    @FXML private TextField prixField;
    @FXML private TextField imageField;
    @FXML private ComboBox<TransportCategory> categorieCombo;
    @FXML private ComboBox<Chauffeur> chauffeurCombo;
    @FXML private ComboBox<String> disponibleCombo;
    @FXML private Button submitBtn;
    @FXML private Label formTitle;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<Transport> tableView;
    @FXML private TableColumn<Transport, String> colType;
    @FXML private TableColumn<Transport, Integer> colCapacite;
    @FXML private TableColumn<Transport, Double> colEmission;
    @FXML private TableColumn<Transport, Double> colPrix;
    @FXML private TableColumn<Transport, String> colDisponible;
    @FXML private TableColumn<Transport, String> colCategorie;
    @FXML private TableColumn<Transport, String> colChauffeur;
    @FXML private TableColumn<Transport, Void> colActions;
    @FXML private Label badgeCount;

    private final TransportService service = new TransportService();
    private final TransportCategoryService categoryService = new TransportCategoryService();
    private final ChauffeurService chauffeurService = new ChauffeurService();

    private List<Transport> allData = new ArrayList<>();
    private Transport editingTransport;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sortCombo.setItems(FXCollections.observableArrayList("Trier par...", "Type (A-Z)", "Prix croissant", "Capacite croissante"));
        sortCombo.getSelectionModel().selectFirst();
        disponibleCombo.setItems(FXCollections.observableArrayList("Disponible", "Indisponible"));
        disponibleCombo.getSelectionModel().selectFirst();
        setupColumns();
        loadCombos();
        refreshAll();
    }

    private void loadCombos() {
        try {
            categorieCombo.setItems(FXCollections.observableArrayList(categoryService.afficherAll()));
            chauffeurCombo.setItems(FXCollections.observableArrayList(chauffeurService.afficherAll()));
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
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
        statDisponible.setText(String.valueOf(allData.stream().filter(Transport::isDisponible).count()));
        double avg = allData.stream().mapToDouble(Transport::getPrixParPersonne).average().orElse(0);
        statPrixMoyen.setText(allData.isEmpty() ? "—" : String.format("%.2f DT", avg));
    }

    private void renderTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<Transport> filtered = allData.stream()
                .filter(t -> query.isEmpty()
                        || t.getType().toLowerCase().contains(query)
                        || (t.getCategory() != null && t.getCategory().getName().toLowerCase().contains(query))
                        || (t.getChauffeur() != null && t.getChauffeur().getFullName().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        String sort = sortCombo.getValue();
        if ("Type (A-Z)".equals(sort)) {
            filtered.sort(Comparator.comparing(Transport::getType, String.CASE_INSENSITIVE_ORDER));
        } else if ("Prix croissant".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(Transport::getPrixParPersonne));
        } else if ("Capacite croissante".equals(sort)) {
            filtered.sort(Comparator.comparingInt(Transport::getCapacite));
        }

        badgeCount.setText(String.valueOf(filtered.size()));
        tableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void setupColumns() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        colEmission.setCellValueFactory(new PropertyValueFactory<>("emissionCo2"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixParPersonne"));
        colDisponible.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().isDisponible() ? "Oui" : "Non"));
        colCategorie.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCategory() == null ? "" : cell.getValue().getCategory().getName()));
        colChauffeur.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getChauffeur() == null ? "" : cell.getValue().getChauffeur().getFullName()));

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
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void onSubmit() {
        try {
            Transport transport = editingTransport == null ? new Transport() : editingTransport;
            transport.setType(typeField.getText().trim());
            transport.setCapacite(Integer.parseInt(capaciteField.getText().trim()));
            transport.setEmissionCo2(Double.parseDouble(emissionField.getText().trim()));
            transport.setPrixParPersonne(Double.parseDouble(prixField.getText().trim()));
            transport.setImage(imageField.getText().trim().isEmpty() ? null : imageField.getText().trim());
            transport.setDisponible("Disponible".equals(disponibleCombo.getValue()));
            transport.setCategory(categorieCombo.getValue());
            transport.setChauffeur(chauffeurCombo.getValue());

            if (editingTransport == null) {
                service.ajouter(transport);
            } else {
                service.modifier(transport);
            }

            onReset();
            refreshAll();
        } catch (NumberFormatException e) {
            showAlert("Valeur invalide", "Capacite, emission et prix doivent etre numeriques.", Alert.AlertType.WARNING);
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onReset() {
        editingTransport = null;
        typeField.clear();
        capaciteField.clear();
        emissionField.clear();
        prixField.clear();
        imageField.clear();
        categorieCombo.getSelectionModel().clearSelection();
        chauffeurCombo.getSelectionModel().clearSelection();
        disponibleCombo.getSelectionModel().selectFirst();
        submitBtn.setText("Ajouter");
        formTitle.setText("Nouveau Transport");
    }

    @FXML
    private void onBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image transport");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        Stage stage = (Stage) imageField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            imageField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void loadForEdit(Transport transport) {
        editingTransport = transport;
        typeField.setText(transport.getType());
        capaciteField.setText(String.valueOf(transport.getCapacite()));
        emissionField.setText(String.valueOf(transport.getEmissionCo2()));
        prixField.setText(String.valueOf(transport.getPrixParPersonne()));
        imageField.setText(transport.getImage() == null ? "" : transport.getImage());
        categorieCombo.setValue(findCategory(transport.getCategory()));
        chauffeurCombo.setValue(findChauffeur(transport.getChauffeur()));
        disponibleCombo.setValue(transport.isDisponible() ? "Disponible" : "Indisponible");
        submitBtn.setText("Enregistrer");
        formTitle.setText("Modifier Transport");
    }

    private TransportCategory findCategory(TransportCategory category) {
        if (category == null) return null;
        return categorieCombo.getItems().stream().filter(c -> c.getId() == category.getId()).findFirst().orElse(null);
    }

    private Chauffeur findChauffeur(Chauffeur chauffeur) {
        if (chauffeur == null) return null;
        return chauffeurCombo.getItems().stream().filter(c -> c.getId() == chauffeur.getId()).findFirst().orElse(null);
    }

    private void confirmDelete(Transport transport) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setHeaderText("Supprimer " + transport.getType() + " ?");
        alert.setContentText("Cette action est irreversible.");
        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try {
                service.supprimer(transport.getId());
                if (editingTransport != null && editingTransport.getId() == transport.getId()) {
                    onReset();
                }
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
