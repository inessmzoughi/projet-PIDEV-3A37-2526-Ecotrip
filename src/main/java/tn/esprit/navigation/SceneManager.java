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

    private static final Map<String, String> publicRoutes = new HashMap<>();
    private static final Map<String, String> frontRoutes  = new HashMap<>();
    private static final Map<String, String> backRoutes   = new HashMap<>();

    static {
        publicRoutes.put(Routes.LOGIN,    "/views/auth/login.fxml");
        publicRoutes.put(Routes.REGISTER, "/views/auth/register.fxml");

        frontRoutes.put(Routes.HOME,              "/views/front/home.fxml");
        frontRoutes.put(Routes.ABOUT,             "/views/front/about.fxml");
        frontRoutes.put(Routes.HEBERGEMENTS, "/views/front/hebergements.fxml");
        frontRoutes.put(Routes.ACTIVITES,         "/views/front/activities.fxml");
        frontRoutes.put(Routes.TRANSPORT,         "/views/front/transport.fxml");
        frontRoutes.put(Routes.FRONT_PRODUCTS,       "/views/front/Products.fxml");
        frontRoutes.put(Routes.FRONT_PRODUCT_DETAIL, "/views/front/Productdetail.fxml");
        frontRoutes.put(Routes.CONTACT, "/views/front/Cart.fxml");
        frontRoutes.put(Routes.MES_RESERVATIONS,  "/views/front/mes-reservations.fxml");
        frontRoutes.put(Routes.FRONT_MON_COMPTE,  "/views/compte/mon-compte.fxml");
        frontRoutes.put(Routes.MES_FAVORIS,       "/views/front/mes-favoris.fxml");
        frontRoutes.put(Routes.HEBERGEMENT_DETAIL, "/views/front/HebergementDetail.fxml");
//*****************back
        backRoutes.put(Routes.ADMIN_DASHBOARD,    "/views/back/dashboard.fxml");
        backRoutes.put(Routes.ADMIN_ACTIVITES,    "/views/back/activites.fxml");
        backRoutes.put(Routes.ADMIN_TRANSPORT,    "/views/back/transport/Transports.fxml");
        backRoutes.put(Routes.ADMIN_CHAUFFEURS,   "/views/back/transport/Chauffeurs.fxml");
        backRoutes.put(Routes.ADMIN_TRANSPORT_CATEGORIES, "/views/back/transport/TransportCategories.fxml");
        backRoutes.put(Routes.ADMIN_RESERVATIONS, "/views/back/reservation/ListReservations.fxml");

        backRoutes.put(Routes.ADMIN_USERS,        "/views/back/user/User.fxml");

        backRoutes.put(Routes.ADMIN_HEBERGEMENTS,           "/views/back/hebergement/ListHebergements.fxml");
        backRoutes.put(Routes.ADMIN_CATEGORIES_HEBERGEMENT, "/views/back/hebergement/CategoriesHebergement.fxml");
        backRoutes.put(Routes.ADMIN_CHAMBRES,               "/views/back/hebergement/Chambres.fxml");
        backRoutes.put(Routes.ADMIN_EQUIPEMENTS,            "/views/back/hebergement/Equipements.fxml");
        backRoutes.put(Routes.ADMIN_ModerationAvis,           "/views/back/hebergement/ModerationAvis.fxml");

        backRoutes.put(Routes.ADMIN_MON_COMPTE,       "/views/compte/mon-compte.fxml");
        //back produit*
        backRoutes.put(Routes.ADMIN_COMMANDE,  "/views/back/produit/Commande.fxml");
        backRoutes.put(Routes.ADMIN_LIGNE_COMMANDE,  "/views/back/produit/LigneCommande.fxml");
        backRoutes.put(Routes.ADMIN_PAIEMENT,  "/views/back/produit/Payment.fxml");
        backRoutes.put(Routes.ADMIN_PRODUCT,  "/views/back/produit/Product.fxml");
        backRoutes.put(Routes.ADMIN_PRODUCT_CATEGORY,  "/views/back/produit/ProductCategory.fxml");

        backRoutes.put(Routes.ADMIN_ACTIVITIES,          "/views/back/activity/ListActivities.fxml");
        backRoutes.put(Routes.ADMIN_ACTIVITY_CATEGORIES, "/views/back/activity/Categories.fxml");
        backRoutes.put(Routes.ADMIN_GUIDES,              "/views/back/activity/Guides.fxml");
        backRoutes.put(Routes.ADMIN_SCHEDULES,           "/views/back/activity/Schedules.fxml");

    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    // ─── Navigation principale ────────────────────────────────────────────────

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

    private static void guardFrontOffice(String routeName) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            loadPublicPage(Routes.LOGIN);
            return;
        }
        loadFrontPage(routeName);
    }

    private static void guardBackOffice(String routeName) {
        if (!SessionManager.getInstance().isAdmin()) {
            if (SessionManager.getInstance().isLoggedIn()) {
                loadFrontPage(Routes.HOME);
            } else {
                loadPublicPage(Routes.LOGIN);
            }
            return;
        }
        loadBackPage(routeName);
    }

    // ─── Loaders internes ─────────────────────────────────────────────────────

    private static void loadPublicPage(String routeName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(publicRoutes.get(routeName)));
            Parent root = loader.load();
            applyScene(root, "auth.css");
            activeShell          = ShellType.NONE;
            frontShellController = null;
            backShellController  = null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load public page: " + routeName, e);
        }
    }

    private static void loadFrontPage(String routeName) {
        try {
            ensureFrontShell();
            FXMLLoader contentLoader = new FXMLLoader(
                    SceneManager.class.getResource(frontRoutes.get(routeName)));
            Parent content = contentLoader.load();
            frontShellController.loadContent(content, routeName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load front page: " + routeName, e);
        }
    }

    private static void loadBackPage(String routeName) {
        try {
            ensureBackShell();
            FXMLLoader contentLoader = new FXMLLoader(
                    SceneManager.class.getResource(backRoutes.get(routeName)));
            Parent content = contentLoader.load();
            backShellController.loadContent(content, routeName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load back page: " + routeName, e);
        }
    }

    // ─── Ensure shells ────────────────────────────────────────────────────────

    /**
     * Crée (ou réutilise) le front shell avec sa navbar.
     * C'est la SEULE source de vérité pour frontShellController.
     * Appelé aussi bien par loadFrontPage que par navigateToAndGetController.
     */
    private static void ensureFrontShell() throws IOException {
        if (activeShell != ShellType.FRONT_OFFICE || frontShellController == null) {
            FXMLLoader shellLoader = new FXMLLoader(
                    SceneManager.class.getResource("/views/layout/front-shell.fxml"));
            Parent shellRoot     = shellLoader.load();
            frontShellController = shellLoader.getController();
            backShellController  = null;
            applyScene(shellRoot, "front.css");
            activeShell = ShellType.FRONT_OFFICE;
        }
    }

    private static void ensureBackShell() throws IOException {
        if (activeShell != ShellType.BACK_OFFICE || backShellController == null) {
            FXMLLoader shellLoader = new FXMLLoader(
                    SceneManager.class.getResource("/views/layout/back-shell.fxml"));
            Parent shellRoot    = shellLoader.load();
            backShellController  = shellLoader.getController();
            frontShellController = null;
            applyScene(shellRoot, "back.css");
            activeShell = ShellType.BACK_OFFICE;
        }
    }

    // ─── navigateToAndGetController ───────────────────────────────────────────

    /**
     * Navigue vers une route front/back ET retourne le contrôleur du contenu.
     *
     * CORRECTIF NAVBAR : ensureFrontShell() est appelé EN PREMIER, ce qui
     * garantit que le BorderPane shell (top=navbar, center=content) est dans
     * la scène AVANT d'injecter le contenu dans le contentArea.
     *
     * Ancien bug : le shell était parfois rechargé après le contenu, ou
     * frontShellController était null, donc le contenu remplaçait tout le
     * BorderPane au lieu de juste le center → pas de navbar.
     *
     * Usage :
     *   ProductDetailController ctrl =
     *       SceneManager.navigateToAndGetController(Routes.FRONT_PRODUCT_DETAIL);
     *   ctrl.initData(product, allProducts);
     */
    @SuppressWarnings("unchecked")
    public static <T> T navigateToAndGetController(String routeName) {
        try {
            final String fxmlPath;

            if (frontRoutes.containsKey(routeName)) {
                // ① Shell + navbar dans la scène en premier
                ensureFrontShell();
                fxmlPath = frontRoutes.get(routeName);

            } else if (backRoutes.containsKey(routeName)) {
                ensureBackShell();
                fxmlPath = backRoutes.get(routeName);

            } else {
                throw new IllegalArgumentException("Unknown route: " + routeName);
            }

            // ② Charge le FXML du contenu uniquement (pas le shell)
            FXMLLoader loader  = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent     content = loader.load();
            T          ctrl    = loader.getController();

            // ③ Injecte dans contentArea — la navbar est déjà en place
            if (frontRoutes.containsKey(routeName) && frontShellController != null) {
                frontShellController.loadContent(content, routeName);
            } else if (backRoutes.containsKey(routeName) && backShellController != null) {
                backShellController.loadContent(content, routeName);
            }

            return ctrl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to navigate to: " + routeName, e);
        }
    }

    // ─── applyScene ──────────────────────────────────────────────────────────

    private static void applyScene(Parent root, String cssFile) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1280, 780);
        } else {
            scene.setRoot(root);
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(
                SceneManager.class.getResource("/styles/shared.css").toExternalForm());
        scene.getStylesheets().add(
                SceneManager.class.getResource("/styles/" + cssFile).toExternalForm());
        if ("back.css".equals(cssFile)) {
            scene.getStylesheets().add(
                    SceneManager.class.getResource("/styles/ecotrip-activity.css").toExternalForm());
        }
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}