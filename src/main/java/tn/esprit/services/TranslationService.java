package tn.esprit.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class TranslationService {

    // Langue source fixe : français (ta DB est en FR)
    private static final String SOURCE_LANG = "fr";

    public static String translate(String text, String targetLang) {
        // Si langue cible = source, on retourne directement
        if (targetLang.equals(SOURCE_LANG)) return text;

        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlStr = "https://api.mymemory.translated.net/get?q="
                    + encoded + "&langpair=" + SOURCE_LANG + "|" + targetLang;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            return json.getJSONObject("responseData").getString("translatedText");

        } catch (Exception e) {
            e.printStackTrace();
            return text; // fallback : texte original
        }
    }
}