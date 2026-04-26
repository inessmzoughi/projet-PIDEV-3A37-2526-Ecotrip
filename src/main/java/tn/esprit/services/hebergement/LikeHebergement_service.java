package tn.esprit.services.hebergement;

import tn.esprit.database.Base;

import java.sql.*;

public class LikeHebergement_service {

    private final Connection cnx = Base.getInstance().getConnection();

    /* ─── Toggle like : like si pas liké, unlike si déjà liké ─── */
    public boolean toggleLike(int userId, int hebergementId) throws SQLException {
        if (isLiked(userId, hebergementId)) {
            unlike(userId, hebergementId);
            return false;
        } else {
            like(userId, hebergementId);
            return true;
        }
    }

    /* ─── Vérifier si l'user a déjà liké ─── */
    public boolean isLiked(int userId, int hebergementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM likes_hebergement "
                + "WHERE user_id=? AND hebergement_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /* ─── Compter les likes d'un hébergement ─── */
    public int countLikes(int hebergementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM likes_hebergement "
                + "WHERE hebergement_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, hebergementId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /* ─── Compter le total des users ─── */
    private int countTotalUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM user";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 1; // éviter division par 0
        }
    }

    /* ─── Pourcentage de users qui ont liké cet hébergement ─── */
    public double getLikePercentage(int hebergementId) throws SQLException {
        int likes = countLikes(hebergementId);
        int total = countTotalUsers();
        if (total == 0) return 0.0;
        double pct = (likes * 100.0) / total;
        return Math.min(pct, 100.0); // plafonner à 100%
    }

    /* ─── Ajouter like ─── */
    private void like(int userId, int hebergementId) throws SQLException {
        String sql = "INSERT INTO likes_hebergement "
                + "(user_id, hebergement_id) VALUES (?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ps.executeUpdate();
        }
    }

    /* ─── Supprimer like ─── */
    private void unlike(int userId, int hebergementId) throws SQLException {
        String sql = "DELETE FROM likes_hebergement "
                + "WHERE user_id=? AND hebergement_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ps.executeUpdate();
        }
    }
}