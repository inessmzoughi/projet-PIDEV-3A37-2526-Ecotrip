package tn.esprit.controller.back.activity;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML private Label statTotal, statSpots, statActivities;

    /* ─── Inline Form Panel (Equipements-style) ─── */
    @FXML private VBox  formPanel;
    @FXML private Label formPanelTitle;

    /* ─── Form Fields ─── */
    @FXML private ComboBox<String> activityCombo;
    @FXML private TextField        startAtField, endAtField, spotsField;
    @FXML private Label            errActivity, errStartAt, errEndAt, errSpots;

    /* ─── View Toggle ─── */
    @FXML private ToggleButton toggleCards, toggleList;
    @FXML private FlowPane     cardGrid;
    @FXML private VBox         tableCard;

    /* ─── Table ─── */
    @FXML private TextField                              searchField;
    @FXML private TableView<ActivitySchedule>            tableView;
    @FXML private TableColumn<ActivitySchedule, Integer> colIndex, colSpots;
    @FXML private TableColumn<ActivitySchedule, String>  colActivity, colStart, colEnd;
    @FXML private TableColumn<ActivitySchedule, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;

    /* ─── State ─── */
    private final ActivityScheduleService service         = new ActivityScheduleService();
    private final ActivityService         activityService = new ActivityService();
    private List<ActivitySchedule>    allData       = new ArrayList<>();
    private List<Activity>            allActivities = new ArrayList<>();
    private final Map<String, Integer> activityMap  = new LinkedHashMap<>();
    private ActivitySchedule scheduleEnEdition = null;
    private static final int PER_PAGE = 6;
    private int currentPage = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /* ══════════════════════════════════════════
       INITIALIZE
       ══════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cardGrid .setVisible(false); cardGrid .setManaged(false);
        tableCard.setVisible(true);  tableCard.setManaged(true);

        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 1; renderAll(); });

        loadActivities();
        setupColumns();
        refreshAll();
    }

    /* ══════════════════════════════════════════
       LOADERS
       ══════════════════════════════════════════ */
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

    /* ══════════════════════════════════════════
       DATA
       ══════════════════════════════════════════ */
    private void refreshAll() {
        try { allData = service.afficherAll(); }
        catch (SQLException e) { allData = new ArrayList<>(); showAlert("Erreur", e.getMessage()); }
        currentPage = 1;
        renderAll();
        updateStats();
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

    /* ══════════════════════════════════════════
       RENDER BOTH VIEWS
       ══════════════════════════════════════════ */
    private void renderAll() {
        String query = searchField.getText().toLowerCase().trim();

        List<ActivitySchedule> filtered = allData.stream()
                .filter(s -> query.isEmpty()
                        || s.getActivity().getTitle().toLowerCase().contains(query))
                .sorted(Comparator.comparing(ActivitySchedule::getStartAt))
                .collect(Collectors.toList());

        badgeCount.setText(String.valueOf(filtered.size()));

        // ── Card grid (not paginated) ──
        cardGrid.getChildren().clear();
        for (ActivitySchedule s : filtered)
            cardGrid.getChildren().add(buildScheduleCard(s));

        // ── Table paginated ──
        int total      = filtered.size();
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
            b.setOnAction(e -> { currentPage = pn; renderAll(); });
            pagButtons.getChildren().add(b);
        }
    }

    /* ══════════════════════════════════════════
       CARD BUILDER
       ══════════════════════════════════════════ */
    private VBox buildScheduleCard(ActivitySchedule s) {
        VBox card = new VBox(8);
        card.getStyleClass().add("eq-card");
        card.setPrefWidth(240);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-border-color:#3b82f6 transparent transparent transparent;"
                + "-fx-border-width:3 0 0 0;");

        Label iconLbl = new Label("📅");
        iconLbl.setStyle("-fx-font-size:32;");

        Label titleLbl = new Label(s.getActivity().getTitle());
        titleLbl.getStyleClass().add("eq-card-name");

        Label dateLbl = new Label("▶ " + s.getStartAt().format(FMT)
                + "\n◀ " + s.getEndAt().format(FMT));
        dateLbl.setWrapText(true);
        dateLbl.setStyle("-fx-font-size:12; -fx-text-fill:#64748b;");

        Label spotsLbl = new Label("🎯 " + s.getAvailableSpots() + " places");
        spotsLbl.setStyle("-fx-font-size:12; -fx-text-fill:#3b82f6; -fx-font-weight:bold;");

        Button editBtn = new Button("✏️");
        editBtn.getStyleClass().add("btn-edit");
        editBtn.setOnAction(e -> openEdit(s));

        Button delBtn = new Button("🗑️");
        delBtn.getStyleClass().add("btn-del");
        delBtn.setOnAction(e -> confirmDelete(s));

        HBox btns = new HBox(8, editBtn, delBtn);
        btns.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(iconLbl, titleLbl, dateLbl, spotsLbl, btns);
        return card;
    }

    /* ══════════════════════════════════════════
       VIEW TOGGLE
       ══════════════════════════════════════════ */
    @FXML private void onToggleCards() {
        cardGrid .setVisible(true);  cardGrid .setManaged(true);
        tableCard.setVisible(false); tableCard.setManaged(false);
        toggleCards.getStyleClass().add("vt-btn-active");
        toggleList .getStyleClass().remove("vt-btn-active");
    }

    @FXML private void onToggleList() {
        cardGrid .setVisible(false); cardGrid .setManaged(false);
        tableCard.setVisible(true);  tableCard.setManaged(true);
        toggleList .getStyleClass().add("vt-btn-active");
        toggleCards.getStyleClass().remove("vt-btn-active");
    }

    /* ══════════════════════════════════════════
       FORM PANEL
       ══════════════════════════════════════════ */
    @FXML private void onOpenForm() {
        scheduleEnEdition = null;
        formPanelTitle.setText("📅 Nouveau Planning");
        activityCombo.setValue(null);
        startAtField.clear(); endAtField.clear(); spotsField.clear();
        errActivity.setVisible(false); errActivity.setManaged(false);
        setFieldError(startAtField, errStartAt, false);
        setFieldError(endAtField,   errEndAt,   false);
        setFieldError(spotsField,   errSpots,   false);
        formPanel.setVisible(true); formPanel.setManaged(true);
        activityCombo.requestFocus();
    }

    @FXML private void onCloseForm() {
        formPanel.setVisible(false); formPanel.setManaged(false);
        scheduleEnEdition = null;
    }

    private void openEdit(ActivitySchedule s) {
        scheduleEnEdition = s;
        formPanelTitle.setText("✏️ Modifier le Planning");
        activityCombo.setValue(s.getActivity().getTitle());
        startAtField.setText(s.getStartAt().format(FMT));
        endAtField  .setText(s.getEndAt().format(FMT));
        spotsField  .setText(String.valueOf(s.getAvailableSpots()));
        errActivity.setVisible(false); errActivity.setManaged(false);
        setFieldError(startAtField, errStartAt, false);
        setFieldError(endAtField,   errEndAt,   false);
        setFieldError(spotsField,   errSpots,   false);
        formPanel.setVisible(true); formPanel.setManaged(true);
        startAtField.requestFocus();
    }

    /* ══════════════════════════════════════════
       SAVE
       ══════════════════════════════════════════ */
    @FXML private void onSave() {
        if (!validateAll()) return;

        ActivitySchedule s = scheduleEnEdition != null ? scheduleEnEdition : new ActivitySchedule();
        s.setStartAt(LocalDateTime.parse(startAtField.getText().trim(), FMT));
        s.setEndAt  (LocalDateTime.parse(endAtField  .getText().trim(), FMT));
        s.setAvailableSpots(Integer.parseInt(spotsField.getText().trim()));

        int actId = activityMap.getOrDefault(activityCombo.getValue(), 0);
        allActivities.stream().filter(a -> a.getId() == actId).findFirst().ifPresent(s::setActivity);

        try {
            if (scheduleEnEdition == null) service.ajouter(s);
            else                           service.modifier(s);
            onCloseForm();
            refreshAll();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    /* ══════════════════════════════════════════
       COLUMNS
       ══════════════════════════════════════════ */
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
                setText(getTableView().getItems().get(getIndex()).getStartAt().format(FMT));
            }
        });

        colEnd.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                setText(getTableView().getItems().get(getIndex()).getEndAt().format(FMT));
            }
        });

        colSpots.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label l = new Label(item + " 🎯");
                l.getStyleClass().add("td-price");
                setGraphic(l); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button delBtn  = new Button("🗑️");
            private final HBox   box     = new HBox(8, editBtn, delBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn .getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> openEdit(getTableView().getItems().get(getIndex())));
                delBtn .setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : box);
            }
        });
    }

    /* ══════════════════════════════════════════
       DELETE
       ══════════════════════════════════════════ */
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
                if (scheduleEnEdition != null && scheduleEnEdition.getId() == s.getId()) onCloseForm();
                refreshAll();
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    /* ══════════════════════════════════════════
       VALIDATION
       ══════════════════════════════════════════ */
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

    /* ══════════════════════════════════════════
       NAVIGATION + HELPERS
       ══════════════════════════════════════════ */
    @FXML private void onSearch()        { currentPage = 1; renderAll(); }
    @FXML private void onNavDashboard()  { onCloseForm(); SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void onNavActivities() { onCloseForm(); SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }

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

    private void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK) {{ setTitle(title); }}.showAndWait();
    }
}