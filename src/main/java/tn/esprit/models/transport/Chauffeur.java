package tn.esprit.models.transport;

import java.util.ArrayList;
import java.util.List;

public class Chauffeur {
    private int id;
    private String firstName;
    private String lastName;
    private String phone;
    private String licenseNumber;
    private int experience;
    private double rating;
    private List<Transport> transports;

    public Chauffeur() {
        this.transports = new ArrayList<>();
        this.experience = 0;
        this.rating = 0.0;
    }

    public Chauffeur(int id, String firstName, String lastName, String phone, String licenseNumber, int experience, double rating) {
        this();
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.experience = experience;
        this.rating = rating;
    }

    public Chauffeur(String firstName, String lastName, String phone, String licenseNumber, int experience, double rating) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.experience = experience;
        this.rating = rating;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public List<Transport> getTransports() {
        return transports;
    }

    public void setTransports(List<Transport> transports) {
        this.transports = transports;
    }

    public void addTransport(Transport transport) {
        if (transport == null || transports.contains(transport)) {
            return;
        }
        transports.add(transport);
        transport.setChauffeur(this);
    }

    public void removeTransport(Transport transport) {
        if (transport == null) {
            return;
        }
        if (transports.remove(transport) && transport.getChauffeur() == this) {
            transport.setChauffeur(null);
        }
    }

    public String getFullName() {
        String first = firstName == null ? "" : firstName;
        String last = lastName == null ? "" : lastName;
        return (first + " " + last).trim();
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
