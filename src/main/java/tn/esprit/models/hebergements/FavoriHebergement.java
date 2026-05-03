package tn.esprit.models.hebergements;

import java.time.LocalDateTime;

public class FavoriHebergement {

    private int            id;
    private int            userId;
    private int            hebergementId;
    private LocalDateTime  createdAt;

    // Données jointes (pour affichage)
    private Hebergement    hebergement;

    /* ─── Constructeurs ─── */
    public FavoriHebergement() {}

    public FavoriHebergement(int userId, int hebergementId) {
        this.userId         = userId;
        this.hebergementId  = hebergementId;
    }

    /* ─── Getters / Setters ─── */
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getUserId()                      { return userId; }
    public void setUserId(int userId)           { this.userId = userId; }

    public int getHebergementId()               { return hebergementId; }
    public void setHebergementId(int hid)       { this.hebergementId = hid; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public Hebergement getHebergement()         { return hebergement; }
    public void setHebergement(Hebergement h)   { this.hebergement = h; }

    @Override
    public String toString() {
        return "FavoriHebergement{id=" + id
                + ", userId=" + userId
                + ", hebergementId=" + hebergementId + "}";
    }
}