package tn.esprit.services.activity;

import tn.esprit.models.Reservation;
import tn.esprit.models.activity.Activity;
import tn.esprit.models.activity.ActivityMetrics;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.models.activity.ActivityScheduleMetrics;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.repository.ReservationRepository;
import tn.esprit.repository.activity.ActivityRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ActivityService {

    private final ActivityRepository repo;
    private final ActivityScheduleService scheduleService;
    private final ReservationRepository reservationRepository;

    public ActivityService() {
        repo = new ActivityRepository();
        scheduleService = new ActivityScheduleService();
        reservationRepository = new ReservationRepository();
    }

    public void ajouter(Activity activity) throws SQLException {
        validate(activity);
        if (repo.existsByTitle(activity.getTitle()))
            throw new IllegalArgumentException(
                    "Une activité intitulée « " + activity.getTitle() + " » existe déjà.");
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
        if (repo.existsByTitleAndNotId(activity.getTitle(), activity.getId()))
            throw new IllegalArgumentException(
                    "Une autre activité porte déjà le titre « " + activity.getTitle() + " ».");
        repo.update(activity);
    }

    public void supprimer(int id) throws SQLException {
        repo.delete(id);
    }

    public ActivityMetrics getMetrics(Activity activity) throws SQLException {
        ActivityMetrics metrics = new ActivityMetrics();
        if (activity == null) {
            return metrics;
        }

        List<ActivitySchedule> schedules = scheduleService.afficherByActivity(activity.getId());
        List<Reservation> reservations = reservationRepository.findAll();

        int bookingCount = 0;
        int confirmedBookingCount = 0;
        int participantsBooked = 0;
        int confirmedParticipants = 0;
        double confirmedRevenue = 0;

        for (Reservation reservation : reservations) {
            if (reservation.getReservationType() != ReservationType.ACTIVITY
                    || reservation.getReservationId() != activity.getId()
                    || reservation.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            bookingCount++;
            int participants = resolveParticipants(reservation);
            participantsBooked += participants;

            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                confirmedBookingCount++;
                confirmedParticipants += participants;
                confirmedRevenue += reservation.getTotalPrice();
            }
        }

        int futureScheduleCount = 0;
        int totalPlannedCapacity = 0;
        int remainingSpots = 0;
        LocalDateTime nextDeparture = null;

        for (ActivitySchedule schedule : schedules) {
            ActivityScheduleMetrics scheduleMetrics = scheduleService.getMetrics(schedule);
            totalPlannedCapacity += Math.max(0, schedule.getAvailableSpots());
            remainingSpots += scheduleMetrics.getRemainingSpots();

            if (schedule.getStartAt() != null && schedule.getStartAt().isAfter(LocalDateTime.now())) {
                futureScheduleCount++;
                if (scheduleMetrics.getRemainingSpots() > 0
                        && (nextDeparture == null || schedule.getStartAt().isBefore(nextDeparture))) {
                    nextDeparture = schedule.getStartAt();
                }
            }
        }

        int occupancyRate = totalPlannedCapacity == 0
                ? 0
                : (int) Math.round((participantsBooked * 100.0) / totalPlannedCapacity);

        metrics.setBookingCount(bookingCount);
        metrics.setConfirmedBookingCount(confirmedBookingCount);
        metrics.setParticipantsBooked(participantsBooked);
        metrics.setConfirmedParticipants(confirmedParticipants);
        metrics.setConfirmedRevenue(confirmedRevenue);
        metrics.setFutureScheduleCount(futureScheduleCount);
        metrics.setTotalPlannedCapacity(totalPlannedCapacity);
        metrics.setRemainingSpots(remainingSpots);
        metrics.setOccupancyRate(Math.min(occupancyRate, 100));
        metrics.setNextDeparture(nextDeparture);
        return metrics;
    }

    private int resolveParticipants(Reservation reservation) {
        Map<String, Object> details = reservation.getDetails() != null
                ? reservation.getDetails()
                : Collections.emptyMap();
        Object participantsValue = details.get("participants");
        if (participantsValue instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (participantsValue instanceof String text) {
            try {
                return Math.max(0, Integer.parseInt(text.trim()));
            } catch (NumberFormatException ignored) {
                return Math.max(0, reservation.getNumberOfPersons());
            }
        }
        return Math.max(0, reservation.getNumberOfPersons());
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
