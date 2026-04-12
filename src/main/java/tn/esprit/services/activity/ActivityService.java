package tn.esprit.services.activity;

import org.example.models.activity.Activity;
import org.example.repository.activity.ActivityRepository;

import java.sql.SQLException;
import java.util.List;

public class ActivityService {

    private ActivityRepository repo;

    public ActivityService() {
        repo = new ActivityRepository();
    }

    public void ajouter(Activity activity) throws SQLException {
        validate(activity);
        repo.save(activity);
    }

    public List<Activity> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public Activity afficherById(int id) throws SQLException {
        Activity a = repo.findById(id);
        if (a == null) throw new IllegalArgumentException("Activity not found with id: " + id);
        return a;
    }

    public void modifier(Activity activity) throws SQLException {
        validate(activity);
        repo.update(activity);
    }

    public void supprimer(int id) throws SQLException {
        repo.delete(id);
    }

    private void validate(Activity activity) {
        if (activity.getTitle() == null || activity.getTitle().isBlank())
            throw new IllegalArgumentException("Title is required");
        if (activity.getTitle().length() < 3 || activity.getTitle().length() > 150)
            throw new IllegalArgumentException("Title must be between 3 and 150 characters");
        if (activity.getDescription() == null || activity.getDescription().isBlank())
            throw new IllegalArgumentException("Description is required");
        if (activity.getDescription().length() < 10)
            throw new IllegalArgumentException("Description must be at least 10 characters");
        if (activity.getPrice() <= 0)
            throw new IllegalArgumentException("Price must be positive");
        if (activity.getDurationMinutes() < 5 || activity.getDurationMinutes() > 1440)
            throw new IllegalArgumentException("Duration must be between 5 and 1440 minutes");
        if (activity.getLocation() == null || activity.getLocation().isBlank())
            throw new IllegalArgumentException("Location is required");
        if (activity.getLocation().length() < 3)
            throw new IllegalArgumentException("Location must be at least 3 characters");
        if (activity.getMaxParticipants() < 1)
            throw new IllegalArgumentException("Max participants must be at least 1");
        if (activity.getCategory() == null)
            throw new IllegalArgumentException("Category is required");
    }
}
