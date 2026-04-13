package tn.esprit.controller.front.modals;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.cart.CartItem;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.utils.CartManager;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ActivityReservationController implements Initializable {

    @FXML private Label      activityNameLabel;
    @FXML private DatePicker activityDatePicker;
    @FXML private Spinner<Integer> participantsSpinner;
    @FXML private Label      maxParticipantsHint;
    @FXML private Label      locationLabel, durationLabel;
    @FXML private Label      errDate, errParticipants, errorLabel;
    @FXML private VBox       priceStrip;
    @FXML private Label      participantsSummary, totalLabel;

    private Activity   selectedActivity;
    private StackPane  overlayRoot;
    private Runnable   onCartUpdated;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Block past dates
        activityDatePicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });
        activityDatePicker.valueProperty().addListener((o, old, n) -> updatePriceStrip());
        participantsSpinner.valueProperty().addListener((o, old, n) -> updatePriceStrip());
    }

    public void setActivity(Activity a) {
        this.selectedActivity = a;
        activityNameLabel.setText(a.getTitle() + "  ·  " + a.getCategory().getName());
        locationLabel.setText("📍 " + a.getLocation());
        durationLabel.setText("⏱ " + formatDuration(a.getDurationMinutes()));
        maxParticipantsHint.setText("max " + a.getMaxParticipants() + " pers.");

        // Set spinner max
        participantsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, a.getMaxParticipants(), 1));
    }

    public void setOverlayRoot(StackPane root)  { this.overlayRoot = root; }
    public void setOnCartUpdated(Runnable cb)   { this.onCartUpdated = cb; }

    private void updatePriceStrip() {
        LocalDate date = activityDatePicker.getValue();
        int participants = participantsSpinner.getValue();
        if (date == null || selectedActivity == null) {
            priceStrip.setVisible(false); priceStrip.setManaged(false); return;
        }
        double total = selectedActivity.getPrice() * participants;
        participantsSummary.setText(participants + " participant(s)  ×  "
                + String.format("%.0f", selectedActivity.getPrice()) + " TND/pers.");
        totalLabel.setText(String.format("%.2f", total));
        priceStrip.setVisible(true); priceStrip.setManaged(true);
    }

    @FXML
    private void onAddToCart() {
        clearErrors();
        if (!validateAll()) return;

        LocalDate date = activityDatePicker.getValue();
        int participants = participantsSpinner.getValue();
        double total = selectedActivity.getPrice() * participants;

        Map<String, Object> details = new HashMap<>();
        details.put("activityId",    selectedActivity.getId());
        details.put("activityTitle", selectedActivity.getTitle());
        details.put("date",          date.toString());
        details.put("participants",  participants);
        details.put("pricePerPerson", selectedActivity.getPrice());
        details.put("location",      selectedActivity.getLocation());
        details.put("duration",      selectedActivity.getDurationMinutes());

        CartItem item = new CartItem();
        item.setType(ReservationType.ACTIVITY);
        item.setItemId(selectedActivity.getId());
        item.setLabel(selectedActivity.getTitle());
        item.setUnitPrice(selectedActivity.getPrice());
        item.setTotalPrice(total);
        item.setDateFrom(date);
        item.setDateTo(date);   // same day for activities
        item.setNumberOfPersons(participants);
        item.setDetails(details);

        CartManager.getInstance().addReservationItem(item);
        if (onCartUpdated != null) onCartUpdated.run();
        closeOverlay();
    }

    @FXML private void onClose() { closeOverlay(); }

    private void closeOverlay() {
        if (overlayRoot != null && overlayRoot.getParent() instanceof StackPane parent) {
            if (!parent.getChildren().isEmpty())
                parent.getChildren().get(0).setEffect(null);
            parent.getChildren().remove(overlayRoot);
        }
    }

    private boolean validateAll() {
        boolean ok = true;
        if (activityDatePicker.getValue() == null) {
            showErr(errDate, "Date requise."); ok = false;
        }
        if (participantsSpinner.getValue() < 1) {
            showErr(errParticipants, "Au moins 1 participant."); ok = false;
        } else if (selectedActivity != null &&
                participantsSpinner.getValue() > selectedActivity.getMaxParticipants()) {
            showErr(errParticipants, "Dépasse la capacité max."); ok = false;
        }
        return ok;
    }

    private void showErr(Label l, String msg) { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    private void clearErrors() {
        for (Label l : new Label[]{errDate, errParticipants, errorLabel})
        { l.setVisible(false); l.setManaged(false); }
    }
    private String formatDuration(int min) {
        int h = min / 60, m = min % 60;
        return m == 0 ? h + "h" : (h > 0 ? h + "h" : "") + m + "min";
    }
}