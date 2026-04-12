package tn.esprit.models.enums;

public enum Role {
    ROLE_USER,
    ROLE_ADMIN,
    ;

    public String getLabel() {
        return this == Role.ROLE_ADMIN ? "Administrateur" : "Utilisateur";
    }

}
