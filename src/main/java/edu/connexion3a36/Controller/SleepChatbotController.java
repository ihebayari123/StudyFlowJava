package edu.connexion3a36.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Controller for the Sleep Analysis chatbot page.
 * Embedded inside fitness_dashboard2.fxml (sleepArea StackPane).
 * Uses analyze_eyes.py + sleep_model_export to predict sleep hours via webcam.
 */
public class SleepChatbotController {

    private static final int ANALYSIS_DURATION = 8; // secondes d'analyse

    private FitnessDashboardController dashboardController;
    private VBox chatBox;
    private ScrollPane chatScroll;
    private Button btnAnalyze;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Timeline progressTimeline;
    private boolean isAnalyzing = false;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    /** Construit l'UI complète du chatbot sommeil. */
    public VBox buildUI() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0d1b2a;");

        // ── Panneau gauche (info) + panneau droit (chat) ──────────────
        HBox mainRow = new HBox(0);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        VBox leftPanel  = buildLeftPanel();
        VBox rightPanel = buildRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        mainRow.getChildren().addAll(leftPanel, rightPanel);
        root.getChildren().add(mainRow);
        return root;
    }

    // ── Panneau gauche ────────────────────────────────────────────────
    private VBox buildLeftPanel() {
        VBox panel = new VBox(20);
        panel.setPrefWidth(280);
        panel.setMinWidth(260);
        panel.setStyle("-fx-background-color: linear-gradient(to bottom, #0d1b2a, #1a1a3e, #2d1b69);");
        panel.setPadding(new Insets(30, 20, 30, 20));

        // Logo
        VBox logoBox = new VBox(8);
        logoBox.setAlignment(Pos.CENTER);
        Label logoIcon = new Label("😴");
        logoIcon.setStyle("-fx-font-size: 52px;");
        Label logoTitle = new Label("SleepAI");
        logoTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c4dff; -fx-font-family: 'Georgia';");
        Label logoSub = new Label("Analyse de Sommeil par IA");
        logoSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9e9e9e; -fx-text-alignment: center;");
        logoSub.setWrapText(true);
        logoBox.getChildren().addAll(logoIcon, logoTitle, logoSub);

        // Séparateur
        HBox sep = new HBox();
        sep.setStyle("-fx-background-color: #7c4dff; -fx-min-height: 1; -fx-max-height: 1;");

        // Infos modèle
        Label modelTitle = new Label("🤖 Modèle IA");
        modelTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #b39ddb;");

        VBox modelInfo = new VBox(10);
        modelInfo.getChildren().addAll(
            buildInfoCard("📊 Algorithme", "Linear Regression"),
            buildInfoCard("👁️ Analyse", "12 métriques oculaires"),
            buildInfoCard("⏱️ Durée", ANALYSIS_DURATION + " secondes"),
            buildInfoCard("🎯 Précision", "Basée sur EAR, rougeur, cernes")
        );

        // Métriques analysées
        Label metricsTitle = new Label("📋 Métriques analysées");
        metricsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #b39ddb;");

        VBox metricsList = new VBox(5);
        for (String m : new String[]{
            "• Ratio d'ouverture des yeux (EAR)",
            "• Taux de clignement",
            "• Score de rougeur",
            "• Dilatation pupillaire",
            "• Cernes sous les yeux",
            "• Ptosis (paupières tombantes)",
            "• Stabilité du regard"
        }) {
            Label ml = new Label(m);
            ml.setStyle("-fx-font-size: 11px; -fx-text-fill: #9e9e9e;");
            metricsList.getChildren().add(ml);
        }

        // Conseil
        VBox tipBox = new VBox(6);
        tipBox.setStyle("-fx-background-color: rgba(124,77,255,0.15); -fx-background-radius: 10; -fx-padding: 12;");
        Label tipTitle = new Label("💡 Conseil");
        tipTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7c4dff;");
        Label tipText = new Label("Assurez-vous d'être dans un endroit bien éclairé et regardez directement la caméra.");
        tipText.setStyle("-fx-font-size: 11px; -fx-text-fill: #9e9e9e; -fx-wrap-text: true;");
        tipBox.getChildren().addAll(tipTitle, tipText);

        panel.getChildren().addAll(logoBox, sep, modelTitle, modelInfo, metricsTitle, metricsList, tipBox);
        VBox.setVgrow(metricsList, Priority.ALWAYS);
        return panel;
    }

    private VBox buildInfoCard(String label, String value) {
        VBox card = new VBox(3);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 8; -fx-padding: 10;");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #9e9e9e;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        card.getChildren().addAll(l, v);
        return card;
    }

    // ── Panneau droit (chat) ──────────────────────────────────────────
    private VBox buildRightPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1923;");

        // Header
        HBox header = new HBox(14);
        header.setPrefHeight(70);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 24, 0, 24));
        header.setStyle("-fx-background-color: #0d1b2a; -fx-border-color: transparent transparent #1e2d3d transparent; -fx-border-width: 0 0 1 0;");

        VBox avatarBox = new VBox();
        avatarBox.setAlignment(Pos.CENTER);
        avatarBox.setStyle("-fx-background-color: #7c4dff; -fx-background-radius: 50%; " +
                           "-fx-min-width: 44; -fx-min-height: 44; -fx-max-width: 44; -fx-max-height: 44;");
        Label avatarLbl = new Label("🤖");
        avatarLbl.setStyle("-fx-font-size: 22px;");
        avatarBox.getChildren().add(avatarLbl);

        VBox headerInfo = new VBox(3);
        Label headerTitle = new Label("Assistant SleepAI");
        headerTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");
        HBox onlineRow = new HBox(6);
        onlineRow.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5, Color.web("#4caf50"));
        Label onlineLbl = new Label("En ligne — Analyse IA prête");
        onlineLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #4caf50;");
        onlineRow.getChildren().addAll(dot, onlineLbl);
        headerInfo.getChildren().addAll(headerTitle, onlineRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("Prêt");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b949e; " +
                             "-fx-background-color: #161b22; -fx-padding: 5 12; -fx-background-radius: 20;");

        header.getChildren().addAll(avatarBox, headerInfo, spacer, statusLabel);

        // Zone de chat
        chatScroll = new ScrollPane();
        chatScroll.setFitToWidth(true);
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScroll.setStyle("-fx-background-color: #0f1923; -fx-border-color: transparent;");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        chatBox = new VBox(12);
        chatBox.setPadding(new Insets(20));
        chatScroll.setContent(chatBox);

        // Barre d'action
        VBox actionBar = buildActionBar();

        panel.getChildren().addAll(header, chatScroll, actionBar);

        // Message de bienvenue
        Platform.runLater(this::sendWelcomeMessages);

        return panel;
    }

    private VBox buildActionBar() {
        VBox bar = new VBox(14);
        bar.setPadding(new Insets(16, 24, 20, 24));
        bar.setStyle("-fx-background-color: #0d1b2a; -fx-border-color: #1e2d3d transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        // Barre de progression
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: #7c4dff; -fx-background-color: #1e2d3d; -fx-background-radius: 5;");
        progressBar.setVisible(false);

        Label progressLbl = new Label("⏳ Analyse en cours — regardez la caméra...");
        progressLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c4dff; -fx-font-weight: bold;");
        progressLbl.setVisible(false);

        // ── Bouton "Début de test" (principal) ───────────────────────
        btnAnalyze = new Button("🎯  Début de test — Analyse des yeux");
        btnAnalyze.setMaxWidth(Double.MAX_VALUE);
        btnAnalyze.setPrefHeight(56);
        btnAnalyze.setStyle(
            "-fx-background-color: linear-gradient(to right, #00c853, #1b5e20); " +
            "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; " +
            "-fx-background-radius: 14; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,200,83,0.55), 16, 0, 0, 5);");
        btnAnalyze.setOnMouseEntered(e -> btnAnalyze.setStyle(
            "-fx-background-color: linear-gradient(to right, #00e676, #2e7d32); " +
            "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; " +
            "-fx-background-radius: 14; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,230,118,0.7), 20, 0, 0, 6);"));
        btnAnalyze.setOnMouseExited(e -> {
            if (!isAnalyzing) btnAnalyze.setStyle(
                "-fx-background-color: linear-gradient(to right, #00c853, #1b5e20); " +
                "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-background-radius: 14; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,200,83,0.55), 16, 0, 0, 5);");
        });
        btnAnalyze.setOnAction(e -> startAnalysis(progressBar, progressLbl));

        // Hint
        Label hint = new Label("📌 La caméra s'ouvre automatiquement — regardez directement l'objectif");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #484f58; -fx-font-style: italic;");
        hint.setWrapText(true);

        bar.getChildren().addAll(progressBar, progressLbl, btnAnalyze, hint);
        return bar;
    }

    // ── Messages de bienvenue ─────────────────────────────────────────
    private void sendWelcomeMessages() {
        addBotMessage("👋 Bonjour ! Je suis votre assistant SleepAI.", false);
        addBotMessage("😴 Je vais analyser vos yeux via la caméra de votre PC pour estimer vos heures de sommeil.", false);
        addBotMessage("🔬 Mon modèle analyse 12 métriques oculaires : ouverture des yeux (EAR), rougeur, cernes, clignements, ptosis et plus encore.", false);
        addBotMessage("🟢 Des points verts s'afficheront sur vos yeux en temps réel pendant l'analyse.", false);
        addBotMessage("📷 Cliquez sur « Début de test » ci-dessous. Regardez directement la caméra pendant "
                + ANALYSIS_DURATION + " secondes.", true);
    }

    // ── Démarrer l'analyse ────────────────────────────────────────────
    private void startAnalysis(ProgressBar pb, Label pbLbl) {
        if (isAnalyzing) return;
        isAnalyzing = true;
        btnAnalyze.setDisable(true);
        pb.setVisible(true);
        pbLbl.setVisible(true);
        pb.setProgress(0);
        statusLabel.setText("Analyse en cours...");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7c4dff; " +
                             "-fx-background-color: #1e1040; -fx-padding: 5 12; -fx-background-radius: 20;");

        addUserMessage("🎯 Début de test — Analyse de mes yeux");
        addBotMessage("📷 Ouverture de la caméra... Une fenêtre va s'ouvrir avec les points verts sur vos yeux.", false);
        addBotMessage("⏳ Analyse en cours pendant " + ANALYSIS_DURATION + " secondes — regardez la caméra.", false);

        // Barre de progression animée
        progressTimeline = new Timeline(
            new KeyFrame(Duration.millis(100), e -> {
                double current = pb.getProgress();
                pb.setProgress(Math.min(current + (0.1 / ANALYSIS_DURATION), 0.95));
            })
        );
        progressTimeline.setCycleCount((int)(ANALYSIS_DURATION * 10));
        progressTimeline.play();

        // Lancer le script Python en arrière-plan
        new Thread(() -> {
            try {
                String result = runPythonAnalysis();
                Platform.runLater(() -> {
                    if (progressTimeline != null) progressTimeline.stop();
                    pb.setProgress(1.0);
                    pbLbl.setText("Analyse terminée !");
                    isAnalyzing = false;
                    btnAnalyze.setDisable(false);
                    statusLabel.setText("Analyse terminée");
                    statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4caf50; " +
                                        "-fx-background-color: #0d2818; -fx-padding: 5 12; -fx-background-radius: 20;");
                    displayResult(result);

                    // Cacher la barre après 3s
                    new Timeline(new KeyFrame(Duration.seconds(3), ev -> {
                        pb.setVisible(false);
                        pbLbl.setVisible(false);
                        pb.setProgress(0);
                    })).play();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (progressTimeline != null) progressTimeline.stop();
                    pb.setVisible(false);
                    pbLbl.setVisible(false);
                    isAnalyzing = false;
                    btnAnalyze.setDisable(false);
                    statusLabel.setText("Erreur");
                    addBotMessage("❌ Erreur lors de l'analyse : " + ex.getMessage(), false);
                });
            }
        }).start();
    }

    // ── Appel Python ──────────────────────────────────────────────────
    private String runPythonAnalysis() throws Exception {
        String scriptPath = findScript("analyze_eyes.py");
        File   scriptDir  = new File(scriptPath).getParentFile();

        ProcessBuilder pb = new ProcessBuilder(
            "python", scriptPath, String.valueOf(ANALYSIS_DURATION)
        );
        // Définir le working directory = dossier du script
        // pour que le modèle soit trouvé par analyze_eyes.py
        pb.directory(scriptDir);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Lire stdout (JSON) et stderr séparément
        String stdout = new String(process.getInputStream().readAllBytes()).trim();
        String stderr = new String(process.getErrorStream().readAllBytes()).trim();
        int exitCode  = process.waitFor();

        // Extraire la dernière ligne JSON valide (ignore les warnings Python)
        String jsonLine = extractLastJsonLine(stdout);

        if (jsonLine == null || jsonLine.isEmpty()) {
            String errMsg = stderr.isEmpty() ? stdout : stderr;
            throw new RuntimeException("Pas de sortie JSON valide. " +
                (errMsg.length() > 200 ? errMsg.substring(0, 200) : errMsg));
        }

        // Vérifier si le JSON contient une erreur Python
        if (jsonLine.contains("\"error\"")) {
            String errVal = extractJsonString(jsonLine, "error");
            throw new RuntimeException(errVal.isEmpty() ? jsonLine : errVal);
        }

        return jsonLine;
    }

    /**
     * Extrait la dernière ligne qui ressemble à un objet JSON { ... }
     */
    private String extractLastJsonLine(String output) {
        if (output == null || output.isEmpty()) return null;
        String[] lines = output.split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) return line;
        }
        return null;
    }

    /**
     * Résout le chemin absolu de analyze_eyes.py de manière robuste.
     * Cherche dans sleep_model_export/ depuis user.dir et ses parents.
     */
    private String findScript(String name) throws FileNotFoundException {
        // 1. Via classpath (URL décodée correctement)
        URL url = getClass().getResource("/" + name);
        if (url != null) {
            try {
                File f = Paths.get(url.toURI()).toFile();
                if (f.exists()) return f.getAbsolutePath();
            } catch (Exception ignored) {}
        }

        // 2. Chercher depuis user.dir vers le haut
        File base = new File(System.getProperty("user.dir"));
        String[] subPaths = {
            "sleep_model_export/" + name,
            "src/main/resources/" + name,
            name
        };
        for (int depth = 0; depth < 6; depth++) {
            for (String sub : subPaths) {
                File candidate = new File(base, sub);
                if (candidate.exists()) return candidate.getAbsolutePath();
            }
            File parent = base.getParentFile();
            if (parent == null) break;
            base = parent;
        }

        throw new FileNotFoundException(
            "Script introuvable: " + name + " (cherché depuis " +
            System.getProperty("user.dir") + ")");
    }

    // ── Afficher le résultat ──────────────────────────────────────────
    private void displayResult(String jsonOutput) {
        try {
            // ── Parser avec Jackson (robuste aux espaces, backslashes, etc.) ──
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonOutput);

            double hours   = root.path("hours_slept").asDouble(0.0);
            double missing = root.path("missing_hours").asDouble(0.0);
            String status  = root.path("status").asText("CRITIQUE");
            String message = root.path("message").asText("");
            String rec     = root.path("recommendation").asText("");
            String imgPath = root.path("image_path").asText("");
            boolean simulated = root.path("simulated").asBoolean(false);

            if (simulated) {
                addBotMessage("ℹ️ Caméra non détectée — simulation utilisée.", false);
            }

            // ── Message principal ─────────────────────────────────────
            addBotMessage("✅ Analyse terminée ! Voici vos résultats.", false);

            // ── Carte résultat ────────────────────────────────────────
            VBox resultCard = buildResultCard(hours, missing, status, message);
            addCustomNode(resultCard);

            // ── Messages personnalisés ────────────────────────────────
            addPersonalizedMessages(hours, missing, status);

            // ── Recommandation ────────────────────────────────────────
            if (!rec.isEmpty()) {
                addBotMessage("💡 Conseil personnalisé : " + rec, false);
            }

            // ── Observations détectées ────────────────────────────────
            JsonNode details = root.path("details");
            if (details.isArray() && details.size() > 0) {
                addBotMessage("🔬 Observations détectées :", false);
                for (JsonNode d : details) {
                    String txt = d.asText("").trim();
                    if (!txt.isEmpty()) addBotMessage("  • " + txt, false);
                }
            }

            // ── Image résumé (capture au 3ème pip) ───────────────────
            if (!imgPath.isEmpty()) {
                File imgFile = new File(imgPath);
                if (imgFile.exists()) {
                    addBotMessage("📸 Capture prise automatiquement à la fin de l'analyse :", false);
                    addImageNode(imgPath, hours, missing, status, root);
                }
            }

            scrollToBottom();

        } catch (Exception e) {
            // Fallback si Jackson échoue (ne devrait pas arriver)
            addBotMessage("❌ Erreur de lecture du résultat : " + e.getMessage(), false);
            addBotMessage("📋 Données brutes : " + jsonOutput.substring(0, Math.min(300, jsonOutput.length())), false);
        }
    }

    /**
     * Messages personnalisés et encourageants selon le score de sommeil.
     */
    private void addPersonalizedMessages(double hours, double missing, String status) {
        switch (status) {
            case "OK" -> {
                if (hours >= 9) {
                    addBotMessage("🌟 Exceptionnel ! " + hours + "h de sommeil — vous êtes au top de votre forme !", true);
                    addBotMessage("🧠 Votre cerveau est parfaitement reposé. Profitez de cette énergie pour apprendre et créer !", false);
                    addBotMessage("💪 Continuez comme ça — votre discipline du sommeil est exemplaire.", false);
                } else {
                    addBotMessage("😊 Bravo ! " + hours + "h de sommeil — vous dormez bien !", true);
                    addBotMessage("✨ Votre corps et votre esprit sont bien récupérés. Vous êtes prêt(e) pour une journée productive !", false);
                    addBotMessage("🎯 Maintenez cette routine — un bon sommeil est la clé de la réussite académique.", false);
                }
            }
            case "insuffisant" -> {
                addBotMessage("⚠️ Attention : seulement " + hours + "h de sommeil détectées.", true);
                addBotMessage("😴 Il vous manque " + missing + "h pour atteindre les 8h recommandées.", false);
                if (missing <= 1.5) {
                    addBotMessage("💛 Vous n'êtes pas loin ! Un coucher 1h plus tôt ce soir suffira à récupérer.", false);
                    addBotMessage("📱 Astuce : activez le mode nuit sur vos appareils 1h avant de dormir.", false);
                } else {
                    addBotMessage("🔴 Ce déficit de sommeil affecte votre concentration et votre mémoire.", false);
                    addBotMessage("🛌 Priorité ce soir : couchez-vous tôt et évitez la caféine après 15h.", false);
                    addBotMessage("📅 Essayez de récupérer progressivement sur 2-3 nuits.", false);
                }
            }
            default -> {
                addBotMessage("🚨 ALERTE : Seulement " + hours + "h de sommeil — situation critique !", true);
                addBotMessage("⚡ Votre corps est en état de stress sévère. Votre système immunitaire et cognitif sont affectés.", false);
                addBotMessage("🏥 Si ce manque est chronique (plus de 3 jours), consultez un médecin.", false);
                addBotMessage("😴 Action immédiate : faites une sieste de 20 min maintenant si possible.", false);
                addBotMessage("🌙 Ce soir, couchez-vous dès 21h et éteignez tous les écrans.", false);
            }
        }
    }

    /**
     * Affiche la capture + paragraphe descriptif dans le chat.
     * La capture a été prise automatiquement au 3ème pip par analyze_eyes.py.
     */
    private void addImageNode(String imagePath, double hours, double missing,
                               String status, JsonNode root) {
        try {
            File imgFile = new File(imagePath);
            if (!imgFile.exists()) {
                addBotMessage("⚠️ Capture non disponible.", false);
                return;
            }

            // ── Paragraphe descriptif de l'état ──────────────────────
            String paragraph = buildDescriptiveParagraph(hours, missing, status, root);
            addBotMessage(paragraph, false);

            // ── Carte image ───────────────────────────────────────────
            VBox imgCard = new VBox(10);
            imgCard.setStyle(
                "-fx-background-color: #0d1b2a; -fx-background-radius: 14; " +
                "-fx-border-color: " + getStatusColor(status) + "; " +
                "-fx-border-radius: 14; -fx-border-width: 2; -fx-padding: 14;");
            imgCard.setMaxWidth(540);

            // Titre de la carte
            HBox cardHeader = new HBox(10);
            cardHeader.setAlignment(Pos.CENTER_LEFT);
            Label camIcon = new Label("📸");
            camIcon.setStyle("-fx-font-size: 18px;");
            VBox headerText = new VBox(2);
            Label cardTitle = new Label("Capture — Analyse en temps réel");
            cardTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");
            Label cardSub = new Label("Prise automatiquement au 3ème pip de fin d'analyse");
            cardSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #484f58; -fx-font-style: italic;");
            headerText.getChildren().addAll(cardTitle, cardSub);
            cardHeader.getChildren().addAll(camIcon, headerText);

            // Image
            Image img = new Image(imgFile.toURI().toString(), 516, 0, true, true, false);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(516);
            iv.setPreserveRatio(true);
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0, 0, 3);");

            // Badge statut
            Label badge = new Label(getStatusEmoji(status) + "  " + hours + "h  —  " + getStatusText(status));
            badge.setStyle(
                "-fx-background-color: " + getStatusColor(status) + "; " +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-padding: 6 16; -fx-background-radius: 20;");

            imgCard.getChildren().addAll(cardHeader, iv, badge);
            addCustomNode(imgCard);

        } catch (Exception e) {
            addBotMessage("⚠️ Impossible d'afficher la capture : " + e.getMessage(), false);
        }
    }

    /**
     * Génère un paragraphe descriptif personnalisé basé sur les métriques réelles.
     */
    private String buildDescriptiveParagraph(double hours, double missing,
                                              String status, JsonNode root) {
        JsonNode metrics = root.path("metrics");
        double ear       = metrics.path("ear_ratio").asDouble(0.29);
        double redness   = metrics.path("redness_score").asDouble(0.36);
        double dark      = metrics.path("dark_circles_score").asDouble(0.30);
        double blink     = metrics.path("blink_rate_per_min").asDouble(16.0);
        double ptosis    = metrics.path("ptosis_score").asDouble(0.34);
        double gaze      = metrics.path("gaze_stability").asDouble(0.75);

        StringBuilder sb = new StringBuilder("📋 Rapport d'état : ");

        switch (status) {
            case "OK" -> {
                sb.append("Votre analyse révèle un état de repos satisfaisant avec ")
                  .append(hours).append("h de sommeil estimées. ");
                if (ear > 0.30) sb.append("Vos yeux sont bien ouverts (EAR=").append(String.format("%.2f", ear)).append("), signe d'une bonne vigilance. ");
                if (redness < 0.35) sb.append("Aucune rougeur oculaire significative détectée. ");
                if (gaze > 0.75) sb.append("Votre regard est stable et concentré. ");
                sb.append("Continuez à maintenir ces bonnes habitudes de sommeil !");
            }
            case "insuffisant" -> {
                sb.append("L'analyse détecte un manque de sommeil de ").append(missing).append("h. ");
                if (redness > 0.45) sb.append("Une rougeur oculaire est visible (score=").append(String.format("%.2f", redness)).append("), indiquant de la fatigue. ");
                if (dark > 0.35) sb.append("Des cernes sont détectés sous vos yeux. ");
                if (blink < 12) sb.append("Votre taux de clignement est faible (").append(String.format("%.0f", blink)).append("/min), signe de sécheresse oculaire. ");
                if (ptosis > 0.25) sb.append("Un léger affaissement des paupières est observé. ");
                sb.append("Un repos supplémentaire de ").append(missing).append("h est recommandé.");
            }
            default -> {
                sb.append("Situation critique : seulement ").append(hours).append("h de sommeil détectées. ");
                if (ear < 0.25) sb.append("L'ouverture oculaire est très réduite (EAR=").append(String.format("%.2f", ear)).append("). ");
                if (redness > 0.55) sb.append("Forte rougeur oculaire détectée — fatigue sévère. ");
                if (ptosis > 0.35) sb.append("Paupières tombantes significatives observées. ");
                if (gaze < 0.65) sb.append("Instabilité du regard détectée. ");
                sb.append("Il vous manque ").append(missing).append("h de sommeil. Consultez un médecin si cela persiste.");
            }
        }

        return sb.toString();
    }
    private VBox buildResultCard(double hours, double missing, String status, String message) {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: " + getStatusBg(status) + "; " +
                      "-fx-background-radius: 16; -fx-border-color: " + getStatusColor(status) + "; " +
                      "-fx-border-radius: 16; -fx-border-width: 2;");
        card.setPadding(new Insets(20));
        card.setMaxWidth(500);

        // Score principal
        HBox scoreRow = new HBox(20);
        scoreRow.setAlignment(Pos.CENTER_LEFT);

        VBox scoreBox = new VBox(4);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.setStyle("-fx-background-color: " + getStatusColor(status) + "; " +
                          "-fx-background-radius: 12; -fx-padding: 16 20;");
        Label hoursLbl = new Label(String.valueOf(hours));
        hoursLbl.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label hoursUnit = new Label("heures");
        hoursUnit.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");
        scoreBox.getChildren().addAll(hoursLbl, hoursUnit);

        VBox infoBox = new VBox(8);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        Label statusLbl = new Label(getStatusEmoji(status) + "  " + getStatusText(status));
        statusLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + getStatusColor(status) + ";");
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0c0c0; -fx-wrap-text: true;");
        msgLbl.setMaxWidth(280);

        if (missing > 0) {
            Label missingLbl = new Label("⏰ Il vous manque " + missing + "h de sommeil");
            missingLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff9800; -fx-font-weight: bold;");
            infoBox.getChildren().addAll(statusLbl, msgLbl, missingLbl);
        } else {
            infoBox.getChildren().addAll(statusLbl, msgLbl);
        }

        scoreRow.getChildren().addAll(scoreBox, infoBox);

        // Barre de progression sommeil
        double pct = Math.min(hours / 9.0, 1.0);
        VBox progressSection = new VBox(6);
        Label progressTitle = new Label("Niveau de sommeil (" + hours + "h / 9h recommandées)");
        progressTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b949e;");
        ProgressBar sleepBar = new ProgressBar(pct);
        sleepBar.setMaxWidth(Double.MAX_VALUE);
        sleepBar.setPrefHeight(10);
        sleepBar.setStyle("-fx-accent: " + getStatusColor(status) + "; -fx-background-color: #1e2d3d; -fx-background-radius: 5;");
        progressSection.getChildren().addAll(progressTitle, sleepBar);

        card.getChildren().addAll(scoreRow, progressSection);
        return card;
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "OK"          -> "#4caf50";
            case "insuffisant" -> "#ff9800";
            default            -> "#f44336";
        };
    }

    private String getStatusBg(String status) {
        return switch (status) {
            case "OK"          -> "#0d2818";
            case "insuffisant" -> "#1a1200";
            default            -> "#1a0000";
        };
    }

    private String getStatusEmoji(String status) {
        return switch (status) {
            case "OK"          -> "✅";
            case "insuffisant" -> "⚠️";
            default            -> "🚨";
        };
    }

    private String getStatusText(String status) {
        return switch (status) {
            case "OK"          -> "Sommeil optimal";
            case "insuffisant" -> "Sommeil insuffisant";
            default            -> "Manque critique de sommeil";
        };
    }

    // ── Helpers UI ────────────────────────────────────────────────────
    private void addBotMessage(String text, boolean isHighlight) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox avatar = new VBox();
        avatar.setAlignment(Pos.TOP_CENTER);
        avatar.setMinWidth(36);
        Label avatarLbl = new Label("🤖");
        avatarLbl.setStyle("-fx-font-size: 18px;");
        avatar.getChildren().add(avatarLbl);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        if (isHighlight) {
            bubble.setStyle("-fx-background-color: #7c4dff; -fx-text-fill: white; " +
                           "-fx-background-radius: 0 16 16 16; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            bubble.setStyle("-fx-background-color: #1e2d3d; -fx-text-fill: #c9d1d9; " +
                           "-fx-background-radius: 0 16 16 16; -fx-font-size: 13px;");
        }

        row.getChildren().addAll(avatar, bubble);

        // Animation d'apparition
        row.setOpacity(0);
        chatBox.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        scrollToBottom();
    }

    private void addUserMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(320);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle("-fx-background-color: #651fff; -fx-text-fill: white; " +
                       "-fx-background-radius: 16 0 16 16; -fx-font-size: 13px;");

        Label avatarLbl = new Label("👤");
        avatarLbl.setStyle("-fx-font-size: 18px;");

        row.getChildren().addAll(bubble, avatarLbl);
        chatBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addCustomNode(VBox node) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 0, 46));
        row.getChildren().add(node);
        chatBox.getChildren().add(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    // ── JSON helpers (kept for compatibility) ────────────────────────
    // Note: displayResult now uses Jackson ObjectMapper for robust parsing.
    // These helpers are no longer used but kept to avoid compilation errors
    // if referenced elsewhere.
    private double extractDouble(String json, String key) {
        try {
            ObjectMapper m = new ObjectMapper();
            return m.readTree(json).path(key).asDouble(0.0);
        } catch (Exception e) { return 0.0; }
    }

    private String extractJsonString(String json, String key) {
        try {
            ObjectMapper m = new ObjectMapper();
            return m.readTree(json).path(key).asText("");
        } catch (Exception e) { return ""; }
    }
}
