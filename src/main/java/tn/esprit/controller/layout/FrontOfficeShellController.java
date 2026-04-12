package tn.esprit.controller.layout;

import javafx.scene.layout.Region;
import tn.esprit.controller.component.FrontNavbarController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class FrontOfficeShellController {

    @FXML private StackPane contentArea;
    @FXML private FrontNavbarController navbarController;

    @FXML
    public void initialize() { }

    public void loadContent(Node content, String routeName) {
        contentArea.getChildren().setAll(content);
        navbarController.setActiveRoute(routeName);

        // ✅ Force le contenu à prendre toute la hauteur disponible
        if (content instanceof Region region) {
            region.prefHeightProperty().bind(contentArea.heightProperty());
            region.minHeightProperty().bind(contentArea.heightProperty());
        }
    }
}