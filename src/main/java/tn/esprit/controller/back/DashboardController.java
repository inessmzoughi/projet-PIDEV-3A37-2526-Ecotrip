package tn.esprit.controller.back;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.*;
import tn.esprit.session.SessionManager;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    @FXML private HBox topRevenueCards;

    // Nouvelles cards
    @FXML private Label mstatActifs;
    @FXML private Label mstatInactifs;
    @FXML private Label mstatAvgEtoiles;
    // Top 5 table
    @FXML private TableView<HebergementRow>            topHebergementsTable;
    @FXML private TableColumn<HebergementRow, String>  colTopNom;
    @FXML private TableColumn<HebergementRow, String>  colTopVille;
    @FXML private TableColumn<HebergementRow, String>  colTopEtoiles;
    @FXML private TableColumn<HebergementRow, Integer> colTopChambres;
    @FXML private TableColumn<HebergementRow, String>  colTopActif;
    // PieChart conservé
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


    // ── DTO interne Top 5 ────────────────────────────────────
    public static class HebergementRow {
        private final String nom, ville, etoiles, actif;
        private final int chambres;

        public HebergementRow(String nom, String ville, int etoiles, int chambres, int actif) {
            this.nom      = nom;
            this.ville    = ville;
            this.etoiles  = "⭐".repeat(Math.max(0, etoiles));
            this.chambres = chambres;
            this.actif    = actif == 1 ? "✅ Actif" : "❌ Inactif";
        }
        public String getNom()      { return nom; }
        public String getVille()    { return ville; }
        public String getEtoiles()  { return etoiles; }
        public int    getChambres() { return chambres; }
        public String getActif()    { return actif; }
    }

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }
        setupTopTable();
        loadStats();
        loadCharts();
        loadRecentActivites();
    }

    // ── Setup colonnes Top 5 ─────────────────────────────────
    private void setupTopTable() {
        topHebergementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colTopNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colTopVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colTopEtoiles.setCellValueFactory(new PropertyValueFactory<>("etoiles"));
        colTopChambres.setCellValueFactory(new PropertyValueFactory<>("chambres"));
        colTopActif.setCellValueFactory(new PropertyValueFactory<>("actif"));

        colTopChambres.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(String.valueOf(item));
                badge.setStyle(
                        "-fx-background-color:" + (item >= 5 ? "#d1fae5" : "#fef3c7") + ";"
                                + "-fx-text-fill:"        + (item >= 5 ? "#065f46" : "#92400e") + ";"
                                + "-fx-font-weight:bold; -fx-background-radius:6;"
                                + "-fx-padding:2 10 2 10;");
                setGraphic(badge); setText(null);
            }
        });
    }

    // ── Stats ────────────────────────────────────────────────
    private void loadStats() {
        // ── Stats hébergement (données réelles) ──
        try {
            List<Hebergement> hebs = hebergementService.getAll();
            int totalHeb  = hebs.size();
            int totalCh   = chambreService.getAll().size();
            int totalEq   = equipementService.getAll().size();
            int totalCat  = categorieHService.getAll().size();
            long actifs   = hebs.stream().filter(h -> h.getActif() == 1).count();
            long inactifs = totalHeb - actifs;
            double avgEt  = hebs.stream().mapToInt(Hebergement::getNb_etoiles).average().orElse(0);

            statHebergements.setText(String.valueOf(totalHeb));
            mstatHebergements.setText(String.valueOf(totalHeb));
            mstatChambres.setText(String.valueOf(totalCh));
            mstatEquipements.setText(String.valueOf(totalEq));
            mstatCategoriesHeb.setText(String.valueOf(totalCat));
            mstatActifs.setText(String.valueOf(actifs));
            mstatInactifs.setText(String.valueOf(inactifs));
            mstatAvgEtoiles.setText(String.format("%.1f / 5", avgEt));

            // Top 5 par nombre de chambres
            List<HebergementRow> top5 = hebs.stream()
                    .map(h -> {
                        int count = 0;
                        try { count = chambreService.getByHebergement(h.getId()).size(); }
                        catch (SQLException ignored) {}
                        return new HebergementRow(
                                h.getNom(), h.getVille(), h.getNb_etoiles(), count, h.getActif());
                    })
                    .sorted(Comparator.comparingInt(HebergementRow::getChambres).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            topHebergementsTable.setItems(FXCollections.observableArrayList(top5));

        } catch (SQLException e) {
            mstatHebergements.setText("—");
            mstatChambres.setText("—");
            mstatEquipements.setText("—");
            mstatCategoriesHeb.setText("—");
            mstatActifs.setText("—");
            mstatInactifs.setText("—");
            mstatAvgEtoiles.setText("—");
            statHebergements.setText("—");
        }

        // ── Autres modules : pas touché ──
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
        // PieChart conservé intact
        try {
            chartHebPie.getData().clear();
            List<Hebergement> hebs = hebergementService.getAll();
            java.util.Map<Integer, Long> countParCat = hebs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Hebergement::getCategorie_id,
                            java.util.stream.Collectors.counting()));
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