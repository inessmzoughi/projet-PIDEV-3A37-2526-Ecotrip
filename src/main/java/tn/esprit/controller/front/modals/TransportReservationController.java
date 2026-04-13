package tn.esprit.controller.front.modals;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.models.cart.CartItem;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.models.transport.Transport;
import tn.esprit.utils.CartManager;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class TransportReservationController implements Initializable {

    @FXML private Label      transportNameLabel;
    @FXML private DatePicker travelDatePicker;
    @FXML private Spinner<Integer> passengersSpinner;
    @FXML private Label      capacityHint;
    @FXML private Label      typeLabel, chauffeurLabel, co2Label;
    @FXML private Label      errDate, errPassengers, errorLabel;
    @FXML private VBox       priceStrip;
    @FXML private Label      passengersSummary, totalLabel;

    private Transport  selectedTransport;
    private StackPane  overlayRoot;
    private Runnable   onCartUpdated;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        travelDatePicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });
        travelDatePicker.valueProperty().addListener((o, old, n)  -> updatePriceStrip());
        passengersSpinner.valueProperty().addListener((o, old, n) -> updatePriceStrip());
    }

    public void setTransport(Transport t) {
        this.selectedTransport = t;
        transportNameLabel.setText(t.getType()
                + (t.getCategory() != null ? "  ·  " + t.getCategory().getName() : ""));
        typeLabel.setText(t.getType());
        chauffeurLabel.setText(t.getChauffeur() != null
                ? t.getChauffeur().getFullName() : "Non assigné");
        co2Label.setText(t.getEmissionCo2() + " g/km");
        capacityHint.setText("max " + t.getCapacite() + " passagers");

        passengersSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, t.getCapacite(), 1));
    }

    public void setOverlayRoot(StackPane root) { this.overlayRoot = root; }
    public void setOnCartUpdated(Runnable cb)  { this.onCartUpdated = cb; }

    private void updatePriceStrip() {
        LocalDate date = travelDatePicker.getValue();
        int passengers = passengersSpinner.getValue();
        if (date == null || selectedTransport == null) {
            priceStrip.setVisible(false); priceStrip.setManaged(false); return;
        }
        double total = selectedTransport.getPrixParPersonne() * passengers;
        passengersSummary.setText(passengers + " passager(s)  ×  "
                + String.format("%.2f", selectedTransport.getPrixParPersonne()) + " TND/pers.");
        totalLabel.setText(String.format("%.2f", total));
        priceStrip.setVisible(true); priceStrip.setManaged(true);
    }

    @FXML
    private void onAddToCart() {
        clearErrors();
        if (!validateAll()) return;

        LocalDate date = travelDatePicker.getValue();
        int passengers = passengersSpinner.getValue();
        double total   = selectedTransport.getPrixParPersonne() * passengers;

        Map<String, Object> details = new HashMap<>();
        details.put("transportId",    selectedTransport.getId());
        details.put("transportType",  selectedTransport.getType());
        details.put("date",           date.toString());
        details.put("passengers",     passengers);
        details.put("pricePerPerson", selectedTransport.getPrixParPersonne());
        details.put("chauffeur",      selectedTransport.getChauffeur() != null
                ? selectedTransport.getChauffeur().getFullName() : "");

        CartItem item = new CartItem();
        item.setType(ReservationType.TRANSPORT);
        item.setItemId(selectedTransport.getId());
        item.setLabel(selectedTransport.getType());
        item.setUnitPrice(selectedTransport.getPrixParPersonne());
        item.setTotalPrice(total);
        item.setDateFrom(date);
        item.setDateTo(date);
        item.setNumberOfPersons(passengers);
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
        if (travelDatePicker.getValue() == null) {
            showErr(errDate, "Date de voyage requise."); ok = false;
        }
        if (passengersSpinner.getValue() < 1) {
            showErr(errPassengers, "Au moins 1 passager."); ok = false;
        } else if (selectedTransport != null &&
                passengersSpinner.getValue() > selectedTransport.getCapacite()) {
            showErr(errPassengers, "Dépasse la capacité (" + selectedTransport.getCapacite() + ")."); ok = false;
        }
        return ok;
    }

    private void showErr(Label l, String m)  { l.setText(m); l.setVisible(true); l.setManaged(true); }
    private void clearErrors() {
        for (Label l : new Label[]{errDate, errPassengers, errorLabel})
        { l.setVisible(false); l.setManaged(false); }
    }
}