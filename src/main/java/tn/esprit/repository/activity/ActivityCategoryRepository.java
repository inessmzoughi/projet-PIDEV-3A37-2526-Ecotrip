package tn.esprit.repository.activity;

import tn.esprit.database.Base;
import tn.esprit.models.activity.ActivityCategory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityCategoryRepository {

    private Connection conn;

    public ActivityCategoryRepository() {
        conn = Base.getInstance().getConnection();
    }

    public void save(ActivityCategory category) throws SQLException {
        String sql = "INSERT INTO activity_category (name, description, icon) VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, category.getName());
        ps.setString(2, category.getDescription());
        ps.setString(3, category.getIcon());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) category.setId(keys.getInt(1));
    }

    public List<ActivityCategory> findAll() throws SQLException {
        List<ActivityCategory> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_category";
        Statement ste = conn.createStatement();
        ResultSet rs = ste.executeQuery(sql);
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    public ActivityCategory findById(int id) throws SQLException {
        String sql = "SELECT * FROM activity_category WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public void update(ActivityCategory category) throws SQLException {
        String sql = "UPDATE activity_category SET name=?, description=?, icon=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, category.getName());
        ps.setString(2, category.getDescription());
        ps.setString(3, category.getIcon());
        ps.setInt(4, category.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM activity_category WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private ActivityCategory mapRow(ResultSet rs) throws SQLException {
        ActivityCategory c = new ActivityCategory();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setIcon(rs.getString("icon"));
        return c;
    }
}
