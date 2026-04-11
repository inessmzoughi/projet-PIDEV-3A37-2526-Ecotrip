package tn.esprit.services.Auth_User;

import tn.esprit.database.Base;
import tn.esprit.models.User;
import tn.esprit.models.enums.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {

    private Connection getConn() throws SQLException {
        return Base.getInstance().getConnection();
    }

    // ── Get all ────────────────────────────────────────
    public List<User> getAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id DESC";
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Search ─────────────────────────────────────────
    public List<User> search(String query, Role role) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM user WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (email LIKE ? OR username LIKE ? OR telephone LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (role != null) {
            sql.append(" AND roles = ?");
            params.add(role.name());
        }
        sql.append(" ORDER BY id DESC");

        List<User> list = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Stats ──────────────────────────────────────────
    public int count() throws SQLException {
        return countWhere("1=1", null);
    }

    public int countByRole(Role role) throws SQLException {
        return countWhere("roles = ?", role.name());
    }

    public int countVerified() throws SQLException {
        return countWhere("is_verified = 1", null);
    }

    private int countWhere(String condition, String param) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE " + condition;
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (param != null) stmt.setString(1, param);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ── Add ────────────────────────────────────────────
    public void ajouter(User user) throws SQLException {
        String sql = "INSERT INTO user (username, email, password, roles, is_verified, address, telephone) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRoles().name());
            stmt.setBoolean(5, user.isVerified());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getTelephone());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
        }
    }

    // ── Update ─────────────────────────────────────────
    public void modifier(User user) throws SQLException {
        String sql = "UPDATE user SET username=?, email=?, roles=?, is_verified=?, address=?, telephone=? WHERE id=?";
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getRoles().name());
            stmt.setBoolean(4, user.isVerified());
            stmt.setString(5, user.getAddress());
            stmt.setString(6, user.getTelephone());
            stmt.setInt(7, user.getId());
            stmt.executeUpdate();
        }
    }

    // ── Update password separately ─────────────────────
    public void modifierMotDePasse(int userId, String newHashedPassword) throws SQLException {
        String sql = "UPDATE user SET password=? WHERE id=?";
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHashedPassword);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    // ── Delete ─────────────────────────────────────────
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id=?";
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ── Find by email ──────────────────────────────────
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    // ── Row mapper ─────────────────────────────────────
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