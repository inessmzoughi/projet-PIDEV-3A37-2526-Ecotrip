package tn.esprit.services;

import tn.esprit.models.Reservation;
import tn.esprit.models.cart.CartItem;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.repository.ReservationRepository;
import tn.esprit.session.SessionManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReservationService {

    private final ReservationRepository repo = new ReservationRepository();

    // Convert a CartItem → Reservation and persist (called after payment confirmation)
    public void finalizeCartItem(CartItem item) throws SQLException {
        Reservation r = new Reservation();
        r.setUserId(SessionManager.getInstance().getCurrentUser().getId());
        r.setReservationType(item.getType());
        r.setReservationId(item.getItemId());
        r.setTotalPrice(item.getTotalPrice());
        r.setStatus(ReservationStatus.CONFIRMED);
        r.setDateFrom(item.getDateFrom());
        r.setDateTo(item.getDateTo());
        r.setNumberOfPersons(item.getNumberOfPersons());
        r.setDetails(item.getDetails());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        repo.save(r);
    }

    // Finalize all reservation items in the cart
    public void finalizeAllReservations(List<CartItem> items) throws SQLException {
        for (CartItem item : items) finalizeCartItem(item);
    }

    public List<Reservation> getMyReservations() throws SQLException {
        return repo.findByUser(SessionManager.getInstance().getCurrentUser().getId());
    }

    public List<Reservation> getAllReservations() throws SQLException {
        return repo.findAll();
    }

    public List<Reservation> getWithFilters(ReservationStatus status, ReservationType type,
                                            java.time.LocalDate dateFrom, java.time.LocalDate dateTo)
            throws SQLException {
        return repo.findWithFilters(status, type, null, dateFrom, dateTo);
    }

    public Map<String, Object> getStats() throws SQLException {
        return repo.getStats();
    }

    public void delete(int id) throws SQLException {
        repo.delete(id);
    }

    public void updateStatus(int id, ReservationStatus status) throws SQLException {
        repo.updateStatus(id, status);
    }
}