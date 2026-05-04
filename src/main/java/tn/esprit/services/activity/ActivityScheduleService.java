package tn.esprit.services.activity;

import tn.esprit.models.Reservation;
import tn.esprit.models.activity.ActivitySchedule;
import tn.esprit.models.activity.ActivityScheduleMetrics;
import tn.esprit.models.enums.ReservationStatus;
import tn.esprit.models.enums.ReservationType;
import tn.esprit.repository.ReservationRepository;
import tn.esprit.repository.activity.ActivityScheduleRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ActivityScheduleService {

    private final ActivityScheduleRepository repo;
    private final ReservationRepository reservationRepository;

    public ActivityScheduleService() {
        repo = new ActivityScheduleRepository();
        reservationRepository = new ReservationRepository();
    }

    public void ajouter(ActivitySchedule schedule) throws SQLException {
        validate(schedule);
        if (repo.hasOverlap(
                schedule.getActivity().getId(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                0))
            throw new IllegalArgumentException(
                    "Ce créneau chevauche un créneau existant pour cette activité.");
        repo.save(schedule);
    }

    public List<ActivitySchedule> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public List<ActivitySchedule> afficherByActivity(int activityId) throws SQLException {
        return repo.findByActivityId(activityId);
    }

    public void modifier(ActivitySchedule schedule) throws SQLException {
        validate(schedule);
        if (repo.hasOverlap(
                schedule.getActivity().getId(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getId()))
            throw new IllegalArgumentException(
                    "Ce créneau chevauche un autre créneau existant pour cette activité.");
        repo.update(schedule);
    }

    public void supprimer(int id) throws SQLException {
        repo.delete(id);
    }

    public ActivityScheduleMetrics getMetrics(ActivitySchedule schedule) throws SQLException {
        ActivityScheduleMetrics metrics = new ActivityScheduleMetrics();
        if (schedule == null) {
            return metrics;
        }

        List<Reservation> reservations = reservationRepository.findAll();
        int reservedParticipants = 0;
        int confirmedParticipants = 0;

        for (Reservation reservation : reservations) {
            if (reservation.getReservationType() != ReservationType.ACTIVITY
                    || reservation.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            Map<String, Object> details = reservation.getDetails() != null
                    ? reservation.getDetails()
                    : Collections.emptyMap();

            Integer scheduleId = parseInteger(details.get("scheduleId"));
            if (scheduleId == null || scheduleId != schedule.getId()) {
                continue;
            }

            int participants = resolveParticipants(reservation, details);
            reservedParticipants += participants;
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                confirmedParticipants += participants;
            }
        }

        int plannedCapacity = Math.max(0, schedule.getAvailableSpots());
        int remainingSpots = Math.max(0, plannedCapacity - reservedParticipants);
        int fillRate = plannedCapacity == 0
                ? 0
                : (int) Math.round((reservedParticipants * 100.0) / plannedCapacity);

        metrics.setReservedParticipants(reservedParticipants);
        metrics.setConfirmedParticipants(confirmedParticipants);
        metrics.setRemainingSpots(remainingSpots);
        metrics.setFillRate(Math.min(fillRate, 100));
        metrics.setAvailabilityLabel(buildAvailabilityLabel(schedule, remainingSpots, fillRate));
        return metrics;
    }

    private int resolveParticipants(Reservation reservation, Map<String, Object> details) {
        Integer fromDetails = parseInteger(details.get("participants"));
        if (fromDetails != null && fromDetails > 0) {
            return fromDetails;
        }
        return Math.max(0, reservation.getNumberOfPersons());
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String buildAvailabilityLabel(ActivitySchedule schedule, int remainingSpots, int fillRate) {
        if (schedule.getEndAt() != null && schedule.getEndAt().isBefore(LocalDateTime.now())) {
            return "Termine";
        }
        if (remainingSpots <= 0) {
            return "Complet";
        }
        if (fillRate >= 80) {
            return "Presque complet";
        }
        return "Disponible";
    }

    private void validate(ActivitySchedule schedule) {
        if (schedule.getStartAt() == null)
            throw new IllegalArgumentException("Start date is required");
        if (schedule.getEndAt() == null)
            throw new IllegalArgumentException("End date is required");
        if (!schedule.getStartAt().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Start date must be in the future");
        if (!schedule.getEndAt().isAfter(schedule.getStartAt()))
            throw new IllegalArgumentException("End date must be after start date");
        if (schedule.getAvailableSpots() < 1)
            throw new IllegalArgumentException("Available spots must be at least 1");
        if (schedule.getActivity() == null)
            throw new IllegalArgumentException("Activity is required");
    }
}
