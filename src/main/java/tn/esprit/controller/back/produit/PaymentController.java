package tn.esprit.controller.back.produit;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.models.produit.Payment;
import tn.esprit.models.produit.PaymentMethod;
import tn.esprit.models.produit.PaymentStatus;
import tn.esprit.services.produit.PaymentService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class PaymentController implements Initializable {

    @FXML private Label statTotal, statCA, statCompleted, statPending;

    @FXML private VBox formPanel;
    @FXML private Label formPanelTitle;

    @FXML private TextField amountField;
    @FXML private ComboBox<PaymentMethod> methodCombo;
    @FXML private ComboBox<PaymentStatus> statusCombo;
    @FXML private DatePicker paidAtPicker;
    @FXML private TextField transactionIdField;
    @FXML private TextField reservationIdField;

    @FXML private TextField searchField;
    @FXML private ComboBox<PaymentStatus> filterStatus;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<Payment> tableView;
    @FXML private TableColumn<Payment, Integer> colIndex;
    @FXML private TableColumn<Payment, Double> colAmount;
    @FXML private TableColumn<Payment, PaymentMethod> colMethod;
    @FXML private TableColumn<Payment, PaymentStatus> colStatus;
    @FXML private TableColumn<Payment, Date> colPaidAt;
    @FXML private TableColumn<Payment, Integer> colTransactionId;
    @FXML private TableColumn<Payment, Integer> colReservationId;
    @FXML private TableColumn<Payment, Date> colCreatedAt;
    @FXML private TableColumn<Payment, Void> colActions;

    @FXML private Label badgeCount;

    private final PaymentService service = new PaymentService();
    private List<Payment> allData = new ArrayList<>();
    private Integer editingId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        methodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        statusCombo.setItems(FXCollections.observableArrayList(PaymentStatus.values()));
        filterStatus.setItems(FXCollections.observableArrayList(PaymentStatus.values()));
        filterStatus.setPromptText("Tous les statuts");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…",
                "Date (récent)",
                "Montant (croissant)",
                "Montant (décroissant)"
        ));
        sortCombo.getSelectionModel().selectFirst();

        // Ces champs n'existent pas dans la table réelle, on les garde juste pour compatibilité FXML
        if (statusCombo != null) {
            statusCombo.setValue(PaymentStatus.COMPLETED);
        }
        if (transactionIdField != null) {
            transactionIdField.setText("0");
            transactionIdField.setEditable(false);
        }

        setupColumns();
        loadData();
    }

    private void loadData() {
        try {
            allData = service.read();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement",
                    "Impossible de lire les paiements : " + e.getMessage());
            allData = new ArrayList<>();
        }

        renderTable();
        updateStats();
    }

    private void setupColumns() {
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                if (!getStyleClass().contains("td-index")) {
                    getStyleClass().add("td-index");
                }
            }
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label chip = new Label(String.format(Locale.US, "%.2f DT", v));
                chip.getStyleClass().addAll("chip", "chip-green");
                setGraphic(chip);
                setText(null);
            }
        });

        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colMethod.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PaymentMethod v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    return;
                }

                String icon = switch (v) {
                    case CASH -> "💵";
                    case CARD -> "💳";
                    case PAYPAL -> "🌐";
                };

                setText(icon + " " + v.name());
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PaymentStatus v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                String style = switch (v) {
                    case COMPLETED -> "chip-green";
                    case PENDING -> "chip-orange";
                    case FAILED -> "chip-red";
                };

                Label chip = new Label(v.name());
                chip.getStyleClass().addAll("chip", style);
                setGraphic(chip);
                setText(null);
            }
        });

        colPaidAt.setCellValueFactory(new PropertyValueFactory<>("paidAt"));
        colPaidAt.setCellFactory(col -> new TableCell<>() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Date v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : sdf.format(v));
            }
        });

        colTransactionId.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        colTransactionId.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : "—");
            }
        });

        colReservationId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colReservationId.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setText(null);
                    return;
                }

                if (v == null || v <= 0) {
                    setText("—");
                } else {
                    setText("🧾 Commande #" + v);
                }
            }
        });

        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Date v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : sdf.format(v));
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    Payment p = getCurrentRowItem();
                    if (p != null) {
                        openEdit(p);
                    }
                });

                delBtn.setOnAction(e -> {
                    Payment p = getCurrentRowItem();
                    if (p != null) {
                        confirmDelete(p);
                    }
                });
            }

            private Payment getCurrentRowItem() {
                int index = getIndex();
                if (index >= 0 && index < getTableView().getItems().size()) {
                    return getTableView().getItems().get(index);
                }
                return null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void renderTable() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        PaymentStatus status = filterStatus.getValue();
        String sort = sortCombo.getValue();

        List<Payment> filtered = allData.stream()
                .filter(p ->
                        q.isEmpty()
                                || String.valueOf(p.getId()).contains(q)
                                || String.valueOf(p.getAmount()).contains(q)
                                || String.valueOf(p.getReservationId()).contains(q)
                                || (p.getPaymentMethod() != null && p.getPaymentMethod().name().toLowerCase().contains(q))
                                || (p.getPaymentStatus() != null && p.getPaymentStatus().name().toLowerCase().contains(q))
                )
                .filter(p -> status == null || p.getPaymentStatus() == status)
                .toList();

        List<Payment> sorted = new ArrayList<>(filtered);

        if ("Date (récent)".equals(sort)) {
            sorted.sort(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if ("Montant (croissant)".equals(sort)) {
            sorted.sort(Comparator.comparingDouble(Payment::getAmount));
        } else if ("Montant (décroissant)".equals(sort)) {
            sorted.sort(Comparator.comparingDouble(Payment::getAmount).reversed());
        }

        badgeCount.setText(String.valueOf(sorted.size()));
        tableView.setItems(FXCollections.observableArrayList(sorted));
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));

        double ca = allData.stream().mapToDouble(Payment::getAmount).sum();
        statCA.setText(String.format(Locale.US, "%.2f DT", ca));

        long completed = allData.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.COMPLETED)
                .count();
        statCompleted.setText(String.valueOf(completed));

        long pending = allData.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING)
                .count();
        statPending.setText(String.valueOf(pending));
    }

    @FXML
    private void onOpenForm() {
        editingId = null;
        formPanelTitle.setText("Nouveau Paiement");
        amountField.clear();
        methodCombo.getSelectionModel().clearSelection();
        if (statusCombo != null) {
            statusCombo.setValue(PaymentStatus.COMPLETED);
        }
        paidAtPicker.setValue(java.time.LocalDate.now());
        if (transactionIdField != null) {
            transactionIdField.setText("0");
        }
        reservationIdField.clear();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onCloseForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editingId = null;
    }

    private void openEdit(Payment p) {
        editingId = p.getId();
        formPanelTitle.setText("Modifier le Paiement");
        amountField.setText(String.valueOf(p.getAmount()));
        methodCombo.setValue(p.getPaymentMethod());

        if (statusCombo != null) {
            statusCombo.setValue(p.getPaymentStatus());
        }

        if (p.getPaidAt() != null) {
            paidAtPicker.setValue(
                    p.getPaidAt().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
            );
        } else {
            paidAtPicker.setValue(java.time.LocalDate.now());
        }

        if (transactionIdField != null) {
            transactionIdField.setText("0");
        }

        if (p.getReservationId() > 0) {
            reservationIdField.setText(String.valueOf(p.getReservationId()));
        } else {
            reservationIdField.clear();
        }

        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        if (amountField.getText().trim().isEmpty()
                || methodCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants",
                    "Montant et méthode de paiement sont obligatoires.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            PaymentMethod method = methodCombo.getValue();
            PaymentStatus status = statusCombo != null && statusCombo.getValue() != null
                    ? statusCombo.getValue()
                    : PaymentStatus.COMPLETED;

            if (amount < 0) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                        "Le montant ne peut pas être négatif.");
                return;
            }

            Date paidAt;
            if (paidAtPicker.getValue() != null) {
                LocalDateTime dateTime = paidAtPicker.getValue().atTime(LocalDateTime.now().toLocalTime());
                paidAt = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
            } else {
                paidAt = new Date();
            }

            int commandeId = 0;
            if (reservationIdField.getText() != null && !reservationIdField.getText().trim().isEmpty()) {
                commandeId = Integer.parseInt(reservationIdField.getText().trim());
            }

            int transactionId = 0;
            Date createdAt = paidAt;

            Payment p;
            if (editingId != null) {
                p = new Payment(editingId, amount, method, status, paidAt, transactionId, createdAt, commandeId);
                service.update(p);
            } else {
                p = new Payment(amount, method, status, paidAt, transactionId, createdAt, commandeId);
                service.create(p);
            }

            onCloseForm();
            loadData();

        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                    "Montant et ID commande doivent être numériques.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", ex.getMessage());
        }
    }

    private void confirmDelete(Payment p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le paiement ?");
        alert.setHeaderText("Supprimer le paiement #" + p.getId() + " ?");
        alert.setContentText("Cette action est irréversible.");

        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);

        try {
            URL css = getClass().getResource("/css/ecotrip.css");
            if (css != null) {
                alert.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        alert.showAndWait().filter(b -> b == confirm).ifPresent(b -> {
            try {
                service.delete(p.getId());
                loadData();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur SQL",
                        "Suppression impossible : " + ex.getMessage());
            }
        });
    }

    @FXML
    private void onSearch() {
        renderTable();
    }

    @FXML private void onNavHebergements() { navigateTo("ListHebergements.fxml", "Hébergements"); }
    @FXML private void onNavChambres() { navigateTo("Chambres.fxml", "Chambres"); }
    @FXML private void onNavEquipements() { navigateTo("Equipements.fxml", "Équipements"); }
    @FXML private void onNavCategories() { navigateTo("CategoriesHebergement.fxml", "Catégories"); }
    @FXML private void onNavProduits() { navigateTo("Product.fxml", "Produits"); }
    @FXML private void onNavLignesCommande() { navigateTo("LigneCommande.fxml", "Lignes de commande"); }
    @FXML private void onNavPaiements() { navigateTo("Payment.fxml", "Paiements"); }
    @FXML private void onNavCommandes() { navigateTo("Commande.fxml", "Commandes"); }
    @FXML private void onNavUtilisateurs() { navigateTo("Utilisateurs.fxml", "Utilisateurs"); }
    @FXML private void onLogout() { System.exit(0); }

    private void navigateTo(String fxml, String title) {
        try {
            URL location = getClass().getResource("/fxml/" + fxml);
            if (location == null) {
                showAlert(Alert.AlertType.INFORMATION,
                        "Page non disponible",
                        "La page « " + title + " » n'est pas encore implémentée.");
                return;
            }

            Parent root = FXMLLoader.load(location);
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle("EcoTrip Admin — " + title);

        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur navigation",
                    "Impossible d'ouvrir la page : " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);

        try {
            URL css = getClass().getResource("/css/ecotrip.css");
            if (css != null) {
                a.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        a.showAndWait();
    }
}