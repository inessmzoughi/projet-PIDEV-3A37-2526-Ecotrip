package tn.esprit.services.activity;

import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivityWeatherSnapshot;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityWeatherService {

    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,is_day&timezone=auto";

    public ActivityWeatherSnapshot fetchCurrentWeather(Activity activity) {
        if (!ActivityMapService.hasValidCoordinates(activity)) {
            throw new IllegalArgumentException("Coordonnees invalides pour recuperer la meteo.");
        }

        try {
            String endpoint = String.format(
                    Locale.US,
                    WEATHER_URL,
                    Double.parseDouble(activity.getLatitude()),
                    Double.parseDouble(activity.getLongitude())
            );

            HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("API meteo indisponible (" + responseCode + ")");
            }

            try (InputStream stream = connection.getInputStream()) {
                String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                ActivityWeatherSnapshot snapshot = new ActivityWeatherSnapshot();
                snapshot.setTemperatureCelsius(extractDouble(response, "temperature_2m"));
                snapshot.setApparentTemperatureCelsius(extractDouble(response, "apparent_temperature"));
                snapshot.setWindSpeedKmh(extractDouble(response, "wind_speed_10m"));
                snapshot.setPrecipitationMm(extractDouble(response, "precipitation"));
                snapshot.setWeatherCode((int) extractDouble(response, "weather_code"));
                snapshot.setDay(extractDouble(response, "is_day") >= 1);
                snapshot.setConditionLabel(mapWeatherCode(snapshot.getWeatherCode(), snapshot.isDay()));
                snapshot.setSuitabilityLabel(resolveSuitability(activity, snapshot));
                snapshot.setAdvisoryText(resolveAdvisory(activity, snapshot));
                return snapshot;
            } finally {
                connection.disconnect();
            }
        } catch (Exception exception) {
            throw new RuntimeException("Impossible de recuperer la meteo de l'activite.", exception);
        }
    }

    private String resolveSuitability(Activity activity, ActivityWeatherSnapshot snapshot) {
        boolean outdoor = isOutdoorActivity(activity);

        if (snapshot.getPrecipitationMm() >= 3 || snapshot.getWindSpeedKmh() >= 35 || snapshot.getApparentTemperatureCelsius() >= 40) {
            return outdoor ? "Conditions peu favorables" : "Confort a surveiller";
        }
        if (snapshot.getPrecipitationMm() >= 0.5 || snapshot.getWindSpeedKmh() >= 20 || snapshot.getApparentTemperatureCelsius() >= 33) {
            return outdoor ? "Meteo a surveiller" : "Conditions variables";
        }
        return outdoor ? "Meteo ideale" : "Confortable";
    }

    private String resolveAdvisory(Activity activity, ActivityWeatherSnapshot snapshot) {
        boolean outdoor = isOutdoorActivity(activity);

        if (snapshot.getPrecipitationMm() >= 3) {
            return outdoor
                    ? "La pluie annoncee peut reduire le confort de cette experience en plein air."
                    : "Des precipitations sont prevues, mais l'activite reste praticable selon le programme.";
        }
        if (snapshot.getWindSpeedKmh() >= 35) {
            return outdoor
                    ? "Le vent est soutenu. Une verification du depart est conseillee avant reservation."
                    : "Le vent est marque autour du site aujourd'hui.";
        }
        if (snapshot.getApparentTemperatureCelsius() >= 40) {
            return "La sensation thermique est elevee. Privilegiez l'hydratation et les departs adaptes.";
        }
        if (snapshot.getApparentTemperatureCelsius() <= 8) {
            return "L'air est frais. Une tenue adaptee est recommandee pour profiter de l'experience.";
        }
        return outdoor
                ? "Les conditions actuelles sont favorables pour une sortie EcoTrip."
                : "Les conditions actuelles sont agreables pour cette activite.";
    }

    private boolean isOutdoorActivity(Activity activity) {
        String text = (safe(activity.getTitle()) + " " + safe(activity.getDescription()) + " " + safe(activity.getLocation())
                + " " + safe(activity.getCategory() != null ? activity.getCategory().getName() : ""))
                .toLowerCase(Locale.ROOT);

        return text.contains("nature")
                || text.contains("aventure")
                || text.contains("camp")
                || text.contains("kayak")
                || text.contains("randon")
                || text.contains("plage")
                || text.contains("desert")
                || text.contains("velo")
                || text.contains("excursion")
                || text.contains("catamaran")
                || text.contains("surf");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double extractDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0;
    }

    private String mapWeatherCode(int code, boolean day) {
        return switch (code) {
            case 0 -> day ? "Ciel degage" : "Nuit degagee";
            case 1, 2 -> "Peu nuageux";
            case 3 -> "Couvert";
            case 45, 48 -> "Brume";
            case 51, 53, 55, 56, 57 -> "Bruine";
            case 61, 63, 65, 66, 67 -> "Pluie";
            case 71, 73, 75, 77 -> "Neige";
            case 80, 81, 82 -> "Averses";
            case 85, 86 -> "Averses de neige";
            case 95 -> "Orage";
            case 96, 99 -> "Orage avec grele";
            default -> "Conditions variables";
        };
    }
}
