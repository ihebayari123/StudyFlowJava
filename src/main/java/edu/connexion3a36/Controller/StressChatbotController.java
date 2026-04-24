package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.services.WellBeingScoreService;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

/**
 * StressChatbotController — Flux complet :
 *
 *  Étape 1 — Demande le StressSurvey_id
 *             → Cherche dans well_being_score WHERE survey_id = ?
 *             → Récupère le score automatiquement
 *  Étape 2 — Demande le nom
 *  Étape 3 — Capture webcam + landmarks (OpenCV)
 *  Étape 4 — Prédiction stress IA (stress_model.pkl)
 *  Étape 5 — Génération PDF + téléchargement
 */
public class StressChatbotController {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private VBox       chatMessages;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField  inputField;
    @FXML private Button     btnSend;
    @FXML private HBox       typingIndicator;
    @FXML private Label      predictionBadge;

    // Sidebar étapes
    @FXML private HBox  step1Box, step2Box, step3Box, step4Box, step5Box;
    @FXML private Label step1Icon, step2Icon, step3Icon, step4Icon, step5Icon;
    @FXML private Circle statusDot;
    @FXML private Label  statusLabel;

    // Photo + PDF
    @FXML private VBox      photoPreviewBox;
    @FXML private ImageView photoPreview;
    @FXML private Label     capturedNameLabel;
    @FXML private VBox      downloadBox;
    @FXML private Button    btnDownloadPdf;

    // ── État de la conversation ───────────────────────────────────────────────
    private enum Step { SURVEY_ID, NAME, CAPTURE, RESULT, DONE }
    private Step currentStep = Step.SURVEY_ID;

    // Données transmises depuis le formulaire StressSurvey
    private int lastSurveyId = 0;   // ID du survey qui vient d'être créé
    private int sleepHours   = 7;
    private int studyHours   = 5;

    // Données récupérées / saisies pendant le chat
    private int    wellBeingScore = 0;
    private String userName       = "";
    private String photoPath      = "";
    private String pdfPath        = "";
    private String stressLevel    = "";

    private FitnessDashboardController dashboardController;
    private final WellBeingScoreService wbService = new WellBeingScoreService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chatbot-worker");
        t.setDaemon(true);
        return t;
    });

    // ── Python ────────────────────────────────────────────────────────────────
    private static final String PYTHON_CMD  = "python";
    private static final String SCRIPT_PATH;
    static {
        SCRIPT_PATH = System.getProperty("user.dir") + File.separator
                + "stress_model_complet" + File.separator + "stress_chatbot.py";
    }

    // ─────────────────────────────────────────────────────────────────────────
    public void setDashboardController(FitnessDashboardController dc) {
        this.dashboardController = dc;
    }

    /**
     * Appelé par FitnessDashboardController avec les données du formulaire.
     * @param surveyId  ID du StressSurvey qui vient d'être inséré en BDD
     * @param sleep     heures de sommeil saisies
     * @param study     heures d'étude saisies
     */
    public void initWithData(int surveyId, int sleep, int study) {
        this.lastSurveyId = surveyId;
        this.sleepHours   = sleep;
        this.studyHours   = study;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        inputField.setOnAction(e -> sendMessage());
        Timeline delay = new Timeline(new KeyFrame(Duration.millis(400), e -> startConversation()));
        delay.play();
    }

    // ── Démarrage ─────────────────────────────────────────────────────────────
    private void startConversation() {
        addBotMessage("👋 Bonjour ! Je suis votre assistant d'analyse de stress.");
        addBotMessage("✅ Votre StressSurvey a été enregistré avec succès en base de données.");

        Timeline t = new Timeline(new KeyFrame(Duration.millis(1200), e -> askSurveyId()));
        t.play();
    }

    private void askSurveyId() {
        addBotMessage("🔍 Veuillez entrer votre StressSurvey_id pour récupérer votre score de bien-être :");
        currentStep = Step.SURVEY_ID;
        activateInput();
    }

    // ── Étape 1 : Survey ID → lookup WellBeingScore ───────────────────────────
    private void handleSurveyIdInput(String text) {
        int surveyId;
        try {
            surveyId = Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            addBotMessage("⚠️ L'ID doit être un nombre entier. Veuillez réessayer.");
            activateInput();
            return;
        }

        showTyping();
        final int sid = surveyId;

        executor.submit(() -> {
            try {
                WellBeingScore wbs = wbService.getBySurveyId(sid);
                Platform.runLater(() -> {
                    hideTyping();
                    if (wbs != null) {
                        wellBeingScore = wbs.getScore();
                        activateStep(2);

                        addBotMessage("✅ WellBeingScore trouvé pour le Survey ID " + sid + " !");
                        addBotMessage("📊 Score de bien-être récupéré automatiquement : "
                                + wellBeingScore + " / 100");
                        addBotMessage("📋 Recommandation : " + wbs.getRecommendation());
                        addBotMessage("📋 Plan d'action  : " + wbs.getAction_plan());

                        Timeline t = new Timeline(new KeyFrame(Duration.millis(900), e -> askName()));
                        t.play();
                        currentStep = Step.NAME;
                    } else {
                        addBotMessage("❌ Aucun WellBeingScore trouvé pour le Survey ID " + sid + ".");
                        addBotMessage("💡 Vérifiez votre ID et réessayez.");
                        activateInput();
                    }
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    hideTyping();
                    addBotMessage("❌ Erreur base de données : " + ex.getMessage());
                    activateInput();
                });
            }
        });
    }

    // ── Étape 2 : Nom ─────────────────────────────────────────────────────────
    private void askName() {
        addBotMessage("❓ Quelle est votre nom ?");
        activateInput();
    }

    private void handleNameInput(String text) {
        userName = text.trim();
        activateStep(3);
        showTyping();
        Timeline t = new Timeline(new KeyFrame(Duration.millis(800), e -> {
            hideTyping();
            addBotMessage("👤 Bonjour " + userName + " !");
            addBotMessage("📸 Je vais maintenant accéder à votre caméra.");
            addBotMessage("🟢 Centrez votre visage dans le cadre, puis appuyez sur ESPACE pour capturer.");
            Timeline t2 = new Timeline(new KeyFrame(Duration.millis(1200), ev -> {
                addCaptureButton();
                currentStep = Step.CAPTURE;
            }));
            t2.play();
        }));
        t.play();
    }

    // ── Étape 3 : Capture caméra ──────────────────────────────────────────────
    private void addCaptureButton() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        Button btn = new Button("📸  Ouvrir la caméra");
        btn.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 20; "
                + "-fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13px; "
                + "-fx-effect: dropshadow(gaussian, rgba(106,27,154,0.4), 8, 0, 0, 2);");
        btn.setOnAction(e -> {
            btn.setDisable(true);
            btn.setText("⏳  Ouverture...");
            launchCapture();
        });

        row.getChildren().add(btn);
        chatMessages.getChildren().add(row);
        scrollToBottom();
    }

    private void launchCapture() {
        addBotMessage("🎥 Ouverture de la caméra...");
        String tmpPhoto = System.getProperty("user.home") + File.separator
                + "studyflow_face_" + System.currentTimeMillis() + ".jpg";

        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(PYTHON_CMD, SCRIPT_PATH, "capture", tmpPhoto);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String out = new String(proc.getInputStream().readAllBytes()).trim();
                proc.waitFor();
                Platform.runLater(() -> handleCaptureResult(out, tmpPhoto));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    addBotMessage("❌ Erreur caméra : " + ex.getMessage());
                    addCaptureButton();
                });
            }
        });
    }

    private void handleCaptureResult(String output, String expectedPath) {
        try {
            JSONObject json = new JSONObject(output);
            if (json.optBoolean("success", false)) {
                photoPath = json.optString("path", expectedPath);
                activateStep(4);
                addBotMessage("✅ Visage capturé avec succès !");
                addBotMessage("🔴 Nom enregistré : " + userName);
                showPhotoPreview(photoPath);
                Timeline t = new Timeline(new KeyFrame(Duration.millis(900), e -> runPrediction()));
                t.play();
            } else {
                addBotMessage("⚠️ " + json.optString("error", "Capture échouée"));
                addCaptureButton();
            }
        } catch (Exception ex) {
            addBotMessage("⚠️ Capture annulée.");
            addCaptureButton();
        }
    }

    // ── Étape 4 : Prédiction IA ───────────────────────────────────────────────
    private void runPrediction() {
        addBotMessage("🧠 Analyse de votre stress en temps réel avec le modèle IA...");
        showTyping();

        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        PYTHON_CMD, SCRIPT_PATH, "predict",
                        String.valueOf(wellBeingScore),
                        String.valueOf(sleepHours),
                        String.valueOf(studyHours)
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String out = new String(proc.getInputStream().readAllBytes()).trim();
                proc.waitFor();
                Platform.runLater(() -> handlePredictionResult(out));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    hideTyping();
                    addBotMessage("❌ Erreur prédiction : " + ex.getMessage());
                });
            }
        });
    }

    private void handlePredictionResult(String output) {
        hideTyping();
        try {
            JSONObject json = new JSONObject(output);
            if (json.optBoolean("success", false)) {
                stressLevel = json.optString("level", "Medium");
                double confidence = json.optDouble("confidence", 0.0);

                updatePredictionBadge(stressLevel);

                String emoji = switch (stressLevel) {
                    case "Low"  -> "🟢";
                    case "High" -> "🔴";
                    default     -> "🟡";
                };
                String levelFr = switch (stressLevel) {
                    case "Low"  -> "Stress Faible";
                    case "High" -> "Stress Élevé";
                    default     -> "Stress Modéré";
                };

                addBotMessage(emoji + " Résultat : " + levelFr);
                addBotMessage("📈 Confiance du modèle : " + confidence + "%");

                JSONObject proba = json.optJSONObject("probabilities");
                if (proba != null) {
                    addBotMessage("📊 Probabilités :\n"
                            + "  • Faible : " + proba.optDouble("Low", 0) + "%\n"
                            + "  • Modéré : " + proba.optDouble("Medium", 0) + "%\n"
                            + "  • Élevé  : " + proba.optDouble("High", 0) + "%");
                }

                activateStep(5);
                Timeline t = new Timeline(new KeyFrame(Duration.millis(1000), e -> generatePdf()));
                t.play();
            } else {
                addBotMessage("❌ Erreur modèle : " + json.optString("error", "inconnue"));
            }
        } catch (Exception ex) {
            addBotMessage("❌ Erreur parsing : " + output);
        }
    }

    // ── Étape 5 : PDF ─────────────────────────────────────────────────────────
    private void generatePdf() {
        addBotMessage("📄 Génération de votre emploi du temps personnalisé...");
        showTyping();

        pdfPath = System.getProperty("user.home") + File.separator
                + "StudyFlow_" + userName.replaceAll("\\s+", "_")
                + "_" + System.currentTimeMillis() + ".pdf";

        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        PYTHON_CMD, SCRIPT_PATH, "pdf",
                        userName,
                        String.valueOf(wellBeingScore),
                        stressLevel,
                        photoPath,
                        pdfPath
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String out = new String(proc.getInputStream().readAllBytes()).trim();
                proc.waitFor();
                Platform.runLater(() -> handlePdfResult(out));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    hideTyping();
                    addBotMessage("❌ Erreur PDF : " + ex.getMessage());
                    addBotMessage("💡 Installez reportlab : pip install reportlab");
                });
            }
        });
    }

    private void handlePdfResult(String output) {
        hideTyping();
        try {
            JSONObject json = new JSONObject(output);
            if (json.optBoolean("success", false)) {
                pdfPath = json.optString("path", pdfPath);
                addBotMessage("✅ PDF généré avec succès !");
                addBotMessage("📋 Votre emploi du temps personnalisé est prêt.");
                addBotMessage("⬇️ Cliquez sur Télécharger le PDF dans la barre latérale.");
                showDownloadButton();
                currentStep = Step.DONE;
                setStatus("Analyse terminée", "#4caf50");
            } else {
                String err = json.optString("error", "inconnue");
                addBotMessage("❌ Erreur PDF : " + err);
                if (err.contains("reportlab")) {
                    addBotMessage("💡 pip install reportlab");
                }
            }
        } catch (Exception ex) {
            addBotMessage("❌ Erreur : " + output);
        }
    }

    // ── Téléchargement ────────────────────────────────────────────────────────
    @FXML
    public void downloadPdf() {
        if (pdfPath == null || pdfPath.isEmpty()) { addBotMessage("⚠️ Aucun PDF disponible."); return; }
        File src = new File(pdfPath);
        if (!src.exists()) { addBotMessage("⚠️ Fichier introuvable : " + pdfPath); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.setInitialFileName("StudyFlow_" + userName + "_Stress.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File dest = fc.showSaveDialog(btnDownloadPdf.getScene().getWindow());

        if (dest != null) {
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                addBotMessage("✅ PDF enregistré : " + dest.getAbsolutePath());
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dest);
            } catch (IOException ex) {
                addBotMessage("❌ Erreur : " + ex.getMessage());
            }
        }
    }

    // ── Saisie utilisateur ────────────────────────────────────────────────────
    @FXML
    public void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        addUserMessage(text);
        inputField.clear();
        inputField.setDisable(true);
        btnSend.setDisable(true);

        switch (currentStep) {
            case SURVEY_ID -> handleSurveyIdInput(text);
            case NAME      -> handleNameInput(text);
            default        -> {}
        }
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private void addBotMessage(String text) {
        Platform.runLater(() -> {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));

            Label avatar = new Label("🤖");
            avatar.setStyle("-fx-font-size: 18px;");
            avatar.setMinWidth(28);

            Label bubble = new Label(text);
            bubble.setWrapText(true);
            bubble.setMaxWidth(520);
            bubble.setStyle("-fx-background-color: #16213e; -fx-text-fill: #e0e0e0; "
                    + "-fx-background-radius: 0 14 14 14; -fx-padding: 10 14; "
                    + "-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; "
                    + "-fx-border-color: rgba(106,27,154,0.3); -fx-border-radius: 0 14 14 14;");

            row.getChildren().addAll(avatar, bubble);
            FadeTransition ft = new FadeTransition(Duration.millis(250), row);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            chatMessages.getChildren().add(row);
            scrollToBottom();
        });
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(2, 0, 2, 0));

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; "
                + "-fx-background-radius: 14 14 0 14; -fx-padding: 10 14; "
                + "-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");

        row.getChildren().add(bubble);
        FadeTransition ft = new FadeTransition(Duration.millis(200), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        chatMessages.getChildren().add(row);
        scrollToBottom();
    }

    private void activateInput() {
        Platform.runLater(() -> {
            inputField.setDisable(false);
            btnSend.setDisable(false);
            inputField.requestFocus();
        });
    }

    private void showTyping() {
        Platform.runLater(() -> { typingIndicator.setVisible(true); typingIndicator.setManaged(true); });
    }

    private void hideTyping() {
        Platform.runLater(() -> { typingIndicator.setVisible(false); typingIndicator.setManaged(false); });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void showPhotoPreview(String path) {
        Platform.runLater(() -> {
            try {
                File f = new File(path);
                if (f.exists()) {
                    photoPreview.setImage(new Image(f.toURI().toString(), 120, 120, true, true));
                    capturedNameLabel.setText(userName);
                    photoPreviewBox.setVisible(true);
                    photoPreviewBox.setManaged(true);
                }
            } catch (Exception ignored) {}
        });
    }

    private void showDownloadButton() {
        Platform.runLater(() -> { downloadBox.setVisible(true); downloadBox.setManaged(true); });
    }

    private void updatePredictionBadge(String level) {
        Platform.runLater(() -> {
            String style = switch (level) {
                case "Low"  -> "-fx-background-color:rgba(46,125,50,0.3);-fx-text-fill:#81c784;";
                case "High" -> "-fx-background-color:rgba(198,40,40,0.3);-fx-text-fill:#ef9a9a;";
                default     -> "-fx-background-color:rgba(245,124,0,0.3);-fx-text-fill:#ffcc80;";
            };
            String text = switch (level) {
                case "Low"  -> "🟢 Stress Faible";
                case "High" -> "🔴 Stress Élevé";
                default     -> "🟡 Stress Modéré";
            };
            predictionBadge.setText(text);
            predictionBadge.setStyle(style
                    + "-fx-background-radius:20;-fx-padding:5 14;"
                    + "-fx-font-size:12px;-fx-font-weight:bold;-fx-font-family:'Segoe UI';");
        });
    }

    private void activateStep(int step) {
        Platform.runLater(() -> {
            String done   = "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:white;"
                    + "-fx-background-color:#4caf50;-fx-background-radius:50%;"
                    + "-fx-min-width:22;-fx-min-height:22;-fx-alignment:CENTER;";
            String active = "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:white;"
                    + "-fx-background-color:#6a1b9a;-fx-background-radius:50%;"
                    + "-fx-min-width:22;-fx-min-height:22;-fx-alignment:CENTER;";
            String bg     = "-fx-background-color:rgba(106,27,154,0.3);";

            if (step > 1) { step1Box.setStyle(bg); step1Icon.setText("✓"); step1Icon.setStyle(done); }
            if (step > 2) { step2Box.setStyle(bg); step2Icon.setText("✓"); step2Icon.setStyle(done); }
            if (step > 3) { step3Box.setStyle(bg); step3Icon.setText("✓"); step3Icon.setStyle(done); }
            if (step > 4) { step4Box.setStyle(bg); step4Icon.setText("✓"); step4Icon.setStyle(done); }

            switch (step) {
                case 1 -> { step1Box.setStyle(bg); step1Icon.setStyle(active); }
                case 2 -> { step2Box.setStyle(bg); step2Icon.setStyle(active); }
                case 3 -> { step3Box.setStyle(bg); step3Icon.setStyle(active); }
                case 4 -> { step4Box.setStyle(bg); step4Icon.setStyle(active); }
                case 5 -> { step5Box.setStyle(bg); step5Icon.setStyle(active); }
            }
        });
    }

    private void setStatus(String text, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setStyle("-fx-font-size:11px;-fx-text-fill:" + color + ";-fx-font-family:'Segoe UI';");
            statusDot.setStyle("-fx-fill:" + color + ";");
        });
    }
}
