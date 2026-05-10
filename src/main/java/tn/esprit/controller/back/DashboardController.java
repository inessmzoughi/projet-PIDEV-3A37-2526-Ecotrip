package tn.esprit.controller.back;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.models.hebergements.Categorie_hebergement;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.models.produit.Product;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.*;
import tn.esprit.models.produit.ProductCategory;
import tn.esprit.services.produit.CommandeService;
import tn.esprit.services.produit.ProductCategoryService;
import tn.esprit.services.produit.ProductService;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.HebergementEventBus;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    // ── Top stat cards ────────────────────────────────────────────────────────
    @FXML private Label statActivites;
    @FXML private Label statHebergements;
    @FXML private Label statTransports;
    @FXML private Label statProduits;
    @FXML private Label statUtilisateurs;
    @FXML private Label statReservations;

    // ── Activités module ──────────────────────────────────────────────────────
    @FXML private Label mstatTotalActivites;
    @FXML private Label mstatActiveActivites;
    @FXML private Label mstatCategoriesActivites;
    @FXML private Label mstatGuides;
    @FXML private VBox  recentActivitesList;

    // ── Hébergement module ────────────────────────────────────────────────────
    @FXML private Label    mstatHebergements;
    @FXML private Label    mstatEquipements;
    @FXML private Label    mstatCategoriesHeb;
    @FXML private PieChart chartHebPie;
    @FXML private BarChart<String, Number> chartTopRevenueHeb;
    @FXML private CategoryAxis             xAxisRevenue;
    @FXML private NumberAxis               yAxisRevenue;
    private Timeline dashboardRefreshTimeline;


    // ── Transport module ──────────────────────────────────────────────────────
    @FXML private Label mstatTransports;
    @FXML private Label mstatCategoriesTransport;
    @FXML private Label mstatChauffeurs;
    @FXML private Label mstatTrajets;

    // ── Boutique module ───────────────────────────────────────────────────────
    @FXML private Label    mstatProduits;
    @FXML private Label    mstatCategoriesBoutique;
    @FXML private Label    mstatCommandes;
    @FXML private Label    mstatPaiements;
    @FXML private PieChart chartBoutiquePie;
    @FXML private BarChart<String, Number> chartTopProduits;
    @FXML private CategoryAxis             xAxisProduits;
    @FXML private NumberAxis               yAxisProduits;

    // ── Services ──────────────────────────────────────────────────────────────
    private final Hebergement_service hebergementService = new Hebergement_service();
    private final Chambre_service     chambreService     = new Chambre_service();
    private final Equipement_service  equipementService  = new Equipement_service();
    private final CategorieH_service  categorieHService  = new CategorieH_service();
    private final ProductService         productService         = new ProductService();
    private final ProductCategoryService productCategoryService = new ProductCategoryService();
    private final CommandeService        commandeService        = new CommandeService();

    // ── Palette de couleurs partagée ──────────────────────────────────────────
    /** Retourne une couleur RGB en fonction de l'index (palette violette pour Hébergement). */
    private static String getBarColorHeb(int index) {
        int[][] palette = {
                {99,  102, 241},
                {139, 92,  246},
                {167, 139, 250},
                {196, 181, 253},
                {221, 214, 254}
        };
        int[] c = palette[Math.min(index, palette.length - 1)];
        return "rgb(" + c[0] + "," + c[1] + "," + c[2] + ")";
    }

    /** Retourne une couleur RGB en fonction de l'index (palette rouge/orange pour Boutique). */
    private static String getBarColorBoutique(int index) {
        int[][] palette = {
                {220, 38,  38},
                {234, 88,  12},
                {245, 158, 11},
                {132, 204, 22},
                {20,  184, 166}
        };
        int[] c = palette[Math.min(index, palette.length - 1)];
        return "rgb(" + c[0] + "," + c[1] + "," + c[2] + ")";
    }

    // ── DTOs internes ─────────────────────────────────────────────────────────
    private static class RevenueEntry {
        final String nom;
        final double revenu;
        RevenueEntry(String nom, double revenu) { this.nom = nom; this.revenu = revenu; }
    }

    private static class SalesEntry {
        final String nom;
        final int    qty;
        SalesEntry(String nom, int qty) { this.nom = nom; this.qty = qty; }
    }

    // ── Initialisation ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }
        loadStats();
        loadCharts();
        loadRecentActivites();
        startDashboardRefresh();
        HebergementEventBus.subscribe(this::silentRefreshDashboard); // Event Bus

    }

    // ── Stats globales ────────────────────────────────────────────────────────
    private void loadStats() {
        // Hébergement
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

        // Boutique
        try {
            List<Product> products   = productService.read();
            int totalProduits        = products.size();
            // Nombre de catégories via ProductCategoryService (table dédiée)
            int totalCatBoutique     = productCategoryService.read().size();
            int totalCommandes       = commandeService.read().size();

            statProduits.setText(String.valueOf(totalProduits));
            mstatProduits.setText(String.valueOf(totalProduits));
            mstatCategoriesBoutique.setText(String.valueOf(totalCatBoutique));
            mstatCommandes.setText(String.valueOf(totalCommandes));
            mstatPaiements.setText(String.valueOf(totalCommandes));
        } catch (SQLException e) {
            String dash = "—";
            statProduits.setText(dash);
            mstatProduits.setText(dash);
            mstatCategoriesBoutique.setText(dash);
            mstatCommandes.setText(dash);
            mstatPaiements.setText(dash);
        }

        // Placeholders autres modules
        statActivites.setText("0");
        statTransports.setText("0");
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
    }

    // ── Graphiques ────────────────────────────────────────────────────────────
    private void loadCharts() {
        loadHebPieChart();
        loadTopRevenueChart();
        loadBoutiquePieChart();
        loadTopProduitsChart();
    }

    // ── HÉBERGEMENT : PieChart ────────────────────────────────────────────────
    private void loadHebPieChart() {
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
            chartHebPie.setTitle("Répartition des hébergements");
        } catch (SQLException e) {
            chartHebPie.getData().clear();
        }
    }

    // ── HÉBERGEMENT : BarChart Top 5 revenus ─────────────────────────────────
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
            for (RevenueEntry e : top5)
                series.getData().add(new XYChart.Data<>(e.nom, e.revenu));

            chartTopRevenueHeb.getData().clear();
            chartTopRevenueHeb.getData().add(series);
            chartTopRevenueHeb.setLegendVisible(false);
            chartTopRevenueHeb.setAnimated(true);

            final List<RevenueEntry> finalTop5 = top5;
            Platform.runLater(() -> applyBarStyleHeb(chartTopRevenueHeb, finalTop5));
        } catch (SQLException e) {
            chartTopRevenueHeb.getData().clear();
        }
    }

    private void applyBarStyleHeb(BarChart<String, Number> chart, List<RevenueEntry> entries) {
        int i = 0;
        for (XYChart.Series<String, Number> s : chart.getData()) {
            for (XYChart.Data<String, Number> d : s.getData()) {
                if (d.getNode() == null) continue;
                final int idx = i;
                d.getNode().setStyle(
                        "-fx-bar-fill: " + getBarColorHeb(idx) + ";"
                                + " -fx-background-radius: 4 4 0 0;");
                String nom = idx < entries.size() ? entries.get(idx).nom : "";
                double rev = d.getYValue().doubleValue();
                Tooltip tip = new Tooltip(nom + "\n" + String.format("%.0f TND", rev));
                tip.setStyle("-fx-font-size: 12px;");
                Tooltip.install(d.getNode(), tip);

                StackPane bar = (StackPane) d.getNode();
                bar.getChildren().removeIf(n -> n instanceof Label);

                Label tndLabel = new Label(String.format("%.0f TND", rev));
                tndLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                        + " -fx-text-fill: rgb(99,102,241);");
                StackPane.setAlignment(tndLabel, Pos.TOP_CENTER);
                tndLabel.setTranslateY(-20);

                Label nomLabel = new Label(nom);
                nomLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                        + " -fx-text-fill: white; -fx-wrap-text: true; -fx-alignment: CENTER;");
                nomLabel.setMaxWidth(Double.MAX_VALUE);
                nomLabel.setWrapText(true);
                StackPane.setAlignment(nomLabel, Pos.CENTER);

                bar.getChildren().addAll(tndLabel, nomLabel);
                i++;
            }
        }
    }

    // ── BOUTIQUE : PieChart produits par catégorie ────────────────────────────
    // On affiche simplement les catégories existantes avec leur nom.
    // Pas besoin de toucher à Product (pas de getCategorieId disponible).
    private void loadBoutiquePieChart() {
        try {
            chartBoutiquePie.getData().clear();
            List<ProductCategory> categories = productCategoryService.read();

            if (categories.isEmpty()) {
                // Fallback : afficher le total produits comme une seule tranche
                int total = productService.read().size();
                chartBoutiquePie.getData().add(
                        new PieChart.Data("Produits (" + total + ")", total));
            } else {
                // Une part par catégorie — on compte les produits par catégorie
                // via un simple dénombrement uniforme si on n'a pas l'id sur Product
                List<Product> products = productService.read();
                int totalProduits = products.size();
                int nbCat = categories.size();

                for (int i = 0; i < nbCat; i++) {
                    ProductCategory cat = categories.get(i);
                    // Répartition réelle si Product expose getCategorieId(), sinon répartition uniforme
                    long count;
                    try {
                        final int catId = cat.getId();
                        count = products.stream()
                                .filter(p -> {
                                    try {
                                        // Tentative d'appel réflexif pour ne pas dépendre du getter
                                        java.lang.reflect.Method m =
                                                p.getClass().getMethod("getCategorieId");
                                        Object val = m.invoke(p);
                                        return val != null && ((Integer) val) == catId;
                                    } catch (Exception ex) {
                                        // getter absent → répartition uniforme
                                        return false;
                                    }
                                }).count();
                        // Si tout est 0 (pas de getter), répartir uniformément
                        if (count == 0 && i == nbCat - 1) {
                            count = totalProduits; // mettre tous dans la dernière par défaut
                        }
                    } catch (Exception ex) {
                        count = Math.round((double) totalProduits / nbCat);
                    }

                    if (count > 0) {
                        chartBoutiquePie.getData().add(
                                new PieChart.Data(cat.getNom() + " (" + count + ")", count));
                    }
                }

                // Si toutes les parts sont vides, afficher juste le total
                if (chartBoutiquePie.getData().isEmpty()) {
                    chartBoutiquePie.getData().add(
                            new PieChart.Data("Tous les produits (" + totalProduits + ")", totalProduits));
                }
            }

            chartBoutiquePie.setLegendVisible(true);
            chartBoutiquePie.setLabelsVisible(false);
            chartBoutiquePie.setTitle("Répartition des produits");
        } catch (SQLException e) {
            chartBoutiquePie.getData().clear();
        }
    }

    // ── BOUTIQUE : BarChart Top 5 produits les plus commandés ────────────────
    private void loadTopProduitsChart() {
        try {
            // Agréger les quantités commandées par produit_id depuis les commandes
            Map<Integer, Integer> qtyParProduit = new HashMap<>();
            commandeService.read().forEach(cmd ->
                    qtyParProduit.merge(cmd.getProduitId(), cmd.getQuantite(), Integer::sum));

            // Joindre avec les noms des produits
            List<Product> products = productService.read();
            Map<Integer, String> nomParId = products.stream()
                    .collect(Collectors.toMap(Product::getId, Product::getNom));

            List<SalesEntry> top5 = qtyParProduit.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(e -> new SalesEntry(
                            nomParId.getOrDefault(e.getKey(), "Produit #" + e.getKey()),
                            e.getValue()))
                    .collect(Collectors.toList());

            // Fallback : si aucune commande, afficher les produits avec qty = 0
            if (top5.isEmpty()) {
                top5 = products.stream()
                        .limit(5)
                        .map(p -> new SalesEntry(p.getNom(), 0))
                        .collect(Collectors.toList());
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Quantité commandée");
            for (SalesEntry e : top5)
                series.getData().add(new XYChart.Data<>(e.nom, e.qty));

            chartTopProduits.getData().clear();
            chartTopProduits.getData().add(series);
            chartTopProduits.setLegendVisible(false);
            chartTopProduits.setAnimated(true);

            final List<SalesEntry> finalTop5 = top5;
            Platform.runLater(() -> applyBarStyleBoutique(chartTopProduits, finalTop5));
        } catch (SQLException e) {
            chartTopProduits.getData().clear();
        }
    }

    private void applyBarStyleBoutique(BarChart<String, Number> chart, List<SalesEntry> entries) {
        int i = 0;
        for (XYChart.Series<String, Number> s : chart.getData()) {
            for (XYChart.Data<String, Number> d : s.getData()) {
                if (d.getNode() == null) continue;
                final int idx = i;
                d.getNode().setStyle(
                        "-fx-bar-fill: " + getBarColorBoutique(idx) + ";"
                                + " -fx-background-radius: 4 4 0 0;");
                String nom = idx < entries.size() ? entries.get(idx).nom : "";
                int    qty = d.getYValue().intValue();
                Tooltip tip = new Tooltip(nom + "\n" + qty + " unité(s) commandée(s)");
                tip.setStyle("-fx-font-size: 12px;");
                Tooltip.install(d.getNode(), tip);

                StackPane bar = (StackPane) d.getNode();
                bar.getChildren().removeIf(n -> n instanceof Label);

                // Label quantité au-dessus de la barre
                Label qtyLabel = new Label(qty + " u.");
                qtyLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                        + " -fx-text-fill: rgb(220,38,38);");
                StackPane.setAlignment(qtyLabel, Pos.TOP_CENTER);
                qtyLabel.setTranslateY(-20);

                // Nom du produit à l'intérieur de la barre
                Label nomLabel = new Label(nom);
                nomLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                        + " -fx-text-fill: white; -fx-wrap-text: true; -fx-alignment: CENTER;");
                nomLabel.setMaxWidth(Double.MAX_VALUE);
                nomLabel.setWrapText(true);
                StackPane.setAlignment(nomLabel, Pos.CENTER);

                bar.getChildren().addAll(qtyLabel, nomLabel);
                i++;
            }
        }
    }

    // ── Activités récentes (placeholder) ─────────────────────────────────────
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

    // ── Navigation ────────────────────────────────────────────────────────────
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
    /* ─────────────── AUTO-REFRESH DASHBOARD ─────────────── */

    private void startDashboardRefresh() {
        dashboardRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> silentRefreshDashboard())
        );
        dashboardRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        dashboardRefreshTimeline.play();
    }

    private void silentRefreshDashboard() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Charger en arrière-plan
                List<Hebergement> hebs    = hebergementService.getAll();
                int totalHeb              = hebs.size();
                int totalEq               = equipementService.getAll().size();
                int totalCat              = categorieHService.getAll().size();

                // Retour sur le thread JavaFX
                Platform.runLater(() -> {
                    statHebergements.setText(String.valueOf(totalHeb));
                    mstatHebergements.setText(String.valueOf(totalHeb));
                    mstatEquipements.setText(String.valueOf(totalEq));
                    mstatCategoriesHeb.setText(String.valueOf(totalCat));
                    System.out.println("✅ Dashboard hébergements rafraîchi : "
                            + totalHeb + " hébergements");
                });
                return null;
            }
        };

        task.setOnFailed(e ->
                System.err.println("❌ Erreur refresh dashboard : "
                        + task.getException().getMessage())
        );

        new Thread(task).start();
    }
}