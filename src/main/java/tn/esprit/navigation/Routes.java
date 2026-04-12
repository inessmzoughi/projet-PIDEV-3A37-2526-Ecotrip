package tn.esprit.navigation;

public class Routes {
    // ── Public (no login required) ──────────────────────────
    public static final String LOGIN    = "login";
    public static final String REGISTER = "register";

    // ── Front Office (USER role) ─────────────────────────────
    public static final String HOME           = "home";
    public static final String ABOUT          = "about";
    public static final String HEBERGEMENTS   = "hebergements";
    public static final String ACTIVITES      = "activites";
    public static final String TRANSPORT      = "transport";
    public static final String BOUTIQUE       = "boutique";
    public static final String PANIER       = "panier";
    public static final String CONTACT        = "contact";
    public static final String MES_RESERVATIONS = "mes-reservations";

    public static final String FRONT_MON_COMPTE      = "front-mon-compte";
    public static final String FRONT_UPDATE_ACCOUNT  = "front-update-account";
    public static final String FRONT_CHANGE_PASSWORD = "front-change-password";
    //frontoffice produit
    public static final String FRONT_PRODUCTS = "front-products";


    // ── Back Office (ADMIN role) ─────────────────────────────
    //public static final String ADMIN_ADD_HEBERGEMENT     = "admin-add-hebergement";
    public static final String ADMIN_DASHBOARD    = "admin-dashboard";
    public static final String ADMIN_ACTIVITES    = "admin-activites";

    public static final String ADMIN_TRANSPORT    = "admin-transport";
    public static final String ADMIN_BOUTIQUE     = "admin-boutique";
    public static final String ADMIN_RESERVATIONS = "admin-reservations";

    public static final String ADMIN_USERS        = "admin-users";

    public static final String ADMIN_HEBERGEMENTS          = "admin-hebergements";
    public static final String ADMIN_CATEGORIES_HEBERGEMENT = "admin-categories-hebergement";
    public static final String ADMIN_CHAMBRES              = "admin-chambres";
    public static final String ADMIN_EQUIPEMENTS           = "admin-equipements";

    public static final String ADMIN_MON_COMPTE      = "admin-mon-compte";

    //backoffice Produit
    public static final String ADMIN_COMMANDE    = "admin-commande";
    public static final String ADMIN_LIGNE_COMMANDE   = "admin-ligne-commande";
    public static final String ADMIN_PAIEMENT    = "admin-paiement";
    public static final String ADMIN_PRODUCT    = "admin-product";
    public static final String ADMIN_PRODUCT_CATEGORY    = "admin-product-category";



}