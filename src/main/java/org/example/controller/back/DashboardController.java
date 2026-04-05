package org.example.controller.back;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.session.SessionManager;

public class DashboardController {

    // ── Top stat cards ───────────────────────────────────────
    @FXML private Label statActivites;
    @FXML private Label statHebergements;
    @FXML private Label statTransports;
    @FXML private Label statProduits;
    @FXML private Label statUtilisateurs;
    @FXML private Label statReservations;

    // ── Activités module ─────────────────────────────────────
    @FXML private Label mstatTotalActivites;
    @FXML private Label mstatActiveActivites;
    @FXML private Label mstatCategoriesActivites;
    @FXML private Label mstatGuides;
    @FXML private VBox  recentActivitesList;

    // ── Hébergement module ───────────────────────────────────
    @FXML private Label    mstatHebergements;
    @FXML private Label    mstatChambres;
    @FXML private Label    mstatEquipements;
    @FXML private Label    mstatCategoriesHeb;
    @FXML private BarChart<String, Number> chartChambres;
    @FXML private PieChart chartHebPie;

    // ── Transport module ─────────────────────────────────────
    @FXML private Label mstatTransports;
    @FXML private Label mstatCategoriesTransport;
    @FXML private Label mstatChauffeurs;
    @FXML private Label mstatTrajets;

    // ── Boutique module ──────────────────────────────────────
    @FXML private Label mstatProduits;
    @FXML private Label mstatCategoriesBoutique;
    @FXML private Label mstatCommandes;
    @FXML private Label mstatPaiements;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        loadStats();
        loadCharts();
        loadRecentActivites();
    }

    // ── Data loading ─────────────────────────────────────────

    private void loadStats() {
        // Replace these with real service/repository calls
        // e.g. int total = activiteService.count();
        // For now wired to 0 — connect your services here

        statActivites.setText("0");
        statHebergements.setText("0");
        statTransports.setText("0");
        statProduits.setText("0");
        statUtilisateurs.setText("0");
        statReservations.setText("0");

        mstatTotalActivites.setText("0");
        mstatActiveActivites.setText("0");
        mstatCategoriesActivites.setText("0");
        mstatGuides.setText("0");

        mstatHebergements.setText("0");
        mstatChambres.setText("0");
        mstatEquipements.setText("0");
        mstatCategoriesHeb.setText("0");

        mstatTransports.setText("0");
        mstatCategoriesTransport.setText("0");
        mstatChauffeurs.setText("0");
        mstatTrajets.setText("0");

        mstatProduits.setText("0");
        mstatCategoriesBoutique.setText("0");
        mstatCommandes.setText("0");
        mstatPaiements.setText("0");
    }

    private void loadCharts() {
        // Bar chart: chambres par hébergement
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Chambres");
        // Replace with real data:
        // for (HebergementStat s : hebergementService.getChambresPerHeberg()) {
        //     series.getData().add(new XYChart.Data<>(s.getName(), s.getCount()));
        // }
        series.getData().add(new XYChart.Data<>("Exemple 1", 5));
        series.getData().add(new XYChart.Data<>("Exemple 2", 3));
        series.getData().add(new XYChart.Data<>("Exemple 3", 8));
        chartChambres.getData().add(series);
        chartChambres.setLegendVisible(false);

        // Pie chart: hébergements par catégorie
        // Replace with real data from service
        chartHebPie.getData().addAll(
                new PieChart.Data("Éco-lodge", 4),
                new PieChart.Data("Hôtel", 6),
                new PieChart.Data("Camping", 2)
        );
    }

    private void loadRecentActivites() {
        // Replace with real data:
        // List<Activite> recents = activiteService.findRecent(3);
        // for (Activite a : recents) {
        //     recentActivitesList.getChildren().add(buildRecentItem(a));
        // }

        // Placeholder — shows structure
        recentActivitesList.getChildren().add(
                buildRecentItem("Randonnée Zaghouan", "Zaghouan • 45 TND", true)
        );
        recentActivitesList.getChildren().add(
                buildRecentItem("Plongée Tabarka", "Tabarka • 80 TND", true)
        );
    }

    // Builds one recent item row (mirrors .recent-item in the web)
    private HBox buildRecentItem(String title, String subtitle, boolean active) {
        HBox row = new HBox(12);
        row.getStyleClass().add("recent-item");

        VBox info = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("recent-title");
        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("recent-sub");
        info.getChildren().addAll(titleLabel, subLabel);

        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().add(info);

        if (active) {
            Label badge = new Label("Actif");
            badge.getStyleClass().add("badge-success");
            row.getChildren().add(badge);
        }

        return row;
    }

    // ── Navigation handlers ──────────────────────────────────

    @FXML private void handleViewActivites()    { SceneManager.navigateTo(Routes.ADMIN_ACTIVITES); }
    @FXML private void handleNewActivite()      { SceneManager.navigateTo(Routes.ADMIN_ACTIVITES); }
    @FXML private void handleNewCatActivite()   { SceneManager.navigateTo(Routes.ADMIN_ACTIVITES); }
    @FXML private void handleNewHoraire()       { SceneManager.navigateTo(Routes.ADMIN_ACTIVITES); }
    @FXML private void handleNewGuide()         { SceneManager.navigateTo(Routes.ADMIN_ACTIVITES); }

    @FXML private void handleNewHeberg()        { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleNewChambre()       { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleNewEquipement()    { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleNewCatHeberg()     { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }

    @FXML private void handleNewTransport()     { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void handleNewCatTransport()  { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void handleNewChauffeur()     { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void handleNewTrajet()        { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void handleViewTransports()   { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }

    @FXML private void handleNewProduit()       { SceneManager.navigateTo(Routes.ADMIN_BOUTIQUE); }
    @FXML private void handleNewCatBoutique()   { SceneManager.navigateTo(Routes.ADMIN_BOUTIQUE); }
    @FXML private void handleViewCommandes()    { SceneManager.navigateTo(Routes.ADMIN_BOUTIQUE); }
    @FXML private void handleViewPaiements()    { SceneManager.navigateTo(Routes.ADMIN_BOUTIQUE); }
}