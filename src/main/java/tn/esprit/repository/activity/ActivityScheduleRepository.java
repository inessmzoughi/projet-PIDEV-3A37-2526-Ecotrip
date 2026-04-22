package tn.esprit.repository.activity;

import tn.esprit.database.Base;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivitySchedule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityScheduleRepository {

    private Connection conn;
    private ActivityRepository activityRepo;

    public ActivityScheduleRepository() {
        conn = Base.getInstance().getConnection();
        activityRepo = new ActivityRepository();
    }

    public void save(ActivitySchedule schedule) throws SQLException {
        String sql = "INSERT INTO activity_schedule (start_at, end_at, available_spots, activity_id) " +
                "VALUES (?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setTimestamp(1, Timestamp.valueOf(schedule.getStartAt()));
        ps.setTimestamp(2, Timestamp.valueOf(schedule.getEndAt()));
        ps.setInt(3, schedule.getAvailableSpots());
        ps.setInt(4, schedule.getActivity().getId());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) schedule.setId(keys.getInt(1));
    }

    public List<ActivitySchedule> findAll() throws SQLException {
        List<ActivitySchedule> list = new ArrayList<>();
        String sql = "SELECT s.*, a.title AS activity_title FROM activity_schedule s " +
                "JOIN activity a ON s.activity_id = a.id";
        Statement ste = conn.createStatement();
        ResultSet rs = ste.executeQuery(sql);
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    public List<ActivitySchedule> findByActivityId(int activityId) throws SQLException {
        List<ActivitySchedule> list = new ArrayList<>();
        String sql = "SELECT s.*, a.title AS activity_title FROM activity_schedule s " +
                "JOIN activity a ON s.activity_id = a.id WHERE s.activity_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, activityId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    public void update(ActivitySchedule schedule) throws SQLException {
        String sql = "UPDATE activity_schedule SET start_at=?, end_at=?, available_spots=?, activity_id=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setTimestamp(1, Timestamp.valueOf(schedule.getStartAt()));
        ps.setTimestamp(2, Timestamp.valueOf(schedule.getEndAt()));
        ps.setInt(3, schedule.getAvailableSpots());
        ps.setInt(4, schedule.getActivity().getId());
        ps.setInt(5, schedule.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM activity_schedule WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ── Unicité / chevauchement ────────────────────────────────────────────────

    public boolean hasOverlap(int activityId,
                              java.time.LocalDateTime startAt,
                              java.time.LocalDateTime endAt,
                              int excludeId) throws SQLException {
        String sql =
                "SELECT COUNT(*) FROM activity_schedule " +
                        "WHERE activity_id = ? " +
                        "AND start_at < ? " +
                        "AND end_at   > ? " +
                        "AND id != ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, activityId);
        ps.setTimestamp(2, Timestamp.valueOf(endAt));
        ps.setTimestamp(3, Timestamp.valueOf(startAt));
        ps.setInt(4, excludeId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    private ActivitySchedule mapRow(ResultSet rs) throws SQLException {
        ActivitySchedule s = new ActivitySchedule();
        s.setId(rs.getInt("id"));
        s.setStartAt(rs.getTimestamp("start_at").toLocalDateTime());
        s.setEndAt(rs.getTimestamp("end_at").toLocalDateTime());
        s.setAvailableSpots(rs.getInt("available_spots"));

        // lightweight activity (just id + title to avoid circular loading)
        Activity a = new Activity();
        a.setId(rs.getInt("activity_id"));
        a.setTitle(rs.getString("activity_title"));
        s.setActivity(a);

        return s;
    }


}
