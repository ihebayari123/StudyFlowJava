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
 * Controller for the About page
 * Displays application info, doctor testimonials, and student testimonials
 */
public class AProposController {

    @FXML
    private Button btnBack;

    @FXML
    private Button btnDashboard;

    /** Référence au dashboard pour la navigation embarquée */
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    /**
     * Go back to the stress options page
     */
    @FXML
    public void goBack(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.goToStressOptions(event);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/stress_options.fxml"));
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

    /**
     * Go to the fitness dashboard
     */
    @FXML
    public void goToDashboard(ActionEvent event) {
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
                showAlert("Erreur", "Impossible de retourner au tableau de bord");
            }
        }
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