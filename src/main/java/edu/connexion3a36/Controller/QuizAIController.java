package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Question;
import edu.connexion3a36.entities.Quiz;
import edu.connexion3a36.services.QuestionService;
import edu.connexion3a36.services.QuizService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizAIController
 * ─────────────────
 * Fonctionnalité avancée US04 :
 *   1. L'enseignant sélectionne un PDF + un Quiz cible
 *   2. Java appelle generate_java.py (PyMuPDF + Ollama/Mistral)
 *   3. Le script retourne un JSON de questions
 *   4. L'enseignant approuve / rejette chaque question (checkbox)
 *   5. Les questions approuvées sont insérées en BDD
 *
 * Dépendances Maven à ajouter dans pom.xml :
 *   <dependency>
 *       <groupId>org.json</groupId>
 *       <artifactId>json</artifactId>
 *       <version>20231013</version>
 *   </dependency>
 */
public class QuizAIController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ComboBox<Quiz>  cbQuiz;
    @FXML private Label           lblPdfPath;
    @FXML private Button          btnChoosePdf;
    @FXML private Button          btnGenerate;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label           lblStatus;
    @FXML private ScrollPane      scrollApproval;
    @FXML private VBox            approvalList;
    @FXML private Button          btnConfirm;
    @FXML private Label           lblCount;
    @FXML private HBox            hboxConfirm;

    // ── État ──────────────────────────────────────────────────────────────────
    private File             selectedPdf     = null;
    private List<JSONObject> pendingQuestions = new ArrayList<>();
    private List<CheckBox>   checkBoxes       = new ArrayList<>();
    private StackPane        contentArea;

    // ── Services ──────────────────────────────────────────────────────────────
    private final QuizService     quizService     = new QuizService();
    private final QuestionService questionService = new QuestionService();

    public void setContentArea(StackPane sp) {
        this.contentArea = sp;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        try {
            cbQuiz.getItems().setAll(quizService.getData());
            cbQuiz.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Quiz q)   { return q == null ? "" : q.getTitre(); }
                @Override public Quiz fromString(String s) { return null; }
            });
        } catch (Exception e) {
            lblStatus.setText("Erreur chargement quiz : " + e.getMessage());
        }

        progressIndicator.setVisible(false);
        scrollApproval.setVisible(false);
        btnConfirm.setVisible(false);
        if (hboxConfirm != null) hboxConfirm.setVisible(false);
        btnConfirm.setDisable(true);
        lblCount.setText("");
    }

    // ── Choisir PDF ───────────────────────────────────────────────────────────

    @FXML
    public void choisirPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un fichier PDF");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        File f = fc.showOpenDialog(btnChoosePdf.getScene().getWindow());
        if (f != null) {
            selectedPdf = f;
            lblPdfPath.setText(f.getName());
            lblStatus.setText("PDF prêt : " + f.getName());
        }
    }

    // ── Générer les questions ─────────────────────────────────────────────────

    @FXML
    public void genererQuestions() {
        // Validation
        if (selectedPdf == null) {
            afficherAlerte("Veuillez choisir un fichier PDF.");
            return;
        }
        Quiz quiz = cbQuiz.getValue();
        if (quiz == null) {
            afficherAlerte("Veuillez sélectionner un quiz cible.");
            return;
        }

        // Trouver le script Python
        String scriptPath = trouverScript();
        if (scriptPath == null) {
            afficherAlerte("Script generate_java.py introuvable.\n" +
                "Placez-le dans : src/main/resources/ai/generate_java.py\n" +
                "ou dans : ai/generate_java.py (racine du projet)");
            return;
        }

        // Lancement en arrière-plan
        btnGenerate.setDisable(true);
        progressIndicator.setVisible(true);
        lblStatus.setText("Génération en cours avec Ollama/Mistral...");
        scrollApproval.setVisible(false);
        btnConfirm.setVisible(false);
        approvalList.getChildren().clear();
        pendingQuestions.clear();
        checkBoxes.clear();

        final String finalScript = scriptPath;
        final int    quizId      = quiz.getId();

        new Thread(() -> {
            try {
                // Commande : py generate_java.py <pdf> <quiz_id>
                ProcessBuilder pb = new ProcessBuilder(
                    getPythonExecutable(), finalScript,
                    selectedPdf.getAbsolutePath(),
                    String.valueOf(quizId)
                );
                pb.redirectErrorStream(false);
                Process process = pb.start();

                // Lire stdout (JSON)
                StringBuilder stdout = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) stdout.append(line);
                }

                // Lire stderr (logs)
                StringBuilder stderr = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        stderr.append(line).append("\n");
                }

                process.waitFor();
                String jsonOutput = stdout.toString().trim();
                System.out.println("[generate_java.py stderr]\n" + stderr);

                Platform.runLater(() -> {
                    try {
                        if (jsonOutput.isEmpty() || !jsonOutput.startsWith("{")) {
                            lblStatus.setText("Erreur : réponse inattendue du script.\n" + stderr);
                            return;
                        }

                        JSONObject root = new JSONObject(jsonOutput);

                        // Vérifier erreur retournée par le script
                        if (root.has("error")) {
                            lblStatus.setText("Erreur Python : " + root.getString("error"));
                            return;
                        }

                        JSONArray questions = root.getJSONArray("questions");
                        if (questions.length() == 0) {
                            lblStatus.setText("Aucune question générée. Vérifiez Ollama / le PDF.");
                            return;
                        }

                        // Construire l'interface d'approbation
                        for (int i = 0; i < questions.length(); i++) {
                            pendingQuestions.add(questions.getJSONObject(i));
                        }
                        construireApprobation();
                        lblStatus.setText(questions.length() + " question(s) générée(s). Approuvez-les ci-dessous.");
                        scrollApproval.setVisible(true);
                        btnConfirm.setVisible(true);
                        btnConfirm.setDisable(false);
                        if (hboxConfirm != null) hboxConfirm.setVisible(true);

                    } catch (Exception ex) {
                        lblStatus.setText("Erreur parsing JSON : " + ex.getMessage());
                    } finally {
                        progressIndicator.setVisible(false);
                        btnGenerate.setDisable(false);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblStatus.setText("Erreur exécution : " + ex.getMessage());
                    progressIndicator.setVisible(false);
                    btnGenerate.setDisable(false);
                });
            }
        }, "pdf-ai-thread").start();
    }

    // ── Construire la liste d'approbation ─────────────────────────────────────

    private void construireApprobation() {
        approvalList.getChildren().clear();
        checkBoxes.clear();

        for (int i = 0; i < pendingQuestions.size(); i++) {
            JSONObject q = pendingQuestions.get(i);
            VBox card = creerCarteQuestion(q, i);
            approvalList.getChildren().add(card);
        }

        mettreAJourCompteur();
    }

    private VBox creerCarteQuestion(JSONObject q, int index) {
        // Type badge color
        String type  = q.optString("type", "texte");
        String niveau = q.optString("niveau", "moyen");
        String badgeColor = switch (type) {
            case "vrai_faux"      -> "#4CAF50";
            case "choix_multiple" -> "#2196F3";
            default               -> "#FF9800";
        };
        String niveauColor = switch (niveau) {
            case "facile"    -> "#0A8A5A";
            case "difficile" -> "#C0222E";
            default          -> "#C47A00";
        };

        // Card container
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                      "-fx-border-color: #eeeeee; -fx-border-radius: 12;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);" +
                      "-fx-padding: 14;");

        // Header row: checkbox + badges
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox();
        cb.setSelected(true);
        cb.setStyle("-fx-font-size: 14;");
        cb.selectedProperty().addListener((obs, old, val) -> mettreAJourCompteur());
        checkBoxes.add(cb);

        Label typeBadge = new Label(type.replace("_", " ").toUpperCase());
        typeBadge.setStyle("-fx-background-color:" + badgeColor + "22; -fx-text-fill:" + badgeColor + ";" +
                           "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 8;");

        Label niveauBadge = new Label(niveau.toUpperCase());
        niveauBadge.setStyle("-fx-background-color:" + niveauColor + "22; -fx-text-fill:" + niveauColor + ";" +
                             "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 8;");

        Label numLbl = new Label("Q" + (index + 1));
        numLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #aaaaaa;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(cb, numLbl, typeBadge, niveauBadge, spacer);

        // Texte question
        Label texte = new Label(q.optString("texte", "—"));
        texte.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111111; -fx-wrap-text: true;");
        texte.setWrapText(true);

        // Indice
        String indice = q.optString("indice", "");
        if (!indice.isBlank()) {
            Label indiceLbl = new Label("Indice : " + indice);
            indiceLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888; -fx-font-style: italic;");
            card.getChildren().addAll(header, texte, indiceLbl);
        } else {
            card.getChildren().addAll(header, texte);
        }

        // Détails selon type
        if ("choix_multiple".equals(type)) {
            GridPane grid = new GridPane();
            grid.setHgap(8); grid.setVgap(4);
            String bonne = q.optString("bonne_reponse_choix", "a");
            String[] keys = {"a","b","c","d"};
            for (int k = 0; k < keys.length; k++) {
                String val = q.optString("choix_" + keys[k], "");
                if (val.isBlank()) continue;
                boolean isCorrect = keys[k].equals(bonne);
                Label kl = new Label(keys[k].toUpperCase() + ".");
                kl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-text-fill:" + (isCorrect ? "#0A8A5A" : "#555555") + ";");
                Label vl = new Label(val + (isCorrect ? " ✓" : ""));
                vl.setStyle("-fx-font-size: 11px; -fx-text-fill:" + (isCorrect ? "#0A8A5A" : "#333333") + ";");
                grid.add(kl, k % 2 * 2, k / 2);
                grid.add(vl, k % 2 * 2 + 1, k / 2);
            }
            card.getChildren().add(grid);

        } else if ("vrai_faux".equals(type)) {
            boolean rep = q.optBoolean("bonne_reponse_bool", true);
            Label rl = new Label("Réponse : " + (rep ? "✓ VRAI" : "✗ FAUX"));
            rl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-text-fill:" + (rep ? "#0A8A5A" : "#C0222E") + ";");
            card.getChildren().add(rl);

        } else {
            String rep = q.optString("reponse_attendue", "");
            if (!rep.isBlank()) {
                Label rl = new Label("Réponse attendue : " + rep);
                rl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-font-style: italic;");
                rl.setWrapText(true);
                card.getChildren().add(rl);
            }
        }

        return card;
    }

    private void mettreAJourCompteur() {
        long approved = checkBoxes.stream().filter(CheckBox::isSelected).count();
        lblCount.setText(approved + " / " + checkBoxes.size() + " question(s) approuvée(s)");
        btnConfirm.setDisable(approved == 0);
    }

    // ── Confirmer et insérer en BDD ───────────────────────────────────────────

    @FXML
    public void confirmerQuestions() {
        int inserted = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < checkBoxes.size(); i++) {
            if (!checkBoxes.get(i).isSelected()) continue;

            JSONObject q = pendingQuestions.get(i);
            try {
                Question question = jsonToQuestion(q);
                questionService.addEntity(question);
                inserted++;
            } catch (Exception e) {
                errors.add("Q" + (i + 1) + ": " + e.getMessage());
            }
        }

        if (errors.isEmpty()) {
            lblStatus.setText("✅ " + inserted + " question(s) insérée(s) avec succès !");
        } else {
            lblStatus.setText("⚠️ " + inserted + " insérée(s), " + errors.size() + " erreur(s) : " +
                String.join(" | ", errors));
        }

        // Réinitialiser
        approvalList.getChildren().clear();
        pendingQuestions.clear();
        checkBoxes.clear();
        scrollApproval.setVisible(false);
        btnConfirm.setVisible(false);
        btnConfirm.setDisable(true);
        if (hboxConfirm != null) hboxConfirm.setVisible(false);
        lblCount.setText("");
    }

    // ── Convertion JSON → Question (STI) ─────────────────────────────────────

    private Question jsonToQuestion(JSONObject q) {
        Question question = new Question();
        question.setTexte(q.optString("texte", "Question générée par IA"));
        question.setNiveau(q.optString("niveau", "moyen"));
        question.setIndice(q.optString("indice", ""));
        question.setQuizId(q.optInt("quiz_id", 0));
        question.setType(q.optString("type", "texte"));

        // Selon le type
        switch (question.getType()) {
            case "choix_multiple" -> {
                question.setChoixA(q.optString("choix_a", null));
                question.setChoixB(q.optString("choix_b", null));
                question.setChoixC(q.optString("choix_c", null));
                question.setChoixD(q.optString("choix_d", null));
                question.setBonneReponseChoix(q.optString("bonne_reponse_choix", "a"));
                question.setBonneReponseBool(null);
                question.setReponseAttendue(null);
            }
            case "vrai_faux" -> {
                question.setChoixA(null); question.setChoixB(null);
                question.setChoixC(null); question.setChoixD(null);
                question.setBonneReponseChoix(null);
                question.setBonneReponseBool(q.optBoolean("bonne_reponse_bool", true));
                question.setReponseAttendue(null);
            }
            default -> { // texte libre
                question.setChoixA(null); question.setChoixB(null);
                question.setChoixC(null); question.setChoixD(null);
                question.setBonneReponseChoix(null);
                question.setBonneReponseBool(null);
                question.setReponseAttendue(q.optString("reponse_attendue", ""));
            }
        }
        return question;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /**
     * Recherche generate_java.py dans plusieurs emplacements possibles.
     */
    private String trouverScript() {
        List<String> candidates = List.of(
            System.getProperty("user.dir") + "/ai/generate_java.py",
            System.getProperty("user.dir") + "/src/main/resources/ai/generate_java.py",
            System.getProperty("user.dir") + "/python/generate_java.py",
            System.getProperty("user.dir") + "/generate_java.py"
        );
        for (String path : candidates) {
            if (new File(path).exists()) return path;
        }
        return null;
    }

    /**
     * Détecte l'exécutable Python disponible (py, python, python3).
     */
    private String getPythonExecutable() {
        for (String exe : new String[]{"py", "python", "python3"}) {
            try {
                Process p = new ProcessBuilder(exe, "--version").start();
                if (p.waitFor() == 0) return exe;
            } catch (Exception ignored) {}
        }
        return "py"; // fallback
    }

    private void afficherAlerte(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).show();
    }

    // ── Retour ────────────────────────────────────────────────────────────────


    @FXML
    public void retourHome() {
        try {
            Node vue = FXMLLoader.load(getClass().getResource("/QuizView.fxml"));
            StackPane parent = (StackPane) btnGenerate.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().setAll(vue);
            } else {
                afficherAlerte("contentArea introuvable !");
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur retour : " + e.getMessage()).show();
        }
    }
}
