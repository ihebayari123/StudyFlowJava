package edu.connexion3a36.Controller;
import edu.connexion3a36.entities.Personne;
import edu.connexion3a36.services.PersonneService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

public class AjouterPersonneController {

    @FXML
    private TextField nomTF;

    @FXML
    private TextField prenomTF;

    @FXML
    void afficher(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherPersonne.fxml"));
        Parent root = loader.load();
        nomTF.getScene().setRoot(root);
    }

    @FXML
    void ajouter(ActionEvent event)throws SQLException {
        PersonneService sc = new PersonneService();
        Personne p = new Personne(nomTF.getText(),prenomTF.getText());
        sc.addEntity2(p);


    }

}

