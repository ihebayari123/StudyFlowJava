package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Consultation;
import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.ConsultationService;
import edu.connexion3a36.services.MedecinService;
import edu.connexion3a36.services.WellBeingScoreService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class AfficherConsultationController {

    @FXML private TableView<Consultation> tableView;
    @FXML private TableColumn<Consultation, Integer> colId;
    @FXML private TableColumn<Consultation, Timestamp> colDate;
    @FXML private TableColumn<Consultation, String> colMotif;
    @FXML private TableColumn<Consultation, String> colGenre;
    @FXML private TableColumn<Consultation, String> colNiveau;
    @FXML private TableColumn<Consultation, Integer> colMedecinId;
    @FXML private TableColumn<Consultation, Integer> colSurveyId;

    @FXML private DatePicker datePicker;
    @FXML private TextField motifTF;
    @FXML private ComboBox<String> genreCB;
    @FXML private ComboBox<String> niveauCB;
    @FXML private ComboBox<Medecin> medecinCB;
    @FXML private ComboBox<WellBeingScore> surveyCB;

    private final ConsultationService service = new ConsultationService();
    private final MedecinService medecinService = new MedecinService();
    private final WellBeingScoreService surveyService = new WellBeingScoreService();
    private Consultation selected;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_de_consultation"));
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        colGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colMedecinId.setCellValueFactory(new PropertyValueFactory<>("medecin_id"));
        colSurveyId.setCellValueFactory(new PropertyValueFactory<>("stress_survey_id"));

        genreCB.getItems().addAll("Homme", "Femme");
        niveauCB.getItems().addAll("L1", "L2", "L3", "M1", "M2", "Doctorat");

        try {
            medecinCB.getItems().addAll(medecinService.getData());
            surveyCB.getItems().addAll(surveyService.getData());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, c) -> {
            if (c != null) {
                selected = c;
                datePicker.setValue(c.getDate_de_consultation().toLocalDateTime().toLocalDate());
                motifTF.setText(c.getMotif());
                genreCB.setValue(c.getGenre());
                niveauCB.setValue(c.getNiveau());
                // Sélectionner le médecin correspondant
                medecinCB.getItems().stream()
                        .filter(m -> m.getId() == c.getMedecin_id())
                        .findFirst().ifPresent(medecinCB::setValue);
                // Sélectionner le survey correspondant
                surveyCB.getItems().stream()
                        .filter(s -> s.getId() == c.getStress_survey_id())
                        .findFirst().ifPresent(surveyCB::setValue);
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
            showAlert(Alert.AlertType.WARNING, "Sélection", "Sélectionnez une consultation !");
            return;
        }
        if (datePicker.getValue() == null || motifTF.getText().trim().isEmpty()
                || genreCB.getValue() == null || niveauCB.getValue() == null
                || medecinCB.getValue() == null || surveyCB.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Remplissez tous les champs !");
            return;
        }
        try {
            selected.setDate_de_consultation(Timestamp.valueOf(datePicker.getValue().atStartOfDay()));
            selected.setMotif(motifTF.getText().trim());
            selected.setGenre(genreCB.getValue());
            selected.setNiveau(niveauCB.getValue());
            selected.setMedecin_id(medecinCB.getValue().getId());
            selected.setStress_survey_id(surveyCB.getValue().getId());
            service.updateEntity(selected.getId(), selected);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Consultation modifiée !");
            loadData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Sélectionnez une consultation !");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la consultation #" + selected.getId() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.deleteEntity(selected);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Consultation supprimée !");
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
