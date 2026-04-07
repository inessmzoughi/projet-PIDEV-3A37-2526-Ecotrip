package tn.esprit.services.Auth_User;

import tn.esprit.exception.AuthException;
import tn.esprit.exception.EmailAlreadyExistsException;
import tn.esprit.models.User;
import tn.esprit.models.enums.Role;
import tn.esprit.repository.UserRepository;
import tn.esprit.utils.PasswordUtil;

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
