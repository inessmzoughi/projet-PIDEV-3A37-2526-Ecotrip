package tn.esprit.controller.back.transport;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.models.transport.TransportCategory;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.transport.TransportCategoryService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TransportCategoriesController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statNamed;
    @FXML private Label formTitle;
    @FXML private Button submitBtn;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<TransportCategory> tableView;
    @FXML private TableColumn<TransportCategory, String> colName;
    @FXML private TableColumn<TransportCategory, String> colDescription;
    @FXML private TableColumn<TransportCategory, Void> colActions;
    @FXML private Label badgeCount;

    private final TransportCategoryService service = new TransportCategoryService();
    private List<TransportCategory> allData = new ArrayList<>();
    private TransportCategory editingCategory;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sortCombo.setItems(FXCollections.observableArrayList("Trier par...", "Nom (A-Z)", "Nom (Z-A)"));
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
        statTotal.setText(String.valueOf(allData.size()));
        statNamed.setText(String.valueOf(allData.stream().filter(c -> c.getName() != null && !c.getName().isBlank()).count()));
        renderTable();
    }

    private void renderTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<TransportCategory> filtered = allData.stream()
                .filter(c -> query.isEmpty()
                        || c.getName().toLowerCase().contains(query)
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        String sort = sortCombo.getValue();
        if ("Nom (A-Z)".equals(sort)) filtered.sort(Comparator.comparing(TransportCategory::getName, String.CASE_INSENSITIVE_ORDER));
        else if ("Nom (Z-A)".equals(sort)) filtered.sort(Comparator.comparing(TransportCategory::getName, String.CASE_INSENSITIVE_ORDER).reversed());

        badgeCount.setText(String.valueOf(filtered.size()));
        tableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
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
            TransportCategory category = editingCategory == null ? new TransportCategory() : editingCategory;
            category.setName(nameField.getText().trim());
            category.setDescription(descriptionField.getText().trim().isEmpty() ? null : descriptionField.getText().trim());

            if (editingCategory == null) service.ajouter(category);
            else service.modifier(category);

            onReset();
            refreshAll();
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onReset() {
        editingCategory = null;
        nameField.clear();
        descriptionField.clear();
        formTitle.setText("Nouvelle Categorie");
        submitBtn.setText("Ajouter");
    }

    private void loadForEdit(TransportCategory category) {
        editingCategory = category;
        nameField.setText(category.getName());
        descriptionField.setText(category.getDescription() == null ? "" : category.getDescription());
        formTitle.setText("Modifier Categorie");
        submitBtn.setText("Enregistrer");
    }

    private void confirmDelete(TransportCategory category) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setHeaderText("Supprimer " + category.getName() + " ?");
        alert.setContentText("Cette action est irreversible.");
        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try {
                service.supprimer(category.getId());
                if (editingCategory != null && editingCategory.getId() == category.getId()) onReset();
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
