package edu.connexion3a36.services;

import edu.connexion3a36.models.Event;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventService {

    private Connection connection;

    public EventService() {
        // ⚠️ On utilise getCnx() comme dans TON MyConnection
        this.connection = MyConnection.getInstance().getCnx();
    }

    // ══════════════════════════════════════
    //  CREATE — Ajouter un event
    // ══════════════════════════════════════
    public void ajouter(Event event) throws SQLException {
        String sql = "INSERT INTO event (titre, description, date_creation, type, image, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, event.getTitre());
        ps.setString(2, event.getDescription());
        ps.setTimestamp(3, Timestamp.valueOf(event.getDateCreation()));
        ps.setString(4, event.getType());
        ps.setString(5, event.getImage());
        ps.setInt(6, event.getUserId());
        ps.executeUpdate();
        System.out.println("✅ Event ajouté : " + event.getTitre());
    }

    // ══════════════════════════════════════
    //  READ — Récupérer tous les events
    // ══════════════════════════════════════
    public List<Event> recupererTous() throws SQLException {
        List<Event> liste = new ArrayList<>();
        String sql = "SELECT * FROM event";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Event e = new Event(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getTimestamp("date_creation").toLocalDateTime(),
                    rs.getString("type"),
                    rs.getString("image"),
                    rs.getInt("user_id")
            );
            liste.add(e);
        }
        return liste;
    }

    // ══════════════════════════════════════
    //  UPDATE — Modifier un event
    // ══════════════════════════════════════
    public void modifier(Event event) throws SQLException {
        String sql = "UPDATE event SET titre=?, description=?, date_creation=?, " +
                "type=?, image=?, user_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, event.getTitre());
        ps.setString(2, event.getDescription());
        ps.setTimestamp(3, Timestamp.valueOf(event.getDateCreation()));
        ps.setString(4, event.getType());
        ps.setString(5, event.getImage());
        ps.setInt(6, event.getUserId());
        ps.setInt(7, event.getId());
        ps.executeUpdate();
        System.out.println("✅ Event modifié : " + event.getTitre());
    }

    // ══════════════════════════════════════
    //  DELETE — Supprimer un event
    // ══════════════════════════════════════
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM event WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✅ Event supprimé, id=" + id);
    }
}