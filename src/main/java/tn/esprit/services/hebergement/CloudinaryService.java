package tn.esprit.services.hebergement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class CloudinaryService {

    private static CloudinaryService instance;
    private final Cloudinary cloudinary;

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
                "cloud_name", loadProperty("cloudinary.cloud.name"),
                "api_key",    loadProperty("cloudinary.api.key"),
                "api_secret", loadProperty("cloudinary.api.secret"),
                "secure",     true
        ));
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
