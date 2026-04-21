package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.Validation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import edu.connexion3a36.Controller.FitnessDashboardController;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField motDePasseField;
    @FXML private Label emailError;
    @FXML private Label motDePasseError;
    @FXML private Label loginError;

    UtilisateurService service = new UtilisateurService();

    // ═══════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════
    @FXML
    public void initialize() {
        loginError.setVisible(false);

        // Validation en temps réel
        emailField.textProperty().addListener((obs, old, val) -> {
            afficherErreur(emailError, emailField, Validation.messageEmail(val));
            loginError.setVisible(false);
        });

        motDePasseField.textProperty().addListener((obs, old, val) -> {
            motDePasseError.setText("");
            motDePasseField.setStyle("-fx-background-radius: 8; -fx-padding: 10; " +
                    "-fx-font-size: 13; -fx-border-color: #4CAF50; " +
                    "-fx-border-radius: 8; -fx-border-width: 1.5;");
            loginError.setVisible(false);
        });
    }

    // ═══════════════════════════════
    // SE CONNECTER
    // ═══════════════════════════════
    @FXML
    void seConnecter(ActionEvent event) {
        String email = emailField.getText().trim();
        String mdp   = motDePasseField.getText().trim();

        // Validation champs
        boolean emailValide = Validation.validerEmail(email);
        boolean mdpValide   = !mdp.isEmpty();

        afficherErreur(emailError, emailField, Validation.messageEmail(email));

        if (!mdpValide) {
            motDePasseError.setText("❌ Le mot de passe est obligatoire.");
            motDePasseField.setStyle("-fx-background-radius: 8; -fx-padding: 10; " +
                    "-fx-font-size: 13; -fx-border-color: #F44336; " +
                    "-fx-border-radius: 8; -fx-border-width: 1.5;");
        }

        if (!emailValide || !mdpValide) return;

        // Vérification en base
        try {
            Utilisateur u = service.login(email, mdp);

            if (u == null) {
                // Email ou mot de passe incorrect
                afficherErreurGlobale("❌ Email ou mot de passe incorrect.");
                return;
            }

            if (u.getStatutCompte().equals("BLOQUE")) {
                // Compte bloqué
                afficherErreurGlobale("🔒 Votre compte est bloqué. Contactez un administrateur.");
                return;
            }

            // ✅ Connexion réussie → redirection
            System.out.println("✅ Connecté : " + u.getNom() + " | Rôle : " + u.getRole());
            redirigerVersTableauDeBord(u);

        } catch (SQLException e) {
            afficherErreurGlobale("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    // ═══════════════════════════════
    // REDIRECTION SELON ROLE
    // ═══════════════════════════════
    private void redirigerVersTableauDeBord(Utilisateur u) {
        try {
            String fxml;

            // Redirection selon le rôle
            if (u.getRole().equals("ETUDIANT")) {
                fxml = "/fitness_dashboard2.fxml";   // front-office
            } else {
                fxml = "/studyflow.fxml";             // back-office (ADMIN, ENSEIGNANT)
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            // Passer l'utilisateur au bon controller
            if (u.getRole().equals("ETUDIANT")) {
                FitnessDashboardController controller = loader.getController();
                controller.setUtilisateurConnecte(u);
            } else {
                DashboardController controller = loader.getController();
                controller.setUtilisateurConnecte(u);
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("StudyFlow — " + u.getRole());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            afficherErreurGlobale("❌ Erreur chargement dashboard : " + e.getMessage());
        }
    }

    // ═══════════════════════════════
    // FEEDBACK VISUEL
    // ═══════════════════════════════
    private void afficherErreur(Label label, TextField field, String message) {
        if (message == null || message.isEmpty()) {
            label.setText("");
            field.setStyle("-fx-background-radius: 8; -fx-padding: 10; " +
                    "-fx-font-size: 13; -fx-border-color: #4CAF50; " +
                    "-fx-border-radius: 8; -fx-border-width: 1.5;");
        } else {
            label.setText(message);
            field.setStyle("-fx-background-radius: 8; -fx-padding: 10; " +
                    "-fx-font-size: 13; -fx-border-color: #F44336; " +
                    "-fx-border-radius: 8; -fx-border-width: 1.5;");
        }
    }

    private void afficherErreurGlobale(String message) {
        loginError.setText(message);
        loginError.setVisible(true);
    }
    @FXML
    void allerVersInscription(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/inscription.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("StudyFlow — Inscription");
            stage.show();
        } catch (IOException e) {
            afficherErreurGlobale("❌ Erreur navigation : " + e.getMessage());
        }
    }
}