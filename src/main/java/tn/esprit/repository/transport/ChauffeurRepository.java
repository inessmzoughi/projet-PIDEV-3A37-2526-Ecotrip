package tn.esprit.repository.transport;

import tn.esprit.database.Base;
import tn.esprit.models.transport.Chauffeur;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChauffeurRepository {

    private final Connection connection;

    public ChauffeurRepository() {
        this.connection = Base.getInstance().getConnection();
    }

    public void save(Chauffeur chauffeur) throws SQLException {
        String sql = """
                INSERT INTO chauffeur (first_name, last_name, phone, license_number, experience, rating)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, chauffeur.getFirstName());
            ps.setString(2, chauffeur.getLastName());
            ps.setString(3, chauffeur.getPhone());
            ps.setString(4, chauffeur.getLicenseNumber());
            ps.setInt(5, chauffeur.getExperience());
            ps.setDouble(6, chauffeur.getRating());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    chauffeur.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<Chauffeur> findAll() throws SQLException {
        List<Chauffeur> chauffeurs = new ArrayList<>();
        String sql = """
                SELECT id, first_name, last_name, phone, license_number, experience, rating
                FROM chauffeur
                ORDER BY first_name, last_name
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                chauffeurs.add(mapRow(rs));
            }
        }
        return chauffeurs;
    }

    public Chauffeur findById(int id) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, phone, license_number, experience, rating
                FROM chauffeur
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public void update(Chauffeur chauffeur) throws SQLException {
        String sql = """
                UPDATE chauffeur
                SET first_name = ?, last_name = ?, phone = ?, license_number = ?, experience = ?, rating = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chauffeur.getFirstName());
            ps.setString(2, chauffeur.getLastName());
            ps.setString(3, chauffeur.getPhone());
            ps.setString(4, chauffeur.getLicenseNumber());
            ps.setInt(5, chauffeur.getExperience());
            ps.setDouble(6, chauffeur.getRating());
            ps.setInt(7, chauffeur.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM chauffeur WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Chauffeur mapRow(ResultSet rs) throws SQLException {
        return new Chauffeur(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone"),
                rs.getString("license_number"),
                rs.getInt("experience"),
                rs.getDouble("rating")
        );
    }
}
