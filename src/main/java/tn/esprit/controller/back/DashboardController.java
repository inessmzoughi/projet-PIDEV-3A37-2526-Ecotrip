package tn.esprit.controller.back;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.CategorieH_service;
import tn.esprit.services.hebergement.Chambre_service;
import tn.esprit.services.hebergement.Equipement_service;
import tn.esprit.services.hebergement.Hebergement_service;
import tn.esprit.session.SessionManager;

import java.sql.SQLException;
import java.util.List;

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
    @FXML private Label mstatHebergements;
    @FXML private Label mstatChambres;
    @FXML private Label mstatEquipements;
    @FXML private Label mstatCategoriesHeb;
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

    // ── Services Hébergement ─────────────────────────────────
    private final Hebergement_service hebergementService = new Hebergement_service();
    private final Chambre_service     chambreService     = new Chambre_service();
    private final Equipement_service  equipementService  = new Equipement_service();
    private final CategorieH_service  categorieHService  = new CategorieH_service();

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

    // ── Chargement des stats ─────────────────────────────────
    private void loadStats() {
        // ── Stats hébergement (données réelles) ──
        try {
            int totalHeb  = hebergementService.getAll().size();
            int totalCh   = chambreService.getAll().size();
            int totalEq   = equipementService.getAll().size();
            int totalCat  = categorieHService.getAll().size();

            List<Hebergement> hebs = hebergementService.getAll();
            long actifs = hebs.stream().filter(h -> h.getActif() == 1).count();

            // Top cards
            statHebergements.setText(String.valueOf(totalHeb));

            // Module hébergement
            mstatHebergements.setText(String.valueOf(totalHeb));
            mstatChambres.setText(String.valueOf(totalCh));
            mstatEquipements.setText(String.valueOf(totalEq));
            mstatCategoriesHeb.setText(String.valueOf(totalCat));

        } catch (SQLException e) {
            mstatHebergements.setText("—");
            mstatChambres.setText("—");
            mstatEquipements.setText("—");
            mstatCategoriesHeb.setText("—");
            statHebergements.setText("—");
        }

        // ── Stats autres modules (à connecter plus tard) ──
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

    // ── Chargement des graphiques ────────────────────────────
    private void loadCharts() {
        // ── BAR CHART ─────────────────────────────────────────
        try {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nombre de chambres");
            List<Hebergement> hebs = hebergementService.getAll();

            for (Hebergement h : hebs) {
                int count = chambreService.getByHebergement(h.getId()).size();
                // ✅ Supprimé le if (count > 0) — affiche même les hébergements sans chambres
                String nom = h.getNom().length() > 12
                        ? h.getNom().substring(0, 12) + "…"
                        : h.getNom();
                series.getData().add(new XYChart.Data<>(nom, count));
            }

            chartChambres.getData().clear();
            chartChambres.getData().add(series);
            chartChambres.setLegendVisible(true);
            chartChambres.setTitle("Chambres par hébergement");

        } catch (SQLException e) {
            chartChambres.getData().clear();
        }

        // ── PIE CHART ─────────────────────────────────────────
        try {
            chartHebPie.getData().clear();
            List<Hebergement> hebs = hebergementService.getAll();
            java.util.Map<Integer, Long> countParCat = hebs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Hebergement::getCategorie_id,
                            java.util.stream.Collectors.counting()
                    ));
            for (java.util.Map.Entry<Integer, Long> entry : countParCat.entrySet()) {
                String nom;
                try {
                    var cat = categorieHService.getById(entry.getKey());
                    nom = (cat != null) ? cat.getNom() : "Cat " + entry.getKey();
                } catch (SQLException ex) {
                    nom = "Cat " + entry.getKey();
                }
                chartHebPie.getData().add(
                        new PieChart.Data(nom + " (" + entry.getValue() + ")", entry.getValue()));
            }
            chartHebPie.setLegendVisible(true);
            chartHebPie.setLabelsVisible(true);
            chartHebPie.setTitle("Répartition des hébergements");

        } catch (SQLException e) {
            chartHebPie.getData().clear();
        }
    }

    // ── Activités récentes ───────────────────────────────────
    private void loadRecentActivites() {
        // À connecter avec ton service Activité quand disponible
        recentActivitesList.getChildren().clear();
        recentActivitesList.getChildren().add(
                buildRecentItem("Randonnée Zaghouan", "Zaghouan • 45 TND", true));
        recentActivitesList.getChildren().add(
                buildRecentItem("Plongée Tabarka", "Tabarka • 80 TND", true));
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
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
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
