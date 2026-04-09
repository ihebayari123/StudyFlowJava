package edu.connexion3a36.services;

import edu.connexion3a36.entities.Personne;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonneService implements IService {
    Connection cnx = MyConnection.getCnx();
    @Override
    public void addEntity(Object o) throws SQLException {
        Personne personne = (Personne) o;
        String requete = "INSERT INTO personne (nom,prenom) VALUES"+"('"+personne.getNom()+"','"+personne.getPrenom()+"')";
        Statement st= MyConnection.getInstance().getCnx().createStatement();
        st.executeUpdate(requete);
        System.out.println("Personne ajouté");
    }
    public void addEntity2(Object o) throws SQLException {
        Personne personne = (Personne) o;
        String requete = "INSERT INTO personne (nom,prenom) VALUES"+"(?,?)";
        PreparedStatement pst= MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setString(1, personne.getNom());
        pst.setString(2, personne.getPrenom());
        pst.executeUpdate();
        System.out.println("Personne ajouté");



    }
    @Override
    public void deleteEntity(Object o) throws SQLException {

    }

    @Override
    public void updateEntity(int id, Object o) throws SQLException {

    }

    @Override
    public List<Personne> getData() throws SQLException {
        List<Personne> data = new ArrayList<>();
        String requete = "SELECT * FROM personne";
        Statement st= MyConnection.getInstance().getCnx().createStatement();
        ResultSet rs = st.executeQuery(requete);
        while (rs.next()) {
            Personne p = new Personne();
            p.setId(rs.getInt(1));
            p.setNom(rs.getString("nom"));
            p.setPrenom(rs.getString("prenom"));
            System.out.println("--- "+p);
            data.add(p);
        }
        System.out.println("_-_-_-_-_-");
        System.out.println(data);
        System.out.println("-_-_-_-_-");
        return data;
    }
}