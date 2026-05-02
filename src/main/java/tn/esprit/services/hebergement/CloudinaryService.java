package tn.esprit.services.hebergement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class CloudinaryService {

    private static CloudinaryService instance;
    private final Cloudinary cloudinary;

    // Chargement en une seule fois
    private static final String MEMBER = loadMember();
    private static final String CLOUD_NAME = loadProperty("cloudinary.cloud.name_" + MEMBER);
    private static final String API_KEY    = loadProperty("cloudinary.api.key_" + MEMBER);
    private static final String API_SECRET = loadProperty("cloudinary.api.secret_" + MEMBER);

    // Une seule méthode de chargement réutilisable
    private static String loadMember() {
        return loadProperty("MEMBER");
    }

    private static String loadProperty(String key) {
        try (var in = CloudinaryService.class.getResourceAsStream("/config.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty(key);
        } catch (Exception e) {
            throw new RuntimeException("config.properties introuvable ! clé : " + key, e);
        }
    }

    private CloudinaryService() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME,
                "api_key",    API_KEY,
                "api_secret", API_SECRET,
                "secure",     true
        ));
    }

    private static String loadCloudName() {
        try (var in = CloudinaryService.class.getResourceAsStream("/config.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("cloudinary.cloud.name");
        } catch (Exception e) {
            throw new RuntimeException("config.properties introuvable !", e);
        }
    }

    private static String loadApiKey() {
        try (var in = CloudinaryService.class.getResourceAsStream("/config.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("cloudinary.api.key");
        } catch (Exception e) {
            throw new RuntimeException("config.properties introuvable !", e);
        }
    }

    private static String loadApiSecret() {
        try (var in = CloudinaryService.class.getResourceAsStream("/config.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("cloudinary.api.secret");
        } catch (Exception e) {
            throw new RuntimeException("config.properties introuvable !", e);
        }
    }

    public static CloudinaryService getInstance() {
        if (instance == null) {
            instance = new CloudinaryService();
        }
        return instance;
    }

    /** Upload image avis client → retourne URL publique */
    public String uploadAvisImage(File fichier) throws Exception {
        Map result = cloudinary.uploader().upload(
                fichier,
                ObjectUtils.asMap("folder", "avis_clients")
        );
        return (String) result.get("secure_url");
    }

    /** Upload image hébergement → retourne URL publique */
    public String uploadHebergementImage(File fichier) throws Exception {
        Map result = cloudinary.uploader().upload(
                fichier,
                ObjectUtils.asMap("folder", "hebergements")
        );
        return (String) result.get("secure_url");
    }

    /** Supprimer une image par son public_id */
    public void deleteImage(String publicId) throws Exception {
        cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
        );
    }
}