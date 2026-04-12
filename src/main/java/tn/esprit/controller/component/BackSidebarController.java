package tn.esprit.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.session.SessionManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class BackSidebarController {

    @FXML private Button dashboardBtn;
    @FXML private Button activitesBtn;
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

    private Map<Button, String> routeMap;

    @FXML
    public void initialize() {
        routeMap = new LinkedHashMap<>();
        registerRoute(dashboardBtn, Routes.ADMIN_DASHBOARD);
        registerRoute(activitesBtn, Routes.ADMIN_ACTIVITES);
        registerRoute(listHebergementsBtn, Routes.ADMIN_HEBERGEMENTS);
        registerRoute(chambresBtn, Routes.ADMIN_CHAMBRES);
        registerRoute(equipementsBtn, Routes.ADMIN_EQUIPEMENTS);
        registerRoute(categoriesBtn, Routes.ADMIN_CATEGORIES_HEBERGEMENT);
        registerRoute(transportBtn, Routes.ADMIN_TRANSPORT);
        registerRoute(boutiqueBtn1, Routes.ADMIN_COMMANDE);
        registerRoute(boutiqueBtn2, Routes.ADMIN_PRODUCT);
        registerRoute(boutiqueBtn3, Routes.ADMIN_LIGNE_COMMANDE);
        registerRoute(boutiqueBtn31, Routes.ADMIN_PAIEMENT);
        registerRoute(boutiqueBtn321, Routes.ADMIN_PRODUCT_CATEGORY);
        registerRoute(reservationsBtn, Routes.ADMIN_RESERVATIONS);
        registerRoute(usersBtn, Routes.ADMIN_USERS);
        registerRoute(monCompteBtn, Routes.ADMIN_MON_COMPTE);
        registerRoute(activitiesBtn, Routes.ADMIN_ACTIVITIES);
        registerRoute(activityCategoriesBtn, Routes.ADMIN_ACTIVITY_CATEGORIES);
        registerRoute(guidesBtn, Routes.ADMIN_GUIDES);
        registerRoute(schedulesBtn, Routes.ADMIN_SCHEDULES);
    }

    private void registerRoute(Button button, String route) {
        if (button != null) {
            routeMap.put(button, route);
        }
    }

    public void setActiveRoute(String routeName) {
        routeMap.forEach((btn, route) -> {
            btn.getStyleClass().removeAll("sidebar-btn-active", "sidebar-sub-btn-active");
            boolean transportRoute = btn == transportBtn
                    && (Routes.ADMIN_TRANSPORT.equals(routeName)
                    || Routes.ADMIN_CHAUFFEURS.equals(routeName)
                    || Routes.ADMIN_TRANSPORT_CATEGORIES.equals(routeName));
            if (route.equals(routeName) || transportRoute) {
                if (btn == listHebergementsBtn || btn == chambresBtn
                        || btn == equipementsBtn || btn == categoriesBtn) {
                    btn.getStyleClass().add("sidebar-sub-btn-active");
                } else {
                    btn.getStyleClass().add("sidebar-btn-active");
                }
            }
        });
    }

    @FXML private void handleDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void handleActivites() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITES); }
    @FXML private void handleListHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleChambres() { SceneManager.navigateTo(Routes.ADMIN_CHAMBRES); }
    @FXML private void handleEquipements() { SceneManager.navigateTo(Routes.ADMIN_EQUIPEMENTS); }
    @FXML private void handleCategories() { SceneManager.navigateTo(Routes.ADMIN_CATEGORIES_HEBERGEMENT); }
    @FXML private void handleTransport() { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void handleCommandes() { SceneManager.navigateTo(Routes.ADMIN_COMMANDE); }
    @FXML private void handleLignesCommande() { SceneManager.navigateTo(Routes.ADMIN_LIGNE_COMMANDE); }
    @FXML private void handlePaiements() { SceneManager.navigateTo(Routes.ADMIN_PAIEMENT); }
    @FXML private void handleProduits() { SceneManager.navigateTo(Routes.ADMIN_PRODUCT); }
    @FXML private void handleProductsCategory() { SceneManager.navigateTo(Routes.ADMIN_PRODUCT_CATEGORY); }
    @FXML private void handleReservations() { SceneManager.navigateTo(Routes.ADMIN_RESERVATIONS); }
    @FXML private void handleUsers() { SceneManager.navigateTo(Routes.ADMIN_USERS); }
    @FXML private void handleViewSite() { SceneManager.navigateTo(Routes.HOME); }
    @FXML private void handleMonCompte() { SceneManager.navigateTo(Routes.ADMIN_MON_COMPTE); }
    @FXML private void handleLogout() { SessionManager.getInstance().logout(); }
    @FXML private void handleActivities() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }
    @FXML private void handleActivityCategories() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITY_CATEGORIES); }
    @FXML private void handleGuides() { SceneManager.navigateTo(Routes.ADMIN_GUIDES); }
    @FXML private void handleSchedules() { SceneManager.navigateTo(Routes.ADMIN_SCHEDULES); }
}
