package edu.connexion3a36.services;

import edu.connexion3a36.entities.Produit;
import edu.connexion3a36.interfaces.IProduit;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements IProduit {
    Connection cnx = MyConnection.getCnx();

    @Override
    public void addP(Produit produit) throws SQLException {
        String requete = "INSERT INTO produit (nom, description, prix, image, type_categorie_id, user_id) VALUES (?,?,?,?,?,?)";
        PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setString(1, produit.getNom());
        pst.setString(2, produit.getDescription());
        pst.setInt(3, produit.getPrix());
        pst.setString(4, produit.getImage());
        pst.setInt(5, produit.getTypeCategorieId());
        pst.setInt(6, produit.getUserId());
        pst.executeUpdate();
        System.out.println("Produit ajouté");
    }

    @Override
    public void deleteP(Produit produit) throws SQLException {
        String requete = "DELETE FROM produit WHERE id = ?";
        PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setInt(1, produit.getId());
        pst.executeUpdate();
        System.out.println("Produit supprimé");
    }

    @Override
    public void updateP(int id, Produit produit) throws SQLException {
        String requete = "UPDATE produit SET nom=?, description=?, prix=?, image=?, type_categorie_id=?, user_id=? WHERE id=?";
        PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setString(1, produit.getNom());
        pst.setString(2, produit.getDescription());
        pst.setInt(3, produit.getPrix());
        pst.setString(4, produit.getImage());
        pst.setInt(5, produit.getTypeCategorieId());
        pst.setInt(6, produit.getUserId());
        pst.setInt(7, id);
        pst.executeUpdate();
        System.out.println("Produit modifié");
    }

    @Override
    public List<Produit> getData() throws SQLException {
        List<Produit> data = new ArrayList<>();
        String requete = "SELECT * FROM produit";
        Statement st = MyConnection.getInstance().getCnx().createStatement();
        ResultSet rs = st.executeQuery(requete);
        while (rs.next()) {
            Produit p = new Produit();
            p.setId(rs.getInt("id"));
            p.setNom(rs.getString("nom"));
            p.setDescription(rs.getString("description"));
            p.setPrix(rs.getInt("prix"));
            p.setImage(rs.getString("image"));
            p.setTypeCategorieId(rs.getInt("type_categorie_id"));
            p.setUserId(rs.getInt("user_id"));
            System.out.println("--- " + p);
            data.add(p);
        }
        System.out.println("_-_-_-_-_-");
        System.out.println(data);
        System.out.println("-_-_-_-_-");
        return data;
    }
}