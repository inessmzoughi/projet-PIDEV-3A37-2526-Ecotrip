package tn.esprit.models.activity;

import java.time.LocalDateTime;

public class ActivityMetrics {

    private int bookingCount;
    private int confirmedBookingCount;
    private int participantsBooked;
    private int confirmedParticipants;
    private double confirmedRevenue;
    private int futureScheduleCount;
    private int totalPlannedCapacity;
    private int remainingSpots;
    private int occupancyRate;
    private LocalDateTime nextDeparture;

    public int getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(int bookingCount) {
        this.bookingCount = bookingCount;
    }

    public int getConfirmedBookingCount() {
        return confirmedBookingCount;
    }

    public void setConfirmedBookingCount(int confirmedBookingCount) {
        this.confirmedBookingCount = confirmedBookingCount;
    }

    public int getParticipantsBooked() {
        return participantsBooked;
    }

    public void setParticipantsBooked(int participantsBooked) {
        this.participantsBooked = participantsBooked;
    }

    public int getConfirmedParticipants() {
        return confirmedParticipants;
    }

    public void setConfirmedParticipants(int confirmedParticipants) {
        this.confirmedParticipants = confirmedParticipants;
    }

    public double getConfirmedRevenue() {
        return confirmedRevenue;
    }

    public void setConfirmedRevenue(double confirmedRevenue) {
        this.confirmedRevenue = confirmedRevenue;
    }

    public int getFutureScheduleCount() {
        return futureScheduleCount;
    }

    public void setFutureScheduleCount(int futureScheduleCount) {
        this.futureScheduleCount = futureScheduleCount;
    }

    public int getTotalPlannedCapacity() {
        return totalPlannedCapacity;
    }

    public void setTotalPlannedCapacity(int totalPlannedCapacity) {
        this.totalPlannedCapacity = totalPlannedCapacity;
    }

    public int getRemainingSpots() {
        return remainingSpots;
    }

    public void setRemainingSpots(int remainingSpots) {
        this.remainingSpots = remainingSpots;
    }

    public int getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(int occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public LocalDateTime getNextDeparture() {
        return nextDeparture;
    }

    public void setNextDeparture(LocalDateTime nextDeparture) {
        this.nextDeparture = nextDeparture;
    }
}
