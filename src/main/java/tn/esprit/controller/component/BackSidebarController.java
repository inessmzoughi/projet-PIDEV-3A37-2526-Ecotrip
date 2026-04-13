package tn.esprit.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import tn.esprit.navigation.Routes;
import tn.esprit.session.SessionManager;
import tn.esprit.navigation.SceneManager;

import java.util.Map;

public class BackSidebarController {

    @FXML private Button dashboardBtn;
    @FXML private Button activitesBtn;
    // hebergementsBtn supprimé — remplacé par un Label non-cliquable dans le FXML
    @FXML private Button listHebergementsBtn;
    @FXML private Button chambresBtn;
    @FXML private Button equipementsBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button transportBtn;
    @FXML private Button reservationsBtn;
    @FXML private Button usersBtn;
    @FXML private Button monCompteBtn;
    @FXML private Button boutiqueBtn1;
    @FXML private Button boutiqueBtn2;
    @FXML private Button boutiqueBtn3;
    @FXML private Button boutiqueBtn31;
    @FXML private Button boutiqueBtn321;
    @FXML private Button activitiesBtn;
    @FXML private Button activityCategoriesBtn;
    @FXML private Button guidesBtn;
    @FXML private Button schedulesBtn;
    @FXML private VBox activitiesBox;
    @FXML private VBox hebergementsBox;
    @FXML private VBox boutiqueBox;


    private Map<Button, String> routeMap;

    @FXML
    public void initialize() {
        routeMap = Map.ofEntries(
                Map.entry(dashboardBtn,        Routes.ADMIN_DASHBOARD),
                Map.entry(listHebergementsBtn, Routes.ADMIN_HEBERGEMENTS),
                Map.entry(chambresBtn,         Routes.ADMIN_CHAMBRES),
                Map.entry(equipementsBtn,      Routes.ADMIN_EQUIPEMENTS),
                Map.entry(categoriesBtn,       Routes.ADMIN_CATEGORIES_HEBERGEMENT),
                Map.entry(transportBtn,        Routes.ADMIN_TRANSPORT),
                Map.entry(boutiqueBtn1,        Routes.ADMIN_COMMANDE),
                Map.entry(boutiqueBtn2,        Routes.ADMIN_PRODUCT),
                Map.entry(boutiqueBtn3,        Routes.ADMIN_LIGNE_COMMANDE),
                Map.entry(boutiqueBtn31,       Routes.ADMIN_PAIEMENT),
                Map.entry(boutiqueBtn321,      Routes.ADMIN_PRODUCT_CATEGORY),
                Map.entry(reservationsBtn,     Routes.ADMIN_RESERVATIONS),
                Map.entry(usersBtn,            Routes.ADMIN_USERS),
                Map.entry(monCompteBtn,        Routes.ADMIN_MON_COMPTE),
                Map.entry(activitiesBtn,          Routes.ADMIN_ACTIVITIES),
                Map.entry(activityCategoriesBtn,  Routes.ADMIN_ACTIVITY_CATEGORIES),
                Map.entry(guidesBtn,              Routes.ADMIN_GUIDES),
                Map.entry(schedulesBtn,           Routes.ADMIN_SCHEDULES)

        );
    }

    public void setActiveRoute(String routeName) {
        routeMap.forEach((btn, route) -> {
            btn.getStyleClass().removeAll("sidebar-btn-active", "sidebar-sub-btn-active");
            if (route.equals(routeName)) {
                // sous-éléments hébergement → style sub-actif
                if (btn == listHebergementsBtn || btn == chambresBtn
                        || btn == equipementsBtn || btn == categoriesBtn) {
                    btn.getStyleClass().add("sidebar-sub-btn-active");
                } else {
                    btn.getStyleClass().add("sidebar-btn-active");
                }
            }
        });
    }
    private void toggleBox(VBox box) {
        boolean isVisible = box.isVisible();
        box.setVisible(!isVisible);
        box.setManaged(!isVisible);
    }
    @FXML private void toggleActivities() {
        toggleBox(activitiesBox);
    }

    @FXML private void toggleHebergements() {
        toggleBox(hebergementsBox);
    }

    @FXML private void toggleBoutique() {
        toggleBox(boutiqueBox);
    }

    @FXML private void handleDashboard()        { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    @FXML private void handleListHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleChambres()         { SceneManager.navigateTo(Routes.ADMIN_CHAMBRES); }
    @FXML private void handleEquipements()      { SceneManager.navigateTo(Routes.ADMIN_EQUIPEMENTS); }
    @FXML private void handleCategories()       { SceneManager.navigateTo(Routes.ADMIN_CATEGORIES_HEBERGEMENT); }
    @FXML private void handleTransport()        { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }

    @FXML private void handleCommandes()        { SceneManager.navigateTo(Routes.ADMIN_COMMANDE); }
    @FXML private void handleLignesCommande()   { SceneManager.navigateTo(Routes.ADMIN_LIGNE_COMMANDE); }
    @FXML private void handlePaiements()        { SceneManager.navigateTo(Routes.ADMIN_PAIEMENT); }
    @FXML private void handleProduits()         { SceneManager.navigateTo(Routes.ADMIN_PRODUCT); }
    @FXML private void handleProductsCategory() { SceneManager.navigateTo(Routes.ADMIN_PRODUCT_CATEGORY);}

    @FXML private void handleReservations()     { SceneManager.navigateTo(Routes.ADMIN_RESERVATIONS); }
    @FXML private void handleUsers()            { SceneManager.navigateTo(Routes.ADMIN_USERS); }
    @FXML private void handleViewSite()         { SceneManager.navigateTo(Routes.HOME); }

    @FXML private void handleMonCompte()        { SceneManager.navigateTo(Routes.ADMIN_MON_COMPTE); }

    @FXML private void handleLogout()           { SessionManager.getInstance().logout(); }

    @FXML private void handleActivities()         { SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }
    @FXML private void handleActivityCategories() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITY_CATEGORIES); }
    @FXML private void handleGuides()             { SceneManager.navigateTo(Routes.ADMIN_GUIDES); }
    @FXML private void handleSchedules()          { SceneManager.navigateTo(Routes.ADMIN_SCHEDULES); }
}