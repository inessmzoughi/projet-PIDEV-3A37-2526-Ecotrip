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

public class AiDescriptionService {

    // Remplace par ta clé Gemini (gratuite sur https://aistudio.google.com)
    private static final String API_KEY = "AIzaSyCoGdQS1A234RtV-Zq7pXDqDIz8cBQLxN4";

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Genere une description pour une activite
    public static String generateActivityDescription(String title,
                                                     String location,
                                                     String price,
                                                     String durationMin) throws Exception {
        String prompt = String.format(
                "Tu es un redacteur pour une plateforme de tourisme eco-responsable.\n" +
                        "Redige une description attrayante en francais pour l'activite suivante :\n" +
                        "- Titre : %s\n- Lieu : %s\n- Prix : %s TND\n- Duree : %s minutes\n\n" +
                        "La description doit faire 2 a 3 phrases, etre engageante et mettre en valeur l'aspect ecologique. " +
                        "Reponds uniquement avec la description, sans titre ni introduction.",
                title, location, price, durationMin
        );
        return callApi(prompt);
    }

    // Genere une description pour une categorie d'activite
    public static String generateCategoryDescription(String categoryName) throws Exception {
        String prompt = String.format(
                "Tu es un redacteur pour une plateforme de tourisme eco-responsable.\n" +
                        "Redige une courte description en francais pour la categorie d'activite : \"%s\".\n\n" +
                        "La description doit faire 1 a 2 phrases, etre claire et donner envie de decouvrir cette categorie. " +
                        "Reponds uniquement avec la description, sans titre ni introduction.",
                categoryName
        );
        return callApi(prompt);
    }

    // Genere une bio pour un guide touristique
    public static String generateGuideBio(String firstName, String lastName) throws Exception {
        String prompt = String.format(
                "Tu es un redacteur pour une plateforme de tourisme eco-responsable.\n" +
                        "Redige une biographie professionnelle courte en francais pour un guide touristique " +
                        "qui s'appelle %s %s.\n\n" +
                        "La bio doit faire 2 a 3 phrases, mettre en avant la passion pour la nature et l'expertise locale. " +
                        "Reponds uniquement avec la bio, sans titre ni introduction.",
                firstName, lastName
        );
        return callApi(prompt);
    }

    // Appel HTTP a l'API Gemini
    private static String callApi(String userPrompt) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        ObjectNode part = parts.addObject();
        part.put("text", userPrompt);

        String requestBody = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + API_KEY))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur API Gemini (" + response.statusCode() + ") : "
                    + response.body());
        }

        // Format reponse Gemini : candidates[0].content.parts[0].text
        JsonNode json = mapper.readTree(response.body());
        JsonNode text = json
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text");

        return text.asText().trim();
    }
}
