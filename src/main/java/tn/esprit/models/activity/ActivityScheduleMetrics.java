package tn.esprit.models.activity;

public class ActivityScheduleMetrics {

    private int reservedParticipants;
    private int confirmedParticipants;
    private int remainingSpots;
    private int fillRate;
    private String availabilityLabel;

    public int getReservedParticipants() {
        return reservedParticipants;
    }

    public void setReservedParticipants(int reservedParticipants) {
        this.reservedParticipants = reservedParticipants;
    }

    public int getConfirmedParticipants() {
        return confirmedParticipants;
    }

    public void setConfirmedParticipants(int confirmedParticipants) {
        this.confirmedParticipants = confirmedParticipants;
    }

    public int getRemainingSpots() {
        return remainingSpots;
    }

    public void setRemainingSpots(int remainingSpots) {
        this.remainingSpots = remainingSpots;
    }

    public int getFillRate() {
        return fillRate;
    }

    public void setFillRate(int fillRate) {
        this.fillRate = fillRate;
    }

    public String getAvailabilityLabel() {
        return availabilityLabel;
    }

    public void setAvailabilityLabel(String availabilityLabel) {
        this.availabilityLabel = availabilityLabel;
    }
}
