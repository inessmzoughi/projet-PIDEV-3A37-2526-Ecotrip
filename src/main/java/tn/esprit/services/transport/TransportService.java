package tn.esprit.services.transport;

import tn.esprit.models.transport.Transport;
import tn.esprit.repository.transport.TransportRepository;

import java.sql.SQLException;
import java.util.List;

public class TransportService {

    private final TransportRepository repository;

    public TransportService() {
        this.repository = new TransportRepository();
    }

    public void ajouter(Transport transport) throws SQLException {
        validate(transport);
        repository.save(transport);
    }

    public List<Transport> afficherAll() throws SQLException {
        return repository.findAll();
    }

    public Transport afficherById(int id) throws SQLException {
        Transport transport = repository.findById(id);
        if (transport == null) {
            throw new IllegalArgumentException("Transport not found with id: " + id);
        }
        return transport;
    }

    public void modifier(Transport transport) throws SQLException {
        validate(transport);
        repository.update(transport);
    }

    public void supprimer(int id) throws SQLException {
        repository.delete(id);
    }

    private void validate(Transport transport) {
        if (transport.getType() == null || transport.getType().isBlank() || transport.getType().trim().length() < 3 || transport.getType().trim().length() > 100) {
            throw new IllegalArgumentException("Transport type must contain between 3 and 100 characters");
        }
        if (transport.getCapacite() < 1 || transport.getCapacite() > 500) {
            throw new IllegalArgumentException("Capacity must be between 1 and 500");
        }
        if (transport.getEmissionCo2() < 0) {
            throw new IllegalArgumentException("CO2 emission must be positive or zero");
        }
        if (transport.getPrixParPersonne() <= 0) {
            throw new IllegalArgumentException("Price per person must be positive");
        }
    }
}
