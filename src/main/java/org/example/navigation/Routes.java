package org.example.navigation;

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
    public static final String CONTACT        = "contact";
    public static final String MES_RESERVATIONS = "mes-reservations";
    public static final String MON_COMPTE     = "mon-compte";

    // ── Back Office (ADMIN role) ─────────────────────────────
    public static final String ADMIN_ADD_HEBERGEMENT     = "admin-add-hebergement";
    public static final String ADMIN_DASHBOARD    = "admin-dashboard";
    public static final String ADMIN_ACTIVITES    = "admin-activites";
    public static final String ADMIN_HEBERGEMENTS = "admin-hebergements";
    public static final String ADMIN_TRANSPORT    = "admin-transport";
    public static final String ADMIN_BOUTIQUE     = "admin-boutique";
    public static final String ADMIN_RESERVATIONS = "admin-reservations";
    public static final String ADMIN_USERS        = "admin-users";
    public static final String ADMIN_MON_COMPTE   = "admin-mon-compte";
}
