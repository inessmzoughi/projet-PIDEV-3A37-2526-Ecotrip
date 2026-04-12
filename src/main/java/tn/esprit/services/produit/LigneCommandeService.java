package tn.esprit.services.produit;



import tn.esprit.database.Base;
import tn.esprit.models.produit.LigneCommande;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LigneCommandeService implements I_service<LigneCommande> {

    private final Connection connection;

    public LigneCommandeService() {
        connection = Base.getInstance().getConnection();
    }

    @Override
    public void create(LigneCommande ligneCommande) throws SQLException {
        String sql = "INSERT INTO ligne_de_commande (commande_id, produit_id, quantite, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ligneCommande.getCommandeId());
            ps.setInt(2, ligneCommande.getProductId());
            ps.setInt(3, ligneCommande.getQuantite());
            ps.setDouble(4, ligneCommande.getUnitprice());
            ps.setDouble(5, ligneCommande.getSubtotal());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(LigneCommande ligneCommande) throws SQLException {
        String sql = "UPDATE ligne_de_commande SET commande_id=?, produit_id=?, quantite=?, unit_price=?, subtotal=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ligneCommande.getCommandeId());
            ps.setInt(2, ligneCommande.getProductId());
            ps.setInt(3, ligneCommande.getQuantite());
            ps.setDouble(4, ligneCommande.getUnitprice());
            ps.setDouble(5, ligneCommande.getSubtotal());
            ps.setInt(6, ligneCommande.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ligne_de_commande WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<LigneCommande> read() throws SQLException {
        List<LigneCommande> list = new ArrayList<>();
        String sql = "SELECT * FROM ligne_de_commande";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    public Optional<LigneCommande> getById(int id) throws SQLException {
        String sql = "SELECT * FROM ligne_de_commande WHERE id=?";

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

    public List<LigneCommande> getByCommande(int commandeId) throws SQLException {
        List<LigneCommande> list = new ArrayList<>();
        String sql = "SELECT * FROM ligne_de_commande WHERE commande_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commandeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    public List<LigneCommande> getByProduct(int productId) throws SQLException {
        List<LigneCommande> list = new ArrayList<>();
        String sql = "SELECT * FROM ligne_de_commande WHERE produit_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    private LigneCommande mapRow(ResultSet rs) throws SQLException {
        return new LigneCommande(
                rs.getInt("id"),
                rs.getInt("commande_id"),
                rs.getInt("produit_id"),
                rs.getInt("quantite"),
                rs.getDouble("unit_price"),
                rs.getDouble("subtotal")
        );
    }
}