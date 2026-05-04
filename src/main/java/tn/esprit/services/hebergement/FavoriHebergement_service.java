package tn.esprit.services.hebergement;

import tn.esprit.database.Base;
import tn.esprit.models.hebergements.Hebergement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriHebergement_service {

    private final Connection cnx = Base.getInstance().getConnection();

    /* ─── Ajouter favori ─── */
    public void ajouter(int userId, int hebergementId) throws SQLException {
        String sql = "INSERT IGNORE INTO favori_hebergement (user_id, hebergement_id) VALUES (?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ps.executeUpdate();
        }
    }

    /* ─── Supprimer favori ─── */
    public void supprimer(int userId, int hebergementId) throws SQLException {
        String sql = "DELETE FROM favori_hebergement WHERE user_id=? AND hebergement_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ps.executeUpdate();
        }
    }

    /* ─── Toggle : retourne true si ajouté, false si retiré ─── */
    public boolean toggle(int userId, int hebergementId) throws SQLException {
        if (isFavori(userId, hebergementId)) {
            supprimer(userId, hebergementId);
            return false;
        } else {
            ajouter(userId, hebergementId);
            return true;
        }
    }

    /* ─── Vérifier si favori ─── */
    public boolean isFavori(int userId, int hebergementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favori_hebergement WHERE user_id=? AND hebergement_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hebergementId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /* ─── Liste des hébergements favoris d'un user ─── */
    public List<Hebergement> getFavoris(int userId) throws SQLException {
        List<Hebergement> list = new ArrayList<>();
        String sql = "SELECT h.* FROM hebergement h "
                + "JOIN favori_hebergement f ON h.id = f.hebergement_id "
                + "WHERE f.user_id = ? "
                + "ORDER BY f.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Hebergement h = new Hebergement();
                h.setId(rs.getInt("id"));
                h.setNom(rs.getString("nom"));
                h.setVille(rs.getString("ville"));
                h.setDescription(rs.getString("description"));
                h.setImage_principale(rs.getString("image_principale"));
                h.setNb_etoiles(rs.getInt("nb_etoiles"));
                h.setAdresse(rs.getString("adresse"));
                h.setCategorie_id(rs.getInt("categorie_id"));
                list.add(h);
            }
        }
        return list;
    }

    /* ─── Compter favoris d'un user ─── */
    public int countFavoris(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favori_hebergement WHERE user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}