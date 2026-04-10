package edu.connexion3a36.services;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedecinService implements IService {
    private Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Object o) throws SQLException {
        Medecin m = (Medecin) o;
        String req = "INSERT INTO medecin (nom, prenom, email, telephone, disponibilite) VALUES (?,?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, m.getNom());
        pst.setString(2, m.getPrenom());
        pst.setString(3, m.getEmail());
        pst.setString(4, m.getTelephone());
        pst.setString(5, m.getDisponibilite());
        pst.executeUpdate();
        pst.close();
        System.out.println("Médecin ajouté avec succès !");
    }

    @Override
    public void deleteEntity(Object o) throws SQLException {
        Medecin m = (Medecin) o;
        String req = "DELETE FROM medecin WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, m.getId());
        pst.executeUpdate();
        pst.close();
        System.out.println("Médecin supprimé !");
    }

    @Override
    public void updateEntity(int id, Object o) throws SQLException {
        Medecin m = (Medecin) o;
        String req = "UPDATE medecin SET nom=?, prenom=?, email=?, telephone=?, disponibilite=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, m.getNom());
        pst.setString(2, m.getPrenom());
        pst.setString(3, m.getEmail());
        pst.setString(4, m.getTelephone());
        pst.setString(5, m.getDisponibilite());
        pst.setInt(6, id);
        pst.executeUpdate();
        pst.close();
        System.out.println("Médecin modifié !");
    }

    @Override
    public List<Medecin> getData() throws SQLException {
        List<Medecin> list = new ArrayList<>();
        String req = "SELECT * FROM medecin";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            Medecin m = new Medecin(
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("telephone"),
                    rs.getString("disponibilite")
            );
            m.setId(rs.getInt("id"));
            list.add(m);
        }
        rs.close();
        st.close();
        return list;
    }
}