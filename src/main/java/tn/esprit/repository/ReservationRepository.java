package tn.esprit.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.database.Base;
import tn.esprit.models.Reservation;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.models.enums.ReservationType;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReservationRepository {

    private static final ObjectMapper mapper = new ObjectMapper();

    public void save(Reservation r) throws SQLException {
        String sql = """
            INSERT INTO reservation
              (user_id, reservation_type, reservation_id, total_price,
               status, date_from, date_to, number_of_persons, details, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, r.getUserId());
            s.setString(2, r.getReservationType().name());
            s.setInt(3, r.getReservationId());
            s.setDouble(4, r.getTotalPrice());
            s.setString(5, r.getStatus().name());
            s.setDate(6, Date.valueOf(r.getDateFrom()));
            s.setDate(7, r.getDateTo() != null ? Date.valueOf(r.getDateTo()) : null);
            s.setInt(8, r.getNumberOfPersons());
            s.setString(9, r.getDetails() != null ? mapper.writeValueAsString(r.getDetails()) : null);
            s.setTimestamp(10, Timestamp.valueOf(r.getCreatedAt()));
            s.setTimestamp(11, Timestamp.valueOf(r.getUpdatedAt()));
            s.executeUpdate();
            ResultSet keys = s.getGeneratedKeys();
            if (keys.next()) r.setId(keys.getInt(1));
        } catch (Exception e) { throw new RuntimeException("Error saving reservation", e); }
    }

    public List<Reservation> findByUser(int userId) throws SQLException {
        return findWhere("user_id = ?", userId);
    }

    public List<Reservation> findAll() throws SQLException {
        return findWhere(null, null);
    }

    public List<Reservation> findWithFilters(ReservationStatus status, ReservationType type,
                                             String userSearch, LocalDate dateFrom, LocalDate dateTo)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM reservation WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null)     { sql.append(" AND status = ?");                 params.add(status.name()); }
        if (type != null)       { sql.append(" AND reservation_type = ?");       params.add(type.name()); }
        if (dateFrom != null)   { sql.append(" AND date_from >= ?");             params.add(Date.valueOf(dateFrom)); }
        if (dateTo != null)     { sql.append(" AND date_from <= ?");             params.add(Date.valueOf(dateTo)); }
        sql.append(" ORDER BY created_at DESC");

        List<Reservation> list = new ArrayList<>();
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) s.setObject(i + 1, params.get(i));
            ResultSet rs = s.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement("DELETE FROM reservation WHERE id=?")) {
            s.setInt(1, id); s.executeUpdate();
        }
    }

    public void updateStatus(int id, ReservationStatus status) throws SQLException {
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE reservation SET status=?, updated_at=? WHERE id=?")) {
            s.setString(1, status.name());
            s.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            s.setInt(3, id);
            s.executeUpdate();
        }
    }
    public void update ( Reservation r) throws SQLException {
        try (Connection conn = Base.getInstance().getConnection();
        PreparedStatement s = conn.prepareStatement(
                "UPDATE reservation SET totalPrice=?, dateFrom=?, dateTo=?, numberOfPersons=?, details=?, updated_at=? WHERE id=?")) {
            s.setDouble(1, r.getTotalPrice());
            s.setDate(2, Date.valueOf(r.getDateFrom()));
            s.setDate(3, r.getDateTo() != null ? Date.valueOf(r.getDateTo()) : null);
            s.setInt(4, r.getNumberOfPersons());
            s.setString(5, r.getDetailsAsJson());
            s.executeUpdate();
        }
    }

    // Stats for admin dashboard
    public Map<String, Object> getStats() throws SQLException {
        String sql = """
            SELECT
              COUNT(*) AS total,
              SUM(CASE WHEN status='PENDING' THEN 1 ELSE 0 END) AS pending,
              SUM(CASE WHEN status='CONFIRMED' THEN 1 ELSE 0 END) AS confirmed,
              SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled,
              SUM(CASE WHEN status='CONFIRMED' THEN total_price ELSE 0 END) AS revenue
            FROM reservation
            """;
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) {
                return Map.of(
                        "total",     rs.getInt("total"),
                        "pending",   rs.getInt("pending"),
                        "confirmed", rs.getInt("confirmed"),
                        "cancelled", rs.getInt("cancelled"),
                        "revenue",   rs.getDouble("revenue")
                );
            }
        }
        return Map.of("total",0,"pending",0,"confirmed",0,"cancelled",0,"revenue",0.0);
    }

    private List<Reservation> findWhere(String condition, Object param) throws SQLException {
        String sql = "SELECT * FROM reservation"
                + (condition != null ? " WHERE " + condition : "")
                + " ORDER BY created_at DESC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = Base.getInstance().getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            if (param != null) s.setObject(1, param);
            ResultSet rs = s.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setReservationType(ReservationType.valueOf(rs.getString("reservation_type")));
        r.setReservationId(rs.getInt("reservation_id"));
        r.setTotalPrice(rs.getDouble("total_price"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setDateFrom(rs.getDate("date_from").toLocalDate());
        Date dateTo = rs.getDate("date_to");
        if (dateTo != null) r.setDateTo(dateTo.toLocalDate());
        r.setNumberOfPersons(rs.getInt("number_of_persons"));
        String detailsJson = rs.getString("details");
        if (detailsJson != null) {
            try { r.setDetails(mapper.readValue(detailsJson, Map.class)); }
            catch (Exception ignored) {}
        }
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return r;
    }
}