package tn.esprit.services.hebergement;

import tn.esprit.database.Base;
import tn.esprit.models.hebergements.HebergementImage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HebergementImage_service {

    private final Connection connection = Base.getInstance().getConnection();

    /* ─── Ajouter une image ─── */
    public void ajouter(HebergementImage img) throws SQLException {
        String sql = "INSERT INTO hebergement_images (hebergement_id, url, legende, ordre) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, img.getHebergementId());
            ps.setString(2, img.getUrl());
            ps.setString(3, img.getLegende());
            ps.setInt(4, img.getOrdre());
            ps.executeUpdate();
        }
    }

    /* ─── Supprimer une image par id ─── */
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM hebergement_images WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /* ─── Supprimer toutes les images d'un hébergement ─── */
    public void supprimerParHebergement(int hebergementId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM hebergement_images WHERE hebergement_id = ?")) {
            ps.setInt(1, hebergementId);
            ps.executeUpdate();
        }
    }

    /* ─── Récupérer toutes les images d'un hébergement (triées par ordre) ─── */
    public List<HebergementImage> getByHebergement(int hebergementId) throws SQLException {
        List<HebergementImage> list = new ArrayList<>();
        String sql = "SELECT * FROM hebergement_images WHERE hebergement_id = ? ORDER BY ordre ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, hebergementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new HebergementImage(
                        rs.getInt("id"),
                        rs.getInt("ordre"),          // ← ordre
                        rs.getString("legende"),     // ← legende
                        rs.getString("url"),         // ← url
                        rs.getInt("hebergement_id")  // ← hebergementId
                ));
            }
        }
        return list;
    }
}