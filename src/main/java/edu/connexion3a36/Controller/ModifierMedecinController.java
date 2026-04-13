package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.services.MedecinService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.event.ActionEvent;

import java.sql.SQLException;

public class ModifierMedecinController {

    @FXML private TextField nomTF;
    @FXML private TextField prenomTF;
    @FXML private TextField emailTF;
    @FXML private TextField telephoneTF;
    @FXML private ComboBox<String> disponibiliteCB;

    private Medecin selectedMedecin;
    private final MedecinService service = new MedecinService();

    @FXML
    public void initialize() {
        // Initialize combo box
        disponibiliteCB.getItems().addAll("disponible", "indisponible");
    }

    /**
     * Call this method to load a doctor's data into the form
     */
    public void setMedecin(Medecin medecin) {
        this.selectedMedecin = medecin;
        if (medecin != null) {
            nomTF.setText(medecin.getNom());
            prenomTF.setText(medecin.getPrenom());
            emailTF.setText(medecin.getEmail());
            telephoneTF.setText(medecin.getTelephone());
            disponibiliteCB.setValue(medecin.getDisponibilite());
        }
    }

    @FXML
    private void modifier(ActionEvent event) {
        if (nomTF.getText().trim().isEmpty() || prenomTF.getText().trim().isEmpty()
                || emailTF.getText().trim().isEmpty() || telephoneTF.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez remplir tous les champs !");
            return;
        }

        try {
            // Update the doctor's information
            selectedMedecin.setNom(nomTF.getText().trim());
            selectedMedecin.setPrenom(prenomTF.getText().trim());
            selectedMedecin.setEmail(emailTF.getText().trim());
            selectedMedecin.setTelephone(telephoneTF.getText().trim());
            selectedMedecin.setDisponibilite(disponibiliteCB.getValue());

            service.updateEntity(selectedMedecin.getId(), selectedMedecin);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Médecin modifié avec succès !");
            // Close the window after successful modification
            closeWindow();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
        }
    }

    @FXML
    private void reinitialiser(ActionEvent event) {
        if (selectedMedecin != null) {
            // Reload the original data
            setMedecin(selectedMedecin);
        }
    }

    @FXML
    private void annuler(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        // Get the current stage and close it
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) nomTF.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            // If we can't get the stage, just return
        }
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}