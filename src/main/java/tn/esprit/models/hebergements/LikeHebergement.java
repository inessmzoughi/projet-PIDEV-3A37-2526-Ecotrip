package tn.esprit.models.hebergements;

import java.time.LocalDateTime;

public class LikeHebergement {

    public enum Type { like, dislike }

    private int            id;
    private int            userId;
    private int            hebergementId;
    private Type           type;
    private LocalDateTime  createdAt;

    /* ─── Constructeurs ─── */
    public LikeHebergement() {}

    public LikeHebergement(int userId, int hebergementId, Type type) {
        this.userId         = userId;
        this.hebergementId  = hebergementId;
        this.type           = type;
    }

    /* ─── Getters / Setters ─── */
    public int           getId()             { return id; }
    public void          setId(int id)       { this.id = id; }

    public int           getUserId()                  { return userId; }
    public void          setUserId(int userId)        { this.userId = userId; }

    public int           getHebergementId()                       { return hebergementId; }
    public void          setHebergementId(int hebergementId)      { this.hebergementId = hebergementId; }

    public Type          getType()               { return type; }
    public void          setType(Type type)      { this.type = type; }

    public LocalDateTime getCreatedAt()                      { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isLike()    { return type == Type.like; }
    public boolean isDislike() { return type == Type.dislike; }

    @Override
    public String toString() {
        return "LikeHebergement{id=" + id
                + ", userId=" + userId
                + ", hebergementId=" + hebergementId
                + ", type=" + type + "}";
    }
}