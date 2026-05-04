package tn.esprit.services.Auth_User.FaceRecognition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.config.HuggingFaceConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class HuggingFaceFaceService {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Java 11 built-in HTTP client — no OkHttp needed
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Send image bytes to HuggingFace API.
     * Returns 768-dim float[] embedding vector.
     * Mirrors PHP: getEmbeddingFromImage(string $imageBytes): array
     */
    public float[] getEmbeddingFromImage(byte[] imageBytes) throws Exception {
        // Detect MIME type from magic bytes
        // Mirrors PHP: (new \finfo(FILEINFO_MIME_TYPE))->buffer($imageBytes)
        String mimeType = detectMimeType(imageBytes);

        // Build data URL — mirrors PHP:
        // $dataUrl = 'data:' . $mimeType . ';base64,' . base64_encode($imageBytes)
        String base64  = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;

        // Build JSON body — mirrors PHP: 'json' => ['inputs' => $dataUrl]
        String requestBody = mapper.writeValueAsString(
                Map.of("inputs", dataUrl)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HuggingFaceConfig.API_URL))
                .header("Authorization", "Bearer " + HuggingFaceConfig.API_TOKEN)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "HuggingFace API error " + response.statusCode()
                            + ": " + response.body()
            );
        }

        JsonNode json = mapper.readTree(response.body());
        return parseEmbedding(json);
    }

    /**
     * Cosine similarity between two embedding vectors.
     * Exact port of PHP cosineSimilarity().
     * Range: -1 to 1. Values above threshold = same face.
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length)
            throw new IllegalArgumentException(
                    "Vector length mismatch: " + a.length + " vs " + b.length);

        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 0.0 : dot / denom;
    }

    /** Returns true if two embeddings belong to the same face */
    public static boolean isSameFace(float[] stored, float[] live) {
        return cosineSimilarity(stored, live)
                >= HuggingFaceConfig.SIMILARITY_THRESHOLD;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parse all known HuggingFace response shapes.
     * Exact port of PHP parseEmbedding().
     *
     * Known shapes from ViT:
     *   [[float, float, …]]    ← most common (ViT default)
     *   [float, float, …]      ← flat array
     *   {"embedding": [...]}
     *   {"image_embeds": [...]}
     */
    private float[] parseEmbedding(JsonNode body) {
        // {"embedding": [...]}
        if (body.has("embedding") && body.get("embedding").isArray())
            return toFloatArray(body.get("embedding"));

        // {"image_embeds": [...]} or {"image_embeds": [[...]]}
        if (body.has("image_embeds")) {
            JsonNode emb = body.get("image_embeds");
            if (emb.isArray() && emb.size() > 0 && emb.get(0).isArray())
                return toFloatArray(emb.get(0));
            return toFloatArray(emb);
        }

        // [[float, float, …]] ← ViT default
        if (body.isArray() && body.size() > 0 && body.get(0).isArray()) {
            JsonNode inner = body.get(0);
            // Handle [[[float,…]]] — 3 levels deep
            if (inner.size() > 0 && inner.get(0).isArray())
                inner = inner.get(0);
            return toFloatArray(inner);
        }

        // [float, float, …] ← flat
        if (body.isArray() && body.size() > 0 && body.get(0).isNumber())
            return toFloatArray(body);

        throw new RuntimeException(
                "Unexpected HuggingFace response format: " + body
        );
    }

    private float[] toFloatArray(JsonNode arrayNode) {
        float[] result = new float[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++)
            result[i] = (float) arrayNode.get(i).asDouble();
        return result;
    }

    /**
     * Detect image MIME type from magic bytes.
     * Mirrors PHP: (new \finfo(FILEINFO_MIME_TYPE))->buffer($bytes)
     */
    private String detectMimeType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF)
            return "image/jpeg";

        if (bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G')
            return "image/png";

        if (bytes.length >= 4
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F')
            return "image/webp";

        return "image/jpeg"; // safe default — JPEG from webcam
    }
}