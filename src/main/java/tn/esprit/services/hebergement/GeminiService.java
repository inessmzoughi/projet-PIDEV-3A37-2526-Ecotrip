package tn.esprit.services.hebergement;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiService {

    /* ─── Config ─── */
    private static final String API_KEY = "AIzaSyC6hK-PSpg8dQVwuDPBnZNoNivVPoCvexI";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + "gemini-2.5-flash:generateContent?key=" + API_KEY;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /* ════════════════════════════════════════
       1. DESCRIPTION depuis NOM/VILLE/ÉTOILES
       ════════════════════════════════════════ */
    public String suggestDescription(String nom, String ville,
                                     int etoiles, String categorie)
            throws IOException, InterruptedException {

        String prompt =
                "Tu es un expert en tourisme tunisien. "
                        + "Genere une description commerciale attrayante EN FRANÇAIS "
                        + "pour cet hebergement. "
                        + "La description doit faire ENTRE 80 ET 120 MOTS exactement. "
                        + "Elle doit mentionner la ville, le standing, l ambiance et les atouts. "
                        + "Hebergement : "
                        + "Nom : " + nom + ", "
                        + "Ville : " + ville + ", "
                        + "Etoiles : " + etoiles + ", "
                        + "Categorie : " + categorie + ". "
                        + "Reponds UNIQUEMENT avec le texte de la description. "
                        + "Pas de titre, pas de guillemets, pas de JSON.";

        String raw = callGeminiText(prompt);
        return raw.length() > 500 ? raw.substring(0, 500) : raw;
    }

    /* ════════════════════════════════════════
       2. PRIX depuis NOM/VILLE/ÉTOILES
       ════════════════════════════════════════ */
    public int suggestPrix(String nom, String ville,
                           int etoiles, String categorie)
            throws IOException, InterruptedException {

        String prompt =
                "Tu es un expert en tourisme tunisien. "
                        + "Donne un prix par nuit en dinars tunisiens REALISTE "
                        + "selon le marche tunisien 2024. "
                        + "Fourchettes : "
                        + "1 etoile 40-80 DT, "
                        + "2 etoiles 80-150 DT, "
                        + "3 etoiles 150-250 DT, "
                        + "4 etoiles 250-400 DT, "
                        + "5 etoiles 400-700 DT. "
                        + "Hebergement : "
                        + "Nom : " + nom + ", "
                        + "Ville : " + ville + ", "
                        + "Etoiles : " + etoiles + ", "
                        + "Categorie : " + categorie + ". "
                        + "Reponds UNIQUEMENT avec un nombre entier. Exemple : 180. "
                        + "Pas de texte, pas de DT, juste le nombre.";

        String raw = callGeminiText(prompt).trim();
        try {
            return Integer.parseInt(raw.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    /* ════════════════════════════════════════
       3. DESCRIPTION depuis IMAGE (Vision)
       ════════════════════════════════════════ */
    public String suggestDescriptionFromImage(String imageUrl, String nom,
                                              String ville, int etoiles,
                                              String categorie)
            throws IOException, InterruptedException {

        // ── Étape 1 : télécharger l'image ──
        byte[] imageBytes = downloadImage(imageUrl);
        String mimeType   = detectMimeType(imageUrl);
        String base64     = java.util.Base64.getEncoder()
                .encodeToString(imageBytes);

        System.out.println("Image téléchargée : "
                + imageBytes.length + " bytes | type : " + mimeType);

        // ── Étape 2 : prompt sans caractères spéciaux ──
        String prompt =
                "Tu es un expert en tourisme tunisien. "
                        + "Regarde attentivement cette photo d hebergement. "
                        + "Genere une description commerciale attrayante EN FRANÇAIS "
                        + "entre 80 et 120 mots, en combinant ce que tu vois sur la photo "
                        + "avec ces informations : "
                        + "Nom : " + nom + ", "
                        + "Ville : " + ville + ", "
                        + "Etoiles : " + etoiles + ", "
                        + "Categorie : " + categorie + ". "
                        + "La description doit mentionner l ambiance visuelle, "
                        + "le standing, la ville et les points forts visibles sur la photo. "
                        + "Reponds UNIQUEMENT avec le texte de description. "
                        + "Pas de titre, pas de guillemets, pas de JSON.";

        // ── Étape 3 : appel Vision ──
        String raw = callGeminiVision(base64, mimeType, prompt);

        System.out.println("Vision réponse : " + raw);

        return raw.length() > 500 ? raw.substring(0, 500) : raw;
    }

    /* ════════════════════════════════════════
       APPELS HTTP
       ════════════════════════════════════════ */

    /* ─── Appel texte seul ─── */
    private String callGeminiText(String prompt)
            throws IOException, InterruptedException {

        String escaped = escapeJson(prompt);

        String body = "{"
                + "\"contents\":[{"
                + "\"parts\":[{\"text\":\"" + escaped + "\"}]"
                + "}],"
                + "\"generationConfig\":{"
                + "\"temperature\":0.7,"
                + "\"maxOutputTokens\":500"
                + "}"
                + "}";

        return sendRequest(body, 20);
    }

    private String callGeminiVision(String base64, String mimeType, String prompt)
            throws IOException, InterruptedException {

        String escaped = escapeJson(prompt);

        String body = "{"
                + "\"contents\":[{"
                + "\"parts\":["
                + "{"
                + "\"inline_data\":{"
                + "\"mime_type\":\"" + mimeType + "\","
                + "\"data\":\"" + base64 + "\""
                + "}"
                + "},"
                + "{"
                + "\"text\":\"" + escaped + "\""
                + "}"
                + "]"
                + "}],"
                + "\"generationConfig\":{"
                + "\"temperature\":0.7,"
                + "\"maxOutputTokens\":800"  // ✅ 800 au lieu de 500
                + "}"
                + "}";

        return sendRequest(body, 60);
    }

    private String sendRequest(String body, int timeoutSeconds)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Gemini HTTP status : " + response.statusCode());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API — HTTP "
                    + response.statusCode() + "\n" + response.body());
        }

        // ✅ HTTP 200 même si MAX_TOKENS → extraire le texte disponible
        return extractText(response.body());
    }

    /* ════════════════════════════════════════
       UTILITAIRES
       ════════════════════════════════════════ */

    /* ─── Télécharger image en bytes ─── */
    private byte[] downloadImage(String imageUrl)
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 Chrome/120.0.0.0")
                .header("Accept", "image/*,*/*")
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<byte[]> response =
                HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Image non accessible (HTTP " + response.statusCode() + "). "
                            + "Essayez une autre URL.");
        }

        byte[] bytes = response.body();

        if (bytes == null || bytes.length == 0) {
            throw new IOException("Image vide — URL invalide.");
        }

        return bytes;
    }

    /* ─── Détecter MIME depuis URL ─── */
    private String detectMimeType(String imageUrl) {
        String url = imageUrl.toLowerCase();
        if (url.contains(".png"))  return "image/png";
        if (url.contains(".webp")) return "image/webp";
        if (url.contains(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    /* ─── Échapper JSON sans apostrophes ni accents ─── */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", "")
                .replace("\t", " ");
    }

    private String extractText(String responseBody) {

        // ✅ Chercher les 2 variantes : avec et sans espace après :
        String key = "\"text\": \"";
        int start = responseBody.indexOf(key);

        // Si pas trouvé avec espace, essayer sans espace
        if (start == -1) {
            key = "\"text\":\"";
            start = responseBody.indexOf(key);
        }

        if (start == -1) {
            throw new IllegalStateException(
                    "Réponse Gemini inattendue : " + responseBody);
        }

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
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 6;
                            } catch (NumberFormatException e) {
                                sb.append('\\'); i++;
                            }
                        } else { sb.append('\\'); i++; }
                    }
                    default -> { sb.append('\\'); sb.append(next); i += 2; }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }

        String result = sb.toString().trim();

        if (result.isEmpty() && responseBody.contains("MAX_TOKENS")) {
            throw new IllegalStateException(
                    "Image trop lourde. Essayez une image plus légère.");
        }

        System.out.println("extractText (" + result.length() + " chars) : " + result);
        return result;
    }
}