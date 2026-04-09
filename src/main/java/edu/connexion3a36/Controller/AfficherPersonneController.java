package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Personne;
import edu.connexion3a36.services.PersonneService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class AfficherPersonneController {

    @FXML
    private TableColumn<Personne, String> nomcol;

    @FXML
    private TableColumn<Personne, String> prenomcol;

    @FXML
    private TableView<Personne> tablepersonne;

    public void initialize() throws Exception {
        PersonneService sc= new PersonneService();
        System.out.println(sc.getData());
        ObservableList<Personne> obs = FXCollections.observableList(sc.getData());
        tablepersonne.setItems(obs);
        nomcol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomcol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
    }

}
