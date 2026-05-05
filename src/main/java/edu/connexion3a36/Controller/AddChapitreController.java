package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.AnthropicAIService;
import edu.connexion3a36.services.ChapitreService;
import edu.connexion3a36.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AddChapitreController {

    @FXML private Label     formTitle;
    @FXML private TextField titreField;
    @FXML private TextArea  contenuField;
    @FXML private TextField ordreField;
    @FXML private ComboBox<String> contentTypeBox;
    @FXML private TextField videoUrlField;
    @FXML private TextField imageUrlField;
    @FXML private TextField fileNameField;
    @FXML private TextField durationField;

    // AI – difficulty controls
    @FXML private Button    aiDifficultyBtn;
    @FXML private Label     aiDifficultyLabel;    // shows badge e.g. "🟢 Débutant"
    @FXML private Label     aiDifficultyConfidence; // "Confiance : Haute"
    @FXML private Label     aiDifficultyReason;   // one-sentence justification
    @FXML private ComboBox<String> difficultyCombo; // manual override

    // Error labels
    @FXML private Label titreErrorLabel;
    @FXML private Label contenuErrorLabel;
    @FXML private Label ordreErrorLabel;

    @FXML private Button saveButton;

    private ChapitreService chapitreService;
    private ChapitreController parentController;
    private Cours currentCours;
    private Chapitre chapitreToUpdate;

    @FXML
    public void initialize() {
        contentTypeBox.getItems().addAll("text", "video", "image", "file", "mixed");
        contentTypeBox.setValue("text");

        // Difficulty combo – teacher can pick manually or accept AI suggestion
        if (difficultyCombo != null) {
            difficultyCombo.getItems().addAll("", "Débutant", "Intermédiaire", "Avancé");
            difficultyCombo.setValue("");
        }

        setupValidation();
        setupListeners();
    }

    // ── AI Feature 3: Difficulty Tagger ──────────────────────────────────────

    @FXML
    private void handleAnalyzeDifficulty() {
        String titre   = titreField.getText().trim();
        String contenu = contenuField.getText().trim();

        if (titre.isEmpty() && contenu.isEmpty()) {
            showAlert("Champs requis",
                    "Veuillez saisir le titre et/ou le contenu avant d'analyser la difficulté.",
                    Alert.AlertType.WARNING);
            return;
        }

        aiDifficultyBtn.setDisable(true);
        aiDifficultyBtn.setText("⏳ Analyse...");
        if (aiDifficultyLabel != null) {
            aiDifficultyLabel.setText("🤖 Analyse en cours...");
            aiDifficultyLabel.setStyle("-fx-text-fill: #2979FF; -fx-font-size: 12;");
        }

        Task<AnthropicAIService.DifficultyResult> task = new Task<>() {
            @Override
            protected AnthropicAIService.DifficultyResult call() {
                return AnthropicAIService.getInstance()
                        .analyzeChapterDifficulty(titre, contenu);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AnthropicAIService.DifficultyResult result = task.getValue();

            // Update badge label
            if (aiDifficultyLabel != null) {
                aiDifficultyLabel.setText(result.getIcon() + "  " + result.getNiveau());
                aiDifficultyLabel.setStyle(
                        "-fx-text-fill: " + result.getBadgeColor() + "; " +
                                "-fx-font-weight: bold; -fx-font-size: 13;");
            }
            if (aiDifficultyConfidence != null) {
                aiDifficultyConfidence.setText("Confiance : " + result.getConfiance());
                aiDifficultyConfidence.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
            }
            if (aiDifficultyReason != null) {
                aiDifficultyReason.setText(result.getJustification());
                aiDifficultyReason.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");
            }

            // Pre-select in the combo so the value gets saved
            if (difficultyCombo != null) {
                difficultyCombo.setValue(result.getNiveau());
            }

            resetDifficultyBtn();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            String msg = task.getException() != null
                    ? task.getException().getMessage() : "Erreur inconnue";
            if (aiDifficultyLabel != null) {
                aiDifficultyLabel.setText("❌ Erreur IA");
                aiDifficultyLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            }
            showAlert("Erreur IA", "Impossible d'analyser la difficulté :\n" + msg,
                    Alert.AlertType.ERROR);
            resetDifficultyBtn();
        }));

        new Thread(task, "ai-difficulty-thread").start();
    }

    private void resetDifficultyBtn() {
        aiDifficultyBtn.setDisable(false);
        aiDifficultyBtn.setText("🎯 Analyser la difficulté");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void setupValidation() {
        saveButton.disableProperty().bind(
                titreField.textProperty().isEmpty()
                        .or(contenuField.textProperty().isEmpty())
                        .or(ordreField.textProperty().isEmpty())
        );
    }

    private void setupListeners() {
        titreField.textProperty().addListener((obs, o, n) -> validateTitre());
        contenuField.textProperty().addListener((obs, o, n) -> validateContenu());
        ordreField.textProperty().addListener((obs, o, n) -> validateOrdre());
    }

    private boolean validateTitre() {
        String v = titreField.getText();
        if (v == null || v.trim().isEmpty()) {
            setError(titreField, titreErrorLabel, "Le titre est obligatoire"); return false;
        }
        if (v.trim().length() < 3 || v.trim().length() > 255) {
            setError(titreField, titreErrorLabel, "Le titre doit contenir entre 3 et 255 caractères"); return false;
        }
        setOk(titreField, titreErrorLabel); return true;
    }

    private boolean validateContenu() {
        String v = contenuField.getText();
        if (v == null || v.trim().isEmpty()) {
            setError(contenuField, contenuErrorLabel, "Le contenu est obligatoire"); return false;
        }
        if (v.trim().length() < 5) {
            setError(contenuField, contenuErrorLabel, "Le contenu doit contenir au moins 5 caractères"); return false;
        }
        setOk(contenuField, contenuErrorLabel); return true;
    }

    private boolean validateOrdre() {
        String v = ordreField.getText();
        if (v == null || v.trim().isEmpty()) {
            setError(ordreField, ordreErrorLabel, "L'ordre est obligatoire"); return false;
        }
        try {
            int val = Integer.parseInt(v.trim());
            if (val < 1) { setError(ordreField, ordreErrorLabel, "L'ordre doit être ≥ 1"); return false; }
        } catch (NumberFormatException e) {
            setError(ordreField, ordreErrorLabel, "L'ordre doit être un nombre entier"); return false;
        }
        setOk(ordreField, ordreErrorLabel); return true;
    }

    private void setError(Control field, Label label, String msg) {
        field.setStyle("-fx-border-color: red; -fx-border-radius: 3;");
        if (label != null) { label.setText(msg); label.setVisible(true); }
    }

    private void setOk(Control field, Label label) {
        field.setStyle("-fx-border-color: green; -fx-border-radius: 3;");
        if (label != null) { label.setText(""); label.setVisible(false); }
    }

    // ── Save / Cancel ─────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        boolean valid = validateTitre() & validateContenu() & validateOrdre();
        if (!valid) {
            showAlert("Erreur de validation",
                    "Veuillez corriger les erreurs dans le formulaire.", Alert.AlertType.ERROR);
            return;
        }

        try {
            String titre     = ValidationUtils.capitalizeFirstLetter(titreField.getText().trim());
            String contenu   = contenuField.getText().trim();
            int    ordre     = Integer.parseInt(ordreField.getText().trim());
            String type      = contentTypeBox.getValue();
            String videoUrl  = videoUrlField.getText().trim();
            String imageUrl  = imageUrlField.getText().trim();
            String fileName  = fileNameField.getText().trim();
            Integer duration = parseDuration();

            // Read difficulty from combo (may be AI-suggested or manually chosen)
            String difficulty = (difficultyCombo != null && difficultyCombo.getValue() != null
                    && !difficultyCombo.getValue().isBlank())
                    ? difficultyCombo.getValue()
                    : null;

            if (chapitreToUpdate != null) {
                chapitreToUpdate.setTitre(titre);
                chapitreToUpdate.setContenu(contenu);
                chapitreToUpdate.setOrdre(ordre);
                chapitreToUpdate.setContentType(type.isEmpty()     ? null : type);
                chapitreToUpdate.setVideoUrl(videoUrl.isEmpty()    ? null : videoUrl);
                chapitreToUpdate.setImageUrl(imageUrl.isEmpty()    ? null : imageUrl);
                chapitreToUpdate.setFileName(fileName.isEmpty()    ? null : fileName);
                chapitreToUpdate.setDurationMinutes(duration);
                chapitreToUpdate.setDifficulty(difficulty);          // NEW
                chapitreService.update(chapitreToUpdate);
                showAlert("Succès", "Chapitre modifié avec succès", Alert.AlertType.INFORMATION);
            } else {
                Chapitre ch = new Chapitre();
                ch.setTitre(titre);
                ch.setContenu(contenu);
                ch.setOrdre(ordre);
                ch.setCourse(currentCours);
                ch.setContentType(type.isEmpty()     ? null : type);
                ch.setVideoUrl(videoUrl.isEmpty()    ? null : videoUrl);
                ch.setImageUrl(imageUrl.isEmpty()    ? null : imageUrl);
                ch.setFileName(fileName.isEmpty()    ? null : fileName);
                ch.setDurationMinutes(duration);
                ch.setDifficulty(difficulty);                        // NEW
                chapitreService.save(ch);
                showAlert("Succès", "Chapitre ajouté avec succès", Alert.AlertType.INFORMATION);
            }

            if (parentController != null) parentController.refreshChapitres();
            closeWindow();

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'enregistrement : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    // ── Public setters ────────────────────────────────────────────────────────

    /**
     * Pre-fills the form with AI-suggested values WITHOUT entering edit mode.
     * chapitreToUpdate stays null → handleSave will do INSERT not UPDATE.
     * Called from CoursController when opening a form from the outline dialog.
     */
    public void prefillForNew(String titre, String contenu, int ordre) {
        formTitle.setText("➕ Ajouter un Chapitre (suggestion IA)");
        titreField.setText(titre);
        contenuField.setText(contenu);
        ordreField.setText(String.valueOf(ordre));
        // chapitreToUpdate remains null — this is a new chapter
        validateTitre();
        validateContenu();
        validateOrdre();
    }

    public void setChapitreToUpdate(Chapitre ch) {
        this.chapitreToUpdate = ch;
        formTitle.setText("✏️ Modifier un Chapitre");
        titreField.setText(ch.getTitre());
        contenuField.setText(ch.getContenu());
        ordreField.setText(String.valueOf(ch.getOrdre()));
        contentTypeBox.setValue(ch.getContentType() != null ? ch.getContentType() : "text");
        videoUrlField.setText(ch.getVideoUrl()  != null ? ch.getVideoUrl()  : "");
        imageUrlField.setText(ch.getImageUrl()  != null ? ch.getImageUrl()  : "");
        fileNameField.setText(ch.getFileName()  != null ? ch.getFileName()  : "");
        durationField.setText(ch.getDurationMinutes() != null
                ? String.valueOf(ch.getDurationMinutes()) : "");

        // Restore difficulty if it was previously set
        if (difficultyCombo != null && ch.getDifficulty() != null) {
            difficultyCombo.setValue(ch.getDifficulty());
            if (aiDifficultyLabel != null) {
                AnthropicAIService.DifficultyResult stub =
                        new AnthropicAIService.DifficultyResult(ch.getDifficulty(), "", "");
                aiDifficultyLabel.setText(stub.getIcon() + "  " + ch.getDifficulty());
                aiDifficultyLabel.setStyle("-fx-text-fill: " + stub.getBadgeColor()
                        + "; -fx-font-weight: bold; -fx-font-size: 13;");
            }
        }

        validateTitre(); validateContenu(); validateOrdre();
    }

    public void setCurrentCours(Cours cours) {
        this.currentCours = cours;
        if (chapitreToUpdate == null && chapitreService != null) {
            int next = chapitreService.getNextOrdre(cours.getId());
            ordreField.setText(String.valueOf(next));
        }
    }

    public void setChapitreService(ChapitreService svc)    { this.chapitreService = svc; }
    public void setParentController(ChapitreController ctrl){ this.parentController = ctrl; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Integer parseDuration() {
        String v = durationField.getText().trim();
        if (v.isEmpty()) return null;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
    }

    private void closeWindow() {
        ((Stage) titreField.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
}