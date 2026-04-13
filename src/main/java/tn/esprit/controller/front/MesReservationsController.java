package tn.esprit.controller.front;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.models.Reservation;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.ReservationService;
import tn.esprit.session.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MesReservationsController implements Initializable {

    /* ─── FXML ─── */
    @FXML private ComboBox<String> filterType, filterStatus, sortSelect;
    @FXML private Label            resultCount;
    @FXML private FlowPane         cardsPane;
    @FXML private VBox             emptyState;
    @FXML private HBox             paginationBar;
    @FXML private Button           btnPrev, btnNext;
    @FXML private Label            pagInfo;

    /* ─── State ─── */
    private final ReservationService service = new ReservationService();
    private List<Reservation> allData     = new ArrayList<>();
    private List<Reservation> filteredData = new ArrayList<>();

    private static final int PER_PAGE = 6;
    private int currentPage = 1;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            javafx.application.Platform.runLater(
                    () -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        // Initialise combos to first item
        filterType.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        sortSelect.getSelectionModel().selectFirst();

        // Live filter on every change
        filterType.setOnAction(e   -> { currentPage = 1; applyFilters(); });
        filterStatus.setOnAction(e -> { currentPage = 1; applyFilters(); });
        sortSelect.setOnAction(e   -> { currentPage = 1; applyFilters(); });

        loadData();
    }

    /* ─── Data ─── */

    private void loadData() {
        try {
            allData = service.getMyReservations();
        } catch (SQLException e) {
            allData = new ArrayList<>();
        }
        applyFilters();
    }

    /* ─── Filters — mirrors HebergementsController.applyFilters() ─── */

    private void applyFilters() {
        String type   = filterType.getValue();
        String status = filterStatus.getValue();
        String sort   = sortSelect.getValue();

        filteredData = allData.stream()
                .filter(r -> type == null
                        || type.equals("Tous les types")
                        || r.getReservationType().name().equals(type))
                .filter(r -> status == null
                        || status.equals("Tous les statuts")
                        || r.getStatus().name().equals(status))
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Plus récentes"    ->
                    filteredData.sort(Comparator.comparing(
                            Reservation::getCreatedAt).reversed());
            case "Plus anciennes"   ->
                    filteredData.sort(Comparator.comparing(
                            Reservation::getCreatedAt));
            case "Prix croissant"   ->
                    filteredData.sort(Comparator.comparingDouble(
                            Reservation::getTotalPrice));
            case "Prix décroissant" ->
                    filteredData.sort(Comparator.comparingDouble(
                            Reservation::getTotalPrice).reversed());
            default -> {}
        }

        currentPage = 1;
        renderPage();
    }

    /* ─── Pagination + render — mirrors HebergementsController.renderPage() ─── */

    private void renderPage() {
        cardsPane.getChildren().clear();

        boolean isEmpty = filteredData == null || filteredData.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        cardsPane.setVisible(!isEmpty);
        cardsPane.setManaged(!isEmpty);

        int total = isEmpty ? 0 : filteredData.size();
        resultCount.setText(total + " Réservation" + (total > 1 ? "s" : "") + " Trouvée" + (total > 1 ? "s" : ""));

        if (isEmpty) {
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            return;
        }

        int totalPages = (int) Math.ceil((double) total / PER_PAGE);
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1)         currentPage = 1;

        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);

        filteredData.subList(from, to)
                .forEach(r -> cardsPane.getChildren().add(buildCard(r)));

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);

        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
    }

    /* ─── Build card — mirrors HebergementsController.buildCard() ─── */

    private VBox buildCard(Reservation r) {
        VBox card = new VBox(0);
        card.getStyleClass().add("res-front-card");
        card.setPrefWidth(340);

        // ── Colored image zone (mirrors heb-card-img zone) ──
        VBox imgZone = buildImageZone(r);
        card.getChildren().add(imgZone);

        // ── Card body ──
        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 20, 8, 20));

        // Status badge (mirrors heb-card-category)
        Label statusBadge = new Label(statusLabel(r.getStatus()));
        statusBadge.getStyleClass().add(statusStyleClass(r.getStatus()));

        // Type label (mirrors heb-card-nom)
        Label typeLabel = new Label(typeIcon(r) + "  " + typeLabel(r));
        typeLabel.getStyleClass().add("heb-card-nom");
        typeLabel.setWrapText(true);

        // Label (item name from details)
        String itemName = resolveLabel(r);
        Label itemLabel = new Label(itemName);
        itemLabel.getStyleClass().add("heb-card-ville");
        itemLabel.setWrapText(true);

        // Dates row (mirrors heb-card-equipement tags)
        HBox datesRow = new HBox(8);
        datesRow.setAlignment(Pos.CENTER_LEFT);
        if (r.getDateFrom() != null) {
            Label from = new Label("📅 " + r.getDateFrom().format(FMT));
            from.getStyleClass().add("heb-card-equipement");
            datesRow.getChildren().add(from);
        }
        if (r.getDateTo() != null && !r.getDateTo().equals(r.getDateFrom())) {
            Label to = new Label("→ " + r.getDateTo().format(FMT));
            to.getStyleClass().add("heb-card-equipement");
            datesRow.getChildren().add(to);
        }

        // Persons tag
        Label persons = new Label("👥 " + r.getNumberOfPersons() + " pers.");
        persons.getStyleClass().add("heb-card-equipement");

        body.getChildren().addAll(statusBadge, typeLabel, itemLabel, datesRow, persons);

        // ── Price + action row (mirrors heb-card-price-box + heb-card-btn) ──
        HBox priceBox = new HBox(5);
        priceBox.getStyleClass().add("heb-card-price-box");
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setMaxWidth(Double.MAX_VALUE);

        Label amount = new Label(String.format("%.2f", r.getTotalPrice()));
        amount.getStyleClass().add("heb-card-price-amount");
        Label currency = new Label("TND");
        currency.getStyleClass().add("heb-card-price-currency");
        Label unit = new Label("total");
        unit.getStyleClass().add("heb-card-price-unit");
        priceBox.getChildren().addAll(amount, currency, unit);

        body.getChildren().add(priceBox);

        // Delete button — only for PENDING (mirrors heb-card-btn)
        if (r.getStatus() == ReservationStatus.PENDING) {
            Button btnCancel = new Button("Annuler la réservation");
            btnCancel.getStyleClass().add("res-cancel-btn");
            btnCancel.setMaxWidth(Double.MAX_VALUE);
            btnCancel.setOnAction(e -> confirmDelete(r));
            body.getChildren().add(btnCancel);
        } else {
            // Non-clickable status label instead of button
            Label statusInfo = new Label(r.getStatus() == ReservationStatus.CONFIRMED
                    ? "✅ Confirmée — non modifiable"
                    : "❌ Annulée");
            statusInfo.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
            body.getChildren().add(statusInfo);
        }

        card.getChildren().add(body);
        return card;
    }

    /* ─── Colored image zone per type ─── */

    private VBox buildImageZone(Reservation r) {
        VBox imgZone = new VBox();
        imgZone.getStyleClass().add("heb-card-img");
        imgZone.setPrefHeight(160);
        imgZone.setMinHeight(160);
        imgZone.setMaxHeight(160);
        imgZone.setAlignment(Pos.CENTER);
        imgZone.setSpacing(8);

        // Override background color per type
        String bg = switch (r.getReservationType()) {
            case HEBERGEMENT -> "-fx-background-color:linear-gradient(to bottom right,#0d3d18,#1a5f2a);";
            case ACTIVITY    -> "-fx-background-color:linear-gradient(to bottom right,#1e3a5f,#2563eb);";
            case TRANSPORT   -> "-fx-background-color:linear-gradient(to bottom right,#7c2d12,#ea580c);";
        };
        imgZone.setStyle(bg);

        Label icon = new Label(typeIcon(r));
        icon.setStyle("-fx-font-size:48px;");

        Label created = new Label("Créée le "
                + r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        created.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.75);");

        imgZone.getChildren().addAll(icon, created);
        return imgZone;
    }

    /* ─── Pagination handlers ─── */

    @FXML
    private void onPrev() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML
    private void onNext() {
        int totalPages = (int) Math.ceil((double) filteredData.size() / PER_PAGE);
        if (currentPage < totalPages) { currentPage++; renderPage(); }
    }

    /* ─── Reset ─── */

    @FXML
    private void onReset() {
        filterType.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        sortSelect.getSelectionModel().selectFirst();
        filteredData = allData;
        currentPage  = 1;
        renderPage();
    }

    /* ─── Delete ─── */

    private void confirmDelete(Reservation r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler la réservation ?");
        alert.setHeaderText("Annuler « " + resolveLabel(r) + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Retour",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Annuler",  ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.delete(r.getId());
                loadData();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
            }
        });
    }

    /* ─── Helpers ─── */

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
            case PENDING   -> "heb-card-category";   // reuses existing yellow-ish badge
            case CONFIRMED -> "badge-actif";
            case CANCELLED -> "badge-inactif";
        };
    }
}