package tn.esprit.models.hebergements;

public class Hebergement {
    private int id;
    private String nom;
    private String description;
    private String adresse;
    private String ville;
    private int nb_etoiles;
    private String image_principale;
    private String label_eco;
    private double latitude;
    private double longitude;
    private int actif;
    private int categorie_id;
    private int propietaire_id;
    @SuppressWarnings("unused")
    public Hebergement(){}
    public Hebergement(int id, String nom, String description, String adresse, String ville, int nb_etoiles, String image_principale, String label_eco, double latitude, double longitude, int actif, int categorie_id, int propietaire_id) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.adresse = adresse;
        this.ville = ville;
        this.nb_etoiles = nb_etoiles;
        this.image_principale = image_principale;
        this.label_eco = label_eco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.actif = actif;
        this.categorie_id = categorie_id;
        this.propietaire_id = propietaire_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public int getNb_etoiles() {
        return nb_etoiles;
    }

    public void setNb_etoiles(int nb_etoiles) {
        this.nb_etoiles = nb_etoiles;
    }

    public String getImage_principale() {
        return image_principale;
    }

    public void setImage_principale(String image_principale) {
        this.image_principale = image_principale;
    }

    public String getLabel_eco() {
        return label_eco;
    }

    public void setLabel_eco(String label_eco) {
        this.label_eco = label_eco;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getActif() {
        return actif;
    }

    public void setActif(int actif) {
        this.actif = actif;
    }

    public int getCategorie_id() {
        return categorie_id;
    }

    public void setCategorie_id(int categorie_id) {
        this.categorie_id = categorie_id;
    }

    public int getPropietaire_id() {
        return propietaire_id;
    }

    public void setPropietaire_id(int propietaire_id) {
        this.propietaire_id = propietaire_id;
    }
}
