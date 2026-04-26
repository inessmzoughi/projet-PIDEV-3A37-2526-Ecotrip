package tn.esprit.controller.front.modals;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import tn.esprit.services.activity.ActivityFavoriteService;
import tn.esprit.services.activity.ActivityImageService;
import tn.esprit.services.activity.ActivityMapService;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class ActivityDetailController implements Initializable {

    @FXML private StackPane heroImagePane;
    @FXML private Label categoryLabel;
    @FXML private Label availabilityBadge;
    @FXML private Label titleLabel;
    @FXML private Label locationLabel;
    @FXML private Label durationLabel;
    @FXML private Label participantsLabel;
    @FXML private Label guideLabel;
    @FXML private Label descriptionLabel;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mapStateLabel.managedProperty().bind(mapStateLabel.visibleProperty());
        mapStateLabel.setVisible(false);
    }

    public void setActivity(Activity activity) {
        this.selectedActivity = activity;
        renderHeader();
        renderContent();
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
}
