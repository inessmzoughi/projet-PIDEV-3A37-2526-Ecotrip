package tn.esprit.services.Auth_User;

import tn.esprit.models.User;
import tn.esprit.models.enums.Role;
import tn.esprit.repository.UserRepository;
import tn.esprit.utils.PasswordUtil;

import java.util.List;

public class UserService {

    private final UserRepository userRepository = new UserRepository();

    // 🔹 Only adds value: validation + hashing
    public void createUser(String username, String email, String password,
                           String address, String telephone,
                           String role, boolean isVerified) {

        if (username.isEmpty()) throw new RuntimeException("Username required");
        if (email.isEmpty() || !email.contains("@")) throw new RuntimeException("Invalid email");
        if (password.length() < 6) throw new RuntimeException("Password too short");

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(PasswordUtil.hash(password));
        user.setAddress(address);
        user.setTelephone(telephone);
        user.setRoles(Role.valueOf(role));
        user.setIsVerified(isVerified);

        userRepository.save(user);
    }

    public void updateUser(int id, String username, String email,
                           String address, String telephone,
                           String role, boolean isVerified,
                           String newPassword) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(username);
        user.setEmail(email);
        user.setAddress(address);
        user.setTelephone(telephone);
        user.setRoles(Role.valueOf(role));
        user.setIsVerified(isVerified);

        userRepository.update(user);

        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(PasswordUtil.hash(newPassword));
            userRepository.updatePassword(user);
        }
    }

    public void deleteUser(int id) {
        userRepository.delete(id);
    }

    // 👉 Direct calls allowed (NO duplication)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String query, Role role) {
        return userRepository.search(query, role);
    }

    public int countUsers() {
        return userRepository.count();
    }

    public int countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public int countVerifiedUsers() {
        return userRepository.countVerified();
    }
}