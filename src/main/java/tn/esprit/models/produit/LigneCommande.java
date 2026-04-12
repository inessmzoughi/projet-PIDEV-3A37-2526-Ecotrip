package tn.esprit.models.produit;

public class LigneCommande {
    private int id;
    private int commandeId;
    private int productId;
    private int quantite;
    private double unitprice;
    private double subtotal;

    public LigneCommande(int id, int commandeId, int productId, int quantite, double unitprice, double subtotal) {
        this.id = id;
        this.commandeId = commandeId;
        this.productId = productId;
        this.quantite = quantite;
        this.unitprice = unitprice;
        this.subtotal = subtotal;
    }

    public LigneCommande(int commandeId, int productId, int quantite, double unitprice, double subtotal) {
        this.commandeId = commandeId;
        this.productId = productId;
        this.quantite = quantite;
        this.unitprice = unitprice;
        this.subtotal = subtotal;
    }

    public LigneCommande() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(int commandeId) {
        this.commandeId = commandeId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public double getUnitprice() {
        return unitprice;
    }

    public void setUnitprice(double unitprice) {
        this.unitprice = unitprice;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
