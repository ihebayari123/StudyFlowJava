package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.Validation;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.sql.Timestamp;

public class AjouterUtilisateurController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField motDePasseField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> statutCombo;

    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label emailError;
    @FXML private Label motDePasseError;
    @FXML private Label roleError;
    @FXML private Label statutError;

    UtilisateurService service = new UtilisateurService();

    // ═══════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════
    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "ENSEIGNANT", "ETUDIANT"));
        statutCombo.setItems(FXCollections.observableArrayList("ACTIF", "BLOQUE"));
        roleCombo.setValue("ETUDIANT");
        statutCombo.setValue("ACTIF");

        // Validation en temps réel
        nomField.textProperty().addListener((obs, old, val) -> {
            afficherErreur(nomError, nomField, Validation.messageNom(val));
        });

        prenomField.textProperty().addListener((obs, old, val) -> {
            afficherErreur(prenomError, prenomField, Validation.messageNom(val));
        });

        emailField.textProperty().addListener((obs, old, val) -> {
            afficherErreur(emailError, emailField, Validation.messageEmail(val));
        });

        motDePasseField.textProperty().addListener((obs, old, val) -> {
            afficherErreur(motDePasseError, motDePasseField, Validation.messageMotDePasse(val));
        });

        roleCombo.valueProperty().addListener((obs, old, val) -> {
            afficherErreurCombo(roleError, roleCombo, Validation.messageRole(val));
        });

        statutCombo.valueProperty().addListener((obs, old, val) -> {
            afficherErreurCombo(statutError, statutCombo, Validation.messageStatut(val));
        });
    }

    // ═══════════════════════════════
    // AJOUTER
    // ═══════════════════════════════
    @FXML
    void ajouter(ActionEvent event) {
        String nom      = nomField.getText().trim();
        String prenom   = prenomField.getText().trim();
        String email    = emailField.getText().trim();
        String mdp      = motDePasseField.getText().trim();
        String role     = roleCombo.getValue();
        String statut   = statutCombo.getValue();

        // Afficher toutes les erreurs à la soumission
        afficherErreur(nomError,        nomField,        Validation.messageNom(nom));
        afficherErreur(prenomError,     prenomField,     Validation.messageNom(prenom));
        afficherErreur(emailError,      emailField,      Validation.messageEmail(email));
        afficherErreur(motDePasseError, motDePasseField, Validation.messageMotDePasse(mdp));
        afficherErreurCombo(roleError,   roleCombo,      Validation.messageRole(role));
        afficherErreurCombo(statutError, statutCombo,    Validation.messageStatut(statut));

        // Bloquer si invalide
        if (!Validation.validerTout(nom, prenom, email, mdp, role, statut)) return;

        try {
            if (service.emailExiste(email, 0)) {
                afficherErreur(emailError, emailField, "Cet email est déjà utilisé.");
                return;
            }
        } catch (SQLException e) {
            afficherAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur vérification email : " + e.getMessage());
            return;
        }

        Utilisateur u = new Utilisateur(nom, prenom, email, mdp, role);
        u.setStatus(statut);

        try {
            service.addEntity(u);
            afficherAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Utilisateur ajouté avec succès !");
            fermerFenetre();
        } catch (SQLException e) {
            afficherAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur : " + e.getMessage());
        }
    }

    // ═══════════════════════════════
    // ANNULER
    // ═══════════════════════════════
    @FXML
    void annuler(ActionEvent event) {
        fermerFenetre();
    }

    // ═══════════════════════════════
    // FEEDBACK VISUEL
    // ═══════════════════════════════
    private void afficherErreur(Label label, TextField field, String message) {
        if (message == null || message.isEmpty()) {
            // ✅ Valide → bordure verte
            label.setText("");
            field.setStyle("-fx-background-radius: 8; -fx-padding: 8; -fx-font-size: 13; " +
                    "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;");
        } else {
            // ❌ Invalide → bordure rouge + message
            label.setText(message);
            field.setStyle("-fx-background-radius: 8; -fx-padding: 8; -fx-font-size: 13; " +
                    "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
        }
    }

    private void afficherErreurCombo(Label label, ComboBox<String> combo, String message) {
        if (message == null || message.isEmpty()) {
            label.setText("");
            combo.setStyle("-fx-background-radius: 8; -fx-font-size: 13; " +
                    "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;");
        } else {
            label.setText(message);
            combo.setStyle("-fx-background-radius: 8; -fx-font-size: 13; " +
                    "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5;");
        }
    }

    // ═══════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════
    private void fermerFenetre() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    private void afficherAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}