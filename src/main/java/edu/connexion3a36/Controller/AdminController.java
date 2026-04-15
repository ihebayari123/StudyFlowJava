package edu.connexion3a36.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class AdminController {

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        // Charger une vue par défaut (par exemple la liste des médecins)
        loadView("listemedecin");
    }

    @FXML
    private void ajouterMedecin() {
        loadView("AjouterMedecin");
    }

    @FXML
    private void voirListeMedecins() {
        loadView("listemedecin");
    }

    @FXML
    private void ajouterConsultation() {
        loadView("AjouterConsultation");
    }

    @FXML
    private void voirConsultations() {
        loadView("AfficherConsultation");
    }

    @FXML
    private void antiStress() {
        loadView("AfficherStressSurvey");
    }

    @FXML
    private void voirWellBeingScore() {
        loadView("AfficherWellBeingScore");
    }

    @FXML
    private void ajouterStressSurvey() {
        loadView("AjouterStressSurvey");
    }

    @FXML
    private void ajouterWellBeingScore() {
        loadView("AjouterWellBeingScore");
    }

    @FXML
    private void deconnexion() {
        // Retour au tableau de bord principal studyflow
        try {
            // Charger studyflow.fxml comme page principale
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/studyflow.fxml"));
            Node root = loader.load();
            contentArea.getScene().setRoot((Parent) root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String viewName) {
        try {
            // Éviter de recharger studyflow.fxml si on vient de l'admin
            if (viewName.equals("studyflow") && contentArea.getScene() != null && contentArea.getScene().getRoot() != null) {
                System.out.println("Retour au tableau de bord principal");
            }

            String resourcePath = "/" + viewName + ".fxml";
            var resourceUrl = getClass().getResource(resourcePath);

            if (resourceUrl == null) {
                System.err.println("Fichier non trouvé: " + resourcePath);
                showNotFoundView(viewName);
                return;
            }

            System.out.println("Chargement: " + resourcePath);
            var loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
            showNotFoundView(viewName);
        }
    }

    private void showNotFoundView(String viewName) {
        var errorLabel = new javafx.scene.control.Label("⚠️ Vue non trouvée: " + viewName + ".fxml");
        errorLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14; -fx-alignment: center;");
        contentArea.getChildren().setAll(errorLabel);
    }
}