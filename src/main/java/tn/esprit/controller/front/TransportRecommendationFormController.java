package tn.esprit.controller.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import tn.esprit.models.transport.Transport;
import tn.esprit.models.transport.TransportRecommendationRequest;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.transport.GeminiTransportRecommendationService;
import tn.esprit.services.transport.TransportService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class TransportRecommendationFormController implements Initializable {

    @FXML private TextField originField;
    @FXML private TextField destinationField;
    @FXML private Spinner<Integer> passengersSpinner;
    @FXML private TextField budgetMinField;
    @FXML private TextField budgetMaxField;
    @FXML private ComboBox<String> preferenceCombo;
    @FXML private ComboBox<String> comfortCombo;
    @FXML private Label feedbackLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button submitButton;

    private final TransportService transportService = new TransportService();
    private final GeminiTransportRecommendationService recommendationService = new GeminiTransportRecommendationService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        passengersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1));
        preferenceCombo.setItems(FXCollections.observableArrayList("Eco-friendly", "Fastest", "Cheapest"));
        preferenceCombo.getSelectionModel().selectFirst();
        comfortCombo.setItems(FXCollections.observableArrayList("Basic", "Standard", "Premium"));
        comfortCombo.getSelectionModel().select("Standard");
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        feedbackLabel.setText("");
    }

    @FXML
    private void onSubmit() {
        feedbackLabel.setText("");

        TransportRecommendationRequest request;
        try {
            request = buildRequest();
        } catch (IllegalArgumentException ex) {
            feedbackLabel.setText(ex.getMessage());
            return;
        }

        setLoading(true);

        Task<GeminiTransportRecommendationService.RecommendationResponse> task = new Task<>() {
            @Override
            protected GeminiTransportRecommendationService.RecommendationResponse call() throws Exception {
                List<Transport> transports = loadTransports();
                return recommendationService.recommend(request, transports);
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);
            TransportRecommendationResultsController controller =
                    SceneManager.navigateToAndGetController(Routes.TRANSPORT_RECOMMENDATION_RESULTS);
            controller.setResults(request, task.getValue().recommendations(), transportsForDisplay(task.getValue().recommendations()));
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable error = task.getException();
            showError(error == null ? "Unable to generate recommendations right now."
                    : error.getMessage());
        });

        Thread thread = new Thread(task, "transport-ai-recommendation");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onBackToTransport() {
        SceneManager.navigateTo(Routes.TRANSPORT);
    }

    private TransportRecommendationRequest buildRequest() {
        String origin = valueOrEmpty(originField.getText());
        String destination = valueOrEmpty(destinationField.getText());

        if (origin.isBlank()) {
            throw new IllegalArgumentException("Origin is required.");
        }
        if (destination.isBlank()) {
            throw new IllegalArgumentException("Destination is required.");
        }

        Double budgetMin = parseOptionalDouble(budgetMinField.getText(), "Budget min");
        Double budgetMax = parseOptionalDouble(budgetMaxField.getText(), "Budget max");

        if (budgetMin != null && budgetMax != null && budgetMin > budgetMax) {
            throw new IllegalArgumentException("Budget max must be greater than or equal to budget min.");
        }

        TransportRecommendationRequest request = new TransportRecommendationRequest();
        request.setOrigin(origin);
        request.setDestination(destination);
        request.setPassengers(passengersSpinner.getValue());
        request.setBudgetMin(budgetMin);
        request.setBudgetMax(budgetMax);
        request.setPreference(valueOrEmpty(preferenceCombo.getValue()));
        request.setComfortLevel(valueOrEmpty(comfortCombo.getValue()));
        return request;
    }

    private List<Transport> loadTransports() throws SQLException {
        return transportService.afficherAll();
    }

    private List<Transport> transportsForDisplay(List<?> ignoredRecommendations) {
        try {
            return transportService.afficherAll();
        } catch (SQLException e) {
            return List.of();
        }
    }

    private Double parseOptionalDouble(String value, String fieldName) {
        String trimmed = valueOrEmpty(value);
        if (trimmed.isBlank()) {
            return null;
        }

        try {
            double parsed = Double.parseDouble(trimmed);
            if (parsed < 0) {
                throw new IllegalArgumentException(fieldName + " must be positive.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void setLoading(boolean loading) {
        submitButton.setDisable(loading);
        loadingIndicator.setManaged(loading);
        loadingIndicator.setVisible(loading);
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("AI Recommendation Error");
            alert.setContentText(message == null || message.isBlank()
                    ? "Unable to generate recommendations right now."
                    : message);
            alert.showAndWait();
        });
    }
}
