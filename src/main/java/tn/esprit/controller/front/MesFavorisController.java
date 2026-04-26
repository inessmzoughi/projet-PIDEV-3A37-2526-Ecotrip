package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tn.esprit.controller.front.modals.ActivityDetailController;
import tn.esprit.controller.front.modals.ActivityReservationController;
import tn.esprit.controller.front.modals.ActivityScheduleController;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityFavoriteService;
import tn.esprit.services.activity.ActivityImageService;
import tn.esprit.services.activity.ActivityMapService;
import tn.esprit.services.activity.ActivityService;
import tn.esprit.utils.CartManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class MesFavorisController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private FlowPane favoritesGrid;
    @FXML private VBox emptyState;

    private final ActivityService activityService = new ActivityService();
    private final ActivityFavoriteService favoriteService = new ActivityFavoriteService();

    private List<Activity> allFavorites = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderFavorites());
        loadFavorites();
    }

    private void loadFavorites() {
        try {
            Set<Integer> favoriteIds = favoriteService.getFavoriteIds();
            allFavorites = activityService.afficherAll().stream()
                    .filter(Activity::isActive)
                    .filter(activity -> favoriteIds.contains(activity.getId()))
                    .sorted(Comparator.comparing(Activity::getTitle, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        } catch (Exception exception) {
            allFavorites = new ArrayList<>();
        }
        renderFavorites();
    }

    private void renderFavorites() {
        favoritesGrid.getChildren().clear();
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<Activity> filtered = allFavorites.stream()
                .filter(activity -> search.isEmpty()
                        || safeLower(activity.getTitle()).contains(search)
                        || safeLower(activity.getLocation()).contains(search)
                        || safeLower(activity.getDescription()).contains(search))
                .collect(Collectors.toList());

        filtered.forEach(activity -> favoritesGrid.getChildren().add(buildCard(activity)));

        boolean isEmpty = filtered.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        favoritesGrid.setVisible(!isEmpty);
        favoritesGrid.setManaged(!isEmpty);
        resultCountLabel.setText(filtered.size() + " activite" + (filtered.size() > 1 ? "s" : "") + " favorite" + (filtered.size() > 1 ? "s" : ""));
    }

    @FXML
    private void handleContact() {
        SceneManager.navigateTo(Routes.ACTIVITES);
    }

    private VBox buildCard(Activity activity) {
        VBox card = new VBox(0);
        card.getStyleClass().add("heb-card");

        card.getChildren().add(buildImageZone(activity));

        VBox body = new VBox(10);
        body.setPadding(new Insets(20, 22, 22, 22));

        if (activity.getCategory() != null) {
            String icon = activity.getCategory().getIcon() != null && !activity.getCategory().getIcon().isBlank()
                    ? activity.getCategory().getIcon()
                    : "";
            Label categoryLabel = new Label((icon.isBlank() ? "" : icon + "  ") + activity.getCategory().getName());
            categoryLabel.getStyleClass().add("heb-card-category");
            body.getChildren().add(categoryLabel);
        }

        Label title = new Label(activity.getTitle());
        title.getStyleClass().add("heb-card-nom");
        title.setWrapText(true);
        body.getChildren().add(title);

        Label location = new Label("📍  " + safeText(activity.getLocation()));
        location.getStyleClass().add("heb-card-ville");
        body.getChildren().add(location);

        if (activity.getDescription() != null && !activity.getDescription().isBlank()) {
            String description = activity.getDescription().length() > 100
                    ? activity.getDescription().substring(0, 100) + "..."
                    : activity.getDescription();
            Label desc = new Label(description);
            desc.getStyleClass().add("heb-card-desc");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        FlowPane meta = new FlowPane(8, 6);
        Label duration = new Label("⏱  " + formatDuration(activity.getDurationMinutes()));
        duration.getStyleClass().add("heb-card-equipement");
        Label participants = new Label("👥  " + activity.getMaxParticipants() + " pers. max");
        participants.getStyleClass().add("heb-card-equipement");
        meta.getChildren().addAll(duration, participants);

        if (ActivityMapService.hasValidCoordinates(activity)) {
            Label mapChip = new Label("🗺  Carte disponible");
            mapChip.getStyleClass().add("heb-card-equipement");
            meta.getChildren().add(mapChip);
        }

        body.getChildren().add(meta);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(String.format("%.0f TND", activity.getPrice()));
        price.getStyleClass().add("heb-card-price-amount");
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        Button detailsBtn = new Button("Voir detail");
        detailsBtn.getStyleClass().add("heb-card-btn");
        detailsBtn.setOnAction(event -> openActivityDetail(activity));
        detailsBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        Button removeBtn = new Button("Retirer");
        removeBtn.getStyleClass().add("activity-favorite-list-btn");
        removeBtn.setOnAction(event -> {
            favoriteService.toggleFavorite(activity.getId());
            loadFavorites();
        });
        removeBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        actions.getChildren().addAll(price, actionSpacer, removeBtn, detailsBtn);
        body.getChildren().addAll(spacer, actions);
        card.getChildren().add(body);
        card.setOnMouseClicked(event -> openActivityDetail(activity));
        return card;
    }

    private StackPane buildImageZone(Activity activity) {
        StackPane imageZone = new StackPane();
        imageZone.getStyleClass().add("heb-card-img");
        imageZone.setPrefHeight(200);
        imageZone.setMinHeight(200);
        imageZone.setMaxHeight(200);

        Image image = ActivityImageService.loadImage(getClass(), activity.getImage(), 320, 200);
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(320);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);

            Rectangle clip = new Rectangle(320, 200);
            clip.setArcWidth(28);
            clip.setArcHeight(28);
            clip.widthProperty().bind(imageZone.widthProperty());
            clip.heightProperty().bind(imageZone.heightProperty());
            imageView.setClip(clip);
            imageZone.getChildren().add(imageView);
        } else {
            VBox fallback = new VBox(8);
            fallback.setAlignment(Pos.CENTER);
            fallback.getStyleClass().add("activity-card-image-fallback");
            Label icon = new Label(activity.getCategory() != null && activity.getCategory().getIcon() != null
                    ? activity.getCategory().getIcon()
                    : "🌿");
            icon.getStyleClass().add("activity-card-image-icon");
            Label title = new Label(activity.getTitle());
            title.getStyleClass().add("activity-card-image-text");
            title.setWrapText(true);
            fallback.getChildren().addAll(icon, title);
            imageZone.getChildren().add(fallback);
        }

        Label favoriteBadge = new Label("♥ Favori");
        favoriteBadge.getStyleClass().add("activity-favorite-badge");
        imageZone.getChildren().add(favoriteBadge);
        StackPane.setAlignment(favoriteBadge, Pos.TOP_LEFT);
        StackPane.setMargin(favoriteBadge, new Insets(14, 0, 0, 14));

        return imageZone;
    }

    private void openActivityDetail(Activity activity) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/ActivityDetailModal.fxml")
            );
            StackPane detailOverlay = loader.load();
            StackPane overlayContainer = ensureOverlayContainer();

            ActivityDetailController controller = loader.getController();
            controller.setActivity(activity);
            controller.setFavoriteService(favoriteService);
            controller.setOverlayRoot(detailOverlay);
            controller.setOnScheduleRequested(() -> {
                overlayContainer.getChildren().remove(detailOverlay);
                openScheduleModal(activity, overlayContainer);
            });
            controller.setOnReserveRequested(() -> {
                overlayContainer.getChildren().remove(detailOverlay);
                openReservationModal(activity, overlayContainer);
            });

            showOverlay(overlayContainer, detailOverlay);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void openScheduleModal(Activity activity, StackPane overlayContainer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/ActivityScheduleModal.fxml")
            );
            StackPane scheduleOverlay = loader.load();

            ActivityScheduleController controller = loader.getController();
            controller.setActivity(activity);
            controller.setOverlayRoot(scheduleOverlay);
            controller.setOnReserveRequested(schedule -> {
                overlayContainer.getChildren().remove(scheduleOverlay);
                openReservationModal(activity, overlayContainer, schedule);
            });

            showOverlay(overlayContainer, scheduleOverlay);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void openReservationModal(Activity activity, StackPane overlayContainer) {
        openReservationModal(activity, overlayContainer, null);
    }

    private void openReservationModal(Activity activity, StackPane overlayContainer, ActivitySchedule schedule) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/front/modals/ActivityReservationModal.fxml")
            );
            StackPane modalOverlay = loader.load();

            ActivityReservationController controller = loader.getController();
            if (schedule != null) {
                controller.setPreselectedSchedule(schedule);
            }
            controller.setActivity(activity);
            controller.setOverlayRoot(modalOverlay);
            controller.setOnCartUpdated(() -> {
                System.out.println("Cart: " + CartManager.getInstance().getCount());
                overlayContainer.getChildren().get(0).setEffect(null);
            });

            showOverlay(overlayContainer, modalOverlay);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private StackPane ensureOverlayContainer() {
        Parent rootNode = favoritesGrid.getScene().getRoot();
        if (rootNode instanceof StackPane stackPane) {
            return stackPane;
        }

        StackPane overlayContainer = new StackPane();
        Scene scene = rootNode.getScene();
        overlayContainer.getChildren().add(rootNode);
        scene.setRoot(overlayContainer);
        return overlayContainer;
    }

    private void showOverlay(StackPane overlayContainer, StackPane overlay) {
        overlayContainer.getChildren().add(overlay);
        if (!overlayContainer.getChildren().isEmpty()) {
            overlayContainer.getChildren().get(0).setEffect(new GaussianBlur(8));
        }
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        return mins == 0 ? hours + "h" : hours + "h" + mins + "min";
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
