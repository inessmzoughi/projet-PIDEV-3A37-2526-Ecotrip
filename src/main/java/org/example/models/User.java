package org.example.models;

import com.mysql.cj.xdevapi.JsonArray;
import org.example.models.enums.Role;

public class User {
    private int id;
    private String username;
    private String email;
    private String password;
    private Role roles;
    private boolean is_verified;
    private String address;
    private String telephone;
    private String image;
    private JsonArray face_descriptor;
    public User (){};
    public User(int id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = Role.User;
        this.is_verified = false;
        this.address = null;
        this.telephone = null;
        this.image = null;
        this.face_descriptor = null;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Role getRoles() {
        return roles;
    }
    public void setRoles(Role roles) {
        this.roles = roles;
    }
    public boolean isIs_verified() {
        return is_verified;
    }
    public void setIs_verified(boolean is_verified) {
        this.is_verified = is_verified;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getTelephone() {
        return telephone;
    }
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public String getFace_descriptor() { return face_descriptor.toString();}
    public void setFace_descriptor(JsonArray face_descriptor) {
        this.face_descriptor = face_descriptor;
    }
}
