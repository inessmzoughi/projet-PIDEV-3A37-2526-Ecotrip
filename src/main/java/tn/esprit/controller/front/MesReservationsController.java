package tn.esprit.controller.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.models.Reservation;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.ReservationService;
import tn.esprit.session.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MesReservationsController implements Initializable {

    @FXML private VBox  emptyState;
    @FXML private VBox  reservationsContainer;
    @FXML private Label toastLabel;

    private final ReservationService service = new ReservationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }
        loadReservations();
    }

    private void loadReservations() {
        try {
            List<Reservation> list = service.getMyReservations();
            reservationsContainer.getChildren().clear();

            if (list.isEmpty()) {
                emptyState.setVisible(true); emptyState.setManaged(true);
                return;
            }
            emptyState.setVisible(false); emptyState.setManaged(false);
            for (Reservation r : list)
                reservationsContainer.getChildren().add(buildCard(r));

        } catch (SQLException e) {
            showToast("Erreur de chargement : " + e.getMessage());
        }
    }

    private VBox buildCard(Reservation r) {
        VBox card = new VBox(0);
        card.getStyleClass().add("res-front-card");

        // Header strip — colored by type
        String headerStyle = switch (r.getReservationType()) {
            case HEBERGEMENT -> "-fx-background-color:#0d3d18;";
            case ACTIVITY    -> "-fx-background-color:#1e3a5f;";
            case TRANSPORT   -> "-fx-background-color:#7c2d12;";
        };
        String icon = switch (r.getReservationType()) {
            case HEBERGEMENT -> "🏨";
            case ACTIVITY    -> "🧭";
            case TRANSPORT   -> "🚌";
        };
        String typeLabel = switch (r.getReservationType()) {
            case HEBERGEMENT -> "Hébergement";
            case ACTIVITY    -> "Activité";
            case TRANSPORT   -> "Transport";
        };

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(headerStyle
                + "-fx-background-radius:16 16 0 0;-fx-padding:16 20 16 20;");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:20px;");
        Label typeLbl = new Label(typeLabel + "  •  #" + r.getId());
        typeLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status badge
        Label statusBadge = new Label(statusLabel(r.getStatus()));
        statusBadge.setStyle(statusStyle(r.getStatus())
                + "-fx-background-radius:20;-fx-padding:4 12;-fx-font-size:12px;-fx-font-weight:bold;");
        header.getChildren().addAll(iconLbl, typeLbl, spacer, statusBadge);
        card.getChildren().add(header);

        // Body
        GridPane body = new GridPane();
        body.setHgap(24); body.setVgap(10);
        body.setStyle("-fx-padding:20 24;-fx-background-color:white;-fx-background-radius:0 0 16 16;");
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        body.getColumnConstraints().addAll(c1, c2);

        addBodyRow(body, 0, "📅 Date début",
                r.getDateFrom() != null ? r.getDateFrom().format(FMT) : "—");
        addBodyRow(body, 1, "📅 Date fin",
                r.getDateTo() != null ? r.getDateTo().format(FMT) : "—");
        addBodyRow(body, 2, "👥 Personnes", String.valueOf(r.getNumberOfPersons()));
        addBodyRow(body, 3, "💰 Total",
                String.format("%.2f TND", r.getTotalPrice()));

        // Delete button (only for PENDING)
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:12 20 16 20;-fx-background-color:#f8fafc;"
                + "-fx-background-radius:0 0 16 16;-fx-border-color:#f1f5f9;-fx-border-width:1 0 0 0;");

        if (r.getStatus() == ReservationStatus.PENDING) {
            Button btnDelete = new Button("🗑  Annuler");
            btnDelete.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#991b1b;"
                    + "-fx-background-radius:8;-fx-padding:8 16;-fx-cursor:hand;-fx-border-width:0;"
                    + "-fx-font-weight:bold;-fx-font-size:13px;");
            btnDelete.setOnAction(e -> confirmDelete(r));
            footer.getChildren().add(btnDelete);
        }

        Label dateLabel = new Label("Créée le " + r.getCreatedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dateLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
        Region fs = new Region(); HBox.setHgrow(fs, Priority.ALWAYS);
        footer.getChildren().addAll(0, List.of(dateLabel, fs));

        card.getChildren().addAll(body, footer);
        return card;
    }

    private void addBodyRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        grid.add(lbl, row % 2 == 0 ? 0 : 1, row / 2 * 2);
        grid.add(val, row % 2 == 0 ? 0 : 1, row / 2 * 2 + 1);
    }

    private void confirmDelete(Reservation r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler la réservation ?");
        alert.setHeaderText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Retour",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Annuler",  ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.delete(r.getId());
                loadReservations();
                showToast("✅ Réservation annulée.");
            } catch (SQLException e) {
                showToast("Erreur : " + e.getMessage());
            }
        });
    }

    private String statusLabel(ReservationStatus s) {
        return switch (s) {
            case PENDING   -> "⏳ En attente";
            case CONFIRMED -> "✅ Confirmée";
            case CANCELLED -> "❌ Annulée";
        };
    }

    private String statusStyle(ReservationStatus s) {
        return switch (s) {
            case PENDING   -> "-fx-background-color:#fef3c7;-fx-text-fill:#92400e;";
            case CONFIRMED -> "-fx-background-color:#dcfce7;-fx-text-fill:#166534;";
            case CANCELLED -> "-fx-background-color:#fee2e2;-fx-text-fill:#991b1b;";
        };
    }

    private void showToast(String msg) {
        toastLabel.setText(msg);
        toastLabel.setVisible(true); toastLabel.setManaged(true);
        new Thread(() -> {
            try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> { toastLabel.setVisible(false); toastLabel.setManaged(false); });
        }).start();
    }
}