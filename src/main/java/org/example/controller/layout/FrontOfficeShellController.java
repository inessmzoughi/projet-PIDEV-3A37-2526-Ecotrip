package org.example.controller.layout;

import org.example.controller.component.FrontNavbarController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class FrontOfficeShellController {

    @FXML private StackPane contentArea;
    @FXML private FrontNavbarController navbarController; // auto-injected by fx:include

    @FXML
    public void initialize() {
        // nothing needed here — navbar handles itself
    }

    public void loadContent(Node content, String routeName) {
        contentArea.getChildren().setAll(content);
        navbarController.setActiveRoute(routeName); // highlight correct nav link
    }
}