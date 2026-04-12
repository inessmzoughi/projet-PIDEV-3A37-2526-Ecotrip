package tn.esprit.repository.activity;

import tn.esprit.database.Base;
import tn.esprit.models.activity.Guide;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuideRepository {

    private Connection conn;

    public GuideRepository() {
        conn = Base.getInstance().getConnection();
    }

    public void save(Guide guide) throws SQLException {
        String sql = "INSERT INTO guide (first_name, last_name, email, phone, bio, rating, photo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, guide.getFirstName());
        ps.setString(2, guide.getLastName());
        ps.setString(3, guide.getEmail());
        ps.setString(4, guide.getPhone());
        ps.setString(5, guide.getBio());
        if (guide.getRating() != null)
            ps.setFloat(6, guide.getRating());
        else
            ps.setNull(6, Types.FLOAT);
        ps.setString(7, guide.getPhoto());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) guide.setId(keys.getInt(1));
    }

    public List<Guide> findAll() throws SQLException {
        List<Guide> list = new ArrayList<>();
        String sql = "SELECT * FROM guide";
        Statement ste = conn.createStatement();
        ResultSet rs = ste.executeQuery(sql);
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    public Guide findById(int id) throws SQLException {
        String sql = "SELECT * FROM guide WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public void update(Guide guide) throws SQLException {
        String sql = "UPDATE guide SET first_name=?, last_name=?, email=?, phone=?, " +
                "bio=?, rating=?, photo=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, guide.getFirstName());
        ps.setString(2, guide.getLastName());
        ps.setString(3, guide.getEmail());
        ps.setString(4, guide.getPhone());
        ps.setString(5, guide.getBio());
        if (guide.getRating() != null)
            ps.setFloat(6, guide.getRating());
        else
            ps.setNull(6, Types.FLOAT);
        ps.setString(7, guide.getPhoto());
        ps.setInt(8, guide.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM guide WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Guide mapRow(ResultSet rs) throws SQLException {
        Guide g = new Guide();
        g.setId(rs.getInt("id"));
        g.setFirstName(rs.getString("first_name"));
        g.setLastName(rs.getString("last_name"));
        g.setEmail(rs.getString("email"));
        g.setPhone(rs.getString("phone"));
        g.setBio(rs.getString("bio"));
        float rating = rs.getFloat("rating");
        if (!rs.wasNull()) g.setRating(rating);
        g.setPhoto(rs.getString("photo"));
        return g;
    }
}
