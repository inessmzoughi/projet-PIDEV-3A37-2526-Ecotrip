package tn.esprit.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.session.SessionManager;
import java.util.Map;

public class FrontNavbarController {

    @FXML private Button homeBtn, hebergBtn, activitesBtn;
    @FXML private Button transportBtn, boutiqueBtn, contactBtn;
    @FXML private Button reservationsBtn, monCompteBtn, logoutBtn, loginBtn;

    // Map each button to its route for active-state highlighting
    private Map<Button, String> routeMap;

    @FXML
    public void initialize() {
        routeMap = Map.of(
                homeBtn,        Routes.HOME,
                hebergBtn,      Routes.HEBERGEMENTS,
                activitesBtn,   Routes.ACTIVITES,
                transportBtn,   Routes.TRANSPORT,
                boutiqueBtn,    Routes.FRONT_PRODUCTS,
                contactBtn,     Routes.CONTACT
        );

        // Show/hide auth buttons based on session
        boolean loggedIn = SessionManager.getInstance().isLoggedIn();
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        reservationsBtn.setVisible(loggedIn);  reservationsBtn.setManaged(loggedIn);
        if (isAdmin){
            monCompteBtn.setText("Dashboard");
        }
//        monCompteBtn.setManaged(loggedIn);
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
    @FXML private void handleHeberg()       { SceneManager.navigateTo(Routes.HEBERGEMENTS); }
    @FXML private void handleActivites()    { SceneManager.navigateTo(Routes.ACTIVITES); }
    @FXML private void handleTransport()    { SceneManager.navigateTo(Routes.TRANSPORT); }
    @FXML private void handleBoutique()     { SceneManager.navigateTo(Routes.FRONT_PRODUCTS); }
    @FXML private void handleContact()      { SceneManager.navigateTo(Routes.CONTACT); }
    @FXML private void handleReservations() { SceneManager.navigateTo(Routes.MES_RESERVATIONS); }
    @FXML private void handleMonCompte()    {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        if (isAdmin){
            SceneManager.navigateTo(Routes.ADMIN_DASHBOARD);
        }else SceneManager.navigateTo(Routes.FRONT_MON_COMPTE); }
    @FXML private void handleLogin()        { SceneManager.navigateTo(Routes.LOGIN); }
    @FXML private void handleLogout()       { SessionManager.getInstance().logout(); }
}