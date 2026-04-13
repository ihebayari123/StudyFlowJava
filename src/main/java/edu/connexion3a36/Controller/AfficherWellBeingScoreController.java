package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.WellBeingScoreService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.Comparator;
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

    // Composants filtre/recherche/tri
    @FXML private TextField rechercheTF;
    @FXML private ComboBox<String> triCB;
    @FXML private ComboBox<String> ordreCB;

    private final WellBeingScoreService service = new WellBeingScoreService();
    private WellBeingScore selected;

    private ObservableList<WellBeingScore> masterData;
    private FilteredList<WellBeingScore> filteredData;
    private SortedList<WellBeingScore> sortedData;

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSurveyId.setCellValueFactory(new PropertyValueFactory<>("survey_id"));
        colRecommendation.setCellValueFactory(new PropertyValueFactory<>("recommendation"));
        colActionPlan.setCellValueFactory(new PropertyValueFactory<>("action_plan"));
        colComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        // Sélection dans le tableau
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, w) -> {
            if (w != null) {
                selected = w;
                recommendationTF.setText(w.getRecommendation());
                actionPlanTF.setText(w.getAction_plan());
                commentTF.setText(w.getComment());
                scoreTF.setText(String.valueOf(w.getScore()));
            }
        });

        // Configuration des ComboBox de tri
        triCB.getItems().addAll("ID", "Score", "Survey ID");
        ordreCB.getItems().addAll("Croissant", "Décroissant");

        // Valeurs par défaut pour le tri
        triCB.setValue("ID");
        ordreCB.setValue("Croissant");

        // Listeners pour le tri
        triCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerTri());
        ordreCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerTri());

        // Recherche temps réel
        rechercheTF.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        loadData();
    }

    private void loadData() {
        try {
            masterData = FXCollections.observableArrayList(service.getData());
            filteredData = new FilteredList<>(masterData, p -> true);
            sortedData = new SortedList<>(filteredData);

            // Appliquer les filtres initiaux
            appliquerFiltres();

            // Appliquer le tri initial
            appliquerTri();

            // Lier à la TableView
            tableView.setItems(sortedData);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void appliquerFiltres() {
        if (filteredData == null) return;

        String recherche = rechercheTF.getText() != null ? rechercheTF.getText().trim().toLowerCase() : "";

        filteredData.setPredicate(score -> {
            if (recherche.isEmpty()) return true;

            // Recherche dans les champs texte
            boolean matchRecommendation = score.getRecommendation() != null &&
                    score.getRecommendation().toLowerCase().contains(recherche);
            boolean matchActionPlan = score.getAction_plan() != null &&
                    score.getAction_plan().toLowerCase().contains(recherche);
            boolean matchComment = score.getComment() != null &&
                    score.getComment().toLowerCase().contains(recherche);

            return matchRecommendation || matchActionPlan || matchComment;
        });
    }

    private void appliquerTri() {
        if (sortedData == null) return;

        String champTri = triCB.getValue();
        String ordre = ordreCB.getValue();

        if (champTri == null || ordre == null) {
            sortedData.setComparator(null);
            return;
        }

        Comparator<WellBeingScore> comparator = null;

        switch (champTri) {
            case "ID":
                comparator = Comparator.comparingInt(WellBeingScore::getId);
                break;
            case "Score":
                comparator = Comparator.comparingInt(WellBeingScore::getScore);
                break;
            case "Survey ID":
                comparator = Comparator.comparingInt(WellBeingScore::getSurvey_id);
                break;
        }

        if (comparator != null) {
            if ("Décroissant".equals(ordre)) {
                comparator = comparator.reversed();
            }
            sortedData.setComparator(comparator);
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
            clearSelection();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
        }
    }

    private void clearSelection() {
        selected = null;
        recommendationTF.clear();
        actionPlanTF.clear();
        commentTF.clear();
        scoreTF.clear();
        tableView.getSelectionModel().clearSelection();
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
                    clearSelection();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
                }
            }
        });
    }

    @FXML
    void actualiser(ActionEvent event) {
        loadData();
        resetFiltre(event);
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        // Réinitialiser les ComboBox
        triCB.setValue("ID");
        ordreCB.setValue("Croissant");

        // Réinitialiser la recherche
        rechercheTF.clear();

        // Réinitialiser les prédicats
        if (filteredData != null) {
            filteredData.setPredicate(p -> true);
        }

        // Réappliquer le tri par défaut
        appliquerTri();

        showAlert(Alert.AlertType.INFORMATION, "Info", "Filtres réinitialisés !");
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}