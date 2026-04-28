package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.WellBeingScoreService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class AjouterWellBeingScoreEtudiantController {

    @FXML private TextField recommendationTF;
    @FXML private TextField actionPlanTF;
    @FXML private TextField commentTF;
    @FXML private TextField scoreTF;

    private final WellBeingScoreService service = new WellBeingScoreService();
    private FitnessDashboardController dashboardController;

    // Données du StressSurvey transmises depuis l'étape précédente
    private int sleepHours = 7;
    private int studyHours = 5;

    public void setDashboardController(FitnessDashboardController controller) {
        this.dashboardController = controller;
    }

    public void setSurveyData(int sleep, int study) {
        this.sleepHours = sleep;
        this.studyHours = study;
    }

    @FXML
    void ajouter(ActionEvent event) {
        if (recommendationTF.getText().trim().isEmpty() || actionPlanTF.getText().trim().isEmpty()
                || commentTF.getText().trim().isEmpty() || scoreTF.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez remplir tous les champs !");
            return;
        }

        int score;
        try {
            score = Integer.parseInt(scoreTF.getText().trim());
            if (score < 0 || score > 100) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide", "Le score doit être entre 0 et 100 !");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur saisie", "Le score doit être un nombre entier !");
            return;
        }

        try {
            WellBeingScore wbs = new WellBeingScore(
                    recommendationTF.getText().trim(),
                    actionPlanTF.getText().trim(),
                    commentTF.getText().trim(),
                    score
            );
            service.addEntity(wbs);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Score de bien-être enregistré avec succès !");
            clearFields();

            // Naviguer vers le chatbot en passant le score + données sommeil/étude
            if (dashboardController != null) {
                dashboardController.handleWellBeingScoreSuccess(score, sleepHours, studyHours);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Erreur : " + e.getMessage());
        }
    }

    @FXML
    void reinitialiser(ActionEvent event) {
        clearFields();
    }

    private void clearFields() {
        recommendationTF.clear();
        actionPlanTF.clear();
        commentTF.clear();
        scoreTF.clear();
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
