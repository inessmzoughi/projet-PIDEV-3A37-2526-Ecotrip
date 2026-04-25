package tn.esprit.models.Auth_User;

public class GoogleUserInfo {

    private String sub;         // Google's unique user ID
    private String email;
    private String name;
    private String picture;     // profile photo URL
    private boolean emailVerified;

    // ── Getters ────────────────────────────────────────
    public String  getSub()           { return sub; }
    public String  getEmail()         { return email; }
    public String  getName()          { return name; }
    public String  getPicture()       { return picture; }
    public boolean isEmailVerified()  { return emailVerified; }

    // ── Setters ────────────────────────────────────────
    public void setSub(String sub)                   { this.sub = sub; }
    public void setEmail(String email)               { this.email = email; }
    public void setName(String name)                 { this.name = name; }
    public void setPicture(String picture)           { this.picture = picture; }
    public void setEmailVerified(boolean v)          { this.emailVerified = v; }
}