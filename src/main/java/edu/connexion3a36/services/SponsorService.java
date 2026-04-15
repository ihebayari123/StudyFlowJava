package edu.connexion3a36.services;

import edu.connexion3a36.models.Sponsor;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SponsorService {

    private final Connection connection;

    public SponsorService() {
        // On récupère la connexion à la base de données
        // (même DataSource que tu utilises pour EventService)
        this.connection = MyConnection.getInstance().getCnx();
    }

    // ══════════════════════════════════════════════════════
    //  AJOUTER un sponsor
    // ══════════════════════════════════════════════════════
    public void ajouter(Sponsor sponsor) throws SQLException {
        String sql = "INSERT INTO sponsor (nom_sponsor, type, montant, event_titre_id) " +
                "VALUES (?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, sponsor.getNomSponsor());
        ps.setString(2, sponsor.getType());
        ps.setInt(3,    sponsor.getMontant());
        ps.setInt(4,    sponsor.getEventTitreId());
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════════════
    //  MODIFIER un sponsor
    // ══════════════════════════════════════════════════════
    public void modifier(Sponsor sponsor) throws SQLException {
        String sql = "UPDATE sponsor SET nom_sponsor=?, type=?, montant=?, event_titre_id=? " +
                "WHERE id=?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, sponsor.getNomSponsor());
        ps.setString(2, sponsor.getType());
        ps.setInt(3,    sponsor.getMontant());
        ps.setInt(4,    sponsor.getEventTitreId());
        ps.setInt(5,    sponsor.getId());
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════════════
    //  SUPPRIMER un sponsor
    // ══════════════════════════════════════════════════════
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM sponsor WHERE id=?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════════════
    //  RÉCUPÉRER TOUS les sponsors
    // ══════════════════════════════════════════════════════
    public List<Sponsor> recupererTous() throws SQLException {
        List<Sponsor> liste = new ArrayList<>();
        String sql = "SELECT * FROM sponsor ORDER BY id DESC";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Sponsor s = new Sponsor(
                    rs.getInt("id"),
                    rs.getString("nom_sponsor"),
                    rs.getString("type"),
                    rs.getInt("montant"),
                    rs.getInt("event_titre_id")
            );
            liste.add(s);
        }
        return liste;
    }

    // ══════════════════════════════════════════════════════
    //  RÉCUPÉRER les sponsors d'un événement précis
    // ══════════════════════════════════════════════════════
    public List<Sponsor> recupererParEvent(int eventId) throws SQLException {
        List<Sponsor> liste = new ArrayList<>();
        String sql = "SELECT * FROM sponsor WHERE event_titre_id=? ORDER BY id DESC";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, eventId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Sponsor s = new Sponsor(
                    rs.getInt("id"),
                    rs.getString("nom_sponsor"),
                    rs.getString("type"),
                    rs.getInt("montant"),
                    rs.getInt("event_titre_id")
            );
            liste.add(s);
        }
        return liste;
    }

    // ══════════════════════════════════════════════════════
    //  VÉRIFIER si un nom de sponsor existe déjà
    // ══════════════════════════════════════════════════════
    public boolean nomExisteDeja(String nom, int idExclu) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sponsor WHERE nom_sponsor=? AND id != ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, nom);
        ps.setInt(2, idExclu);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }
}