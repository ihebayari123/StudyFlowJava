package edu.connexion3a36.interfaces;

import edu.connexion3a36.entities.Produit;
import java.sql.SQLException;
import java.util.List;

public interface IProduit {
    void addP(Produit produit) throws SQLException;
    void deleteP(Produit produit) throws SQLException;
    void updateP(int id, Produit produit) throws SQLException;
    List<Produit> getData() throws SQLException;
}