package edu.connexion3a36.Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller for the Breathing Exercise page
 * Implements the 4-7-8 breathing technique with motivational messages
 */
public class RespirationController {

    // Breathing timings (in seconds)
    private static final int INHALE_TIME = 4;
    private static final int HOLD_TIME = 7;
    private static final int EXHALE_TIME = 8;
    private static final int TOTAL_CYCLES = 4;

    // Motivational messages for each phase
    private static final String[] INHALE_MESSAGES = {
            "Respirez profondément... sentez l'air frais entrer dans vos poumons.",
            "Chaque inspiration vous apporte plus de calme et de détente.",
            "Visualisez une lumière dorée qui vous envahit de sérénité.",
            "Vous vous sentez de plus en plus détendu à chaque Inspiration.",
            "L'oxygène purifie votre corps et votre esprit."
    };

    private static final String[] HOLD_MESSAGES = {
            "Retenez votre souffle... laissez le calme s'installer en vous.",
            "Ce moment de pause renforce votre intérieure peace.",
            "Accumulez toute cette énergie thérapeutlque.",
            "Votre corps se détend de plus en plus profondément.",
            "Profitez de ce moment de stillness complète."
    };

    private static final String[] EXHALE_MESSAGES = {
            "Expirez lentement... libérez toute la tension.",
            "Laissez partir tout votre stress et votre anxiété.",
            "Vous vous sentez plus léger à chaque expiration.",
            "Libérez toutes les pensées négatives.",
            "Votre esprit se clears, votre cœur se calme."
    };

    private static final String[] COMPLETE_MESSAGES = {
            "Félicitations! Vous avez completado l'exercice!",
            "Bien joué! Vous êtes maintenant plus détendu.",
            "Excellent travail! Votre stress a été réduite.",
            "Vous avez fait un grand pas vers le bien-être!",
            "Continuez comme ça! Votre esprit vous remercie."
    };

    // FXML elements
    @FXML
    private Label breathPhaseLabel;

    @FXML
    private Label breathTimerLabel;

    @FXML
    private Label breathInstructionLabel;

    @FXML
    private TextArea motivationText;

    @FXML
    private Label cycleCounterLabel;

    @FXML
    private Button btnStart;

    @FXML
    private Button btnStop;

    // Internal state
    private Timer breathingTimer;
    private int currentCycle = 0;
    private int currentPhase = 0; // 0=inhale, 1=hold, 2=exhale
    private int currentSecond = 0;
    private boolean isRunning = false;
    private Random random = new Random();

    /** Référence au dashboard pour la navigation embarquée */
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    /**
     * Start the breathing exercise
     */
    @FXML
    public void startBreathing() {
        if (isRunning) return;

        isRunning = true;
        currentCycle = 1;
        currentPhase = 0;
        currentSecond = 0;

        // Update button states
        btnStart.setDisable(true);
        btnStop.setDisable(false);

        // Update UI
        updateCycleLabel();
        setMotivationMessage("🌿 Commençons cet exercice de relaxation...\n\nSuivez les instructions à l'écran et respirez calmement.");

        // Start the timer
        startTimer();
    }

    /**
     * Stop the breathing exercise
     */
    @FXML
    public void stopBreathing() {
        isRunning = false;

        if (breathingTimer != null) {
            breathingTimer.cancel();
            breathingTimer = null;
        }

        // Update button states
        btnStart.setDisable(false);
        btnStop.setDisable(true);

        // Reset UI
        breathPhaseLabel.setText("Exercise arrêté");
        breathTimerLabel.setText("");
        breathInstructionLabel.setText("Appuyez sur 'Démarrer' pour recommencer");
        cycleCounterLabel.setText("Cycle: 0 / " + TOTAL_CYCLES);
        setMotivationMessage("Exercice arrêté. Vous pouvez recommencer quand vous voulez!");
    }

    /**
     * Go back to the stress options page
     */
    @FXML
    public void goBack(ActionEvent event) {
        if (isRunning) stopBreathing();

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
        if (isRunning) stopBreathing();

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
     * Start the breathing timer
     */
    private void startTimer() {
        breathingTimer = new Timer();
        breathingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning) {
                    cancel();
                    return;
                }
                Platform.runLater(() -> tick());
            }
        }, 0, 1000);
    }

    /**
     * Process each timer tick
     */
    private void tick() {
        currentSecond++;

        // Determine phase based on timing
        if (currentPhase == 0) { // Inhale phase
            if (currentSecond <= INHALE_TIME) {
                breathPhaseLabel.setText("INSPIREZ");
                breathTimerLabel.setText(String.valueOf(INHALE_TIME - currentSecond + 1));
                breathInstructionLabel.setText("Par le nez, doucement...");
                setMotivationMessage(getRandomMessage(INHALE_MESSAGES));
            } else {
                currentPhase = 1;
                currentSecond = 0;
            }
        } else if (currentPhase == 1) { // Hold phase
            if (currentSecond <= HOLD_TIME) {
                breathPhaseLabel.setText("RETENEZ");
                breathTimerLabel.setText(String.valueOf(HOLD_TIME - currentSecond + 1));
                breathInstructionLabel.setText("Gardez l'air...");
                setMotivationMessage(getRandomMessage(HOLD_MESSAGES));
            } else {
                currentPhase = 2;
                currentSecond = 0;
            }
        } else if (currentPhase == 2) { // Exhale phase
            if (currentSecond <= EXHALE_TIME) {
                breathPhaseLabel.setText("EXPIREZ");
                breathTimerLabel.setText(String.valueOf(EXHALE_TIME - currentSecond + 1));
                breathInstructionLabel.setText("Par la bouche, lentement...");
                setMotivationMessage(getRandomMessage(EXHALE_MESSAGES));
            } else {
                // Completed one cycle
                currentSecond = 0;
                currentPhase = 0;

                if (currentCycle >= TOTAL_CYCLES) {
                    // Completed all cycles
                    completeExercise();
                } else {
                    currentCycle++;
                    updateCycleLabel();
                }
            }
        }
    }

    /**
     * Complete the exercise
     */
    private void completeExercise() {
        isRunning = false;

        if (breathingTimer != null) {
            breathingTimer.cancel();
            breathingTimer = null;
        }

        // Update button states
        btnStart.setDisable(false);
        btnStop.setDisable(true);

        // Update UI
        breathPhaseLabel.setText("🌟 FÉlicitations! 🌟");
        breathTimerLabel.setText("✓");
        breathInstructionLabel.setText("Exercice terminé avec succès!");
        cycleCounterLabel.setText("Cycle: " + TOTAL_CYCLES + " / " + TOTAL_CYCLES);
        setMotivationMessage(getRandomMessage(COMPLETE_MESSAGES));

        // Show completion dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exercice Terminé");
        alert.setHeaderText("Bravo!");
        alert.setContentText("Vous avez complété " + TOTAL_CYCLES + " cycles de respiration 4-7-8.\n\nVous devriez vous sentir plus détendu et calme.\n\nPratiquez cet exercice régulièrement pour de meilleurs résultats.");
        alert.showAndWait();
    }

    /**
     * Update the cycle counter label
     */
    private void updateCycleLabel() {
        cycleCounterLabel.setText("Cycle: " + currentCycle + " / " + TOTAL_CYCLES);
    }

    /**
     * Set motivation message in the text area
     */
    private void setMotivationMessage(String message) {
        motivationText.setText(message);
    }

    /**
     * Get a random message from the array
     */
    private String getRandomMessage(String[] messages) {
        return messages[random.nextInt(messages.length)];
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