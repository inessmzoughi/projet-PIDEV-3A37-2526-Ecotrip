package tn.esprit.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MollieConfig {

    private static final BigDecimal DEFAULT_TEST_DT_TO_EUR_RATE = new BigDecimal("0.2930");

    public static final String API_BASE_URL       = "https://api.mollie.com/v2";
    public static final String API_KEY            = "test_djSuxkpxBMdaBHu3vUpEy8nQf3SDvx"; // ← ta clé de l'image
    public static final String PROFILE_ID         = "pfl_2gyqozK2NJ";                       // ← ton profile ID
    public static final String PROFILE_WEBSITE    = "https://www.example.org";
    public static final String REDIRECT_URL       = PROFILE_WEBSITE + "/mollie-return";
    public static final String CANCEL_URL         = PROFILE_WEBSITE + "/mollie-cancel";
    public static final String CHECKOUT_LOCALE    = "fr_FR";
    public static final String STORE_CURRENCY_LABEL = "TND";
    public static final String MOLLIE_CURRENCY    = "EUR";
    public static final String STORE_TO_MOLLIE_RATE_ENV = "MOLLIE_TND_TO_EUR_RATE";

    private MollieConfig() {}

    public static BigDecimal convertStoreAmountToMollie(BigDecimal storeAmount) {
        if (storeAmount == null)
            throw new IllegalArgumentException("Le montant à convertir est obligatoire.");

        if (STORE_CURRENCY_LABEL.equalsIgnoreCase(MOLLIE_CURRENCY))
            return normalizeAmount(storeAmount);

        String envValue = System.getenv(STORE_TO_MOLLIE_RATE_ENV);
        if (envValue == null || envValue.isBlank()) {
            if (isTestMode())
                return normalizeAmount(storeAmount.multiply(DEFAULT_TEST_DT_TO_EUR_RATE));
            throw new IllegalStateException(
                    "Mollie ne supporte pas " + STORE_CURRENCY_LABEL + ". "
                            + "Configurez la variable d'environnement " + STORE_TO_MOLLIE_RATE_ENV + ".");
        }

        try {
            BigDecimal rate = new BigDecimal(envValue.trim());
            if (rate.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalStateException("Le taux doit être strictement positif.");
            return normalizeAmount(storeAmount.multiply(rate));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Le taux " + STORE_TO_MOLLIE_RATE_ENV + " est invalide.", e);
        }
    }

    public static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean isTestMode() {
        return API_KEY != null && API_KEY.startsWith("test_");
    }
}
