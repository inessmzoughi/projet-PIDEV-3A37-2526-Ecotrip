package tn.esprit.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;

import java.awt.Desktop;
import java.net.URI;

public class FrontFooterController {

    @FXML
    private TextField emailField;

    @FXML
    private Label infoLabel;

    @FXML
    private Hyperlink authLink;

    private boolean isLoggedIn = false; // replace with real auth state

    @FXML
    public void initialize() {
        updateAuthLink();
    }

    // =======================
    // AUTH
    // =======================
    private void updateAuthLink() {
        if (isLoggedIn) {
            authLink.setText("Déconnexion");
        } else {
            authLink.setText("Connexion");
        }
    }

    @FXML
    private void handleAuth() {
        if (isLoggedIn) {
            System.out.println("Logout...");
            isLoggedIn = false;
        } else {
            System.out.println("Redirect to login...");
        }
        updateAuthLink();
    }

    // =======================
    // NAVIGATION
    // =======================
    @FXML private void goHome() { SceneManager.navigateTo(Routes.HOME); }
    @FXML private void goHebergement() { SceneManager.navigateTo(Routes.HEBERGEMENTS); }
    @FXML private void goActivites() { SceneManager.navigateTo(Routes.ACTIVITES); }
    @FXML private void goTransport() { SceneManager.navigateTo(Routes.TRANSPORT); }
    @FXML private void goBoutique() { SceneManager.navigateTo(Routes.FRONT_PRODUCTS); }
    @FXML private void goAbout() { SceneManager.navigateTo(Routes.ABOUT); }
    @FXML private void goContact() { SceneManager.navigateTo(Routes.CONTACT); }


    // =======================
    // NEWSLETTER
    // =======================
    @FXML
    private void handleSubscribe() {
        String email = emailField.getText();

        if (email == null || email.isEmpty()) {
            infoLabel.setText("Veuillez entrer un email.");
            return;
        }

        // TODO: call backend API
        System.out.println("Subscribed: " + email);

        infoLabel.setText("Inscription réussie !");
        emailField.clear();
    }

    // =======================
    // SOCIAL LINKS
    // =======================
    @FXML private void openFacebook() { openLink("https://facebook.com"); }
    @FXML private void openInstagram() { openLink("https://instagram.com"); }
    @FXML private void openTwitter() { openLink("https://twitter.com"); }
    @FXML private void openYoutube() { openLink("https://youtube.com"); }

    private void openLink(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}