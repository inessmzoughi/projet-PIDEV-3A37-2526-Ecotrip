package tn.esprit.services.activity;

import tn.esprit.models.activity.Activity;

import java.util.Locale;

public final class ActivityMapService {

    private ActivityMapService() {
    }

    public static boolean hasValidCoordinates(Activity activity) {
        if (activity == null) {
            return false;
        }
        return hasValidCoordinates(activity.getLatitude(), activity.getLongitude());
    }

    public static boolean hasValidCoordinates(String latitude, String longitude) {
        try {
            double lat = Double.parseDouble(latitude.trim());
            double lng = Double.parseDouble(longitude.trim());
            return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
        } catch (Exception exception) {
            return false;
        }
    }

    public static String buildOpenStreetMapUrl(String latitude, String longitude) {
        return String.format(Locale.US,
                "https://www.openstreetmap.org/?mlat=%s&mlon=%s#map=13/%s/%s",
                latitude.trim(),
                longitude.trim(),
                latitude.trim(),
                longitude.trim());
    }

    public static String buildMapHtml(String title, String location, String latitude, String longitude) {
        if (!hasValidCoordinates(latitude, longitude)) {
            return buildEmptyStateHtml(
                    "Localisation en attente",
                    "Ajoutez une latitude et une longitude valides pour afficher la carte."
            );
        }

        String safeTitle = escape(title == null || title.isBlank() ? "Activite" : title.trim());
        String safeLocation = escape(location == null || location.isBlank() ? "Tunisie" : location.trim());
        String safeLatitude = latitude.trim();
        String safeLongitude = longitude.trim();

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                  <style>
                    html, body { margin: 0; padding: 0; height: 100%%; overflow: hidden; background: #eef7ef; }
                    #map { height: 100%%; width: 100%%; border-radius: 16px; }
                    .leaflet-container { font-family: Arial, sans-serif; background: #e8f5e9; }
                    .map-badge {
                      position: absolute;
                      top: 14px;
                      left: 14px;
                      z-index: 999;
                      background: rgba(15, 61, 24, 0.92);
                      color: white;
                      padding: 8px 12px;
                      border-radius: 999px;
                      font-size: 12px;
                      font-weight: 700;
                      box-shadow: 0 8px 24px rgba(15, 61, 24, 0.18);
                    }
                  </style>
                </head>
                <body>
                  <div class="map-badge">EcoTrip map</div>
                  <div id="map"></div>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <script>
                    const lat = %s;
                    const lng = %s;
                    const map = L.map('map', { zoomControl: false }).setView([lat, lng], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      maxZoom: 19,
                      attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);
                    L.control.zoom({ position: 'bottomright' }).addTo(map);
                    const marker = L.marker([lat, lng]).addTo(map);
                    marker.bindPopup('<strong>%s</strong><br/>%s').openPopup();
                  </script>
                </body>
                </html>
                """.formatted(safeLatitude, safeLongitude, safeTitle, safeLocation);
    }

    public static String buildEmptyStateHtml(String title, String subtitle) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <style>
                    html, body {
                      margin: 0;
                      padding: 0;
                      height: 100%%;
                      overflow: hidden;
                      font-family: Arial, sans-serif;
                      background: linear-gradient(135deg, #eef7ef, #f8fafc);
                    }
                    body {
                      display: flex;
                      align-items: center;
                      justify-content: center;
                    }
                    .state {
                      width: calc(100%% - 32px);
                      margin: 16px;
                      padding: 22px;
                      border-radius: 18px;
                      background: rgba(255,255,255,0.86);
                      border: 1px solid #dbe7de;
                      box-shadow: 0 12px 28px rgba(45, 80, 22, 0.08);
                      text-align: center;
                    }
                    .title {
                      margin: 0 0 8px 0;
                      color: #1f3d25;
                      font-size: 18px;
                      font-weight: 700;
                    }
                    .subtitle {
                      margin: 0;
                      color: #64748b;
                      font-size: 13px;
                      line-height: 1.5;
                    }
                  </style>
                </head>
                <body>
                  <div class="state">
                    <p class="title">%s</p>
                    <p class="subtitle">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(escape(title), escape(subtitle));
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
