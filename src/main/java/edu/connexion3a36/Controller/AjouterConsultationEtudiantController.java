package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Consultation;
import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.services.ConsultationService;
import edu.connexion3a36.services.MedecinService;
import edu.connexion3a36.services.StressSurveyService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public class AjouterConsultationEtudiantController {

    @FXML private DatePicker datePicker;
    @FXML private TextField motifTF;
    @FXML private TextField genreTF;
    @FXML private TextField niveauTF;
    @FXML private ComboBox<Medecin> medecinCB;
    @FXML private ComboBox<StressSurvey> surveyCB;

    private final ConsultationService service = new ConsultationService();
    private final MedecinService medecinService = new MedecinService();
    private final StressSurveyService surveyService = new StressSurveyService();

    /** Référence au dashboard pour la navigation embarquée */
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    @FXML
    public void initialize() {
        try {
            List<Medecin> medecins = medecinService.getData();
            medecinCB.getItems().addAll(medecins);

            List<StressSurvey> surveys = surveyService.getData();
            surveyCB.getItems().addAll(surveys);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur chargement", e.getMessage());
        }
    }

    @FXML
    void ajouter(ActionEvent event) {
        // 🔴 CONTROLE 1 : Tous les champs obligatoires remplis
        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez sélectionner une date de consultation !");
            datePicker.requestFocus();
            return;
        }
        
        String motif = motifTF.getText().trim();
        if (motif.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez indiquer le motif de la consultation !");
            motifTF.requestFocus();
            return;
        }
        
        String genre = genreTF.getText().trim();
        if (genre.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez indiquer le genre !");
            genreTF.requestFocus();
            return;
        }
        
        // ✅ Contrainte Genre : doit être Homme, Femme ou Etudiant (première lettre majuscule)
        if (!genre.equals("Homme") && !genre.equals("Femme") && !genre.equals("Etudiant")) {
            showAlert(Alert.AlertType.WARNING, "Genre invalide", "Le genre doit être : Homme, Femme ou Etudiant (première lettre majuscule) !");
            genreTF.requestFocus();
            return;
        }
        
        String niveau = niveauTF.getText().trim();
        if (niveau.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez indiquer le niveau d'étude !");
            niveauTF.requestFocus();
            return;
        }
        
        // ✅ Contrainte longueur minimum Genre
        if (genre.length() < 3) {
            showAlert(Alert.AlertType.WARNING, "Genre invalide", "Le genre doit contenir au moins 3 caractères !");
            genreTF.requestFocus();
            return;
        }
        
        // ✅ Contrainte longueur maximum Genre
        if (genre.length() > 30) {
            showAlert(Alert.AlertType.WARNING, "Genre trop long", "Le genre ne peut pas dépasser 30 caractères !");
            genreTF.requestFocus();
            return;
        }
        
        // ✅ Contrainte longueur minimum Niveau
        if (niveau.length() < 2) {
            showAlert(Alert.AlertType.WARNING, "Niveau invalide", "Le niveau d'étude doit contenir au moins 2 caractères !");
            niveauTF.requestFocus();
            return;
        }
        
        // ✅ Contrainte longueur maximum Niveau
        if (niveau.length() > 30) {
            showAlert(Alert.AlertType.WARNING, "Niveau trop long", "Le niveau d'étude ne peut pas dépasser 30 caractères !");
            niveauTF.requestFocus();
            return;
        }
        
        if (medecinCB.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez sélectionner un médecin !");
            medecinCB.requestFocus();
            return;
        }
        
        if (surveyCB.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez sélectionner un questionnaire de stress !");
            surveyCB.requestFocus();
            return;
        }
        
        // 🔴 CONTROLE 2 : Date ne peut pas être dans le passé
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate.isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Date invalide", "La date de consultation ne peut pas être dans le passé !");
            datePicker.requestFocus();
            return;
        }
        
        // 🔴 CONTROLE 3 : Longueur minimum du motif
        if (motif.length() < 5) {
            showAlert(Alert.AlertType.WARNING, "Motif invalide", "Le motif doit contenir au moins 5 caractères !");
            motifTF.requestFocus();
            return;
        }
        
        // 🔴 CONTROLE 4 : Longueur maximum du motif
        if (motif.length() > 255) {
            showAlert(Alert.AlertType.WARNING, "Motif trop long", "Le motif ne peut pas dépasser 255 caractères !");
            motifTF.requestFocus();
            return;
        }
        
        try {
            Timestamp ts = Timestamp.valueOf(datePicker.getValue().atStartOfDay());
            Consultation c = new Consultation(
                    ts,
                    motif,
                    genre,
                    niveau,
                    medecinCB.getValue().getId(),
                    surveyCB.getValue().getId()
            );
            service.addEntity(c);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Consultation ajoutée avec succès !");
            clearFields();
            
            // Navigation vers stress_options via le dashboard (embarqué)
            if (dashboardController != null) {
                dashboardController.handleFormSubmitSuccess();
            } else {
                navigateToStressOptionsFallback();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Erreur : " + e.getMessage());
        }
    }

    @FXML
    void reinitialiser(ActionEvent event) { clearFields(); }

    private void clearFields() {
        datePicker.setValue(null);
        motifTF.clear();
        genreTF.clear();
        niveauTF.clear();
        medecinCB.setValue(null);
        surveyCB.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
    
    /**
     * Fallback : ouvre stress_options dans une nouvelle fenêtre si pas de dashboard
     */
    private void navigateToStressOptionsFallback() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/stress_options.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) datePicker.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers les options : " + e.getMessage());
        }
    }
}
