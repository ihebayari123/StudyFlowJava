package edu.connexion3a36.services;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService implements IService<Utilisateur> {

    Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Utilisateur utilisateur) throws SQLException {
        String requete = "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role, statut_compte, " +
                "login_frequency, last_login, failed_login_attempts, created_at, email_verified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setString(1, utilisateur.getNom());
        pst.setString(2, utilisateur.getPrenom());
        pst.setString(3, utilisateur.getEmail());
        pst.setString(4, utilisateur.getMotDePasse());
        pst.setString(5, utilisateur.getRole());
        pst.setString(6, utilisateur.getStatutCompte());
        pst.setInt(7, 0);                                              // login_frequency
        pst.setTimestamp(8, null);                                     // last_login
        pst.setInt(9, 0);                                              // failed_login_attempts
        pst.setTimestamp(10, new Timestamp(System.currentTimeMillis())); // created_at
        pst.setInt(11, 0);                                             // email_verified
        pst.executeUpdate();
        System.out.println("Utilisateur ajouté !");
    }

    @Override
    public void deleteEntity(Utilisateur utilisateur) throws SQLException {
        String requete = "DELETE FROM utilisateur WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setLong(1, utilisateur.getId());   // Long → setLong
        pst.executeUpdate();
        System.out.println("Utilisateur supprimé !");
    }

    @Override
    public void updateEntity(int id, Utilisateur utilisateur) throws SQLException {
        String requete = "UPDATE utilisateur SET nom=?, prenom=?, email=?, mot_de_passe=?, role=?, statut_compte=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setString(1, utilisateur.getNom());
        pst.setString(2, utilisateur.getPrenom());
        pst.setString(3, utilisateur.getEmail());
        pst.setString(4, utilisateur.getMotDePasse());
        pst.setString(5, utilisateur.getRole());
        pst.setString(6, utilisateur.getStatutCompte());
        pst.setLong(7, id);                    // int param is fine — widened to long automatically
        pst.executeUpdate();
        System.out.println("Utilisateur modifié !");
    }

    @Override
    public List<Utilisateur> getData() throws SQLException {
        List<Utilisateur> data = new ArrayList<>();
        String requete = "SELECT * FROM utilisateur";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(requete);
        while (rs.next()) {
            Utilisateur u = mapRow(rs);
            data.add(u);
        }
        return data;
    }

    // ═══════════════════════════════
    // LOGIN
    // ═══════════════════════════════
    public Utilisateur login(String email, String motDePasse) throws SQLException {
        String requete = "SELECT * FROM utilisateur WHERE email = ? AND mot_de_passe = ?";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setString(1, email);
        pst.setString(2, motDePasse);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    // ═══════════════════════════════
    // BLOQUER / DEBLOQUER
    // ═══════════════════════════════
    public void bloquerDebloquer(Utilisateur utilisateur) throws SQLException {
        String nouveauStatut = utilisateur.getStatutCompte().equals("ACTIF") ? "BLOQUE" : "ACTIF";
        String requete = "UPDATE utilisateur SET statut_compte = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setString(1, nouveauStatut);
        pst.setLong(2, utilisateur.getId());   // Long → setLong
        pst.executeUpdate();
        System.out.println("Statut changé → " + nouveauStatut);
    }

    // ═══════════════════════════════
    // HELPER
    // ═══════════════════════════════
    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getLong("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setRole(rs.getString("role"));
        u.setStatutCompte(rs.getString("statut_compte"));
        return u;
    }
}