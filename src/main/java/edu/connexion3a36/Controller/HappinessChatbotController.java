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
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Chatbot EduWell — Prédit le score de bonheur étudiant
 * via le modèle best_happiness_model.pkl (RandomForestRegressor)
 * Features : heures_sommeil, heures_etude, age, cafes_par_jour
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
    @FXML private VBox       chatMessagesBox;
    @FXML private TextField  answerField;
    @FXML private Button     btnSend;
    @FXML private Button     btnReset;
    @FXML private Label      progressLabel;

    // Zone résultat
    @FXML private VBox  resultBox;
    @FXML private Label resultScoreLabel;
    @FXML private Label resultEmojiLabel;
    @FXML private Label resultTitleLabel;
    @FXML private Label resultDescLabel;
    @FXML private VBox  recommendationsBox;
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
        addBotMessage("👋 Bonjour ! Je suis votre assistant EduWell.", false);
        addBotMessage("🎓 Je vais analyser votre bien-être étudiant grâce à notre modèle d'intelligence artificielle.", false);
        addBotMessage("📊 Je vous poserai 4 questions simples, puis je calculerai votre score de bonheur personnalisé.", false);
        addBotMessage("✨ Commençons !", false);

        Platform.runLater(() -> {
            addBotMessage(QUESTIONS[0][0], true);
            addBotMessage("💬 " + QUESTIONS[0][1], false);
            progressLabel.setText("1 / 4 questions");
            btnSend.setDisable(false);
            answerField.setDisable(false);
            answerField.requestFocus();
        });

        answerField.setOnAction(e -> submitAnswer());
    }

    // ─── Soumettre une réponse ───────────────────────────────────────
    @FXML
    public void submitAnswer() {
        String input = answerField.getText().trim();
        if (input.isEmpty()) return;

        double value;
        try {
            value = Double.parseDouble(input.replace(",", "."));
        } catch (NumberFormatException ex) {
            addBotMessage("⚠️ Veuillez entrer un nombre valide (ex: 7).", false);
            answerField.clear();
            return;
        }

        String error = validateAnswer(currentQuestion, value);
        if (error != null) {
            addBotMessage("⚠️ " + error, false);
            answerField.clear();
            return;
        }

        addUserMessage(input + " " + getUnit(currentQuestion));
        answerField.clear();
        answers.add(value);
        currentQuestion++;

        if (currentQuestion < QUESTIONS.length) {
            progressLabel.setText((currentQuestion + 1) + " / 4 questions");
            addBotMessage(QUESTIONS[currentQuestion][0], true);
            addBotMessage("💬 " + QUESTIONS[currentQuestion][1], false);
        } else {
            progressLabel.setText("Analyse en cours...");
            btnSend.setDisable(true);
            answerField.setDisable(true);
            addBotMessage("⏳ Analyse de vos données en cours avec le modèle IA...", false);
            runPrediction();
        }

        scrollToBottom();
    }

    // ─── Validation ─────────────────────────────────────────────────
    private String validateAnswer(int q, double v) {
        return switch (q) {
            case 0 -> (v < 1 || v > 12)  ? "Les heures de sommeil doivent être entre 1 et 12." : null;
            case 1 -> (v < 0 || v > 16)  ? "Les heures d'étude doivent être entre 0 et 16."   : null;
            case 2 -> (v < 10 || v > 100) ? "L'âge doit être entre 10 et 100."                : null;
            case 3 -> (v < 0 || v > 20)  ? "Le nombre de cafés doit être entre 0 et 20."      : null;
            default -> null;
        };
    }

    private String getUnit(int q) {
        return switch (q) {
            case 0 -> "h de sommeil";
            case 1 -> "h d'étude";
            case 2 -> "ans";
            case 3 -> "café(s)/jour";
            default -> "";
        };
    }

    // ═════════════════════════════════════════════════════════════════
    //  PRÉDICTION ML — best_happiness_model.pkl via Python
    // ═════════════════════════════════════════════════════════════════

    private void runPrediction() {
        double sommeil = answers.get(0);
        double etude   = answers.get(1);
        double age     = answers.get(2);
        double cafe    = answers.get(3);

        new Thread(() -> {
            String method = "modèle IA (best_happiness_model.pkl)";
            double score;
            try {
                score = callPythonModel(sommeil, etude, age, cafe);
            } catch (Exception ex) {
                // Fallback local si Python indisponible
                score = computeLocalScore(sommeil, etude, age, cafe);
                method = "estimation locale (Python indisponible : " + ex.getMessage() + ")";
            }
            final double finalScore = score;
            final String finalMethod = method;
            Platform.runLater(() -> {
                addBotMessage("ℹ️ Prédiction via : " + finalMethod, false);
                showResult(finalScore);
            });
        }).start();
    }

    /**
     * Appelle predict_happiness.py avec les 4 features.
     * Résout le chemin du script de manière robuste (gère les espaces Windows).
     */
    private double callPythonModel(double sommeil, double etude, double age, double cafe)
            throws Exception {

        String scriptPath = resolveScriptPath("predict_happiness.py");

        ProcessBuilder pb = new ProcessBuilder(
                "python",
                scriptPath,
                String.valueOf(sommeil),
                String.valueOf(etude),
                String.valueOf(age),
                String.valueOf(cafe)
        );
        // Définir le working directory = répertoire du script
        // pour que find_model() dans le .py trouve best_happiness_model.pkl
        pb.directory(new File(scriptPath).getParentFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode  = process.waitFor();

        if (exitCode != 0 || output.startsWith("ERROR")) {
            throw new RuntimeException(output);
        }

        // Extraire la dernière ligne numérique (ignore les warnings sklearn)
        String[] lines = output.split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(line);
            }
        }
        throw new RuntimeException("Sortie Python non numérique : " + output);
    }

    /**
     * Résout le chemin absolu du script Python de manière robuste.
     * Priorité :
     *   1. Classpath (target/classes) — via URI pour gérer les espaces
     *   2. src/main/resources (développement)
     *   3. Répertoire courant
     */
    private String resolveScriptPath(String scriptName) throws FileNotFoundException {
        // 1. Via classpath (fonctionne en dev et en production)
        URL url = getClass().getResource("/" + scriptName);
        if (url != null) {
            try {
                // Utiliser URI pour décoder correctement les %20 et autres caractères
                File f = Paths.get(url.toURI()).toFile();
                if (f.exists()) return f.getAbsolutePath();
            } catch (Exception ignored) {}
        }

        // 2. Chercher depuis user.dir vers le haut
        File base = new File(System.getProperty("user.dir"));
        String[] subPaths = {
                "src/main/resources/" + scriptName,
                "target/classes/"     + scriptName,
                scriptName
        };
        for (int depth = 0; depth < 5; depth++) {
            for (String sub : subPaths) {
                File candidate = new File(base, sub);
                if (candidate.exists()) return candidate.getAbsolutePath();
            }
            base = base.getParentFile();
            if (base == null) break;
        }

        // 3. Extraire depuis le jar vers un fichier temporaire
        try (InputStream is = getClass().getResourceAsStream("/" + scriptName)) {
            if (is != null) {
                Path tmp = Files.createTempFile("predict_happiness_", ".py");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return tmp.toAbsolutePath().toString();
            }
        } catch (IOException ignored) {}

        throw new FileNotFoundException("Script introuvable : " + scriptName);
    }

    /**
     * Calcul local de secours — calibré sur les mêmes plages que le modèle RF.
     */
    private double computeLocalScore(double sommeil, double etude, double age, double cafe) {
        double score = 50.0;
        // Sommeil : optimal 7-9h
        if      (sommeil >= 7 && sommeil <= 9) score += 20;
        else if (sommeil >= 6)                 score += 10;
        else if (sommeil < 5)                  score -= 15;
        // Étude : optimal 4-6h
        if      (etude >= 4 && etude <= 6)     score += 15;
        else if (etude > 8)                    score -= 10;
        else if (etude < 2)                    score -= 5;
        // Café : optimal ≤ 2
        if      (cafe <= 2)                    score += 10;
        else if (cafe > 4)                     score -= 15;
        // Âge
        if (age >= 18 && age <= 25)            score += 5;
        return Math.max(0, Math.min(100, score));
    }

    // ═════════════════════════════════════════════════════════════════
    //  AFFICHAGE DU RÉSULTAT
    // ═════════════════════════════════════════════════════════════════

    private void showResult(double score) {
        int s = (int) Math.round(score);

        addBotMessage("✅ Analyse terminée ! Voici votre score de bonheur.", false);

        resultBox.setVisible(true);
        resultBox.setManaged(true);
        resultScoreLabel.setText(String.valueOf(s));

        String emoji, title, desc, r1, r2, r3, scoreColor;

        if (s >= 80) {
            emoji = "🌟"; title = "Excellent bien-être !";
            desc  = "Vous avez un niveau de bonheur remarquable. Vos habitudes de vie sont très équilibrées.";
            r1 = "✅ Continuez vos bonnes habitudes de sommeil et d'étude.";
            r2 = "✅ Partagez vos stratégies avec vos camarades.";
            r3 = "✅ Maintenez cet équilibre même en période d'examens.";
            scoreColor = "#1b5e20";
        } else if (s >= 60) {
            emoji = "😊"; title = "Bon niveau de bien-être";
            desc  = "Votre bien-être est satisfaisant. Quelques ajustements peuvent encore l'améliorer.";
            r1 = "💡 Essayez d'atteindre 7-8h de sommeil par nuit.";
            r2 = "💡 Faites des pauses régulières pendant vos sessions d'étude.";
            r3 = "💡 Limitez votre consommation de café à 2 par jour.";
            scoreColor = "#1565c0";
        } else if (s >= 40) {
            emoji = "😐"; title = "Bien-être modéré";
            desc  = "Votre niveau de bonheur est moyen. Des changements dans vos habitudes peuvent faire une grande différence.";
            r1 = "⚠️ Améliorez votre qualité de sommeil (7-9h recommandées).";
            r2 = "⚠️ Réduisez les heures d'étude excessives et faites des pauses.";
            r3 = "⚠️ Pratiquez une activité physique régulière pour réduire le stress.";
            scoreColor = "#e65100";
        } else {
            emoji = "😟"; title = "Bien-être à améliorer";
            desc  = "Votre score indique un niveau de stress élevé. Il est important d'agir maintenant.";
            r1 = "🚨 Consultez un médecin ou un conseiller académique.";
            r2 = "🚨 Priorisez le sommeil — c'est la base du bien-être.";
            r3 = "🚨 Réduisez la caféine et adoptez des techniques de relaxation.";
            scoreColor = "#b71c1c";
        }

        resultEmojiLabel.setText(emoji);
        resultTitleLabel.setText(title);
        resultDescLabel.setText(desc);
        rec1Label.setText(r1);
        rec2Label.setText(r2);
        rec3Label.setText(r3);
        resultScoreLabel.setStyle(
                "-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");

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

        VBox avatar = new VBox();
        avatar.setAlignment(Pos.TOP_CENTER);
        avatar.setMinWidth(36);
        Label avatarLabel = new Label("🤖");
        avatarLabel.setStyle("-fx-font-size: 18px;");
        avatar.getChildren().add(avatarLabel);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle(isQuestion
                ? "-fx-background-color: #1a237e; -fx-text-fill: white; "
                  + "-fx-background-radius: 0 16 16 16; -fx-font-size: 14px; -fx-font-weight: bold;"
                : "-fx-background-color: #e8eaf6; -fx-text-fill: #1a237e; "
                  + "-fx-background-radius: 0 16 16 16; -fx-font-size: 13px;");

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
        bubble.setStyle("-fx-background-color: #3949ab; -fx-text-fill: white; "
                + "-fx-background-radius: 16 0 16 16; -fx-font-size: 14px;");

        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-font-size: 18px;");

        row.getChildren().addAll(bubble, avatarLabel);
        chatMessagesBox.getChildren().add(row);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
}
