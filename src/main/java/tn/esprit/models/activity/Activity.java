package tn.esprit.models.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Activity {

    private int id;
    private String title;
    private String description;
    private double price;
    private int durationMinutes;
    private String location;
    private int maxParticipants;
    private String image;
    private boolean isActive;
    private String latitude;
    private String longitude;
    private ActivityCategory category;
    private Guide guide;
    private List<ActivitySchedule> schedules = new ArrayList<>();

    public Activity() {}

    public Activity(String title, String description, double price, int durationMinutes, String location, int maxParticipants, String image, boolean isActive, ActivityCategory category) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.location = location;
        this.maxParticipants = maxParticipants;
        this.image = image;
        this.isActive = isActive;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public ActivityCategory getCategory() {
        return category;
    }

    public void setCategory(ActivityCategory category) {
        this.category = category;
    }

    public Guide getGuide() {
        return guide;
    }

    public void setGuide(Guide guide) {
        this.guide = guide;
    }

    public List<ActivitySchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ActivitySchedule> schedules) {
        this.schedules = schedules;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Activity activity)) return false;
        return id == activity.id && Double.compare(price, activity.price) == 0 && durationMinutes == activity.durationMinutes && maxParticipants == activity.maxParticipants && isActive == activity.isActive && Objects.equals(title, activity.title) && Objects.equals(description, activity.description) && Objects.equals(location, activity.location) && Objects.equals(image, activity.image) && Objects.equals(latitude, activity.latitude) && Objects.equals(longitude, activity.longitude) && Objects.equals(category, activity.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, price, durationMinutes, location, isActive, category);
    }

    @Override
    public String toString() {return title;}
}
