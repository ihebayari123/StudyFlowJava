package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.services.StressSurveyService;
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
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Chatbot EduWell — Prédit le score de bonheur étudiant
 * via le modèle best_happiness_model.pkl (RandomForestRegressor).
 *
 * Flux de conversation :
 *   1. Demande l'ID StressSurvey
 *   2. Vérifie en BDD → affiche les données si trouvé, sinon erreur
 *   3. Demande l'âge (date de naissance ou âge direct)
 *   4. Demande le nombre de cafés par jour
 *   5. Prédiction ML → affiche le résultat
 */
public class HappinessChatbotController {

    // ─── États de la conversation ────────────────────────────────────
    private enum Step {
        ASK_SURVEY_ID,
        ASK_AGE,
        ASK_COFFEE,
        DONE
    }

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
    @FXML private Label rec1Label;
    @FXML private Label rec2Label;
    @FXML private Label rec3Label;

    // ─── État ────────────────────────────────────────────────────────
    private Step currentStep = Step.ASK_SURVEY_ID;
    private StressSurvey loadedSurvey = null;
    private double ageValue   = 0;
    private double cafeValue  = 0;

    private final StressSurveyService surveyService = new StressSurveyService();
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    // ─── Init ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        currentStep   = Step.ASK_SURVEY_ID;
        loadedSurvey  = null;
        ageValue      = 0;
        cafeValue     = 0;

        addBotMessage("👋 Bonjour ! Je suis votre assistant EduWell.", false);
        addBotMessage("🎓 Je vais analyser votre bien-être grâce à notre modèle d'intelligence artificielle.", false);
        addBotMessage("📊 Je vais utiliser vos données de stress survey (sommeil, étude) ainsi que votre âge et votre consommation de café.", false);

        Platform.runLater(() -> {
            askSurveyId();
            btnSend.setDisable(false);
            answerField.setDisable(false);
            answerField.requestFocus();
        });

        answerField.setOnAction(e -> submitAnswer());
    }

    // ─── Demander l'ID ───────────────────────────────────────────────
    private void askSurveyId() {
        addBotMessage("🔍 Veuillez entrer votre ID dans la table StressSurvey :", true);
        addBotMessage("💬 (Entrez un nombre entier, ex: 3)", false);
        progressLabel.setText("Étape 1 / 3");
    }

    // ─── Soumettre une réponse ───────────────────────────────────────
    @FXML
    public void submitAnswer() {
        String input = answerField.getText().trim();
        if (input.isEmpty()) return;

        answerField.clear();

        switch (currentStep) {
            case ASK_SURVEY_ID -> handleSurveyId(input);
            case ASK_AGE       -> handleAge(input);
            case ASK_COFFEE    -> handleCoffee(input);
            default            -> { /* DONE — ignore */ }
        }

        scrollToBottom();
    }

    // ─── Étape 1 : ID StressSurvey ───────────────────────────────────
    private void handleSurveyId(String input) {
        int id;
        try {
            id = Integer.parseInt(input);
            if (id <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            addUserMessage(input);
            addBotMessage("⚠️ Veuillez entrer un identifiant numérique valide (entier positif).", false);
            addBotMessage("🔍 Entrez votre ID StressSurvey :", true);
            return;
        }

        addUserMessage("ID : " + id);

        // Vérification en BDD
        try {
            StressSurvey survey = surveyService.getById(id);
            if (survey == null) {
                // ID non trouvé
                addBotMessage("❌ Aucun enregistrement trouvé pour l'ID " + id + " dans StressSurvey.", false);
                addBotMessage("🔁 Veuillez vérifier votre ID et réessayer.", false);
                addBotMessage("🔍 Entrez votre ID StressSurvey :", true);
                // On reste sur ASK_SURVEY_ID
            } else {
                // ID trouvé — afficher les données
                loadedSurvey = survey;
                addBotMessage("✅ Données trouvées pour l'ID " + id + " !", false);
                addBotMessage(
                    "📋 Voici vos informations :\n" +
                    "   • Date du survey : " + survey.getDate() + "\n" +
                    "   • Heures de sommeil : " + survey.getSleep_hours() + " h\n" +
                    "   • Heures d'étude   : " + survey.getStudy_hours() + " h\n" +
                    "   • User ID          : " + survey.getUser_id(),
                    false
                );
                addBotMessage("🎯 Ces données seront utilisées pour la prédiction.", false);

                // Passer à l'étape suivante
                currentStep = Step.ASK_AGE;
                progressLabel.setText("Étape 2 / 3");
                addBotMessage(
                    "🎂 Quel est votre âge ?\n" +
                    "   • Entrez votre âge directement (ex: 21)\n" +
                    "   • Ou votre date de naissance au format JJ/MM/AAAA (ex: 15/03/2002)",
                    true
                );
            }
        } catch (SQLException e) {
            addBotMessage("❌ Erreur de base de données : " + e.getMessage(), false);
            addBotMessage("🔍 Entrez votre ID StressSurvey :", true);
        }
    }

    // ─── Étape 2 : Âge ───────────────────────────────────────────────
    private void handleAge(String input) {
        addUserMessage(input);

        // Essayer d'abord comme date JJ/MM/AAAA
        if (input.contains("/") || input.contains("-")) {
            String normalized = input.replace("-", "/");
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate birthDate = LocalDate.parse(normalized, fmt);
                LocalDate today = LocalDate.now();
                if (birthDate.isAfter(today)) {
                    addBotMessage("⚠️ La date de naissance ne peut pas être dans le futur.", false);
                    addBotMessage("🎂 Entrez votre âge ou date de naissance (JJ/MM/AAAA) :", true);
                    return;
                }
                int age = Period.between(birthDate, today).getYears();
                if (age < 10 || age > 100) {
                    addBotMessage("⚠️ L'âge calculé (" + age + " ans) semble invalide.", false);
                    addBotMessage("🎂 Entrez votre âge ou date de naissance (JJ/MM/AAAA) :", true);
                    return;
                }
                ageValue = age;
                addBotMessage("✅ Âge calculé : " + age + " ans (né(e) le " + normalized + ")", false);
                askCoffee();
                return;
            } catch (DateTimeParseException ex) {
                // Pas une date valide — essayer comme nombre
            }
        }

        // Essayer comme nombre direct
        try {
            double age = Double.parseDouble(input.replace(",", "."));
            if (age < 10 || age > 100) {
                addBotMessage("⚠️ L'âge doit être entre 10 et 100 ans.", false);
                addBotMessage("🎂 Entrez votre âge ou date de naissance (JJ/MM/AAAA) :", true);
                return;
            }
            ageValue = age;
            addBotMessage("✅ Âge enregistré : " + (int) age + " ans", false);
            askCoffee();
        } catch (NumberFormatException e) {
            addBotMessage("⚠️ Format non reconnu. Entrez un nombre (ex: 21) ou une date (ex: 15/03/2002).", false);
            addBotMessage("🎂 Entrez votre âge ou date de naissance (JJ/MM/AAAA) :", true);
        }
    }

    private void askCoffee() {
        currentStep = Step.ASK_COFFEE;
        progressLabel.setText("Étape 3 / 3");
        addBotMessage("☕ Combien de cafés buvez-vous par jour ?", true);
        addBotMessage("💬 Entrez un nombre entre 0 et 20 (ex: 2)", false);
    }

    // ─── Étape 3 : Café ──────────────────────────────────────────────
    private void handleCoffee(String input) {
        addUserMessage(input + " café(s)/jour");

        double cafe;
        try {
            cafe = Double.parseDouble(input.replace(",", "."));
            if (cafe < 0 || cafe > 20) {
                addBotMessage("⚠️ Le nombre de cafés doit être entre 0 et 20.", false);
                addBotMessage("☕ Combien de cafés buvez-vous par jour ?", true);
                return;
            }
        } catch (NumberFormatException e) {
            addBotMessage("⚠️ Veuillez entrer un nombre valide (ex: 2).", false);
            addBotMessage("☕ Combien de cafés buvez-vous par jour ?", true);
            return;
        }

        cafeValue = cafe;
        currentStep = Step.DONE;
        btnSend.setDisable(true);
        answerField.setDisable(true);
        progressLabel.setText("Analyse en cours...");

        addBotMessage("✅ Données collectées ! Lancement de la prédiction...", false);
        addBotMessage("⏳ Analyse avec le modèle best_happiness_model.pkl...", false);

        // Lancer la prédiction dans un thread séparé
        double sleepH  = loadedSurvey.getSleep_hours();
        double studyH  = loadedSurvey.getStudy_hours();
        double age     = ageValue;
        double coffees = cafeValue;

        new Thread(() -> {
            String method = "best_happiness_model.pkl";
            double score;
            try {
                score = callPythonModel(sleepH, studyH, age, coffees);
            } catch (Exception ex) {
                score = computeLocalScore(sleepH, studyH, age, coffees);
                method = "estimation locale (Python indisponible : " + ex.getMessage() + ")";
            }
            final double finalScore  = score;
            final String finalMethod = method;
            Platform.runLater(() -> {
                addBotMessage("ℹ️ Prédiction via : " + finalMethod, false);
                showResult(finalScore, sleepH, studyH, age, coffees);
                progressLabel.setText("✅ Analyse terminée");
            });
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════
    //  PRÉDICTION ML — best_happiness_model.pkl via Python
    // ═════════════════════════════════════════════════════════════════

    private double callPythonModel(double sommeil, double etude, double age, double cafe)
            throws Exception {

        String scriptPath = resolveScriptPath("predict_happiness.py");

        // Résoudre le chemin Python (priorité : chemin absolu connu sur Windows)
        String pythonExe = resolvePythonExecutable();

        ProcessBuilder pb = new ProcessBuilder(
                pythonExe,
                scriptPath,
                String.valueOf(sommeil),
                String.valueOf(etude),
                String.valueOf(age),
                String.valueOf(cafe)
        );
        // Working dir = répertoire du script pour que find_model() trouve best_happiness_model.pkl
        pb.directory(new File(scriptPath).getParentFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output   = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode    = process.waitFor();

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
     * Résout l'exécutable Python de manière robuste sur Windows.
     * Essaie "python", puis les chemins courants d'installation Windows.
     */
    private String resolvePythonExecutable() {
        // Chemins courants Python sur Windows
        String[] candidates = {
                "python",
                "python3",
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
                "C:\\Python311\\python.exe",
                "C:\\Python312\\python.exe",
                "C:\\Python310\\python.exe",
        };
        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.isAbsolute() && f.exists()) return candidate;
            // Pour "python" / "python3" — tester via ProcessBuilder
            if (!f.isAbsolute()) {
                try {
                    Process p = new ProcessBuilder(candidate, "--version")
                            .redirectErrorStream(true).start();
                    String out = new String(p.getInputStream().readAllBytes()).trim();
                    p.waitFor();
                    if (out.startsWith("Python")) return candidate;
                } catch (Exception ignored) {}
            }
        }
        return "python"; // fallback
    }

    private String resolveScriptPath(String scriptName) throws FileNotFoundException {
        // 1. Via classpath
        java.net.URL url = getClass().getResource("/" + scriptName);
        if (url != null) {
            try {
                File f = Paths.get(url.toURI()).toFile();
                if (f.exists()) return f.getAbsolutePath();
            } catch (Exception ignored) {}
        }

        // 2. Chercher depuis user.dir
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

    /** Calcul local de secours si Python est indisponible */
    private double computeLocalScore(double sommeil, double etude, double age, double cafe) {
        double score = 50.0;
        if      (sommeil >= 7 && sommeil <= 9) score += 20;
        else if (sommeil >= 6)                 score += 10;
        else if (sommeil < 5)                  score -= 15;
        if      (etude >= 4 && etude <= 6)     score += 15;
        else if (etude > 8)                    score -= 10;
        else if (etude < 2)                    score -= 5;
        if      (cafe <= 2)                    score += 10;
        else if (cafe > 4)                     score -= 15;
        if (age >= 18 && age <= 25)            score += 5;
        return Math.max(0, Math.min(100, score));
    }

    // ═════════════════════════════════════════════════════════════════
    //  AFFICHAGE DU RÉSULTAT
    // ═════════════════════════════════════════════════════════════════

    private void showResult(double score, double sommeil, double etude, double age, double cafe) {
        int s = (int) Math.round(score);

        addBotMessage("🎉 Analyse terminée ! Voici votre score de bonheur personnalisé.", false);

        resultBox.setVisible(true);
        resultBox.setManaged(true);
        resultScoreLabel.setText(String.valueOf(s));

        String emoji, title, desc, r1, r2, r3, scoreColor;

        if (s >= 80) {
            emoji = "🌟"; title = "Excellent bien-être !";
            desc  = "Vous avez un niveau de bonheur remarquable. Vos habitudes de vie sont très équilibrées.";
            r1 = "✅ Continuez vos bonnes habitudes de sommeil (" + (int)sommeil + "h) et d'étude (" + (int)etude + "h).";
            r2 = "✅ Partagez vos stratégies avec vos camarades.";
            r3 = "✅ Maintenez cet équilibre même en période d'examens.";
            scoreColor = "#1b5e20";
        } else if (s >= 60) {
            emoji = "😊"; title = "Bon niveau de bien-être";
            desc  = "Votre bien-être est satisfaisant. Quelques ajustements peuvent encore l'améliorer.";
            r1 = sommeil < 7 ? "💡 Essayez d'atteindre 7-8h de sommeil (actuellement " + (int)sommeil + "h)."
                             : "💡 Votre sommeil est bon (" + (int)sommeil + "h). Maintenez-le !";
            r2 = "💡 Faites des pauses régulières pendant vos sessions d'étude.";
            r3 = cafe > 2 ? "💡 Réduisez votre café à 2/jour (actuellement " + (int)cafe + ")."
                          : "💡 Votre consommation de café est raisonnable.";
            scoreColor = "#1565c0";
        } else if (s >= 40) {
            emoji = "😐"; title = "Bien-être modéré";
            desc  = "Votre niveau de bonheur est moyen. Des changements dans vos habitudes peuvent faire une grande différence.";
            r1 = "⚠️ Améliorez votre sommeil : visez 7-9h (actuellement " + (int)sommeil + "h).";
            r2 = etude > 8 ? "⚠️ Réduisez les heures d'étude excessives (" + (int)etude + "h/jour)."
                           : "⚠️ Pratiquez une activité physique régulière pour réduire le stress.";
            r3 = cafe > 3 ? "⚠️ Réduisez votre consommation de café (" + (int)cafe + "/jour → max 2)."
                          : "⚠️ Consultez un conseiller académique si le stress persiste.";
            scoreColor = "#e65100";
        } else {
            emoji = "😟"; title = "Bien-être à améliorer";
            desc  = "Votre score indique un niveau de stress élevé. Il est important d'agir maintenant.";
            r1 = "🚨 Consultez un médecin ou un conseiller académique dès que possible.";
            r2 = "🚨 Priorisez le sommeil — vous dormez " + (int)sommeil + "h, visez 7-9h.";
            r3 = cafe > 2 ? "🚨 Réduisez drastiquement le café (" + (int)cafe + "/jour) et adoptez des techniques de relaxation."
                          : "🚨 Adoptez des techniques de relaxation (respiration, méditation).";
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
        currentStep  = Step.ASK_SURVEY_ID;
        loadedSurvey = null;
        ageValue     = 0;
        cafeValue    = 0;

        chatMessagesBox.getChildren().clear();
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        btnSend.setDisable(false);
        answerField.setDisable(false);
        answerField.clear();
        progressLabel.setText("Étape 1 / 3");

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
