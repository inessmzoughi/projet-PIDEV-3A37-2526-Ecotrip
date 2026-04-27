package tn.esprit.services.hebergement;

import tn.esprit.models.hebergements.Avis;
import tn.esprit.database.Base;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AvisHebergement_service {

    private final Connection cnx = Base.getInstance().getConnection();

    /* ─── Ajouter avis (statut EN_ATTENTE par défaut) ─── */
    public void ajouter(Avis avis) throws SQLException {
        String sql = "INSERT INTO avis_hebergement "
                + "(user_id, hebergement_id, commentaire, image_path, statut) "
                + "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, avis.getUserId());
            ps.setInt(2, avis.getHebergementId());
            ps.setString(3, avis.getCommentaire());
            ps.setString(4, avis.getImagePath());
            ps.setString(5, "EN_ATTENTE");
            ps.executeUpdate();
        }
    }

    /* ─── Modifier avis ─── */
    public void modifier(Avis avis) throws SQLException {
        String sql = "UPDATE avis_hebergement "
                + "SET commentaire=?, image_path=?, statut='EN_ATTENTE' "
                + "WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, avis.getCommentaire());
            ps.setString(2, avis.getImagePath());
            ps.setInt(3, avis.getId());
            ps.executeUpdate();
        }
    }

    /* ─── Supprimer avis ─── */
    public void supprimer(int avisId) throws SQLException {
        String sql = "DELETE FROM avis_hebergement WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, avisId);
            ps.executeUpdate();
        }
    }

    /* ─── Approuver avis ─── */
    public void approuver(int avisId) throws SQLException {
        changerStatut(avisId, "APPROUVE");
    }

    /* ─── Rejeter avis ─── */
    public void rejeter(int avisId) throws SQLException {
        supprimer(avisId);
    }

    private void changerStatut(int avisId, String statut) throws SQLException {
        String sql = "UPDATE avis_hebergement SET statut=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, avisId);
            ps.executeUpdate();
        }
    }

    /* ─── Avis APPROUVÉS d'un hébergement (côté front) ─── */
    public List<Avis> getApprouvesByHebergement(int hebergementId)
            throws SQLException {
        return getByHebergementAndStatut(hebergementId, "APPROUVE");
    }

    /* ─── Avis EN_ATTENTE (côté admin) ─── */
    public List<Avis> getEnAttente() throws SQLException {
        List<Avis> list = new ArrayList<>();
        String sql = "SELECT a.*, u.username, h.nom as heb_nom "
                + "FROM avis_hebergement a "
                + "JOIN user u ON a.user_id = u.id "
                + "JOIN hebergement h ON a.hebergement_id = h.id "
                + "WHERE a.statut = 'EN_ATTENTE' "
                + "ORDER BY a.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Avis av = mapRow(rs);
                av.setUsername(rs.getString("username")
                        + " → " + rs.getString("heb_nom"));
                list.add(av);
            }
        }
        return list;
    }

    /* ─── Tous les avis d'un hébergement (admin) ─── */
    public List<Avis> getAllByHebergement(int hebergementId)
            throws SQLException {
        List<Avis> list = new ArrayList<>();
        String sql = "SELECT a.*, u.username FROM avis_hebergement a "
                + "JOIN user u ON a.user_id = u.id "
                + "WHERE a.hebergement_id=? "
                + "ORDER BY a.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, hebergementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /* ─── Compter avis approuvés ─── */
    public int countApprouves(int hebergementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM avis_hebergement "
                + "WHERE hebergement_id=? AND statut='APPROUVE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, hebergementId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /* ─── Compter avis en attente (pour badge admin) ─── */
    public int countEnAttente() throws SQLException {
        String sql = "SELECT COUNT(*) FROM avis_hebergement "
                + "WHERE statut='EN_ATTENTE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /* ─── Helper : filtre par statut ─── */
    private List<Avis> getByHebergementAndStatut(int hebergementId,
                                                 String statut)
            throws SQLException {
        List<Avis> list = new ArrayList<>();
        String sql = "SELECT a.*, u.username FROM avis_hebergement a "
                + "JOIN user u ON a.user_id = u.id "
                + "WHERE a.hebergement_id=? AND a.statut=? "
                + "ORDER BY a.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, hebergementId);
            ps.setString(2, statut);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /* ─── Mapper une ligne SQL → Avis ─── */
    private Avis mapRow(ResultSet rs) throws SQLException {
        Avis av = new Avis();
        av.setId(rs.getInt("id"));
        av.setUserId(rs.getInt("user_id"));
        av.setHebergementId(rs.getInt("hebergement_id"));
        av.setCommentaire(rs.getString("commentaire"));
        av.setImagePath(rs.getString("image_path"));
        av.setStatut(rs.getString("statut"));
        av.setUsername(rs.getString("username"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) av.setCreatedAt(ts.toLocalDateTime());
        return av;
    }
}