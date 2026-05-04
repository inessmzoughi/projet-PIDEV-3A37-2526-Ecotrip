package tn.esprit.controller.front.modals;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.services.activity.ActivityScheduleService;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ActivityScheduleController implements Initializable {

    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label activityNameLabel;
    @FXML private Label activityMetaLabel;
    @FXML private Label selectionStateLabel;
    @FXML private Label selectedDateLabel;
    @FXML private Label emptySlotsLabel;
    @FXML private StackPane calendarHost;
    @FXML private VBox slotsBox;
    @FXML private Button reserveButton;

    private final ActivityScheduleService scheduleService = new ActivityScheduleService();
    private final List<ActivitySchedule> schedules = new ArrayList<>();

    private Activity selectedActivity;
    private StackPane overlayRoot;
    private Consumer<ActivitySchedule> onReserveRequested;
    private ActivitySchedule selectedSchedule;
    private CalendarView calendarView;
    private LocalDate focusedDate = LocalDate.now();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        buildCalendar();
        emptySlotsLabel.managedProperty().bind(emptySlotsLabel.visibleProperty());
        selectionStateLabel.managedProperty().bind(selectionStateLabel.visibleProperty());
        selectionStateLabel.setVisible(false);
        reserveButton.setDisable(true);
    }

    public void setActivity(Activity activity) {
        this.selectedActivity = activity;
        activityNameLabel.setText(activity.getTitle());
        activityMetaLabel.setText(buildMetaText(activity));
        loadSchedules();
    }

    public void setOverlayRoot(StackPane overlayRoot) {
        this.overlayRoot = overlayRoot;
    }

    public void setOnReserveRequested(Consumer<ActivitySchedule> onReserveRequested) {
        this.onReserveRequested = onReserveRequested;
    }

    @FXML
    private void onClose() {
        closeOverlay();
    }

    @FXML
    private void onReserve() {
        if (selectedSchedule != null && onReserveRequested != null) {
            onReserveRequested.accept(selectedSchedule);
        }
    }

    private void buildCalendar() {
        calendarView = new CalendarView();
        calendarView.showMonthPage();
        calendarView.dateProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                focusedDate = newDate;
                renderSlotsForDate(newDate);
            }
        });
        calendarHost.getChildren().setAll(calendarView);
    }

    private void loadSchedules() {
        schedules.clear();
        selectedSchedule = null;
        reserveButton.setDisable(true);

        try {
            schedules.addAll(scheduleService.afficherByActivity(selectedActivity.getId()).stream()
                    .sorted(Comparator.comparing(ActivitySchedule::getStartAt))
                    .collect(Collectors.toList()));
            renderCalendarEntries();
            focusInitialDate();
        } catch (Exception exception) {
            calendarHost.getChildren().clear();
            selectionStateLabel.setText("Impossible de charger le planning pour le moment.");
            selectionStateLabel.getStyleClass().removeAll(
                    "activity-schedule-status-success",
                    "activity-schedule-status-error"
            );
            selectionStateLabel.getStyleClass().add("activity-schedule-status-error");
            selectionStateLabel.setVisible(true);
            emptySlotsLabel.setVisible(false);
        }
    }

    private void renderCalendarEntries() {
        Calendar scheduleCalendar = new Calendar("Departs");
        scheduleCalendar.setStyle(Calendar.Style.STYLE2);

        for (ActivitySchedule schedule : schedules) {
            Entry<String> entry = new Entry<>(selectedActivity.getTitle());
            entry.changeStartDate(schedule.getStartAt().toLocalDate());
            entry.changeStartTime(schedule.getStartAt().toLocalTime());
            entry.changeEndDate(schedule.getEndAt().toLocalDate());
            entry.changeEndTime(schedule.getEndAt().toLocalTime());
            entry.setLocation(schedule.getActivity() != null ? schedule.getActivity().getTitle() : selectedActivity.getLocation());
            scheduleCalendar.addEntry(entry);
        }

        CalendarSource source = new CalendarSource("EcoTrip");
        source.getCalendars().add(scheduleCalendar);
        calendarView.getCalendarSources().setAll(source);
    }

    private void focusInitialDate() {
        if (schedules.isEmpty()) {
            focusedDate = LocalDate.now();
            selectedDateLabel.setText("Aucun depart planifie");
            slotsBox.getChildren().clear();
            emptySlotsLabel.setText("Cette activite n'a pas encore de creneaux publies.");
            emptySlotsLabel.setVisible(true);
            selectionStateLabel.setText("Le calendrier est pret. Les futurs departs apparaitront ici.");
            selectionStateLabel.getStyleClass().removeAll(
                    "activity-schedule-status-success",
                    "activity-schedule-status-error"
            );
            selectionStateLabel.getStyleClass().add("activity-schedule-status-error");
            selectionStateLabel.setVisible(true);
            return;
        }

        focusedDate = schedules.get(0).getStartAt().toLocalDate();
        calendarView.setDate(focusedDate);
        renderSlotsForDate(focusedDate);
        selectionStateLabel.setText(schedules.size() + " depart(s) disponibles pour cette activite.");
        selectionStateLabel.getStyleClass().removeAll(
                "activity-schedule-status-success",
                "activity-schedule-status-error"
        );
        selectionStateLabel.getStyleClass().add("activity-schedule-status-success");
        selectionStateLabel.setVisible(true);
    }

    private void renderSlotsForDate(LocalDate date) {
        selectedDateLabel.setText(capitalize(date.format(DAY_FORMATTER)));
        slotsBox.getChildren().clear();

        List<ActivitySchedule> daySchedules = schedules.stream()
                .filter(schedule -> schedule.getStartAt().toLocalDate().equals(date))
                .sorted(Comparator.comparing(ActivitySchedule::getStartAt))
                .collect(Collectors.toList());

        if (daySchedules.isEmpty()) {
            emptySlotsLabel.setText("Aucun depart programme pour cette journee.");
            emptySlotsLabel.setVisible(true);
            return;
        }

        emptySlotsLabel.setVisible(false);
        for (ActivitySchedule schedule : daySchedules) {
            slotsBox.getChildren().add(buildSlotCard(schedule));
        }

        if (selectedSchedule == null || !daySchedules.contains(selectedSchedule)) {
            selectSchedule(daySchedules.get(0));
        } else {
            refreshSlotSelection();
        }
    }

    private VBox buildSlotCard(ActivitySchedule schedule) {
        VBox card = new VBox(8);
        card.getStyleClass().add("activity-schedule-slot-card");
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setOnMouseClicked(event -> selectSchedule(schedule));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(formatScheduleTime(schedule));
        timeLabel.getStyleClass().add("activity-schedule-slot-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(schedule.getAvailableSpots() + " places");
        badge.getStyleClass().add("activity-schedule-slot-badge");

        row.getChildren().addAll(timeLabel, spacer, badge);

        Label summary = new Label(
                "Depart a " + TIME_FORMATTER.format(schedule.getStartAt()) +
                        " pour environ " + formatDurationMinutes(schedule)
        );
        summary.getStyleClass().add("activity-schedule-slot-summary");
        summary.setWrapText(true);

        card.getChildren().addAll(row, summary);
        card.setUserData(schedule);
        return card;
    }

    private void selectSchedule(ActivitySchedule schedule) {
        selectedSchedule = schedule;
        reserveButton.setDisable(false);
        selectionStateLabel.setText(
                "Creneau selectionne : " + capitalize(schedule.getStartAt().toLocalDate().format(DAY_FORMATTER)) +
                        " a " + TIME_FORMATTER.format(schedule.getStartAt())
        );
        selectionStateLabel.getStyleClass().removeAll(
                "activity-schedule-status-success",
                "activity-schedule-status-error"
        );
        selectionStateLabel.getStyleClass().add("activity-schedule-status-success");
        selectionStateLabel.setVisible(true);
        refreshSlotSelection();
    }

    private void refreshSlotSelection() {
        for (javafx.scene.Node node : slotsBox.getChildren()) {
            if (!(node instanceof VBox card)) {
                continue;
            }
            ActivitySchedule schedule = (ActivitySchedule) card.getUserData();
            boolean isSelected = schedule != null && schedule.equals(selectedSchedule);
            card.getStyleClass().remove("activity-schedule-slot-card-selected");
            if (isSelected) {
                card.getStyleClass().add("activity-schedule-slot-card-selected");
            }
        }
    }

    private void closeOverlay() {
        if (overlayRoot != null && overlayRoot.getParent() instanceof StackPane parent) {
            if (!parent.getChildren().isEmpty()) {
                parent.getChildren().get(0).setEffect(null);
            }
            parent.getChildren().remove(overlayRoot);
        }
    }

    private String buildMetaText(Activity activity) {
        String location = activity.getLocation() == null ? "Localisation a confirmer" : activity.getLocation();
        return location + "  •  " + String.format(Locale.US, "%.0f TND / personne", activity.getPrice());
    }

    private String formatScheduleTime(ActivitySchedule schedule) {
        return TIME_FORMATTER.format(schedule.getStartAt()) + " - " + TIME_FORMATTER.format(schedule.getEndAt());
    }

    private String formatDurationMinutes(ActivitySchedule schedule) {
        long minutes = java.time.Duration.between(schedule.getStartAt(), schedule.getEndAt()).toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remaining = minutes % 60;
        return remaining == 0 ? hours + "h" : hours + "h" + remaining + "min";
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.FRANCE) + text.substring(1);
    }
}
