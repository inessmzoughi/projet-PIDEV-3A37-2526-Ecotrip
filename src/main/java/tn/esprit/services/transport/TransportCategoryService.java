package tn.esprit.services.transport;

import tn.esprit.models.transport.TransportCategory;
import tn.esprit.repository.transport.TransportCategoryRepository;

import java.sql.SQLException;
import java.util.List;

public class TransportCategoryService {

    private final TransportCategoryRepository repository;

    public TransportCategoryService() {
        this.repository = new TransportCategoryRepository();
    }

    public void ajouter(TransportCategory category) throws SQLException {
        validate(category);
        repository.save(category);
    }

    public List<TransportCategory> afficherAll() throws SQLException {
        return repository.findAll();
    }

    public TransportCategory afficherById(int id) throws SQLException {
        TransportCategory category = repository.findById(id);
        if (category == null) {
            throw new IllegalArgumentException("Transport category not found with id: " + id);
        }
        return category;
    }

    public void modifier(TransportCategory category) throws SQLException {
        validate(category);
        repository.update(category);
    }

    public void supprimer(int id) throws SQLException {
        repository.delete(id);
    }

    private void validate(TransportCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (category.getName().trim().length() < 2 || category.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Category name must contain between 2 and 100 characters");
        }
        if (category.getDescription() != null && category.getDescription().length() > 2000) {
            throw new IllegalArgumentException("Category description must not exceed 2000 characters");
        }
    }
}
