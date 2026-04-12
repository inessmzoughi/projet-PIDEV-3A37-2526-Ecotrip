package tn.esprit.models.produit;

public class ProductCategory {

    private int id;
    private String nom;
    private String description;

    public ProductCategory(int id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }

    public ProductCategory(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    public ProductCategory() {
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

    @Override
    public String toString() {
        return "ProductCategory{id=" + id
                + ", nom='" + nom + '\''
                + ", description='" + description + '\''
                + '}';
    }
}

