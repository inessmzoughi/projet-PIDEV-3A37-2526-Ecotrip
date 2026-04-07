package tn.esprit.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainText, String hashed) {
        if (plainText == null || hashed == null || hashed.isEmpty()) {
            return false;
        }

        // Les hash $2y$ (PHP) sont convertis en $2a$ pour jBCrypt
        String compatibleHash = hashed;
        if (hashed.startsWith("$2y$") || hashed.startsWith("$2b$")) {
            compatibleHash = "$2a$" + hashed.substring(4);
        }

        try {
            return BCrypt.checkpw(plainText, compatibleHash);
        } catch (IllegalArgumentException e) {
            System.err.println("Hash invalide : " + hashed);
            return false;
        }
    }
}