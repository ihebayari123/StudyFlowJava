package edu.connexion3a36.services;

import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.interfaces.ITypeCategorie;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TypeCategorieService implements ITypeCategorie {
    Connection cnx = MyConnection.getCnx();

    @Override
    public void addCat(TypeCategorie typeCategorie) throws SQLException {
        String requete = "INSERT INTO type_categorie (nom_categorie,description) VALUES" + "('" + typeCategorie.getNomCategorie() + "','" + typeCategorie.getDescription() + "')";
        Statement st = MyConnection.getInstance().getCnx().createStatement();
        st.executeUpdate(requete);
        System.out.println("TypeCategorie ajouté");
    }


/*
    public void addEntity2(TypeCategorie typeCategorie) throws SQLException {
        String requete = "INSERT INTO type_categorie (nom_categorie,description) VALUES" + "(?,?)";
        PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setString(1, typeCategorie.getNomCategorie());
        pst.setString(2, typeCategorie.getDescription());
        pst.executeUpdate();
        System.out.println("TypeCategorie ajouté");
    }

 */

    @Override
    public void deleteCat(TypeCategorie typeCategorie) throws SQLException {
        String requete = "DELETE FROM type_categorie WHERE id = ?";
        PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setInt(1, typeCategorie.getId());
        pst.executeUpdate();
        System.out.println("TypeCategorie supprimé");
    }

    @Override
    public void updateCat(int id, TypeCategorie typeCategorie) throws SQLException {
        String requete = "UPDATE type_categorie SET nom_categorie = ?, description = ? WHERE id = ?";
        PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setString(1, typeCategorie.getNomCategorie());
        pst.setString(2, typeCategorie.getDescription());
        pst.setInt(3, id);
        pst.executeUpdate();
        System.out.println("TypeCategorie modifié");
    }

    @Override
    public List<TypeCategorie> getData() throws SQLException {
        List<TypeCategorie> data = new ArrayList<>();
        String requete = "SELECT * FROM type_categorie";
        Statement st = MyConnection.getInstance().getCnx().createStatement();
        ResultSet rs = st.executeQuery(requete);
        while (rs.next()) {
            TypeCategorie tc = new TypeCategorie();
            tc.setId(rs.getInt(1));
            tc.setNomCategorie(rs.getString("nom_categorie"));
            tc.setDescription(rs.getString("description"));
            System.out.println("--- " + tc);
            data.add(tc);
        }
        System.out.println("_-_-_-_-_-");
        System.out.println(data);
        System.out.println("-_-_-_-_-");
        return data;
    }
}