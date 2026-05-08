package tn.esprit.models.hebergements;

// models/hebergements/HebergementImage.java
public class HebergementImage {
    private int    id;
    private int    hebergementId;
    private String url;
    private String legende;   // optionnel
    private int    ordre;     // pour trier

    public HebergementImage(int id, int ordre, String legende, String url, int hebergementId) {
        this.id = id;
        this.ordre = ordre;
        this.legende = legende;
        this.url = url;
        this.hebergementId = hebergementId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLegende() {
        return legende;
    }

    public void setLegende(String legende) {
        this.legende = legende;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getHebergementId() {
        return hebergementId;
    }

    public void setHebergementId(int hebergementId) {
        this.hebergementId = hebergementId;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    // constructeurs + getters/setters
}