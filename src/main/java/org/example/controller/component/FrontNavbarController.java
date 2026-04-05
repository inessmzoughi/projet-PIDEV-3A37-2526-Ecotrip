package org.example.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.session.SessionManager;
import java.util.Map;

public class FrontNavbarController {

    @FXML private Button homeBtn, aboutBtn, hebergBtn, activitesBtn;
    @FXML private Button transportBtn, boutiqueBtn, contactBtn;
    @FXML private Button reservationsBtn, monCompteBtn, logoutBtn, loginBtn;

    // Map each button to its route for active-state highlighting
    private Map<Button, String> routeMap;

    @FXML
    public void initialize() {
        routeMap = Map.of(
                homeBtn,        Routes.HOME,
                aboutBtn,       Routes.ABOUT,
                hebergBtn,      Routes.HEBERGEMENTS,
                activitesBtn,   Routes.ACTIVITES,
                transportBtn,   Routes.TRANSPORT,
                boutiqueBtn,    Routes.BOUTIQUE,
                contactBtn,     Routes.CONTACT
        );

        // Show/hide auth buttons based on session
        boolean loggedIn = SessionManager.getInstance().isLoggedIn();
        reservationsBtn.setVisible(loggedIn);  reservationsBtn.setManaged(loggedIn);
        monCompteBtn.setVisible(loggedIn);     monCompteBtn.setManaged(loggedIn);
        logoutBtn.setVisible(loggedIn);        logoutBtn.setManaged(loggedIn);
        loginBtn.setVisible(!loggedIn);        loginBtn.setManaged(!loggedIn);
    }

    public void setActiveRoute(String routeName) {
        // Remove active from all, add to matching
        routeMap.forEach((btn, route) -> {
            btn.getStyleClass().remove("nav-btn-active");
            if (route.equals(routeName)) btn.getStyleClass().add("nav-btn-active");
        });
    }

    @FXML private void handleHome()         { SceneManager.navigateTo(Routes.HOME); }
    @FXML private void handleAbout()        { SceneManager.navigateTo(Routes.ABOUT); }
    @FXML private void handleHeberg()       { SceneManager.navigateTo(Routes.HEBERGEMENTS); }
    @FXML private void handleActivites()    { SceneManager.navigateTo(Routes.ACTIVITES); }
    @FXML private void handleTransport()    { SceneManager.navigateTo(Routes.TRANSPORT); }
    @FXML private void handleBoutique()     { SceneManager.navigateTo(Routes.BOUTIQUE); }
    @FXML private void handleContact()      { SceneManager.navigateTo(Routes.CONTACT); }
    @FXML private void handleReservations() { SceneManager.navigateTo(Routes.MES_RESERVATIONS); }
    @FXML private void handleMonCompte()    { SceneManager.navigateTo(Routes.MON_COMPTE); }
    @FXML private void handleLogin()        { SceneManager.navigateTo(Routes.LOGIN); }
    @FXML private void handleLogout()       { SessionManager.getInstance().logout(); }
}