package tn.esprit.repository.activity;

import tn.esprit.database.Base;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivityCategory;
import tn.esprit.models.activity.Guide;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityRepository {

    private Connection conn;

    public ActivityRepository() {
        conn = Base.getInstance().getConnection();
    }

    public void save(Activity activity) throws SQLException {
        String sql = "INSERT INTO activity (title, description, price, duration_minutes, " +
                "location, max_participants, image, is_active, latitude, longitude, category_id, guide_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, activity.getTitle());
        ps.setString(2, activity.getDescription());
        ps.setDouble(3, activity.getPrice());
        ps.setInt(4, activity.getDurationMinutes());
        ps.setString(5, activity.getLocation());
        ps.setInt(6, activity.getMaxParticipants());
        ps.setString(7, activity.getImage());
        ps.setBoolean(8, activity.isActive());
        ps.setString(9, activity.getLatitude());
        ps.setString(10, activity.getLongitude());
        ps.setInt(11, activity.getCategory().getId());
        if (activity.getGuide() != null)
            ps.setInt(12, activity.getGuide().getId());
        else
            ps.setNull(12, Types.INTEGER);
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) activity.setId(keys.getInt(1));
    }

    public List<Activity> findAll() throws SQLException {
        List<Activity> list = new ArrayList<>();
        String sql = "SELECT a.*, " +
                "ac.name AS cat_name, ac.description AS cat_desc, ac.icon AS cat_icon, " +
                "g.first_name, g.last_name, g.email, g.phone " +
                "FROM activity a " +
                "JOIN activity_category ac ON a.category_id = ac.id " +
                "LEFT JOIN guide g ON a.guide_id = g.id";
        Statement ste = conn.createStatement();
        ResultSet rs = ste.executeQuery(sql);
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    public Activity findById(int id) throws SQLException {
        String sql = "SELECT a.*, " +
                "ac.name AS cat_name, ac.description AS cat_desc, ac.icon AS cat_icon, " +
                "g.first_name, g.last_name, g.email, g.phone " +
                "FROM activity a " +
                "JOIN activity_category ac ON a.category_id = ac.id " +
                "LEFT JOIN guide g ON a.guide_id = g.id " +
                "WHERE a.id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public void update(Activity activity) throws SQLException {
        String sql = "UPDATE activity SET title=?, description=?, price=?, duration_minutes=?, " +
                "location=?, max_participants=?, image=?, is_active=?, latitude=?, longitude=?, " +
                "category_id=?, guide_id=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, activity.getTitle());
        ps.setString(2, activity.getDescription());
        ps.setDouble(3, activity.getPrice());
        ps.setInt(4, activity.getDurationMinutes());
        ps.setString(5, activity.getLocation());
        ps.setInt(6, activity.getMaxParticipants());
        ps.setString(7, activity.getImage());
        ps.setBoolean(8, activity.isActive());
        ps.setString(9, activity.getLatitude());
        ps.setString(10, activity.getLongitude());
        ps.setInt(11, activity.getCategory().getId());
        if (activity.getGuide() != null)
            ps.setInt(12, activity.getGuide().getId());
        else
            ps.setNull(12, Types.INTEGER);
        ps.setInt(13, activity.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM activity WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Activity mapRow(ResultSet rs) throws SQLException {
        Activity a = new Activity();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        a.setPrice(rs.getDouble("price"));
        a.setDurationMinutes(rs.getInt("duration_minutes"));
        a.setLocation(rs.getString("location"));
        a.setMaxParticipants(rs.getInt("max_participants"));
        a.setImage(rs.getString("image"));
        a.setActive(rs.getBoolean("is_active"));
        a.setLatitude(rs.getString("latitude"));
        a.setLongitude(rs.getString("longitude"));

        // map category
        ActivityCategory cat = new ActivityCategory();
        cat.setId(rs.getInt("category_id"));
        cat.setName(rs.getString("cat_name"));
        cat.setDescription(rs.getString("cat_desc"));
        cat.setIcon(rs.getString("cat_icon"));
        a.setCategory(cat);

        // map guide (nullable)
        int guideId = rs.getInt("guide_id");
        if (!rs.wasNull()) {
            Guide g = new Guide();
            g.setId(guideId);
            g.setFirstName(rs.getString("first_name"));
            g.setLastName(rs.getString("last_name"));
            g.setEmail(rs.getString("email"));
            g.setPhone(rs.getString("phone"));
            a.setGuide(g);
        }

        return a;
    }


}
