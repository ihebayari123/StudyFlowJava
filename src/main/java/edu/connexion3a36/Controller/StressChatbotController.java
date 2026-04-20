package edu.connexion3a36.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Stress Chatbot
 * Asks 5 health-related questions and calculates stress level
 */
public class StressChatbotController {

    // The 5 questions in simple French
    private static final String[] QUESTIONS = {
        "1. Quel est votre frequence cardiaque actuelle?\n( nombre de battements par minute )",
        "2. Quel est votre niveau d oxygene dans le sang (SpO2)?\n( pourcentage )",
        "3. Quelle est votre temperatura corporelle actuelle?\n( en degres Celsius )",
        "4. Quel est votre niveau d activite physique aujourd hui?\n( 1=aucune, 2=legere, 3=moderee, 4=intense )",
        "5. Comment decririez-vous votre transpiration/peau?\n( 1=seche, 2=legere, 3=moderee, 4=abondante )"
    };

    // FXML elements
    @FXML
    private TextArea chatArea;

    @FXML
    private Label questionLabel;

    @FXML
    private TextField answerField;

    @FXML
    private Button btnSubmit;

    @FXML
    private Label progressLabel;

    @FXML
    private Label resultLabel;

    @FXML
    private Label resultDescription;

    @FXML
    private VBox resultBox;

    @FXML
    private Button btnStart;

    @FXML
    private Button btnReset;

    // Internal state
    private int currentQuestion = 0;
    private List<Double> answers = new ArrayList<>();
    private boolean isChatActive = false;

    /**
     * Start the chat
     */
    @FXML
    public void startChat() {
        // Reset state
        currentQuestion = 0;
        answers.clear();
        isChatActive = true;

        // Update UI
        chatArea.setText("");
        addBotMessage("Bonjour! Je suis votre assistant de bien-etre.");
        addBotMessage("Je vais vous poser 5 questions pour calculer votre niveau de stress.");
        addBotMessage("Veuillez repondre honestement a chaque question.");
        addBotMessage("");

        // Show first question
        showCurrentQuestion();

        // Enable/disable buttons
        btnStart.setDisable(true);
        btnSubmit.setDisable(false);
        answerField.setDisable(false);
        answerField.requestFocus();
        btnReset.setDisable(false);

        // Hide result if shown
        resultBox.setVisible(false);
    }

    /**
     * Show the current question
     */
    private void showCurrentQuestion() {
        if (currentQuestion < QUESTIONS.length) {
            questionLabel.setText(QUESTIONS[currentQuestion]);
            progressLabel.setText("Question: " + (currentQuestion + 1) + " / " + QUESTIONS.length);
            addBotMessage("Question " + (currentQuestion + 1) + ": " + QUESTIONS[currentQuestion].replace("\n", " "));
        }
    }

    /**
     * Submit the answer
     */
    @FXML
    public void submitAnswer() {
        String answer = answerField.getText().trim();

        if (answer.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer une reponse");
            return;
        }

        // Parse the answer based on question type
        try {
            double value;
            switch (currentQuestion) {
                case 0: // Heart rate (60-100 normal)
                    value = Double.parseDouble(answer);
                    if (value < 30 || value > 250) {
                        showAlert("Erreur", "Veuillez entrer une frequence cardiaque valide (30-250)");
                        return;
                    }
                    break;
                case 1: // SpO2 (0-100 normal)
                    value = Double.parseDouble(answer);
                    if (value < 50 || value > 100) {
                        showAlert("Erreur", "Veuillez entrer un percentage valide (50-100)");
                        return;
                    }
                    break;
                case 2: // Temperature (35-42 normal)
                    value = Double.parseDouble(answer.replace(",", "."));
                    if (value < 30 || value > 45) {
                        showAlert("Erreur", "Veuillez entrer une temperature valide (30-45)");
                        return;
                    }
                    break;
                case 3: // Activity level (1-4)
                case 4: // Sweating level (1-4)
                    value = Double.parseDouble(answer);
                    if (value < 1 || value > 4) {
                        showAlert("Erreur", "Veuillez entrer un nombre entre 1 et 4");
                        return;
                    }
                    break;
                default:
                    value = 0;
            }

            // Store answer
            answers.add(value);
            addUserMessage(answer);

            // Clear input
            answerField.clear();

            // Move to next question
            currentQuestion++;

            if (currentQuestion < QUESTIONS.length) {
                // Show next question
                showCurrentQuestion();
            } else {
                // All questions answered - calculate stress
                calculateStress();
            }

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer un nombre valide");
        }
    }

    /**
     * Calculate stress level based on answers
     * Note: This is a simplified simulation. For real ML model, use TensorFlow Java API.
     */
    private void calculateStress() {
        double heartRate = answers.get(0);
        double spO2 = answers.get(1);
        double temp = answers.get(2);
        double activity = answers.get(3);
        double sweating = answers.get(4);

        // Simple stress calculation formula (simulating ML model)
        double stress = 0;

        // Heart rate contribution (normal 60-80, elevated 80-100, high >100)
        if (heartRate > 100) stress += 30;
        else if (heartRate > 80) stress += 15;
        else stress += 5;

        // SpO2 contribution (normal 95-100, low <95 indicates stress)
        if (spO2 < 95) stress += 25;
        else if (spO2 < 97) stress += 10;

        // Temperature contribution (normal 36.5-37.5)
        if (temp > 38) stress += 25;
        else if (temp > 37.5) stress += 15;
        else if (temp >= 36 && temp <= 37.5) stress += 0;
        else stress += 5;

        // Activity contribution (more activity = less stress generally, but too much can indicate stress)
        stress += (5 - activity) * 3; // Inverted - less activity can mean more stress
        if (activity >= 3) stress += 10; // High activity can indicate stress response

        // Sweating contribution (sweating = stress response)
        stress += (sweating - 1) * 8;

        // Add some randomness to simulate ML model
        stress += (Math.random() * 10) - 5;

        // Clamp to 0-100
        stress = Math.max(0, Math.min(100, stress));

        // Round to integer
        int stressPercent = (int) Math.round(stress);

        // Show result
        isChatActive = false;
        btnSubmit.setDisable(true);
        answerField.setDisable(true);

        addBotMessage("");
        addBotMessage("=== RESULTAT ===");
        addBotMessage("Analyse terminee! Voici votre niveau de stress calcule.");

        resultBox.setVisible(true);
        resultLabel.setText(stressPercent + "%");

        // Add description based on stress level
        String description;
        if (stressPercent < 20) {
            description = "Votre niveau de stress est tres faible. Bien joue! Continuez vos bonne habitudes.";
        } else if (stressPercent < 40) {
            description = "Votre niveau de stress est faible. Continuez a prendre soin de vous.";
        } else if (stressPercent < 60) {
            description = "Votre niveau de stress est modere. Pensez a faire des pauses регулиер.";
        } else if (stressPercent < 80) {
            description = "Votre niveau de stress est eleve. Il serait preferable de consulter un specialiste.";
        } else {
            description = "Votre niveau de stress est tres eleve. Je vous recommande de consulter un medecin rapidement.";
        }
        resultDescription.setText(description);

        addBotMessage("Votre niveau de stress: " + stressPercent + "%");
        addBotMessage(description);
    }

    /**
     * Reset the chat
     */
    @FXML
    public void resetChat() {
        // Reset state
        currentQuestion = 0;
        answers.clear();
        isChatActive = false;

        // Reset UI
        chatArea.setText("");
        questionLabel.setText("Appuyez sur 'Demarrer' pour commencer");
        progressLabel.setText("Question: 0 / 5");
        answerField.clear();
        answerField.setDisable(true);
        resultBox.setVisible(false);

        // Reset buttons
        btnStart.setDisable(false);
        btnSubmit.setDisable(true);
        btnReset.setDisable(true);
    }

    /**
     * Go back to stress options
     */
    @FXML
    public void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/stress_options.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner a la page precedente");
        }
    }

    /**
     * Add bot message to chat area
     */
    private void addBotMessage(String message) {
        chatArea.appendText("[Assistant]: " + message + "\n\n");
    }

    /**
     * Add user message to chat area
     */
    private void addUserMessage(String message) {
        chatArea.appendText("[Vous]: " + message + "\n\n");
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