package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.FaceRecognitionUtil;
import edu.connexion3a36.utils.Validation;
import edu.connexion3a36.utils.WebcamCaptureUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
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

    // Face Recognition
    @FXML private ImageView webcamView;
    @FXML private Label faceStatusLabel;
    @FXML private Button startCameraBtn;
    @FXML private Button captureBtn;
    @FXML private Button stopCameraBtn;

    private WebcamCaptureUtil webcam = new WebcamCaptureUtil();
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

        // Désactiver capture tant que caméra pas démarrée
        captureBtn.setDisable(true);
        stopCameraBtn.setDisable(true);
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

        // Afficher statut face encoding existant
        if (u.getFaceEncoding() != null && !u.getFaceEncoding().isEmpty()) {
            faceStatusLabel.setText("✅ Visage déjà enregistré — vous pouvez le mettre à jour");
            faceStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12;");
        } else {
            faceStatusLabel.setText("⚠ Aucun visage enregistré");
            faceStatusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12;");
        }
    }

    // ═══════════════════════════════
    // WEBCAM — DÉMARRER
    // ═══════════════════════════════
    @FXML
    void startCamera(ActionEvent event) {
        faceStatusLabel.setText("📷 Caméra en cours...");
        faceStatusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12;");
        webcam.startCamera(webcamView);
        startCameraBtn.setDisable(true);
        captureBtn.setDisable(false);
        stopCameraBtn.setDisable(false);
    }

    // ═══════════════════════════════
    // WEBCAM — CAPTURER VISAGE
    // ═══════════════════════════════
    @FXML
    void captureVisage(ActionEvent event) {
        try {
            faceStatusLabel.setText("⏳ Analyse du visage...");
            faceStatusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12;");

            // Capturer la photo
            String imagePath = webcam.capturePhoto();

            // Encoder le visage via Python
            String encoding = FaceRecognitionUtil.encodeFace(imagePath);

            // Sauvegarder en base
            service.saveFaceEncoding(utilisateurActuel.getId(), encoding);
            utilisateurActuel.setFaceEncoding(encoding);

            // Feedback succès
            faceStatusLabel.setText("✅ Visage enregistré avec succès !");
            faceStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 12;");

            webcam.stopCamera();
            startCameraBtn.setDisable(false);
            captureBtn.setDisable(true);
            stopCameraBtn.setDisable(true);

        } catch (Exception e) {
            faceStatusLabel.setText("❌ " + e.getMessage());
            faceStatusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12;");
        }
    }

    // ═══════════════════════════════
    // WEBCAM — ARRÊTER
    // ═══════════════════════════════
    @FXML
    void stopCamera(ActionEvent event) {
        webcam.stopCamera();
        startCameraBtn.setDisable(false);
        captureBtn.setDisable(true);
        stopCameraBtn.setDisable(true);
        faceStatusLabel.setText("⏹ Caméra arrêtée");
        faceStatusLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12;");
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

        try {
            if (service.emailExiste(email, utilisateurActuel.getId().intValue())) {
                afficherErreur(emailError, emailField, "Cet email est déjà utilisé par un autre compte.");
                return;
            }
        } catch (SQLException e) {
            afficherAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur vérification email : " + e.getMessage());
            return;
        }

        utilisateurActuel.setNom(nom);
        utilisateurActuel.setPrenom(prenom);
        utilisateurActuel.setEmail(email);
        utilisateurActuel.setMotDePasse(mdp);
        utilisateurActuel.setRole(role);
        utilisateurActuel.setStatutCompte(statut);

        try {
            service.updateEntity(utilisateurActuel.getId().intValue(), utilisateurActuel);
            afficherAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Utilisateur modifié avec succès !");
            webcam.stopCamera();
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
        webcam.stopCamera();
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