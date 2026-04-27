package tn.esprit.controller.back;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.models.hebergements.Categorie_hebergement;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.*;
import tn.esprit.session.SessionManager;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    // ── Top stat cards ───────────────────────────────────────
    @FXML private Label statActivites;
    @FXML private Label statHebergements;
    @FXML private Label statTransports;
    @FXML private Label statProduits;
    @FXML private Label statUtilisateurs;
    @FXML private Label statReservations;

    // ── Activites module ─────────────────────────────────────
    @FXML private Label mstatTotalActivites;
    @FXML private Label mstatActiveActivites;
    @FXML private Label mstatCategoriesActivites;
    @FXML private Label mstatGuides;
    @FXML private VBox  recentActivitesList;

    // ── Hebergement module ───────────────────────────────────
    @FXML private Label mstatHebergements;
    @FXML private Label mstatEquipements;
    @FXML private Label mstatCategoriesHeb;

    // Charts
    @FXML private PieChart chartHebPie;
    @FXML private BarChart<String, Number> chartTopRevenueHeb;
    @FXML private CategoryAxis             xAxisRevenue;
    @FXML private NumberAxis               yAxisRevenue;

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

    // ── Services ─────────────────────────────────────────────
    private final Hebergement_service hebergementService = new Hebergement_service();
    private final Chambre_service     chambreService     = new Chambre_service(); // kept for revenue only
    private final Equipement_service  equipementService  = new Equipement_service();
    private final CategorieH_service  categorieHService  = new CategorieH_service();

    // ── Couleurs barres ──────────────────────────────────────
    private static String getBarColorRgb(int index) {
        int r, g, b;
        switch (index) {
            case 0:  r = 99;  g = 102; b = 241; break;
            case 1:  r = 139; g = 92;  b = 246; break;
            case 2:  r = 167; g = 139; b = 250; break;
            case 3:  r = 196; g = 181; b = 253; break;
            default: r = 221; g = 214; b = 254; break;
        }
        return "rgb(" + r + "," + g + "," + b + ")";
    }

    // ── DTO revenu (utilisé uniquement pour le BarChart) ─────
    private static class RevenueEntry {
        final String nom;
        final double revenu;
        RevenueEntry(String nom, double revenu) { this.nom = nom; this.revenu = revenu; }
    }

    // ── Initialisation ───────────────────────────────────────
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

    // ── Stats ────────────────────────────────────────────────
    private void loadStats() {
        try {
            List<Hebergement> hebs = hebergementService.getAll();
            int totalHeb = hebs.size();
            int totalEq  = equipementService.getAll().size();
            int totalCat = categorieHService.getAll().size();

            statHebergements.setText(String.valueOf(totalHeb));
            mstatHebergements.setText(String.valueOf(totalHeb));
            mstatEquipements.setText(String.valueOf(totalEq));
            mstatCategoriesHeb.setText(String.valueOf(totalCat));

        } catch (SQLException e) {
            String dash = "—";
            statHebergements.setText(dash);
            mstatHebergements.setText(dash);
            mstatEquipements.setText(dash);
            mstatCategoriesHeb.setText(dash);
        }

        // Placeholders autres modules
        statActivites.setText("0");
        statTransports.setText("0");
        statProduits.setText("0");
        statUtilisateurs.setText("0");
        statReservations.setText("0");
        mstatTotalActivites.setText("0");
        mstatActiveActivites.setText("0");
        mstatCategoriesActivites.setText("0");
        mstatGuides.setText("0");
        mstatTransports.setText("0");
        mstatCategoriesTransport.setText("0");
        mstatChauffeurs.setText("0");
        mstatTrajets.setText("0");
        mstatProduits.setText("0");
        mstatCategoriesBoutique.setText("0");
        mstatCommandes.setText("0");
        mstatPaiements.setText("0");
    }

    // ── Graphiques ───────────────────────────────────────────
    private void loadCharts() {
        loadPieChart();
        loadTopRevenueChart();
    }

    private void loadPieChart() {
        try {
            chartHebPie.getData().clear();
            List<Hebergement> hebs = hebergementService.getAll();
            Map<Integer, Long> countParCat = hebs.stream()
                    .collect(Collectors.groupingBy(Hebergement::getCategorie_id, Collectors.counting()));

            for (Map.Entry<Integer, Long> entry : countParCat.entrySet()) {
                String nom = "Categorie " + entry.getKey();
                try {
                    Categorie_hebergement cat = categorieHService.getById(entry.getKey());
                    if (cat != null) nom = cat.getNom();
                } catch (SQLException ex) { /* keep default */ }
                chartHebPie.getData().add(
                        new PieChart.Data(nom + " (" + entry.getValue() + ")", entry.getValue()));
            }
            chartHebPie.setLegendVisible(true);
            chartHebPie.setLabelsVisible(false);
            chartHebPie.setTitle("Repartition des hebergements");
        } catch (SQLException e) {
            chartHebPie.getData().clear();
        }
    }

    private void loadTopRevenueChart() {
        try {
            List<Hebergement> hebs = hebergementService.getAll();
            List<RevenueEntry> top5 = new ArrayList<>();
            for (Hebergement h : hebs) {
                double rev = 0;
                try { rev = chambreService.getRevenueByHebergement(h.getId()); }
                catch (SQLException ignored) {}
                top5.add(new RevenueEntry(h.getNom(), rev));
            }
            top5.sort((a, b) -> Double.compare(b.revenu, a.revenu));
            if (top5.size() > 5) top5 = top5.subList(0, 5);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Revenu (TND)");

            // Utiliser le nom complet comme clé d'axe X
            for (RevenueEntry e : top5) {
                series.getData().add(new XYChart.Data<>(e.nom, e.revenu));
            }

            chartTopRevenueHeb.getData().clear();
            chartTopRevenueHeb.getData().add(series);
            chartTopRevenueHeb.setLegendVisible(false);
            chartTopRevenueHeb.setAnimated(true);

            final List<RevenueEntry> finalTop5 = top5;
            Platform.runLater(() -> {
                int i = 0;
                for (XYChart.Series<String, Number> s : chartTopRevenueHeb.getData()) {
                    for (XYChart.Data<String, Number> d : s.getData()) {
                        if (d.getNode() != null) {
                            final int idx = i;
                            d.getNode().setStyle(
                                    "-fx-bar-fill: " + getBarColorRgb(idx) + ";"
                                            + " -fx-background-radius: 4 4 0 0;");

                            // Tooltip : nom + revenu
                            String nomComplet = idx < finalTop5.size() ? finalTop5.get(idx).nom : "";
                            double rev = d.getYValue().doubleValue();
                            Tooltip tip = new Tooltip(nomComplet + "\n" + String.format("%.0f TND", rev));
                            tip.setStyle("-fx-font-size: 12px;");
                            Tooltip.install(d.getNode(), tip);

                            javafx.scene.layout.StackPane bar =
                                    (javafx.scene.layout.StackPane) d.getNode();
                            bar.getChildren().removeIf(n -> n instanceof Label);

                            // Label "XXX TND" au-dessus de la barre
                            Label tndLabel = new Label(String.format("%.0f TND", rev));
                            tndLabel.setStyle(
                                    "-fx-font-size: 11px; -fx-font-weight: bold;"
                                            + " -fx-text-fill: rgb(99,102,241);");
                            javafx.scene.layout.StackPane.setAlignment(
                                    tndLabel, javafx.geometry.Pos.TOP_CENTER);
                            tndLabel.setTranslateY(-20);

                            // Label nom de l'hébergement à l'intérieur de la barre
                            Label nomLabel = new Label(nomComplet);
                            nomLabel.setStyle(
                                    "-fx-font-size: 11px; -fx-font-weight: bold;"
                                            + " -fx-text-fill: white;"
                                            + " -fx-wrap-text: true;"
                                            + " -fx-alignment: CENTER;");
                            nomLabel.setMaxWidth(Double.MAX_VALUE);
                            nomLabel.setWrapText(true);
                            javafx.scene.layout.StackPane.setAlignment(
                                    nomLabel, javafx.geometry.Pos.CENTER);

                            bar.getChildren().addAll(tndLabel, nomLabel);

                            i++;
                        }
                    }
                }
            });

        } catch (SQLException e) {
            chartTopRevenueHeb.getData().clear();
        }
    }

    // ── Activites recentes ───────────────────────────────────
    private void loadRecentActivites() {
        recentActivitesList.getChildren().clear();
        recentActivitesList.getChildren().add(
                buildRecentItem("Randonnee Zaghouan", "Zaghouan - 45 TND", true));
        recentActivitesList.getChildren().add(
                buildRecentItem("Plongee Tabarka", "Tabarka - 80 TND", true));
    }

    private HBox buildRecentItem(String title, String subtitle, boolean active) {
        HBox row = new HBox(12);
        row.getStyleClass().add("recent-item");
        VBox info = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("recent-title");
        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("recent-sub");
        info.getChildren().addAll(titleLabel, subLabel);
        HBox.setHgrow(info, Priority.ALWAYS);
        row.getChildren().add(info);
        if (active) {
            Label badge = new Label("Actif");
            badge.getStyleClass().add("badge-success");
            row.getChildren().add(badge);
        }
        return row;
    }

    // ── Navigation ───────────────────────────────────────────
    @FXML private void handleNewActivite()      { SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }
    @FXML private void handleNewCatActivite()   { SceneManager.navigateTo(Routes.ADMIN_ACTIVITY_CATEGORIES); }
    @FXML private void handleNewHoraire()       { SceneManager.navigateTo(Routes.ADMIN_SCHEDULES); }
    @FXML private void handleNewGuide()         { SceneManager.navigateTo(Routes.ADMIN_GUIDES); }
    @FXML private void handleNewHeberg()        { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleNewChambre()       { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleNewEquipement()    { SceneManager.navigateTo(Routes.ADMIN_EQUIPEMENTS); }
    @FXML private void handleNewCatHeberg()     { SceneManager.navigateTo(Routes.ADMIN_CATEGORIES_HEBERGEMENT); }
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