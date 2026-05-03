package tn.esprit.services.hebergement;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Service de modération automatique des avis via HuggingFace Inference API.
 *
 * Modèle : facebook/bart-large-mnli  (classification zero-shot, gratuit)
 * Endpoint: https://api-inference.huggingface.co/models/facebook/bart-large-mnli
 *
 * Configuration dans config.properties :
 *   moderation.api.url=https://api-inference.huggingface.co/models/facebook/bart-large-mnli
 *   moderation.api.key=           ← optionnel, HuggingFace token
 *   moderation.api.timeout=10     ← secondes
 */
public class AvisModeratorService {

    // ─── Résultat de modération ───────────────────────────────────────────────
    public enum Decision { APPROUVE, REJETE, EN_ATTENTE }

    public record ModerationResult(Decision decision, String reason) {}

    // ─── Config chargée depuis config.properties ──────────────────────────────
    private final String apiUrl;
    private final String apiKey;
    private final int    timeoutSec;

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static AvisModeratorService instance;

    public static AvisModeratorService getInstance() {
        if (instance == null) instance = new AvisModeratorService();
        return instance;
    }

    private AvisModeratorService() {
        Properties props = new Properties();
        try (InputStream in =
                     getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            System.err.println("[AvisModerator] Impossible de lire config.properties : "
                    + e.getMessage());
        }

        apiUrl     = props.getProperty("moderation.api.url",
                "https://api-inference.huggingface.co/models/facebook/bart-large-mnli");
        apiKey     = props.getProperty("moderation.api.key", "").trim();
        timeoutSec = parseInt(props.getProperty("moderation.api.timeout", "10"), 10);
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Analyse un texte d'avis et retourne la décision de modération.
     *
     * @param texte Le commentaire soumis par l'utilisateur
     * @return ModerationResult avec la décision et une explication courte
     */
    public ModerationResult moderer(String texte) {
        if (texte == null || texte.isBlank())
            return new ModerationResult(Decision.REJETE, "Commentaire vide.");

        // ── Pré-filtre local rapide (insultes évidentes) ──────────────────────
        ModerationResult localCheck = localFilter(texte);
        if (localCheck != null) return localCheck;

        // ── Appel API HuggingFace ─────────────────────────────────────────────
        try {
            String responseJson = callHuggingFace(texte);
            return parseHuggingFaceResponse(responseJson);
        } catch (Exception e) {
            System.err.println("[AvisModerator] Erreur API HuggingFace : " + e.getMessage());
            // Fallback : règles locales étendues si l'API est indisponible
            return localFallback(texte);
        }
    }

    // ─── Pré-filtre local (évite un appel API pour les cas évidents) ──────────
    private ModerationResult localFilter(String texte) {
        String lower = texte.toLowerCase();
        String[] motsCles = {
                "connard", "salaud", "idiot", "con ", " con,", "merde", "putain",
                "enculé", "fdp", "nique", "bâtard", "pute", "fuck", "shit", "asshole",
                "spam", "viagra", "casino", "http://", "https://",
                "@gmail", "@yahoo", "@hotmail", "whatsapp", "telegram"
        };
        for (String mot : motsCles) {
            if (lower.contains(mot))
                return new ModerationResult(Decision.REJETE,
                        "Contenu inapproprié ou spam détecté.");
        }
        return null;
    }

    // ─── Fallback sans API ────────────────────────────────────────────────────
    private ModerationResult localFallback(String texte) {
        // Si le filtre local n'a rien trouvé, on approuve par défaut
        // (l'avis passe en attente uniquement si l'API est requise pour certitude)
        if (texte.length() < 10)
            return new ModerationResult(Decision.EN_ATTENTE,
                    "Commentaire trop court – vérification manuelle.");
        return new ModerationResult(Decision.EN_ATTENTE,
                "Service de modération indisponible – envoyé en attente.");
    }

    // ─── Appel HTTP HuggingFace Inference API ─────────────────────────────────
    /**
     * Requête zero-shot classification :
     * On soumet le texte avec deux labels candidats :
     *   "appropriate review" et "inappropriate content"
     * HuggingFace renvoie un score de confiance pour chaque label.
     */
    private String callHuggingFace(String texte) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSec))
                .build();

        // Escape du texte pour JSON
        String escaped = texte
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String body = "{"
                + "\"inputs\":\"" + escaped + "\","
                + "\"parameters\":{"
                + "\"candidate_labels\":[\"appropriate hotel review\",\"inappropriate content\"]"
                + "}"
                + "}";

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSec))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        // Ajout du token HuggingFace si configuré
        if (!apiKey.isEmpty())
            reqBuilder.header("Authorization", "Bearer " + apiKey);

        HttpResponse<String> response = client.send(
                reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status != 200)
            throw new Exception("HTTP " + status + " : " + response.body());

        System.out.println("[AvisModerator] Réponse brute : " + response.body());
        return response.body();
    }

    // ─── Parsing de la réponse HuggingFace ───────────────────────────────────
    /**
     * Réponse attendue :
     * {
     *   "sequence": "...",
     *   "labels":  ["appropriate hotel review", "inappropriate content"],
     *   "scores":  [0.92, 0.08]
     * }
     * On compare les scores : si "appropriate" > 0.60 → APPROUVE, sinon REJETE.
     */
    private ModerationResult parseHuggingFaceResponse(String json) {
        try {
            double scoreAppropriate = -1;
            String[] entries = json.split("\\},\\s*\\{");
            for (String entry : entries) {
                if (entry.contains("appropriate hotel review")) {
                    int idx = entry.indexOf("\"score\":");
                    if (idx != -1) {
                        String scoreStr = entry.substring(idx + 8)
                                .replaceAll("[^0-9.]", "")
                                .replaceAll("(\\d+\\.?\\d*).*", "$1");
                        scoreAppropriate = Double.parseDouble(scoreStr);
                    }
                }
            }

            if (scoreAppropriate < 0)
                return new ModerationResult(Decision.EN_ATTENTE, "Réponse IA incomplète.");

            System.out.printf("[AvisModerator] Score approprié : %.2f%n", scoreAppropriate);

            if (scoreAppropriate >= 0.60)
                return new ModerationResult(Decision.APPROUVE,
                        String.format("Avis jugé approprié (%.0f%%)", scoreAppropriate * 100));
            else if (scoreAppropriate >= 0.35)
                return new ModerationResult(Decision.EN_ATTENTE,
                        "Score ambigu – vérification manuelle recommandée.");
            else
                return new ModerationResult(Decision.REJETE,
                        String.format("Contenu jugé inapproprié (%.0f%% de confiance)",
                                (1 - scoreAppropriate) * 100));

        } catch (Exception e) {
            System.err.println("[AvisModerator] Erreur parsing : " + e.getMessage());
            return new ModerationResult(Decision.EN_ATTENTE, "Erreur analyse IA.");
        }
    }



    // ─── Utilitaire ──────────────────────────────────────────────────────────
    private int parseInt(String val, int defaultVal) {
        try { return Integer.parseInt(val.trim()); }
        catch (Exception e) { return defaultVal; }
    }
}