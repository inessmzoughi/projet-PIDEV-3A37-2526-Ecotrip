package tn.esprit.services.produit;


import tn.esprit.database.Base;
import tn.esprit.models.produit.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductService implements I_service<Product> {

    private final Connection connection;

    public ProductService() {
        connection = Base.getInstance().getConnection();
    }

    @Override
    public void create(Product p) throws SQLException {
        String sql = "INSERT INTO produits (nom, prix, stock, categorie_id, image) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setDouble(2, p.getPrix());
            ps.setInt(3, p.getStock());
            ps.setInt(4, p.getProductCategoryId());

            if (p.getImage() == null || p.getImage().trim().isEmpty()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, p.getImage().trim());
            }

            ps.executeUpdate();
        }
    }

    @Override
    public void update(Product p) throws SQLException {
        String sql = "UPDATE produits SET nom=?, prix=?, stock=?, categorie_id=?, image=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setDouble(2, p.getPrix());
            ps.setInt(3, p.getStock());
            ps.setInt(4, p.getProductCategoryId());

            if (p.getImage() == null || p.getImage().trim().isEmpty()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, p.getImage().trim());
            }

            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM produits WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Product> read() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM produits";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    public Optional<Product> getById(int id) throws SQLException {
        String sql = "SELECT * FROM produits WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Product> getByCategory(int categoryId) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM produits WHERE categorie_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getDouble("prix"),
                rs.getInt("stock"),
                rs.getInt("categorie_id"),
                rs.getString("image")
        );
    }
}