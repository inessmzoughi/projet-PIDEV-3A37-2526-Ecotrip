package tn.esprit.services.hebergement;

import tn.esprit.database.Base;
import tn.esprit.models.hebergements.Equipement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HebergementEquipement_service {

    private final Connection connection = Base.getInstance().getConnection();

    public List<Equipement> getEquipementsByHebergement(int hebergementId) throws SQLException {
        List<Equipement> list = new ArrayList<>();
        String sql = "SELECT e.* FROM equipement e " +
                "JOIN hebergement_equipement he ON e.id = he.equipement_id " +
                "WHERE he.hebergement_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, hebergementId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Equipement(rs.getInt("id"), rs.getString("nom"), rs.getString("description")));
        }
        return list;
    }

    public void ajouter(int hebergementId, int equipementId) throws SQLException {
        String sql = "INSERT INTO hebergement_equipement (hebergement_id, equipement_id) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, hebergementId);
        ps.setInt(2, equipementId);
        ps.executeUpdate();
    }

    public void supprimerParHebergement(int hebergementId) throws SQLException {
        String sql = "DELETE FROM hebergement_equipement WHERE hebergement_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, hebergementId);
        ps.executeUpdate();
    }

    public void sauvegarder(int hebergementId, List<Integer> equipementIds) throws SQLException {
        supprimerParHebergement(hebergementId);
        for (int eqId : equipementIds) {
            ajouter(hebergementId, eqId);
        }
    }
}