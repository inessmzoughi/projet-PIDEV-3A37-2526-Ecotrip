package tn.esprit.services.hebergement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
public class  AvisModeratorService {

    // ─── Endpoint Anthropic (pas de clé : géré par le proxy claude.ai) ───────
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-sonnet-4-20250514";

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static AvisModeratorService instance;
    public static AvisModeratorService getInstance() {
        if (instance == null) instance = new AvisModeratorService();
        return instance;
    }
    private AvisModeratorService() {}

    // ─── Résultat de modération ───────────────────────────────────────────────
    public enum Decision { APPROUVE, REJETE, EN_ATTENTE }

    public record ModerationResult(Decision decision, String reason) {}

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

        try {
            String prompt = buildPrompt(texte);
            String responseJson = callClaude(prompt);
            return parseDecision(responseJson);
        } catch (Exception e) {
            System.err.println("[AvisModerator] Erreur API : " + e.getMessage());
            // En cas d'erreur réseau, on laisse l'admin décider
            return new ModerationResult(Decision.EN_ATTENTE,
                    "Service de modération indisponible – envoyé en attente.");
        }
    }

    // ─── Construction du prompt ───────────────────────────────────────────────
    private String buildPrompt(String texte) {
        return """
                Tu es un modérateur de contenu pour un site de tourisme éco-responsable en Tunisie.
                Ton rôle est d'analyser un avis client et de décider s'il doit être publié.
                
                Règles :
                - APPROUVE si : l'avis est un retour honnête sur un hébergement (positif ou négatif),
                  rédigé de façon correcte, sans insultes, sans spam, sans données personnelles.
                - REJETE si : l'avis contient des insultes, discours haineux, spam, contenu sexuel,
                  menaces, données personnelles (emails, téléphones), ou est totalement hors sujet.
                
                Réponds UNIQUEMENT avec ce JSON (sans markdown, sans explication autour) :
                {"decision":"APPROUVE","raison":"<explication courte en français, max 15 mots>"}
                ou
                {"decision":"REJETE","raison":"<explication courte en français, max 15 mots>"}
                
                Avis à analyser :
                """ + texte;
    }

    // ─── Appel HTTP à l'API Anthropic ─────────────────────────────────────────
    private String callClaude(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Corps de la requête JSON (assemblé manuellement pour éviter une dépendance)
        String escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", " ");

        String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"max_tokens\":150,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapedPrompt + "\"}]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("HTTP " + response.statusCode() + " : " + response.body());
        }
        return response.body();
    }

    // ─── Parsing de la réponse Claude ────────────────────────────────────────
    /**
     * Extrait le JSON {"decision":"...","raison":"..."} de la réponse Anthropic.
     * La réponse de l'API a la forme :
     * {"content":[{"type":"text","text":"{\"decision\":\"APPROUVE\",...}"}], ...}
     */
    private ModerationResult parseDecision(String apiResponse) {
        try {
            // Extraire le champ "text" de la réponse Anthropic
            String text = extractJsonValue(apiResponse, "text");
            if (text == null) {
                System.err.println("[AvisModerator] Réponse inattendue : " + apiResponse);
                return new ModerationResult(Decision.EN_ATTENTE, "Réponse IA invalide.");
            }

            // Décoder les escapes JSON (\n, \", etc.)
            text = text.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            // Extraire decision et raison du JSON retourné par Claude
            String decision = extractJsonValue(text, "decision");
            String raison   = extractJsonValue(text, "raison");

            if ("APPROUVE".equalsIgnoreCase(decision))
                return new ModerationResult(Decision.APPROUVE,
                        raison != null ? raison : "Contenu acceptable.");
            if ("REJETE".equalsIgnoreCase(decision))
                return new ModerationResult(Decision.REJETE,
                        raison != null ? raison : "Contenu inapproprié.");

            // Valeur inconnue → fallback
            return new ModerationResult(Decision.EN_ATTENTE, "Décision IA non reconnue.");

        } catch (Exception e) {
            System.err.println("[AvisModerator] Erreur parsing : " + e.getMessage());
            return new ModerationResult(Decision.EN_ATTENTE, "Erreur analyse IA.");
        }
    }

    // ─── Utilitaire extraction JSON simple ───────────────────────────────────
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        // Trouver la fermeture en ignorant les \" échappés
        int i = start;
        while (i < json.length()) {
            if (json.charAt(i) == '\\') { i += 2; continue; }
            if (json.charAt(i) == '"')  break;
            i++;
        }
        return (i <= json.length()) ? json.substring(start, i) : null;
    }
}