package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.OtpService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;

public class OtpController {

    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private Label emailLabel;
    @FXML private Label timerLabel;
    @FXML private Label errorLabel;
    @FXML private Label renvoyerLabel;

    private Utilisateur utilisateurEnAttente;
    private Timeline countdown;
    private int secondesRestantes = 300; // 5 minutes
    private boolean otpValide = false;
    private Runnable onSuccess;

    private final UtilisateurService service = new UtilisateurService();

    @FXML
    public void initialize() {
        setupOtpFields();
        startTimer();
    }

    // ═══════════════════════════════════════
    // SETUP — appelé depuis InscriptionController
    // ═══════════════════════════════════════
    public void setUtilisateur(Utilisateur u, Runnable onSuccess) {
        this.utilisateurEnAttente = u;
        this.onSuccess = onSuccess;

        // Masquer l'email partiellement : o***@gmail.com
        String email = u.getEmail();
        int atIndex = email.indexOf("@");
        String masque = email.charAt(0) +
                "***" + email.substring(atIndex);
        emailLabel.setText("Code envoyé à : " + masque);
    }

    // ═══════════════════════════════════════
    // AUTO-FOCUS entre les champs
    // ═══════════════════════════════════════
    private void setupOtpFields() {
        TextField[] fields = {otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].textProperty().addListener((obs, old, val) -> {
                // Un seul caractère max
                if (val.length() > 1) {
                    fields[index].setText(val.substring(0, 1));
                    return;
                }
                // Chiffres uniquement
                if (!val.matches("[0-9]?")) {
                    fields[index].setText("");
                    return;
                }
                // Auto-focus suivant
                if (val.length() == 1 && index < fields.length - 1) {
                    fields[index + 1].requestFocus();
                }
                // Bordure bleue si rempli
                if (!val.isEmpty()) {
                    fields[index].setStyle(fields[index].getStyle()
                            .replace("#E0E0E0", "#2979FF"));
                } else {
                    fields[index].setStyle(fields[index].getStyle()
                            .replace("#2979FF", "#E0E0E0"));
                }
            });

            // Backspace → retour au champ précédent
            fields[i].setOnKeyPressed(e -> {
                if (e.getCode().toString().equals("BACK_SPACE")
                        && fields[index].getText().isEmpty()
                        && index > 0) {
                    fields[index - 1].requestFocus();
                }
            });
        }
    }

    // ═══════════════════════════════════════
    // TIMER 5 MINUTES
    // ═══════════════════════════════════════
    private void startTimer() {
        secondesRestantes = 300;
        if (countdown != null) countdown.stop();

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondesRestantes--;
            int min = secondesRestantes / 60;
            int sec = secondesRestantes % 60;
            timerLabel.setText(String.format("Expire dans : %02d:%02d", min, sec));

            // Couleur rouge quand < 1 minute
            if (secondesRestantes <= 60) {
                timerLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;" +
                        "-fx-text-fill: #E53935;");
            }
            if (secondesRestantes <= 0) {
                countdown.stop();
                timerLabel.setText("⌛ Code expiré");
                afficherErreur("Code expiré. Cliquez sur Renvoyer.");
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    // ═══════════════════════════════════════
    // VÉRIFIER OTP
    // ═══════════════════════════════════════
    @FXML
    void verifierOtp() {
        String code = otp1.getText() + otp2.getText() + otp3.getText()
                + otp4.getText() + otp5.getText() + otp6.getText();

        if (code.length() < 6) {
            afficherErreur("❌ Entrez les 6 chiffres du code.");
            return;
        }

        OtpService.OtpResult result = OtpService.verifyOtp(
                utilisateurEnAttente.getEmail(), code);

        switch (result) {
            case SUCCESS -> {
                if (countdown != null) countdown.stop();
                try {
                    // Créer le compte
                    service.addEntity(utilisateurEnAttente);
                    otpValide = true;
                    // Fermer la fenêtre OTP
                    Stage stage = (Stage) otp1.getScene().getWindow();
                    stage.close();
                    // Callback → InscriptionController
                    if (onSuccess != null) {
                        Platform.runLater(onSuccess);
                    }
                } catch (SQLException e) {
                    afficherErreur("❌ Erreur création compte : " + e.getMessage());
                }
            }
            case WRONG_CODE -> afficherErreur("❌ Code incorrect. Réessayez.");
            case EXPIRED    -> afficherErreur("⌛ Code expiré. Cliquez sur Renvoyer.");
            case NO_OTP     -> afficherErreur("❌ Aucun code trouvé. Cliquez sur Renvoyer.");
        }
    }

    // ═══════════════════════════════════════
    // RENVOYER OTP
    // ═══════════════════════════════════════
    @FXML
    void renvoyerOtp(MouseEvent event) {
        renvoyerLabel.setText("Envoi...");
        renvoyerLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #9E9E9E;");

        new Thread(() -> {
            boolean envoye = OtpService.sendOtp(
                    utilisateurEnAttente.getEmail(),
                    utilisateurEnAttente.getPrenom()
            );
            Platform.runLater(() -> {
                if (envoye) {
                    // Reset champs
                    otp1.setText(""); otp2.setText(""); otp3.setText("");
                    otp4.setText(""); otp5.setText(""); otp6.setText("");
                    errorLabel.setVisible(false);
                    errorLabel.setManaged(false);
                    renvoyerLabel.setText("Renvoyer");
                    renvoyerLabel.setStyle("-fx-font-size: 12;" +
                            "-fx-text-fill: #2979FF; -fx-font-weight: bold;");
                    startTimer();
                } else {
                    afficherErreur("❌ Échec de l'envoi. Vérifiez votre email.");
                }
            });
        }).start();
    }

    // ═══════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════
    private void afficherErreur(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}