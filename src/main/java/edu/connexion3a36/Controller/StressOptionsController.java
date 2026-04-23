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
 * Controller for the Stress Options page.
 * Loaded inside medecinArea (StackPane) of fitness_dashboard2.fxml.
 * Uses a callback to FitnessDashboardController for in-dashboard navigation.
 */
public class StressOptionsController {

    @FXML private Button btnConsulterPsychiatre;
    @FXML private Button btnRespiration;
    @FXML private Button btnAPropos;
    @FXML private Button btnConsulterMedecin;

    /** Référence au dashboard parent pour la navigation embarquée */
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    /**
     * Consulter cabinet psychiatre — charge dans le dashboard (viewCabinet)
     */
    @FXML
    public void handleConsulterPsychiatre(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.handleOpenCabinet();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/cabinet_psychiatre.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de charger la page du cabinet psychiatre");
            }
        }
    }

    /**
     * Consulter Médecin (Chatbot) — charge happiness_chatbot dans le dashboard (viewChatbot)
     */
    @FXML
    public void handleConsulterMedecin(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.handleOpenChatbot();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/happiness_chatbot.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de charger le chatbot");
            }
        }
    }

    /**
     * Exercice de respiration — charge dans le dashboard (viewRespiration)
     */
    @FXML
    public void handleRespiration(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.handleOpenRespiration();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/respiration.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de charger la page de respiration");
            }
        }
    }

    /**
     * À propos — charge dans le dashboard (viewAPropos)
     */
    @FXML
    public void handleAPropos(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.handleOpenAPropos();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/a_propos.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de charger la page À propos");
            }
        }
    }

    /**
     * Retour vers le dashboard
     */
    @FXML
    public void goBack(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.goToRelax(event);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fitness_dashboard2.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de retourner à la page précédente");
            }
        }
    }

    @FXML
    public void goToDashboard(ActionEvent event) {
        goBack(event);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
