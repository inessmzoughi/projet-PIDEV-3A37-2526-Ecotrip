package tn.esprit.services.hebergement;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.util.Map;

public class CloudinaryService {

    private static CloudinaryService instance;
    private final Cloudinary cloudinary;

    private CloudinaryService() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dzxaahx6v",
                "api_key",    "917792891782727",
                "api_secret", "I_jzycVu6wbe8luahEhXE8RUfMk"
        ));
    }

    public static CloudinaryService getInstance() {
        if (instance == null) instance = new CloudinaryService();
        return instance;
    }

    /** Upload image avis client → retourne URL publique */
    public String uploadAvisImage(File fichier) throws Exception {
        Map result = cloudinary.uploader().upload(fichier,
                ObjectUtils.asMap("folder", "avis_clients")
        );
        return (String) result.get("secure_url");
    }

    /** Upload image hébergement → retourne URL publique */
    public String uploadHebergementImage(File fichier) throws Exception {
        Map result = cloudinary.uploader().upload(fichier,
                ObjectUtils.asMap("folder", "hebergements")
        );
        return (String) result.get("secure_url");
    }

    /** Supprimer une image par son public_id */
    public void deleteImage(String publicId) throws Exception {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}