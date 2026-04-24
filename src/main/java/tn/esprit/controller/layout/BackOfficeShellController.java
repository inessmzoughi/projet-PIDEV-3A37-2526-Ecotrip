package tn.esprit.controller.layout;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.controller.component.BackSidebarController;
import tn.esprit.services.face.FaceGate;
import tn.esprit.session.SessionManager;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class BackOfficeShellController  implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitle;
    @FXML private Label adminNameLabel;
    @FXML private BackSidebarController sidebarController;
    @FXML private VBox faceNudgeBanner;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ... your existing init code ...

        // Show face enrollment nudge if admin hasn't enrolled yet
        checkFaceNudge();
    }
    private void checkFaceNudge() {
        // Only show once per session
        if (SessionManager.getInstance().isFaceEnrollmentNudgeShown()) {
            faceNudgeBanner.setVisible(false);
            faceNudgeBanner.setManaged(false);
            return;
        }

        if (!FaceGate.isEnrolled()) {
            faceNudgeBanner.setVisible(true);
            faceNudgeBanner.setManaged(true);
            SessionManager.getInstance().markFaceEnrollmentNudgeShown();
        } else {
            faceNudgeBanner.setVisible(false);
            faceNudgeBanner.setManaged(false);
        }
    }

    @FXML
    private void onDismissNudge() {
        faceNudgeBanner.setVisible(false);
        faceNudgeBanner.setManaged(false);
    }

    @FXML
    private void onGoToFaceEnrollment() {
        faceNudgeBanner.setVisible(false);
        faceNudgeBanner.setManaged(false);
        SessionManager.getInstance().requestOpenFacePanel();
        SceneManager.navigateTo(Routes.ADMIN_MON_COMPTE);
        // MonCompteController will open the face panel automatically — see Step 5
    }

    public void loadContent(Node content, String routeName) {
        contentArea.getChildren().setAll(content);
        sidebarController.setActiveRoute(routeName);
//        pageTitle.setText(getPageTitle(routeName));
    }

}
