package org.example.services.Auth_User;

import org.example.exception.AuthException;
import org.example.exception.EmailAlreadyExistsException;
import org.example.models.User;
import org.example.models.enums.Role;
import org.example.repository.UserRepository;
import org.example.utils.PasswordUtil;

public class AuthService {

    private final UserRepository userRepository = new UserRepository();

    public User login(String email, String password) throws AuthException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("No account found with this email."));

        if (!PasswordUtil.verify(password, user.getPassword())) {
            throw new AuthException("Incorrect password.");
        }

        return user;
    }

    public User register(String username, String email, String password) throws EmailAlreadyExistsException {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException("Email already in use.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(PasswordUtil.hash(password)); // NEVER store plain text
        user.setRoles(Role.User);

        return userRepository.save(user);
    }
}
