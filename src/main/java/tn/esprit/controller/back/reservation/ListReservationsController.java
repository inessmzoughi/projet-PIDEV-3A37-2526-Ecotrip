package tn.esprit.controller.back.reservation;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.esprit.models.Reservation;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.ReservationService;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ListReservationsController implements Initializable {

    /* ─── Stats ─── */
    @FXML private Label statTotal, statPending, statConfirmed, statCancelled, statRevenue;

    /* ─── Filters ─── */
    @FXML private ComboBox<String> filterStatus, filterType;
    @FXML private DatePicker       filterDateFrom, filterDateTo;
    @FXML private TextField        searchField;

    /* ─── Table ─── */
    @FXML private TableView<Reservation>           tableView;
    @FXML private TableColumn<Reservation, String> colId, colUser, colType, colLabel;
    @FXML private TableColumn<Reservation, String> colDateFrom, colDateTo, colPersons;
    @FXML private TableColumn<Reservation, String> colTotal, colStatus;
    @FXML private TableColumn<Reservation, Void>   colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;

    private final ReservationService service = new ReservationService();
    private List<Reservation> allData = new ArrayList<>();
    private static final int PER_PAGE = 10;
    private int currentPage = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatus.getItems().addAll("Tous", "PENDING", "CONFIRMED", "CANCELLED");
        filterStatus.setValue("Tous");
        filterType.getItems().addAll("Tous", "HEBERGEMENT", "ACTIVITY", "TRANSPORT");
        filterType.setValue("Tous");

        filterStatus.setOnAction(e -> { currentPage = 1; renderTable(); });
        filterType.setOnAction(e ->   { currentPage = 1; renderTable(); });
        searchField.setOnKeyReleased(e -> { currentPage = 1; renderTable(); });

        setupColumns();
        loadData();
    }

    private void loadData() {
        try {
            allData = service.getAllReservations();
            updateStats();
            renderTable();
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    private void updateStats() {
        try {
            Map<String, Object> stats = service.getStats();
            statTotal.setText(stats.get("total").toString());
            statPending.setText(stats.get("pending").toString());
            statConfirmed.setText(stats.get("confirmed").toString());
            statCancelled.setText(stats.get("cancelled").toString());
            statRevenue.setText(String.format("%.2f TND", (double) stats.get("revenue")));
        } catch (SQLException e) { /* ignore stats error */ }
    }

    private List<Reservation> getFiltered() {
        String query  = searchField.getText().toLowerCase().trim();
        String status = filterStatus.getValue();
        String type   = filterType.getValue();

        return allData.stream()
                .filter(r -> query.isEmpty()
                        || String.valueOf(r.getId()).contains(query)
                        || String.valueOf(r.getUserId()).contains(query)
                        || r.getReservationType().name().toLowerCase().contains(query))
                .filter(r -> status == null || status.equals("Tous")
                        || r.getStatus().name().equals(status))
                .filter(r -> type == null || type.equals("Tous")
                        || r.getReservationType().name().equals(type))
                .collect(Collectors.toList());
    }

    private void renderTable() {
        List<Reservation> filtered = getFiltered();
        badgeCount.setText(String.valueOf(filtered.size()));

        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        if (currentPage > totalPages) currentPage = 1;
        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);

        tableView.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
        pagInfo.setText(total == 0 ? "" : "Affichage " + (from+1) + "–" + to + " sur " + total);

        pagButtons.getChildren().clear();
        for (int p = 1; p <= totalPages; p++) {
            final int pn = p;
            Button b = new Button(String.valueOf(p));
            b.getStyleClass().add("page-btn");
            if (p == currentPage) b.getStyleClass().add("page-btn-active");
            b.setOnAction(e -> { currentPage = pn; renderTable(); });
            pagButtons.getChildren().add(b);
        }
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
                new SimpleStringProperty("#" + c.getValue().getId()));

        colUser.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsername(c.getValue().getUserId())));

        colType.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getReservationType().name()));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setGraphic(null); return; }
                String icon = switch (t) {
                    case "HEBERGEMENT" -> "🏨"; case "ACTIVITY" -> "🧭"; default -> "🚌"; };
                Label lbl = new Label(icon + " " + t);
                lbl.getStyleClass().add("badge-user");
                setGraphic(lbl); setText(null);
            }
        });

        colLabel.setCellValueFactory(c ->
                new SimpleStringProperty(resolveLabel(c.getValue())));

        colDateFrom.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDateFrom() != null
                        ? c.getValue().getDateFrom().format(FMT) : "—"));
        colDateTo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDateTo() != null
                        ? c.getValue().getDateTo().format(FMT) : "—"));

        colPersons.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getNumberOfPersons())));

        colTotal.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.2f TND", c.getValue().getTotalPrice())));

        // Status badge
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label lbl = new Label(switch (s) {
                    case "PENDING"   -> "⏳ En attente";
                    case "CONFIRMED" -> "✅ Confirmée";
                    default          -> "❌ Annulée";
                });
                lbl.getStyleClass().add(switch (s) {
                    case "PENDING"   -> "badge-user";
                    case "CONFIRMED" -> "badge-actif";
                    default          -> "badge-inactif";
                });
                setGraphic(lbl); setText(null);
            }
        });

        // Actions: confirm / cancel / delete
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnConfirm = new Button("✅");
            private final Button btnCancel  = new Button("❌");
            private final Button btnDel     = new Button("🗑️");
            private final HBox   box        = new HBox(6, btnConfirm, btnCancel, btnDel);
            {
                btnConfirm.getStyleClass().add("btn-edit");
                btnCancel.getStyleClass().add("btn-edit");
                btnDel.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                btnConfirm.setOnAction(e -> updateStatus(
                        getTableView().getItems().get(getIndex()), ReservationStatus.CONFIRMED));
                btnCancel.setOnAction(e -> updateStatus(
                        getTableView().getItems().get(getIndex()), ReservationStatus.CANCELLED));
                btnDel.setOnAction(e ->
                        confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : box);
            }
        });
    }

    private void updateStatus(Reservation r, ReservationStatus status) {
        try {
            service.updateStatus(r.getId(), status);
            loadData();
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    private void confirmDelete(Reservation r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer la réservation #" + r.getId() + " ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try { service.delete(r.getId()); loadData(); }
            catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    @FXML private void onSearch() { currentPage = 1; renderTable(); }
    @FXML private void onFilter() { currentPage = 1; renderTable(); }
    @FXML private void onReset()  {
        searchField.clear();
        filterStatus.setValue("Tous"); filterType.setValue("Tous");
        filterDateFrom.setValue(null); filterDateTo.setValue(null);
        currentPage = 1; loadData();
    }
    @FXML private void onNavDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

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

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}