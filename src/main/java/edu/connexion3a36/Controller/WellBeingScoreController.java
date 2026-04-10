package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.WellBeingScoreService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class WellBeingScoreController {

    @FXML
    private TextField recommendationTF;

    @FXML
    private TextField actionPlanTF;

    @FXML
    private TextField commentTF;

    @FXML
    private TextField scoreTF;

    private final WellBeingScoreService wbsService = new WellBeingScoreService();

    @FXML
    void ajouter(ActionEvent event) {

        // Validation : champs vides
        if (recommendationTF.getText().trim().isEmpty() ||
                actionPlanTF.getText().trim().isEmpty()     ||
                commentTF.getText().trim().isEmpty()         ||
                scoreTF.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "Champ manquant",
                    "Veuillez remplir tous les champs !");
            return;
        }

        // Validation : score = entier entre 0 et 100
        int score;
        try {
            score = Integer.parseInt(scoreTF.getText().trim());
            if (score < 0 || score > 100) {
                showAlert(Alert.AlertType.WARNING,
                        "Score invalide",
                        "Le score doit être compris entre 0 et 100 !");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur de saisie",
                    "Le Score doit être un nombre entier.\nExemple : 78");
            return;
        }

        // Création et insertion
        try {
            WellBeingScore wbs = new WellBeingScore(
                    recommendationTF.getText().trim(),
                    actionPlanTF.getText().trim(),
                    commentTF.getText().trim(),
                    score
            );
            wbsService.addEntity(wbs);
            showAlert(Alert.AlertType.INFORMATION,
                    "Succès",
                    "✔  Score de bien-être enregistré avec succès !");
            clearFields();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur Base de Données",
                    "Erreur lors de l'ajout :\n" + e.getMessage());
        }
    }

    @FXML
    void reinitialiser(ActionEvent event) {
        clearFields();
    }

    // ── Helpers ──────────────────────────────────────────────

    private void clearFields() {
        recommendationTF.clear();
        actionPlanTF.clear();
        commentTF.clear();
        scoreTF.clear();
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}