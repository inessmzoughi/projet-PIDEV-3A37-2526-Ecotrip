package tn.esprit.services.hebergement;

import tn.esprit.models.Equipement;
import tn.esprit.database.Base;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Equipement_service {

    private final Connection connection = Base.getInstance().getConnection();

    public void ajouter(Equipement e) throws SQLException {
        String sql = "INSERT INTO equipement (nom, description) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getDescription());
        ps.executeUpdate();
    }

    public List<Equipement> getAll() throws SQLException {
        List<Equipement> list = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM equipement ORDER BY nom ASC");
        while (rs.next()) {
            list.add(new Equipement(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("description")
            ));
        }
        return list;
    }

    @SuppressWarnings("unused")
    public Equipement getById(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM equipement WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return new Equipement(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description")
        );
        return null;
    }

    public void modifier(Equipement e) throws SQLException {
        String sql = "UPDATE equipement SET nom=?, description=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getDescription());
        ps.setInt(3, e.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM equipement WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // Nombre d'équipements avec description renseignée
    public int countAvecDescription() throws SQLException {
        ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM equipement WHERE description IS NOT NULL AND description != ''");
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    // Nombre d'hébergements qui utilisent au moins un équipement
    // (suppose une table de liaison hebergement_equipement)
    public int countHebergementsLies() throws SQLException {
        try {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT COUNT(DISTINCT hebergement_id) FROM hebergement_equipement");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // Table de liaison inexistante — retourne 0 silencieusement
        }
        return 0;
    }
}