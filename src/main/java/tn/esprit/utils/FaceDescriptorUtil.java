package tn.esprit.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FaceDescriptorUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    /** float[] → JSON string for DB (user.face_descriptor column) */
    public static String encode(float[] descriptor) {
        try {
            return mapper.writeValueAsString(descriptor);
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode face descriptor", e);
        }
    }

    /** JSON string from DB → float[] for comparison */
    public static float[] decode(String json) {
        try {
            return mapper.readValue(json, float[].class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot decode face descriptor", e);
        }
    }

    public static boolean isEnrolled(String faceDescriptor) {
        return faceDescriptor != null && !faceDescriptor.isBlank();
    }
}