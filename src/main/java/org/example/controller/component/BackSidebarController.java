package org.example.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.example.navigation.Routes;
import org.example.session.SessionManager;
import org.example.navigation.SceneManager;

import java.util.List;
import java.util.Map;

public class BackSidebarController {

    @FXML private Button dashboardBtn, activitesBtn, hebergementsBtn;
    @FXML private Button transportBtn, boutiqueBtn, reservationsBtn, usersBtn;

    private Map<Button, String> routeMap;

    @FXML
    public void initialize() {
        routeMap = Map.of(
                dashboardBtn,    Routes.ADMIN_DASHBOARD,
                activitesBtn,    Routes.ADMIN_ACTIVITES,
                hebergementsBtn, Routes.ADMIN_HEBERGEMENTS,
                transportBtn,    Routes.ADMIN_TRANSPORT,
                boutiqueBtn,     Routes.ADMIN_BOUTIQUE,
                reservationsBtn, Routes.ADMIN_RESERVATIONS,
                usersBtn,        Routes.ADMIN_USERS
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
    @FXML private void handleTransport()    { SceneManager.navigateTo(Routes.ADMIN_TRANSPORT); }
    @FXML private void handleBoutique()     { SceneManager.navigateTo(Routes.ADMIN_BOUTIQUE); }
    @FXML private void handleReservations() { SceneManager.navigateTo(Routes.ADMIN_RESERVATIONS); }
    @FXML private void handleUsers()        { SceneManager.navigateTo(Routes.ADMIN_USERS); }
    @FXML private void handleViewSite()     { SceneManager.navigateTo(Routes.HOME); }
    @FXML private void handleLogout()       { SessionManager.getInstance().logout(); }
}