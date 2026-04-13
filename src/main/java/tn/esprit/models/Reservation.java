package tn.esprit.models;

import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class Reservation {

    private int id;
    private int userId;                      // FK → user.id
    private ReservationType reservationType; // HEBERGEMENT | ACTIVITY | TRANSPORT
    private int reservationId;               // FK → the booked item's id
    private double totalPrice;
    private ReservationStatus status;        // PENDING | CONFIRMED | CANCELLED
    private LocalDate dateFrom;
    private LocalDate dateTo;               // nullable (activities may not need it)
    private int numberOfPersons;
    private Map<String, Object> details;    // JSON column — nights, guests, etc.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Reservation() {
        this.status         = ReservationStatus.PENDING;
        this.createdAt      = LocalDateTime.now();
        this.updatedAt      = LocalDateTime.now();
        this.numberOfPersons = 1;
    }

    // ── Getters ────────────────────────────────────────
    public int getId()                        { return id; }
    public int getUserId()                    { return userId; }
    public String getUsername(int id)                {
        UserRepository repo = new UserRepository();
        User u = repo.findById(id).get();
        return u.getUsername();
    }
    public ReservationType getReservationType() { return reservationType; }
    public int getReservationId()             { return reservationId; }
    public double getTotalPrice()             { return totalPrice; }
    public ReservationStatus getStatus()      { return status; }
    public LocalDate getDateFrom()            { return dateFrom; }
    public LocalDate getDateTo()              { return dateTo; }
    public int getNumberOfPersons()           { return numberOfPersons; }
    public Map<String, Object> getDetails()   { return details; }
    public String getDetailsAsJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(details);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    // ── Setters ────────────────────────────────────────
    public void setId(int id)                                    { this.id = id; }
    public void setUserId(int userId)                            { this.userId = userId; }
    public void setReservationType(ReservationType t)            { this.reservationType = t; }
    public void setReservationId(int reservationId)              { this.reservationId = reservationId; }
    public void setTotalPrice(double totalPrice)                 { this.totalPrice = totalPrice; }
    public void setStatus(ReservationStatus status)              { this.status = status; this.updatedAt = LocalDateTime.now(); }
    public void setDateFrom(LocalDate dateFrom)                  { this.dateFrom = dateFrom; }
    public void setDateTo(LocalDate dateTo)                      { this.dateTo = dateTo; }
    public void setNumberOfPersons(int numberOfPersons)          { this.numberOfPersons = numberOfPersons; }
    public void setDetails(Map<String, Object> details)          { this.details = details; }
    public void setCreatedAt(LocalDateTime createdAt)            { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)            { this.updatedAt = updatedAt; }

    // ── Convenience: nights from dates ────────────────
    public long getNights() {
        if (dateFrom == null || dateTo == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo);
    }
}