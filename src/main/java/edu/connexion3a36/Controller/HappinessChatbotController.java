package edu.connexion3a36.Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Chatbot EduWell — Prédit le score de bonheur étudiant
 * via le modèle best_happiness_model.pkl (RandomForestRegressor)
 * Questions : heures_sommeil, heures_etude, age, cafes_par_jour
 */
public class HappinessChatbotController {

    // ─── Questions ───────────────────────────────────────────────────
    private static final String[][] QUESTIONS = {
        {
            "😴 Combien d'heures dormez-vous par nuit en moyenne ?",
            "Entrez un nombre entre 1 et 12 (ex: 7)",
            "heures_sommeil"
        },
        {
            "📚 Combien d'heures étudiez-vous par jour ?",
            "Entrez un nombre entre 0 et 16 (ex: 5)",
            "heures_etude"
        },
        {
            "🎂 Quel est votre âge ?",
            "Entrez votre âge (ex: 21)",
            "age"
        },
        {
            "☕ Combien de cafés buvez-vous par jour ?",
            "Entrez un nombre entre 0 et 10 (ex: 2)",
            "cafes_par_jour"
        }
    };

    // ─── FXML ────────────────────────────────────────────────────────
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextField answerField;
    @FXML private Button btnSend;
    @FXML private Button btnReset;
    @FXML private Label progressLabel;

    // Résultat
    @FXML private VBox resultBox;
    @FXML private Label resultScoreLabel;
    @FXML private Label resultEmojiLabel;
    @FXML private Label resultTitleLabel;
    @FXML private Label resultDescLabel;
    @FXML private VBox recommendationsBox;
    @FXML private Label rec1Label;
    @FXML private Label rec2Label;
    @FXML private Label rec3Label;

    // ─── État ────────────────────────────────────────────────────────
    private int currentQuestion = 0;
    private final List<Double> answers = new ArrayList<>();
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    // ─── Init ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Message de bienvenue
        addBotMessage("👋 Bonjour ! Je suis votre assistant EduWell.", false);
        addBotMessage("🎓 Je vais analyser votre bien-être étudiant grâce à notre modèle d'intelligence artificielle.", false);
        addBotMessage("📊 Je vous poserai 4 questions simples, puis je calculerai votre score de bonheur personnalisé.", false);
        addBotMessage("✨ Commençons !", false);

        // Petite pause puis première question
        Platform.runLater(() -> {
            addBotMessage(QUESTIONS[0][0], true);
            addBotMessage("💬 " + QUESTIONS[0][1], false);
            progressLabel.setText("1 / 4 questions");
            btnSend.setDisable(false);
            answerField.setDisable(false);
            answerField.requestFocus();
        });

        // Envoyer avec Entrée
        answerField.setOnAction(e -> submitAnswer());
    }

    // ─── Soumettre une réponse ───────────────────────────────────────
    @FXML
    public void submitAnswer() {
        String input = answerField.getText().trim();
        if (input.isEmpty()) return;

        // Valider la saisie
        double value;
        try {
            value = Double.parseDouble(input.replace(",", "."));
        } catch (NumberFormatException ex) {
            addBotMessage("⚠️ Veuillez entrer un nombre valide (ex: 7).", false);
            answerField.clear();
            return;
        }

        // Validation par question
        String error = validateAnswer(currentQuestion, value);
        if (error != null) {
            addBotMessage("⚠️ " + error, false);
            answerField.clear();
            return;
        }

        // Afficher la réponse de l'utilisateur
        addUserMessage(input + " " + getUnit(currentQuestion));
        answerField.clear();
        answers.add(value);
        currentQuestion++;

        if (currentQuestion < QUESTIONS.length) {
            progressLabel.setText((currentQuestion + 1) + " / 4 questions");
            addBotMessage(QUESTIONS[currentQuestion][0], true);
            addBotMessage("💬 " + QUESTIONS[currentQuestion][1], false);
        } else {
            // Toutes les questions répondues
            progressLabel.setText("Analyse en cours...");
            btnSend.setDisable(true);
            answerField.setDisable(true);
            addBotMessage("⏳ Analyse de vos données en cours...", false);
            runPrediction();
        }

        scrollToBottom();
    }

    // ─── Validation ─────────────────────────────────────────────────
    private String validateAnswer(int q, double v) {
        switch (q) {
            case 0: // sommeil
                if (v < 1 || v > 12) return "Les heures de sommeil doivent être entre 1 et 12.";
                break;
            case 1: // étude
                if (v < 0 || v > 16) return "Les heures d'étude doivent être entre 0 et 16.";
                break;
            case 2: // âge
                if (v < 10 || v > 100) return "L'âge doit être entre 10 et 100.";
                break;
            case 3: // café
                if (v < 0 || v > 20) return "Le nombre de cafés doit être entre 0 et 20.";
                break;
        }
        return null;
    }

    private String getUnit(int q) {
        switch (q) {
            case 0: return "h de sommeil";
            case 1: return "h d'étude";
            case 2: return "ans";
            case 3: return "café(s)/jour";
            default: return "";
        }
    }

    // ─── Prédiction ML ──────────────────────────────────────────────
    private void runPrediction() {
        new Thread(() -> {
            try {
                double score = callPythonModel(
                    answers.get(0), answers.get(1), answers.get(2), answers.get(3)
                );
                Platform.runLater(() -> showResult(score));
            } catch (Exception ex) {
                // Fallback : calcul local si Python échoue
                double score = computeLocalScore(
                    answers.get(0), answers.get(1), answers.get(2), answers.get(3)
                );
                Platform.runLater(() -> {
                    addBotMessage("ℹ️ Prédiction locale utilisée (modèle Python indisponible).", false);
                    showResult(score);
                });
            }
        }).start();
    }

    /**
     * Appelle le script Python predict_happiness.py
     */
    private double callPythonModel(double sommeil, double etude, double age, double cafe)
            throws Exception {
        // Trouver le script Python
        String scriptPath = findScript("predict_happiness.py");

        ProcessBuilder pb = new ProcessBuilder(
            "python", scriptPath,
            String.valueOf(sommeil),
            String.valueOf(etude),
            String.valueOf(age),
            String.valueOf(cafe)
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode = process.waitFor();

        if (exitCode != 0 || output.startsWith("ERROR")) {
            throw new RuntimeException("Python error: " + output);
        }

        return Double.parseDouble(output);
    }

    /**
     * Cherche le script Python dans les répertoires du projet
     */
    private String findScript(String scriptName) throws FileNotFoundException {
        // Essayer depuis le classpath
        java.net.URL url = getClass().getResource("/" + scriptName);
        if (url != null) {
            return new File(url.getFile()).getAbsolutePath();
        }
        // Chercher dans les répertoires parents
        File current = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 6; i++) {
            File candidate = new File(current, "src/main/resources/" + scriptName);
            if (candidate.exists()) return candidate.getAbsolutePath();
            candidate = new File(current, scriptName);
            if (candidate.exists()) return candidate.getAbsolutePath();
            current = current.getParentFile();
            if (current == null) break;
        }
        throw new FileNotFoundException("Script introuvable: " + scriptName);
    }

    /**
     * Calcul local de secours si Python n'est pas disponible
     */
    private double computeLocalScore(double sommeil, double etude, double age, double cafe) {
        double score = 50.0;
        // Sommeil optimal : 7-9h
        if (sommeil >= 7 && sommeil <= 9) score += 20;
        else if (sommeil >= 6 && sommeil < 7) score += 10;
        else if (sommeil < 5) score -= 15;
        // Étude : 4-6h optimal
        if (etude >= 4 && etude <= 6) score += 15;
        else if (etude > 8) score -= 10;
        else if (etude < 2) score -= 5;
        // Café : 1-2 optimal
        if (cafe <= 2) score += 10;
        else if (cafe > 4) score -= 15;
        // Âge : légère variation
        if (age >= 18 && age <= 25) score += 5;
        return Math.max(0, Math.min(100, score));
    }

    // ─── Afficher le résultat ────────────────────────────────────────
    private void showResult(double score) {
        int s = (int) Math.round(score);

        addBotMessage("✅ Analyse terminée ! Voici votre score de bonheur.", false);

        // Afficher la zone résultat
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        resultScoreLabel.setText(String.valueOf(s));

        // Interprétation
        String emoji, title, desc;
        String r1, r2, r3;

        if (s >= 80) {
            emoji = "🌟"; title = "Excellent bien-être !";
            desc = "Vous avez un niveau de bonheur remarquable. Vos habitudes de vie sont très équilibrées.";
            r1 = "✅ Continuez vos bonnes habitudes de sommeil et d'étude.";
            r2 = "✅ Partagez vos stratégies avec vos camarades.";
            r3 = "✅ Maintenez cet équilibre même en période d'examens.";
        } else if (s >= 60) {
            emoji = "😊"; title = "Bon niveau de bien-être";
            desc = "Votre bien-être est satisfaisant. Quelques ajustements peuvent encore l'améliorer.";
            r1 = "💡 Essayez d'atteindre 7-8h de sommeil par nuit.";
            r2 = "💡 Faites des pauses régulières pendant vos sessions d'étude.";
            r3 = "💡 Limitez votre consommation de café à 2 par jour.";
        } else if (s >= 40) {
            emoji = "😐"; title = "Bien-être modéré";
            desc = "Votre niveau de bonheur est moyen. Des changements dans vos habitudes peuvent faire une grande différence.";
            r1 = "⚠️ Améliorez votre qualité de sommeil (7-9h recommandées).";
            r2 = "⚠️ Réduisez les heures d'étude excessives et faites des pauses.";
            r3 = "⚠️ Pratiquez une activité physique régulière pour réduire le stress.";
        } else {
            emoji = "😟"; title = "Bien-être à améliorer";
            desc = "Votre score indique un niveau de stress élevé. Il est important d'agir maintenant.";
            r1 = "🚨 Consultez un médecin ou un conseiller académique.";
            r2 = "🚨 Priorisez le sommeil — c'est la base du bien-être.";
            r3 = "🚨 Réduisez la caféine et adoptez des techniques de relaxation.";
        }

        resultEmojiLabel.setText(emoji);
        resultTitleLabel.setText(title);
        resultDescLabel.setText(desc);
        rec1Label.setText(r1);
        rec2Label.setText(r2);
        rec3Label.setText(r3);

        // Couleur du score selon le niveau
        String scoreColor = s >= 80 ? "#1b5e20" : s >= 60 ? "#1565c0" : s >= 40 ? "#e65100" : "#b71c1c";
        resultScoreLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");

        scrollToBottom();
    }

    // ─── Reset ───────────────────────────────────────────────────────
    @FXML
    public void resetChat() {
        currentQuestion = 0;
        answers.clear();
        chatMessagesBox.getChildren().clear();
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        btnSend.setDisable(false);
        answerField.setDisable(false);
        answerField.clear();
        progressLabel.setText("0 / 4 questions");
        initialize();
    }

    // ─── Retour ──────────────────────────────────────────────────────
    @FXML
    public void goBack(ActionEvent event) {
        if (dashboardController != null) {
            // Retour vers stress_options dans le dashboard
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
            }
        }
    }

    // ─── Helpers UI ─────────────────────────────────────────────────
    private void addBotMessage(String text, boolean isQuestion) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar bot
        VBox avatar = new VBox();
        avatar.setAlignment(Pos.TOP_CENTER);
        avatar.setMinWidth(36);
        Label avatarLabel = new Label("🤖");
        avatarLabel.setStyle("-fx-font-size: 18px;");
        avatar.getChildren().add(avatarLabel);

        // Bulle
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        if (isQuestion) {
            bubble.setStyle("-fx-background-color: #1a237e; -fx-text-fill: white; " +
                           "-fx-background-radius: 0 16 16 16; -fx-font-size: 14px; -fx-font-weight: bold;");
        } else {
            bubble.setStyle("-fx-background-color: #e8eaf6; -fx-text-fill: #1a237e; " +
                           "-fx-background-radius: 0 16 16 16; -fx-font-size: 13px;");
        }

        row.getChildren().addAll(avatar, bubble);
        chatMessagesBox.getChildren().add(row);
    }

    private void addUserMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(320);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle("-fx-background-color: #3949ab; -fx-text-fill: white; " +
                       "-fx-background-radius: 16 0 16 16; -fx-font-size: 14px;");

        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-font-size: 18px;");

        row.getChildren().addAll(bubble, avatarLabel);
        chatMessagesBox.getChildren().add(row);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
}
