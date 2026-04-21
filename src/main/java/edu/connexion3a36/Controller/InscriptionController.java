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
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class InscriptionController {

    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     emailField;
    @FXML private PasswordField motDePasseField;
    @FXML private PasswordField confirmMotDePasseField;

    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label emailError;
    @FXML private Label motDePasseError;
    @FXML private Label confirmMotDePasseError;
    @FXML private Label inscriptionError;

    UtilisateurService service = new UtilisateurService();

    // ═══════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════
    @FXML
    public void initialize() {
        inscriptionError.setVisible(false);

        // Validation en temps réel — Nom
        nomField.textProperty().addListener((obs, old, val) -> {
            String msg = Validation.messageNom(val);
            nomError.setText(msg);
            nomField.setStyle(msg.isEmpty()
                    ? "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;"
                    : "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
        });

        // Validation en temps réel — Prénom
        prenomField.textProperty().addListener((obs, old, val) -> {
            String msg = Validation.messageNom(val);
            prenomError.setText(msg);
            prenomField.setStyle(msg.isEmpty()
                    ? "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;"
                    : "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
        });

        // Validation en temps réel — Email
        emailField.textProperty().addListener((obs, old, val) -> {
            String msg = Validation.messageEmail(val);
            emailError.setText(msg);
            emailField.setStyle(msg.isEmpty()
                    ? "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;"
                    : "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
            inscriptionError.setVisible(false);
        });

        // Validation en temps réel — Mot de passe
        motDePasseField.textProperty().addListener((obs, old, val) -> {
            String msg = Validation.messageMotDePasse(val);
            motDePasseError.setText(msg);
            motDePasseField.setStyle(msg.isEmpty()
                    ? "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;"
                    : "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
            // Vérifier aussi la confirmation si déjà remplie
            verifierConfirmation();
        });

        // Validation en temps réel — Confirmation mot de passe
        confirmMotDePasseField.textProperty().addListener((obs, old, val) -> {
            verifierConfirmation();
        });
    }

    // ═══════════════════════════════
    // HELPER — CONFIRMATION MDP
    // ═══════════════════════════════
    private void verifierConfirmation() {
        String mdp     = motDePasseField.getText();
        String confirm = confirmMotDePasseField.getText();
        if (confirm.isEmpty()) {
            confirmMotDePasseError.setText("");
            confirmMotDePasseField.setStyle("-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-border-width: 1.5;");
            return;
        }
        if (!mdp.equals(confirm)) {
            confirmMotDePasseError.setText("❌ Les mots de passe ne correspondent pas.");
            confirmMotDePasseField.setStyle("-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
        } else {
            confirmMotDePasseError.setText("");
            confirmMotDePasseField.setStyle("-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;");
        }
    }

    // ═══════════════════════════════
    // S'INSCRIRE
    // ═══════════════════════════════
    @FXML
    void sInscrire(ActionEvent event) {
        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim();
        String mdp     = motDePasseField.getText().trim();
        String confirm = confirmMotDePasseField.getText().trim();

        // Contrôle de saisie complet
        String msgNom     = Validation.messageNom(nom);
        String msgPrenom  = Validation.messageNom(prenom);
        String msgEmail   = Validation.messageEmail(email);
        String msgMdp     = Validation.messageMotDePasse(mdp);
        String msgConfirm = !mdp.equals(confirm) ? "❌ Les mots de passe ne correspondent pas." : "";

        nomError.setText(msgNom);
        prenomError.setText(msgPrenom);
        emailError.setText(msgEmail);
        motDePasseError.setText(msgMdp);
        confirmMotDePasseError.setText(msgConfirm);

        appliquerStyle(nomField,              msgNom.isEmpty());
        appliquerStyle(prenomField,           msgPrenom.isEmpty());
        appliquerStyle(emailField,            msgEmail.isEmpty());
        appliquerStyle(motDePasseField,       msgMdp.isEmpty());
        appliquerStyle(confirmMotDePasseField, msgConfirm.isEmpty());

        if (!msgNom.isEmpty() || !msgPrenom.isEmpty() || !msgEmail.isEmpty()
                || !msgMdp.isEmpty() || !msgConfirm.isEmpty()) return;

        // Vérifier email non déjà utilisé
        try {
            Utilisateur existant = service.login(email, mdp);
            // On vérifie par email uniquement
            boolean emailExiste = service.getData().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

            if (emailExiste) {
                afficherErreurGlobale("❌ Cet email est déjà utilisé.");
                return;
            }

            // Créer l'utilisateur
            Utilisateur nouvel = new Utilisateur(nom, prenom, email, mdp);
            service.addEntity(nouvel);

            // Succès → retour login
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Inscription réussie");
            success.setHeaderText(null);
            success.setContentText("✅ Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
            success.showAndWait();

            allerVersLogin(null);

        } catch (SQLException e) {
            afficherErreurGlobale("❌ Erreur : " + e.getMessage());
        }
    }

    // ═══════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════
    @FXML
    void allerVersLogin(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("StudyFlow — Login");
            stage.show();
        } catch (IOException e) {
            afficherErreurGlobale("❌ Erreur navigation : " + e.getMessage());
        }
    }

    // ═══════════════════════════════
    // HELPERS
    // ═══════════════════════════════
    private void appliquerStyle(TextField field, boolean valide) {
        field.setStyle(valide
                ? "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;"
                : "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13; -fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
    }

    private void afficherErreurGlobale(String message) {
        inscriptionError.setText(message);
        inscriptionError.setVisible(true);
    }
}