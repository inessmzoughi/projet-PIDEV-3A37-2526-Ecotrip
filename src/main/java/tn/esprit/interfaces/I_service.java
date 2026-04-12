package tn.esprit.interfaces;

import java.sql.SQLException;
import java.util.List;

public interface I_service<T> {
    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<T> getAll() throws SQLException;
}