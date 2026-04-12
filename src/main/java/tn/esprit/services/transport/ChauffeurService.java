package tn.esprit.services.transport;

import tn.esprit.models.transport.Chauffeur;
import tn.esprit.repository.transport.ChauffeurRepository;

import java.sql.SQLException;
import java.util.List;

public class ChauffeurService {

    private final ChauffeurRepository repository;

    public ChauffeurService() {
        this.repository = new ChauffeurRepository();
    }

    public void ajouter(Chauffeur chauffeur) throws SQLException {
        validate(chauffeur);
        repository.save(chauffeur);
    }

    public List<Chauffeur> afficherAll() throws SQLException {
        return repository.findAll();
    }

    public Chauffeur afficherById(int id) throws SQLException {
        Chauffeur chauffeur = repository.findById(id);
        if (chauffeur == null) {
            throw new IllegalArgumentException("Chauffeur not found with id: " + id);
        }
        return chauffeur;
    }

    public void modifier(Chauffeur chauffeur) throws SQLException {
        validate(chauffeur);
        repository.update(chauffeur);
    }

    public void supprimer(int id) throws SQLException {
        repository.delete(id);
    }

    private void validate(Chauffeur chauffeur) {
        if (isBlank(chauffeur.getFirstName()) || chauffeur.getFirstName().trim().length() < 2 || chauffeur.getFirstName().trim().length() > 80) {
            throw new IllegalArgumentException("First name must contain between 2 and 80 characters");
        }
        if (isBlank(chauffeur.getLastName()) || chauffeur.getLastName().trim().length() < 2 || chauffeur.getLastName().trim().length() > 80) {
            throw new IllegalArgumentException("Last name must contain between 2 and 80 characters");
        }
        if (isBlank(chauffeur.getPhone()) || !chauffeur.getPhone().trim().matches("^[0-9+\\s\\-()]{8,30}$")) {
            throw new IllegalArgumentException("Phone number is invalid");
        }
        if (isBlank(chauffeur.getLicenseNumber()) || chauffeur.getLicenseNumber().trim().length() < 5 || chauffeur.getLicenseNumber().trim().length() > 50) {
            throw new IllegalArgumentException("License number must contain between 5 and 50 characters");
        }
        if (chauffeur.getExperience() < 0 || chauffeur.getExperience() > 50) {
            throw new IllegalArgumentException("Experience must be between 0 and 50 years");
        }
        if (chauffeur.getRating() < 0 || chauffeur.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
