package tn.esprit.services.produit;


import tn.esprit.database.Base;
import tn.esprit.models.produit.Commande;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommandeService implements I_service<Commande> {

    private final Connection connection;

    public CommandeService() {
        connection = Base.getInstance().getConnection();
    }

    @Override
    public void create(Commande t) throws SQLException {
        String sql = "INSERT INTO commandes (id_user, produit_id, quantite, prix_unitaire, total, date_commande) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, t.getIdUser());
            ps.setInt(2, t.getProduitId());
            ps.setInt(3, t.getQuantite());
            ps.setDouble(4, t.getPrixUnitaire());
            ps.setDouble(5, t.getTotal());

            if (t.getDateCommande() != null) {
                ps.setTimestamp(6, new Timestamp(t.getDateCommande().getTime()));
            } else {
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            }

            ps.executeUpdate();
        }
    }

    @Override
    public void update(Commande t) throws SQLException {
        String sql = "UPDATE commandes SET id_user=?, produit_id=?, quantite=?, prix_unitaire=?, total=?, date_commande=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, t.getIdUser());
            ps.setInt(2, t.getProduitId());
            ps.setInt(3, t.getQuantite());
            ps.setDouble(4, t.getPrixUnitaire());
            ps.setDouble(5, t.getTotal());

            if (t.getDateCommande() != null) {
                ps.setTimestamp(6, new Timestamp(t.getDateCommande().getTime()));
            } else {
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            }

            ps.setInt(7, t.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM commandes WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Commande> read() throws SQLException {
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commandes";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    public Optional<Commande> getById(int id) throws SQLException {
        String sql = "SELECT * FROM commandes WHERE id=?";

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

    public List<Commande> getByUser(int idUser) throws SQLException {
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commandes WHERE id_user=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    private Commande mapRow(ResultSet rs) throws SQLException {
        return new Commande(
                rs.getInt("id"),
                rs.getInt("id_user"),
                rs.getInt("produit_id"),
                rs.getInt("quantite"),
                rs.getDouble("prix_unitaire"),
                rs.getDouble("total"),
                rs.getTimestamp("date_commande")
        );
    }
}