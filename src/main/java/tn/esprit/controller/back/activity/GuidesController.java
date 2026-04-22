package tn.esprit.controller.back.activity;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.models.activity.Guide;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.AiDescriptionService;
import tn.esprit.services.activity.GuideService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GuidesController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statRating;
    @FXML private Label statAssigned;

    @FXML private VBox formPanel;
    @FXML private Label formPanelTitle;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField ratingField;
    @FXML private TextField photoField;
    @FXML private TextArea bioField;
    @FXML private Label errFirstName;
    @FXML private Label errLastName;
    @FXML private Label errEmail;
    @FXML private Label errPhone;
    @FXML private Label errRating;
    @FXML private Label charCount;
    @FXML private Button submitBtn;
    @FXML private ImageView photoPreview;
    @FXML private Label photoLabel;
    @FXML private Button generateBioBtn;
    @FXML private ProgressIndicator aiProgress;
    @FXML private Label aiStatusLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<Guide> tableView;
    @FXML private TableColumn<Guide, Integer> colIndex;
    @FXML private TableColumn<Guide, String> colName;
    @FXML private TableColumn<Guide, String> colEmail;
    @FXML private TableColumn<Guide, String> colPhone;
    @FXML private TableColumn<Guide, Float> colRating;
    @FXML private TableColumn<Guide, Void> colActions;
    @FXML private Label badgeCount;
    @FXML private Label pagInfo;
    @FXML private HBox pagButtons;

    private final GuideService service = new GuideService();
    private List<Guide> allData = new ArrayList<>();
    private Guide guideEnEdition;
    private String selectedPhotoPath;
    private static final int PER_PAGE = 8;
    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par...",
                "Nom (A-Z)",
                "Note (croissante)",
                "Note (decroissante)"
        ));
        sortCombo.getSelectionModel().selectFirst();
        aiProgress.managedProperty().bind(aiProgress.visibleProperty());
        aiStatusLabel.managedProperty().bind(aiStatusLabel.visibleProperty());
        aiProgress.setVisible(false);
        aiStatusLabel.setVisible(false);
        setupColumns();
        refreshAll();
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
        statTotal.setText(String.valueOf(allData.size()));
        double avg = allData.stream()
                .filter(guide -> guide.getRating() != null)
                .mapToDouble(Guide::getRating)
                .average()
                .orElse(0);
        statRating.setText(allData.isEmpty() ? "--" : String.format("%.1f / 5", avg));
        statAssigned.setText("--");
    }

    @FXML
    private void onOpenForm() {
        guideEnEdition = null;
        formPanelTitle.setText("Nouveau guide");
        submitBtn.setText("Ajouter");
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        ratingField.clear();
        bioField.clear();
        photoField.clear();
        photoPreview.setImage(null);
        photoLabel.setText("Aucune photo selectionnee");
        charCount.setText("0 / 5000 caracteres");
        setAiFeedback(false, "", false);
        resetAllErrors();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
        firstNameField.requestFocus();
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        guideEnEdition = null;
        setAiFeedback(false, "", false);
        resetAllErrors();
    }

    private void loadForEdit(Guide guide) {
        guideEnEdition = guide;
        formPanelTitle.setText("Modifier le guide");
        submitBtn.setText("Enregistrer");
        firstNameField.setText(guide.getFirstName());
        lastNameField.setText(guide.getLastName());
        emailField.setText(guide.getEmail());
        phoneField.setText(guide.getPhone());
        bioField.setText(guide.getBio() != null ? guide.getBio() : "");
        photoField.setText(guide.getPhoto() != null ? guide.getPhoto() : "");
        ratingField.setText(guide.getRating() != null ? String.valueOf(guide.getRating()) : "");
        if (guide.getPhoto() != null && !guide.getPhoto().isBlank()) {
            try {
                photoPreview.setImage(new Image(guide.getPhoto(), 80, 80, true, true));
            } catch (Exception ignored) {
                photoPreview.setImage(null);
            }
        } else {
            photoPreview.setImage(null);
        }
        photoLabel.setText("Aucune photo selectionnee");
        updateCounter();
        setAiFeedback(false, "", false);
        resetAllErrors();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
        firstNameField.requestFocus();
    }

    @FXML
    private void onSubmit() {
        if (!validateAll()) {
            return;
        }

        Guide guide = guideEnEdition != null ? guideEnEdition : new Guide();
        guide.setFirstName(firstNameField.getText().trim());
        guide.setLastName(lastNameField.getText().trim());
        guide.setEmail(emailField.getText().trim());
        guide.setPhone(phoneField.getText().trim());
        guide.setBio(bioField.getText().trim());
        guide.setPhoto(photoField.getText().trim());

        String ratingText = ratingField.getText().trim();
        guide.setRating(ratingText.isEmpty() ? null : Float.parseFloat(ratingText));

        try {
            if (guideEnEdition == null) {
                service.ajouter(guide);
            } else {
                service.modifier(guide);
            }
            onCloseForm();
            refreshAll();
        } catch (Exception exception) {
            showAlert("Erreur", exception.getMessage());
        }
    }

    @FXML
    private void onReset() {
        onOpenForm();
    }

    private void renderTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String sort = sortCombo.getValue();

        List<Guide> filtered = allData.stream()
                .filter(guide -> query.isEmpty()
                        || guide.getFirstName().toLowerCase().contains(query)
                        || guide.getLastName().toLowerCase().contains(query)
                        || guide.getEmail().toLowerCase().contains(query))
                .collect(Collectors.toList());

        if ("Nom (A-Z)".equals(sort)) {
            filtered.sort(Comparator.comparing(Guide::getLastName, String.CASE_INSENSITIVE_ORDER));
        } else if ("Note (croissante)".equals(sort)) {
            filtered.sort(Comparator.comparing(guide -> guide.getRating() == null ? 0f : guide.getRating()));
        } else if ("Note (decroissante)".equals(sort)) {
            filtered.sort((first, second) -> Float.compare(
                    second.getRating() == null ? 0f : second.getRating(),
                    first.getRating() == null ? 0f : first.getRating()
            ));
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
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));

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

        colName.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Guide guide = getTableView().getItems().get(getIndex());
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);

                Label avatar = new Label();
                if (guide.getPhoto() != null && !guide.getPhoto().isBlank()) {
                    try {
                        ImageView view = new ImageView(new Image(guide.getPhoto(), 32, 32, true, true));
                        view.setFitWidth(32);
                        view.setFitHeight(32);
                        avatar.setGraphic(view);
                    } catch (Exception exception) {
                        avatar.setText("Guide");
                    }
                } else {
                    avatar.setText("Guide");
                    avatar.setStyle("-fx-background-color:#f0fff4; -fx-background-radius:16px;"
                            + "-fx-min-width:32px; -fx-min-height:32px; -fx-alignment:center;"
                            + "-fx-font-size:10px; -fx-text-fill:#2d7a50;");
                }

                Label name = new Label(guide.getFirstName() + " " + guide.getLastName());
                name.getStyleClass().add("td-name");
                box.getChildren().addAll(avatar, name);
                setGraphic(box);
                setText(null);
            }
        });

        colRating.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("--");
                    setGraphic(null);
                    return;
                }
                Label label = new Label(String.format("%.1f / 5", item));
                label.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                setGraphic(label);
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
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPhotoPath = file.toURI().toString();
            photoField.setText(file.getAbsolutePath());
            photoPreview.setImage(new Image(selectedPhotoPath, 80, 80, true, true));
            photoLabel.setText("Photo selectionnee: " + file.getName());
        }
    }

    private void confirmDelete(Guide guide) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer \"" + guide.getFirstName() + " " + guide.getLastName() + "\" ?");
        alert.setContentText("Cette action est irreversible.");
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(result -> result == confirm).ifPresent(result -> {
            try {
                service.supprimer(guide.getId());
                if (guideEnEdition != null && guideEnEdition.getId() == guide.getId()) {
                    onCloseForm();
                }
                refreshAll();
            } catch (SQLException exception) {
                showAlert("Erreur", exception.getMessage());
            }
        });
    }

    @FXML
    private void validateFirstName() {
        setFieldError(firstNameField, errFirstName, firstNameField.getText().trim().length() < 2);
    }

    @FXML
    private void validateLastName() {
        setFieldError(lastNameField, errLastName, lastNameField.getText().trim().length() < 2);
    }

    @FXML
    private void validateEmail() {
        setFieldError(emailField, errEmail,
                !emailField.getText().trim().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"));
    }

    @FXML
    private void validatePhone() {
        setFieldError(phoneField, errPhone, phoneField.getText().trim().length() < 10);
    }

    @FXML
    private void validateRating() {
        String text = ratingField.getText().trim();
        if (text.isEmpty()) {
            setFieldError(ratingField, errRating, false);
            return;
        }
        try {
            float value = Float.parseFloat(text);
            setFieldError(ratingField, errRating, value < 0 || value > 5);
        } catch (NumberFormatException exception) {
            setFieldError(ratingField, errRating, true);
        }
    }

    @FXML
    private void updateCounter() {
        charCount.setText(bioField.getText().length() + " / 5000 caracteres");
    }

    @FXML
    private void onGenerateBio() {
        validateFirstName();
        validateLastName();
        if (firstNameField.getText().trim().length() < 2 || lastNameField.getText().trim().length() < 2) {
            setAiFeedback(false, "Renseignez d'abord le prenom et le nom du guide.", true);
            return;
        }

        setAiFeedback(true, "Generation de la biographie en cours...", false);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return AiDescriptionService.generateGuideBio(
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim()
                );
            }
        };

        task.setOnSucceeded(event -> {
            bioField.setText(task.getValue());
            bioField.positionCaret(bioField.getText().length());
            updateCounter();
            setAiFeedback(false,
                    AiDescriptionService.isAiConfigured()
                            ? "Biographie generee avec succes."
                            : "Biographie proposee en mode local.",
                    false);
        });
        task.setOnFailed(event -> setAiFeedback(false, "Generation indisponible pour le moment.", true));

        Thread thread = new Thread(task, "guide-ai-bio");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean validateAll() {
        boolean valid = true;

        if (firstNameField.getText().trim().length() < 2) {
            setFieldError(firstNameField, errFirstName, true);
            valid = false;
        } else {
            setFieldError(firstNameField, errFirstName, false);
        }

        if (lastNameField.getText().trim().length() < 2) {
            setFieldError(lastNameField, errLastName, true);
            valid = false;
        } else {
            setFieldError(lastNameField, errLastName, false);
        }

        if (!emailField.getText().trim().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            setFieldError(emailField, errEmail, true);
            valid = false;
        } else {
            setFieldError(emailField, errEmail, false);
        }

        if (phoneField.getText().trim().length() < 10) {
            setFieldError(phoneField, errPhone, true);
            valid = false;
        } else {
            setFieldError(phoneField, errPhone, false);
        }

        String ratingText = ratingField.getText().trim();
        if (!ratingText.isEmpty()) {
            try {
                float value = Float.parseFloat(ratingText);
                if (value < 0 || value > 5) {
                    setFieldError(ratingField, errRating, true);
                    valid = false;
                } else {
                    setFieldError(ratingField, errRating, false);
                }
            } catch (NumberFormatException exception) {
                setFieldError(ratingField, errRating, true);
                valid = false;
            }
        } else {
            setFieldError(ratingField, errRating, false);
        }

        return valid;
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        renderTable();
    }

    @FXML
    private void onNavDashboard() {
        onCloseForm();
        SceneManager.navigateTo(Routes.ADMIN_DASHBOARD);
    }

    @FXML
    private void onNavActivities() {
        onCloseForm();
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

    private void resetAllErrors() {
        setFieldError(firstNameField, errFirstName, false);
        setFieldError(lastNameField, errLastName, false);
        setFieldError(emailField, errEmail, false);
        setFieldError(phoneField, errPhone, false);
        setFieldError(ratingField, errRating, false);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void setAiFeedback(boolean loading, String message, boolean error) {
        aiProgress.setVisible(loading);
        generateBioBtn.setDisable(loading);
        aiStatusLabel.setText(message == null ? "" : message);
        aiStatusLabel.setVisible(message != null && !message.isBlank());
        aiStatusLabel.getStyleClass().removeAll("ai-assist-status-error", "ai-assist-status-success");
        if (aiStatusLabel.isVisible()) {
            aiStatusLabel.getStyleClass().add(error ? "ai-assist-status-error" : "ai-assist-status-success");
        }
    }
}
