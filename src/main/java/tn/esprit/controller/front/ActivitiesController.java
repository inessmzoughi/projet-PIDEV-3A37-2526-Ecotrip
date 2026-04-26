package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
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
import tn.esprit.models.activity.ActivityCategory;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityAssistantService;
import tn.esprit.services.activity.ActivityCategoryService;
import tn.esprit.services.activity.ActivityFavoriteService;
import tn.esprit.services.activity.ActivityImageService;
import tn.esprit.services.activity.ActivityMapService;
import tn.esprit.services.activity.ActivityService;
import tn.esprit.utils.CartManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ActivitiesController implements Initializable {

    private static final String ALL_CATEGORIES = "categories";
    private static final int PER_PAGE = 8;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private Button tabAll;
    @FXML private HBox categoryTabsBox;
    @FXML private Label resultsCountLabel;
    @FXML private Label activeFilterLabel;
    @FXML private Button clearFilterBtn;
    @FXML private FlowPane activitiesGrid;
    @FXML private VBox emptyState;
    @FXML private HBox paginationBar;
    @FXML private Label pagInfo;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Button chatbotLauncher;
    @FXML private VBox chatbotPanel;
    @FXML private Label chatbotGreetingLabel;
    @FXML private Label chatbotStatusLabel;
    @FXML private VBox chatbotSuggestionsBox;
    @FXML private TextField chatbotInputField;
    @FXML private Button chatbotSendBtn;

    private final ActivityService activityService = new ActivityService();
    private final ActivityCategoryService categoryService = new ActivityCategoryService();
    private final ActivityAssistantService assistantService = new ActivityAssistantService();
    private final ActivityFavoriteService favoriteService = new ActivityFavoriteService();

    private final List<ActivityCategory> categories = new ArrayList<>();
    private List<Activity> allData = new ArrayList<>();
    private List<Activity> filteredData = new ArrayList<>();
    private Integer activeCategoryId;
    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.getItems().addAll(
                "Prix croissant",
                "Prix decroissant",
                "Duree croissante",
                "Alphabetique"
        );

        categoryCombo.getItems().add(ALL_CATEGORIES);
        categoryCombo.getSelectionModel().select(ALL_CATEGORIES);

        sortCombo.setOnAction(event -> applyFilters());
        categoryCombo.setOnAction(event -> handleCategoryCombo());
        searchField.setOnKeyReleased(event -> handleSearch());

        initializeChatbot();
        loadCategories();
        loadActivities();
    }

    private void initializeChatbot() {
        chatbotPanel.setVisible(false);
        chatbotPanel.setManaged(false);
        chatbotPanel.setMouseTransparent(false);
        chatbotInputField.setEditable(true);
        chatbotInputField.setFocusTraversable(true);
        chatbotGreetingLabel.setText("Salut ! Je suis ton assistant EcoTrip.");
        chatbotStatusLabel.setText(
                "Tu veux une activite a Tunis, a Djerba, quelque chose de calme, d'aventure ou de culturel ? Dis-moi ton envie."
        );
        chatbotSuggestionsBox.getChildren().clear();
    }

    private void loadCategories() {
        try {
            categories.clear();
            categories.addAll(categoryService.afficherAll());

            for (ActivityCategory category : categories) {
                String icon = category.getIcon() != null && !category.getIcon().isBlank()
                        ? category.getIcon()
                        : "";
                Button tab = new Button((icon.isBlank() ? "" : icon + "  ") + category.getName());
                tab.getStyleClass().add("act-tab");
                tab.setOnAction(event -> handleCategoryTab(tab, category.getId(), category.getName()));
                categoryTabsBox.getChildren().add(tab);
                categoryCombo.getItems().add(category.getName());
            }
        } catch (Exception exception) {
            System.err.println("Error loading categories: " + exception.getMessage());
        }
    }

    private void loadActivities() {
        try {
            allData = activityService.afficherAll();
            filteredData = new ArrayList<>(allData);
            renderPage();
            renderChatbotSuggestions(allData.stream().filter(Activity::isActive).limit(2).collect(Collectors.toList()));
        } catch (Exception exception) {
            System.err.println("Error loading activities: " + exception.getMessage());
            resultsCountLabel.setText("Erreur de chargement");
            chatbotStatusLabel.setText("Le catalogue des activites n'a pas pu etre charge pour le moment.");
        }
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String sort = sortCombo.getValue();

        filteredData = allData.stream()
                .filter(Activity::isActive)
                .filter(activity -> activeCategoryId == null
                        || (activity.getCategory() != null && activity.getCategory().getId() == activeCategoryId))
                .filter(activity -> search.isEmpty()
                        || safeLower(activity.getTitle()).contains(search)
                        || safeLower(activity.getLocation()).contains(search)
                        || safeLower(activity.getDescription()).contains(search))
                .collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "Prix croissant" ->
                        filteredData.sort((first, second) -> Double.compare(first.getPrice(), second.getPrice()));
                case "Prix decroissant" ->
                        filteredData.sort((first, second) -> Double.compare(second.getPrice(), first.getPrice()));
                case "Duree croissante" ->
                        filteredData.sort((first, second) -> Integer.compare(first.getDurationMinutes(), second.getDurationMinutes()));
                case "Alphabetique" ->
                        filteredData.sort((first, second) -> safeLower(first.getTitle()).compareTo(safeLower(second.getTitle())));
                default -> {
                }
            }
        }

        currentPage = 1;
        renderPage();
    }

    private void renderPage() {
        activitiesGrid.getChildren().clear();

        boolean isEmpty = filteredData == null || filteredData.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        activitiesGrid.setVisible(!isEmpty);
        activitiesGrid.setManaged(!isEmpty);

        int total = isEmpty ? 0 : filteredData.size();
        resultsCountLabel.setText(total + " Activite" + (total > 1 ? "s" : "") + " trouvee" + (total > 1 ? "s" : ""));

        if (isEmpty) {
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            return;
        }

        int totalPages = (int) Math.ceil((double) total / PER_PAGE);
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PER_PAGE;
        int to = Math.min(from + PER_PAGE, total);

        filteredData.subList(from, to).forEach(activity -> activitiesGrid.getChildren().add(buildCard(activity)));

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
    }

    @FXML
    private void onPrev() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void onNext() {
        int totalPages = (int) Math.ceil((double) filteredData.size() / PER_PAGE);
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
        }
    }

    private VBox buildCard(Activity activity) {
        VBox card = new VBox(0);
        card.getStyleClass().add("heb-card");

        card.getChildren().add(buildImageZone(activity, true));

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

        if (activity.getGuide() != null) {
            Label guide = new Label("🧭  " + activity.getGuide().getFirstName() + " " + activity.getGuide().getLastName());
            guide.getStyleClass().add("heb-card-equipement");
            meta.getChildren().add(guide);
        }
        body.getChildren().add(meta);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox priceRow = new HBox(0);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        priceRow.setPadding(new Insets(10, 0, 0, 0));

        VBox priceBox = new VBox(2);
        Label price = new Label(String.format("%.0f", activity.getPrice()));
        Label currency = new Label("TND");
        Label unit = new Label("/ personne");
        price.getStyleClass().add("heb-card-price-amount");
        currency.getStyleClass().add("heb-card-price-currency");
        unit.getStyleClass().add("heb-card-price-unit");
        priceBox.getChildren().addAll(price, currency, unit);

        Region priceSpace = new Region();
        HBox.setHgrow(priceSpace, Priority.ALWAYS);

        Button button = new Button("Voir detail");
        button.getStyleClass().add("heb-card-btn");
        button.setOnAction(event -> openActivityDetail(activity));
        button.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        priceRow.getChildren().addAll(priceBox, priceSpace, button);
        body.getChildren().addAll(spacer, priceRow);
        card.getChildren().add(body);
        card.setOnMouseClicked(event -> openActivityDetail(activity));

        return card;
    }

    private StackPane buildImageZone(Activity activity, boolean includeFavoriteToggle) {
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

        if (activity.isActive()) {
            Label badge = new Label("Disponible");
            badge.getStyleClass().add("heb-card-eco-badge");
            imageZone.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(14, 14, 0, 0));
        }

        if (includeFavoriteToggle) {
            Button favoriteBtn = buildFavoriteButton(activity, favoriteService.isFavorite(activity.getId()));
            favoriteBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
            imageZone.getChildren().add(favoriteBtn);
            StackPane.setAlignment(favoriteBtn, Pos.TOP_LEFT);
            StackPane.setMargin(favoriteBtn, new Insets(14, 0, 0, 14));
        }

        return imageZone;
    }

    private Button buildFavoriteButton(Activity activity, boolean favorite) {
        Button button = new Button();
        updateFavoriteButton(button, favorite);
        button.setOnAction(event -> {
            boolean nowFavorite = favoriteService.toggleFavorite(activity.getId());
            updateFavoriteButton(button, nowFavorite);
        });
        return button;
    }

    private void updateFavoriteButton(Button button, boolean favorite) {
        button.setText(favorite ? "♥" : "♡");
        button.getStyleClass().removeAll("activity-favorite-btn", "activity-favorite-btn-active");
        button.getStyleClass().add("activity-favorite-btn");
        if (favorite) {
            button.getStyleClass().add("activity-favorite-btn-active");
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

    private void setActiveTab(Button selected) {
        tabAll.getStyleClass().remove("act-tab-active");
        for (var node : categoryTabsBox.getChildren()) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("act-tab-active");
            }
        }
        if (!selected.getStyleClass().contains("act-tab-active")) {
            selected.getStyleClass().add("act-tab-active");
        }
    }

    private void showFilterBadge(String text) {
        activeFilterLabel.setText(text);
        activeFilterLabel.setVisible(true);
        activeFilterLabel.setManaged(true);
        clearFilterBtn.setVisible(true);
        clearFilterBtn.setManaged(true);
    }

    private void hideFilterBadge() {
        activeFilterLabel.setVisible(false);
        activeFilterLabel.setManaged(false);
        clearFilterBtn.setVisible(false);
        clearFilterBtn.setManaged(false);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (!query.isEmpty()) {
            showFilterBadge("🔍 \"" + query + "\"");
        } else if (activeCategoryId == null) {
            hideFilterBadge();
        }
        applyFilters();
    }

    @FXML
    private void handleTabAll() {
        activeCategoryId = null;
        setActiveTab(tabAll);
        categoryCombo.setValue(ALL_CATEGORIES);
        if (searchField.getText() == null || searchField.getText().isBlank()) {
            hideFilterBadge();
        }
        applyFilters();
    }

    private void handleCategoryTab(Button tab, int categoryId, String categoryName) {
        activeCategoryId = categoryId;
        setActiveTab(tab);
        categoryCombo.setValue(categoryName);
        showFilterBadge(categoryName);
        applyFilters();
    }

    @FXML
    private void handleCategoryCombo() {
        String selectedCategory = categoryCombo.getValue();
        if (selectedCategory == null || selectedCategory.equals(ALL_CATEGORIES)) {
            handleTabAll();
            return;
        }

        for (ActivityCategory category : categories) {
            if (category.getName().equals(selectedCategory)) {
                for (var node : categoryTabsBox.getChildren()) {
                    if (node instanceof Button button && button.getText().contains(selectedCategory)) {
                        handleCategoryTab(button, category.getId(), category.getName());
                        return;
                    }
                }
            }
        }
    }

    @FXML
    private void handleClearFilter() {
        activeCategoryId = null;
        searchField.clear();
        sortCombo.getSelectionModel().clearSelection();
        categoryCombo.setValue(ALL_CATEGORIES);
        setActiveTab(tabAll);
        hideFilterBadge();
        applyFilters();
    }

    @FXML
    private void handleResetFilters() {
        handleClearFilter();
    }

    @FXML
    private void toggleChatbot() {
        boolean shouldShow = !chatbotPanel.isVisible();
        chatbotPanel.setVisible(shouldShow);
        chatbotPanel.setManaged(shouldShow);
        chatbotLauncher.setVisible(!shouldShow);
        chatbotLauncher.setManaged(!shouldShow);
        if (shouldShow) {
            Platform.runLater(() -> {
                chatbotInputField.requestFocus();
                chatbotInputField.positionCaret(chatbotInputField.getText() == null ? 0 : chatbotInputField.getText().length());
            });
        }
    }

    @FXML
    private void handleChatbotSend() {
        String prompt = chatbotInputField.getText() == null ? "" : chatbotInputField.getText().trim();
        if (prompt.isBlank()) {
            chatbotStatusLabel.setText("Donne-moi une ville, une categorie ou une ambiance pour que je puisse t'aider.");
            return;
        }

        chatbotSendBtn.setDisable(true);
        chatbotStatusLabel.setText("Je prepare des suggestions personnalisees dans le style EcoTrip...");

        Task<ActivityAssistantService.ActivitySuggestionResponse> task = new Task<>() {
            @Override
            protected ActivityAssistantService.ActivitySuggestionResponse call() {
                return assistantService.suggestActivities(prompt, allData);
            }
        };

        task.setOnSucceeded(event -> {
            chatbotSendBtn.setDisable(false);
            ActivityAssistantService.ActivitySuggestionResponse result = task.getValue();
            chatbotGreetingLabel.setText(result.responseText());
            chatbotStatusLabel.setText(
                    result.suggestions().isEmpty()
                            ? "Je n'ai pas trouve mieux pour cette demande. Essaie avec une autre ville ou categorie."
                            : "Voici les activites qui correspondent le mieux a ta demande."
            );
            renderChatbotSuggestions(result.suggestions());
        });

        task.setOnFailed(event -> {
            chatbotSendBtn.setDisable(false);
            chatbotStatusLabel.setText("Je n'ai pas pu traiter la demande pour le moment.");
        });

        Thread worker = new Thread(task, "activity-chatbot-task");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleChatbotNature() {
        chatbotInputField.setText("Je cherche une activite nature a Djerba dans une ambiance calme.");
        handleChatbotSend();
    }

    @FXML
    private void handleChatbotCulture() {
        chatbotInputField.setText("Je veux une activite culturelle a Tunis avec une vraie immersion locale.");
        handleChatbotSend();
    }

    @FXML
    private void handleChatbotAdventure() {
        chatbotInputField.setText("Je cherche une activite aventure dans le desert pour un petit groupe.");
        handleChatbotSend();
    }

    @FXML
    private void handleContact() {
        SceneManager.navigateTo(Routes.CONTACT);
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
            controller.setOverlayRoot(detailOverlay);
            controller.setFavoriteService(favoriteService);
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
        Parent rootNode = activitiesGrid.getScene().getRoot();
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

    private void renderChatbotSuggestions(List<Activity> suggestions) {
        chatbotSuggestionsBox.getChildren().clear();
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }

        suggestions.stream()
                .limit(3)
                .forEach(activity -> chatbotSuggestionsBox.getChildren().add(buildChatbotCard(activity)));
    }

    private HBox buildChatbotCard(Activity activity) {
        HBox card = new HBox(12);
        card.getStyleClass().add("activity-chatbot-card");

        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("activity-chatbot-card-image");
        imagePane.setPrefSize(88, 88);
        imagePane.setMinSize(88, 88);
        imagePane.setMaxSize(88, 88);

        Image image = ActivityImageService.loadImage(getClass(), activity.getImage(), 88, 88);
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(88);
            imageView.setFitHeight(88);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);

            Rectangle clip = new Rectangle(88, 88);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imageView.setClip(clip);
            imagePane.getChildren().add(imageView);
        } else {
            Label icon = new Label(activity.getCategory() != null && activity.getCategory().getIcon() != null
                    ? activity.getCategory().getIcon()
                    : "🌿");
            icon.getStyleClass().add("activity-chatbot-card-fallback-icon");
            imagePane.getChildren().add(icon);
        }

        VBox body = new VBox(6);
        body.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(activity.getTitle());
        title.getStyleClass().add("activity-chatbot-card-title");
        title.setWrapText(true);

        Label meta = new Label(safeText(activity.getLocation()) + " • " + formatDuration(activity.getDurationMinutes()));
        meta.getStyleClass().add("activity-chatbot-card-meta");

        Label description = new Label(activity.getDescription() == null || activity.getDescription().isBlank()
                ? "Une experience EcoTrip pensee pour une decouverte locale plus douce et plus immersive."
                : activity.getDescription());
        description.getStyleClass().add("activity-chatbot-card-desc");
        description.setWrapText(true);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(String.format("%.0f TND", activity.getPrice()));
        price.getStyleClass().add("activity-chatbot-card-price");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button detailsBtn = new Button("Voir details");
        detailsBtn.getStyleClass().add("activity-chatbot-card-btn");
        detailsBtn.setOnAction(event -> openActivityDetail(activity));
        actions.getChildren().addAll(price, spacer, detailsBtn);

        body.getChildren().addAll(title, meta, description, actions);
        card.getChildren().addAll(imagePane, body);
        return card;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
