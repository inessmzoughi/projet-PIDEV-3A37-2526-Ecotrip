package tn.esprit.controller.layout;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.controller.component.BackSidebarController;
import tn.esprit.session.SessionManager;

public class BackOfficeShellController {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitle;
    @FXML private Label adminNameLabel;
    @FXML private BackSidebarController sidebarController;

//    @FXML
//    public void initialize() {
//        String name = SessionManager.getInstance().getCurrentUser().getUsername();
//        adminNameLabel.setText("👤 " + name);
//    }

    public void loadContent(Node content, String routeName) {
        contentArea.getChildren().setAll(content);
        sidebarController.setActiveRoute(routeName);
//        pageTitle.setText(getPageTitle(routeName));
    }

    private String getPageTitle(String routeName) {
        return switch (routeName) {
            case "admin-dashboard"              -> "Dashboard — Vue d'ensemble";
            case "admin-activites"              -> "Module Activités";
            case "admin-transport"              -> "Module Transport";
            case "admin-chauffeurs"             -> "Chauffeurs";
            case "admin-transport-categories"   -> "Categories Transport";
            case "admin-boutique"               -> "Module Boutique";
            case "admin-reservations"           -> "Réservations";
            case "admin-users"                  -> "Gestion Utilisateurs";
            case "admin-mon-compte"             -> "Mon Compte";
            case "admin-hebergements"           -> "Hébergements";
            case "admin-add-hebergement"        -> "Ajouter un Hébergement";
            case "admin-edit-hebergement"       -> "Modifier un Hébergement";
            case "admin-categories-hebergement" -> "Catégories d'Hébergement";
            case "admin-chambres"               -> "Chambres";
            case "admin-equipements"            -> "Équipements";
            case "admin-activities"           -> "Activités";
            case "admin-activity-categories"  -> "Catégories d'Activités";
            case "admin-guides"               -> "Guides";
            case "admin-schedules"            -> "Plannings";
            default                             -> "EcoTrip Admin";
        };
    }
}
