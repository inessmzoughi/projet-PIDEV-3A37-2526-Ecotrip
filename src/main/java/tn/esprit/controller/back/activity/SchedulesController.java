package tn.esprit.controller.back.activity;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityScheduleService;
import tn.esprit.services.activity.ActivityService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class SchedulesController implements Initializable {

    /* ─── Stats ─── */
    @FXML
    private Label statTotal, statSpots, statActivities;

    /* ─── Form ─── */
    @FXML private Label    formIcon, formTitle, formSubtitle;
    @FXML private ComboBox<String> activityCombo;
    @FXML private TextField startAtField, endAtField, spotsField;
    @FXML private Label     errActivity, errStartAt, errEndAt, errSpots;
    @FXML private Button submitBtn;

    /* ─── Table ─── */
    @FXML private TextField searchField;
    @FXML private TableView<ActivitySchedule> tableView;
    @FXML private TableColumn<ActivitySchedule, Integer> colIndex, colSpots;
    @FXML private TableColumn<ActivitySchedule, String> colActivity, colStart, colEnd;
    @FXML private TableColumn<ActivitySchedule, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox pagButtons;

    /* ─── State ─── */
    private final ActivityScheduleService service         = new ActivityScheduleService();
    private final ActivityService activityService = new ActivityService();
    private List<ActivitySchedule> allData      = new ArrayList<>();
    private List<Activity>         allActivities = new ArrayList<>();
    private final Map<String, Integer> activityMap = new LinkedHashMap<>();
    private ActivitySchedule scheduleEnEdition = null;
    private static final int PER_PAGE = 8;
    private int currentPage = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadActivities();
        setupColumns();
        refreshAll();
    }

    /* ─── Loaders ─── */
    private void loadActivities() {
        try {
            allActivities = activityService.afficherAll();
            activityMap.clear();
            List<String> names = new ArrayList<>();
            for (Activity a : allActivities) {
                activityMap.put(a.getTitle(), a.getId());
                names.add(a.getTitle());
            }
            activityCombo.setItems(FXCollections.observableArrayList(names));
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
        statTotal.setText(String.valueOf(allData.size()));
        int totalSpots = allData.stream().mapToInt(ActivitySchedule::getAvailableSpots).sum();
        statSpots.setText(String.valueOf(totalSpots));
        long distinct = allData.stream()
                .map(s -> s.getActivity().getId())
                .distinct().count();
        statActivities.setText(String.valueOf(distinct));
    }

    private void renderTable() {
        String query = searchField.getText().toLowerCase().trim();
        List<ActivitySchedule> filtered = allData.stream()
                .filter(s -> query.isEmpty()
                        || s.getActivity().getTitle().toLowerCase().contains(query))
                .sorted(Comparator.comparing(ActivitySchedule::getStartAt))
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
        colSpots.setCellValueFactory(new PropertyValueFactory<>("availableSpots"));

        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                setText(String.valueOf(getIndex() + 1 + (currentPage - 1) * PER_PAGE));
                getStyleClass().add("td-index");
            }
        });

        colActivity.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); return; }
                ActivitySchedule s = getTableView().getItems().get(getIndex());
                Label l = new Label(s.getActivity().getTitle());
                l.getStyleClass().add("td-name");
                setGraphic(l); setText(null);
            }
        });

        colStart.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                ActivitySchedule s = getTableView().getItems().get(getIndex());
                setText(s.getStartAt().format(FMT));
            }
        });

        colEnd.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                ActivitySchedule s = getTableView().getItems().get(getIndex());
                setText(s.getEndAt().format(FMT));
            }
        });

        colSpots.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                Label l = new Label(item + " 🎯");
                l.getStyleClass().add("td-price");
                setGraphic(l); setText(null);
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
    @FXML private void validateStartAt() {
        try {
            LocalDateTime dt = LocalDateTime.parse(startAtField.getText().trim(), FMT);
            setFieldError(startAtField, errStartAt, !dt.isAfter(LocalDateTime.now()));
        } catch (DateTimeParseException e) { setFieldError(startAtField, errStartAt, true); }
    }

    @FXML private void validateEndAt() {
        try {
            LocalDateTime start = LocalDateTime.parse(startAtField.getText().trim(), FMT);
            LocalDateTime end   = LocalDateTime.parse(endAtField.getText().trim(), FMT);
            setFieldError(endAtField, errEndAt, !end.isAfter(start));
        } catch (DateTimeParseException e) { setFieldError(endAtField, errEndAt, true); }
    }

    @FXML private void validateSpots() {
        try {
            int v = Integer.parseInt(spotsField.getText().trim());
            setFieldError(spotsField, errSpots, v < 1);
        } catch (NumberFormatException e) { setFieldError(spotsField, errSpots, true); }
    }

    private boolean validateAll() {
        boolean ok = true;
        if (activityCombo.getValue() == null) {
            errActivity.setVisible(true); errActivity.setManaged(true); ok = false;
        } else { errActivity.setVisible(false); errActivity.setManaged(false); }

        try {
            LocalDateTime dt = LocalDateTime.parse(startAtField.getText().trim(), FMT);
            if (!dt.isAfter(LocalDateTime.now())) { setFieldError(startAtField, errStartAt, true); ok = false; }
            else setFieldError(startAtField, errStartAt, false);
        } catch (DateTimeParseException e) { setFieldError(startAtField, errStartAt, true); ok = false; }

        try {
            LocalDateTime start = LocalDateTime.parse(startAtField.getText().trim(), FMT);
            LocalDateTime end   = LocalDateTime.parse(endAtField.getText().trim(), FMT);
            if (!end.isAfter(start)) { setFieldError(endAtField, errEndAt, true); ok = false; }
            else setFieldError(endAtField, errEndAt, false);
        } catch (DateTimeParseException e) { setFieldError(endAtField, errEndAt, true); ok = false; }

        try {
            int v = Integer.parseInt(spotsField.getText().trim());
            if (v < 1) { setFieldError(spotsField, errSpots, true); ok = false; }
            else setFieldError(spotsField, errSpots, false);
        } catch (NumberFormatException e) { setFieldError(spotsField, errSpots, true); ok = false; }

        return ok;
    }

    /* ─── Submit ─── */
    @FXML private void onSubmit() {
        if (!validateAll()) return;

        ActivitySchedule s = scheduleEnEdition != null ? scheduleEnEdition : new ActivitySchedule();
        s.setStartAt(LocalDateTime.parse(startAtField.getText().trim(), FMT));
        s.setEndAt(LocalDateTime.parse(endAtField.getText().trim(), FMT));
        s.setAvailableSpots(Integer.parseInt(spotsField.getText().trim()));

        int actId = activityMap.getOrDefault(activityCombo.getValue(), 0);
        allActivities.stream().filter(a -> a.getId() == actId).findFirst().ifPresent(s::setActivity);

        try {
            if (scheduleEnEdition == null) {
                service.ajouter(s);
                showToast("✅ Planning ajouté !");
            } else {
                service.modifier(s);
                showToast("💾 Planning modifié !");
            }
            onReset();
            refreshAll();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ─── Reset ─── */
    @FXML private void onReset() {
        scheduleEnEdition = null;
        activityCombo.setValue(null);
        startAtField.clear(); endAtField.clear(); spotsField.clear();
        errActivity.setVisible(false); errActivity.setManaged(false);
        setFieldError(startAtField, errStartAt, false);
        setFieldError(endAtField,   errEndAt,   false);
        setFieldError(spotsField,   errSpots,   false);
        formIcon.setText("📅");
        formTitle.setText("Nouveau Planning");
        formSubtitle.setText("Remplissez les informations.");
        submitBtn.setText("➕ Ajouter");
    }

    /* ─── Load for edit ─── */
    private void loadForEdit(ActivitySchedule s) {
        scheduleEnEdition = s;
        activityCombo.setValue(s.getActivity().getTitle());
        startAtField.setText(s.getStartAt().format(FMT));
        endAtField.setText(s.getEndAt().format(FMT));
        spotsField.setText(String.valueOf(s.getAvailableSpots()));
        formIcon.setText("✏️");
        formTitle.setText("Modifier le Planning");
        formSubtitle.setText("Mettez à jour les informations.");
        submitBtn.setText("💾 Enregistrer");
        startAtField.requestFocus();
    }

    /* ─── Delete ─── */
    private void confirmDelete(ActivitySchedule s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer ce planning pour « " + s.getActivity().getTitle() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            try {
                service.supprimer(s.getId());
                if (scheduleEnEdition != null && scheduleEnEdition.getId() == s.getId()) onReset();
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
