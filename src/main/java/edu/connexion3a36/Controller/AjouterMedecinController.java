package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.services.MedecinService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;

import java.sql.SQLException;

public class AjouterMedecinController {

    @FXML private TextField nomTF;
    @FXML private TextField prenomTF;
    @FXML private TextField emailTF;
    @FXML private TextField telephoneTF;
    @FXML private ComboBox<String> disponibiliteCB;

    private final MedecinService service = new MedecinService();

    @FXML
    public void initialize() {
        // Initialize combo box
        disponibiliteCB.getItems().addAll("disponible", "indisponible");
    }

    @FXML
    private void ajouter(ActionEvent event) {
        if (nomTF.getText().trim().isEmpty() || prenomTF.getText().trim().isEmpty()
                || emailTF.getText().trim().isEmpty() || telephoneTF.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez remplir tous les champs !");
            return;
        }

        try {
            Medecin medecin = new Medecin();
            medecin.setNom(nomTF.getText().trim());
            medecin.setPrenom(prenomTF.getText().trim());
            medecin.setEmail(emailTF.getText().trim());
            medecin.setTelephone(telephoneTF.getText().trim());
            medecin.setDisponibilite(disponibiliteCB.getValue());

            service.addEntity(medecin);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Médecin ajouté avec succès !");
            // Optionally clear fields or switch views
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
        }
    }

    @FXML
    private void reinitialiser(ActionEvent event) {
        nomTF.clear();
        prenomTF.clear();
        emailTF.clear();
        telephoneTF.clear();
        disponibiliteCB.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void consulterListe(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/AfficherMedecin.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Liste des Médecins");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (java.io.IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la liste : " + e.getMessage());
        }
    }
}