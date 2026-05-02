package tn.esprit.services.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import tn.esprit.models.activity.Activity;

public class AiDescriptionService {

    private static final String API_KEY_ENV = "GEMINI_API_KEY";
    private static final String API_KEY_PROPERTY = "gemini.api.key";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private AiDescriptionService() {
    }

    public static String generateActivityDescription(String title,
                                                     String location,
                                                     String price,
                                                     String durationMin) throws Exception {
        String safeTitle = clean(title, "cette activite");
        String safeLocation = clean(location, "Tunisie");
        String safePrice = clean(price, "sur demande");
        String safeDuration = clean(durationMin, "60");

        String prompt = String.format(
                "Tu es un redacteur pour une plateforme de tourisme eco-responsable.%n" +
                        "Redige une description attrayante en francais pour l'activite suivante :%n" +
                        "- Titre : %s%n- Lieu : %s%n- Prix : %s TND%n- Duree : %s minutes%n%n" +
                        "La description doit faire 2 a 3 phrases, etre engageante et mettre en valeur l'aspect ecologique. " +
                        "Reponds uniquement avec la description, sans titre ni introduction.",
                safeTitle, safeLocation, safePrice, safeDuration
        );

        String fallback = String.format(
                "%s vous invite a decouvrir %s dans une experience pensee pour les voyageurs en quete d'authenticite. " +
                        "Entre immersion locale, rythme accessible et approche eco-responsable, cette activite propose un moment nature aussi memorable que respectueux de l'environnement.",
                capitalize(safeTitle), safeLocation
        );

        return generateText(prompt, fallback);
    }

    public static String generateCategoryDescription(String categoryName) throws Exception {
        String safeCategory = clean(categoryName, "cette categorie");

        String prompt = String.format(
                "Tu es un redacteur pour une plateforme de tourisme eco-responsable.%n" +
                        "Redige une courte description en francais pour la categorie d'activite : \"%s\".%n%n" +
                        "La description doit faire 1 a 2 phrases, etre claire et donner envie de decouvrir cette categorie. " +
                        "Reponds uniquement avec la description, sans titre ni introduction.",
                safeCategory
        );

        String fallback = String.format(
                "%s rassemble des experiences immersives qui mettent en valeur le patrimoine local, la nature et un tourisme plus responsable.",
                capitalize(safeCategory)
        );

        return generateText(prompt, fallback);
    }

    public static String generateGuideBio(String firstName, String lastName) throws Exception {
        String safeFirstName = clean(firstName, "Ce");
        String safeLastName = clean(lastName, "guide");

        String prompt = String.format(
                "Tu es un redacteur pour une plateforme de tourisme eco-responsable.%n" +
                        "Redige une biographie professionnelle courte en francais pour un guide touristique " +
                        "qui s'appelle %s %s.%n%n" +
                        "La bio doit faire 2 a 3 phrases, mettre en avant la passion pour la nature et l'expertise locale. " +
                        "Reponds uniquement avec la bio, sans titre ni introduction.",
                safeFirstName, safeLastName
        );

        String fallback = String.format(
                "%s %s accompagne les voyageurs avec une approche chaleureuse, une excellente connaissance du terrain et une vraie sensibilite aux experiences durables. " +
                        "Sa passion pour la nature et le patrimoine local permet de creer des sorties authentiques, rassurantes et enrichissantes.",
                capitalize(safeFirstName), capitalize(safeLastName)
        );

        return generateText(prompt, fallback);
    }

    public static boolean isAiConfigured() {
        return !resolveApiKey().isBlank();
    }

    public static String generateActivitySuggestionReply(String userPrompt,
                                                         List<Activity> suggestions,
                                                         String fallback) throws Exception {
        if (suggestions == null || suggestions.isEmpty()) {
            return fallback;
        }

        String promptText = clean(userPrompt, "une experience eco-responsable en Tunisie");

        StringBuilder activitiesContext = new StringBuilder();
        for (int index = 0; index < suggestions.size(); index++) {
            Activity activity = suggestions.get(index);
            activitiesContext.append(index + 1)
                    .append(". ")
                    .append(clean(activity.getTitle(), "Activite"))
                    .append(" | lieu: ")
                    .append(clean(activity.getLocation(), "Tunisie"))
                    .append(" | categorie: ")
                    .append(activity.getCategory() != null ? clean(activity.getCategory().getName(), "nature") : "nature")
                    .append(" | prix: ")
                    .append(String.format(Locale.US, "%.0f", activity.getPrice()))
                    .append(" TND | duree: ")
                    .append(activity.getDurationMinutes())
                    .append(" min | description: ")
                    .append(clean(activity.getDescription(), "Experience eco-responsable"))
                    .append(System.lineSeparator());
        }

        String prompt = String.format(
                "Tu es un assistant voyage premium pour une plateforme d'activites eco-responsables.%n" +
                        "Un utilisateur cherche une suggestion pour : %s%n%n" +
                        "Voici les activites disponibles a recommander :%n%s%n" +
                        "Redige une reponse en francais, chaleureuse et concise (4 a 6 phrases maximum). " +
                        "Mets surtout en avant la meilleure activite, cite aussi 1 ou 2 alternatives si elles sont pertinentes, " +
                        "et insiste sur le lieu, l'ambiance, le type d'experience et ce qui peut plaire a l'utilisateur. " +
                        "Reponds uniquement avec le texte de conseil, sans liste numérotée ni markdown.",
                promptText,
                activitiesContext
        );

        return generateText(prompt, fallback);
    }

    private static String generateText(String prompt, String fallback) throws Exception {
        String apiKey = resolveApiKey();
        if (apiKey.isBlank()) {
            return fallback;
        }

        try {
            String response = callApi(prompt, apiKey);
            return response.isBlank() ? fallback : response;
        } catch (Exception exception) {
            System.err.println("Gemini unavailable, fallback generated instead: " + exception.getMessage());
            return fallback;
        }
    }

    private static String callApi(String userPrompt, String apiKey) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        ObjectNode part = parts.addObject();
        part.put("text", userPrompt);

        String requestBody = MAPPER.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + apiKey))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur API Gemini (" + response.statusCode() + ") : " + response.body());
        }

        JsonNode json = MAPPER.readTree(response.body());
        JsonNode textNode = json.path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text");

        return textNode.asText("").trim();
    }

    private static String resolveApiKey() {
        String propertyKey = System.getProperty(API_KEY_PROPERTY, "").trim();
        if (!propertyKey.isEmpty()) {
            return propertyKey;
        }

        String envKey = System.getenv(API_KEY_ENV);
        return envKey == null ? "" : envKey.trim();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() == 1) {
            return value.toUpperCase();
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
