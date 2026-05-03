package tn.esprit.models.hebergements;

import java.time.LocalDateTime;

public class Avis {

    private int           id;
    private int           userId;
    private int           hebergementId;
    private String        commentaire;
    private String        imagePath;
    private String        statut;       // EN_ATTENTE / APPROUVE / REJETE
    private LocalDateTime createdAt;
    private String        username;

    public Avis() {}

    public Avis(int userId, int hebergementId,
                String commentaire, String imagePath) {
        this.userId         = userId;
        this.hebergementId  = hebergementId;
        this.commentaire    = commentaire;
        this.imagePath      = imagePath;
        this.statut         = "EN_ATTENTE";
    }

    /* ─── Getters / Setters ─── */
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public int getUserId()                    { return userId; }
    public void setUserId(int u)              { this.userId = u; }
    public int getHebergementId()             { return hebergementId; }
    public void setHebergementId(int h)       { this.hebergementId = h; }
    public String getCommentaire()            { return commentaire; }
    public void setCommentaire(String c)      { this.commentaire = c; }
    public String getImagePath()              { return imagePath; }
    public void setImagePath(String p)        { this.imagePath = p; }
    public String getStatut()                 { return statut; }
    public void setStatut(String s)           { this.statut = s; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime d) { this.createdAt = d; }
    public String getUsername()               { return username; }
    public void setUsername(String u)         { this.username = u; }
}