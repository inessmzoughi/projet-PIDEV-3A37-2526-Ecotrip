package tn.esprit.controller.back.activity;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivityCategory;
import tn.esprit.models.activity.Guide;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityCategoryService;
import tn.esprit.services.activity.ActivityService;
import tn.esprit.services.activity.GuideService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ListActivitiesController implements Initializable {

    /* ─── Stats ─── */
    @FXML private Label statTotal, statActive, statAvgPrice;

    /* ─── Formulaire ─── */
    @FXML private Label    formIcon, formTitle, formSubtitle;
    @FXML private TextField titleField, priceField, durationField,
            locationField, maxParticipantsField,
            imageField, latitudeField, longitudeField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> categoryCombo, activeCombo, guideCombo, sortCombo;
    @FXML private Label     errTitle, errPrice, errDuration, errLocation,
            errCategory, errDescription, charCount;
    @FXML private Button submitBtn;
    @FXML private ImageView photoPreview;
    @FXML private Label photoLabel;
    private String selectedPhotoPath = null;

    /* ─── Table ─── */
    @FXML private TextField searchField;
    @FXML private TableView<Activity> tableView;
    @FXML private TableColumn<Activity, Integer> colIndex;
    @FXML private TableColumn<Activity, String>  colTitle, colCategory, colLocation;
    @FXML private TableColumn<Activity, Double>  colPrice;
    @FXML private TableColumn<Activity, Integer> colDuration;
    @FXML private TableColumn<Activity, Boolean> colActive;
    @FXML private TableColumn<Activity, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;
    @FXML private HBox  paginationBar;

    /* ─── State ─── */
    private final ActivityService         service         = new ActivityService();
    private final ActivityCategoryService categoryService = new ActivityCategoryService();
    private final GuideService            guideService    = new GuideService();
    private List<Activity>         allData       = new ArrayList<>();
    private List<ActivityCategory> allCategories = new ArrayList<>();
    private List<Guide>            allGuides     = new ArrayList<>();
    private final Map<String, Integer> categoryMap = new LinkedHashMap<>();
    private final Map<String, Integer> guideMap    = new LinkedHashMap<>();
    private Activity activityEnEdition = null;
    private static final int PER_PAGE = 6;
    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeCombo.setItems(FXCollections.observableArrayList("Actif", "Inactif"));
        activeCombo.getSelectionModel().selectFirst();
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…", "Titre (A→Z)", "Prix (croissant)", "Prix (décroissant)", "Durée (croissant)"));
        sortCombo.getSelectionModel().selectFirst();
        loadCategories();
        loadGuides();
        setupColumns();
        refreshAll();
    }

    /* ─── Loaders ─── */
    private void loadCategories() {
        try {
            allCategories = categoryService.afficherAll();
            categoryMap.clear();
            List<String> names = new ArrayList<>();
            for (ActivityCategory c : allCategories) {
                categoryMap.put(c.getName(), c.getId());
                names.add(c.getName());
            }
            categoryCombo.setItems(FXCollections.observableArrayList(names));
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    private void loadGuides() {
        try {
            allGuides = guideService.afficherAll();
            guideMap.clear();
            List<String> names = new ArrayList<>();
            names.add("— Aucun —");
            guideMap.put("— Aucun —", 0);
            for (Guide g : allGuides) {
                String fullName = g.getFirstName() + " " + g.getLastName();
                guideMap.put(fullName, g.getId());
                names.add(fullName);
            }
            guideCombo.setItems(FXCollections.observableArrayList(names));
            guideCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Data ─── */
    private void refreshAll() {
        try { allData = service.afficherAll(); }
        catch (SQLException e) { allData = new ArrayList<>(); showAlert("Erreur", e.getMessage()); }
        updateStats();
        renderTable();
    }

    private void updateStats() {
        int total = allData.size();
        statTotal.setText(String.valueOf(total));
        if (total == 0) { statActive.setText("—"); statAvgPrice.setText("—"); return; }
        long active = allData.stream().filter(Activity::isActive).count();
        statActive.setText(active + " actives");
        double avg = allData.stream().mapToDouble(Activity::getPrice).average().orElse(0);
        statAvgPrice.setText(String.format("%.1f TND", avg));
    }

    private void renderTable() {
        String query = searchField.getText().toLowerCase().trim();
        String sort  = sortCombo.getValue();
        List<Activity> filtered = allData.stream()
                .filter(a -> query.isEmpty()
                        || a.getTitle().toLowerCase().contains(query)
                        || a.getLocation().toLowerCase().contains(query))
                .collect(Collectors.toList());

        if ("Titre (A→Z)".equals(sort))
            filtered.sort(Comparator.comparing(Activity::getTitle));
        else if ("Prix (croissant)".equals(sort))
            filtered.sort(Comparator.comparingDouble(Activity::getPrice));
        else if ("Prix (décroissant)".equals(sort))
            filtered.sort(Comparator.comparingDouble(Activity::getPrice).reversed());
        else if ("Durée (croissant)".equals(sort))
            filtered.sort(Comparator.comparingInt(Activity::getDurationMinutes));

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
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                setText(String.valueOf(getIndex() + 1 + (currentPage - 1) * PER_PAGE));
                getStyleClass().add("td-index");
            }
        });

        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); return; }
                Activity a = getTableView().getItems().get(getIndex());
                Label chip = new Label(a.getCategory() != null ? a.getCategory().getName() : "—");
                chip.getStyleClass().add("td-city");
                setGraphic(chip); setText(null);
            }
        });

        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                Label l = new Label(String.format("%.1f TND", item));
                l.getStyleClass().add("td-price");
                setGraphic(l); setText(null);
            }
        });

        colDuration.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item + " min");
            }
        });

        colLocation.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label chip = new Label("📍 " + item);
                chip.getStyleClass().add("td-city");
                setGraphic(chip); setText(null);
            }
        });

        colActive.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Activity a = getTableView().getItems().get(getIndex());
                Label badge = new Label(a.isActive() ? "✅ Actif" : "❌ Inactif");
                badge.getStyleClass().add(a.isActive() ? "badge-actif" : "badge-inactif");
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
                editBtn.setOnAction(e -> loadForEdit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e  -> confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    /* ─── Photo chooser ─── */
    @FXML private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );
        Stage stage = (Stage) titleField.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPhotoPath = file.toURI().toString();
            imageField.setText(file.getAbsolutePath());
            photoPreview.setImage(new Image(selectedPhotoPath, 80, 80, true, true));
            photoLabel.setText("✅ " + file.getName());
        }
    }

    /* ─── Validation en temps réel ─── */
    @FXML private void validateTitle() {
        String t = titleField.getText().trim();
        setFieldError(titleField, errTitle, t.length() < 3 || t.length() > 150);
    }
    @FXML private void validatePrice() {
        try {
            double v = Double.parseDouble(priceField.getText().trim());
            setFieldError(priceField, errPrice, v <= 0);
        } catch (NumberFormatException e) { setFieldError(priceField, errPrice, true); }
    }
    @FXML private void validateDuration() {
        try {
            int v = Integer.parseInt(durationField.getText().trim());
            setFieldError(durationField, errDuration, v < 5 || v > 1440);
        } catch (NumberFormatException e) { setFieldError(durationField, errDuration, true); }
    }
    @FXML private void validateLocation() {
        setFieldError(locationField, errLocation, locationField.getText().trim().length() < 3);
    }
    @FXML private void updateCounter() {
        charCount.setText(descriptionField.getText().length() + " / 5000 caractères");
    }

    /* ─── Validation globale ─── */
    private boolean validateAll() {
        boolean ok = true;
        String t = titleField.getText().trim();
        if (t.length() < 3 || t.length() > 150) { setFieldError(titleField, errTitle, true); ok = false; }
        else setFieldError(titleField, errTitle, false);
        try {
            double v = Double.parseDouble(priceField.getText().trim());
            if (v <= 0) { setFieldError(priceField, errPrice, true); ok = false; }
            else setFieldError(priceField, errPrice, false);
        } catch (NumberFormatException e) { setFieldError(priceField, errPrice, true); ok = false; }
        try {
            int v = Integer.parseInt(durationField.getText().trim());
            if (v < 5 || v > 1440) { setFieldError(durationField, errDuration, true); ok = false; }
            else setFieldError(durationField, errDuration, false);
        } catch (NumberFormatException e) { setFieldError(durationField, errDuration, true); ok = false; }
        if (locationField.getText().trim().length() < 3) { setFieldError(locationField, errLocation, true); ok = false; }
        else setFieldError(locationField, errLocation, false);
        if (categoryCombo.getValue() == null) {
            errCategory.setVisible(true); errCategory.setManaged(true); ok = false;
        } else { errCategory.setVisible(false); errCategory.setManaged(false); }
        if (descriptionField.getText().trim().length() < 10) { setFieldError(descriptionField, errDescription); ok = false; }
        else { clearTextAreaError(descriptionField, errDescription); }
        return ok;
    }

    /* ─── Submit ─── */
    @FXML private void onSubmit() {
        if (!validateAll()) return;
        Activity a = activityEnEdition != null ? activityEnEdition : new Activity();
        a.setTitle(titleField.getText().trim());
        a.setDescription(descriptionField.getText().trim());
        try { a.setPrice(Double.parseDouble(priceField.getText().trim())); } catch (NumberFormatException ignored) {}
        try { a.setDurationMinutes(Integer.parseInt(durationField.getText().trim())); } catch (NumberFormatException ignored) {}
        a.setLocation(locationField.getText().trim());
        try { a.setMaxParticipants(Integer.parseInt(maxParticipantsField.getText().trim())); } catch (NumberFormatException ignored) {}
        a.setImage(imageField.getText().trim());
        a.setLatitude(latitudeField.getText().trim());
        a.setLongitude(longitudeField.getText().trim());
        a.setActive("Actif".equals(activeCombo.getValue()));

        // Catégorie
        int catId = categoryMap.getOrDefault(categoryCombo.getValue(), 0);
        allCategories.stream().filter(c -> c.getId() == catId).findFirst().ifPresent(a::setCategory);

        // Guide
        String guideName = guideCombo.getValue();
        int guideId = guideMap.getOrDefault(guideName, 0);
        if (guideId == 0) {
            a.setGuide(null);
        } else {
            allGuides.stream().filter(g -> g.getId() == guideId).findFirst().ifPresent(a::setGuide);
        }

        try {
            if (activityEnEdition == null) {
                service.ajouter(a);
                showSuccessPopup("Activité ajoutée avec succès !", "✅");
            } else {
                service.modifier(a);
                showSuccessPopup("Activité modifiée avec succès !", "💾");
            }
            onReset();
            refreshAll();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Reset ─── */
    @FXML private void onReset() {
        activityEnEdition = null;
        titleField.clear(); priceField.clear(); durationField.clear();
        locationField.clear(); maxParticipantsField.clear();
        imageField.clear(); latitudeField.clear(); longitudeField.clear();
        descriptionField.clear();
        categoryCombo.setValue(null);
        activeCombo.getSelectionModel().selectFirst();
        guideCombo.getSelectionModel().selectFirst();

        // Reset erreurs
        setFieldError(titleField,    errTitle,    false);
        setFieldError(priceField,    errPrice,    false);
        setFieldError(durationField, errDuration, false);
        setFieldError(locationField, errLocation, false);
        errCategory.setVisible(false); errCategory.setManaged(false);
        clearTextAreaError(descriptionField, errDescription);

        charCount.setText("0 / 5000 caractères");
        if (photoPreview != null) photoPreview.setImage(null);
        if (photoLabel   != null) photoLabel.setText("Aucune photo sélectionnée");
        selectedPhotoPath = null;
        formIcon.setText("🏃");
        formTitle.setText("Nouvelle Activité");
        formSubtitle.setText("Remplissez les informations.");
        submitBtn.setText("➕ Ajouter");
    }

    /* ─── Charger pour édition ─── */
    private void loadForEdit(Activity a) {
        activityEnEdition = a;
        titleField.setText(a.getTitle());
        priceField.setText(String.valueOf(a.getPrice()));
        durationField.setText(String.valueOf(a.getDurationMinutes()));
        locationField.setText(a.getLocation());
        maxParticipantsField.setText(String.valueOf(a.getMaxParticipants()));
        imageField.setText(a.getImage() != null ? a.getImage() : "");
        latitudeField.setText(a.getLatitude() != null ? a.getLatitude() : "");
        longitudeField.setText(a.getLongitude() != null ? a.getLongitude() : "");
        descriptionField.setText(a.getDescription() != null ? a.getDescription() : "");
        activeCombo.setValue(a.isActive() ? "Actif" : "Inactif");
        if (a.getCategory() != null) categoryCombo.setValue(a.getCategory().getName());
        if (a.getGuide() != null)
            guideCombo.setValue(a.getGuide().getFirstName() + " " + a.getGuide().getLastName());
        else
            guideCombo.setValue("— Aucun —");
        updateCounter();
        formIcon.setText("✏️");
        formTitle.setText("Modifier l'Activité");
        formSubtitle.setText("Mettez à jour les informations.");
        submitBtn.setText("💾 Enregistrer");
        titleField.requestFocus();
    }

    /* ─── Supprimer ─── */
    private void confirmDelete(Activity a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer « " + a.getTitle() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            try {
                service.supprimer(a.getId());
                if (activityEnEdition != null && activityEnEdition.getId() == a.getId()) onReset();
                refreshAll();
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    /* ─── Navigation ─── */
    @FXML private void onSearch()        { currentPage = 1; renderTable(); }
    @FXML private void onSort()          { currentPage = 1; renderTable(); }
    @FXML private void onNavDashboard()  { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void onNavCategories() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITY_CATEGORIES); }
    @FXML private void onNavGuides()     { SceneManager.navigateTo(Routes.ADMIN_GUIDES); }
    @FXML private void onNavSchedules()  { SceneManager.navigateTo(Routes.ADMIN_SCHEDULES); }

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

    private void setFieldError(TextArea field, Label errLabel) {
        if (!field.getStyleClass().contains("form-input-error"))
            field.getStyleClass().add("form-input-error");
        errLabel.setVisible(true); errLabel.setManaged(true);
    }

    private void clearTextAreaError(TextArea field, Label errLabel) {
        field.getStyleClass().remove("form-input-error");
        errLabel.setVisible(false); errLabel.setManaged(false);
    }

    private void showSuccessPopup(String message, String iconText) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(submitBtn.getScene().getWindow());

        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size:44px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button closeBtn = new Button("OK");
        closeBtn.setStyle(
                "-fx-background-color:#38a169; -fx-text-fill:white; -fx-font-weight:bold;"
                        + "-fx-background-radius:10; -fx-padding:10 40 10 40;"
                        + "-fx-cursor:hand; -fx-border-width:0; -fx-font-size:14px;");
        closeBtn.setOnAction(e -> popup.close());

        VBox box = new VBox(16, icon, msg, closeBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(36, 40, 32, 40));
        box.setStyle(
                "-fx-background-color:white;"
                        + "-fx-background-radius:16;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),24,0,0,6);"
                        + "-fx-border-color:#e2e8f0;"
                        + "-fx-border-radius:16;"
                        + "-fx-border-width:1;");

        Scene scene = new Scene(box, 320, 230);
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
