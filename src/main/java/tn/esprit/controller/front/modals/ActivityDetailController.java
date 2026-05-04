package tn.esprit.controller.front.modals;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivityMetrics;
import tn.esprit.models.activity.ActivityWeatherSnapshot;
import tn.esprit.services.activity.ActivityFavoriteService;
import tn.esprit.services.activity.ActivityImageService;
import tn.esprit.services.activity.ActivityMapService;
import tn.esprit.services.activity.ActivityService;
import tn.esprit.services.activity.ActivityWeatherService;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class ActivityDetailController implements Initializable {

    private static final DateTimeFormatter DEPARTURE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE d MMM - HH:mm", Locale.FRANCE);

    @FXML private StackPane heroImagePane;
    @FXML private Label categoryLabel;
    @FXML private Label availabilityBadge;
    @FXML private Label titleLabel;
    @FXML private Label locationLabel;
    @FXML private Label durationLabel;
    @FXML private Label participantsLabel;
    @FXML private Label guideLabel;
    @FXML private Label bookingCountLabel;
    @FXML private Label occupancyRateLabel;
    @FXML private Label remainingSpotsLabel;
    @FXML private Label nextDepartureLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label weatherSuitabilityBadge;
    @FXML private Label weatherConditionLabel;
    @FXML private Label weatherTemperatureLabel;
    @FXML private Label weatherWindLabel;
    @FXML private Label weatherAdvisoryLabel;
    @FXML private Label priceAmountLabel;
    @FXML private Label mapStateLabel;
    @FXML private Button openMapBtn;
    @FXML private WebView mapView;
    @FXML private Button favoriteButton;

    private Activity selectedActivity;
    private StackPane overlayRoot;
    private Runnable onReserveRequested;
    private Runnable onScheduleRequested;
    private ActivityFavoriteService favoriteService;
    private final ActivityService activityService = new ActivityService();
    private final ActivityWeatherService weatherService = new ActivityWeatherService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mapStateLabel.managedProperty().bind(mapStateLabel.visibleProperty());
        mapStateLabel.setVisible(false);
    }

    public void setActivity(Activity activity) {
        this.selectedActivity = activity;
        renderHeader();
        renderContent();
        renderWeather();
        renderMap();
        refreshFavoriteButton();
    }

    public void setOverlayRoot(StackPane overlayRoot) {
        this.overlayRoot = overlayRoot;
    }

    public void setOnReserveRequested(Runnable onReserveRequested) {
        this.onReserveRequested = onReserveRequested;
    }

    public void setOnScheduleRequested(Runnable onScheduleRequested) {
        this.onScheduleRequested = onScheduleRequested;
    }

    public void setFavoriteService(ActivityFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
        refreshFavoriteButton();
    }

    @FXML
    private void onClose() {
        closeOverlay();
    }

    @FXML
    private void onReserve() {
        if (onReserveRequested != null) {
            onReserveRequested.run();
        }
    }

    @FXML
    private void onShowSchedule() {
        if (onScheduleRequested != null) {
            onScheduleRequested.run();
        }
    }

    @FXML
    private void onToggleFavorite() {
        if (selectedActivity == null || favoriteService == null) {
            return;
        }
        favoriteService.toggleFavorite(selectedActivity.getId());
        refreshFavoriteButton();
    }

    @FXML
    private void onOpenMap() {
        if (selectedActivity == null || !ActivityMapService.hasValidCoordinates(selectedActivity)) {
            mapStateLabel.setText("Coordonnees indisponibles pour cette activite.");
            mapStateLabel.setVisible(true);
            mapStateLabel.getStyleClass().removeAll("activity-detail-map-error", "activity-detail-map-success");
            mapStateLabel.getStyleClass().add("activity-detail-map-error");
            return;
        }

        try {
            Desktop.getDesktop().browse(new URI(
                    ActivityMapService.buildOpenStreetMapUrl(
                            selectedActivity.getLatitude(),
                            selectedActivity.getLongitude()
                    )
            ));
        } catch (Exception exception) {
            mapStateLabel.setText("Impossible d'ouvrir la carte.");
            mapStateLabel.setVisible(true);
            mapStateLabel.getStyleClass().removeAll("activity-detail-map-error", "activity-detail-map-success");
            mapStateLabel.getStyleClass().add("activity-detail-map-error");
        }
    }

    private void renderHeader() {
        heroImagePane.getChildren().clear();

        Image image = ActivityImageService.loadImage(getClass(), selectedActivity.getImage(), 720, 260);
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(720);
            imageView.setFitHeight(260);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);

            Rectangle clip = new Rectangle(720, 260);
            clip.setArcWidth(28);
            clip.setArcHeight(28);
            imageView.setClip(clip);

            heroImagePane.getChildren().add(imageView);
        } else {
            VBox fallback = new VBox(8);
            fallback.setAlignment(Pos.CENTER);
            fallback.getStyleClass().add("activity-detail-hero-fallback");

            Label icon = new Label(selectedActivity.getCategory() != null && selectedActivity.getCategory().getIcon() != null
                    ? selectedActivity.getCategory().getIcon()
                    : "🌿");
            icon.getStyleClass().add("activity-detail-hero-icon");
            Label text = new Label("Experience EcoTrip");
            text.getStyleClass().add("activity-detail-hero-text");
            fallback.getChildren().addAll(icon, text);
            heroImagePane.getChildren().add(fallback);
        }

        if (selectedActivity.isActive()) {
            Label badge = new Label("Disponible");
            badge.getStyleClass().add("activity-detail-availability");
            heroImagePane.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(18, 18, 0, 0));
        }
    }

    private void renderContent() {
        categoryLabel.setText(selectedActivity.getCategory() != null
                ? selectedActivity.getCategory().getName()
                : "Activite");
        availabilityBadge.setText(selectedActivity.isActive() ? "Reservee aux experiences ouvertes" : "Activite temporairement indisponible");
        titleLabel.setText(selectedActivity.getTitle());
        locationLabel.setText(selectedActivity.getLocation());
        durationLabel.setText(formatDuration(selectedActivity.getDurationMinutes()));
        participantsLabel.setText(selectedActivity.getMaxParticipants() + " participants max");
        guideLabel.setText(selectedActivity.getGuide() != null
                ? selectedActivity.getGuide().getFirstName() + " " + selectedActivity.getGuide().getLastName()
                : "Guide communique apres reservation");
        descriptionLabel.setText(selectedActivity.getDescription() == null || selectedActivity.getDescription().isBlank()
                ? "Cette activite vous plonge dans une experience nature pensee pour l'exploration douce et le tourisme responsable."
                : selectedActivity.getDescription());
        priceAmountLabel.setText(String.format("%.0f TND", selectedActivity.getPrice()));
        renderMetrics();
    }

    private void renderMap() {
        if (!ActivityMapService.hasValidCoordinates(selectedActivity)) {
            mapView.getEngine().loadContent(
                    ActivityMapService.buildEmptyStateHtml(
                            "Localisation non disponible",
                            "Les coordonnees de cette activite n'ont pas encore ete renseignees."
                    )
            );
            openMapBtn.setDisable(true);
            mapStateLabel.setText("La carte sera visible des que les coordonnees seront ajoutees.");
            mapStateLabel.setVisible(true);
            mapStateLabel.getStyleClass().removeAll("activity-detail-map-error", "activity-detail-map-success");
            mapStateLabel.getStyleClass().add("activity-detail-map-error");
            return;
        }

        mapView.getEngine().loadContent(
                ActivityMapService.buildMapHtml(
                        selectedActivity.getTitle(),
                        selectedActivity.getLocation(),
                        selectedActivity.getLatitude(),
                        selectedActivity.getLongitude()
                )
        );
        openMapBtn.setDisable(false);
        mapStateLabel.setText("Position confirmee sur la carte.");
        mapStateLabel.setVisible(true);
        mapStateLabel.getStyleClass().removeAll("activity-detail-map-error", "activity-detail-map-success");
        mapStateLabel.getStyleClass().add("activity-detail-map-success");
    }

    private void refreshFavoriteButton() {
        if (favoriteButton == null || selectedActivity == null || favoriteService == null) {
            return;
        }
        boolean favorite = favoriteService.isFavorite(selectedActivity.getId());
        favoriteButton.setText(favorite ? "♥ Favori" : "♡ Favori");
        favoriteButton.getStyleClass().removeAll("activity-favorite-detail-btn", "activity-favorite-detail-btn-active");
        favoriteButton.getStyleClass().add("activity-favorite-detail-btn");
        if (favorite) {
            favoriteButton.getStyleClass().add("activity-favorite-detail-btn-active");
        }
    }

    private void renderWeather() {
        if (weatherConditionLabel == null || weatherSuitabilityBadge == null) {
            return;
        }

        if (!ActivityMapService.hasValidCoordinates(selectedActivity)) {
            showWeatherUnavailable("Meteo indisponible", "Ajoutez des coordonnees valides pour obtenir une lecture meteo.");
            return;
        }

        weatherSuitabilityBadge.setText("Analyse meteo");
        weatherSuitabilityBadge.getStyleClass().removeAll(
                "activity-weather-badge-good",
                "activity-weather-badge-watch",
                "activity-weather-badge-risk"
        );
        weatherConditionLabel.setText("Chargement en cours...");
        weatherTemperatureLabel.setText("--");
        weatherWindLabel.setText("--");
        weatherAdvisoryLabel.setText("Nous analysons les conditions actuelles autour de cette activite.");

        Task<ActivityWeatherSnapshot> task = new Task<>() {
            @Override
            protected ActivityWeatherSnapshot call() {
                return weatherService.fetchCurrentWeather(selectedActivity);
            }
        };

        task.setOnSucceeded(event -> updateWeatherUI(task.getValue()));
        task.setOnFailed(event -> showWeatherUnavailable("Meteo non disponible", "Impossible de recuperer les conditions actuelles pour le moment."));

        Thread worker = new Thread(task, "activity-weather-task");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderMetrics() {
        if (selectedActivity == null) {
            return;
        }

        try {
            ActivityMetrics metrics = activityService.getMetrics(selectedActivity);
            bookingCountLabel.setText(metrics.getBookingCount() + " reservation(s)");
            occupancyRateLabel.setText(metrics.getOccupancyRate() + "%");
            remainingSpotsLabel.setText(metrics.getRemainingSpots() + " places");
            nextDepartureLabel.setText(formatDeparture(metrics.getNextDeparture()));

            if (selectedActivity.isActive()) {
                availabilityBadge.setText(buildAvailabilityText(metrics));
            }
        } catch (Exception exception) {
            bookingCountLabel.setText("-");
            occupancyRateLabel.setText("-");
            remainingSpotsLabel.setText("-");
            nextDepartureLabel.setText("A confirmer");
        }
    }

    private void updateWeatherUI(ActivityWeatherSnapshot snapshot) {
        if (snapshot == null) {
            showWeatherUnavailable("Meteo non disponible", "Impossible de recuperer les conditions actuelles pour le moment.");
            return;
        }

        weatherSuitabilityBadge.setText(snapshot.getSuitabilityLabel());
        weatherSuitabilityBadge.getStyleClass().removeAll(
                "activity-weather-badge-good",
                "activity-weather-badge-watch",
                "activity-weather-badge-risk"
        );
        weatherSuitabilityBadge.getStyleClass().add(resolveWeatherBadgeClass(snapshot.getSuitabilityLabel()));
        weatherConditionLabel.setText(snapshot.getConditionLabel());
        weatherTemperatureLabel.setText(String.format(Locale.US, "%.0f°C ressenti %.0f°C",
                snapshot.getTemperatureCelsius(),
                snapshot.getApparentTemperatureCelsius()));
        weatherWindLabel.setText(String.format(Locale.US, "%.0f km/h · pluie %.1f mm",
                snapshot.getWindSpeedKmh(),
                snapshot.getPrecipitationMm()));
        weatherAdvisoryLabel.setText(snapshot.getAdvisoryText());
    }

    private void showWeatherUnavailable(String badgeText, String advisory) {
        weatherSuitabilityBadge.setText(badgeText);
        weatherSuitabilityBadge.getStyleClass().removeAll(
                "activity-weather-badge-good",
                "activity-weather-badge-watch",
                "activity-weather-badge-risk"
        );
        weatherConditionLabel.setText("Conditions inconnues");
        weatherTemperatureLabel.setText("--");
        weatherWindLabel.setText("--");
        weatherAdvisoryLabel.setText(advisory);
    }

    private String resolveWeatherBadgeClass(String suitability) {
        if (suitability == null) {
            return "activity-weather-badge-watch";
        }
        String normalized = suitability.toLowerCase(Locale.ROOT);
        if (normalized.contains("ideale") || normalized.contains("confortable")) {
            return "activity-weather-badge-good";
        }
        if (normalized.contains("peu favorables")) {
            return "activity-weather-badge-risk";
        }
        return "activity-weather-badge-watch";
    }

    private String buildAvailabilityText(ActivityMetrics metrics) {
        if (metrics.getFutureScheduleCount() == 0) {
            return "Aucun depart publie";
        }
        if (metrics.getRemainingSpots() <= 0) {
            return "Complet sur les departs publies";
        }
        if (metrics.getOccupancyRate() >= 80) {
            return "Depart tres demandes";
        }
        return metrics.getFutureScheduleCount() + " depart(s) disponible(s)";
    }

    private void closeOverlay() {
        if (overlayRoot != null && overlayRoot.getParent() instanceof StackPane parent) {
            if (!parent.getChildren().isEmpty()) {
                parent.getChildren().get(0).setEffect(null);
            }
            parent.getChildren().remove(overlayRoot);
        }
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return remainingMinutes == 0 ? hours + "h" : hours + "h" + remainingMinutes + "min";
    }

    private String formatDeparture(LocalDateTime departure) {
        if (departure == null) {
            return "A confirmer";
        }
        return DEPARTURE_FORMATTER.format(departure);
    }
}
