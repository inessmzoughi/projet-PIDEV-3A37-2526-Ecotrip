package tn.esprit.controller.front;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.Calendar.Style;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.models.Reservation;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.repository.ReservationRepository;
import tn.esprit.services.reservation.ReservationService;
import tn.esprit.session.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class MesReservationsController implements Initializable {

    /* ─── Filters ─── */
    @FXML private ComboBox<String> filterType, filterStatus, sortSelect;
    @FXML private Label            resultCount;

    /* ─── View toggles ─── */
    @FXML private Button btnListView, btnCalView;

    /* ─── List view ─── */
    @FXML private HBox contentBox;
    @FXML private VBox itemsContainer;
    @FXML private VBox emptyState;
    @FXML private Label summaryCount, summaryTotal;
    @FXML private Label summaryPending, summaryConfirmed, summaryCancelled;

    /* ─── Edit panel ─── */
    @FXML private VBox       editPanel;
    @FXML private Label      editPanelSubtitle;
    @FXML private DatePicker editDateFrom, editDateTo;
    @FXML private Label      editDateToLabel;
    @FXML private Spinner<Integer> editPersonsSpinner;
    @FXML private Label      editSuccessLabel, editErrorLabel;
    @FXML private Label      errEditDateFrom, errEditDateTo, errEditPersons;
    @FXML private VBox       editPricePreview;
    @FXML private Label      editPriceBreakdown, editNewTotal;

    /* ─── Pagination ─── */
    @FXML private HBox   paginationBar;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label  pagInfo;

    /* ─── CalendarFX container ─── */
    @FXML private VBox calendarContainer;

    /* ─── CalendarFX objects ─── */
    private CalendarView  calendarView;
    private Calendar      calHeb;      // Hébergements — green
    private Calendar      calAct;      // Activités   — blue
    private Calendar      calTrans;    // Transports  — orange

    /* ─── State ─── */
    private final ReservationService    service    = new ReservationService();
    private final ReservationRepository repository = new ReservationRepository();
    private List<Reservation> allData       = new ArrayList<>();
    private List<Reservation> filteredData  = new ArrayList<>();
    private Reservation       editingReservation = null;
    private boolean           calendarMode  = false;

    private static final int PER_PAGE = 8;
    private int currentPage = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ═══════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        filterType.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        sortSelect.getSelectionModel().selectFirst();

        filterType.setOnAction(e   -> { currentPage = 1; applyFilters(); });
        filterStatus.setOnAction(e -> { currentPage = 1; applyFilters(); });
        sortSelect.setOnAction(e   -> { currentPage = 1; applyFilters(); });

        editDateFrom.valueProperty().addListener((o, old, n) -> recalcEditPrice());
        editDateTo.valueProperty().addListener((o, old, n)   -> recalcEditPrice());
        editPersonsSpinner.valueProperty().addListener((o, old, n) -> recalcEditPrice());

        // Build CalendarFX view once — reuse on every switch
        buildCalendarFX();

        loadData();
    }

    // ═══════════════════════════════════════════════════
    //  CALENDARFX SETUP
    // ═══════════════════════════════════════════════════

    private void buildCalendarFX() {
        // Three separate calendars — one per reservation type
        // Each gets a CalendarFX Style (STYLE1–STYLE7) for color coding
        calHeb   = new Calendar("Hébergements");
        calAct   = new Calendar("Activités");
        calTrans = new Calendar("Transports");

        calHeb.setStyle(Style.STYLE1);   // green  — matches heb theme
        calAct.setStyle(Style.STYLE2);   // blue   — matches activity theme
        calTrans.setStyle(Style.STYLE3); // orange — matches transport theme

        // Read-only — user sees entries but can't drag/create from calendar
        calHeb.setReadOnly(true);
        calAct.setReadOnly(true);
        calTrans.setReadOnly(true);

        CalendarSource source = new CalendarSource("Mes Réservations EcoTrip");
        source.getCalendars().addAll(calHeb, calAct, calTrans);

        // CalendarView is the main widget — supports Day/Week/Month/Year views
        calendarView = new CalendarView();
        calendarView.getCalendarSources().setAll(source);

        // Start on Month view — most useful for seeing all reservations
        calendarView.showMonthPage();

        // Set preferred height so it fills the container nicely
        calendarView.setPrefHeight(700);
        calendarView.setMinHeight(600);
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        // Disable creating new entries by clicking on the calendar
        calendarView.setEntryFactory(param -> null);

        // Disable the "new event" button in the toolbar
        calendarView.setShowAddCalendarButton(false);
        calendarView.setShowSearchField(false);
        calendarView.setShowPrintButton(false);

        // When user clicks an entry → show the edit panel for PENDING reservations
        calendarView.setEntryDetailsPopOverContentCallback(param -> {
            Entry<?> entry = param.getEntry();
            Object userObject = entry.getUserObject();
            if (userObject instanceof Reservation r) {
                Platform.runLater(() -> {
                    // Switch to list view and open the edit panel
                    onSwitchToList();
                    if (r.getStatus() == ReservationStatus.PENDING) {
                        openEditPanel(r);
                    }
                });
            }
            // Return null to suppress the default popover — we handle it ourselves
            return null;
        });

        // Add the CalendarFX view into the FXML container
        calendarContainer.getChildren().add(calendarView);
    }

    /** Populate the three CalendarFX calendars from filteredData */
    private void populateCalendar() {
        calHeb.clear();
        calAct.clear();
        calTrans.clear();

        for (Reservation r : filteredData) {
            if (r.getDateFrom() == null) continue;

            // Build the entry label
            String icon  = typeIcon(r);
            String label = icon + "  " + resolveLabel(r);
            String notes = String.format(
                    "%s\n%s → %s\n👥 %d pers.\n💰 %.2f TND",
                    statusLabel(r.getStatus()),
                    r.getDateFrom().format(FMT),
                    r.getDateTo() != null ? r.getDateTo().format(FMT) : r.getDateFrom().format(FMT),
                    r.getNumberOfPersons(),
                    r.getTotalPrice()
            );

            // dateFrom and dateTo for the entry
            LocalDateTime start = r.getDateFrom().atTime(LocalTime.of(9, 0));
            LocalDateTime end   = r.getDateTo() != null
                    ? r.getDateTo().atTime(LocalTime.of(18, 0))
                    : r.getDateFrom().atTime(LocalTime.of(18, 0));

            Entry<Reservation> entry = new Entry<>(label);
            entry.setInterval(start, end);
            entry.setFullDay(true);          // show as all-day event
            entry.setUserObject(r);          // attach the reservation for click handling
            entry.setTitle(label);
            entry.setLocation(notes);

            // Add to the correct calendar based on type
            switch (r.getReservationType()) {
                case HEBERGEMENT -> calHeb.addEntry(entry);
                case ACTIVITY    -> calAct.addEntry(entry);
                case TRANSPORT   -> calTrans.addEntry(entry);
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  VIEW TOGGLE
    // ═══════════════════════════════════════════════════

    @FXML
    public void onSwitchToList() {
        calendarMode = false;
        btnListView.getStyleClass().add("view-toggle-active");
        btnCalView.getStyleClass().remove("view-toggle-active");

        calendarContainer.setVisible(false); calendarContainer.setManaged(false);
        renderPage();
    }

    @FXML
    public void onSwitchToCalendar() {
        calendarMode = true;
        btnCalView.getStyleClass().add("view-toggle-active");
        btnListView.getStyleClass().remove("view-toggle-active");

        // Hide list-only UI
        contentBox.setVisible(false);    contentBox.setManaged(false);
        paginationBar.setVisible(false); paginationBar.setManaged(false);
        emptyState.setVisible(false);    emptyState.setManaged(false);
        editPanel.setVisible(false);     editPanel.setManaged(false);

        // Show calendar container
        calendarContainer.setVisible(true); calendarContainer.setManaged(true);

        // Populate with current filtered data
        populateCalendar();

        resultCount.setText(filteredData.size() + " réservation"
                + (filteredData.size() > 1 ? "s" : "") + " affichée"
                + (filteredData.size() > 1 ? "s" : ""));
    }

    // ═══════════════════════════════════════════════════
    //  DATA
    // ═══════════════════════════════════════════════════

    private void loadData() {
        try {
            allData = service.getMyReservations();
        } catch (SQLException e) {
            allData = new ArrayList<>();
        }
        applyFilters();
    }

    private void applyFilters() {
        String type   = filterType.getValue();
        String status = filterStatus.getValue();
        String sort   = sortSelect.getValue();

        filteredData = allData.stream()
                .filter(r -> type == null || type.equals("Tous les types")
                        || r.getReservationType().name().equals(type))
                .filter(r -> status == null || status.equals("Tous les statuts")
                        || r.getStatus().name().equals(status))
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Plus récentes"    -> filteredData.sort(
                    Comparator.comparing(Reservation::getCreatedAt).reversed());
            case "Plus anciennes"   -> filteredData.sort(
                    Comparator.comparing(Reservation::getCreatedAt));
            case "Prix croissant"   -> filteredData.sort(
                    Comparator.comparingDouble(Reservation::getTotalPrice));
            case "Prix décroissant" -> filteredData.sort(
                    Comparator.comparingDouble(Reservation::getTotalPrice).reversed());
            default -> {}
        }

        currentPage = 1;
        if (calendarMode) {
            populateCalendar();
        } else {
            renderPage();
        }
        updateSummary();
    }

    // ═══════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════

    private void renderPage() {
        itemsContainer.getChildren().clear();
        calendarContainer.setVisible(false); calendarContainer.setManaged(false);

        boolean isEmpty = filteredData == null || filteredData.isEmpty();
        emptyState.setVisible(isEmpty);   emptyState.setManaged(isEmpty);
        contentBox.setVisible(!isEmpty);  contentBox.setManaged(!isEmpty);

        int total = isEmpty ? 0 : filteredData.size();
        resultCount.setText(total + " Réservation" + (total > 1 ? "s" : "")
                + " Trouvée" + (total > 1 ? "s" : ""));

        if (isEmpty) {
            paginationBar.setVisible(false); paginationBar.setManaged(false);
            return;
        }

        int totalPages = (int) Math.ceil((double) total / PER_PAGE);
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1)         currentPage = 1;

        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);

        filteredData.subList(from, to)
                .forEach(r -> itemsContainer.getChildren().add(buildRow(r)));

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
    }

    private HBox buildRow(Reservation r) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18));
        row.getStyleClass().add("res-row");

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinWidth(56); iconBox.setMaxWidth(56);
        iconBox.setMinHeight(56); iconBox.setMaxHeight(56);
        iconBox.getStyleClass().add("res-row-icon-"
                + r.getReservationType().name().toLowerCase());
        Label icon = new Label(typeIcon(r));
        icon.setStyle("-fx-font-size:24px;");
        iconBox.getChildren().add(icon);

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(resolveLabel(r));
        nameLabel.setStyle(
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#0d2b1a;");
        Label statusBadge = new Label(statusLabel(r.getStatus()));
        statusBadge.getStyleClass().add(statusStyleClass(r.getStatus()));
        titleRow.getChildren().addAll(nameLabel, statusBadge);

        String dateText = r.getDateFrom() != null
                ? "📅 " + r.getDateFrom().format(FMT)
                + (r.getDateTo() != null && !r.getDateTo().equals(r.getDateFrom())
                ? "  →  " + r.getDateTo().format(FMT) : "")
                : "";
        Label datesLabel = new Label(dateText);
        datesLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;");

        HBox tagsRow = new HBox(8);
        tagsRow.setAlignment(Pos.CENTER_LEFT);
        Label typeTag = new Label(typeIcon(r) + "  " + typeLabel(r));
        typeTag.getStyleClass().add("heb-card-equipement");
        Label personsTag = new Label("👥 " + r.getNumberOfPersons() + " pers.");
        personsTag.getStyleClass().add("heb-card-equipement");
        tagsRow.getChildren().addAll(typeTag, personsTag);

        info.getChildren().addAll(titleRow, datesLabel, tagsRow);

        Label totalLabel = new Label(String.format("%.2f TND", r.getTotalPrice()));
        totalLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;"
                + "-fx-text-fill:#1a5f2a;-fx-min-width:110px;");
        totalLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        if (r.getStatus() == ReservationStatus.PENDING) {
            Button btnEdit = new Button("✏️ Modifier");
            btnEdit.setStyle(
                    "-fx-background-color:#dbeafe;-fx-text-fill:#1e40af;"
                            + "-fx-font-size:12px;-fx-font-weight:bold;"
                            + "-fx-background-radius:8;-fx-cursor:hand;-fx-border-width:0;"
                            + "-fx-padding:8 14;");
            btnEdit.setOnAction(e -> openEditPanel(r));
            Button btnDel = new Button("🗑️");
            btnDel.setStyle(
                    "-fx-background-color:#fee2e2;-fx-text-fill:#c62828;"
                            + "-fx-font-size:14px;-fx-background-radius:8;"
                            + "-fx-cursor:hand;-fx-border-width:0;-fx-padding:8 12;");
            btnDel.setOnAction(e -> confirmDelete(r));
            actions.getChildren().addAll(btnEdit, btnDel);
        } else {
            Label locked = new Label(r.getStatus() == ReservationStatus.CONFIRMED
                    ? "🔒 Confirmée" : "🚫 Annulée");
            locked.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
            actions.getChildren().add(locked);
        }

        if (editingReservation != null && editingReservation.getId() == r.getId()) {
            row.setStyle("-fx-border-color:#2d5016;-fx-border-width:2;"
                    + "-fx-border-radius:14;");
        }

        row.getChildren().addAll(iconBox, info, totalLabel, actions);
        return row;
    }

    // ═══════════════════════════════════════════════════
    //  SUMMARY
    // ═══════════════════════════════════════════════════

    private void updateSummary() {
        long pending   = allData.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
        long confirmed = allData.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
        long cancelled = allData.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();
        double total   = allData.stream()
                .mapToDouble(Reservation::getTotalPrice).sum();

        summaryCount.setText(String.valueOf(allData.size()));
        summaryTotal.setText(String.format("%.2f TND", total));
        summaryPending.setText(String.valueOf(pending));
        summaryConfirmed.setText(String.valueOf(confirmed));
        summaryCancelled.setText(String.valueOf(cancelled));
    }

    // ═══════════════════════════════════════════════════
    //  INLINE EDIT PANEL
    // ═══════════════════════════════════════════════════

    private void openEditPanel(Reservation r) {
        editingReservation = r;
        editPanelSubtitle.setText(typeIcon(r) + "  " + resolveLabel(r));
        editDateFrom.setValue(r.getDateFrom());
        editDateTo.setValue(r.getDateTo());

        boolean sameDay = r.getDateTo() == null || r.getDateTo().equals(r.getDateFrom());
        editDateToLabel.setVisible(!sameDay); editDateToLabel.setManaged(!sameDay);
        editDateTo.setVisible(!sameDay);      editDateTo.setManaged(!sameDay);

        editPersonsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 50, r.getNumberOfPersons()));

        clearEditErrors();
        editPanel.setVisible(true); editPanel.setManaged(true);
        renderPage();
    }

    @FXML
    public void onCloseEdit() {
        editingReservation = null;
        editPanel.setVisible(false); editPanel.setManaged(false);
        clearEditErrors();
        renderPage();
    }

    private void recalcEditPrice() {
        if (editingReservation == null) return;
        LocalDate from = editDateFrom.getValue();
        LocalDate to   = editDateTo.getValue();
        int persons    = editPersonsSpinner.getValue();
        if (from == null) { hidePricePreview(); return; }

        double newTotal;
        String breakdown;
        switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> {
                if (to == null || !to.isAfter(from)) { hidePricePreview(); return; }
                long nights = ChronoUnit.DAYS.between(from, to);
                double ppu  = getPricePerUnit();
                newTotal    = nights * ppu;
                breakdown   = nights + " nuit(s)  ×  "
                        + String.format("%.2f", ppu) + " TND";
            }
            case ACTIVITY, TRANSPORT -> {
                double ppu = getPricePerUnit();
                newTotal   = ppu * persons;
                breakdown  = persons + " pers.  ×  "
                        + String.format("%.2f", ppu) + " TND";
            }
            default -> { hidePricePreview(); return; }
        }
        editPriceBreakdown.setText(breakdown);
        editNewTotal.setText(String.format("%.2f", newTotal));
        editPricePreview.setVisible(true); editPricePreview.setManaged(true);
    }

    private double getPricePerUnit() {
        if (editingReservation == null) return 0;
        Map<String, Object> details = editingReservation.getDetails();
        if (details != null) {
            Object key = switch (editingReservation.getReservationType()) {
                case HEBERGEMENT -> details.get("pricePerNight");
                case ACTIVITY, TRANSPORT -> details.get("pricePerPerson");
            };
            if (key instanceof Number) return ((Number) key).doubleValue();
        }
        return switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> {
                long n = editingReservation.getNights();
                yield n > 0 ? editingReservation.getTotalPrice() / n : 0;
            }
            case ACTIVITY, TRANSPORT -> {
                int p = editingReservation.getNumberOfPersons();
                yield p > 0 ? editingReservation.getTotalPrice() / p : 0;
            }
        };
    }

    private void hidePricePreview() {
        editPricePreview.setVisible(false); editPricePreview.setManaged(false);
    }

    @FXML
    public void onSaveEdit() {
        if (editingReservation == null) return;
        clearEditErrors();
        if (!validateEdit()) return;

        LocalDate from  = editDateFrom.getValue();
        LocalDate to    = editDateTo.isVisible() ? editDateTo.getValue() : from;
        int persons     = editPersonsSpinner.getValue();
        double newTotal = recalcTotal(from, to, persons);

        Map<String, Object> details = editingReservation.getDetails();
        if (details == null) details = new HashMap<>();
        switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> {
                long nights = ChronoUnit.DAYS.between(from, to);
                details.put("nights", nights);
                details.put("guests", persons);
                details.put("dateFrom", from.toString());
                details.put("dateTo", to.toString());
            }
            case ACTIVITY -> {
                details.put("date", from.toString());
                details.put("participants", persons);
            }
            case TRANSPORT -> {
                details.put("date", from.toString());
                details.put("passengers", persons);
            }
        }

        editingReservation.setDateFrom(from);
        editingReservation.setDateTo(to);
        editingReservation.setNumberOfPersons(persons);
        editingReservation.setTotalPrice(newTotal);
        editingReservation.setDetails(details);

        try {
            repository.update(editingReservation);
            showEditSuccess("✅ Réservation mise à jour !");
            loadData();
            editingReservation = null;
            Platform.runLater(() -> {
                editPanel.setVisible(false); editPanel.setManaged(false);
                // Refresh calendar if it was open
                if (calendarMode) populateCalendar();
            });
        } catch (SQLException e) {
            showEditError("Erreur : " + e.getMessage());
        }
    }

    private double recalcTotal(LocalDate from, LocalDate to, int persons) {
        double u = getPricePerUnit();
        return switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> ChronoUnit.DAYS.between(from, to) * u;
            case ACTIVITY, TRANSPORT -> persons * u;
        };
    }

    private boolean validateEdit() {
        boolean ok = true;
        LocalDate from = editDateFrom.getValue();
        LocalDate to   = editDateTo.isVisible() ? editDateTo.getValue() : null;
        if (from == null) {
            showFieldError(errEditDateFrom, "Date requise."); ok = false;
        } else if (from.isBefore(LocalDate.now())) {
            showFieldError(errEditDateFrom, "Date dans le passé."); ok = false;
        }
        if (editDateTo.isVisible()) {
            if (to == null) {
                showFieldError(errEditDateTo, "Date de fin requise."); ok = false;
            } else if (from != null && !to.isAfter(from)) {
                showFieldError(errEditDateTo, "Doit être après la date de début."); ok = false;
            }
        }
        if (editPersonsSpinner.getValue() < 1) {
            showFieldError(errEditPersons, "Au moins 1 personne."); ok = false;
        }
        return ok;
    }

    // ═══════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════

    private void confirmDelete(Reservation r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler ?");
        alert.setHeaderText("Annuler « " + resolveLabel(r) + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Retour",  ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Annuler", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.delete(r.getId());
                if (editingReservation != null && editingReservation.getId() == r.getId())
                    onCloseEdit();
                loadData();
                if (calendarMode) populateCalendar();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
            }
        });
    }

    // ═══════════════════════════════════════════════════
    //  PAGINATION
    // ═══════════════════════════════════════════════════

    @FXML private void onPrev() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML private void onNext() {
        int tp = (int) Math.ceil((double) filteredData.size() / PER_PAGE);
        if (currentPage < tp) { currentPage++; renderPage(); }
    }

    @FXML private void onReset() {
        filterType.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        sortSelect.getSelectionModel().selectFirst();
        filteredData = allData;
        currentPage  = 1;
        if (calendarMode) populateCalendar(); else renderPage();
    }

    // ═══════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════

    private String typeIcon(Reservation r) {
        return switch (r.getReservationType()) {
            case HEBERGEMENT -> "🏨";
            case ACTIVITY    -> "🧭";
            case TRANSPORT   -> "🚌";
        };
    }

    private String typeLabel(Reservation r) {
        return switch (r.getReservationType()) {
            case HEBERGEMENT -> "Hébergement";
            case ACTIVITY    -> "Activité";
            case TRANSPORT   -> "Transport";
        };
    }

    private String resolveLabel(Reservation r) {
        if (r.getDetails() != null) {
            Object name = r.getDetails().get(switch (r.getReservationType()) {
                case HEBERGEMENT -> "hebergementNom";
                case ACTIVITY    -> "activityTitle";
                case TRANSPORT   -> "transportType";
            });
            if (name != null) return name.toString();
        }
        return r.getReservationType().name() + " #" + r.getReservationId();
    }

    private String statusLabel(ReservationStatus s) {
        return switch (s) {
            case PENDING   -> "⏳ En attente";
            case CONFIRMED -> "✅ Confirmée";
            case CANCELLED -> "❌ Annulée";
        };
    }

    private String statusStyleClass(ReservationStatus s) {
        return switch (s) {
            case PENDING   -> "heb-card-category";
            case CONFIRMED -> "badge-actif";
            case CANCELLED -> "badge-inactif";
        };
    }

    private void showEditSuccess(String msg) {
        editSuccessLabel.setText(msg);
        editSuccessLabel.setVisible(true); editSuccessLabel.setManaged(true);
        editErrorLabel.setVisible(false);  editErrorLabel.setManaged(false);
    }

    private void showEditError(String msg) {
        editErrorLabel.setText(msg);
        editErrorLabel.setVisible(true); editErrorLabel.setManaged(true);
    }

    private void showFieldError(Label l, String msg) {
        l.setText(msg); l.setVisible(true); l.setManaged(true);
    }

    private void clearEditErrors() {
        for (Label l : new Label[]{editSuccessLabel, editErrorLabel,
                errEditDateFrom, errEditDateTo, errEditPersons})
        { l.setVisible(false); l.setManaged(false); }
        hidePricePreview();
    }
}