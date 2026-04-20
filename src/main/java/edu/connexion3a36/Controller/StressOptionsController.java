package edu.connexion3a36.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the Stress Options page
 * Provides navigation to Psychiatre, Respiration, and About sections
 */
public class StressOptionsController {

    @FXML
    private Button btnConsulterPsychiatre;
    
    @FXML
    private Button btnRespiration;
    
    @FXML
    private Button btnAPropos;
    
    @FXML
    private Button btnConsulterMedecin;

    /**
     * Handle "Consulter cabinet psychiatre" button click
     */
    @FXML
    public void handleConsulterPsychiatre(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cabinet_psychiatre.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page du cabinet psychiatre");
        }
    }

    /**
     * Handle "Consulter Medecin" button click - opens chatbot
     */
    @FXML
    public void handleConsulterMedecin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/stress_chatbot.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le chatbot");
        }
    }

    /**
     * Handle "Exercice de respiration" button click
     */
    @FXML
    public void handleRespiration(ActionEvent event) {
        // Navigate to the breathing exercise page
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/respiration.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page d'exercice de respiration");
        }
    }

    /**
     * Handle "À propos" button click
     */
    @FXML
    public void handleAPropos(ActionEvent event) {
        // Navigate to the about page
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/a_propos.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page À propos");
        }
    }

    /**
     * Go back to the previous page (medecin form)
     */
    @FXML
    public void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fitness_dashboard2.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à la page précédente");
        }
    }

    /**
     * Go back to the main fitness dashboard
     */
    @FXML
    public void goToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fitness_dashboard2.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner au tableau de bord");
        }
    }

    /**
     * Show breathing exercise dialog
     */
    private void showBreathingExerciseDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exercice de Respiration");
        alert.setHeaderText("Technique de Respiration 4-7-8");
        alert.setContentText(
            "Cet exercice aide à réduire l'anxiété et favorise la relaxation.\n\n" +
            "Instructions :\n" +
            "1. Inspirez par le nez pendant 4 secondes\n" +
            "2. Retenez votre souffle pendant 7 secondes\n" +
            "3. Expirez lentement par la bouche pendant 8 secondes\n" +
            "4. Répétez ce cycle 3 à 4 fois\n\n" +
            "Conseil : Pratiquez cet exercice régulièrement pour de meilleurs résultats."
        );
        alert.showAndWait();
    }

    /**
     * Show about dialog
     */
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos - Anti-Stress");
        alert.setHeaderText("Application Anti-Stress");
        alert.setContentText(
            "Version 1.0\n\n" +
            "Cette application a été conçue pour vous aider à gérer votre stress " +
            "quotidien et améliorer votre bien-être global.\n\n" +
            "Fonctionnalités :\n" +
            "- Consultation de médecins spécialisés\n" +
            "- Calcul du score de bien-être\n" +
            "- Exercices de relaxation et respiration\n" +
            "- Conseils en nutrition et fitness\n\n" +
            "© 2024 Anti-Stress Application"
        );
        alert.showAndWait();
    }

    /**
     * Show error alert
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}