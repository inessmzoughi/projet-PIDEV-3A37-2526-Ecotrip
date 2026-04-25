package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import tn.esprit.config.GoogleAuthConfig;
import tn.esprit.models.GoogleUserInfo;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GoogleAuthService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient   client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static HttpServer activeServer = null;

    private static synchronized void stopExistingServer() {
        if (activeServer != null) {
            try { activeServer.stop(0); } catch (Exception ignored) {}
            activeServer = null;
        }
    }

    /**
     * Find a free port starting from the preferred port.
     * Tries preferred port first, then scans up to preferred+10.
     */
    private int findFreePort(int preferred) {
        // First try the preferred port
        try (ServerSocket s = new ServerSocket(preferred)) {
            return preferred;
        } catch (Exception ignored) {}

        // Preferred is taken — try nearby ports
        for (int port = preferred + 1; port <= preferred + 10; port++) {
            try (ServerSocket s = new ServerSocket(port)) {
                return port;
            } catch (Exception ignored) {}
        }

        // Let OS assign any free port
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Cannot find a free port", e);
        }
    }

    public GoogleUserInfo authenticate() throws Exception {
        stopExistingServer();

        // Find a free port — use preferred if available, fallback otherwise
        int port = findFreePort(GoogleAuthConfig.CALLBACK_PORT);
        String redirectUri = "http://localhost:" + port + "/callback";

        String state = HexFormat.of().formatHex(
                SecureRandom.getInstanceStrong().generateSeed(16));

        String authUrl = GoogleAuthConfig.AUTH_URL
                + "?client_id="     + encode(GoogleAuthConfig.CLIENT_ID)
                + "&redirect_uri="  + encode(redirectUri)  // ← use dynamic redirectUri
                + "&response_type=" + encode("code")
                + "&scope="         + encode(GoogleAuthConfig.SCOPES)
                + "&state="         + encode(state)
                + "&access_type="   + encode("offline")
                + "&prompt="        + encode("select_account");

        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        // Start server on the free port we found
        HttpServer server = HttpServer.create(
                new InetSocketAddress(port), 0);
        activeServer = server;

        server.createContext("/callback", exchange -> {
            String query         = exchange.getRequestURI().getQuery();
            String code          = null;
            String receivedState = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2) {
                        if ("code".equals(kv[0]))  code          = kv[1];
                        if ("state".equals(kv[0])) receivedState = kv[1];
                    }
                }
            }

            String html  = buildSuccessHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }

            if (code != null && state.equals(receivedState)) {
                codeFuture.complete(code);
            } else if (!codeFuture.isDone()) {
                codeFuture.completeExceptionally(
                        new RuntimeException("State mismatch"));
            }
        });

        server.setExecutor(null);
        server.start();

        // Open browser
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(authUrl));
        } else {
            stopExistingServer();
            throw new RuntimeException("Impossible d'ouvrir le navigateur.");
        }

        try {
            // Exchange code for token — pass the same redirectUri used in auth URL
            String code        = codeFuture.get(3, TimeUnit.MINUTES);
            String accessToken = exchangeCodeForToken(code, redirectUri);
            return fetchUserInfo(accessToken);

        } catch (java.util.concurrent.TimeoutException e) {
            throw new java.util.concurrent.TimeoutException("Délai dépassé.");
        } finally {
            stopExistingServer();
        }
    }

    // ── Note: redirectUri is now passed as parameter ──────────────────────
    private String exchangeCodeForToken(String code,
                                        String redirectUri) throws Exception {
        String body = "code="         + encode(code)
                + "&client_id="           + encode(GoogleAuthConfig.CLIENT_ID)
                + "&client_secret="       + encode(GoogleAuthConfig.CLIENT_SECRET)
                + "&redirect_uri="        + encode(redirectUri)  // ← dynamic
                + "&grant_type="          + encode("authorization_code");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GoogleAuthConfig.TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new RuntimeException(
                    "Token exchange failed " + response.statusCode()
                            + ": " + response.body());

        JsonNode json = mapper.readTree(response.body());
        if (!json.has("access_token"))
            throw new RuntimeException("No access_token: " + response.body());

        return json.get("access_token").asText();
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GoogleAuthConfig.USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new RuntimeException("Userinfo failed: " + response.body());

        JsonNode json = mapper.readTree(response.body());

        GoogleUserInfo info = new GoogleUserInfo();
        info.setSub(json.has("sub")
                ? json.get("sub").asText() : "");
        info.setEmail(json.has("email")
                ? json.get("email").asText() : "");
        info.setName(json.has("name")
                ? json.get("name").asText() : "");
        info.setPicture(json.has("picture")
                ? json.get("picture").asText() : "");
        info.setEmailVerified(json.has("email_verified")
                && json.get("email_verified").asBoolean());
        return info;
    }

    private String encode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private String buildSuccessHtml() {
        return """
            <!DOCTYPE html><html>
            <head><meta charset="UTF-8"><title>EcoTrip</title>
            <style>
              body{font-family:'Segoe UI',Arial,sans-serif;background:#f0fdf4;
                   display:flex;align-items:center;justify-content:center;
                   min-height:100vh;margin:0;}
              .card{background:white;border-radius:16px;padding:48px 40px;
                    text-align:center;box-shadow:0 4px 24px rgba(0,0,0,.1);max-width:380px;}
              .icon{font-size:56px;margin-bottom:16px;}
              h1{color:#0d3d18;font-size:22px;margin:0 0 10px;}
              p{color:#475569;font-size:14px;line-height:1.6;}
              .brand{color:#1a5f2a;font-weight:bold;}
            </style></head>
            <body><div class="card">
              <div class="icon">✅</div>
              <h1>Connexion réussie !</h1>
              <p>Connecté à <span class="brand">EcoTrip</span>.<br>
                 Fermez cet onglet et revenez à l'application.</p>
            </div></body></html>
            """;
    }
}