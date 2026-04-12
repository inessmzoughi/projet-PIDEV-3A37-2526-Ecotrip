package tn.esprit.models.produit;

public class Product {
    private int id;
    private String nom;
    private double prix;
    private int stock;
    private int productCategoryId;
    private String image;

    public Product(int id, String nom, double prix, int stock, int productCategoryId, String image) {
        this.id = id;
        this.nom = nom;
        this.prix = prix;
        this.stock = stock;
        this.productCategoryId = productCategoryId;
        this.image = image;
    }

    public Product(String nom, double prix, int stock, int productCategoryId, String image) {
        this.nom = nom;
        this.prix = prix;
        this.stock = stock;
        this.productCategoryId = productCategoryId;
        this.image = image;
    }

    public Product() {
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

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getProductCategoryId() {
        return productCategoryId;
    }

    public void setProductCategoryId(int productCategoryId) {
        this.productCategoryId = productCategoryId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
