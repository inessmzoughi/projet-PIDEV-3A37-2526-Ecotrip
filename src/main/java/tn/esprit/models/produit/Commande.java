package tn.esprit.models.produit;

import java.util.Date;

public class Commande {
    private int id;
    private int idUser;
    private int produitId;      // était idProduct → produit_id en BDD
    private int quantite;
    private double prixUnitaire;
    private double total;
    private Date dateCommande;  // était dateDeCommande → date_commande en BDD

    // Constructeur complet (lecture BDD)
    public Commande(int id, int idUser, int produitId, int quantite, double prixUnitaire, double total, Date dateCommande) {
        this.id = id;
        this.idUser = idUser;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.total = total;
        this.dateCommande = dateCommande;
    }

    // Constructeur sans id (création)
    public Commande(int idUser, int produitId, int quantite, double prixUnitaire, double total, Date dateCommande) {
        this.idUser = idUser;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.total = total;
        this.dateCommande = dateCommande;
    }

    public int getId() { return id; }
    public int getIdUser() { return idUser; }
    public int getProduitId() { return produitId; }
    public int getQuantite() { return quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public double getTotal() { return total; }
    public Date getDateCommande() { return dateCommande; }

    public void setId(int id) { this.id = id; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public void setTotal(double total) { this.total = total; }
    public void setDateCommande(Date dateCommande) { this.dateCommande = dateCommande; }

    @Override
    public String toString() {
        return "Commande{id=" + id + ", idUser=" + idUser + ", produitId=" + produitId +
                ", quantite=" + quantite + ", total=" + total + ", date=" + dateCommande + "}";
    }
}