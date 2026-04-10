package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.services.StressSurveyService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class AfficherStressSurveyController {

    @FXML private TableView<StressSurvey> tableView;
    @FXML private TableColumn<StressSurvey, Integer> colId;
    @FXML private TableColumn<StressSurvey, Date> colDate;
    @FXML private TableColumn<StressSurvey, Integer> colSleepHours;
    @FXML private TableColumn<StressSurvey, Integer> colStudyHours;
    @FXML private TableColumn<StressSurvey, Integer> colUserId;

    @FXML private DatePicker datePicker;
    @FXML private TextField sleepHoursTF;
    @FXML private TextField studyHoursTF;
    @FXML private TextField userIdTF;

    private final StressSurveyService service = new StressSurveyService();
    private StressSurvey selected;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSleepHours.setCellValueFactory(new PropertyValueFactory<>("sleep_hours"));
        colStudyHours.setCellValueFactory(new PropertyValueFactory<>("study_hours"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("user_id"));

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, s) -> {
            if (s != null) {
                selected = s;
                datePicker.setValue(s.getDate().toLocalDate());
                sleepHoursTF.setText(String.valueOf(s.getSleep_hours()));
                studyHoursTF.setText(String.valueOf(s.getStudy_hours()));
                userIdTF.setText(String.valueOf(s.getUser_id()));
            }
        });
        loadData();
    }

    private void loadData() {
        try {
            List<StressSurvey> list = service.getData();
            tableView.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void modifier(ActionEvent event) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Sélectionnez un survey !");
            return;
        }
        try {
            selected.setDate(Date.valueOf(datePicker.getValue()));
            selected.setSleep_hours(Integer.parseInt(sleepHoursTF.getText().trim()));
            selected.setStudy_hours(Integer.parseInt(studyHoursTF.getText().trim()));
            selected.setUser_id(Integer.parseInt(userIdTF.getText().trim()));
            service.updateEntity(selected.getId(), selected);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Survey modifié !");
            loadData();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Sélectionnez un survey !");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le survey #" + selected.getId() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.deleteEntity(selected);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Survey supprimé !");
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
