package tn.esprit.models.activity;

import java.time.LocalDateTime;
import java.util.Objects;

public class ActivitySchedule {

    private int id;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private int availableSpots;
    private Activity activity;

    public ActivitySchedule() {}

    public ActivitySchedule(LocalDateTime startAt, LocalDateTime endAt,
                            int availableSpots, Activity activity) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.availableSpots = availableSpots;
        this.activity = activity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }

    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }

    public int getAvailableSpots() { return availableSpots; }
    public void setAvailableSpots(int availableSpots) { this.availableSpots = availableSpots; }

    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivitySchedule)) return false;
        ActivitySchedule that = (ActivitySchedule) o;
        return id == that.id &&
                availableSpots == that.availableSpots &&
                Objects.equals(startAt, that.startAt) &&
                Objects.equals(endAt, that.endAt) &&
                Objects.equals(activity, that.activity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startAt, endAt, availableSpots, activity);
    }

    @Override
    public String toString() { return activity.getTitle() + " | " + startAt; }
}

