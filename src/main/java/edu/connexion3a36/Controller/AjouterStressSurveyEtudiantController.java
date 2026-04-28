package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.services.StressSurveyService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.sql.Date;
import java.sql.SQLException;

public class AjouterStressSurveyEtudiantController {

    @FXML private DatePicker datePicker;
    @FXML private TextField  sleepHoursTF;
    @FXML private TextField  studyHoursTF;
    @FXML private TextField  userIdTF;

    private final StressSurveyService service = new StressSurveyService();
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    void ajouter(ActionEvent event) {
        // ── Validation ────────────────────────────────────────────────────────
        if (datePicker.getValue() == null
                || sleepHoursTF.getText().trim().isEmpty()
                || studyHoursTF.getText().trim().isEmpty()
                || userIdTF.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez remplir tous les champs !");
            return;
        }

        int sleepH, studyH, userId;
        try {
            sleepH = Integer.parseInt(sleepHoursTF.getText().trim());
            studyH = Integer.parseInt(studyHoursTF.getText().trim());
            userId = Integer.parseInt(userIdTF.getText().trim());
            if (sleepH < 0 || sleepH > 24 || studyH < 0 || studyH > 24) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide",
                        "Les heures doivent être entre 0 et 24 !");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur saisie",
                    "Sleep hours, Study hours et User ID doivent être des nombres !");
            return;
        }

        // ── Enregistrement + récupération de l'ID généré ─────────────────────
        try {
            StressSurvey s = new StressSurvey(
                    Date.valueOf(datePicker.getValue()),
                    sleepH, studyH, userId
            );
            int surveyId = service.addEntityAndGetId(s);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Survey ajouté avec succès ! (ID : " + surveyId + ")");
            clearFields();

            // Ouvrir le chatbot en passant l'ID du survey + données sommeil/étude
            if (dashboardController != null) {
                dashboardController.handleStressSurveySuccess(surveyId, sleepH, studyH);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Erreur : " + e.getMessage());
        }
    }

    @FXML
    void reinitialiser(ActionEvent event) { clearFields(); }

    private void clearFields() {
        datePicker.setValue(null);
        sleepHoursTF.clear();
        studyHoursTF.clear();
        userIdTF.clear();
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
