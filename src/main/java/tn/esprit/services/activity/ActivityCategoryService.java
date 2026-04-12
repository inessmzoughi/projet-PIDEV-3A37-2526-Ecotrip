package tn.esprit.services.activity;

import org.example.models.activity.ActivityCategory;
import org.example.repository.activity.ActivityCategoryRepository;

import java.sql.SQLException;
import java.util.List;

public class ActivityCategoryService {

    private ActivityCategoryRepository repo;

    public ActivityCategoryService() {
        repo = new ActivityCategoryRepository();
    }

    public void ajouter(ActivityCategory category) throws SQLException {
        if (category.getName() == null || category.getName().isBlank())
            throw new IllegalArgumentException("Category name is required");
        if (category.getName().length() < 3)
            throw new IllegalArgumentException("Category name must be at least 3 characters");
        repo.save(category);
    }

    public List<ActivityCategory> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public ActivityCategory afficherById(int id) throws SQLException {
        ActivityCategory c = repo.findById(id);
        if (c == null) throw new IllegalArgumentException("Category not found with id: " + id);
        return c;
    }

    public void modifier(ActivityCategory category) throws SQLException {
        if (category.getName() == null || category.getName().isBlank())
            throw new IllegalArgumentException("Category name is required");
        if (category.getName().length() < 3)
            throw new IllegalArgumentException("Category name must be at least 3 characters");
        repo.update(category);
    }

    public void supprimer(int id) throws SQLException {
        repo.delete(id);
    }
}
