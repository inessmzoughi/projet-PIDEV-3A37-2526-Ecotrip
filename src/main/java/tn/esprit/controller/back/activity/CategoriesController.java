package tn.esprit.controller.back.activity;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.models.activity.ActivityCategory;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityCategoryService;
import tn.esprit.services.activity.AiDescriptionService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CategoriesController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statActivities;
    @FXML private Label statAvg;

    @FXML private Label formPreviewIcon;
    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private Label errName;
    @FXML private Label charCount;
    @FXML private Button submitBtn;
    @FXML private FlowPane iconGrid;
    @FXML private FlowPane colorGrid;
    @FXML private Button generateDescBtn;
    @FXML private ProgressIndicator aiProgress;
    @FXML private Label aiStatusLabel;

    @FXML private TextField searchField;
    @FXML private Label badgeCount;
    @FXML private FlowPane catCardsPane;
    @FXML private VBox emptyState;

    private static final String[] ICONS = {
            "🏕️", "🌿", "🥾", "🚣", "⛰️", "🌊",
            "🎣", "🚴", "🌅", "🌲", "🦋", "🎭"
    };

    private static final String[] COLORS = {
            "#3b82f6", "#38a169", "#8b5cf6", "#f59e0b",
            "#ef4444", "#0d9488", "#ec4899", "#6366f1",
            "#14b8a6", "#f97316", "#84cc16", "#06b6d4"
    };

    private final ActivityCategoryService service = new ActivityCategoryService();
    private List<ActivityCategory> allData = new ArrayList<>();
    private ActivityCategory categoryEnEdition;
    private String selectedIcon = ICONS[0];
    private String selectedColor = COLORS[0];

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        aiProgress.managedProperty().bind(aiProgress.visibleProperty());
        aiStatusLabel.managedProperty().bind(aiStatusLabel.visibleProperty());
        aiProgress.setVisible(false);
        aiStatusLabel.setVisible(false);
        buildIconPicker();
        buildColorPicker();
        refreshAll();
    }

    private void buildIconPicker() {
        iconGrid.getChildren().clear();
        for (String icon : ICONS) {
            Button button = new Button(icon);
            button.getStyleClass().add("icon-option-btn");
            if (icon.equals(selectedIcon)) {
                highlightIconBtn(button);
            }
            button.setOnAction(event -> {
                selectedIcon = icon;
                formPreviewIcon.setText(icon);
                buildIconPicker();
            });
            iconGrid.getChildren().add(button);
        }
    }

    private void highlightIconBtn(Button button) {
        button.setStyle("-fx-background-color:" + selectedColor + "22;"
                + "-fx-border-color:" + selectedColor + ";"
                + "-fx-border-width:1.5;"
                + "-fx-border-radius:8;");
    }

    private void buildColorPicker() {
        colorGrid.getChildren().clear();
        for (String color : COLORS) {
            Button button = new Button();
            button.setPrefSize(28, 28);
            button.setUserData(color);
            button.setStyle("-fx-background-color:" + color
                    + "; -fx-background-radius:50; -fx-cursor:hand;"
                    + "-fx-border-color:transparent; -fx-border-width:2; -fx-border-radius:50;");
            if (color.equals(selectedColor)) {
                highlightColorBtn(button, color);
            }
            button.setOnAction(event -> {
                selectedColor = color;
                buildColorPicker();
                buildIconPicker();
            });
            colorGrid.getChildren().add(button);
        }
    }

    private void highlightColorBtn(Button button, String color) {
        button.setStyle("-fx-background-color:" + color
                + "; -fx-background-radius:50; -fx-cursor:hand;"
                + "-fx-border-color:white; -fx-border-width:2; -fx-border-radius:50;"
                + "-fx-effect:dropshadow(gaussian," + color + ",6,0.6,0,0);");
    }

    private void refreshAll() {
        try {
            allData = service.afficherAll();
        } catch (SQLException exception) {
            allData = new ArrayList<>();
            showAlert("Erreur", exception.getMessage());
        }
        updateStats();
        renderCards();
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));
        statActivities.setText("--");
        statAvg.setText("--");
    }

    @FXML
    private void onSearch() {
        renderCards();
    }

    private void renderCards() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        List<ActivityCategory> filtered = allData.stream()
                .filter(category -> query.isEmpty()
                        || category.getName().toLowerCase().contains(query)
                        || (category.getDescription() != null
                        && category.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        badgeCount.setText(String.valueOf(filtered.size()));
        catCardsPane.getChildren().clear();

        if (filtered.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        for (int index = 0; index < filtered.size(); index++) {
            catCardsPane.getChildren().add(buildCatCard(filtered.get(index), index));
        }
    }

    private VBox buildCatCard(ActivityCategory category, int index) {
        String color = COLORS[index % COLORS.length];
        String icon = category.getIcon() != null && !category.getIcon().isBlank()
                ? category.getIcon()
                : ICONS[index % ICONS.length];

        VBox card = new VBox(10);
        card.getStyleClass().add("cat-card");
        card.setPrefWidth(250);
        card.setPadding(new Insets(18, 20, 16, 20));
        card.setStyle("-fx-border-color:" + color + " transparent transparent transparent; -fx-border-width:0 0 0 4;");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("cat-card-icon");
        iconLabel.setStyle("-fx-background-color:" + color + "55; -fx-border-color:" + color + "bb;");

        Label nameLabel = new Label(category.getName());
        nameLabel.getStyleClass().add("cat-card-name");
        top.getChildren().addAll(iconLabel, nameLabel);

        String descriptionText = category.getDescription() == null || category.getDescription().isEmpty()
                ? "Aucune description."
                : category.getDescription();
        Label description = new Label(descriptionText);
        description.getStyleClass().add("cat-card-desc");
        description.setWrapText(true);

        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("btn-edit");
        Button delBtn = new Button("Supprimer");
        delBtn.getStyleClass().add("btn-del");
        editBtn.setOnAction(event -> loadForEdit(category));
        delBtn.setOnAction(event -> confirmDelete(category));
        meta.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(top, description, meta);
        return card;
    }

    @FXML
    private void validateName() {
        setFieldError(nameField, errName, nameField.getText().trim().length() < 3);
    }

    @FXML
    private void updateCounter() {
        charCount.setText(descriptionField.getText().length() + " / 1000 caracteres");
    }

    @FXML
    private void onGenerateDescription() {
        validateName();
        if (nameField.getText().trim().length() < 3) {
            setAiFeedback(false, "Saisissez d'abord le nom de la categorie.", true);
            return;
        }

        setAiFeedback(true, "Generation de la description en cours...", false);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return AiDescriptionService.generateCategoryDescription(nameField.getText().trim());
            }
        };

        task.setOnSucceeded(event -> {
            descriptionField.setText(task.getValue());
            descriptionField.positionCaret(descriptionField.getText().length());
            updateCounter();
            setAiFeedback(false,
                    AiDescriptionService.isAiConfigured()
                            ? "Description generee avec succes."
                            : "Description proposee en mode local.",
                    false);
        });
        task.setOnFailed(event -> setAiFeedback(false, "Generation indisponible pour le moment.", true));

        Thread thread = new Thread(task, "category-ai-description");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean validateAll() {
        boolean valid = true;
        if (nameField.getText().trim().length() < 3) {
            setFieldError(nameField, errName, true);
            valid = false;
        } else {
            setFieldError(nameField, errName, false);
        }
        return valid;
    }

    @FXML
    private void onSubmit() {
        if (!validateAll()) {
            return;
        }

        ActivityCategory category = categoryEnEdition != null ? categoryEnEdition : new ActivityCategory();
        category.setName(nameField.getText().trim());
        category.setDescription(descriptionField.getText().trim());
        category.setIcon(selectedIcon);

        try {
            if (categoryEnEdition == null) {
                service.ajouter(category);
            } else {
                service.modifier(category);
            }
            onReset();
            refreshAll();
        } catch (Exception exception) {
            showAlert("Erreur", exception.getMessage());
        }
    }

    @FXML
    private void onReset() {
        categoryEnEdition = null;
        selectedIcon = ICONS[0];
        selectedColor = COLORS[0];
        nameField.clear();
        descriptionField.clear();
        charCount.setText("0 / 1000 caracteres");
        setFieldError(nameField, errName, false);
        setAiFeedback(false, "", false);
        formPreviewIcon.setText(selectedIcon);
        formTitle.setText("Nouvelle Categorie");
        submitBtn.setText("Enregistrer");
        buildIconPicker();
        buildColorPicker();
    }

    private void loadForEdit(ActivityCategory category) {
        categoryEnEdition = category;
        nameField.setText(category.getName());
        descriptionField.setText(category.getDescription() != null ? category.getDescription() : "");
        selectedIcon = category.getIcon() != null && !category.getIcon().isBlank() ? category.getIcon() : ICONS[0];
        formPreviewIcon.setText(selectedIcon);
        formTitle.setText("Modifier la categorie");
        submitBtn.setText("Enregistrer");
        updateCounter();
        setFieldError(nameField, errName, false);
        setAiFeedback(false, "", false);
        buildIconPicker();
        buildColorPicker();
        nameField.requestFocus();
    }

    private void confirmDelete(ActivityCategory category) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer \"" + category.getName() + "\" ?");
        alert.setContentText("Cette action est irreversible.");
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(result -> result == confirm).ifPresent(result -> {
            try {
                service.supprimer(category.getId());
                if (categoryEnEdition != null && categoryEnEdition.getId() == category.getId()) {
                    onReset();
                }
                refreshAll();
            } catch (SQLException exception) {
                showAlert("Erreur", exception.getMessage());
            }
        });
    }

    @FXML
    private void onNavDashboard() {
        SceneManager.navigateTo(Routes.ADMIN_DASHBOARD);
    }

    @FXML
    private void onNavActivities() {
        SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES);
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
}
