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

        String recommendation = recommendationTF.getText().trim();
        String actionPlan = actionPlanTF.getText().trim();
        String comment = commentTF.getText().trim();
        String scoreStr = scoreTF.getText().trim();

        // ✅ Contrainte 1 : Tous les champs obligatoires
        if (recommendation.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire", "⚠️  La recommandation médicale est requise !");
            recommendationTF.requestFocus();
            return;
        }
        if (actionPlan.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire", "⚠️  Le plan d'action est requis !");
            actionPlanTF.requestFocus();
            return;
        }
        if (comment.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire", "⚠️  Les observations cliniques sont requises !");
            commentTF.requestFocus();
            return;
        }
        if (scoreStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire", "⚠️  Le score de bien-être est requis !");
            scoreTF.requestFocus();
            return;
        }

        // ✅ Contrainte 2 : Longueur minimum/maximum
        if (recommendation.length() < 10) {
            showAlert(Alert.AlertType.WARNING, "Contrainte longueur", "⚠️  La recommandation doit contenir au moins 10 caractères !");
            recommendationTF.requestFocus();
            return;
        }
        if (recommendation.length() > 255) {
            showAlert(Alert.AlertType.WARNING, "Contrainte longueur", "⚠️  La recommandation ne peut pas dépasser 255 caractères !");
            recommendationTF.requestFocus();
            return;
        }
        if (actionPlan.length() < 8) {
            showAlert(Alert.AlertType.WARNING, "Contrainte longueur", "⚠️  Le plan d'action doit contenir au moins 8 caractères !");
            actionPlanTF.requestFocus();
            return;
        }
        if (actionPlan.length() > 255) {
            showAlert(Alert.AlertType.WARNING, "Contrainte longueur", "⚠️  Le plan d'action ne peut pas dépasser 255 caractères !");
            actionPlanTF.requestFocus();
            return;
        }
        if (comment.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Contrainte longueur", "⚠️  Les observations doivent contenir au moins 6 caractères !");
            commentTF.requestFocus();
            return;
        }
        if (comment.length() > 500) {
            showAlert(Alert.AlertType.WARNING, "Contrainte longueur", "⚠️  Les observations ne peuvent pas dépasser 500 caractères !");
            commentTF.requestFocus();
            return;
        }

        // ✅ Contrainte 3 : Score numérique entre 0 et 100 exclusivement
        int score;
        try {
            score = Integer.parseInt(scoreStr);
            if (score < 0 || score > 100) {
                showAlert(Alert.AlertType.WARNING, "Valeur invalide", "⚠️  Le score doit être un nombre entier compris strictement entre 0 et 100 !");
                scoreTF.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Format invalide", "❌  Le score doit être un nombre entier positif.\nExemple valide : 78");
            scoreTF.requestFocus();
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