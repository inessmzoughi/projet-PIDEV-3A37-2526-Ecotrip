package tn.esprit.services;

import tn.esprit.utils.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TwilioService {

    private static final String API_URL_TEMPLATE = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";

    private final HttpClient httpClient;

    public TwilioService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendSms(String driverName, String transportName) {
        try {
            String accountSid = AppConfig.getRequiredProperty("TWILIO_ACCOUNT_SID");
            String authToken = AppConfig.getRequiredProperty("TWILIO_AUTH_TOKEN");
            String from = AppConfig.getRequiredProperty("TWILIO_FROM");
            String to = AppConfig.getRequiredProperty("TWILIO_TO");

            String safeDriverName = isBlank(driverName) ? "Unknown driver" : driverName.trim();
            String safeTransportName = isBlank(transportName) ? "unknown transport" : transportName.trim();
            String message = "New driver " + safeDriverName + " has been assigned to transport " + safeTransportName + ".";

            String requestBody = "From=" + encode(from)
                    + "&To=" + encode(to)
                    + "&Body=" + encode(message);

            String authHeader = Base64.getEncoder()
                    .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL_TEMPLATE.formatted(accountSid)))
                    .header("Authorization", "Basic " + authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Twilio SMS sent successfully for driver assignment.");
            } else {
                System.err.println("Twilio SMS failed with status " + response.statusCode() + ": " + response.body());
            }
        } catch (IllegalStateException e) {
            System.err.println("Twilio SMS skipped: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("Twilio SMS failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Twilio SMS failed unexpectedly: " + e.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
