package tn.esprit.services.hebergement;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiService {

    private static final String API_KEY = "AIzaSyC6hK-PSpg8dQVwuDPBnZNoNivVPoCvexI";

    // ✅ Même modèle que ton projet Symfony
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + "gemini-2.5-flash:generateContent?key=" + API_KEY;

    // ✅ Limite de caractères pour la description
    private static final int MAX_DESC_LENGTH = 150;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String suggestDescription(String nom, String ville,
                                     int etoiles, String categorie)
            throws IOException, InterruptedException {
        String raw = callGemini(buildDescriptionPrompt(nom, ville, etoiles, categorie));
        return raw.length() > 500 ? raw.substring(0, 500) : raw;
    }

    public int suggestPrix(String nom, String ville,
                           int etoiles, String categorie)
            throws IOException, InterruptedException {
        String raw = callGemini(buildPrixPrompt(nom, ville, etoiles, categorie)).trim();
        try {
            return Integer.parseInt(raw.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    /* ─── Appel HTTP commun ─── */
    private String callGemini(String prompt)
            throws IOException, InterruptedException {

        String escaped = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");

        String body = "{"
                + "\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}],"
                + "\"generationConfig\":{"
                + "  \"temperature\":0.7,"
                + "  \"maxOutputTokens\":300"
                + "}"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API — HTTP "
                    + response.statusCode() + "\n" + response.body());
        }

        return extractText(response.body());
    }

    /* ─── Parser la réponse Gemini ─── */
    private String extractText(String responseBody) {
        // Structure : candidates[0].content.parts[0].text
        String key = "\"text\":";
        int idx = responseBody.indexOf(key);
        if (idx == -1) throw new IllegalStateException(
                "Réponse Gemini inattendue : " + responseBody);

        int start = responseBody.indexOf('"', idx + key.length()) + 1;
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < responseBody.length()) {
            char c = responseBody.charAt(i);
            if (c == '\\' && i + 1 < responseBody.length()) {
                char next = responseBody.charAt(i + 1);
                if      (next == '"')  { sb.append('"');  i += 2; }
                else if (next == 'n')  { sb.append('\n'); i += 2; }
                else if (next == '\\') { sb.append('\\'); i += 2; }
                else                   { sb.append(c);    i++; }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString().trim();
    }
    private String buildDescriptionPrompt(String nom, String ville,
                                          int etoiles, String categorie) {
        return "Tu es un expert en tourisme tunisien. "
                + "Génère une description commerciale attrayante EN FRANÇAIS "
                + "pour cet hébergement. "
                + "La description doit faire ENTRE 80 ET 120 MOTS exactement. "
                + "Elle doit mentionner la ville, le standing, l'ambiance et les atouts. "
                + "Hébergement :\n"
                + "- Nom : " + nom + "\n"
                + "- Ville : " + ville + "\n"
                + "- Étoiles : " + etoiles + "\n"
                + "- Catégorie : " + categorie + "\n\n"
                + "Réponds UNIQUEMENT avec le texte de la description. "
                + "Pas de titre, pas de guillemets, pas de JSON.";
    }private String buildPrixPrompt(String nom, String ville,
                                    int etoiles, String categorie) {
        return "Tu es un expert en tourisme tunisien. "
                + "Donne un prix par nuit en dinars tunisiens (DT) "
                + "RÉALISTE selon le marché tunisien actuel 2024.\n"
                + "Fourchettes de référence du marché tunisien :\n"
                + "- 1 étoile : 40 à 80 DT\n"
                + "- 2 étoiles : 80 à 150 DT\n"
                + "- 3 étoiles : 150 à 250 DT\n"
                + "- 4 étoiles : 250 à 400 DT\n"
                + "- 5 étoiles : 400 à 700 DT\n\n"
                + "Hébergement :\n"
                + "- Nom : " + nom + "\n"
                + "- Ville : " + ville + "\n"
                + "- Étoiles : " + etoiles + "\n"
                + "- Catégorie : " + categorie + "\n\n"
                + "Réponds UNIQUEMENT avec un nombre entier. Exemple : 180\n"
                + "Pas de texte, pas de DT, juste le nombre.";
    }
}