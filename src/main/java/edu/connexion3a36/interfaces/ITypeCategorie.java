package edu.connexion3a36.interfaces;

import java.sql.SQLException;
import java.util.List;
import edu.connexion3a36.entities.TypeCategorie;

public interface ITypeCategorie {
    void addCat(TypeCategorie typeCategorie) throws SQLException;
    void deleteCat(TypeCategorie typeCategorie) throws SQLException;
    void updateCat(int id, TypeCategorie typeCategorie) throws SQLException;
    List<TypeCategorie> getData() throws SQLException;
}