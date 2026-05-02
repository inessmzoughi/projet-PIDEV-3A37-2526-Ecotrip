package tn.esprit.controller.back.activity;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
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
import tn.esprit.services.activity.ActivityMapService;
import tn.esprit.services.activity.ActivityService;
import tn.esprit.services.activity.AiDescriptionService;
import tn.esprit.services.activity.GuideService;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ListActivitiesController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statAvgPrice;

    @FXML private Label formIcon;
    @FXML private Label formTitle;
    @FXML private Label formSubtitle;
    @FXML private TextField titleField;
    @FXML private TextField priceField;
    @FXML private TextField durationField;
    @FXML private TextField locationField;
    @FXML private TextField maxParticipantsField;
    @FXML private TextField imageField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private TextArea descriptionField;
    @FXML private WebView mapPreview;
    @FXML private Label mapStatusLabel;
    @FXML private Button openMapBtn;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> activeCombo;
    @FXML private ComboBox<String> guideCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button generateDescBtn;
    @FXML private ProgressIndicator aiProgress;
    @FXML private Label aiStatusLabel;
    @FXML private Label errTitle;
    @FXML private Label errPrice;
    @FXML private Label errDuration;
    @FXML private Label errLocation;
    @FXML private Label errCategory;
    @FXML private Label errDescription;
    @FXML private Label charCount;
    @FXML private Button submitBtn;
    @FXML private ImageView photoPreview;
    @FXML private Label photoLabel;

    @FXML private TextField searchField;
    @FXML private TableView<Activity> tableView;
    @FXML private TableColumn<Activity, Integer> colIndex;
    @FXML private TableColumn<Activity, String> colTitle;
    @FXML private TableColumn<Activity, String> colCategory;
    @FXML private TableColumn<Activity, String> colLocation;
    @FXML private TableColumn<Activity, Double> colPrice;
    @FXML private TableColumn<Activity, Integer> colDuration;
    @FXML private TableColumn<Activity, Boolean> colActive;
    @FXML private TableColumn<Activity, Void> colActions;
    @FXML private Label badgeCount;
    @FXML private Label pagInfo;
    @FXML private HBox pagButtons;
    @FXML private HBox paginationBar;

    private final ActivityService service = new ActivityService();
    private final ActivityCategoryService categoryService = new ActivityCategoryService();
    private final GuideService guideService = new GuideService();

    private List<Activity> allData = new ArrayList<>();
    private List<ActivityCategory> allCategories = new ArrayList<>();
    private List<Guide> allGuides = new ArrayList<>();
    private final Map<String, Integer> categoryMap = new LinkedHashMap<>();
    private final Map<String, Integer> guideMap = new LinkedHashMap<>();

    private Activity activityEnEdition;
    private String selectedPhotoPath;
    private static final int PER_PAGE = 6;
    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        activeCombo.setItems(FXCollections.observableArrayList("Actif", "Inactif"));
        activeCombo.getSelectionModel().selectFirst();
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par...",
                "Titre (A-Z)",
                "Prix (croissant)",
                "Prix (decroissant)",
                "Duree (croissant)"
        ));
        sortCombo.getSelectionModel().selectFirst();

        aiProgress.managedProperty().bind(aiProgress.visibleProperty());
        aiStatusLabel.managedProperty().bind(aiStatusLabel.visibleProperty());
        aiProgress.setVisible(false);
        aiStatusLabel.setVisible(false);
        mapStatusLabel.managedProperty().bind(mapStatusLabel.visibleProperty());

        loadCategories();
        loadGuides();
        setupColumns();
        configureMapPreview();
        refreshAll();
    }

    private void loadCategories() {
        try {
            allCategories = categoryService.afficherAll();
            categoryMap.clear();
            List<String> names = new ArrayList<>();
            for (ActivityCategory category : allCategories) {
                categoryMap.put(category.getName(), category.getId());
                names.add(category.getName());
            }
            categoryCombo.setItems(FXCollections.observableArrayList(names));
        } catch (SQLException exception) {
            showAlert("Erreur", exception.getMessage());
        }
    }

    private void loadGuides() {
        try {
            allGuides = guideService.afficherAll();
            guideMap.clear();
            List<String> names = new ArrayList<>();
            names.add("-- Aucun --");
            guideMap.put("-- Aucun --", 0);
            for (Guide guide : allGuides) {
                String fullName = guide.getFirstName() + " " + guide.getLastName();
                guideMap.put(fullName, guide.getId());
                names.add(fullName);
            }
            guideCombo.setItems(FXCollections.observableArrayList(names));
            guideCombo.getSelectionModel().selectFirst();
        } catch (SQLException exception) {
            showAlert("Erreur", exception.getMessage());
        }
    }

    private void refreshAll() {
        try {
            allData = service.afficherAll();
        } catch (SQLException exception) {
            allData = new ArrayList<>();
            showAlert("Erreur", exception.getMessage());
        }
        updateStats();
        renderTable();
    }

    private void updateStats() {
        int total = allData.size();
        statTotal.setText(String.valueOf(total));
        if (total == 0) {
            statActive.setText("--");
            statAvgPrice.setText("--");
            return;
        }

        long active = allData.stream().filter(Activity::isActive).count();
        statActive.setText(active + " actives");
        double avg = allData.stream().mapToDouble(Activity::getPrice).average().orElse(0);
        statAvgPrice.setText(String.format("%.1f TND", avg));
    }

    private void renderTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String sort = sortCombo.getValue();

        List<Activity> filtered = allData.stream()
                .filter(activity -> query.isEmpty()
                        || activity.getTitle().toLowerCase().contains(query)
                        || activity.getLocation().toLowerCase().contains(query))
                .collect(Collectors.toList());

        if ("Titre (A-Z)".equals(sort)) {
            filtered.sort(Comparator.comparing(Activity::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else if ("Prix (croissant)".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(Activity::getPrice));
        } else if ("Prix (decroissant)".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(Activity::getPrice).reversed());
        } else if ("Duree (croissant)".equals(sort)) {
            filtered.sort(Comparator.comparingInt(Activity::getDurationMinutes));
        }

        badgeCount.setText(String.valueOf(filtered.size()));
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        if (currentPage > totalPages) {
            currentPage = 1;
        }

        int from = (currentPage - 1) * PER_PAGE;
        int to = Math.min(from + PER_PAGE, total);
        tableView.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
        pagInfo.setText(total == 0 ? "" : "Affichage " + (from + 1) + "-" + to + " sur " + total);

        pagButtons.getChildren().clear();
        for (int page = 1; page <= totalPages; page++) {
            final int pageNumber = page;
            Button button = new Button(String.valueOf(page));
            button.getStyleClass().add("page-btn");
            if (page == currentPage) {
                button.getStyleClass().add("page-btn-active");
            }
            button.setOnAction(event -> {
                currentPage = pageNumber;
                renderTable();
            });
            pagButtons.getChildren().add(button);
        }
    }

    private void setupColumns() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        colIndex.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(String.valueOf(getIndex() + 1 + (currentPage - 1) * PER_PAGE));
                getStyleClass().add("td-index");
            }
        });

        colCategory.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Activity activity = getTableView().getItems().get(getIndex());
                Label chip = new Label(activity.getCategory() != null ? activity.getCategory().getName() : "--");
                chip.getStyleClass().add("td-city");
                setGraphic(chip);
                setText(null);
            }
        });

        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label label = new Label(String.format("%.1f TND", item));
                label.getStyleClass().add("td-price");
                setGraphic(label);
                setText(null);
            }
        });

        colDuration.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item + " min");
            }
        });

        colLocation.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label chip = new Label("Lieu: " + item);
                chip.getStyleClass().add("td-city");
                setGraphic(chip);
                setText(null);
            }
        });

        colActive.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Activity activity = getTableView().getItems().get(getIndex());
                Label badge = new Label(activity.isActive() ? "Actif" : "Inactif");
                badge.getStyleClass().add(activity.isActive() ? "badge-actif" : "badge-inactif");
                setGraphic(badge);
                setText(null);
            }
        });

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(event -> loadForEdit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(event -> confirmDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void onChoosePhoto() {
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
            photoLabel.setText("Photo selectionnee: " + file.getName());
        }
    }

    @FXML
    private void validateTitle() {
        String title = titleField.getText().trim();
        setFieldError(titleField, errTitle, title.length() < 3 || title.length() > 150);
    }

    @FXML
    private void validatePrice() {
        try {
            double value = Double.parseDouble(priceField.getText().trim());
            setFieldError(priceField, errPrice, value <= 0);
        } catch (NumberFormatException exception) {
            setFieldError(priceField, errPrice, true);
        }
    }

    @FXML
    private void validateDuration() {
        try {
            int value = Integer.parseInt(durationField.getText().trim());
            setFieldError(durationField, errDuration, value < 5 || value > 1440);
        } catch (NumberFormatException exception) {
            setFieldError(durationField, errDuration, true);
        }
    }

    @FXML
    private void validateLocation() {
        setFieldError(locationField, errLocation, locationField.getText().trim().length() < 3);
    }

    @FXML
    private void updateCounter() {
        charCount.setText(descriptionField.getText().length() + " / 5000 caracteres");
    }

    private void configureMapPreview() {
        latitudeField.textProperty().addListener((obs, oldValue, newValue) -> updateMapPreview());
        longitudeField.textProperty().addListener((obs, oldValue, newValue) -> updateMapPreview());
        locationField.textProperty().addListener((obs, oldValue, newValue) -> updateMapPreview());
        titleField.textProperty().addListener((obs, oldValue, newValue) -> updateMapPreview());
        updateMapPreview();
    }

    @FXML
    private void onGenerateDescription() {
        if (!validateInputsForAi()) {
            return;
        }

        setAiFeedback(true, "Generation de la description en cours...", false);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return AiDescriptionService.generateActivityDescription(
                        titleField.getText().trim(),
                        locationField.getText().trim(),
                        priceField.getText().trim(),
                        durationField.getText().trim()
                );
            }
        };

        task.setOnSucceeded(event -> {
            descriptionField.setText(task.getValue());
            descriptionField.positionCaret(descriptionField.getText().length());
            updateCounter();
            clearTextAreaError(descriptionField, errDescription);
            setAiFeedback(
                    false,
                    AiDescriptionService.isAiConfigured()
                            ? "Description generee avec succes."
                            : "Description proposee en mode local.",
                    false
            );
        });
        task.setOnFailed(event -> setAiFeedback(false, "Generation indisponible pour le moment.", true));

        Thread thread = new Thread(task, "activity-ai-description");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onOpenMap() {
        if (!ActivityMapService.hasValidCoordinates(latitudeField.getText(), longitudeField.getText())) {
            mapStatusLabel.setText("Ajoutez une latitude et une longitude valides pour ouvrir la carte.");
            mapStatusLabel.setVisible(true);
            mapStatusLabel.getStyleClass().removeAll("map-status-error", "map-status-success");
            mapStatusLabel.getStyleClass().add("map-status-error");
            return;
        }

        try {
            Desktop.getDesktop().browse(new URI(
                    ActivityMapService.buildOpenStreetMapUrl(latitudeField.getText(), longitudeField.getText())
            ));
        } catch (Exception exception) {
            showAlert("Erreur", "Impossible d'ouvrir la carte : " + exception.getMessage());
        }
    }

    private boolean validateAll() {
        boolean valid = true;

        String title = titleField.getText().trim();
        if (title.length() < 3 || title.length() > 150) {
            setFieldError(titleField, errTitle, true);
            valid = false;
        } else {
            setFieldError(titleField, errTitle, false);
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                setFieldError(priceField, errPrice, true);
                valid = false;
            } else {
                setFieldError(priceField, errPrice, false);
            }
        } catch (NumberFormatException exception) {
            setFieldError(priceField, errPrice, true);
            valid = false;
        }

        try {
            int duration = Integer.parseInt(durationField.getText().trim());
            if (duration < 5 || duration > 1440) {
                setFieldError(durationField, errDuration, true);
                valid = false;
            } else {
                setFieldError(durationField, errDuration, false);
            }
        } catch (NumberFormatException exception) {
            setFieldError(durationField, errDuration, true);
            valid = false;
        }

        if (locationField.getText().trim().length() < 3) {
            setFieldError(locationField, errLocation, true);
            valid = false;
        } else {
            setFieldError(locationField, errLocation, false);
        }

        if (categoryCombo.getValue() == null) {
            errCategory.setVisible(true);
            errCategory.setManaged(true);
            valid = false;
        } else {
            errCategory.setVisible(false);
            errCategory.setManaged(false);
        }

        if (descriptionField.getText().trim().length() < 10) {
            setFieldError(descriptionField, errDescription);
            valid = false;
        } else {
            clearTextAreaError(descriptionField, errDescription);
        }

        String maxParticipantsText = maxParticipantsField.getText().trim();
        if (maxParticipantsText.isEmpty()) {
            maxParticipantsField.setText("1");
        } else {
            try {
                if (Integer.parseInt(maxParticipantsText) < 1) {
                    maxParticipantsField.setText("1");
                }
            } catch (NumberFormatException exception) {
                showAlert("Erreur", "Le nombre maximum de participants doit etre numerique.");
                valid = false;
            }
        }

        return valid;
    }

    private boolean validateInputsForAi() {
        boolean valid = true;
        validateTitle();
        validateLocation();
        validatePrice();
        validateDuration();

        if (titleField.getText().trim().length() < 3) {
            valid = false;
        }
        if (locationField.getText().trim().length() < 3) {
            valid = false;
        }

        try {
            if (Double.parseDouble(priceField.getText().trim()) <= 0) {
                valid = false;
            }
        } catch (NumberFormatException exception) {
            valid = false;
        }

        try {
            int duration = Integer.parseInt(durationField.getText().trim());
            if (duration < 5 || duration > 1440) {
                valid = false;
            }
        } catch (NumberFormatException exception) {
            valid = false;
        }

        if (!valid) {
            setAiFeedback(false,
                    "Renseignez le titre, le lieu, le prix et la duree avant de lancer l'assistant.",
                    true);
        }

        return valid;
    }

    @FXML
    private void onSubmit() {
        if (!validateAll()) {
            return;
        }

        Activity activity = activityEnEdition != null ? activityEnEdition : new Activity();
        activity.setTitle(titleField.getText().trim());
        activity.setDescription(descriptionField.getText().trim());

        try {
            activity.setPrice(Double.parseDouble(priceField.getText().trim()));
        } catch (NumberFormatException ignored) {
        }

        try {
            activity.setDurationMinutes(Integer.parseInt(durationField.getText().trim()));
        } catch (NumberFormatException ignored) {
        }

        activity.setLocation(locationField.getText().trim());

        try {
            activity.setMaxParticipants(Integer.parseInt(maxParticipantsField.getText().trim()));
        } catch (NumberFormatException ignored) {
            activity.setMaxParticipants(1);
        }

        activity.setImage(imageField.getText().trim());
        activity.setLatitude(latitudeField.getText().trim());
        activity.setLongitude(longitudeField.getText().trim());
        activity.setActive("Actif".equals(activeCombo.getValue()));

        int categoryId = categoryMap.getOrDefault(categoryCombo.getValue(), 0);
        allCategories.stream()
                .filter(category -> category.getId() == categoryId)
                .findFirst()
                .ifPresent(activity::setCategory);

        String guideName = guideCombo.getValue();
        int guideId = guideMap.getOrDefault(guideName, 0);
        if (guideId == 0) {
            activity.setGuide(null);
        } else {
            allGuides.stream()
                    .filter(guide -> guide.getId() == guideId)
                    .findFirst()
                    .ifPresent(activity::setGuide);
        }

        try {
            if (activityEnEdition == null) {
                service.ajouter(activity);
                showSuccessPopup("Activite ajoutee avec succes.", "OK");
            } else {
                service.modifier(activity);
                showSuccessPopup("Activite modifiee avec succes.", "OK");
            }
            onReset();
            refreshAll();
        } catch (Exception exception) {
            showAlert("Erreur", exception.getMessage());
        }
    }

    @FXML
    private void onReset() {
        activityEnEdition = null;
        titleField.clear();
        priceField.clear();
        durationField.clear();
        locationField.clear();
        maxParticipantsField.clear();
        imageField.clear();
        latitudeField.clear();
        longitudeField.clear();
        descriptionField.clear();
        categoryCombo.setValue(null);
        activeCombo.getSelectionModel().selectFirst();
        guideCombo.getSelectionModel().selectFirst();

        setFieldError(titleField, errTitle, false);
        setFieldError(priceField, errPrice, false);
        setFieldError(durationField, errDuration, false);
        setFieldError(locationField, errLocation, false);
        errCategory.setVisible(false);
        errCategory.setManaged(false);
        clearTextAreaError(descriptionField, errDescription);

        charCount.setText("0 / 5000 caracteres");
        setAiFeedback(false, "", false);
        updateMapPreview();
        if (photoPreview != null) {
            photoPreview.setImage(null);
        }
        if (photoLabel != null) {
            photoLabel.setText("Aucune photo selectionnee");
        }
        selectedPhotoPath = null;
        formIcon.setText("🏃");
        formTitle.setText("Nouvelle Activite");
        formSubtitle.setText("Remplissez les informations.");
        submitBtn.setText("Ajouter");
    }

    private void loadForEdit(Activity activity) {
        activityEnEdition = activity;
        titleField.setText(activity.getTitle());
        priceField.setText(String.valueOf(activity.getPrice()));
        durationField.setText(String.valueOf(activity.getDurationMinutes()));
        locationField.setText(activity.getLocation());
        maxParticipantsField.setText(String.valueOf(activity.getMaxParticipants()));
        imageField.setText(activity.getImage() != null ? activity.getImage() : "");
        latitudeField.setText(activity.getLatitude() != null ? activity.getLatitude() : "");
        longitudeField.setText(activity.getLongitude() != null ? activity.getLongitude() : "");
        descriptionField.setText(activity.getDescription() != null ? activity.getDescription() : "");
        activeCombo.setValue(activity.isActive() ? "Actif" : "Inactif");
        if (activity.getCategory() != null) {
            categoryCombo.setValue(activity.getCategory().getName());
        }
        if (activity.getGuide() != null) {
            guideCombo.setValue(activity.getGuide().getFirstName() + " " + activity.getGuide().getLastName());
        } else {
            guideCombo.setValue("-- Aucun --");
        }
        updateCounter();
        setAiFeedback(false, "", false);
        updateMapPreview();
        formIcon.setText("✏️");
        formTitle.setText("Modifier l'activite");
        formSubtitle.setText("Mettez a jour les informations.");
        submitBtn.setText("Enregistrer");
        titleField.requestFocus();
    }

    private void confirmDelete(Activity activity) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer \"" + activity.getTitle() + "\" ?");
        alert.setContentText("Cette action est irreversible.");
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(result -> result == confirm).ifPresent(result -> {
            try {
                service.supprimer(activity.getId());
                if (activityEnEdition != null && activityEnEdition.getId() == activity.getId()) {
                    onReset();
                }
                refreshAll();
            } catch (SQLException exception) {
                showAlert("Erreur", exception.getMessage());
            }
        });
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        renderTable();
    }

    @FXML
    private void onSort() {
        currentPage = 1;
        renderTable();
    }

    @FXML
    private void onNavDashboard() {
        SceneManager.navigateTo(Routes.ADMIN_DASHBOARD);
    }

    @FXML
    private void onNavCategories() {
        SceneManager.navigateTo(Routes.ADMIN_ACTIVITY_CATEGORIES);
    }

    @FXML
    private void onNavGuides() {
        SceneManager.navigateTo(Routes.ADMIN_GUIDES);
    }

    @FXML
    private void onNavSchedules() {
        SceneManager.navigateTo(Routes.ADMIN_SCHEDULES);
    }

    private void setFieldError(TextField field, Label errLabel, boolean hasError) {
        if (hasError) {
            if (!field.getStyleClass().contains("form-input-error")) {
                field.getStyleClass().add("form-input-error");
            }
            errLabel.setVisible(true);
            errLabel.setManaged(true);
        } else {
            field.getStyleClass().remove("form-input-error");
            errLabel.setVisible(false);
            errLabel.setManaged(false);
        }
    }

    private void setFieldError(TextArea field, Label errLabel) {
        if (!field.getStyleClass().contains("form-input-error")) {
            field.getStyleClass().add("form-input-error");
        }
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private void clearTextAreaError(TextArea field, Label errLabel) {
        field.getStyleClass().remove("form-input-error");
        errLabel.setVisible(false);
        errLabel.setManaged(false);
    }

    private void showSuccessPopup(String message, String iconText) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(submitBtn.getScene().getWindow());

        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size: 30px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button closeBtn = new Button("OK");
        closeBtn.setStyle("-fx-background-color:#38a169; -fx-text-fill:white; -fx-font-weight:bold;"
                + "-fx-background-radius:10; -fx-padding:10 40 10 40; -fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(event -> popup.close());

        VBox box = new VBox(16, icon, msg, closeBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(36, 40, 32, 40));
        box.setStyle("-fx-background-color:white;"
                + "-fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),24,0,0,6);"
                + "-fx-border-color:#e2e8f0;"
                + "-fx-border-radius:16;"
                + "-fx-border-width:1;");

        Scene scene = new Scene(box, 320, 220);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);

        popup.setOnShown(event -> {
            Stage owner = (Stage) submitBtn.getScene().getWindow();
            popup.setX(owner.getX() + (owner.getWidth() - popup.getWidth()) / 2);
            popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
        });

        popup.showAndWait();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void setAiFeedback(boolean loading, String message, boolean error) {
        aiProgress.setVisible(loading);
        generateDescBtn.setDisable(loading);
        aiStatusLabel.setText(message == null ? "" : message);
        aiStatusLabel.setVisible(message != null && !message.isBlank());
        aiStatusLabel.getStyleClass().removeAll("ai-assist-status-error", "ai-assist-status-success");
        if (aiStatusLabel.isVisible()) {
            aiStatusLabel.getStyleClass().add(error ? "ai-assist-status-error" : "ai-assist-status-success");
        }
    }

    private void updateMapPreview() {
        if (mapPreview == null) {
            return;
        }

        boolean hasCoordinates = ActivityMapService.hasValidCoordinates(latitudeField.getText(), longitudeField.getText());
        openMapBtn.setDisable(!hasCoordinates);
        mapStatusLabel.getStyleClass().removeAll("map-status-error", "map-status-success");

        if (!hasCoordinates) {
            mapPreview.getEngine().loadContent(
                    ActivityMapService.buildEmptyStateHtml(
                            "Carte de l'activite",
                            "Ajoutez des coordonnees pour visualiser instantanement la localisation."
                    )
            );
            mapStatusLabel.setText("Coordonnees non disponibles.");
            mapStatusLabel.setVisible(true);
            mapStatusLabel.getStyleClass().add("map-status-error");
            return;
        }

        mapPreview.getEngine().loadContent(
                ActivityMapService.buildMapHtml(
                        titleField.getText(),
                        locationField.getText(),
                        latitudeField.getText(),
                        longitudeField.getText()
                )
        );
        mapStatusLabel.setText("Carte synchronisee avec les coordonnees saisies.");
        mapStatusLabel.setVisible(true);
        mapStatusLabel.getStyleClass().add("map-status-success");
    }
}
