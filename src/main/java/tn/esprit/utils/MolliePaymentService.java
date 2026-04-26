package tn.esprit.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MolliePaymentService {

    private static final Pattern ID_PATTERN           = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern STATUS_PATTERN        = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CHECKOUT_URL_PATTERN  = Pattern.compile("\"checkout\"\\s*:\\s*\\{.*?\"href\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
    private static final Pattern DETAIL_PATTERN        = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TITLE_PATTERN         = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public MolliePayment createPayment(String numeroCommande,
                                       String description,
                                       BigDecimal amount,
                                       String customerEmail) throws IOException, InterruptedException {

        // ← Ces lignes manquaient !
        String redirectUrl = MollieConfig.REDIRECT_URL + "?commande=" + urlToken(numeroCommande);
        String cancelUrl   = MollieConfig.CANCEL_URL   + "?commande=" + urlToken(numeroCommande);

        String payload = "{"
                + "\"amount\":{\"currency\":\"" + escapeJson(MollieConfig.MOLLIE_CURRENCY) + "\",\"value\":\"" + amount.toPlainString() + "\"},"
                + "\"description\":\"" + escapeJson(description) + "\","
                + "\"redirectUrl\":\"" + escapeJson(redirectUrl) + "\","
                + "\"cancelUrl\":\"" + escapeJson(cancelUrl) + "\","
                + "\"locale\":\"" + escapeJson(MollieConfig.CHECKOUT_LOCALE) + "\","
                // ← PAS DE "method" ici
                + "\"metadata\":{"
                + "\"commandeNumero\":\"" + escapeJson(numeroCommande) + "\","
                + "\"profileId\":\"" + escapeJson(MollieConfig.PROFILE_ID) + "\","
                + "\"customerEmail\":\"" + escapeJson(customerEmail == null ? "" : customerEmail.trim()) + "\""
                + "}"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MollieConfig.API_BASE_URL + "/payments"))
                .header("Authorization", "Bearer " + MollieConfig.API_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/hal+json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response);
        return parsePayment(response.body(), true);
    }

    public MolliePayment getPayment(String paymentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MollieConfig.API_BASE_URL + "/payments/" + paymentId))
                .header("Authorization", "Bearer " + MollieConfig.API_KEY)
                .header("Accept", "application/hal+json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response);
        return parsePayment(response.body(), false);
    }

    private void ensureSuccess(HttpResponse<String> response) throws IOException {
        int code = response.statusCode();
        if (code >= 200 && code < 300) return;
        String body   = response.body() == null ? "" : response.body();
        String detail = find(body, DETAIL_PATTERN);
        String title  = find(body, TITLE_PATTERN);
        throw new IOException((title == null ? "Erreur Mollie" : title)
                + (detail == null ? "" : " - " + detail)
                + " (HTTP " + code + ")");
    }

    private MolliePayment parsePayment(String body, boolean requireCheckoutUrl) throws IOException {
        String id          = find(body, ID_PATTERN);
        String status      = find(body, STATUS_PATTERN);
        String checkoutUrl = find(body, CHECKOUT_URL_PATTERN);
        if (id == null || status == null)
            throw new IOException("Réponse Mollie invalide : paiement introuvable.");
        if (requireCheckoutUrl && checkoutUrl == null)
            throw new IOException("Réponse Mollie invalide : checkoutUrl introuvable.");
        return new MolliePayment(id, checkoutUrl, status);
    }

    private String find(String body, Pattern pattern) {
        Matcher m = pattern.matcher(body);
        return m.find() ? unescapeJson(m.group(1)) : null;
    }

    private String escapeJson(String v) {
        return v.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");
    }

    private String unescapeJson(String v) {
        return v.replace("\\/","/").replace("\\\"","\"").replace("\\\\","\\")
                .replace("\\n","\n").replace("\\r","\r").replace("\\t","\t").replace("\\u0026","&");
    }

    private String urlToken(String v) { return v.replace(" ", "%20"); }
}