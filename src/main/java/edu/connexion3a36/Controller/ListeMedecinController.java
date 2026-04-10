package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.services.MedecinService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class ListeMedecinController {

    @FXML private TableView<Medecin> tableView;
    @FXML private TableColumn<Medecin, Integer> colId;
    @FXML private TableColumn<Medecin, String> colNom;
    @FXML private TableColumn<Medecin, String> colPrenom;
    @FXML private TableColumn<Medecin, String> colEmail;
    @FXML private TableColumn<Medecin, String> colTelephone;
    @FXML private TableColumn<Medecin, String> colDisponibilite;

    private final MedecinService service = new MedecinService();

    @FXML
    public void initialize() {
        // Set up table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colDisponibilite.setCellValueFactory(new PropertyValueFactory<>("disponibilite"));

        // Load and display data
        loadData();
    }

    private void loadData() {
        try {
            List<Medecin> list = service.getData();
            tableView.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void fermer() {
        // Close the current window
        tableView.getScene().getWindow().hide();
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}