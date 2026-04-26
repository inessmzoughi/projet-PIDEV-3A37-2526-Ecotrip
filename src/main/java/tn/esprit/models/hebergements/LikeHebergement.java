package tn.esprit.models.hebergements;

import java.time.LocalDateTime;

public class LikeHebergement {

    private int id;
    private int userId;
    private int hebergementId;
    private LocalDateTime createdAt;
    private String username; // pour affichage

    public LikeHebergement() {}

    public LikeHebergement(int userId, int hebergementId) {
        this.userId        = userId;
        this.hebergementId = hebergementId;
    }

    /* ─── Getters / Setters ─── */
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public int getUserId()                    { return userId; }
    public void setUserId(int userId)         { this.userId = userId; }
    public int getHebergementId()             { return hebergementId; }
    public void setHebergementId(int id)      { this.hebergementId = id; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime d) { this.createdAt = d; }
    public String getUsername()               { return username; }
    public void setUsername(String u)         { this.username = u; }
}