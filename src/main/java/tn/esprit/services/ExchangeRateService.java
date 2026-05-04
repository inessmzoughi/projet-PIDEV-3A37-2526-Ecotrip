package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class ExchangeRateService {

    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/TND";
    private static final Duration CACHE_DURATION = Duration.ofHours(1);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile CachedRates cachedRates;

    public ExchangeRateService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String convertFromTND(double amountTND) {
        PriceDisplay display = buildPriceDisplay(amountTND);
        return display.secondaryLine().isBlank()
                ? display.primaryLine()
                : display.primaryLine() + " | " + display.secondaryLine();
    }

    public PriceDisplay buildPriceDisplay(double amountTND) {
        String primaryLine = String.format(Locale.US, "%.2f DT / personne", amountTND);

        try {
            CachedRates rates = getRates();
            if (rates == null) {
                return new PriceDisplay(primaryLine, "");
            }

            double amountEur = amountTND * rates.eurRate();
            double amountUsd = amountTND * rates.usdRate();
            String secondaryLine = String.format(Locale.US, "\u20AC %.2f  |  $ %.2f", amountEur, amountUsd);
            return new PriceDisplay(primaryLine, secondaryLine);
        } catch (Exception e) {
            System.err.println("Exchange rate conversion failed: " + e.getMessage());
            return new PriceDisplay(primaryLine, "");
        }
    }

    private CachedRates getRates() throws IOException, InterruptedException {
        CachedRates current = cachedRates;
        if (current != null && !current.isExpired()) {
            return current;
        }

        synchronized (this) {
            current = cachedRates;
            if (current != null && !current.isExpired()) {
                return current;
            }

            CachedRates refreshed = fetchRates();
            if (refreshed != null) {
                cachedRates = refreshed;
            }
            return cachedRates;
        }
    }

    private CachedRates fetchRates() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Exchange rate request failed with status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode ratesNode = root.path("rates");
        if (ratesNode.isMissingNode()) {
            throw new IOException("Exchange rate response does not contain rates.");
        }

        JsonNode eurNode = ratesNode.get("EUR");
        JsonNode usdNode = ratesNode.get("USD");
        if (eurNode == null || usdNode == null || !eurNode.isNumber() || !usdNode.isNumber()) {
            throw new IOException("Exchange rate response is missing EUR or USD.");
        }

        return new CachedRates(eurNode.asDouble(), usdNode.asDouble(), Instant.now().plus(CACHE_DURATION));
    }

    private record CachedRates(double eurRate, double usdRate, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record PriceDisplay(String primaryLine, String secondaryLine) {
    }
}
