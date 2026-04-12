package tn.esprit.repository.transport;

import tn.esprit.database.Base;
import tn.esprit.models.transport.Chauffeur;
import tn.esprit.models.transport.Transport;
import tn.esprit.models.transport.TransportCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TransportRepository {

    private final Connection connection;

    public TransportRepository() {
        this.connection = Base.getInstance().getConnection();
    }

    public void save(Transport transport) throws SQLException {
        String sql = """
                INSERT INTO transport (type, capacite, emissionco2, prixparpersonne, disponible, image, category_id, chauffeur_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(ps, transport);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    transport.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<Transport> findAll() throws SQLException {
        List<Transport> transports = new ArrayList<>();
        String sql = """
                SELECT t.id, t.type, t.capacite, t.emissionco2, t.prixparpersonne, t.disponible, t.image,
                       c.id AS category_id, c.name AS category_name, c.description AS category_description,
                       ch.id AS chauffeur_id, ch.first_name, ch.last_name, ch.phone, ch.license_number, ch.experience, ch.rating
                FROM transport t
                LEFT JOIN transport_category c ON c.id = t.category_id
                LEFT JOIN chauffeur ch ON ch.id = t.chauffeur_id
                ORDER BY t.id DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                transports.add(mapRow(rs));
            }
        }
        return transports;
    }

    public Transport findById(int id) throws SQLException {
        String sql = """
                SELECT t.id, t.type, t.capacite, t.emissionco2, t.prixparpersonne, t.disponible, t.image,
                       c.id AS category_id, c.name AS category_name, c.description AS category_description,
                       ch.id AS chauffeur_id, ch.first_name, ch.last_name, ch.phone, ch.license_number, ch.experience, ch.rating
                FROM transport t
                LEFT JOIN transport_category c ON c.id = t.category_id
                LEFT JOIN chauffeur ch ON ch.id = t.chauffeur_id
                WHERE t.id = ?
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

    public void update(Transport transport) throws SQLException {
        String sql = """
                UPDATE transport
                SET type = ?, capacite = ?, emissionco2 = ?, prixparpersonne = ?, disponible = ?, image = ?, category_id = ?, chauffeur_id = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fillStatement(ps, transport);
            ps.setInt(9, transport.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM transport WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void fillStatement(PreparedStatement ps, Transport transport) throws SQLException {
        ps.setString(1, transport.getType());
        ps.setInt(2, transport.getCapacite());
        ps.setDouble(3, transport.getEmissionCo2());
        ps.setDouble(4, transport.getPrixParPersonne());
        ps.setBoolean(5, transport.isDisponible());
        ps.setString(6, transport.getImage());
        if (transport.getCategory() == null) {
            ps.setNull(7, java.sql.Types.INTEGER);
        } else {
            ps.setInt(7, transport.getCategory().getId());
        }
        if (transport.getChauffeur() == null) {
            ps.setNull(8, java.sql.Types.INTEGER);
        } else {
            ps.setInt(8, transport.getChauffeur().getId());
        }
    }

    private Transport mapRow(ResultSet rs) throws SQLException {
        Transport transport = new Transport();
        transport.setId(rs.getInt("id"));
        transport.setType(rs.getString("type"));
        transport.setCapacite(rs.getInt("capacite"));
        transport.setEmissionCo2(rs.getDouble("emissionco2"));
        transport.setPrixParPersonne(rs.getDouble("prixparpersonne"));
        transport.setDisponible(rs.getBoolean("disponible"));
        transport.setImage(rs.getString("image"));

        int categoryId = rs.getInt("category_id");
        if (!rs.wasNull()) {
            transport.setCategory(new TransportCategory(
                    categoryId,
                    rs.getString("category_name"),
                    rs.getString("category_description")
            ));
        }

        int chauffeurId = rs.getInt("chauffeur_id");
        if (!rs.wasNull()) {
            transport.setChauffeur(new Chauffeur(
                    chauffeurId,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("phone"),
                    rs.getString("license_number"),
                    rs.getInt("experience"),
                    rs.getDouble("rating")
            ));
        }

        return transport;
    }
}
