package org.example.models.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Guide {

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String bio;
    private Float rating;
    private String photo;
    private List<Activity> activities = new ArrayList<>();

    public Guide() {}

    public Guide(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Float getRating() { return rating; }
    public void setRating(Float rating) { this.rating = rating; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guide)) return false;
        Guide that = (Guide) o;
        return id == that.id && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() { return Objects.hash(id, email); }

    @Override
    public String toString() { return firstName + " " + lastName; }
}

