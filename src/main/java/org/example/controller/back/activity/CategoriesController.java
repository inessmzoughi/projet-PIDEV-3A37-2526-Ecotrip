package org.example.controller.back.activity;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.example.models.activity.ActivityCategory;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.services.activity.ActivityCategoryService;
import org.example.services.activity.ActivityService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CategoriesController implements Initializable {

    /* ─── Stats ─── */
    @FXML
    private Label statTotal, statActivities, statAvg;

    /* ─── Form ─── */
    @FXML private Label formIcon, formTitle, formSubtitle;
    @FXML private TextField nameField, iconField;
    @FXML private TextArea descriptionField;
    @FXML private Label errName, charCount;
    @FXML private Button submitBtn;

    /* ─── Table ─── */
    @FXML private TextField searchField;
    @FXML private TableView<ActivityCategory> tableView;
    @FXML private TableColumn<ActivityCategory, Integer> colIndex;
    @FXML private TableColumn<ActivityCategory, String>  colIcon, colName, colDesc;
    @FXML private TableColumn<ActivityCategory, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox pagButtons;

    /* ─── State ─── */
    private final ActivityCategoryService service         = new ActivityCategoryService();
    private List<ActivityCategory> allData = new ArrayList<>();
    private ActivityCategory categoryEnEdition = null;
    private static final int PER_PAGE = 8;
    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        refreshAll();
    }

    /* ─── Load data ─── */
    private void refreshAll() {
        try { allData = service.afficherAll(); }
        catch (SQLException e) { allData = new ArrayList<>(); showAlert("Erreur", e.getMessage()); }
        updateStats();
        renderTable();
    }

    private void updateStats() {
        int total = allData.size();
        statTotal.setText(String.valueOf(total));
        statActivities.setText("—");
        statAvg.setText("—");
    }

    private void renderTable() {
        String query = searchField.getText().toLowerCase().trim();
        List<ActivityCategory> filtered = allData.stream()
                .filter(c -> query.isEmpty() || c.getName().toLowerCase().contains(query))
                .sorted(Comparator.comparing(ActivityCategory::getName))
                .collect(Collectors.toList());

        badgeCount.setText(String.valueOf(filtered.size()));
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        if (currentPage > totalPages) currentPage = 1;
        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);
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

    /* ─── Columns ─── */
    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colIcon.setCellValueFactory(new PropertyValueFactory<>("icon"));

        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                setText(String.valueOf(getIndex() + 1 + (currentPage - 1) * PER_PAGE));
                getStyleClass().add("td-index");
            }
        });

        colIcon.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-font-size: 18px;");
                setGraphic(l); setText(null);
            }
        });

        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.length() > 60 ? item.substring(0, 60) + "…" : item);
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
                editBtn.setOnAction(e -> loadForEdit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e  -> confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    /* ─── Validation ─── */
    @FXML private void validateName() {
        boolean error = nameField.getText().trim().length() < 3;
        setFieldError(nameField, errName, error);
    }

    @FXML private void updateCounter() {
        charCount.setText(descriptionField.getText().length() + " / 1000 caractères");
    }

    private boolean validateAll() {
        boolean ok = true;
        if (nameField.getText().trim().length() < 3) {
            setFieldError(nameField, errName, true); ok = false;
        } else setFieldError(nameField, errName, false);
        return ok;
    }

    /* ─── Submit ─── */
    @FXML private void onSubmit() {
        if (!validateAll()) return;
        ActivityCategory c = categoryEnEdition != null ? categoryEnEdition : new ActivityCategory();
        c.setName(nameField.getText().trim());
        c.setDescription(descriptionField.getText().trim());
        c.setIcon(iconField.getText().trim());
        try {
            if (categoryEnEdition == null) {
                service.ajouter(c);
                showToast("✅ Catégorie ajoutée !");
            } else {
                service.modifier(c);
                showToast("💾 Catégorie modifiée !");
            }
            onReset();
            refreshAll();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Reset ─── */
    @FXML private void onReset() {
        categoryEnEdition = null;
        nameField.clear(); descriptionField.clear(); iconField.clear();
        setFieldError(nameField, errName, false);
        charCount.setText("0 / 1000 caractères");
        formIcon.setText("🏷️");
        formTitle.setText("Nouvelle Catégorie");
        formSubtitle.setText("Remplissez les informations.");
        submitBtn.setText("➕ Ajouter");
    }

    /* ─── Load for edit ─── */
    private void loadForEdit(ActivityCategory c) {
        categoryEnEdition = c;
        nameField.setText(c.getName());
        descriptionField.setText(c.getDescription() != null ? c.getDescription() : "");
        iconField.setText(c.getIcon() != null ? c.getIcon() : "");
        updateCounter();
        formIcon.setText("✏️");
        formTitle.setText("Modifier la Catégorie");
        formSubtitle.setText("Mettez à jour les informations.");
        submitBtn.setText("💾 Enregistrer");
        nameField.requestFocus();
    }

    /* ─── Delete ─── */
    private void confirmDelete(ActivityCategory c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer « " + c.getName() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            try {
                service.supprimer(c.getId());
                if (categoryEnEdition != null && categoryEnEdition.getId() == c.getId()) onReset();
                refreshAll();
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    /* ─── Navigation ─── */
    @FXML private void onSearch()        { currentPage = 1; renderTable(); }
    @FXML private void onNavDashboard()  { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void onNavActivities() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }

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

    private void showToast(String msg) {
        pagInfo.setText(msg);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(this::renderTable);
        }).start();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }


}
