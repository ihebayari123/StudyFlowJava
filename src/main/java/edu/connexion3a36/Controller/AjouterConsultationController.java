package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Consultation;
import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.services.ConsultationService;
import edu.connexion3a36.services.MedecinService;
import edu.connexion3a36.services.StressSurveyService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class AjouterConsultationController {

    @FXML private DatePicker datePicker;
    @FXML private TextField motifTF;
    @FXML private ComboBox<String> genreCB;
    @FXML private ComboBox<String> niveauCB;
    @FXML private ComboBox<Medecin> medecinCB;
    @FXML private ComboBox<StressSurvey> surveyCB;

    private final ConsultationService service = new ConsultationService();
    private final MedecinService medecinService = new MedecinService();
    private final StressSurveyService surveyService = new StressSurveyService();

    @FXML
    public void initialize() {
        genreCB.getItems().addAll("Homme", "Femme");
        niveauCB.getItems().addAll("L1", "L2", "L3", "M1", "M2", "Doctorat");

        try {
            List<Medecin> medecins = medecinService.getData();
            medecinCB.getItems().addAll(medecins);

            List<StressSurvey> surveys = surveyService.getData();
            surveyCB.getItems().addAll(surveys);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur chargement", e.getMessage());
        }
    }

    @FXML
    void ajouter(ActionEvent event) {
        if (datePicker.getValue() == null || motifTF.getText().trim().isEmpty()
                || genreCB.getValue() == null || niveauCB.getValue() == null
                || medecinCB.getValue() == null || surveyCB.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez remplir tous les champs !");
            return;
        }
        try {
            Timestamp ts = Timestamp.valueOf(datePicker.getValue().atStartOfDay());
            Consultation c = new Consultation(
                    ts,
                    motifTF.getText().trim(),
                    genreCB.getValue(),
                    niveauCB.getValue(),
                    medecinCB.getValue().getId(),
                    surveyCB.getValue().getId()
            );
            service.addEntity(c);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Consultation ajoutée avec succès !");
            clearFields();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Erreur : " + e.getMessage());
        }
    }

    @FXML
    void reinitialiser(ActionEvent event) { clearFields(); }

    private void clearFields() {
        datePicker.setValue(null);
        motifTF.clear();
        genreCB.setValue(null);
        niveauCB.setValue(null);
        medecinCB.setValue(null);
        surveyCB.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
