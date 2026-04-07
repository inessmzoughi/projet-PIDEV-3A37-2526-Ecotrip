package tn.esprit.repository;

import tn.esprit.models.User;
import tn.esprit.models.enums.Role;
import tn.esprit.database.Base;

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
        String sql = "INSERT INTO user (username, email, password, roles, is_verified ) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement stmt = Base.getInstance().getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword()); // already hashed
            stmt.setString(4, user.getRoles().toString());
            stmt.setBoolean(5,true);
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
        u.setRoles(Role.valueOf(rs.getString("roles")));
        return u;
    }

    public void update(User user) {
        String sql = "UPDATE users SET username=?, email=?, address=?, phone=? WHERE id=?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getAddress());
            stmt.setString(4, user.getTelephone());
            stmt.setInt(5, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error updating user", e);
        }
    }

    public void updatePassword(User user) {
        String sql = "UPDATE users SET password=? WHERE id=?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getPassword());
            stmt.setInt(2, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error updating password", e);
        }
    }
}