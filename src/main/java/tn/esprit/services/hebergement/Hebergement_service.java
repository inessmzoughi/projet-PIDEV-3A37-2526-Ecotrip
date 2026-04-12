package tn.esprit.services.hebergement;

import tn.esprit.database.Base;
import tn.esprit.interfaces.I_service;
import tn.esprit.models.Hebergement;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Hebergement_service implements I_service<Hebergement> {

    private final Connection connection = Base.getInstance().getConnection();

    private void setHebergementParams(PreparedStatement ps, Hebergement h) throws SQLException {
        ps.setString(1, h.getNom());
        ps.setString(2, h.getDescription());
        ps.setString(3, h.getAdresse());
        ps.setString(4, h.getVille());
        ps.setInt(5, h.getNb_etoiles());
        ps.setString(6, h.getImage_principale());
        ps.setString(7, h.getLabel_eco());
        ps.setDouble(8, h.getLatitude());
        ps.setDouble(9, h.getLongitude());
        ps.setInt(10, h.getActif());
        ps.setInt(11, h.getCategorie_id());
        ps.setInt(12, h.getPropietaire_id());
    }

    // Méthode interface (void)
    @Override
    public void ajouter(Hebergement h) throws SQLException {
        ajouterAvecId(h);
    }

    // Méthode spécifique qui retourne l'ID généré
    public int ajouterAvecId(Hebergement h) throws SQLException {
        String sql = "INSERT INTO hebergement (nom, description, adresse, ville, nb_etoiles, image_principale, label_eco, latitude, longitude, actif, categorie_id, propietaire_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        setHebergementParams(ps, h);
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) return keys.getInt(1);
        return -1;
    }

    @Override
    public List<Hebergement> getAll() throws SQLException {
        List<Hebergement> list = new ArrayList<>();
        String sql = "SELECT * FROM hebergement";
        ResultSet rs = connection.createStatement().executeQuery(sql);
        while (rs.next()) list.add(mapResultSet(rs));
        return list;
    }

    public Hebergement getById(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM hebergement WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapResultSet(rs);
        return null;
    }

    @Override
    public void modifier(Hebergement h) throws SQLException {
        String sql = "UPDATE hebergement SET nom=?, description=?, adresse=?, ville=?, nb_etoiles=?, image_principale=?, label_eco=?, latitude=?, longitude=?, actif=?, categorie_id=?, propietaire_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        setHebergementParams(ps, h);
        ps.setInt(13, h.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM hebergement WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Hebergement mapResultSet(ResultSet rs) throws SQLException {
        return new Hebergement(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getString("adresse"),
                rs.getString("ville"),
                rs.getInt("nb_etoiles"),
                rs.getString("image_principale"),
                rs.getString("label_eco"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getInt("actif"),
                rs.getInt("categorie_id"),
                rs.getInt("propietaire_id")
        );
    }

    public int countTotal() throws SQLException {
        ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM hebergement");
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    public double avgEtoiles() throws SQLException {
        ResultSet rs = connection.createStatement()
                .executeQuery("SELECT AVG(nb_etoiles) FROM hebergement");
        if (rs.next()) return rs.getDouble(1);
        return 0;
    }

    public int countActifs() throws SQLException {
        ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM hebergement WHERE actif = 1");
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    public Map<String, Integer> getPropietairesMap() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("— Aucun —", 0);
        ResultSet rs = connection.createStatement()
                .executeQuery("SELECT id, username FROM user");
        while (rs.next())
            map.put(rs.getString("username"), rs.getInt("id"));
        return map;
    }
}