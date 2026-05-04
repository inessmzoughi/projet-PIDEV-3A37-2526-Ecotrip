package tn.esprit.services.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.models.transport.Transport;
import tn.esprit.models.transport.TransportRecommendation;
import tn.esprit.models.transport.TransportRecommendationRequest;
import tn.esprit.utils.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GeminiTransportRecommendationService {

    private static final String SYSTEM_PROMPT = """
            You are an assistant that ranks transport options for eco-booking.
            Always respond with valid JSON only, with this exact array schema:
            [{"rank": 1, "type": "...", "price": "...", "justification": "..."}]
            Rules:
            - Return exactly 3 recommendations.
            - Use only transport types and prices from the given transport list.
            - Rank from best to third best in the returned order.
            - justification must be concise, maximum 2 sentences.
            - Capacity is a hard constraint: do not recommend options with insufficient capacity.
            - Respect budgetMin and budgetMax when provided. Both are per person.
            - Comfort level rules:
              - Basic: prioritize simple and economical options.
              - Standard: prioritize balanced options.
              - Premium: prioritize more comfortable and higher-quality options.
            - Preference rules:
              - Eco-friendly: lower CO2 emissions are best.
              - Cheapest: lower price per person is best.
              - Fastest: infer likely speed from type or category and justify briefly.
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;

    public GeminiTransportRecommendationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiUrl = AppConfig.getRequiredProperty("gemini.api.url");
        this.apiKey = AppConfig.getRequiredProperty("gemini.api.key");
    }

    public RecommendationResponse recommend(TransportRecommendationRequest request, List<Transport> transports) throws IOException, InterruptedException {
        List<Transport> availableTransports = transports.stream()
                .filter(Transport::isDisponible)
                .toList();

        if (availableTransports.isEmpty()) {
            return new RecommendationResponse(List.of(), "No available transport could be ranked right now.");
        }

        try {
            List<TransportRecommendation> recommendations = fetchGeminiRecommendations(request, availableTransports);
            if (!recommendations.isEmpty()) {
                return new RecommendationResponse(recommendations, null);
            }
        } catch (Exception e) {
            System.err.println("Gemini failed, fallback ranking will be used: " + e.getMessage());
        }

        return new RecommendationResponse(buildFallbackRecommendations(request, availableTransports),
                "Gemini is currently unavailable. Fallback ranking was used.");
    }

    private List<TransportRecommendation> fetchGeminiRecommendations(TransportRecommendationRequest request, List<Transport> transports)
            throws IOException, InterruptedException {
        Map<String, Object> userInput = new LinkedHashMap<>();
        userInput.put("origin", request.getOrigin());
        userInput.put("destination", request.getDestination());
        userInput.put("passengers", request.getPassengers());
        userInput.put("budgetMin", request.getBudgetMin());
        userInput.put("budgetMax", request.getBudgetMax());
        userInput.put("preference", request.getPreference());
        userInput.put("comfortLevel", request.getComfortLevel());

        Map<String, Object> promptBody = new LinkedHashMap<>();
        promptBody.put("user_input", userInput);
        promptBody.put("available_transports", transports.stream().map(this::toTransportPayload).toList());

        String prompt = objectMapper.writeValueAsString(promptBody);

        String payload = objectMapper.writeValueAsString(Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini request failed with status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new IOException("Gemini returned an empty response.");
        }

        String normalizedJson = stripCodeFences(textNode.asText());
        List<TransportRecommendation> recommendations = objectMapper.readValue(
                normalizedJson,
                new TypeReference<List<TransportRecommendation>>() { }
        );

        List<TransportRecommendation> normalized = new ArrayList<>();
        for (int i = 0; i < recommendations.size(); i++) {
            TransportRecommendation item = recommendations.get(i);
            if (item == null || item.getType() == null || item.getType().isBlank()
                    || item.getPrice() == null || item.getPrice().isBlank()
                    || item.getJustification() == null || item.getJustification().isBlank()) {
                continue;
            }

            normalized.add(new TransportRecommendation(
                    item.getRank() > 0 ? item.getRank() : i + 1,
                    item.getType().trim(),
                    item.getPrice().trim(),
                    item.getJustification().trim()
            ));
        }

        return normalized.stream()
                .sorted(Comparator.comparingInt(TransportRecommendation::getRank))
                .limit(3)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toTransportPayload(Transport transport) {
        return Map.of(
                "type", transport.getType(),
                "capacity", transport.getCapacite(),
                "emissionCo2", transport.getEmissionCo2(),
                "pricePerPerson", transport.getPrixParPersonne(),
                "category", transport.getCategory() == null ? "Uncategorized" : transport.getCategory().getName()
        );
    }

    private List<TransportRecommendation> buildFallbackRecommendations(TransportRecommendationRequest request, List<Transport> transports) {
        List<Transport> filtered = transports.stream()
                .filter(t -> t.getCapacite() >= request.getPassengers())
                .filter(t -> request.getBudgetMin() == null || t.getPrixParPersonne() >= request.getBudgetMin())
                .filter(t -> request.getBudgetMax() == null || t.getPrixParPersonne() <= request.getBudgetMax())
                .collect(Collectors.toCollection(ArrayList::new));

        if (filtered.isEmpty()) {
            filtered.addAll(transports);
        }

        filtered.sort((a, b) -> {
            int baseComparison = switch (normalize(request.getPreference())) {
                case "cheapest" -> Double.compare(a.getPrixParPersonne(), b.getPrixParPersonne());
                case "fastest" -> estimateSpeedRank(b) - estimateSpeedRank(a);
                default -> Double.compare(a.getEmissionCo2(), b.getEmissionCo2());
            };

            if (baseComparison != 0) {
                return baseComparison;
            }

            return switch (normalize(request.getComfortLevel())) {
                case "premium" -> comfortScore(b) - comfortScore(a);
                case "basic" -> comfortScore(a) - comfortScore(b);
                default -> Integer.compare(Math.abs(comfortScore(a) - 2), Math.abs(comfortScore(b) - 2));
            };
        });

        List<TransportRecommendation> recommendations = new ArrayList<>();
        Set<String> seenTypes = new LinkedHashSet<>();
        for (Transport transport : filtered) {
            if (!seenTypes.add(transport.getType().toLowerCase(Locale.ROOT))) {
                continue;
            }

            recommendations.add(new TransportRecommendation(
                    recommendations.size() + 1,
                    transport.getType(),
                    String.format(Locale.US, "%.2f DT / person", transport.getPrixParPersonne()),
                    buildFallbackJustification(request, transport)
            ));

            if (recommendations.size() == 3) {
                break;
            }
        }

        if (recommendations.size() < 3) {
            for (Transport transport : filtered) {
                boolean alreadyAdded = recommendations.stream()
                        .anyMatch(item -> item.getType().equalsIgnoreCase(transport.getType())
                                && item.getPrice().equalsIgnoreCase(String.format(Locale.US, "%.2f DT / person", transport.getPrixParPersonne())));
                if (alreadyAdded) {
                    continue;
                }

                recommendations.add(new TransportRecommendation(
                        recommendations.size() + 1,
                        transport.getType(),
                        String.format(Locale.US, "%.2f DT / person", transport.getPrixParPersonne()),
                        buildFallbackJustification(request, transport)
                ));

                if (recommendations.size() == 3) {
                    break;
                }
            }
        }

        return recommendations;
    }

    private String buildFallbackJustification(TransportRecommendationRequest request, Transport transport) {
        String preferenceReason = switch (normalize(request.getPreference())) {
            case "cheapest" -> "Competitive per-person pricing matches a budget-first preference.";
            case "fastest" -> "Its transport profile suggests a faster trip than most alternatives.";
            default -> "Its lower CO2 profile aligns well with an eco-friendly preference.";
        };

        String comfortReason = switch (normalize(request.getComfortLevel())) {
            case "premium" -> "It also fits a premium comfort expectation.";
            case "basic" -> "It keeps the experience simple and practical.";
            default -> "It offers a balanced comfort level for most trips.";
        };

        return preferenceReason + " " + comfortReason;
    }

    private int comfortScore(Transport transport) {
        String label = normalize(transport.getType() + " " + (transport.getCategory() == null ? "" : transport.getCategory().getName()));
        if (label.contains("vip") || label.contains("lux") || label.contains("premium")) {
            return 3;
        }
        if (label.contains("standard") || label.contains("comfort")) {
            return 2;
        }
        return 1;
    }

    private int estimateSpeedRank(Transport transport) {
        String label = normalize(transport.getType() + " " + (transport.getCategory() == null ? "" : transport.getCategory().getName()));
        if (label.contains("plane") || label.contains("avion")) {
            return 5;
        }
        if (label.contains("train") || label.contains("metro")) {
            return 4;
        }
        if (label.contains("taxi") || label.contains("car") || label.contains("voiture")) {
            return 3;
        }
        if (label.contains("van") || label.contains("minibus")) {
            return 2;
        }
        return 1;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String stripCodeFences(String rawText) {
        String cleaned = rawText.trim();
        if (cleaned.startsWith("```")) {
            int firstLineBreak = cleaned.indexOf('\n');
            if (firstLineBreak >= 0) {
                cleaned = cleaned.substring(firstLineBreak + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }
        return cleaned.trim();
    }

    public record RecommendationResponse(List<TransportRecommendation> recommendations, String warningMessage) {
    }
}
