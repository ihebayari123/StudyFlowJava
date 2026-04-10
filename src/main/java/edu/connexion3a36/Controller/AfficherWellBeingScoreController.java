package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.WellBeingScoreService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class AfficherWellBeingScoreController {

    @FXML private TableView<WellBeingScore> tableView;
    @FXML private TableColumn<WellBeingScore, Integer> colId;
    @FXML private TableColumn<WellBeingScore, Integer> colSurveyId;
    @FXML private TableColumn<WellBeingScore, String> colRecommendation;
    @FXML private TableColumn<WellBeingScore, String> colActionPlan;
    @FXML private TableColumn<WellBeingScore, String> colComment;
    @FXML private TableColumn<WellBeingScore, Integer> colScore;

    @FXML private TextField recommendationTF;
    @FXML private TextField actionPlanTF;
    @FXML private TextField commentTF;
    @FXML private TextField scoreTF;

    private final WellBeingScoreService service = new WellBeingScoreService();
    private WellBeingScore selected;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSurveyId.setCellValueFactory(new PropertyValueFactory<>("survey_id"));
        colRecommendation.setCellValueFactory(new PropertyValueFactory<>("recommendation"));
        colActionPlan.setCellValueFactory(new PropertyValueFactory<>("action_plan"));
        colComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, w) -> {
            if (w != null) {
                selected = w;
                recommendationTF.setText(w.getRecommendation());
                actionPlanTF.setText(w.getAction_plan());
                commentTF.setText(w.getComment());
                scoreTF.setText(String.valueOf(w.getScore()));
            }
        });
        loadData();
    }

    private void loadData() {
        try {
            tableView.setItems(FXCollections.observableArrayList(service.getData()));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void modifier(ActionEvent event) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Sélectionnez un score !");
            return;
        }
        if (recommendationTF.getText().trim().isEmpty() || actionPlanTF.getText().trim().isEmpty()
                || commentTF.getText().trim().isEmpty() || scoreTF.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Remplissez tous les champs !");
            return;
        }
        int score;
        try {
            score = Integer.parseInt(scoreTF.getText().trim());
            if (score < 0 || score > 100) {
                showAlert(Alert.AlertType.WARNING, "Score invalide", "Le score doit être entre 0 et 100 !");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur saisie", "Le score doit être un entier !");
            return;
        }
        try {
            selected.setRecommendation(recommendationTF.getText().trim());
            selected.setAction_plan(actionPlanTF.getText().trim());
            selected.setComment(commentTF.getText().trim());
            selected.setScore(score);
            service.updateEntity(selected.getId(), selected);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Score modifié !");
            loadData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Sélectionnez un score !");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le score #" + selected.getId() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.deleteEntity(selected);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Score supprimé !");
                    loadData();
                    selected = null;
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
                }
            }
        });
    }

    @FXML
    void actualiser(ActionEvent event) { loadData(); }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
