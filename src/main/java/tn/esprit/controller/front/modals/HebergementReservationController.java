package tn.esprit.controller.front.modals;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.models.hebergements.Chambre;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.models.cart.CartItem;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.services.hebergement.Chambre_service;
import tn.esprit.utils.CartManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class HebergementReservationController implements Initializable {

    @FXML private Label      hebergementNameLabel;
    @FXML private DatePicker checkInPicker, checkOutPicker;
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private Label      errCheckIn, errCheckOut, errGuests, errorLabel;
    @FXML private VBox       priceStrip;
    @FXML private Label      nightsLabel, totalLabel;

    private Hebergement   selectedHebergement;
    private double        pricePerNight = 0.0;
    private StackPane     overlayRoot;   // reference to remove self from scene
    private Runnable      onCartUpdated;

    private final Chambre_service chambreService = new Chambre_service();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Block past dates on check-in
        checkInPicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty); setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });
        // Block dates before check-in on check-out
        checkOutPicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                LocalDate min = checkInPicker.getValue() != null
                        ? checkInPicker.getValue().plusDays(1)
                        : LocalDate.now().plusDays(1);
                setDisable(empty || d.isBefore(min));
            }
        });
        checkInPicker.valueProperty().addListener((o, old, n)  -> updatePriceStrip());
        checkOutPicker.valueProperty().addListener((o, old, n) -> updatePriceStrip());
    }

    public void setHebergement(Hebergement h) {
        this.selectedHebergement = h;
        hebergementNameLabel.setText(h.getNom() + "  ·  " + h.getVille());
        try {
            List<Chambre> chambres = chambreService.getByHebergement(h.getId());
            pricePerNight = chambres.stream().mapToDouble(Chambre::getPrix_par_nuit).min().orElse(0.0);
        } catch (SQLException e) { pricePerNight = 0.0; }
    }

    public void setOverlayRoot(StackPane root)    { this.overlayRoot = root; }
    public void setOnCartUpdated(Runnable cb)     { this.onCartUpdated = cb; }

    private void updatePriceStrip() {
        LocalDate from = checkInPicker.getValue();
        LocalDate to   = checkOutPicker.getValue();
        if (from == null || to == null || !to.isAfter(from)) {
            priceStrip.setVisible(false); priceStrip.setManaged(false); return;
        }
        long nights = ChronoUnit.DAYS.between(from, to);
        nightsLabel.setText(nights + " nuit" + (nights > 1 ? "s" : "")
                + (pricePerNight > 0 ? "  ×  " + String.format("%.0f", pricePerNight) + " TND/nuit" : ""));
        totalLabel.setText(String.format("%.2f", nights * pricePerNight));
        priceStrip.setVisible(true); priceStrip.setManaged(true);
    }

    @FXML
    private void onAddToCart() {
        clearErrors();
        if (!validateAll()) return;

        LocalDate from  = checkInPicker.getValue();
        LocalDate to    = checkOutPicker.getValue();
        int guests      = guestsSpinner.getValue();
        long nights     = ChronoUnit.DAYS.between(from, to);
        double total    = nights * pricePerNight;

        Map<String, Object> details = new HashMap<>();
        details.put("nights", nights);
        details.put("guests", guests);
        details.put("dateFrom", from.toString());
        details.put("dateTo", to.toString());
        details.put("pricePerNight", pricePerNight);
        details.put("hebergementId", selectedHebergement.getId());
        details.put("hebergementNom", selectedHebergement.getNom());

        CartItem item = new CartItem();
        item.setType(ReservationType.HEBERGEMENT);
        item.setItemId(selectedHebergement.getId());
        item.setLabel(selectedHebergement.getNom());
        item.setUnitPrice(pricePerNight);
        item.setTotalPrice(total);
        item.setDateFrom(from); item.setDateTo(to);
        item.setNumberOfPersons(guests);
        item.setDetails(details);

        CartManager.getInstance().addReservationItem(item);
        if (onCartUpdated != null) onCartUpdated.run();
        closeOverlay();
    }

    @FXML private void onClose() { closeOverlay(); }

    private void closeOverlay() {
        if (overlayRoot != null && overlayRoot.getParent() instanceof StackPane parent) {
            // Remove blur from the content underneath
            if (!parent.getChildren().isEmpty())
                parent.getChildren().get(0).setEffect(null);
            parent.getChildren().remove(overlayRoot);
        }
    }

    private boolean validateAll() {
        boolean ok = true;
        LocalDate today = LocalDate.now();
        LocalDate from  = checkInPicker.getValue();
        LocalDate to    = checkOutPicker.getValue();
        if (from == null) { showErr(errCheckIn, "Date d'arrivée requise."); ok = false; }
        else if (from.isBefore(today)) { showErr(errCheckIn, "Date dans le passé."); ok = false; }
        if (to == null) { showErr(errCheckOut, "Date de départ requise."); ok = false; }
        else if (from != null && !to.isAfter(from)) { showErr(errCheckOut, "Doit être après l'arrivée."); ok = false; }
        return ok;
    }

    private void showErr(Label l, String msg) { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    private void clearErrors() {
        for (Label l : new Label[]{errCheckIn, errCheckOut, errGuests, errorLabel}) {
            l.setVisible(false); l.setManaged(false);
        }
    }
}