package tn.esprit.services;

import tn.esprit.config.EmailConfig;
import tn.esprit.models.PasswordResetToken;
import tn.esprit.models.User;
import tn.esprit.repository.PasswordResetRepository;
import tn.esprit.repository.UserRepository;
import tn.esprit.utils.PasswordUtil;

import java.util.Optional;

public class PasswordResetService {

    private final UserRepository          userRepo  = new UserRepository();
    private final PasswordResetRepository tokenRepo = new PasswordResetRepository();
    private final EmailService            email     = new EmailService();

    /**
     * Step 1 — Request reset.
     * Finds user by email, creates token, sends email.
     *
     * Returns true even if email not found (security: don't reveal
     * whether an email exists — same as Symfony behavior).
     */
    /**
     * Request a password reset.
     * Throws IllegalArgumentException if email not found — caller shows the message.
     */
    public void requestReset(String emailAddress) throws Exception {
        java.util.Optional<User> userOpt =
                userRepo.findByEmail(emailAddress.trim());

        // Email not in DB → tell the user explicitly
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("NOT_FOUND");
        }

        User user = userOpt.get();
        PasswordResetToken token = tokenRepo.createToken(user.getId());

        email.sendPasswordResetEmail(
                user.getEmail(),
                user.getUsername(),
                token.getToken(),
                EmailConfig.TOKEN_EXPIRY_MINUTES
        );
    }

    /**
     * Step 2 — Validate token.
     * Returns the token object if valid, empty if expired/used/not found.
     */
    public Optional<PasswordResetToken> validateToken(String tokenString) {
        return tokenRepo.findByToken(tokenString.trim())
                .filter(PasswordResetToken::isValid);
    }

    /**
     * Step 3 — Reset password.
     * Validates token, hashes new password, saves to DB, marks token used.
     *
     * @throws IllegalArgumentException if token is invalid/expired
     */
    public void resetPassword(String tokenString, String newPassword) {
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 6 caractères.");

        PasswordResetToken token = tokenRepo.findByToken(tokenString.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Code invalide ou introuvable."));

        if (!token.isValid())
            throw new IllegalArgumentException(
                    "Ce code a expiré ou a déjà été utilisé.");

        // Hash + save new password
        User user = userRepo.findById(token.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        user.setPassword(PasswordUtil.hash(newPassword));
        userRepo.updatePassword(user);

        // Mark token used — can only be used once
        tokenRepo.markUsed(tokenString.trim());
    }
}