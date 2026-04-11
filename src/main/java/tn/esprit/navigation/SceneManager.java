package tn.esprit.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import tn.esprit.controller.layout.BackOfficeShellController;
import tn.esprit.controller.layout.FrontOfficeShellController;
import tn.esprit.session.SessionManager;

public class SceneManager {

    public enum ShellType { NONE, FRONT_OFFICE, BACK_OFFICE }

    private static Stage primaryStage;
    private static ShellType activeShell = ShellType.NONE;

    private static FrontOfficeShellController frontShellController;
    private static BackOfficeShellController  backShellController;

    // Public routes — no shell, no auth needed
    private static final Map<String, String> publicRoutes = new HashMap<>();

    // Front office routes — FrontOffice shell, USER role
    private static final Map<String, String> frontRoutes = new HashMap<>();

    // Back office routes — BackOffice shell, ADMIN role
    private static final Map<String, String> backRoutes = new HashMap<>();

    static {
        publicRoutes.put(Routes.LOGIN,    "/views/auth/login.fxml");
        publicRoutes.put(Routes.REGISTER, "/views/auth/register.fxml");

        frontRoutes.put(Routes.HOME,              "/views/front/home.fxml");
        frontRoutes.put(Routes.ABOUT,             "/views/front/about.fxml");
        frontRoutes.put(Routes.HEBERGEMENTS,      "/views/front/hebergements.fxml");
        frontRoutes.put(Routes.ACTIVITES,         "/views/front/activites.fxml");
        frontRoutes.put(Routes.TRANSPORT,         "/views/front/transport.fxml");
        frontRoutes.put(Routes.BOUTIQUE,          "/views/front/boutique.fxml");
        frontRoutes.put(Routes.CONTACT,           "/views/front/contact.fxml");
        frontRoutes.put(Routes.MES_RESERVATIONS,  "/views/front/mes-reservations.fxml");
        frontRoutes.put(Routes.FRONT_MON_COMPTE,      "/views/compte/mon-compte.fxml");
        frontRoutes.put(Routes.FRONT_UPDATE_ACCOUNT,  "/views/compte/update-account.fxml");
        frontRoutes.put(Routes.FRONT_CHANGE_PASSWORD, "/views/compte/change-password.fxml");
//*****************back
        backRoutes.put(Routes.ADMIN_DASHBOARD,    "/views/back/dashboard.fxml");
        backRoutes.put(Routes.ADMIN_ACTIVITES,    "/views/back/activites.fxml");
        backRoutes.put(Routes.ADMIN_TRANSPORT,    "/views/back/transport.fxml");
        backRoutes.put(Routes.ADMIN_BOUTIQUE,     "/views/back/boutique.fxml");
        backRoutes.put(Routes.ADMIN_RESERVATIONS, "/views/back/reservations.fxml");
        backRoutes.put(Routes.ADMIN_USERS,        "/views/back/users.fxml");
        backRoutes.put(Routes.ADMIN_HEBERGEMENTS,           "/views/back/hebergement/ListHebergements.fxml");
        backRoutes.put(Routes.ADMIN_CATEGORIES_HEBERGEMENT, "/views/back/hebergement/CategoriesHebergement.fxml");
        backRoutes.put(Routes.ADMIN_CHAMBRES,               "/views/back/hebergement/Chambres.fxml");
        backRoutes.put(Routes.ADMIN_EQUIPEMENTS,            "/views/back/hebergement/Equipements.fxml");
        backRoutes.put(Routes.ADMIN_MON_COMPTE,       "/views/compte/mon-compte.fxml");
        backRoutes.put(Routes.ADMIN_UPDATE_ACCOUNT,   "/views/compte/update-account.fxml");
        backRoutes.put(Routes.ADMIN_CHANGE_PASSWORD,  "/views/compte/change-password.fxml");
    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    public static void navigateTo(String routeName) {
        if (publicRoutes.containsKey(routeName)) {
            loadPublicPage(routeName);
        } else if (frontRoutes.containsKey(routeName)) {
            guardFrontOffice(routeName);
        } else if (backRoutes.containsKey(routeName)) {
            guardBackOffice(routeName);
        } else {
            throw new IllegalArgumentException("Unknown route: " + routeName);
        }
    }

//     ── Access guards ────────────────────────────────────────

    private static void guardFrontOffice(String routeName) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            loadPublicPage(Routes.LOGIN);
            return;
        }
//         Admins can view front office too (they're also users)
        loadFrontPage(routeName);
    }

    private static void guardBackOffice(String routeName) {
        if (!SessionManager.getInstance().isAdmin()) {
            // Not an admin → kick to log in or home
            if (SessionManager.getInstance().isLoggedIn()) {
                loadFrontPage(Routes.HOME); // logged in but wrong role
            } else {
                loadPublicPage(Routes.LOGIN);
            }
            return;
        }
        loadBackPage(routeName);
    }

    // ── Page loaders ─────────────────────────────────────────

    private static void loadPublicPage(String routeName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(publicRoutes.get(routeName))
            );
            Parent root = loader.load();
            applyScene(root, "auth.css");
            activeShell = ShellType.NONE;
            frontShellController = null;
            backShellController  = null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load public page: " + routeName, e);
        }
    }

    private static void loadFrontPage(String routeName) {
        try {
            // Load front shell only once
            if (activeShell != ShellType.FRONT_OFFICE || frontShellController == null) {
                FXMLLoader shellLoader = new FXMLLoader(
                        SceneManager.class.getResource("/views/layout/front-shell.fxml")
                );
                Parent shellRoot = shellLoader.load();
                frontShellController = shellLoader.getController();
                backShellController  = null;
                applyScene(shellRoot, "front.css");
                activeShell = ShellType.FRONT_OFFICE;
            }

            // Load and inject the page content
            FXMLLoader contentLoader = new FXMLLoader(
                    SceneManager.class.getResource(frontRoutes.get(routeName))
            );
            Parent content = contentLoader.load();
            frontShellController.loadContent(content, routeName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load front page: " + routeName, e);
        }
    }

    private static void loadBackPage(String routeName) {
        try {
            // Load back shell only once
            if (activeShell != ShellType.BACK_OFFICE || backShellController == null) {
                FXMLLoader shellLoader = new FXMLLoader(
                        SceneManager.class.getResource("/views/layout/back-shell.fxml")
                );
                Parent shellRoot = shellLoader.load();
                backShellController  = shellLoader.getController();
                frontShellController = null;
                applyScene(shellRoot, "back.css");
                activeShell = ShellType.BACK_OFFICE;
            }

            // Load and inject the page content
            FXMLLoader contentLoader = new FXMLLoader(
                    SceneManager.class.getResource(backRoutes.get(routeName))
            );
            Parent content = contentLoader.load();
            backShellController.loadContent(content, routeName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load back page: " + routeName, e);
        }
    }

    // ── Helper to navigate and get controller (for passing data) ──

    public static <T> T navigateToAndGetController(String routeName) {
        try {
            String path = frontRoutes.containsKey(routeName)
                    ? frontRoutes.get(routeName)
                    : backRoutes.get(routeName);

            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(path));
            Parent content = loader.load();

            if (frontRoutes.containsKey(routeName) && frontShellController != null) {
                frontShellController.loadContent(content, routeName);
            } else if (backRoutes.containsKey(routeName) && backShellController != null) {
                backShellController.loadContent(content, routeName);
            } else {
                navigateTo(routeName); // fallback — shell not ready yet
            }

            return loader.getController();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void applyScene(Parent root, String cssFile) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1280, 780);
        } else {
            scene.setRoot(root);
        }
        // Reset stylesheets and apply the right one for this shell
        scene.getStylesheets().clear();
        scene.getStylesheets().add(
                SceneManager.class.getResource("/styles/shared.css").toExternalForm()
        );
        scene.getStylesheets().add(
                SceneManager.class.getResource("/styles/" + cssFile).toExternalForm()
        );
        primaryStage.setScene(scene);
    }
}