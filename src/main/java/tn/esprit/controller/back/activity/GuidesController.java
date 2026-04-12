package tn.esprit.controller.back.activity;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.activity.Guide;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.services.activity.GuideService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GuidesController implements Initializable {

    /* ─── Stats ─── */
    @FXML private Label statTotal, statRating, statAssigned;

    /* ─── Form ─── */
    @FXML private Label    formIcon, formTitle, formSubtitle;
    @FXML private TextField firstNameField, lastNameField, emailField,
            phoneField, ratingField, photoField;
    @FXML private TextArea  bioField;
    @FXML private Label     errFirstName, errLastName, errEmail,
            errPhone, errRating, charCount;
    @FXML private Button    submitBtn;
    @FXML private ImageView photoPreview;
    @FXML private Label     photoLabel;

    /* ─── Table ─── */
    @FXML private TextField searchField;
    @FXML private TableView<Guide> tableView;
    @FXML private TableColumn<Guide, Integer> colIndex;
    @FXML private TableColumn<Guide, String>  colName, colEmail, colPhone;
    @FXML private TableColumn<Guide, Float>   colRating;
    @FXML private TableColumn<Guide, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;

    /* ─── State ─── */
    private final GuideService service = new GuideService();
    private List<Guide> allData = new ArrayList<>();
    private Guide guideEnEdition = null;
    private String selectedPhotoPath = null;
    private static final int PER_PAGE = 8;
    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        refreshAll();
    }

    private void refreshAll() {
        try { allData = service.afficherAll(); }
        catch (SQLException e) { allData = new ArrayList<>(); showAlert("Erreur", e.getMessage()); }
        updateStats();
        renderTable();
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));
        double avg = allData.stream()
                .filter(g -> g.getRating() != null)
                .mapToDouble(Guide::getRating)
                .average().orElse(0);
        statRating.setText(allData.isEmpty() ? "—" : String.format("%.1f ⭐", avg));
        statAssigned.setText("—");
    }

    private void renderTable() {
        String query = searchField.getText().toLowerCase().trim();
        List<Guide> filtered = allData.stream()
                .filter(g -> query.isEmpty()
                        || g.getFirstName().toLowerCase().contains(query)
                        || g.getLastName().toLowerCase().contains(query)
                        || g.getEmail().toLowerCase().contains(query))
                .sorted(Comparator.comparing(Guide::getLastName))
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

    private void setupColumns() {
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));

        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                setText(String.valueOf(getIndex() + 1 + (currentPage - 1) * PER_PAGE));
                getStyleClass().add("td-index");
            }
        });

        colName.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); return; }
                Guide g = getTableView().getItems().get(getIndex());
                // Show photo thumbnail if available
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);
                Label avatar = new Label();
                if (g.getPhoto() != null && !g.getPhoto().isBlank()) {
                    try {
                        ImageView iv = new ImageView(new Image(g.getPhoto(), 32, 32, true, true));
                        iv.setFitWidth(32); iv.setFitHeight(32);
                        iv.setStyle("-fx-background-radius: 16;");
                        avatar.setGraphic(iv);
                    } catch (Exception ex) {
                        avatar.setText("🧭");
                        avatar.setStyle("-fx-font-size: 18px;");
                    }
                } else {
                    avatar.setText("🧭");
                    avatar.setStyle("-fx-font-size: 18px; -fx-background-color: #f0fff4; " +
                            "-fx-background-radius: 16px; -fx-min-width: 32px; " +
                            "-fx-min-height: 32px; -fx-alignment: CENTER;");
                }
                Label name = new Label(g.getFirstName() + " " + g.getLastName());
                name.getStyleClass().add("td-name");
                box.getChildren().addAll(avatar, name);
                setGraphic(box); setText(null);
            }
        });

        colRating.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText("—"); return; }
                Label l = new Label(String.format("%.1f ⭐", item));
                l.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                setGraphic(l); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button delBtn  = new Button("🗑️");
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

    @FXML private void onChoosePhoto() {
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
            photoLabel.setText("✅ " + file.getName());
        }
    }

    @FXML private void validateFirstName() {
        setFieldError(firstNameField, errFirstName, firstNameField.getText().trim().length() < 2);
    }
    @FXML private void validateLastName() {
        setFieldError(lastNameField, errLastName, lastNameField.getText().trim().length() < 2);
    }
    @FXML private void validateEmail() {
        setFieldError(emailField, errEmail,
                !emailField.getText().trim().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"));
    }
    @FXML private void validatePhone() {
        setFieldError(phoneField, errPhone, phoneField.getText().trim().length() < 10);
    }
    @FXML private void validateRating() {
        String txt = ratingField.getText().trim();
        if (txt.isEmpty()) { setFieldError(ratingField, errRating, false); return; }
        try {
            float v = Float.parseFloat(txt);
            setFieldError(ratingField, errRating, v < 0 || v > 5);
        } catch (NumberFormatException e) { setFieldError(ratingField, errRating, true); }
    }
    @FXML private void updateCounter() {
        charCount.setText(bioField.getText().length() + " / 5000 caractères");
    }

    private boolean validateAll() {
        boolean ok = true;
        if (firstNameField.getText().trim().length() < 2) {
            setFieldError(firstNameField, errFirstName, true); ok = false;
        } else setFieldError(firstNameField, errFirstName, false);
        if (lastNameField.getText().trim().length() < 2) {
            setFieldError(lastNameField, errLastName, true); ok = false;
        } else setFieldError(lastNameField, errLastName, false);
        if (!emailField.getText().trim().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            setFieldError(emailField, errEmail, true); ok = false;
        } else setFieldError(emailField, errEmail, false);
        if (phoneField.getText().trim().length() < 10) {
            setFieldError(phoneField, errPhone, true); ok = false;
        } else setFieldError(phoneField, errPhone, false);
        return ok;
    }

    @FXML private void onSubmit() {
        if (!validateAll()) return;
        Guide g = guideEnEdition != null ? guideEnEdition : new Guide();
        g.setFirstName(firstNameField.getText().trim());
        g.setLastName(lastNameField.getText().trim());
        g.setEmail(emailField.getText().trim());
        g.setPhone(phoneField.getText().trim());
        g.setBio(bioField.getText().trim());
        g.setPhoto(photoField.getText().trim());
        String ratingTxt = ratingField.getText().trim();
        g.setRating(ratingTxt.isEmpty() ? null : Float.parseFloat(ratingTxt));
        try {
            if (guideEnEdition == null) { service.ajouter(g); showToast("✅ Guide ajouté !"); }
            else { service.modifier(g); showToast("💾 Guide modifié !"); }
            onReset(); refreshAll();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML private void onReset() {
        guideEnEdition = null; selectedPhotoPath = null;
        firstNameField.clear(); lastNameField.clear(); emailField.clear();
        phoneField.clear(); ratingField.clear(); bioField.clear(); photoField.clear();
        photoPreview.setImage(null);
        photoLabel.setText("Aucune photo sélectionnée");
        setFieldError(firstNameField, errFirstName, false);
        setFieldError(lastNameField,  errLastName,  false);
        setFieldError(emailField,     errEmail,     false);
        setFieldError(phoneField,     errPhone,     false);
        charCount.setText("0 / 5000 caractères");
        formIcon.setText("🧭"); formTitle.setText("Nouveau Guide");
        formSubtitle.setText("Remplissez les informations.");
        submitBtn.setText("➕ Ajouter");
    }

    private void loadForEdit(Guide g) {
        guideEnEdition = g;
        firstNameField.setText(g.getFirstName());
        lastNameField.setText(g.getLastName());
        emailField.setText(g.getEmail());
        phoneField.setText(g.getPhone());
        bioField.setText(g.getBio() != null ? g.getBio() : "");
        photoField.setText(g.getPhoto() != null ? g.getPhoto() : "");
        ratingField.setText(g.getRating() != null ? String.valueOf(g.getRating()) : "");
        if (g.getPhoto() != null && !g.getPhoto().isBlank()) {
            try { photoPreview.setImage(new Image(g.getPhoto(), 80, 80, true, true)); }
            catch (Exception ignored) {}
        }
        updateCounter();
        formIcon.setText("✏️"); formTitle.setText("Modifier le Guide");
        formSubtitle.setText("Mettez à jour les informations.");
        submitBtn.setText("💾 Enregistrer");
        firstNameField.requestFocus();
    }

    private void confirmDelete(Guide g) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer « " + g.getFirstName() + " " + g.getLastName() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            try {
                service.supprimer(g.getId());
                if (guideEnEdition != null && guideEnEdition.getId() == g.getId()) onReset();
                refreshAll();
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    @FXML private void onSearch()        { currentPage = 1; renderTable(); }
    @FXML private void onNavDashboard()  { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void onNavActivities() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }

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