package tn.esprit.config;

public class EmailConfig {

    // ── Your Gmail credentials ──────────────────────────────────────────────
    // Use an App Password, NOT your real password.
    // Generate one at: https://myaccount.google.com/apppasswords
    // (Requires 2-Step Verification to be ON on your Google account)
    public static final String SMTP_HOST     = "smtp.gmail.com";
    public static final int    SMTP_PORT     = 587;
    public static final String FROM_EMAIL    = "yasminselmi582@gmail.com";
    public static final String FROM_NAME     = "EcoTrip";
    public static final String APP_PASSWORD  = "pofriixmnmsmlelo"; // 16-char app password

    // Token expires after 15 minutes — same as Symfony default
    public static final int    TOKEN_EXPIRY_MINUTES = 15;

    private EmailConfig() {}
}