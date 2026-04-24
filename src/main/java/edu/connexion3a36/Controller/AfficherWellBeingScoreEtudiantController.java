package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.WellBeingScoreService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class AfficherWellBeingScoreEtudiantController {

    @FXML private TextField surveyIdTF;
    @FXML private VBox      resultBox;
    @FXML private VBox      noResultBox;
    @FXML private Label     scoreLabel;

    private final WellBeingScoreService service = new WellBeingScoreService();

    @FXML
    void rechercher(ActionEvent event) {
        String text = surveyIdTF.getText().trim();

        if (text.isEmpty()) {
            showAlert("Champ vide", "Veuillez entrer un Survey ID !");
            return;
        }

        int surveyId;
        try {
            surveyId = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            showAlert("Erreur saisie", "Le Survey ID doit être un nombre entier !");
            return;
        }

        try {
            WellBeingScore found = service.getBySurveyId(surveyId);

            if (found != null) {
                scoreLabel.setText(String.valueOf(found.getScore()));
                resultBox.setVisible(true);
                resultBox.setManaged(true);
                noResultBox.setVisible(false);
                noResultBox.setManaged(false);
            } else {
                resultBox.setVisible(false);
                resultBox.setManaged(false);
                noResultBox.setVisible(true);
                noResultBox.setManaged(true);
            }
        } catch (SQLException e) {
            showAlert("Erreur BDD", "Erreur lors de la recherche : " + e.getMessage());
        }
    }

    private void showAlert(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
