package tn.esprit.models.transport;

public class Transport {
    private int id;
    private String type;
    private int capacite;
    private double emissionCo2;
    private double prixParPersonne;
    private boolean disponible;
    private String image;
    private TransportCategory category;
    private Chauffeur chauffeur;

    public Transport() {
    }

    public Transport(int id, String type, int capacite, double emissionCo2, double prixParPersonne, boolean disponible, String image, TransportCategory category, Chauffeur chauffeur) {
        this.id = id;
        this.type = type;
        this.capacite = capacite;
        this.emissionCo2 = emissionCo2;
        this.prixParPersonne = prixParPersonne;
        this.disponible = disponible;
        this.image = image;
        this.category = category;
        this.chauffeur = chauffeur;
    }

    public Transport(String type, int capacite, double emissionCo2, double prixParPersonne, boolean disponible, String image, TransportCategory category, Chauffeur chauffeur) {
        this.type = type;
        this.capacite = capacite;
        this.emissionCo2 = emissionCo2;
        this.prixParPersonne = prixParPersonne;
        this.disponible = disponible;
        this.image = image;
        this.category = category;
        this.chauffeur = chauffeur;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public double getEmissionCo2() {
        return emissionCo2;
    }

    public void setEmissionCo2(double emissionCo2) {
        this.emissionCo2 = emissionCo2;
    }

    public double getPrixParPersonne() {
        return prixParPersonne;
    }

    public void setPrixParPersonne(double prixParPersonne) {
        this.prixParPersonne = prixParPersonne;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public TransportCategory getCategory() {
        return category;
    }

    public void setCategory(TransportCategory category) {
        this.category = category;
    }

    public Chauffeur getChauffeur() {
        return chauffeur;
    }

    public void setChauffeur(Chauffeur chauffeur) {
        this.chauffeur = chauffeur;
    }
}
