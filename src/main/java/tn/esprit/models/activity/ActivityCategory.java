package tn.esprit.models.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ActivityCategory {

    private int id;
    private String name;
    private String description;
    private String icon;
    private List<Activity> activities = new ArrayList<>();


    public ActivityCategory() {}

    public ActivityCategory(String name, String description, String icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    @Override
    public String toString() {return name;}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ActivityCategory that)) return false;
        return id == that.id && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
