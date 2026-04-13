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

public class ModifierUtilisateurController {

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
    private Utilisateur utilisateurActuel;

    // ═══════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════
    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "ENSEIGNANT", "ETUDIANT"));
        statutCombo.setItems(FXCollections.observableArrayList("ACTIF", "BLOQUE"));

        nomField.textProperty().addListener((obs, old, val) ->
                afficherErreur(nomError, nomField, Validation.messageNom(val)));

        prenomField.textProperty().addListener((obs, old, val) ->
                afficherErreur(prenomError, prenomField, Validation.messageNom(val)));

        emailField.textProperty().addListener((obs, old, val) ->
                afficherErreur(emailError, emailField, Validation.messageEmail(val)));

        motDePasseField.textProperty().addListener((obs, old, val) ->
                afficherErreur(motDePasseError, motDePasseField, Validation.messageMotDePasse(val)));

        roleCombo.valueProperty().addListener((obs, old, val) ->
                afficherErreurCombo(roleError, roleCombo, Validation.messageRole(val)));

        statutCombo.valueProperty().addListener((obs, old, val) ->
                afficherErreurCombo(statutError, statutCombo, Validation.messageStatut(val)));
    }

    // ═══════════════════════════════
    // PRE-REMPLIR LES CHAMPS
    // ═══════════════════════════════
    public void setUtilisateur(Utilisateur u) {
        this.utilisateurActuel = u;
        nomField.setText(u.getNom());
        prenomField.setText(u.getPrenom());
        emailField.setText(u.getEmail());
        motDePasseField.setText(u.getMotDePasse());
        roleCombo.setValue(u.getRole());
        statutCombo.setValue(u.getStatutCompte());
    }

    // ═══════════════════════════════
    // MODIFIER
    // ═══════════════════════════════
    @FXML
    void modifier(ActionEvent event) {
        String nom    = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email  = emailField.getText().trim();
        String mdp    = motDePasseField.getText().trim();
        String role   = roleCombo.getValue();
        String statut = statutCombo.getValue();

        afficherErreur(nomError,        nomField,        Validation.messageNom(nom));
        afficherErreur(prenomError,     prenomField,     Validation.messageNom(prenom));
        afficherErreur(emailError,      emailField,      Validation.messageEmail(email));
        afficherErreur(motDePasseError, motDePasseField, Validation.messageMotDePasse(mdp));
        afficherErreurCombo(roleError,   roleCombo,      Validation.messageRole(role));
        afficherErreurCombo(statutError, statutCombo,    Validation.messageStatut(statut));

        if (!Validation.validerTout(nom, prenom, email, mdp, role, statut)) return;

        utilisateurActuel.setNom(nom);
        utilisateurActuel.setPrenom(prenom);
        utilisateurActuel.setEmail(email);
        utilisateurActuel.setMotDePasse(mdp);
        utilisateurActuel.setRole(role);
        utilisateurActuel.setStatutCompte(statut);

        try {
            // IService.updateEntity expects int — cast Long safely
            service.updateEntity(utilisateurActuel.getId().intValue(), utilisateurActuel);
            afficherAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Utilisateur modifié avec succès !");
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
            label.setText("");
            field.setStyle("-fx-background-radius: 8; -fx-padding: 8; -fx-font-size: 13; " +
                    "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5;");
        } else {
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