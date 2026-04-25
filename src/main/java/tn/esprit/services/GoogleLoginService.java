package tn.esprit.services;

import tn.esprit.models.GoogleUserInfo;
import tn.esprit.models.User;
import tn.esprit.models.enums.Role;
import tn.esprit.repository.UserRepository;
import tn.esprit.utils.PasswordUtil;

import java.util.Optional;
import java.util.UUID;

public class GoogleLoginService {

    private final UserRepository userRepository = new UserRepository();

    /**
     * Find or create a user from Google profile.
     *
     * If email already exists in DB → log them in (link Google to existing account).
     * If new email → create account automatically (no password, google_id stored).
     *
     * @return the User to log in
     */
    public User findOrCreateUser(GoogleUserInfo googleInfo) {
        // Try to find by email first
        Optional<User> existing = userRepository.findByEmail(googleInfo.getEmail());

        if (existing.isPresent()) {
            // User already exists — just return them
            // Optionally update their profile photo if they don't have one
            User user = existing.get();
            if (user.getImage() == null && googleInfo.getPicture() != null) {
                user.setImage(googleInfo.getPicture());
                userRepository.update(user);
            }
            return user;
        }

        // New user — create account from Google profile
        User user = new User();
        user.setEmail(googleInfo.getEmail());
        user.setUsername(googleInfo.getName().isEmpty()
                ? googleInfo.getEmail().split("@")[0]  // use email prefix as username
                : googleInfo.getName());
        user.setImage(googleInfo.getPicture());
        user.setIsVerified(googleInfo.isEmailVerified()); // Google emails are pre-verified
        user.setRoles(Role.ROLE_USER);

        // Set a random unusable password — user can set one later via forgot password
        // They log in exclusively via Google
        user.setPassword(PasswordUtil.hash(UUID.randomUUID().toString()));

        userRepository.save(user);
        return user;
    }
}