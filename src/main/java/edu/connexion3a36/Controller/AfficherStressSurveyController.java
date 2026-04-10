package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.services.StressSurveyService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
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

    // Composants filtre/recherche/tri
    @FXML private TextField rechercheTF;
    @FXML private ComboBox<String> triCB;
    @FXML private ComboBox<String> ordreCB;
    @FXML private DatePicker filtreDatePicker;

    private final StressSurveyService service = new StressSurveyService();
    private StressSurvey selected;

    private ObservableList<StressSurvey> masterData;
    private FilteredList<StressSurvey> filteredData;
    private SortedList<StressSurvey> sortedData;

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

        // Configuration Filtre / Recherche / Tri
        triCB.getItems().addAll("ID", "Date", "Sommeil", "Étude", "User ID");
        ordreCB.getItems().addAll("Croissant", "Décroissant");

        triCB.setOnAction(e -> appliquerTri());
        ordreCB.setOnAction(e -> appliquerTri());

        // Filtre par date
        filtreDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        // Recherche par User ID
        rechercheTF.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        loadData();
    }

    private void loadData() {
        try {
            masterData = FXCollections.observableArrayList(service.getData());
            filteredData = new FilteredList<>(masterData, p -> true);
            sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(tableView.comparatorProperty());
            tableView.setItems(sortedData);
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

    private void appliquerFiltres() {
        String recherche = rechercheTF.getText().trim().toLowerCase();
        LocalDate dateFiltre = filtreDatePicker.getValue();

        filteredData.setPredicate(survey -> {
            // Filtre recherche User ID
            if (!recherche.isEmpty()) {
                if (!String.valueOf(survey.getUser_id()).contains(recherche)) {
                    return false;
                }
            }
            // Filtre date
            if (dateFiltre != null) {
                if (!survey.getDate().toLocalDate().equals(dateFiltre)) {
                    return false;
                }
            }
            return true;
        });
    }

    private void appliquerTri() {
        String champTri = triCB.getValue();
        String ordre = ordreCB.getValue();

        if (champTri == null || ordre == null) {
            sortedData.setComparator(null);
            return;
        }

        Comparator<StressSurvey> comparator = switch (champTri) {
            case "ID" -> Comparator.comparingInt(StressSurvey::getId);
            case "Date" -> Comparator.comparing(StressSurvey::getDate);
            case "Sommeil" -> Comparator.comparingInt(StressSurvey::getSleep_hours);
            case "Étude" -> Comparator.comparingInt(StressSurvey::getStudy_hours);
            case "User ID" -> Comparator.comparingInt(StressSurvey::getUser_id);
            default -> null;
        };

        if (comparator != null) {
            if ("Décroissant".equals(ordre)) {
                comparator = comparator.reversed();
            }
            sortedData.setComparator(comparator);
        }
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        triCB.setValue(null);
        ordreCB.setValue(null);
        filtreDatePicker.setValue(null);
        rechercheTF.clear();
        filteredData.setPredicate(p -> true);
        sortedData.setComparator(null);
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
