package org.example.repository;

import org.example.models.User;
import org.example.models.enums.Role;
import org.example.database.Base;

import java.sql.*;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error finding user by email", e);
        }
        return Optional.empty();
    }

    public User save(User user) {
        String sql = "INSERT INTO user (username, email, password, roles) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement stmt = Base.getInstance().getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword()); // already hashed
            stmt.setString(4, user.getRoles().toString());
            stmt.executeUpdate();
            System.out.println("User saved");

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("DB error saving user", e);
        }
        return user;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRoles(Role.valueOf(rs.getString("role")));
        return u;
    }
}
