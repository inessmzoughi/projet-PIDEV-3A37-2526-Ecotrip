package tn.esprit.services.activity;

import javafx.scene.image.Image;

import java.io.File;
import java.net.URL;

public final class ActivityImageService {

    private static final String UPLOADS_DIR = "uploads/activities/";

    private ActivityImageService() {
    }

    public static Image loadImage(Class<?> resourceClass, String imagePath, double width, double height) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Image image = new Image(imagePath, width, height, false, true, true);
                return image.isError() ? null : image;
            }

            File absoluteFile = new File(imagePath);
            if (absoluteFile.exists()) {
                return new Image(absoluteFile.toURI().toString(), width, height, false, true);
            }

            String fileName = new File(imagePath).getName();
            File uploadFile = new File(UPLOADS_DIR + fileName);
            if (uploadFile.exists()) {
                return new Image(uploadFile.toURI().toString(), width, height, false, true);
            }

            URL resource = resourceClass.getResource("/images/" + fileName);
            if (resource != null) {
                return new Image(resource.toExternalForm(), width, height, false, true);
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
