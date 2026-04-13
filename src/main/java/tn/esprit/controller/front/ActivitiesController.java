package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivityCategory;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityCategoryService;
import tn.esprit.services.activity.ActivityService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ActivitiesController implements Initializable {

    /* ── FXML ─────────────────────────────────────────────── */
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button           tabAll;
    @FXML private HBox             categoryTabsBox;
    @FXML private Label            resultsCountLabel;
    @FXML private Label            activeFilterLabel;
    @FXML private Button           clearFilterBtn;
    @FXML private Slider           priceSlider;
    @FXML private Label            priceLabel;
    @FXML private RadioButton      durAll, durShort, durMed, durLong;
    @FXML private Spinner<Integer> participantsSpinner;
    @FXML private FlowPane         activitiesGrid;
    @FXML private VBox             emptyState;
    @FXML private HBox             paginationBar;
    @FXML private Label            pagInfo;
    @FXML private Button           btnPrev, btnNext;

    /* ── Services ─────────────────────────────────────────── */
    private final ActivityService         activityService = new ActivityService();
    private final ActivityCategoryService categoryService = new ActivityCategoryService();

    /* ── State ────────────────────────────────────────────── */
    private List<Activity> allData      = new ArrayList<>();
    private List<Activity> filteredData = new ArrayList<>();
    private Integer        activeCategoryId = null;
    private ToggleGroup    durationGroup;
    private static final int PER_PAGE = 8;
    private int currentPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Duration toggle group
        durationGroup = new ToggleGroup();
        durAll.setToggleGroup(durationGroup);
        durShort.setToggleGroup(durationGroup);
        durMed.setToggleGroup(durationGroup);
        durLong.setToggleGroup(durationGroup);

        // Price slider live label
        priceSlider.valueProperty().addListener((obs, o, n) ->
                priceLabel.setText((int) n.doubleValue() + " TND"));

        // Listeners
        sortCombo.setOnAction(e -> applyFilters());
        searchField.setOnKeyReleased(e -> handleSearch());

        loadCategories();
        loadActivities();
    }

    /* ── Data ─────────────────────────────────────────────── */

    private void loadCategories() {
        try {
            List<ActivityCategory> cats = categoryService.afficherAll();
            for (ActivityCategory cat : cats) {
                String icon = (cat.getIcon() != null && !cat.getIcon().isBlank())
                        ? cat.getIcon() : "🏷️";
                Button tab = new Button(icon + "  " + cat.getName());
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
            allData = activityService.afficherAll();
            filteredData = new ArrayList<>(allData);
            renderPage();
        } catch (Exception e) {
            System.err.println("Error loading activities: " + e.getMessage());
            resultsCountLabel.setText("Erreur de chargement");
        }
    }

    /* ── Filtering ────────────────────────────────────────── */

    private void applyFilters() {
        String search   = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        double maxPrice = priceSlider.getValue();
        int    minPart  = participantsSpinner.getValue();
        String sort     = sortCombo.getValue();

        filteredData = allData.stream()
                .filter(Activity::isActive)
                .filter(a -> activeCategoryId == null
                        || a.getCategory().getId() == activeCategoryId)
                .filter(a -> search.isEmpty()
                        || a.getTitle().toLowerCase().contains(search)
                        || a.getLocation().toLowerCase().contains(search)
                        || a.getDescription().toLowerCase().contains(search))
                .filter(a -> a.getPrice() <= maxPrice)
                .filter(a -> a.getMaxParticipants() >= minPart)
                .filter(a -> {
                    if (durShort.isSelected()) return a.getDurationMinutes() < 120;
                    if (durMed.isSelected())   return a.getDurationMinutes() >= 120 && a.getDurationMinutes() <= 240;
                    if (durLong.isSelected())  return a.getDurationMinutes() > 240;
                    return true;
                })
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Prix croissant"   -> filteredData.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
            case "Prix décroissant" -> filteredData.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
            case "Durée croissante" -> filteredData.sort((a, b) -> Integer.compare(a.getDurationMinutes(), b.getDurationMinutes()));
            case "Alphabétique"     -> filteredData.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        }

        currentPage = 1;
        renderPage();
    }

    /* ── Pagination ───────────────────────────────────────── */

    private void renderPage() {
        activitiesGrid.getChildren().clear();

        boolean isEmpty = filteredData == null || filteredData.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        activitiesGrid.setVisible(!isEmpty);
        activitiesGrid.setManaged(!isEmpty);

        int total = isEmpty ? 0 : filteredData.size();
        resultsCountLabel.setText(total + " Activité" + (total > 1 ? "s" : "") + " Trouvée" + (total > 1 ? "s" : ""));

        if (isEmpty) {
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            return;
        }

        int totalPages = (int) Math.ceil((double) total / PER_PAGE);
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);

        filteredData.subList(from, to).forEach(a -> activitiesGrid.getChildren().add(buildCard(a)));

        pagInfo.setText("Page " + currentPage + " / " + totalPages);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
    }

    @FXML private void onPrev() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML private void onNext() {
        int totalPages = (int) Math.ceil((double) filteredData.size() / PER_PAGE);
        if (currentPage < totalPages) { currentPage++; renderPage(); }
    }

    /* ── Card builder ─────────────────────────────────────── */

    private VBox buildCard(Activity a) {
        VBox card = new VBox(0);
        card.getStyleClass().add("heb-card");

        // Image zone
        card.getChildren().add(buildImageZone(a));

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(20, 22, 22, 22));

        // Category badge
        if (a.getCategory() != null) {
            String icon = (a.getCategory().getIcon() != null && !a.getCategory().getIcon().isBlank())
                    ? a.getCategory().getIcon() : "🏷️";
            Label catLabel = new Label(icon + "  " + a.getCategory().getName());
            catLabel.getStyleClass().add("heb-card-category");
            body.getChildren().add(catLabel);
        }

        // Title
        Label title = new Label(a.getTitle());
        title.getStyleClass().add("heb-card-nom");
        title.setWrapText(true);
        body.getChildren().add(title);

        // Location
        Label location = new Label("📍  " + a.getLocation());
        location.getStyleClass().add("heb-card-ville");
        body.getChildren().add(location);

        // Description
        if (a.getDescription() != null && !a.getDescription().isBlank()) {
            String text = a.getDescription().length() > 90
                    ? a.getDescription().substring(0, 90) + "…"
                    : a.getDescription();
            Label desc = new Label(text);
            desc.getStyleClass().add("heb-card-desc");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        // Meta chips
        FlowPane meta = new FlowPane(8, 6);
        Label duration = new Label("⏱  " + formatDuration(a.getDurationMinutes()));
        duration.getStyleClass().add("heb-card-equipement");
        Label participants = new Label("👥  " + a.getMaxParticipants() + " pers. max");
        participants.getStyleClass().add("heb-card-equipement");
        meta.getChildren().addAll(duration, participants);

        if (a.getGuide() != null) {
            Label guide = new Label("🧭  " + a.getGuide().getFirstName() + " " + a.getGuide().getLastName());
            guide.getStyleClass().add("heb-card-equipement");
            meta.getChildren().add(guide);
        }
        body.getChildren().add(meta);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Price + button row
        HBox priceRow = new HBox(0);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        priceRow.setPadding(new Insets(10, 0, 0, 0));

        VBox priceBox = new VBox(2);
        Label price    = new Label(String.format("%.0f", a.getPrice()));
        Label currency = new Label("TND");
        Label unit     = new Label("/ personne");
        price.getStyleClass().add("heb-card-price-amount");
        currency.getStyleClass().add("heb-card-price-currency");
        unit.getStyleClass().add("heb-card-price-unit");
        priceBox.getChildren().addAll(price, currency, unit);

        Region priceSpace = new Region();
        HBox.setHgrow(priceSpace, Priority.ALWAYS);

        Button btn = new Button("Voir →");
        btn.getStyleClass().add("heb-card-btn");
        btn.setPrefWidth(90);
        btn.setOnAction(e -> handleViewActivity(a));

        priceRow.getChildren().addAll(priceBox, priceSpace, btn);
        body.getChildren().addAll(spacer, priceRow);
        card.getChildren().add(body);
        card.setOnMouseClicked(e -> handleViewActivity(a));

        return card;
    }

    /* ── Image zone ───────────────────────────────────────── */

    private StackPane buildImageZone(Activity a) {
        VBox imgBox = new VBox();
        imgBox.getStyleClass().add("heb-card-img");
        imgBox.setPrefHeight(200);
        imgBox.setMinHeight(200);
        imgBox.setMaxHeight(200);
        imgBox.setAlignment(Pos.CENTER);

        Image image = loadImage(a.getImage());
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(320);
            iv.setFitHeight(200);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            imgBox.getChildren().add(iv);
        } else {
            String fallbackIcon = (a.getCategory() != null
                    && a.getCategory().getIcon() != null
                    && !a.getCategory().getIcon().isBlank())
                    ? a.getCategory().getIcon() : "🏃";
            Label fallback = new Label(fallbackIcon);
            fallback.setStyle("-fx-font-size: 52px;");
            imgBox.getChildren().add(fallback);
        }

        StackPane stack = new StackPane(imgBox);
        if (a.isActive()) {
            Label badge = new Label("✅ Disponible");
            badge.getStyleClass().add("heb-card-eco-badge");
            stack.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(14, 14, 0, 0));
        }
        return stack;
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Image img = new Image(imagePath, 320, 200, false, true, true);
                return img.isError() ? null : img;
            }
            File absFile = new File(imagePath);
            if (absFile.exists()) return new Image(absFile.toURI().toString(), 320, 200, false, true);
        } catch (Exception e) {
            System.err.println("Image non chargée : " + imagePath);
        }
        return null;
    }

    /* ── Helpers ──────────────────────────────────────────── */

    private String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + "h" : h + "h" + m + "min";
    }

    private void setActiveTab(Button selected) {
        tabAll.getStyleClass().remove("act-tab-active");
        for (var node : categoryTabsBox.getChildren()) {
            if (node instanceof Button b) b.getStyleClass().remove("act-tab-active");
        }
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

    /* ── FXML Handlers ────────────────────────────────────── */

    @FXML private void handleSearch() {
        String q = searchField.getText().trim();
        if (!q.isEmpty()) showFilterBadge("🔍 \"" + q + "\"");
        else hideFilterBadge();
        applyFilters();
    }

    @FXML private void handleTabAll() {
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

    @FXML private void handleClearFilter() {
        activeCategoryId = null;
        searchField.clear();
        sortCombo.getSelectionModel().selectFirst();
        priceSlider.setValue(500);
        durAll.setSelected(true);
        participantsSpinner.getValueFactory().setValue(1);
        setActiveTab(tabAll);
        hideFilterBadge();
        applyFilters();
    }

    @FXML private void handleApplyFilters()  { applyFilters(); }
    @FXML private void handleResetFilters()  { handleClearFilter(); }

    @FXML private void handleContact() {
        SceneManager.navigateTo(Routes.CONTACT);
    }

    private void handleViewActivity(Activity activity) {
        System.out.println("View activity: " + activity.getTitle());
    }
}
