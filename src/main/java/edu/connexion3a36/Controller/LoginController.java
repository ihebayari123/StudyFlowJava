package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.FaceRecognitionUtil;
import edu.connexion3a36.utils.Validation;
import edu.connexion3a36.utils.WebcamCaptureUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField motDePasseField;
    @FXML private Label emailError;
    @FXML private Label motDePasseError;
    @FXML private Label loginError;

    @FXML private VBox webcamSection;
    @FXML private ImageView webcamView;
    @FXML private Label faceStatusLabel;

    private WebcamCaptureUtil webcam = new WebcamCaptureUtil();
    private ScheduledExecutorService faceScanner;
    UtilisateurService service = new UtilisateurService();

    @FXML
    public void initialize() {
        loginError.setVisible(false);

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

    @FXML
    void seConnecter(ActionEvent event) {
        String email = emailField.getText().trim();
        String mdp   = motDePasseField.getText().trim();

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

        try {
            Utilisateur u = service.login(email, mdp);
            redirigerVersTableauDeBord(u);
        } catch (SQLException e) {
            switch (e.getMessage()) {
                case "EMAIL_INTROUVABLE"      -> afficherErreurGlobale("❌ Aucun compte trouvé avec cet email.");
                case "COMPTE_BLOQUE"          -> afficherErreurGlobale("🔒 Votre compte est bloqué. Contactez un administrateur.");
                case "COMPTE_INACTIF"         -> afficherErreurGlobale("⚠️ Votre compte n'est pas encore activé.");
                case "MOT_DE_PASSE_INCORRECT" -> afficherErreurGlobale("❌ Mot de passe incorrect.");
                default                       -> afficherErreurGlobale("❌ Erreur de connexion : " + e.getMessage());
            }
        }
    }

    @FXML
    void seConnecterAvecVisage(ActionEvent event) {
        webcamSection.setVisible(true);
        webcamSection.setManaged(true);

        faceStatusLabel.setText("📷 Regardez la caméra...");
        faceStatusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12;");

        webcam.startCamera(webcamView);

        faceScanner = Executors.newSingleThreadScheduledExecutor();
        faceScanner.scheduleAtFixedRate(() -> {
            try {
                String imagePath = webcam.capturePhoto();
                List<Utilisateur> users = service.getAllUsersWithFace();

                if (users.isEmpty()) {
                    Platform.runLater(() -> {
                        faceStatusLabel.setText("⚠ Aucun utilisateur avec visage enregistré");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF9800;");
                    });
                    return;
                }

                for (Utilisateur u : users) {
                    FaceRecognitionUtil.FaceResult result = FaceRecognitionUtil.recognizeFace(
                            imagePath, u.getFaceEncoding()
                    );

                    if (result.match) {
                        if (u.getStatutCompte().equals("BLOQUE")) {
                            Platform.runLater(() ->
                                    afficherErreurGlobale("🔒 Compte bloqué. Contactez un administrateur.")
                            );
                            arreterScanner();
                            return;
                        }

                        service.resetFaceAttempts(u.getId());

                        String confidence = String.format("%.1f", result.confidence);
                        Platform.runLater(() -> {
                            faceStatusLabel.setText("✅ Visage reconnu ! (" + confidence + "%) Connexion...");
                            faceStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                        });

                        arreterScanner();
                        Thread.sleep(1000);
                        Platform.runLater(() -> redirigerVersTableauDeBord(u));
                        return;
                    }
                }

                // Aucun visage reconnu — continuer à scanner
                Platform.runLater(() -> {
                    faceStatusLabel.setText("🔍 Recherche en cours...");
                    faceStatusLabel.setStyle("-fx-text-fill: #757575;");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    faceStatusLabel.setText("❌ Erreur : " + e.getMessage());
                    faceStatusLabel.setStyle("-fx-text-fill: #F44336;");
                });
            }
        }, 1, 2, TimeUnit.SECONDS);
    }

    @FXML
    void annulerReconnaissance(ActionEvent event) {
        arreterScanner();
        webcamSection.setVisible(false);
        webcamSection.setManaged(false);
        faceStatusLabel.setText("");
    }

    private void arreterScanner() {
        if (faceScanner != null && !faceScanner.isShutdown()) {
            faceScanner.shutdown();
        }
        webcam.stopCamera();
    }

    private void redirigerVersTableauDeBord(Utilisateur u) {
        try {
            String fxml = u.getRole().equals("ETUDIANT") ? "/fitness_dashboard2.fxml" : "/studyflow.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

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