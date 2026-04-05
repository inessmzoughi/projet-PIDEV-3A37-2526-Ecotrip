package org.example.controller.layout;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.example.controller.component.BackSidebarController;
import org.example.session.SessionManager;

public class BackOfficeShellController {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitle;
    @FXML private Label adminNameLabel;
    @FXML private BackSidebarController sidebarController; // auto-injected

    @FXML
    public void initialize() {
        String name = SessionManager.getInstance().getCurrentUser().getUsername();
        adminNameLabel.setText("👤 " + name);
    }

    public void loadContent(Node content, String routeName) {
        contentArea.getChildren().setAll(content);
        sidebarController.setActiveRoute(routeName);
        pageTitle.setText(getPageTitle(routeName));
    }

    private String getPageTitle(String routeName) {
        return switch (routeName) {
            case "admin-dashboard"    -> "Dashboard — Vue d'ensemble";
            case "admin-activites"    -> "Module Activités";
            case "admin-hebergements" -> "Module Hébergements";
            case "admin-transport"    -> "Module Transport";
            case "admin-boutique"     -> "Module Boutique";
            case "admin-reservations" -> "Réservations";
            case "admin-users"        -> "Gestion Utilisateurs";
            case "admin-mon-compte"   -> "Mon Compte";
            default -> "EcoTrip Admin";
        };
    }
}