package tn.esprit.utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class MollieCheckoutWindow {

    private MollieCheckoutWindow() {}

    public static CheckoutResult show(MolliePayment payment, MolliePaymentService paymentService) {

        // Ouvrir l'URL dans le navigateur système
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(payment.getCheckoutUrl()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Boîte de dialogue d'attente avec bouton Vérifier
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Paiement Mollie");
        stage.setMinWidth(500);
        stage.setMinHeight(300);

        Label badgeLabel = new Label("Mode test Mollie");
        badgeLabel.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;");

        Label titleLabel = new Label("Paiement sécurisé Mollie");
        titleLabel.setStyle("-fx-text-fill:#0f172a;-fx-font-size:17;-fx-font-weight:bold;");

        Label paymentIdLabel = new Label("Paiement : " + payment.getId());
        paymentIdLabel.setStyle("-fx-text-fill:#475569;-fx-font-size:11;-fx-font-weight:bold;");

        Label statusPill = new Label("En attente");
        statusPill.setStyle("-fx-background-color:#fef3c7;-fx-text-fill:#92400e;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;");

        Label infoLabel = new Label(
                "✅ Le paiement s'est ouvert dans votre navigateur.\n\n" +
                        "1. Complétez le paiement dans le navigateur\n" +
                        "2. Revenez ici et cliquez sur 'Vérifier le paiement'\n\n" +
                        "Carte de test : 4543 4740 0224 9996\n" +
                        "Expiration : 12/27   CVC : 123"
        );
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill:#334155;-fx-font-size:13;-fx-padding:10;");

        Label statusLabel = new Label("Complétez le paiement puis cliquez sur Vérifier.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill:#334155;-fx-font-size:11;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        Button verifyButton = new Button("Vérifier le paiement");
        verifyButton.setStyle("-fx-background-color:#0f766e;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:9;-fx-padding:8 14;-fx-cursor:hand;");

        Button closeButton = new Button("Fermer");
        closeButton.setStyle("-fx-background-color:white;-fx-text-fill:#334155;-fx-border-color:#cbd5e1;-fx-border-radius:9;-fx-background-radius:9;-fx-padding:8 14;-fx-cursor:hand;");

        final CheckoutResult[] resultHolder = {new CheckoutResult("open", false)};

        Runnable refreshStatus = () -> {
            if (progressIndicator.isVisible()) return;
            progressIndicator.setVisible(true);
            verifyButton.setDisable(true);
            statusLabel.setText("Vérification du statut...");
            statusPill.setText("Vérification");
            statusPill.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;");

            javafx.concurrent.Task<MolliePayment> task = new javafx.concurrent.Task<>() {
                @Override protected MolliePayment call() throws Exception {
                    return paymentService.getPayment(payment.getId());
                }
            };

            task.setOnSucceeded(event -> {
                progressIndicator.setVisible(false);
                verifyButton.setDisable(false);
                MolliePayment current = task.getValue();
                String s = current.getStatus();
                resultHolder[0] = new CheckoutResult(s, isSuccessful(s));

                if (isSuccessful(s)) {
                    statusLabel.setText("✅ Paiement confirmé par Mollie !");
                    statusPill.setText("Payé");
                    statusPill.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#166534;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;");
                    stage.close();
                    return;
                }
                if (isTerminal(s)) {
                    statusLabel.setText("Paiement terminé : " + s);
                    statusPill.setText(formatStatus(s));
                    statusPill.setStyle(statusStyleFor(s));
                    stage.close();
                    return;
                }
                statusLabel.setText("Statut actuel : " + s + ". Continuez le paiement dans le navigateur.");
                statusPill.setText(formatStatus(s));
                statusPill.setStyle(statusStyleFor(s));
            });

            task.setOnFailed(event -> {
                progressIndicator.setVisible(false);
                verifyButton.setDisable(false);
                Throwable error = task.getException();
                statusLabel.setText("Erreur : " + (error == null ? "inconnue" : error.getMessage()));
                statusPill.setText("Erreur");
                statusPill.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#b91c1c;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;");
            });

            Thread t = new Thread(task, "mollie-status-check");
            t.setDaemon(true);
            t.start();
        };

        verifyButton.setOnAction(e -> refreshStatus.run());
        closeButton.setOnAction(e -> {
            if (!isTerminal(resultHolder[0].status())) {
                refreshStatus.run();
                return;
            }
            stage.close();
        });

        HBox actions = new HBox(10, verifyButton, closeButton, progressIndicator);
        actions.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox metaRow = new HBox(10, badgeLabel, titleLabel, paymentIdLabel, spacer, statusPill);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        VBox topBox = new VBox(8, metaRow, statusLabel, actions);
        topBox.setPadding(new Insets(10, 14, 10, 14));
        topBox.setStyle("-fx-background-color:linear-gradient(to right,#f8fafc,#eff6ff);-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;");

        VBox center = new VBox(16, infoLabel);
        center.setPadding(new Insets(20));
        center.setAlignment(Pos.CENTER);
        center.setStyle("-fx-background-color:#f8fafc;");

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(center);
        root.setStyle("-fx-background-color:white;");

        stage.setScene(new Scene(root, 550, 380));
        stage.showAndWait();

        return resultHolder[0];
    }

    private static boolean isSuccessful(String s) {
        return "paid".equalsIgnoreCase(s) || "authorized".equalsIgnoreCase(s);
    }
    private static boolean isTerminal(String s) {
        return isSuccessful(s) || "canceled".equalsIgnoreCase(s)
                || "failed".equalsIgnoreCase(s) || "expired".equalsIgnoreCase(s);
    }
    private static String formatStatus(String s) {
        if (s == null || s.isBlank()) return "Inconnu";
        return switch (s.toLowerCase()) {
            case "paid" -> "Payé"; case "authorized" -> "Autorisé";
            case "open" -> "En attente"; case "pending" -> "En cours";
            case "failed" -> "Échoué"; case "canceled" -> "Annulé";
            case "expired" -> "Expiré"; default -> s;
        };
    }
    private static String statusStyleFor(String s) {
        if (s == null) return "-fx-background-color:#e2e8f0;-fx-text-fill:#334155;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;";
        return switch (s.toLowerCase()) {
            case "paid","authorized" -> "-fx-background-color:#dcfce7;-fx-text-fill:#166534;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;";
            case "failed","canceled","expired" -> "-fx-background-color:#fee2e2;-fx-text-fill:#b91c1c;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;";
            default -> "-fx-background-color:#fef3c7;-fx-text-fill:#92400e;-fx-background-radius:999;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;";
        };
    }

    public record CheckoutResult(String status, boolean successful) {}
}