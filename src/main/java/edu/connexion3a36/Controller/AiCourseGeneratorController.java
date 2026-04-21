package edu.connexion3a36.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.ChapitreService;
import edu.connexion3a36.services.CoursService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for AI-powered Course & Chapter generation using local Ollama.
 *
 * Wire this up to ai_course_generator.fxml and add a nav item in
 * DashboardController / studyflow.fxml.
 */
public class AiCourseGeneratorController {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private ComboBox<String> modelComboBox;
    @FXML private TextField subjectField;
    @FXML private TextField levelField;
    @FXML private Spinner<Integer> chaptersSpinner;
    @FXML private CheckBox includeVideosCheck;
    @FXML private CheckBox includeResourcesCheck;
    @FXML private CheckBox saveToDbCheck;

    @FXML private Button generateButton;
    @FXML private Button saveButton;
    @FXML private Button clearButton;

    @FXML private TextArea rawOutputArea;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private Label ollamaStatusLabel;

    // Preview table
    @FXML private TableView<ChapterPreview> previewTable;
    @FXML private TableColumn<ChapterPreview, Integer> orderCol;
    @FXML private TableColumn<ChapterPreview, String>  titleCol;
    @FXML private TableColumn<ChapterPreview, String>  typeCol;
    @FXML private TableColumn<ChapterPreview, Integer> durationCol;

    // Cours preview
    @FXML private Label generatedTitleLabel;
    @FXML private Label generatedDescLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String[] AVAILABLE_MODELS = {
        "phi3:latest",
        "gemma:2b",
        "hf.co/aliekuhgd/chat_quiz:latest"
    };

    private final CoursService     coursService     = new CoursService();
    private final ChapitreService  chapitreService  = new ChapitreService();
    private final ObjectMapper     mapper           = new ObjectMapper();
    private final ExecutorService  executor         = Executors.newSingleThreadExecutor();

    private GeneratedCourse generatedCourse = null;

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        modelComboBox.getItems().addAll(AVAILABLE_MODELS);
        modelComboBox.setValue("phi3:latest");

        chaptersSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 5));

        setupPreviewTable();
        checkOllamaStatus();

        saveButton.setDisable(true);
        progressIndicator.setVisible(false);
    }

    private void setupPreviewTable() {
        orderCol.setCellValueFactory(d -> d.getValue().orderProperty().asObject());
        titleCol.setCellValueFactory(d -> d.getValue().titleProperty());
        typeCol.setCellValueFactory(d -> d.getValue().typeProperty());
        durationCol.setCellValueFactory(d -> d.getValue().durationProperty().asObject());
    }

    // ── Ollama health check ───────────────────────────────────────────────────
    private void checkOllamaStatus() {
        executor.submit(() -> {
            try {
                URL url = new URL("http://localhost:11434/api/tags");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                Platform.runLater(() -> {
                    if (code == 200) {
                        ollamaStatusLabel.setText("🟢 Ollama actif");
                        ollamaStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        ollamaStatusLabel.setText("🔴 Ollama hors ligne");
                        ollamaStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ollamaStatusLabel.setText("🔴 Ollama non détecté (port 11434)");
                    ollamaStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                });
            }
        });
    }

    // ── Generate ──────────────────────────────────────────────────────────────
    @FXML
    private void handleGenerate() {
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            showAlert("Champ requis", "Veuillez entrer un sujet de cours.", Alert.AlertType.WARNING);
            return;
        }

        String model    = modelComboBox.getValue();
        String level    = levelField.getText().trim().isEmpty() ? "débutant" : levelField.getText().trim();
        int    chapters = chaptersSpinner.getValue();
        boolean videos  = includeVideosCheck.isSelected();
        boolean res     = includeResourcesCheck.isSelected();

        setLoading(true);
        rawOutputArea.clear();
        previewTable.getItems().clear();
        saveButton.setDisable(true);
        generatedCourse = null;

        String prompt = buildPrompt(subject, level, chapters, videos, res);

        executor.submit(() -> {
            try {
                String result = callOllama(model, prompt);
                Platform.runLater(() -> {
                    rawOutputArea.setText(result);
                    parseAndPreview(result, subject, level);
                    setLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    statusLabel.setText("Erreur: " + e.getMessage());
                    showAlert("Erreur Ollama",
                        "Impossible de contacter Ollama.\n" +
                        "Vérifiez qu'Ollama est démarré: ollama serve\n\n" + e.getMessage(),
                        Alert.AlertType.ERROR);
                });
            }
        });
    }

    // ── Prompt builder ────────────────────────────────────────────────────────
    private String buildPrompt(String subject, String level, int chapters,
                               boolean videos, boolean resources) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert pédagogique. Génère un cours complet en JSON UNIQUEMENT.\n");
        sb.append("Réponds UNIQUEMENT avec du JSON valide, sans texte avant ou après.\n\n");
        sb.append("Sujet: ").append(subject).append("\n");
        sb.append("Niveau: ").append(level).append("\n");
        sb.append("Nombre de chapitres: ").append(chapters).append("\n\n");
        sb.append("Format JSON requis:\n");
        sb.append("{\n");
        sb.append("  \"titre\": \"Titre du cours\",\n");
        sb.append("  \"description\": \"Description du cours (50-200 mots)\",\n");
        sb.append("  \"chapitres\": [\n");
        sb.append("    {\n");
        sb.append("      \"ordre\": 1,\n");
        sb.append("      \"titre\": \"Titre du chapitre\",\n");
        sb.append("      \"contenu\": \"Contenu détaillé du chapitre (100-300 mots)\",\n");
        sb.append("      \"contentType\": \"text\",\n");
        sb.append("      \"durationMinutes\": 30");
        if (videos) {
            sb.append(",\n      \"videoUrl\": \"https://www.youtube.com/results?search_query=");
            sb.append(subject.replace(" ", "+")).append("+chapitre+1\"");
        }
        if (resources) {
            sb.append(",\n      \"resources\": [\"https://developer.mozilla.org\", \"https://www.w3schools.com\"]");
        }
        sb.append("\n    }\n  ]\n}\n\n");
        sb.append("Génère exactement ").append(chapters).append(" chapitres pour le sujet: ").append(subject);
        return sb.toString();
    }

    // ── Ollama API call ───────────────────────────────────────────────────────
    private String callOllama(String model, String prompt) throws Exception {
        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);

        // Build request body
        String body = mapper.writeValueAsString(
            mapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", true)
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        // Stream the response
        StringBuilder full = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    JsonNode node = mapper.readTree(line);
                    String token = node.path("response").asText("");
                    full.append(token);
                    // Stream to UI
                    final String chunk = token;
                    Platform.runLater(() -> rawOutputArea.appendText(chunk));
                } catch (Exception ignored) {}
            }
        }
        return full.toString();
    }

    // ── Parse & preview ───────────────────────────────────────────────────────
    private void parseAndPreview(String raw, String subject, String level) {
        try {
            // Extract JSON from the raw output (model may wrap it)
            String json = extractJson(raw);
            if (json == null) {
                statusLabel.setText("⚠️ JSON non trouvé dans la réponse. Voir le texte brut.");
                return;
            }

            JsonNode root = mapper.readTree(json);
            String titre       = root.path("titre").asText(subject + " - Cours " + level);
            String description = root.path("description").asText("Cours généré par IA sur " + subject);

            generatedTitleLabel.setText(titre);
            generatedDescLabel.setText(description);

            ObservableList<ChapterPreview> previews = FXCollections.observableArrayList();
            List<JsonNode> chapNodes = new ArrayList<>();
            root.path("chapitres").forEach(chapNodes::add);

            for (JsonNode ch : chapNodes) {
                ChapterPreview cp = new ChapterPreview(
                    ch.path("ordre").asInt(previews.size() + 1),
                    ch.path("titre").asText("Chapitre " + (previews.size() + 1)),
                    ch.path("contentType").asText("text"),
                    ch.path("durationMinutes").asInt(30)
                );
                previews.add(cp);
            }

            previewTable.setItems(previews);

            // Store for save
            generatedCourse = new GeneratedCourse(titre, description, chapNodes);
            saveButton.setDisable(false);
            statusLabel.setText("✅ " + chapNodes.size() + " chapitre(s) générés pour « " + titre + " »");

        } catch (Exception e) {
            statusLabel.setText("⚠️ Erreur parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractJson(String text) {
        // Try to find first { ... } block
        int start = text.indexOf('{');
        if (start == -1) return null;
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        return null;
    }

    // ── Save to DB ────────────────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        if (generatedCourse == null) return;

        try {
            // 1. Save cours
            Cours cours = new Cours();
            cours.setTitre(generatedCourse.titre);
            cours.setDescription(generatedCourse.description);
            cours.setUserId(1L); // default user
            coursService.save(cours);

            // 2. Save chapitres
            int saved = 0;
            for (JsonNode ch : generatedCourse.chapitres) {
                Chapitre chapitre = new Chapitre();
                chapitre.setCourse(cours);
                chapitre.setOrdre(ch.path("ordre").asInt(saved + 1));
                chapitre.setTitre(ch.path("titre").asText("Chapitre " + (saved + 1)));
                chapitre.setContenu(ch.path("contenu").asText("Contenu généré par IA."));
                chapitre.setContentType(ch.path("contentType").asText("text"));
                chapitre.setDurationMinutes(ch.path("durationMinutes").asInt(30));

                String videoUrl = ch.path("videoUrl").asText(null);
                if (videoUrl != null && !videoUrl.isBlank()) chapitre.setVideoUrl(videoUrl);

                String imageUrl = ch.path("imageUrl").asText(null);
                if (imageUrl != null && !imageUrl.isBlank()) chapitre.setImageUrl(imageUrl);

                chapitreService.save(chapitre);
                saved++;
            }

            showAlert("Succès",
                "✅ Cours « " + generatedCourse.titre + " » sauvegardé avec " + saved + " chapitre(s)!",
                Alert.AlertType.INFORMATION);
            statusLabel.setText("✅ Sauvegardé en base de données.");
            saveButton.setDisable(true);

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClear() {
        subjectField.clear();
        levelField.clear();
        rawOutputArea.clear();
        previewTable.getItems().clear();
        generatedTitleLabel.setText("—");
        generatedDescLabel.setText("—");
        statusLabel.setText("Prêt");
        saveButton.setDisable(true);
        generatedCourse = null;
    }

    @FXML
    private void handleRefreshOllama() {
        checkOllamaStatus();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        generateButton.setDisable(loading);
        statusLabel.setText(loading ? "⏳ Génération en cours avec Ollama..." : "Terminé");
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /** Lightweight DTO for the preview table. */
    public static class ChapterPreview {
        private final javafx.beans.property.SimpleIntegerProperty order;
        private final javafx.beans.property.SimpleStringProperty  title;
        private final javafx.beans.property.SimpleStringProperty  type;
        private final javafx.beans.property.SimpleIntegerProperty duration;

        public ChapterPreview(int order, String title, String type, int duration) {
            this.order    = new javafx.beans.property.SimpleIntegerProperty(order);
            this.title    = new javafx.beans.property.SimpleStringProperty(title);
            this.type     = new javafx.beans.property.SimpleStringProperty(type);
            this.duration = new javafx.beans.property.SimpleIntegerProperty(duration);
        }

        public javafx.beans.property.SimpleIntegerProperty orderProperty()    { return order; }
        public javafx.beans.property.SimpleStringProperty  titleProperty()    { return title; }
        public javafx.beans.property.SimpleStringProperty  typeProperty()     { return type; }
        public javafx.beans.property.SimpleIntegerProperty durationProperty() { return duration; }
    }

    /** Holds the parsed AI output before DB save. */
    private static class GeneratedCourse {
        final String         titre;
        final String         description;
        final List<JsonNode> chapitres;

        GeneratedCourse(String titre, String description, List<JsonNode> chapitres) {
            this.titre       = titre;
            this.description = description;
            this.chapitres   = chapitres;
        }
    }
}
