package tn.esprit.repository;

import tn.esprit.database.Base;
import tn.esprit.models.User;
import tn.esprit.models.enums.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    public List<User> search(String query, Role roleFilter) {
        StringBuilder sql = new StringBuilder("SELECT * FROM user WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (email LIKE ? OR username LIKE ? OR telephone LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }

        if (roleFilter != null) {
            sql.append(" AND roles = ?");
            params.add(roleFilter.name());
        }

        sql.append(" ORDER BY id DESC");

        List<User> users = new ArrayList<>();

        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) users.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException("DB error searching users", e);
        }

        return users;
    }

    public List<User> findAll() {
        return search(null, null);
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("DB error finding user by id", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("DB error finding user by email", e);
        }
        return Optional.empty();
    }

    public int count() {
        return countWhere("1=1", null);
    }

    public int countByRole(Role role) {
        return countWhere("roles = ?", role.name());
    }

    public int countVerified() {
        return countWhere("is_verified = 1", null);
    }

    private int countWhere(String condition, String param) {
        String sql = "SELECT COUNT(*) FROM user WHERE " + condition;
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (param != null) stmt.setString(1, param);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("DB error counting users", e);
        }
        return 0;
    }

    public User save(User user) {
        String sql = "INSERT INTO user (username, email, password, roles, is_verified, address, telephone, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRoles().name());
            stmt.setBoolean(5, user.isVerified());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getTelephone());
            stmt.setString(8, user.getImage());

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));

        } catch (SQLException e) {
            throw new RuntimeException("DB error saving user", e);
        }

        return user;
    }

    public void update(User user) {
        String sql = "UPDATE user SET username=?, email=?, roles=?, is_verified=?, address=?, telephone=?, image=?  WHERE id=?";

        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getRoles().name());
            stmt.setBoolean(4, user.isVerified());
            stmt.setString(5, user.getAddress());
            stmt.setString(6, user.getTelephone());
            stmt.setString(7, user.getImage());
            stmt.setInt(8, user.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB error updating user", e);
        }
    }

    public void updatePassword(User user) {
        String sql = "UPDATE user SET password=? WHERE id=?";

        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getPassword());
            stmt.setInt(2, user.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB error updating password", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id=?";

        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB error deleting user", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRoles(Role.valueOf(rs.getString("roles")));
        u.setIsVerified(rs.getBoolean("is_verified"));
        u.setAddress(rs.getString("address"));
        u.setTelephone(rs.getString("telephone"));
        u.setImage(rs.getString("image"));
        u.setFaceDescriptor(rs.getString("face_descriptor"));
        return u;
    }
}