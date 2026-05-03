package tn.esprit.services.transport;

import tn.esprit.models.transport.Transport;
import tn.esprit.repository.transport.TransportRepository;
import tn.esprit.services.TwilioService;

import java.sql.SQLException;
import java.util.List;

public class TransportService {

    private final TransportRepository repository;
    private final TwilioService twilioService;

    public TransportService() {
        this.repository = new TransportRepository();
        this.twilioService = new TwilioService();
    }

    public void ajouter(Transport transport) throws SQLException {
        validate(transport);
        validateUniqueness(transport);
        repository.save(transport);
        notifyIfDriverAssigned(transport, null);
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
        Transport existingTransport = repository.findById(transport.getId());
        validate(transport);
        validateUniqueness(transport);
        repository.update(transport);
        notifyIfDriverAssigned(transport, existingTransport);
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

    private void validateUniqueness(Transport transport) throws SQLException {
        Integer excludedId = transport.getId() > 0 ? transport.getId() : null;
        if (repository.existsDuplicate(transport, excludedId)) {
            throw new IllegalArgumentException("Un transport avec le meme type, la meme categorie et le meme chauffeur existe deja");
        }
    }

    private void notifyIfDriverAssigned(Transport transport, Transport existingTransport) {
        if (transport.getChauffeur() == null) {
            return;
        }

        Integer previousDriverId = existingTransport == null || existingTransport.getChauffeur() == null
                ? null
                : existingTransport.getChauffeur().getId();

        if (previousDriverId != null && previousDriverId == transport.getChauffeur().getId()) {
            return;
        }

        String transportName = buildTransportLabel(transport);
        twilioService.sendSms(transport.getChauffeur().getFullName(), transportName);
    }

    private String buildTransportLabel(Transport transport) {
        String type = transport.getType() == null ? "" : transport.getType().trim();
        if (!type.isEmpty() && transport.getId() > 0) {
            return type + " (#" + transport.getId() + ")";
        }
        if (!type.isEmpty()) {
            return type;
        }
        if (transport.getId() > 0) {
            return "#" + transport.getId();
        }
        return "unknown transport";
    }
}
