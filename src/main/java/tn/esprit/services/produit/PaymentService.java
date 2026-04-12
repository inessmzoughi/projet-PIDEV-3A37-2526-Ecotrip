package tn.esprit.services.produit;

import tn.esprit.database.Base;
import tn.esprit.models.produit.Payment;
import tn.esprit.models.produit.PaymentMethod;
import tn.esprit.models.produit.PaymentStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentService implements I_service<Payment> {

    private final Connection connection;

    public PaymentService() {
        connection = Base.getInstance().getConnection();
    }

    @Override
    public void create(Payment t) throws SQLException {
        String sql = "INSERT INTO paiement (montant, methode_paiement, date_paiement, commande_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, t.getAmount());
            ps.setString(2, t.getPaymentMethod() != null ? t.getPaymentMethod().name() : PaymentMethod.CASH.name());

            Timestamp paidAt = t.getPaidAt() != null
                    ? new Timestamp(t.getPaidAt().getTime())
                    : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(3, paidAt);

            if (t.getReservationId() > 0) {
                ps.setInt(4, t.getReservationId()); // ici reservationId = commande_id
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            ps.executeUpdate();
        }
    }

    @Override
    public void update(Payment t) throws SQLException {
        String sql = "UPDATE paiement SET montant=?, methode_paiement=?, date_paiement=?, commande_id=? WHERE id_paiement=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, t.getAmount());
            ps.setString(2, t.getPaymentMethod() != null ? t.getPaymentMethod().name() : PaymentMethod.CASH.name());

            Timestamp paidAt = t.getPaidAt() != null
                    ? new Timestamp(t.getPaidAt().getTime())
                    : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(3, paidAt);

            if (t.getReservationId() > 0) {
                ps.setInt(4, t.getReservationId()); // ici reservationId = commande_id
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            ps.setInt(5, t.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM paiement WHERE id_paiement=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Payment> read() throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM paiement";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    public Optional<Payment> getById(int id) throws SQLException {
        String sql = "SELECT * FROM paiement WHERE id_paiement=?";

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

    public List<Payment> getByCommande(int commandeId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM paiement WHERE commande_id=?";

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

    public List<Payment> getByMethod(PaymentMethod method) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM paiement WHERE methode_paiement=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, method.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Timestamp datePaiement = rs.getTimestamp("date_paiement");

        int commandeId = rs.getInt("commande_id");
        if (rs.wasNull()) {
            commandeId = 0;
        }

        return new Payment(
                rs.getInt("id_paiement"),
                rs.getDouble("montant"),
                parsePaymentMethod(rs.getString("methode_paiement")),
                datePaiement != null ? PaymentStatus.COMPLETED : PaymentStatus.PENDING,
                datePaiement,
                0,                  // transaction_id n'existe pas dans la table
                datePaiement,        // created_at n'existe pas dans la table
                commandeId           // ici reservationId = commande_id
        );
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PaymentMethod.CASH;
        }

        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PaymentMethod.CASH;
        }
    }
}