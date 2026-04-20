package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Consultation;
import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.ConsultationService;
import edu.connexion3a36.services.MedecinService;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
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

    // Composants filtre et tri
    @FXML private TextField rechercheTF;
    @FXML private ComboBox<String> triCB;
    @FXML private ComboBox<String> ordreCB;
    @FXML private DatePicker filtreDatePicker;
    @FXML private ComboBox<String> filtreGenreCB;
    @FXML private ComboBox<String> filtreNiveauCB;
    @FXML private Label resultatsLabel;

    private final ConsultationService service = new ConsultationService();
    private final MedecinService medecinService = new MedecinService();
    private final WellBeingScoreService surveyService = new WellBeingScoreService();
    private Consultation selected;

    private ObservableList<Consultation> masterData;
    private FilteredList<Consultation> filteredData;
    private SortedList<Consultation> sortedData;

    @FXML
    public void initialize() {
        // Configuration des colonnes
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

        // Sélection dans le tableau
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

        // Configuration des filtres
        if (filtreGenreCB != null) {
            filtreGenreCB.getItems().addAll("Tous", "Homme", "Femme");
            filtreGenreCB.setValue("Tous");
            filtreGenreCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        }

        if (filtreNiveauCB != null) {
            filtreNiveauCB.getItems().addAll("Tous", "L1", "L2", "L3", "M1", "M2", "Doctorat");
            filtreNiveauCB.setValue("Tous");
            filtreNiveauCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        }

        // Configuration du tri
        if (triCB != null) {
            triCB.getItems().addAll("ID", "Date", "Motif", "Genre", "Niveau");
            triCB.setValue("ID");
            triCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerTri());
        }

        if (ordreCB != null) {
            ordreCB.getItems().addAll("Croissant", "Décroissant");
            ordreCB.setValue("Croissant");
            ordreCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerTri());
        }

        // Filtre par date
        if (filtreDatePicker != null) {
            filtreDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        }

        // Recherche textuelle
        if (rechercheTF != null) {
            rechercheTF.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        }

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

            // Mettre à jour le label des résultats
            updateResultatsLabel();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void appliquerFiltres() {
        if (filteredData == null) return;

        String recherche = rechercheTF != null && rechercheTF.getText() != null ?
                rechercheTF.getText().trim().toLowerCase() : "";
        LocalDate dateFiltre = filtreDatePicker != null ? filtreDatePicker.getValue() : null;
        String genreFiltre = filtreGenreCB != null ? filtreGenreCB.getValue() : "Tous";
        String niveauFiltre = filtreNiveauCB != null ? filtreNiveauCB.getValue() : "Tous";

        filteredData.setPredicate(consultation -> {
            // Filtre de recherche textuelle (motif)
            if (!recherche.isEmpty()) {
                boolean matchMotif = consultation.getMotif() != null &&
                        consultation.getMotif().toLowerCase().contains(recherche);
                if (!matchMotif) {
                    return false;
                }
            }

            // Filtre par date
            if (dateFiltre != null) {
                LocalDate dateConsultation = consultation.getDate_de_consultation().toLocalDateTime().toLocalDate();
                if (!dateConsultation.equals(dateFiltre)) {
                    return false;
                }
            }

            // Filtre par genre
            if (!"Tous".equals(genreFiltre)) {
                if (!genreFiltre.equals(consultation.getGenre())) {
                    return false;
                }
            }

            // Filtre par niveau
            if (!"Tous".equals(niveauFiltre)) {
                if (!niveauFiltre.equals(consultation.getNiveau())) {
                    return false;
                }
            }

            return true;
        });

        updateResultatsLabel();
    }

    private void appliquerTri() {
        if (sortedData == null) return;

        String champTri = triCB != null ? triCB.getValue() : "ID";
        String ordre = ordreCB != null ? ordreCB.getValue() : "Croissant";

        if (champTri == null || ordre == null) {
            sortedData.setComparator(null);
            return;
        }

        Comparator<Consultation> comparator = null;

        switch (champTri) {
            case "ID":
                comparator = Comparator.comparingInt(Consultation::getId);
                break;
            case "Date":
                comparator = Comparator.comparing(Consultation::getDate_de_consultation);
                break;
            case "Motif":
                comparator = Comparator.comparing(Consultation::getMotif,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Genre":
                comparator = Comparator.comparing(Consultation::getGenre,
                        Comparator.nullsLast(String::compareTo));
                break;
            case "Niveau":
                comparator = Comparator.comparing(Consultation::getNiveau,
                        Comparator.nullsLast(String::compareTo));
                break;
        }

        if (comparator != null) {
            if ("Décroissant".equals(ordre)) {
                comparator = comparator.reversed();
            }
            sortedData.setComparator(comparator);
        }
    }

    private void updateResultatsLabel() {
        if (resultatsLabel != null && filteredData != null && masterData != null) {
            int total = masterData.size();
            int affiches = filteredData.size();
            resultatsLabel.setText("Affichage de " + affiches + " consultation(s) sur " + total);
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
            clearSelection();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
        }
    }

    private void clearSelection() {
        selected = null;
        datePicker.setValue(null);
        motifTF.clear();
        genreCB.setValue(null);
        niveauCB.setValue(null);
        medecinCB.setValue(null);
        surveyCB.setValue(null);
        tableView.getSelectionModel().clearSelection();
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
        // Réinitialiser les ComboBox de tri
        if (triCB != null) triCB.setValue("ID");
        if (ordreCB != null) ordreCB.setValue("Croissant");

        // Réinitialiser les filtres
        if (filtreDatePicker != null) filtreDatePicker.setValue(null);
        if (rechercheTF != null) rechercheTF.clear();
        if (filtreGenreCB != null) filtreGenreCB.setValue("Tous");
        if (filtreNiveauCB != null) filtreNiveauCB.setValue("Tous");

        // Réinitialiser les prédicats
        if (filteredData != null) {
            filteredData.setPredicate(p -> true);
        }

        // Réappliquer le tri par défaut
        appliquerTri();

        updateResultatsLabel();

        showAlert(Alert.AlertType.INFORMATION, "Filtres réinitialisés",
                "Tous les filtres ont été réinitialisés.");
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}