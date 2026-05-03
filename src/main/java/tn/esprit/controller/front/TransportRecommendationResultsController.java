package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.models.transport.Transport;
import tn.esprit.models.transport.TransportRecommendation;
import tn.esprit.models.transport.TransportRecommendationRequest;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.ExchangeRateService;
import tn.esprit.services.ExchangeRateService.PriceDisplay;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class TransportRecommendationResultsController implements Initializable {

    private static final String UPLOADS_DIR = "uploads/transports/";
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    @FXML private Label routeSummaryLabel;
    @FXML private Label filtersSummaryLabel;
    @FXML private FlowPane cardsPane;
    @FXML private VBox emptyState;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
    }

    public void setResults(TransportRecommendationRequest request,
                           List<TransportRecommendation> recommendations,
                           List<Transport> transports) {
        routeSummaryLabel.setText(String.format(
                Locale.US,
                "From %s to %s for %d passenger(s)",
                request.getOrigin(),
                request.getDestination(),
                request.getPassengers()
        ));

        filtersSummaryLabel.setText(String.format(
                Locale.US,
                "Preference: %s   |   Budget: %s   |   Comfort: %s",
                request.getPreference(),
                formatBudgetRange(request),
                request.getComfortLevel()
        ));

        cardsPane.getChildren().clear();
        boolean isEmpty = recommendations == null || recommendations.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        cardsPane.setVisible(!isEmpty);
        cardsPane.setManaged(!isEmpty);

        if (!isEmpty) {
            for (TransportRecommendation recommendation : recommendations) {
                cardsPane.getChildren().add(buildRecommendationCard(recommendation, findMatchingTransport(recommendation, transports)));
            }
        }
    }

    @FXML
    private void onTryAgain() {
        SceneManager.navigateTo(Routes.TRANSPORT_RECOMMENDATION_FORM);
    }

    @FXML
    private void onBackToTransport() {
        SceneManager.navigateTo(Routes.TRANSPORT);
    }

    private VBox buildRecommendationCard(TransportRecommendation recommendation, Transport transport) {
        VBox card = new VBox(14);
        card.getStyleClass().add("reco-card");
        card.setPrefWidth(320);

        StackPane media = buildImageZone(transport);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20));

        Label rankChip = new Label("Rank #" + recommendation.getRank());
        rankChip.getStyleClass().add("reco-rank-chip");

        Label typeLabel = new Label(recommendation.getType());
        typeLabel.getStyleClass().add("reco-card-title");
        typeLabel.setWrapText(true);

        PriceDisplay priceDisplay = resolveDisplayPrice(recommendation, transport);
        Label priceLabel = new Label(priceDisplay.primaryLine());
        priceLabel.getStyleClass().add("reco-card-price");
        priceLabel.setWrapText(true);

        Label convertedPriceLabel = new Label(priceDisplay.secondaryLine());
        convertedPriceLabel.getStyleClass().add("reco-card-price-secondary");
        convertedPriceLabel.setWrapText(true);
        convertedPriceLabel.setVisible(!priceDisplay.secondaryLine().isBlank());
        convertedPriceLabel.setManaged(!priceDisplay.secondaryLine().isBlank());

        Label justificationTitle = new Label("Why it fits");
        justificationTitle.getStyleClass().add("reco-card-subtitle");

        Label justificationLabel = new Label(recommendation.getJustification());
        justificationLabel.getStyleClass().add("reco-card-justification");
        justificationLabel.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button retryButton = new Button("Try another search");
        retryButton.getStyleClass().add("reco-inline-btn");
        retryButton.setOnAction(event -> onTryAgain());

        HBox footer = new HBox(retryButton);
        footer.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().addAll(rankChip, typeLabel, priceLabel, convertedPriceLabel, justificationTitle, justificationLabel, spacer, footer);
        card.getChildren().addAll(media, body);
        return card;
    }

    private String formatBudgetRange(TransportRecommendationRequest request) {
        String min = request.getBudgetMin() == null ? "Any" : String.format(Locale.US, "%.2f", request.getBudgetMin());
        String max = request.getBudgetMax() == null ? "Any" : String.format(Locale.US, "%.2f", request.getBudgetMax());
        return min + " - " + max;
    }

    private Transport findMatchingTransport(TransportRecommendation recommendation, List<Transport> transports) {
        if (recommendation == null || transports == null || transports.isEmpty()) {
            return null;
        }

        String recommendationType = normalize(recommendation.getType());
        double recommendedPrice = extractPrice(recommendation.getPrice());

        Transport exactMatch = transports.stream()
                .filter(transport -> normalize(transport.getType()).equals(recommendationType))
                .filter(transport -> recommendedPrice < 0 || Math.abs(transport.getPrixParPersonne() - recommendedPrice) < 0.01)
                .findFirst()
                .orElse(null);

        if (exactMatch != null) {
            return exactMatch;
        }

        return transports.stream()
                .filter(transport -> normalize(transport.getType()).equals(recommendationType))
                .findFirst()
                .orElse(null);
    }

    private StackPane buildImageZone(Transport transport) {
        VBox imgBox = new VBox();
        imgBox.getStyleClass().add("reco-card-media");
        imgBox.setPrefHeight(200);
        imgBox.setMinHeight(200);
        imgBox.setMaxHeight(200);
        imgBox.setAlignment(Pos.CENTER);

        Image image = transport == null ? null : loadImage(transport.getImage());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(320);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imgBox.getChildren().add(imageView);
        } else {
            Label fallback = new Label("No image");
            fallback.getStyleClass().add("reco-card-media-fallback");
            imgBox.getChildren().add(fallback);
        }

        return new StackPane(imgBox);
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Image image = new Image(imagePath, 320, 200, false, true, true);
                return image.isError() ? null : image;
            }

            File absoluteFile = new File(imagePath);
            if (absoluteFile.exists()) {
                return new Image(absoluteFile.toURI().toURL().toExternalForm(), 320, 200, false, true);
            }

            String fileName = new File(imagePath).getName();
            File uploadFile = new File(UPLOADS_DIR + fileName);
            if (uploadFile.exists()) {
                return new Image(uploadFile.toURI().toURL().toExternalForm(), 320, 200, false, true);
            }

            URL resource = getClass().getResource("/images/" + fileName);
            if (resource != null) {
                return new Image(resource.toExternalForm(), 320, 200, false, true);
            }
        } catch (MalformedURLException ignored) {
        } catch (Exception ignored) {
        }

        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double extractPrice(String price) {
        if (price == null || price.isBlank()) {
            return -1;
        }

        String normalized = price.replace(',', '.');
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if ((character >= '0' && character <= '9') || character == '.') {
                digits.append(character);
            } else if (digits.length() > 0) {
                break;
            }
        }

        if (digits.isEmpty()) {
            return -1;
        }

        try {
            return Double.parseDouble(digits.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private PriceDisplay resolveDisplayPrice(TransportRecommendation recommendation, Transport transport) {
        if (transport != null) {
            return exchangeRateService.buildPriceDisplay(transport.getPrixParPersonne());
        }

        double recommendedPrice = extractPrice(recommendation == null ? null : recommendation.getPrice());
        if (recommendedPrice >= 0) {
            return exchangeRateService.buildPriceDisplay(recommendedPrice);
        }

        return new PriceDisplay(recommendation == null ? "" : recommendation.getPrice(), "");
    }
}
