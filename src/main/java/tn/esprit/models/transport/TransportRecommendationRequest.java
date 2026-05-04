package tn.esprit.models.transport;

public class TransportRecommendationRequest {
    private String origin;
    private String destination;
    private int passengers;
    private Double budgetMin;
    private Double budgetMax;
    private String preference;
    private String comfortLevel;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getPassengers() {
        return passengers;
    }

    public void setPassengers(int passengers) {
        this.passengers = passengers;
    }

    public Double getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }

    public Double getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getComfortLevel() {
        return comfortLevel;
    }

    public void setComfortLevel(String comfortLevel) {
        this.comfortLevel = comfortLevel;
    }
}
