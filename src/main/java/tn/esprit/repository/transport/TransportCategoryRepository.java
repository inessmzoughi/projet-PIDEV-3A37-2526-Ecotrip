package tn.esprit.repository.transport;

import tn.esprit.database.Base;
import tn.esprit.models.transport.TransportCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TransportCategoryRepository {

    private final Connection connection;

    public TransportCategoryRepository() {
        this.connection = Base.getInstance().getConnection();
    }

    public void save(TransportCategory category) throws SQLException {
        String sql = "INSERT INTO transport_category (name, description) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<TransportCategory> findAll() throws SQLException {
        List<TransportCategory> categories = new ArrayList<>();
        String sql = "SELECT id, name, description FROM transport_category ORDER BY name";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
        }
        return categories;
    }

    public TransportCategory findById(int id) throws SQLException {
        String sql = "SELECT id, name, description FROM transport_category WHERE id = ?";
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

    public void update(TransportCategory category) throws SQLException {
        String sql = "UPDATE transport_category SET name = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM transport_category WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private TransportCategory mapRow(ResultSet rs) throws SQLException {
        return new TransportCategory(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}
