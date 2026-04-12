package tn.esprit.services.produit;



import tn.esprit.database.Base;
import tn.esprit.models.produit.ProductCategory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductCategoryService implements I_service<ProductCategory> {

    private final Connection cnx;

    public ProductCategoryService() {
        cnx = Base.getInstance().getConnection();
    }

    @Override
    public void create(ProductCategory pc) throws SQLException {
        String sql = "INSERT INTO categorie (nom, description) VALUES (?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, pc.getNom());

            if (pc.getDescription() == null || pc.getDescription().trim().isEmpty()) {
                ps.setNull(2, Types.LONGVARCHAR);
            } else {
                ps.setString(2, pc.getDescription().trim());
            }

            ps.executeUpdate();
        }
    }

    @Override
    public List<ProductCategory> read() throws SQLException {
        List<ProductCategory> list = new ArrayList<>();
        String sql = "SELECT id, nom, description FROM categorie ORDER BY id ASC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ProductCategory pc = new ProductCategory();
                pc.setId(rs.getInt("id"));
                pc.setNom(rs.getString("nom"));
                pc.setDescription(rs.getString("description"));
                list.add(pc);
            }
        }

        return list;
    }

    public Optional<ProductCategory> getById(int id) throws SQLException {
        String sql = "SELECT id, nom, description FROM categorie WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ProductCategory pc = new ProductCategory();
                    pc.setId(rs.getInt("id"));
                    pc.setNom(rs.getString("nom"));
                    pc.setDescription(rs.getString("description"));
                    return Optional.of(pc);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void update(ProductCategory pc) throws SQLException {
        String sql = "UPDATE categorie SET nom = ?, description = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, pc.getNom());

            if (pc.getDescription() == null || pc.getDescription().trim().isEmpty()) {
                ps.setNull(2, Types.LONGVARCHAR);
            } else {
                ps.setString(2, pc.getDescription().trim());
            }

            ps.setInt(3, pc.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM categorie WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}