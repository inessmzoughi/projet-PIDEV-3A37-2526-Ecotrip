package tn.esprit.session;

import tn.esprit.models.User;
import tn.esprit.models.enums.Role;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setCurrentUser(User user) { this.currentUser = user; }
    public User getCurrentUser()          { return currentUser; }
    public boolean isLoggedIn()           { return currentUser != null; }
    public boolean isAdmin()              { return isLoggedIn() && currentUser.getRoles() == Role.ROLE_ADMIN; }
    public boolean isUser()               { return isLoggedIn() && currentUser.getRoles() == Role.ROLE_USER; }

//     After login, redirect to the right office based on role
    public void redirectAfterLogin() {
        if (isAdmin()) {
            SceneManager.navigateTo(Routes.ADMIN_DASHBOARD);
        } else {
            SceneManager.navigateTo(Routes.HOME);
        }
    }

    public void logout() {
        this.currentUser = null;
        SceneManager.navigateTo(Routes.LOGIN);
    }

    private User pendingUser;       // for passing user between screens
    private String flashMessage;    // for success messages after navigation

    public void setPendingUser(User user)     { this.pendingUser = user; }
    public User getPendingUser()              { return pendingUser; }

    public void setFlashMessage(String msg)  { this.flashMessage = msg; }
    public String getFlashMessage()          { return flashMessage; }
    public void clearFlashMessage()          { this.flashMessage = null; }


}
