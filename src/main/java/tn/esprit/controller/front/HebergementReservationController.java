package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.models.Chambre;
import tn.esprit.models.Hebergement;
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
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private Label      errCheckIn, errCheckOut, errGuests, errorLabel;
    @FXML private HBox       summaryBox;
    @FXML private Label      nightsLabel, totalLabel;

    private Hebergement selectedHebergement;
    private double      pricePerNight = 0.0;

    // Callback to notify the parent list controller that cart changed
    private Runnable onCartUpdated;

    private final Chambre_service chambreService = new Chambre_service();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Block past dates
        checkInPicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        checkOutPicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate minOut = checkInPicker.getValue() != null
                        ? checkInPicker.getValue().plusDays(1)
                        : LocalDate.now().plusDays(1);
                setDisable(empty || date.isBefore(minOut));
            }
        });

        // Auto-update summary when dates change
        checkInPicker.valueProperty().addListener((obs, o, n) -> updateSummary());
        checkOutPicker.valueProperty().addListener((obs, o, n) -> updateSummary());
    }

    // ── Called by HebergementsController before showing modal ────

    public void setHebergement(Hebergement h) {
        this.selectedHebergement = h;
        hebergementNameLabel.setText(h.getNom() + " — " + h.getVille());

        // Load minimum room price for this hebergement
        try {
            List<Chambre> chambres = chambreService.getByHebergement(h.getId());
            pricePerNight = chambres.stream()
                    .mapToDouble(Chambre::getPrix_par_nuit)
                    .min().orElse(0.0);
        } catch (SQLException e) {
            pricePerNight = 0.0;
        }
    }

    public void setOnCartUpdated(Runnable callback) {
        this.onCartUpdated = callback;
    }

    // ── Summary auto-calculation (mirrors the Symfony JS) ────────

    private void updateSummary() {
        LocalDate from = checkInPicker.getValue();
        LocalDate to   = checkOutPicker.getValue();

        if (from == null || to == null || !to.isAfter(from)) {
            summaryBox.setVisible(false);
            summaryBox.setManaged(false);
            return;
        }

        long nights = ChronoUnit.DAYS.between(from, to);
        double total = nights * pricePerNight;

        nightsLabel.setText(nights + " nuit" + (nights > 1 ? "s" : "")
                + (pricePerNight > 0 ? " × " + String.format("%.0f", pricePerNight) + " TND" : ""));
        totalLabel.setText(String.format("Total : %.2f TND", total));

        summaryBox.setVisible(true);
        summaryBox.setManaged(true);
    }

    // ── Add to cart ──────────────────────────────────────────────

    @FXML
    private void onAddToCart() {
        clearErrors();
        if (!validateAll()) return;

        LocalDate from   = checkInPicker.getValue();
        LocalDate to     = checkOutPicker.getValue();
        int guests       = guestsSpinner.getValue();
        long nights      = ChronoUnit.DAYS.between(from, to);
        double total     = nights * pricePerNight;

        // Build the details map — mirrors Symfony's CartService::addHebergement()
        // These will go into the JSON `details` column when finalizing
        Map<String, Object> details = new HashMap<>();
        details.put("nights",  nights);
        details.put("guests",  guests);
        details.put("dateFrom", from.toString());
        details.put("dateTo",   to.toString());
        details.put("pricePerNight", pricePerNight);
        details.put("hebergementId", selectedHebergement.getId());
        details.put("hebergementNom", selectedHebergement.getNom());

        CartItem item = new CartItem();
        item.setType(ReservationType.HEBERGEMENT);
        item.setItemId(selectedHebergement.getId());
        item.setLabel(selectedHebergement.getNom());
        item.setUnitPrice(pricePerNight);
        item.setTotalPrice(total);
        item.setDateFrom(from);
        item.setDateTo(to);
        item.setNumberOfPersons(guests);
        item.setDetails(details);

        CartManager.getInstance().addReservationItem(item);

        // Notify parent to update cart badge
        if (onCartUpdated != null) onCartUpdated.run();

        closeModal();
    }

    // ── Validation ───────────────────────────────────────────────

    private boolean validateAll() {
        boolean ok = true;
        LocalDate today = LocalDate.now();

        LocalDate from = checkInPicker.getValue();
        LocalDate to   = checkOutPicker.getValue();

        if (from == null) {
            showFieldError(errCheckIn, "La date d'arrivée est requise."); ok = false;
        } else if (from.isBefore(today)) {
            showFieldError(errCheckIn, "La date d'arrivée ne peut pas être dans le passé."); ok = false;
        }

        if (to == null) {
            showFieldError(errCheckOut, "La date de départ est requise."); ok = false;
        } else if (from != null && !to.isAfter(from)) {
            showFieldError(errCheckOut, "La date de départ doit être après la date d'arrivée."); ok = false;
        }

        if (guestsSpinner.getValue() < 1) {
            showFieldError(errGuests, "Au moins 1 personne requise."); ok = false;
        }

        return ok;
    }

    // ── Navigation ───────────────────────────────────────────────

    @FXML
    private void onClose() { closeModal(); }

    private void closeModal() {
        Stage stage = (Stage) hebergementNameLabel.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void showFieldError(Label label, String msg) {
        label.setText(msg); label.setVisible(true); label.setManaged(true);
    }

    private void clearErrors() {
        for (Label l : new Label[]{errCheckIn, errCheckOut, errGuests, errorLabel}) {
            l.setVisible(false); l.setManaged(false);
        }
    }
}