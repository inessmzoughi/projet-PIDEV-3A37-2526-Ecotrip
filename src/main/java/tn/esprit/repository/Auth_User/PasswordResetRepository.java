package tn.esprit.repository.Auth_User;

import tn.esprit.config.EmailConfig;
import tn.esprit.database.Base;
import tn.esprit.models.Auth_User.PasswordResetToken;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetRepository {

    /**
     * Create a new token for a user.
     * Deletes any existing unused tokens for that user first.
     */
    public PasswordResetToken createToken(int userId) {
        // Delete old tokens for this user
        deleteByUserId(userId);

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(EmailConfig.TOKEN_EXPIRY_MINUTES);

        String sql = """
            INSERT INTO password_reset_token
              (user_id, token, expires_at, used, created_at)
            VALUES (?, ?, ?, 0, ?)
            """;

        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            s.setInt(1, userId);
            s.setString(2, token);
            s.setTimestamp(3, Timestamp.valueOf(expiresAt));
            s.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            s.executeUpdate();

            PasswordResetToken prt = new PasswordResetToken();
            prt.setUserId(userId);
            prt.setToken(token);
            prt.setExpiresAt(expiresAt);
            prt.setUsed(false);
            prt.setCreatedAt(LocalDateTime.now());

            ResultSet keys = s.getGeneratedKeys();
            if (keys.next()) prt.setId(keys.getInt(1));

            return prt;

        } catch (SQLException e) {
            throw new RuntimeException("Error creating reset token", e);
        }
    }

    /** Find a token by its string value */
    public Optional<PasswordResetToken> findByToken(String token) {
        String sql = "SELECT * FROM password_reset_token WHERE token = ?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, token);
            ResultSet rs = s.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reset token", e);
        }
        return Optional.empty();
    }

    /** Mark token as used after successful password reset */
    public void markUsed(String token) {
        String sql = "UPDATE password_reset_token SET used = 1 WHERE token = ?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, token);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking token used", e);
        }
    }

    public void deleteByUserId(int userId) {
        String sql = "DELETE FROM password_reset_token WHERE user_id = ?";
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, userId);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting tokens", e);
        }
    }

    private PasswordResetToken mapRow(ResultSet rs) throws SQLException {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setToken(rs.getString("token"));
        t.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        t.setUsed(rs.getBoolean("used"));
        t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return t;
    }

    /** Generate a cryptographically secure random token */
    private String generateToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes); // 64-char hex string
    }
}