package org.example.services.hebergement;

import org.example.models.Chambre;
import org.example.database.Base;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Chambre_service {

    private final Connection connection = Base.getInstance().getConnection();

    public void ajouter(Chambre c) throws SQLException {
        String sql = "INSERT INTO chambre (numero, type, capacite, prix_par_nuit, description, disponible, hebergement_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, c.getNumero());
        ps.setString(2, c.getType());
        ps.setInt(3, c.getCapacite());
        ps.setDouble(4, c.getPrix_par_nuit());
        ps.setString(5, c.getDescription());
        ps.setInt(6, c.getDisponible());
        ps.setInt(7, c.getHebergement_id());
        ps.executeUpdate();
    }

    public List<Chambre> getAll() throws SQLException {
        List<Chambre> list = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM chambre");
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Chambre> getByHebergement(int hebergementId) throws SQLException {
        List<Chambre> list = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM chambre WHERE hebergement_id = ?");
        ps.setInt(1, hebergementId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapResultSet(rs));
        return list;
    }

    public Chambre getById(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM chambre WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapResultSet(rs);
        return null;
    }

    public void modifier(Chambre c) throws SQLException {
        String sql = "UPDATE chambre SET numero=?, type=?, capacite=?, prix_par_nuit=?, description=?, disponible=?, hebergement_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, c.getNumero());
        ps.setString(2, c.getType());
        ps.setInt(3, c.getCapacite());
        ps.setDouble(4, c.getPrix_par_nuit());
        ps.setString(5, c.getDescription());
        ps.setInt(6, c.getDisponible());
        ps.setInt(7, c.getHebergement_id());
        ps.setInt(8, c.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM chambre WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Chambre mapResultSet(ResultSet rs) throws SQLException {
        return new Chambre(
                rs.getInt("id"),
                rs.getInt("hebergement_id"),
                rs.getInt("disponible"),
                rs.getString("description"),
                rs.getDouble("prix_par_nuit"),
                rs.getInt("capacite"),
                rs.getString("type"),
                rs.getString("numero")
        );
    }
}