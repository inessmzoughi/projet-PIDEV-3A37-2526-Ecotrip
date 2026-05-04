package tn.esprit.session;

import tn.esprit.models.Auth_User.User;
import tn.esprit.models.enums.Role;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.Auth_User.FaceRecognition.FaceGate;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }
    private boolean faceEnrollmentNudgeShown = false;

    public boolean isFaceEnrollmentNudgeShown() {
        return faceEnrollmentNudgeShown;
    }

    public void markFaceEnrollmentNudgeShown() {
        this.faceEnrollmentNudgeShown = true;
    }
    // Add to SessionManager.java:
    private boolean openFacePanelOnLoad = false;

    public boolean shouldOpenFacePanelOnLoad() { return openFacePanelOnLoad; }
    public void requestOpenFacePanel()          { this.openFacePanelOnLoad = true; }
    public void clearOpenFacePanel()            { this.openFacePanelOnLoad = false; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public User getCurrentUser()          { return currentUser; }
    public boolean isLoggedIn()           { return currentUser != null; }
    public boolean isAdmin()              { return isLoggedIn() && currentUser.getRoles() == Role.ROLE_ADMIN; }
    public boolean isUser()               { return isLoggedIn() && currentUser.getRoles() == Role.ROLE_USER; }

//     After login, redirect to the right office based on role
    public void redirectAfterLogin() {
        if (isAdmin()) {
            FaceGate.GateResult result = FaceGate.check();
            switch (result) {
                case NOT_ENROLLED:
                    // No face registered yet → allow access + nudge shown by shell
                    SceneManager.navigateTo(Routes.ADMIN_MON_COMPTE);
                    break;

                case PASS:
                    // Enrolled + verified → allow
                    SceneManager.navigateTo(Routes.ADMIN_MON_COMPTE);
                    break;

                case FAIL:
                    // Enrolled but face didn't match → block, stay on current page
                    // Alert already shown inside FaceGate.showDialog() via statusLabel
                    // Do NOT call loadBackPage — just silently return
                    break;
            }
        } else {
            SceneManager.navigateTo(Routes.HOME);
        }
    }

    public void logout() {
        this.currentUser = null;
        SceneManager.navigateTo(Routes.LOGIN);
        this.faceEnrollmentNudgeShown = false;
    }

    private User pendingUser;       // for passing user between screens
    private String flashMessage;    // for success messages after navigation

    public void setPendingUser(User user)     { this.pendingUser = user; }
    public User getPendingUser()              { return pendingUser; }

    public void setFlashMessage(String msg)  { this.flashMessage = msg; }
    public String getFlashMessage()          { return flashMessage; }
    public void clearFlashMessage()          { this.flashMessage = null; }


}
