package tn.esprit.services.activity;

import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.repository.activity.ActivityScheduleRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ActivityScheduleService {

    private ActivityScheduleRepository repo;

    public ActivityScheduleService() {
        repo = new ActivityScheduleRepository();
    }

    public void ajouter(ActivitySchedule schedule) throws SQLException {
        validate(schedule);
        if (repo.hasOverlap(
                schedule.getActivity().getId(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                0))
            throw new IllegalArgumentException(
                    "Ce créneau chevauche un créneau existant pour cette activité.");
        repo.save(schedule);
    }

    public List<ActivitySchedule> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public List<ActivitySchedule> afficherByActivity(int activityId) throws SQLException {
        return repo.findByActivityId(activityId);
    }

    public void modifier(ActivitySchedule schedule) throws SQLException {
        validate(schedule);
        if (repo.hasOverlap(
                schedule.getActivity().getId(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getId()))
            throw new IllegalArgumentException(
                    "Ce créneau chevauche un autre créneau existant pour cette activité.");
        repo.update(schedule);
    }

    public void supprimer(int id) throws SQLException {
        repo.delete(id);
    }

    private void validate(ActivitySchedule schedule) {
        if (schedule.getStartAt() == null)
            throw new IllegalArgumentException("Start date is required");
        if (schedule.getEndAt() == null)
            throw new IllegalArgumentException("End date is required");
        if (!schedule.getStartAt().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Start date must be in the future");
        if (!schedule.getEndAt().isAfter(schedule.getStartAt()))
            throw new IllegalArgumentException("End date must be after start date");
        if (schedule.getAvailableSpots() < 1)
            throw new IllegalArgumentException("Available spots must be at least 1");
        if (schedule.getActivity() == null)
            throw new IllegalArgumentException("Activity is required");
    }
}
