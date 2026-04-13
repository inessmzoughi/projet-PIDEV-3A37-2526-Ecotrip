package tn.esprit.models.hebergements;

public class Chambre {
    private int id;
    private String numero;
    private String type;
    private int capacite;
    private double 	prix_par_nuit;
    private String description;
    private int disponible;
    private int hebergement_id;
    public Chambre(){}
    public Chambre(int id, int hebergement_id, int disponible, String description, double prix_par_nuit, int capacite, String type, String numero) {
        this.id = id;
        this.hebergement_id = hebergement_id;
        this.disponible = disponible;
        this.description = description;
        this.prix_par_nuit = prix_par_nuit;
        this.capacite = capacite;
        this.type = type;
        this.numero = numero;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }




    public String getType() {
        return type;
    }





    public int getCapacite() {
        return capacite;
    }





    public double getPrix_par_nuit() {
        return prix_par_nuit;
    }





    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public int getDisponible() {
        return disponible;
    }




    public int getHebergement_id() {
        return hebergement_id;
    }


    public void setNumero(String s) {this.numero = numero;
    }

    public void setPrix_par_nuit(double v) {
    }
}
