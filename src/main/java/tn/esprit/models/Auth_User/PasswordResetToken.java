package tn.esprit.models.Auth_User;

import java.time.LocalDateTime;

public class PasswordResetToken {

    private int           id;
    private int           userId;
    private String        token;
    private LocalDateTime expiresAt;
    private boolean       used;
    private LocalDateTime createdAt;

    public PasswordResetToken() {}

    // ── Getters ────────────────────────────────────────
    public int           getId()        { return id; }
    public int           getUserId()    { return userId; }
    public String        getToken()     { return token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean       isUsed()       { return used; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ────────────────────────────────────────
    public void setId(int id)                        { this.id = id; }
    public void setUserId(int userId)                { this.userId = userId; }
    public void setToken(String token)               { this.token = token; }
    public void setExpiresAt(LocalDateTime expiresAt){ this.expiresAt = expiresAt; }
    public void setUsed(boolean used)                { this.used = used; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    /** True if token is still valid (not expired and not used) */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}