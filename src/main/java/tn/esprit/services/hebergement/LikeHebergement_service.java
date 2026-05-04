package tn.esprit.services.hebergement;

import tn.esprit.database.Base;
import tn.esprit.models.hebergements.LikeHebergement;

import java.sql.*;

public class LikeHebergement_service {

    private final Connection cnx = Base.getInstance().getConnection();

    /* ══════════════════════════════════════════════════════
       LIKES
       ══════════════════════════════════════════════════════ */

    public boolean toggleLike(int userId, int hebergementId) throws SQLException {
        if (isLiked(userId, hebergementId)) {
            removeVote(userId, hebergementId);
            return false;
        } else {
            upsertVote(userId, hebergementId, LikeHebergement.Type.like);
            return true;
        }
    }

    public boolean isLiked(int userId, int hebergementId) throws SQLException {
        return getType(userId, hebergementId) == LikeHebergement.Type.like;
    }

    public int countLikes(int hebergementId) throws SQLException {
        return countByType(hebergementId, LikeHebergement.Type.like);
    }

    /* ══════════════════════════════════════════════════════
       DISLIKES
       ══════════════════════════════════════════════════════ */

    public boolean toggleDislike(int userId, int hebergementId) throws SQLException {
        if (isDisliked(userId, hebergementId)) {
            removeVote(userId, hebergementId);
            return false;
        } else {
            upsertVote(userId, hebergementId, LikeHebergement.Type.dislike);
            return true;
        }
    }

    public boolean isDisliked(int userId, int hebergementId) throws SQLException {
        return getType(userId, hebergementId) == LikeHebergement.Type.dislike;
    }

    public int countDislikes(int hebergementId) throws SQLException {
        return countByType(hebergementId, LikeHebergement.Type.dislike);
    }

    public void removeDislike(int userId, int hebergementId) throws SQLException {
        if (isDisliked(userId, hebergementId)) removeVote(userId, hebergementId);
    }

    /* ══════════════════════════════════════════════════════
       STATS
       ══════════════════════════════════════════════════════ */

    public int getNetScore(int hebergementId) throws SQLException {
        return countLikes(hebergementId) - countDislikes(hebergementId);
    }

    public double getLikePercentage(int hebergementId) throws SQLException {
        int likes    = countLikes(hebergementId);
        int dislikes = countDislikes(hebergementId);
        int total    = likes + dislikes;
        if (total == 0) return 0.0;
        return (likes * 100.0) / total;
    }

    /* ══════════════════════════════════════════════════════
       MÉTHODES INTERNES
       ══════════════════════════════════════════════════════ */

    /* ─── Récupérer le type de vote actuel (null si aucun) ─── */
    private LikeHebergement.Type getType(int userId, int hebergementId) throws SQLException {
        String sql = "SELECT type FROM likes_hebergement "
                + "WHERE user_id=? AND hebergement_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return LikeHebergement.Type.valueOf(rs.getString("type"));
            }
            return null; // pas de vote
        }
    }

    /* ─── INSERT ou UPDATE le vote (upsert) ─── */
    private void upsertVote(int userId, int hebergementId,
                            LikeHebergement.Type type) throws SQLException {
        String sql = "INSERT INTO likes_hebergement (user_id, hebergement_id, type) "
                + "VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE type = VALUES(type)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ps.setString(3, type.name());
            ps.executeUpdate();
        }
    }

    /* ─── Supprimer le vote ─── */
    private void removeVote(int userId, int hebergementId) throws SQLException {
        String sql = "DELETE FROM likes_hebergement "
                + "WHERE user_id=? AND hebergement_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ps.executeUpdate();
        }
    }

    /* ─── Compter par type ─── */
    private int countByType(int hebergementId,
                            LikeHebergement.Type type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM likes_hebergement "
                + "WHERE hebergement_id=? AND type=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, hebergementId);
            ps.setString(2, type.name());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}