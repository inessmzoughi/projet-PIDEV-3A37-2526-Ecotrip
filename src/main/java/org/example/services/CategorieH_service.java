package org.example.services;

import org.example.models.Categorie_hebergement;
import org.example.database.Base;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieH_service {


    private final Connection connection ;
    public CategorieH_service (){connection = Base.getInstance().getConnection();}

    public void ajouter(Categorie_hebergement c) throws SQLException {
        String sql = "INSERT INTO categorie_hebergement (nom, description) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.executeUpdate();
    }

    public List<Categorie_hebergement> getAll() throws SQLException {
        List<Categorie_hebergement> list = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM categorie_hebergement");
        while (rs.next()) {
            list.add(new Categorie_hebergement(rs.getInt("id"), rs.getString("nom"), rs.getString("description")));
        }
        return list;
    }

    public Categorie_hebergement getById(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM categorie_hebergement WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return new Categorie_hebergement(rs.getInt("id"), rs.getString("nom"), rs.getString("description"));
        return null;
    }

    public void modifier(Categorie_hebergement c) throws SQLException {
        String sql = "UPDATE categorie_hebergement SET nom=?, description=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setInt(3, c.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM categorie_hebergement WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}