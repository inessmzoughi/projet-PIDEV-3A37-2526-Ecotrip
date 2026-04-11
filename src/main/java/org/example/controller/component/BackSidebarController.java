package org.example.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.example.navigation.Routes;
import org.example.session.SessionManager;
import org.example.navigation.SceneManager;

import java.util.Map;

public class BackSidebarController {

    @FXML private Button dashboardBtn;
    @FXML private Button hebergementsBtn;
    @FXML private Button listHebergementsBtn;
    @FXML private Button chambresBtn;
    @FXML private Button equipementsBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button transportBtn;
    @FXML private Button boutiqueBtn;
    @FXML private Button reservationsBtn;
    @FXML private Button usersBtn;
    @FXML private Button activitiesBtn;
    @FXML private Button activityCategoriesBtn;
    @FXML private Button guidesBtn;
    @FXML private Button schedulesBtn;

    private Map<Button, String> routeMap;

    @FXML
    public void initialize() {
        routeMap = Map.ofEntries(
                Map.entry(dashboardBtn,         Routes.ADMIN_DASHBOARD),
                Map.entry(hebergementsBtn,      Routes.ADMIN_HEBERGEMENTS),
                Map.entry(listHebergementsBtn,  Routes.ADMIN_HEBERGEMENTS),
                Map.entry(chambresBtn,          Routes.ADMIN_CHAMBRES),
                Map.entry(equipementsBtn,       Routes.ADMIN_EQUIPEMENTS),
                Map.entry(categoriesBtn,        Routes.ADMIN_CATEGORIES_HEBERGEMENT),
                Map.entry(transportBtn,         Routes.ADMIN_TRANSPORT),
                Map.entry(boutiqueBtn,          Routes.ADMIN_BOUTIQUE),
                Map.entry(reservationsBtn,      Routes.ADMIN_RESERVATIONS),
                Map.entry(usersBtn,             Routes.ADMIN_USERS),
                Map.entry(activitiesBtn,          Routes.ADMIN_ACTIVITIES),
                Map.entry(activityCategoriesBtn,  Routes.ADMIN_ACTIVITY_CATEGORIES),
                Map.entry(guidesBtn,              Routes.ADMIN_GUIDES),
                Map.entry(schedulesBtn,           Routes.ADMIN_SCHEDULES)
        );
    }

    public void setActiveRoute(String routeName) {
        routeMap.forEach((btn, route) -> {
            btn.getStyleClass().remove("sidebar-btn-active");
            if (route.equals(routeName)) btn.getStyleClass().add("sidebar-btn-active");
        });
    }

    @FXML private void handleDashboard()    { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void handleActivites()    { SceneManager.navigateTo(Routes.ADMIN_ACTIVITES); }
    @FXML private void handleHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleListHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void handleChambres()     { SceneManager.navigateTo(Routes.ADMIN_CHAMBRES); }
    @FXML private void handleEquipements()  { SceneManager.navigateTo(Routes.ADMIN_EQUIPEMENTS); }
    @FXML private void handleCategories()   { SceneManager.navigateTo(Routes.ADMIN_CATEGORIES_HEBERGEMENT); }
    @FXML private void handleTransport()    { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void handleBoutique()     { SceneManager.navigateTo(Routes.ADMIN_BOUTIQUE); }
    @FXML private void handleReservations() { SceneManager.navigateTo(Routes.ADMIN_RESERVATIONS); }
    @FXML private void handleUsers()        { SceneManager.navigateTo(Routes.ADMIN_USERS); }
    @FXML private void handleViewSite()     { SceneManager.navigateTo(Routes.HOME); }
    @FXML private void handleLogout()       { SessionManager.getInstance().logout(); }
    @FXML private void handleActivities()         { SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }
    @FXML private void handleActivityCategories() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITY_CATEGORIES); }
    @FXML private void handleGuides()             { SceneManager.navigateTo(Routes.ADMIN_GUIDES); }
    @FXML private void handleSchedules()          { SceneManager.navigateTo(Routes.ADMIN_SCHEDULES); }
}