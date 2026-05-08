package tn.esprit.services.hebergement;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiService {

    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        try (var in = GeminiService.class.getResourceAsStream("/config.properties")) {
            var props = new java.util.Properties();
            props.load(in);
            return props.getProperty("GEMINI_API_KEY_I");
        } catch (Exception e) {
            throw new RuntimeException("config.properties introuvable !", e);
        }
    }

    // Modèles gratuits classés du plus léger au plus lourd
    private static final String[] MODELS = {
            "gemini-2.0-flash-lite",  // le plus léger, rarement bloqué
            "gemini-1.5-flash-latest",
            "gemini-2.5-flash-lite",
            "gemini-2.5-flash"
    };

    private static final String BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /* ════════ 1. DESCRIPTION texte ════════ */
    public String suggestDescription(String nom, String ville,
                                     int etoiles, String categorie)
            throws IOException, InterruptedException {

        String prompt =
                "Tu es un expert en tourisme tunisien. "
                        + "Genere une description commerciale attrayante EN FRANÇAIS "
                        + "pour cet hebergement. "
                        + "La description doit faire ENTRE 80 ET 120 MOTS exactement. "
                        + "Elle doit mentionner la ville, le standing, l ambiance et les atouts. "
                        + "Hebergement : Nom : " + nom + ", Ville : " + ville
                        + ", Etoiles : " + etoiles + ", Categorie : " + categorie + ". "
                        + "Reponds UNIQUEMENT avec le texte de la description. "
                        + "Pas de titre, pas de guillemets, pas de JSON.";

        String raw = callWithFallback(buildTextBody(escapeJson(prompt)), 20);
        return raw.length() > 500 ? raw.substring(0, 500) : raw;
    }

    /* ════════ 2. PRIX ════════ */
    public int suggestPrix(String nom, String ville,
                           int etoiles, String categorie)
            throws IOException, InterruptedException {

        String prompt =
                "Tu es un expert en tourisme tunisien. "
                        + "Donne un prix par nuit en dinars tunisiens REALISTE "
                        + "selon le marche tunisien 2024. "
                        + "Fourchettes : 1 etoile 40-80 DT, 2 etoiles 80-150 DT, "
                        + "3 etoiles 150-250 DT, 4 etoiles 250-400 DT, 5 etoiles 400-700 DT. "
                        + "Hebergement : Nom : " + nom + ", Ville : " + ville
                        + ", Etoiles : " + etoiles + ", Categorie : " + categorie + ". "
                        + "Reponds UNIQUEMENT avec un nombre entier. Exemple : 180.";

        String raw = callWithFallback(buildTextBody(escapeJson(prompt)), 20).trim();
        try {
            return Integer.parseInt(raw.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    /* ════════ 3. DESCRIPTION depuis IMAGE ════════ */
    public String suggestDescriptionFromImage(String imageUrl, String nom,
                                              String ville, int etoiles,
                                              String categorie)
            throws IOException, InterruptedException {

        byte[] imageBytes = downloadImage(imageUrl);
        String mimeType = detectMimeType(imageUrl);
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

        System.out.println("Image téléchargée : " + imageBytes.length + " bytes | type : " + mimeType);

        String prompt =
                "Tu es un expert en tourisme tunisien. "
                        + "Regarde attentivement cette photo d hebergement. "
                        + "Genere une description commerciale attrayante EN FRANÇAIS "
                        + "entre 80 et 120 mots, en combinant ce que tu vois sur la photo "
                        + "avec ces informations : Nom : " + nom + ", Ville : " + ville
                        + ", Etoiles : " + etoiles + ", Categorie : " + categorie + ". "
                        + "Reponds UNIQUEMENT avec le texte de description. "
                        + "Pas de titre, pas de guillemets, pas de JSON.";

        String raw = callWithFallback(buildVisionBody(base64, mimeType, escapeJson(prompt)), 60);
        System.out.println("Vision réponse : " + raw);
        return raw.length() > 500 ? raw.substring(0, 500) : raw;
    }

    /* ════════════════════════════════════════════════
       FALLBACK : essaie chaque modèle, gère 429 + 503
       ════════════════════════════════════════════════ */
    private String callWithFallback(String body, int timeoutSeconds)
            throws IOException, InterruptedException {

        IOException lastError = null;

        for (String model : MODELS) {
            String url = BASE + model + ":generateContent?key=" + API_KEY;
            System.out.println("⏳ Essai modèle : " + model);

            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    String result = sendRequest(url, body, timeoutSeconds);
                    System.out.println("✅ Succès : " + model);
                    return result;

                } catch (IOException e) {
                    lastError = e;
                    String msg = e.getMessage() != null ? e.getMessage() : "";

                    boolean is429 = msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED");
                    boolean is503 = msg.contains("503") || msg.contains("UNAVAILABLE");

                    if (is429) {
                        // Quota dépassé sur ce modèle → passer au suivant immédiatement
                        System.out.println("⛔ 429 quota sur " + model + " → modèle suivant");
                        break;
                    } else if (is503 && attempt < 2) {
                        System.out.println("⚠️ 503 sur " + model + " → attente 3s...");
                        Thread.sleep(3000);
                    } else {
                        System.out.println("❌ Erreur " + model + " : " + msg);
                        break;
                    }
                }
            }
        }

        throw new IOException(
                "Tous les modèles Gemini sont indisponibles ou quota dépassé. "
                        + "Réessayez dans 1 minute.\n"
                        + (lastError != null ? lastError.getMessage() : ""),
                lastError
        );
    }

    /* ════════ BUILDERS JSON ════════ */

    private String buildTextBody(String escapedPrompt) {
        return "{"
                + "\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.7,\"maxOutputTokens\":500}"
                + "}";
    }

    private String buildVisionBody(String base64, String mimeType, String escapedPrompt) {
        return "{"
                + "\"contents\":[{\"parts\":["
                + "{\"inline_data\":{\"mime_type\":\"" + mimeType + "\","
                + "\"data\":\"" + base64 + "\"}},"
                + "{\"text\":\"" + escapedPrompt + "\"}"
                + "]}],"
                + "\"generationConfig\":{\"temperature\":0.7,\"maxOutputTokens\":800}"
                + "}";
    }

    /* ════════ HTTP ════════ */

    private String sendRequest(String url, String body, int timeoutSeconds)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Gemini HTTP status ["
                + url.split("/models/")[1].split(":")[0] + "] : "
                + response.statusCode());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API — HTTP "
                    + response.statusCode() + "\n" + response.body());
        }

        return extractText(response.body());
    }

    /* ════════ UTILITAIRES ════════ */

    private byte[] downloadImage(String imageUrl)
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept", "image/*,*/*")
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<byte[]> response =
                HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200)
            throw new IOException("Image non accessible (HTTP " + response.statusCode() + ").");

        byte[] bytes = response.body();
        if (bytes == null || bytes.length == 0)
            throw new IOException("Image vide — URL invalide.");

        return bytes;
    }

    private String detectMimeType(String imageUrl) {
        String url = imageUrl.toLowerCase();
        if (url.contains(".png"))  return "image/png";
        if (url.contains(".webp")) return "image/webp";
        if (url.contains(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", "")
                .replace("\t", " ");
    }

    private String extractText(String responseBody) {
        String key = "\"text\": \"";
        int start = responseBody.indexOf(key);
        if (start == -1) { key = "\"text\":\""; start = responseBody.indexOf(key); }
        if (start == -1)
            throw new IllegalStateException("Réponse Gemini inattendue : " + responseBody);

        start += key.length();
        StringBuilder sb = new StringBuilder();
        int i = start;

        while (i < responseBody.length()) {
            char c = responseBody.charAt(i);
            if (c == '\\' && i + 1 < responseBody.length()) {
                char next = responseBody.charAt(i + 1);
                switch (next) {
                    case '"'  -> { sb.append('"');  i += 2; }
                    case 'n'  -> { sb.append('\n'); i += 2; }
                    case 'r'  -> { sb.append('\r'); i += 2; }
                    case 't'  -> { sb.append('\t'); i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    case 'u'  -> {
                        if (i + 5 < responseBody.length()) {
                            String hex = responseBody.substring(i + 2, i + 6);
                            try { sb.append((char) Integer.parseInt(hex, 16)); i += 6; }
                            catch (NumberFormatException e) { sb.append('\\'); i++; }
                        } else { sb.append('\\'); i++; }
                    }
                    default -> { sb.append('\\'); sb.append(next); i += 2; }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c); i++;
            }
        }

        String result = sb.toString().trim();
        if (result.isEmpty() && responseBody.contains("MAX_TOKENS"))
            throw new IllegalStateException("Image trop lourde. Essayez une image plus légère.");

        System.out.println("extractText (" + result.length() + " chars) : " + result);
        return result;
    }
}