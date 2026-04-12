package org.example.controller.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import org.example.models.activity.Activity;
import org.example.models.activity.ActivityCategory;
import org.example.navigation.SceneManager;
import org.example.navigation.Routes;
import org.example.services.activity.ActivityService;
import org.example.services.activity.ActivityCategoryService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ActivitiesController implements Initializable {

    // ── FXML injections ─────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button           tabAll;
    @FXML private HBox             categoryTabsBox;
    @FXML private Label            resultsCountLabel;
    @FXML private Label            activeFilterLabel;
    @FXML private Button           clearFilterBtn;
    @FXML private Slider           priceSlider;
    @FXML private Label            priceLabel;
    @FXML private RadioButton      durAll;
    @FXML private RadioButton      durShort;
    @FXML private RadioButton      durMed;
    @FXML private RadioButton      durLong;
    @FXML private Spinner<Integer> participantsSpinner;
    @FXML private FlowPane         activitiesGrid;
    @FXML private VBox             emptyState;

    // ── Services ─────────────────────────────────────────────
    private final ActivityService         activityService         = new ActivityService();
    private final ActivityCategoryService categoryService         = new ActivityCategoryService();

    // ── State ────────────────────────────────────────────────
    private List<Activity>         allActivities  = new ArrayList<>();
    private Integer                activeCategoryId = null; // null = show all
    private ToggleGroup            durationGroup;

    // ────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Sort options
        sortCombo.getItems().addAll(
                "Prix croissant",
                "Prix décroissant",
                "Durée croissante",
                "Alphabétique"
        );

        // Duration toggle group
        durationGroup = new ToggleGroup();
        durAll.setToggleGroup(durationGroup);
        durShort.setToggleGroup(durationGroup);
        durMed.setToggleGroup(durationGroup);
        durLong.setToggleGroup(durationGroup);

        // Price slider — live label update
        priceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                priceLabel.setText((int) newVal.doubleValue() + " TND")
        );

        // Load data
        loadCategories();
        loadActivities();
    }

    // ── Data loading ─────────────────────────────────────────

    private void loadCategories() {
        try {
            List<ActivityCategory> categories = categoryService.afficherAll();
            for (ActivityCategory cat : categories) {
                Button tab = new Button(cat.getIcon() + "  " + cat.getName());
                tab.getStyleClass().add("act-tab");
                tab.setOnAction(e -> handleCategoryTab(tab, cat.getId(), cat.getName()));
                categoryTabsBox.getChildren().add(tab);
            }
        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
        }
    }

    private void loadActivities() {
        try {
            allActivities = activityService.afficherAll();
            applyFilters();
        } catch (Exception e) {
            System.err.println("Error loading activities: " + e.getMessage());
            resultsCountLabel.setText("Erreur de chargement");
        }
    }

    // ── Filtering & rendering ────────────────────────────────

    private void applyFilters() {
        String search      = searchField.getText().trim().toLowerCase();
        double maxPrice    = priceSlider.getValue();
        int    minPart     = participantsSpinner.getValue();
        String sortOption  = sortCombo.getValue();

        List<Activity> filtered = allActivities.stream()
                // active only
                .filter(Activity::isActive)
                // category tab
                .filter(a -> activeCategoryId == null
                        || a.getCategory().getId() == activeCategoryId)
                // search text
                .filter(a -> search.isEmpty()
                        || a.getTitle().toLowerCase().contains(search)
                        || a.getLocation().toLowerCase().contains(search)
                        || a.getDescription().toLowerCase().contains(search))
                // price
                .filter(a -> a.getPrice() <= maxPrice)
                // participants
                .filter(a -> a.getMaxParticipants() >= minPart)
                // duration
                .filter(a -> {
                    if (durShort.isSelected()) return a.getDurationMinutes() < 120;
                    if (durMed.isSelected())   return a.getDurationMinutes() >= 120
                            && a.getDurationMinutes() <= 240;
                    if (durLong.isSelected())  return a.getDurationMinutes() > 240;
                    return true; // durAll
                })
                .collect(Collectors.toList());

        // Sort
        if (sortOption != null) {
            switch (sortOption) {
                case "Prix croissant"    -> filtered.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                case "Prix décroissant"  -> filtered.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                case "Durée croissante"  -> filtered.sort((a, b) -> Integer.compare(a.getDurationMinutes(), b.getDurationMinutes()));
                case "Alphabétique"      -> filtered.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
            }
        }

        renderCards(filtered);
    }

    private void renderCards(List<Activity> activities) {
        activitiesGrid.getChildren().clear();

        // Update results count
        resultsCountLabel.setText(activities.size() + " activité"
                + (activities.size() > 1 ? "s" : "") + " trouvée"
                + (activities.size() > 1 ? "s" : ""));

        // Show/hide empty state
        boolean empty = activities.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        activitiesGrid.setVisible(!empty);
        activitiesGrid.setManaged(!empty);

        for (Activity activity : activities) {
            activitiesGrid.getChildren().add(buildCard(activity));
        }
    }

    private VBox buildCard(Activity activity) {
        VBox card = new VBox(0);
        card.getStyleClass().add("act-card");

        // ── Image placeholder ──
        Rectangle img = new Rectangle(280, 180);
        img.getStyleClass().add("act-card-img-placeholder");
        img.setArcWidth(14);
        img.setArcHeight(14);
        card.getChildren().add(img);

        // ── Card body ──
        VBox body = new VBox(8);
        body.getStyleClass().add("act-card-body");

        // Category badge
        Label catBadge = new Label(
                activity.getCategory().getIcon() + "  " + activity.getCategory().getName()
        );
        catBadge.getStyleClass().add("act-card-category");

        // Title
        Label title = new Label(activity.getTitle());
        title.getStyleClass().add("act-card-title");
        title.setWrapText(true);
        title.setMaxWidth(248);

        // Location
        Label location = new Label("📍  " + activity.getLocation());
        location.getStyleClass().add("act-card-location");

        // Meta row: duration + participants
        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label duration = new Label("⏱  " + formatDuration(activity.getDurationMinutes()));
        duration.getStyleClass().add("act-card-meta");
        Label participants = new Label("👥  " + activity.getMaxParticipants() + " pers. max");
        participants.getStyleClass().add("act-card-meta");
        meta.getChildren().addAll(duration, participants);

        // Guide (if assigned)
        if (activity.getGuide() != null) {
            Label guide = new Label("🧭  " + activity.getGuide().getFirstName()
                    + " " + activity.getGuide().getLastName());
            guide.getStyleClass().add("act-card-meta");
            body.getChildren().addAll(catBadge, title, location, meta, guide);
        } else {
            body.getChildren().addAll(catBadge, title, location, meta);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Price + button row
        HBox priceRow = new HBox(0);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(2);
        Label price = new Label(String.format("%.0f TND", activity.getPrice()));
        price.getStyleClass().add("act-card-price");
        Label priceSub = new Label("/ personne");
        priceSub.getStyleClass().add("act-card-price-sub");
        priceBox.getChildren().addAll(price, priceSub);

        Region priceSpace = new Region();
        HBox.setHgrow(priceSpace, Priority.ALWAYS);

        Button btn = new Button("Voir →");
        btn.getStyleClass().add("act-card-btn");
        btn.setPrefWidth(90);
        btn.setOnAction(e -> handleViewActivity(activity));

        priceRow.getChildren().addAll(priceBox, priceSpace, btn);

        body.getChildren().addAll(spacer, priceRow);
        card.getChildren().add(body);

        // Click anywhere on card
        card.setOnMouseClicked(e -> handleViewActivity(activity));

        return card;
    }

    // ── Helpers ──────────────────────────────────────────────

    private String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + "h" : h + "h" + m + "min";
    }

    private void setActiveTab(Button selected) {
        // Remove active style from all tabs
        tabAll.getStyleClass().remove("act-tab-active");
        for (var node : categoryTabsBox.getChildren()) {
            if (node instanceof Button b) b.getStyleClass().remove("act-tab-active");
        }
        // Add to selected
        if (!selected.getStyleClass().contains("act-tab-active"))
            selected.getStyleClass().add("act-tab-active");
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

    // ── FXML handlers ────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String q = searchField.getText().trim();
        if (!q.isEmpty()) showFilterBadge("🔍 \"" + q + "\"");
        else hideFilterBadge();
        applyFilters();
    }

    @FXML
    private void handleTabAll() {
        activeCategoryId = null;
        setActiveTab(tabAll);
        hideFilterBadge();
        applyFilters();
    }

    private void handleCategoryTab(Button tab, int categoryId, String categoryName) {
        activeCategoryId = categoryId;
        setActiveTab(tab);
        showFilterBadge(categoryName);
        applyFilters();
    }

    @FXML
    private void handleClearFilter() {
        activeCategoryId = null;
        searchField.clear();
        sortCombo.setValue(null);
        priceSlider.setValue(500);
        durAll.setSelected(true);
        participantsSpinner.getValueFactory().setValue(1);
        setActiveTab(tabAll);
        hideFilterBadge();
        applyFilters();
    }

    @FXML
    private void handleApplyFilters() {
        applyFilters();
    }

    @FXML
    private void handleResetFilters() {
        handleClearFilter();
    }

    @FXML
    private void handleContact() {
        SceneManager.navigateTo(Routes.CONTACT);
    }

    private void handleViewActivity(Activity activity) {
        // Step 3 — detail page (coming next)
        System.out.println("View activity: " + activity.getTitle());
    }
}