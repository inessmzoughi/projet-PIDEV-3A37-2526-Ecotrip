package tn.esprit.services.activity;

import tn.esprit.models.activity.Guide;
import tn.esprit.repository.activity.GuideRepository;

import java.sql.SQLException;
import java.util.List;

public class GuideService {

    private GuideRepository repo;

    public GuideService() {
        repo = new GuideRepository();
    }

    public void ajouter(Guide guide) throws SQLException {
        validate(guide);
        if (repo.existsByEmail(guide.getEmail()))
            throw new IllegalArgumentException(
                    "Un guide avec l'email « " + guide.getEmail() + " » existe déjà.");
        repo.save(guide);
    }

    public List<Guide> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public Guide afficherById(int id) throws SQLException {
        Guide g = repo.findById(id);
        if (g == null) throw new IllegalArgumentException("Guide not found with id: " + id);
        return g;
    }

    public void modifier(Guide guide) throws SQLException {
        validate(guide);
        if (repo.existsByEmailAndNotId(guide.getEmail(), guide.getId()))
            throw new IllegalArgumentException(
                    "Cet email est déjà utilisé par un autre guide.");
        repo.update(guide);
    }

    public void supprimer(int id) throws SQLException {
        repo.delete(id);
    }

    private void validate(Guide guide) {
        if (guide.getFirstName() == null || guide.getFirstName().isBlank())
            throw new IllegalArgumentException("First name is required");
        if (guide.getFirstName().length() < 2)
            throw new IllegalArgumentException("First name must be at least 2 characters");
        if (guide.getLastName() == null || guide.getLastName().isBlank())
            throw new IllegalArgumentException("Last name is required");
        if (guide.getLastName().length() < 2)
            throw new IllegalArgumentException("Last name must be at least 2 characters");
        if (guide.getEmail() == null || guide.getEmail().isBlank())
            throw new IllegalArgumentException("Email is required");
        if (!guide.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            throw new IllegalArgumentException("Email is not valid");
        if (guide.getPhone() == null || guide.getPhone().isBlank())
            throw new IllegalArgumentException("Phone is required");
        if (guide.getPhone().length() < 10)
            throw new IllegalArgumentException("Phone must be at least 10 characters");
        if (guide.getRating() != null && (guide.getRating() < 0 || guide.getRating() > 5))
            throw new IllegalArgumentException("Rating must be between 0 and 5");
    }
}
