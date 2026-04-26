package tn.esprit.controller.front.modals;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.models.cart.CartItem;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.services.activity.ActivityMapService;
import tn.esprit.services.activity.ActivityScheduleService;
import tn.esprit.utils.CartManager;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ActivityReservationController implements Initializable {

    private static final DateTimeFormatter SLOT_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM  •  HH:mm", Locale.FRANCE);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label activityNameLabel;
    @FXML private DatePicker activityDatePicker;
    @FXML private ComboBox<ActivitySchedule> scheduleCombo;
    @FXML private Label scheduleHintLabel;
    @FXML private Spinner<Integer> participantsSpinner;
    @FXML private Label maxParticipantsHint;
    @FXML private Label locationLabel;
    @FXML private Label durationLabel;
    @FXML private Label errDate;
    @FXML private Label errSchedule;
    @FXML private Label errParticipants;
    @FXML private Label errorLabel;
    @FXML private VBox priceStrip;
    @FXML private Label participantsSummary;
    @FXML private Label totalLabel;
    @FXML private WebView mapView;
    @FXML private Label mapStateLabel;
    @FXML private Button openMapBtn;

    private final ActivityScheduleService scheduleService = new ActivityScheduleService();
    private final List<ActivitySchedule> activitySchedules = new ArrayList<>();

    private Activity selectedActivity;
    private ActivitySchedule preselectedSchedule;
    private StackPane overlayRoot;
    private Runnable onCartUpdated;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activityDatePicker.valueProperty().addListener((o, oldValue, newValue) -> {
            updateScheduleOptions(newValue);
            updatePriceStrip();
        });
        participantsSpinner.valueProperty().addListener((o, oldValue, newValue) -> updatePriceStrip());
        scheduleCombo.valueProperty().addListener((o, oldValue, newValue) -> {
            updateCapacityFromSchedule();
            updatePriceStrip();
        });

        mapStateLabel.managedProperty().bind(mapStateLabel.visibleProperty());
        mapStateLabel.setVisible(false);
        scheduleHintLabel.managedProperty().bind(scheduleHintLabel.visibleProperty());
        scheduleHintLabel.setVisible(false);

        scheduleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ActivitySchedule schedule) {
                return schedule == null ? "" : formatScheduleLabel(schedule);
            }

            @Override
            public ActivitySchedule fromString(String string) {
                return null;
            }
        });

        setParticipantBounds(1, 1);
        refreshDatePicker();
    }

    public void setActivity(Activity activity) {
        this.selectedActivity = activity;
        activityNameLabel.setText(activity.getTitle() + " · " + buildCategoryText(activity));
        locationLabel.setText("📍 " + activity.getLocation());
        durationLabel.setText("⏱ " + formatDuration(activity.getDurationMinutes()));
        setParticipantBounds(activity.getMaxParticipants(), 1);
        loadSchedules();
        updateMapSection();
    }

    public void setPreselectedSchedule(ActivitySchedule schedule) {
        this.preselectedSchedule = schedule;
        if (schedule != null) {
            activityDatePicker.setValue(schedule.getStartAt().toLocalDate());
            if (selectedActivity != null) {
                updateScheduleOptions(schedule.getStartAt().toLocalDate());
                scheduleCombo.setValue(findMatchingSchedule(schedule));
                updateCapacityFromSchedule();
                updatePriceStrip();
            }
        }
    }

    public void setOverlayRoot(StackPane root) {
        this.overlayRoot = root;
    }

    public void setOnCartUpdated(Runnable callback) {
        this.onCartUpdated = callback;
    }

    @FXML
    private void onAddToCart() {
        clearErrors();
        if (!validateAll()) {
            return;
        }

        ActivitySchedule selectedSchedule = scheduleCombo.getValue();
        LocalDate reservationDate = selectedSchedule != null
                ? selectedSchedule.getStartAt().toLocalDate()
                : activityDatePicker.getValue();
        int participants = participantsSpinner.getValue();
        double total = selectedActivity.getPrice() * participants;

        Map<String, Object> details = new HashMap<>();
        details.put("activityId", selectedActivity.getId());
        details.put("activityTitle", selectedActivity.getTitle());
        details.put("date", reservationDate.toString());
        details.put("participants", participants);
        details.put("pricePerPerson", selectedActivity.getPrice());
        details.put("location", selectedActivity.getLocation());
        details.put("duration", selectedActivity.getDurationMinutes());
        details.put("latitude", selectedActivity.getLatitude());
        details.put("longitude", selectedActivity.getLongitude());

        if (selectedSchedule != null) {
            details.put("scheduleId", selectedSchedule.getId());
            details.put("scheduleStartAt", selectedSchedule.getStartAt().toString());
            details.put("scheduleEndAt", selectedSchedule.getEndAt().toString());
            details.put("availableSpots", selectedSchedule.getAvailableSpots());
        }

        CartItem item = new CartItem();
        item.setType(ReservationType.ACTIVITY);
        item.setItemId(selectedActivity.getId());
        item.setLabel(selectedActivity.getTitle());
        item.setUnitPrice(selectedActivity.getPrice());
        item.setTotalPrice(total);
        item.setDateFrom(reservationDate);
        item.setDateTo(reservationDate);
        item.setNumberOfPersons(participants);
        item.setDetails(details);

        CartManager.getInstance().addReservationItem(item);
        if (onCartUpdated != null) {
            onCartUpdated.run();
        }
        closeOverlay();
    }

    @FXML
    private void onClose() {
        closeOverlay();
    }

    @FXML
    private void onOpenMap() {
        if (selectedActivity == null || !ActivityMapService.hasValidCoordinates(selectedActivity)) {
            mapStateLabel.setText("Coordonnees indisponibles pour cette activite.");
            mapStateLabel.setVisible(true);
            mapStateLabel.getStyleClass().removeAll("res-map-status-error", "res-map-status-success");
            mapStateLabel.getStyleClass().add("res-map-status-error");
            return;
        }

        try {
            Desktop.getDesktop().browse(new URI(
                    ActivityMapService.buildOpenStreetMapUrl(
                            selectedActivity.getLatitude(),
                            selectedActivity.getLongitude()
                    )
            ));
        } catch (Exception exception) {
            errorLabel.setText("Impossible d'ouvrir la carte : " + exception.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void loadSchedules() {
        activitySchedules.clear();

        try {
            activitySchedules.addAll(scheduleService.afficherByActivity(selectedActivity.getId()).stream()
                    .filter(schedule -> schedule.getEndAt().isAfter(LocalDateTime.now()))
                    .sorted(Comparator.comparing(ActivitySchedule::getStartAt))
                    .collect(Collectors.toList()));
        } catch (Exception exception) {
            errorLabel.setText("Impossible de charger les creneaux : " + exception.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }

        refreshDatePicker();

        if (!activitySchedules.isEmpty()) {
            LocalDate initialDate = preselectedSchedule != null
                    ? preselectedSchedule.getStartAt().toLocalDate()
                    : activitySchedules.get(0).getStartAt().toLocalDate();
            activityDatePicker.setValue(initialDate);
            updateScheduleOptions(initialDate);

            if (preselectedSchedule != null) {
                scheduleCombo.setValue(findMatchingSchedule(preselectedSchedule));
            } else if (!scheduleCombo.getItems().isEmpty()) {
                scheduleCombo.setValue(scheduleCombo.getItems().get(0));
            }
        } else {
            scheduleCombo.getItems().clear();
            scheduleCombo.setDisable(true);
            scheduleHintLabel.setText("Aucun creneau publie pour cette activite.");
            scheduleHintLabel.setVisible(true);
        }

        updateCapacityFromSchedule();
    }

    private void refreshDatePicker() {
        activityDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                boolean isPast = date.isBefore(LocalDate.now());
                boolean hasScheduledSlot = activitySchedules.isEmpty()
                        || activitySchedules.stream().anyMatch(schedule -> schedule.getStartAt().toLocalDate().equals(date));
                setDisable(empty || isPast || !hasScheduledSlot);
            }
        });
    }

    private void updateScheduleOptions(LocalDate date) {
        scheduleCombo.getItems().clear();
        scheduleCombo.setDisable(false);

        if (date == null) {
            scheduleHintLabel.setVisible(false);
            return;
        }

        List<ActivitySchedule> daySchedules = activitySchedules.stream()
                .filter(schedule -> schedule.getStartAt().toLocalDate().equals(date))
                .sorted(Comparator.comparing(ActivitySchedule::getStartAt))
                .collect(Collectors.toList());

        if (daySchedules.isEmpty()) {
            scheduleCombo.setDisable(!activitySchedules.isEmpty());
            scheduleHintLabel.setText(activitySchedules.isEmpty()
                    ? "Cette activite n'a pas encore de planning publie."
                    : "Aucun depart sur cette date.");
            scheduleHintLabel.setVisible(true);
            return;
        }

        scheduleCombo.getItems().setAll(daySchedules);
        scheduleHintLabel.setText(daySchedules.size() + " creneau(x) disponible(s) ce jour.");
        scheduleHintLabel.setVisible(true);

        if (preselectedSchedule != null && preselectedSchedule.getStartAt().toLocalDate().equals(date)) {
            scheduleCombo.setValue(findMatchingSchedule(preselectedSchedule));
            preselectedSchedule = null;
            return;
        }

        scheduleCombo.setValue(daySchedules.get(0));
    }

    private void updateCapacityFromSchedule() {
        ActivitySchedule selectedSchedule = scheduleCombo.getValue();
        if (selectedActivity == null) {
            return;
        }

        if (selectedSchedule != null) {
            int maxAvailable = Math.max(1, selectedSchedule.getAvailableSpots());
            setParticipantBounds(maxAvailable, Math.min(participantsSpinner.getValue(), maxAvailable));
            maxParticipantsHint.setText(selectedSchedule.getAvailableSpots() + " places restantes");
            scheduleHintLabel.setText(
                    "Depart de " + TIME_FORMATTER.format(selectedSchedule.getStartAt()) +
                            " a " + TIME_FORMATTER.format(selectedSchedule.getEndAt())
            );
            scheduleHintLabel.setVisible(true);
            return;
        }

        setParticipantBounds(selectedActivity.getMaxParticipants(), 1);
        maxParticipantsHint.setText("max " + selectedActivity.getMaxParticipants() + " pers.");
    }

    private void updatePriceStrip() {
        LocalDate date = activityDatePicker.getValue();
        int participants = participantsSpinner.getValue();
        if (date == null || selectedActivity == null) {
            priceStrip.setVisible(false);
            priceStrip.setManaged(false);
            return;
        }

        ActivitySchedule selectedSchedule = scheduleCombo.getValue();
        String slotLabel = selectedSchedule != null
                ? " • " + TIME_FORMATTER.format(selectedSchedule.getStartAt())
                : "";
        double total = selectedActivity.getPrice() * participants;
        participantsSummary.setText(
                participants + " participant(s) × " + String.format(Locale.US, "%.0f", selectedActivity.getPrice()) +
                        " TND/pers." + slotLabel
        );
        totalLabel.setText(String.format(Locale.US, "%.2f", total));
        priceStrip.setVisible(true);
        priceStrip.setManaged(true);
    }

    private boolean validateAll() {
        boolean valid = true;
        if (activityDatePicker.getValue() == null) {
            showErr(errDate, "Date requise.");
            valid = false;
        }
        if (!activitySchedules.isEmpty() && scheduleCombo.getValue() == null) {
            showErr(errSchedule, "Choisissez un creneau disponible.");
            valid = false;
        }
        if (participantsSpinner.getValue() < 1) {
            showErr(errParticipants, "Au moins 1 participant.");
            valid = false;
        } else if (scheduleCombo.getValue() != null
                && participantsSpinner.getValue() > scheduleCombo.getValue().getAvailableSpots()) {
            showErr(errParticipants, "Le nombre depasse les places restantes.");
            valid = false;
        } else if (selectedActivity != null && participantsSpinner.getValue() > selectedActivity.getMaxParticipants()) {
            showErr(errParticipants, "Depasse la capacite max.");
            valid = false;
        }
        return valid;
    }

    private void showErr(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        for (Label label : new Label[]{errDate, errSchedule, errParticipants, errorLabel}) {
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void setParticipantBounds(int maxValue, int initialValue) {
        int safeMax = Math.max(1, maxValue);
        int safeInitial = Math.max(1, Math.min(initialValue, safeMax));
        participantsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, safeMax, safeInitial)
        );
    }

    private ActivitySchedule findMatchingSchedule(ActivitySchedule target) {
        return activitySchedules.stream()
                .filter(schedule -> schedule.getId() == target.getId())
                .findFirst()
                .orElse(target);
    }

    private void closeOverlay() {
        if (overlayRoot != null && overlayRoot.getParent() instanceof StackPane parent) {
            if (!parent.getChildren().isEmpty()) {
                parent.getChildren().get(0).setEffect(null);
            }
            parent.getChildren().remove(overlayRoot);
        }
    }

    private String buildCategoryText(Activity activity) {
        return activity.getCategory() != null ? activity.getCategory().getName() : "Activite";
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return mins == 0 ? hours + "h" : (hours > 0 ? hours + "h" : "") + mins + "min";
    }

    private String formatScheduleLabel(ActivitySchedule schedule) {
        return capitalize(schedule.getStartAt().format(SLOT_FORMATTER)) +
                "  →  " + TIME_FORMATTER.format(schedule.getEndAt());
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.FRANCE) + text.substring(1);
    }

    private void updateMapSection() {
        if (selectedActivity == null) {
            return;
        }

        if (!ActivityMapService.hasValidCoordinates(selectedActivity)) {
            mapView.getEngine().loadContent(
                    ActivityMapService.buildEmptyStateHtml(
                            "Carte indisponible",
                            "Cette activite ne contient pas encore de coordonnees exploitables."
                    )
            );
            openMapBtn.setDisable(true);
            mapStateLabel.setText("Aucune coordonnee valide pour le moment.");
            mapStateLabel.setVisible(true);
            mapStateLabel.getStyleClass().removeAll("res-map-status-error", "res-map-status-success");
            mapStateLabel.getStyleClass().add("res-map-status-error");
            return;
        }

        mapView.getEngine().loadContent(
                ActivityMapService.buildMapHtml(
                        selectedActivity.getTitle(),
                        selectedActivity.getLocation(),
                        selectedActivity.getLatitude(),
                        selectedActivity.getLongitude()
                )
        );
        openMapBtn.setDisable(false);
        mapStateLabel.setText("Localisation chargee pour cette activite.");
        mapStateLabel.setVisible(true);
        mapStateLabel.getStyleClass().removeAll("res-map-status-error", "res-map-status-success");
        mapStateLabel.getStyleClass().add("res-map-status-success");
    }
}
