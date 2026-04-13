package tn.esprit.controller.front;

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
import tn.esprit.services.ReservationService;
import tn.esprit.session.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class MesReservationsController implements Initializable {

    /* ─── Filters / result count ─── */
    @FXML private ComboBox<String> filterType, filterStatus, sortSelect;
    @FXML private Label            resultCount;

    /* ─── Main layout ─── */
    @FXML private HBox contentBox;
    @FXML private VBox itemsContainer;
    @FXML private VBox emptyState;

    /* ─── Summary panel (right) ─── */
    @FXML private Label summaryCount, summaryTotal;
    @FXML private Label summaryPending, summaryConfirmed, summaryCancelled;

    /* ─── Inline edit panel ─── */
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

    /* ─── State ─── */
    private final ReservationService    service    = new ReservationService();
    private List<Reservation> allData      = new ArrayList<>();
    private List<Reservation> filteredData = new ArrayList<>();
    private Reservation       editingReservation = null;

    private static final int PER_PAGE = 8;
    private int currentPage = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /* ═══════════════════════════════════════════════════ */
    /*  INIT                                               */
    /* ═══════════════════════════════════════════════════ */

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

        // Wire date listeners for price recalculation in edit panel
        editDateFrom.valueProperty().addListener((o, old, n) -> recalcEditPrice());
        editDateTo.valueProperty().addListener((o, old, n)   -> recalcEditPrice());
        editPersonsSpinner.valueProperty().addListener((o, old, n) -> recalcEditPrice());

        loadData();
    }

    /* ═══════════════════════════════════════════════════ */
    /*  DATA                                               */
    /* ═══════════════════════════════════════════════════ */

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
        renderPage();
    }

    /* ═══════════════════════════════════════════════════ */
    /*  RENDER — cart-style rows                           */
    /* ═══════════════════════════════════════════════════ */

    private void renderPage() {
        itemsContainer.getChildren().clear();

        boolean isEmpty = filteredData == null || filteredData.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        contentBox.setVisible(!isEmpty);
        contentBox.setManaged(!isEmpty);

        int total = isEmpty ? 0 : filteredData.size();
        resultCount.setText(total + " Réservation" + (total > 1 ? "s" : "")
                + " Trouvée" + (total > 1 ? "s" : ""));

        if (isEmpty) {
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            updateSummary();
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

        updateSummary();
    }

    /* ─── Single row — same structure as CartController.buildItemRow() ─── */
    private HBox buildRow(Reservation r) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18));
        row.getStyleClass().add("res-row");

        // ── Type icon ──
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinWidth(56); iconBox.setMaxWidth(56);
        iconBox.setMinHeight(56); iconBox.setMaxHeight(56);
        iconBox.getStyleClass().add("res-row-icon-" + r.getReservationType().name().toLowerCase());
        Label icon = new Label(typeIcon(r));
        icon.setStyle("-fx-font-size:24px;");
        iconBox.getChildren().add(icon);

        // ── Info ──
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Title row: item name + status badge
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(resolveLabel(r));
        nameLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#0d2b1a;");
        Label statusBadge = new Label(statusLabel(r.getStatus()));
        statusBadge.getStyleClass().add(statusStyleClass(r.getStatus()));
        titleRow.getChildren().addAll(nameLabel, statusBadge);

        // Subtitle row: type + dates
        String dateText = r.getDateFrom() != null
                ? "📅 " + r.getDateFrom().format(FMT)
                + (r.getDateTo() != null && !r.getDateTo().equals(r.getDateFrom())
                ? "  →  " + r.getDateTo().format(FMT) : "")
                : "";
        Label datesLabel = new Label(dateText);
        datesLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;");

        // Tags row: type + persons
        HBox tagsRow = new HBox(8);
        tagsRow.setAlignment(Pos.CENTER_LEFT);
        Label typeTag = new Label(typeIcon(r) + "  " + typeLabel(r));
        typeTag.getStyleClass().add("heb-card-equipement");
        Label personsTag = new Label("👥 " + r.getNumberOfPersons() + " pers.");
        personsTag.getStyleClass().add("heb-card-equipement");
        tagsRow.getChildren().addAll(typeTag, personsTag);

        info.getChildren().addAll(titleRow, datesLabel, tagsRow);

        // ── Price ──
        Label totalLabel = new Label(String.format("%.2f TND", r.getTotalPrice()));
        totalLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;"
                + "-fx-text-fill:#1a5f2a;-fx-min-width:110px;");
        totalLabel.setAlignment(Pos.CENTER_RIGHT);

        // ── Action buttons ──
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
            // Read-only state indicator
            Label locked = new Label(r.getStatus() == ReservationStatus.CONFIRMED
                    ? "🔒 Confirmée" : "🚫 Annulée");
            locked.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
            actions.getChildren().add(locked);
        }

        row.getChildren().addAll(iconBox, info, totalLabel, actions);

        // Highlight if currently being edited
        if (editingReservation != null && editingReservation.getId() == r.getId()) {
            row.setStyle(row.getStyle()
                    + "-fx-border-color:#2d5016;-fx-border-width:2;-fx-border-radius:14;");
        }

        return row;
    }

    /* ═══════════════════════════════════════════════════ */
    /*  SUMMARY PANEL                                      */
    /* ═══════════════════════════════════════════════════ */

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

    /* ═══════════════════════════════════════════════════ */
    /*  INLINE EDIT PANEL                                  */
    /* ═══════════════════════════════════════════════════ */

    private void openEditPanel(Reservation r) {
        editingReservation = r;

        // Panel subtitle
        editPanelSubtitle.setText(typeIcon(r) + "  " + resolveLabel(r));

        // Pre-fill fields
        editDateFrom.setValue(r.getDateFrom());
        editDateTo.setValue(r.getDateTo());

        // For activities (same-day), hide date-to
        boolean sameDay = r.getDateTo() == null || r.getDateTo().equals(r.getDateFrom());
        editDateToLabel.setVisible(!sameDay);
        editDateToLabel.setManaged(!sameDay);
        editDateTo.setVisible(!sameDay);
        editDateTo.setManaged(!sameDay);

        // Max persons from reservation type context
        editPersonsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, r.getNumberOfPersons()));

        clearEditErrors();
        editPanel.setVisible(true);
        editPanel.setManaged(true);

        // Scroll to show edit panel — re-render rows to highlight the selected one
        renderPage();
    }

    @FXML
    public void onCloseEdit() {
        editingReservation = null;
        editPanel.setVisible(false);
        editPanel.setManaged(false);
        clearEditErrors();
        renderPage(); // remove highlight
    }

    /* ─── Recalculate price preview as user changes fields ─── */
    private void recalcEditPrice() {
        if (editingReservation == null) return;

        LocalDate from    = editDateFrom.getValue();
        LocalDate to      = editDateTo.getValue();
        int persons       = editPersonsSpinner.getValue();

        if (from == null) { hidePricePreview(); return; }

        double newTotal;
        String breakdown;

        switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> {
                if (to == null || !to.isAfter(from)) { hidePricePreview(); return; }
                long nights = ChronoUnit.DAYS.between(from, to);
                double pricePer = getPricePerUnit();
                newTotal  = nights * pricePer;
                breakdown = nights + " nuit(s)  ×  " + String.format("%.2f", pricePer) + " TND";
            }
            case ACTIVITY -> {
                double pricePer = getPricePerUnit();
                newTotal  = pricePer * persons;
                breakdown = persons + " pers.  ×  " + String.format("%.2f", pricePer) + " TND";
            }
            case TRANSPORT -> {
                double pricePer = getPricePerUnit();
                newTotal  = pricePer * persons;
                breakdown = persons + " pass.  ×  " + String.format("%.2f", pricePer) + " TND";
            }
            default -> { hidePricePreview(); return; }
        }

        editPriceBreakdown.setText(breakdown);
        editNewTotal.setText(String.format("%.2f", newTotal));
        editPricePreview.setVisible(true);
        editPricePreview.setManaged(true);
    }

    private double getPricePerUnit() {
        if (editingReservation == null) return 0;
        // Extract price per unit from the stored details map
        Map<String, Object> details = editingReservation.getDetails();
        if (details == null) {
            // Fallback: derive from total and original quantity
            return deriveUnitPrice();
        }
        Object key = switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> details.get("pricePerNight");
            case ACTIVITY    -> details.get("pricePerPerson");
            case TRANSPORT   -> details.get("pricePerPerson");
        };
        if (key instanceof Number) return ((Number) key).doubleValue();
        return deriveUnitPrice();
    }

    private double deriveUnitPrice() {
        // Fallback: original total / original persons or nights
        if (editingReservation == null) return 0;
        Map<String, Object> details = editingReservation.getDetails();
        return switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> {
                long nights = editingReservation.getNights();
                yield nights > 0 ? editingReservation.getTotalPrice() / nights : 0;
            }
            case ACTIVITY, TRANSPORT -> {
                int persons = editingReservation.getNumberOfPersons();
                yield persons > 0 ? editingReservation.getTotalPrice() / persons : 0;
            }
        };
    }

    private void hidePricePreview() {
        editPricePreview.setVisible(false);
        editPricePreview.setManaged(false);
    }

    /* ─── Save edit ─── */
    @FXML
    public void onSaveEdit() {
        if (editingReservation == null) return;
        clearEditErrors();
        if (!validateEdit()) return;

        LocalDate from  = editDateFrom.getValue();
        LocalDate to    = editDateTo.isVisible() ? editDateTo.getValue() : from;
        int persons     = editPersonsSpinner.getValue();

        // Recalculate total
        double newTotal = recalcTotal(from, to, persons);

        // Update details map
        Map<String, Object> details = editingReservation.getDetails();
        if (details == null) details = new HashMap<>();

        switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> {
                long nights = ChronoUnit.DAYS.between(from, to);
                details.put("nights",   nights);
                details.put("guests",   persons);
                details.put("dateFrom", from.toString());
                details.put("dateTo",   to.toString());
            }
            case ACTIVITY -> {
                details.put("date",         from.toString());
                details.put("participants", persons);
            }
            case TRANSPORT -> {
                details.put("date",       from.toString());
                details.put("passengers", persons);
            }
        }

        editingReservation.setDateFrom(from);
        editingReservation.setDateTo(to);
        editingReservation.setNumberOfPersons(persons);
        editingReservation.setTotalPrice(newTotal);
        editingReservation.setDetails(details);

        try {
            service.update(editingReservation);
            showEditSuccess("✅ Réservation mise à jour avec succès !");
            loadData();                     // refresh all data
            editingReservation = null;
            Platform.runLater(() -> {
                editPanel.setVisible(false);
                editPanel.setManaged(false);
            });
        } catch (SQLException e) {
            showEditError("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    private double recalcTotal(LocalDate from, LocalDate to, int persons) {
        double unitPrice = getPricePerUnit();
        return switch (editingReservation.getReservationType()) {
            case HEBERGEMENT -> ChronoUnit.DAYS.between(from, to) * unitPrice;
            case ACTIVITY, TRANSPORT -> persons * unitPrice;
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

    /* ═══════════════════════════════════════════════════ */
    /*  DELETE                                             */
    /* ═══════════════════════════════════════════════════ */

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
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
            }
        });
    }

    /* ═══════════════════════════════════════════════════ */
    /*  PAGINATION                                         */
    /* ═══════════════════════════════════════════════════ */

    @FXML private void onPrev() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML private void onNext() {
        int totalPages = (int) Math.ceil((double) filteredData.size() / PER_PAGE);
        if (currentPage < totalPages) { currentPage++; renderPage(); }
    }

    @FXML private void onReset() {
        filterType.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        sortSelect.getSelectionModel().selectFirst();
        filteredData = allData;
        currentPage  = 1;
        renderPage();
    }

    /* ═══════════════════════════════════════════════════ */
    /*  REPOSITORY UPDATE (add to ReservationRepository)  */
    /* ═══════════════════════════════════════════════════ */
    // See note below — update() is called directly on repository here

    /* ═══════════════════════════════════════════════════ */
    /*  HELPERS                                            */
    /* ═══════════════════════════════════════════════════ */

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
                errEditDateFrom, errEditDateTo, errEditPersons}) {
            l.setVisible(false); l.setManaged(false);
        }
        hidePricePreview();
    }
}