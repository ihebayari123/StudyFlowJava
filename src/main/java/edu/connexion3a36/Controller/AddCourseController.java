package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.AnthropicAIService;
import edu.connexion3a36.services.CoursService;
import edu.connexion3a36.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AddCourseController {

    @FXML private Label formTitle;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private TextField imageField;
    @FXML private ImageView imagePreview;
    @FXML private Button saveButton;

    // AI controls
    @FXML private TextField aiAudienceField;
    @FXML private Button aiGenerateBtn;
    @FXML private Label aiStatusLabel;
    @FXML private TextArea aiObjectivesArea;

    // Error labels
    @FXML private Label titreErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label imageErrorLabel;

    private CoursService coursService;
    private CoursController parentController;
    private Cours courseToUpdate;
    private Utilisateur currentUser;

    @FXML
    public void initialize() {
        setupValidation();
        setupInputListeners();
        if (coursService == null) {
            coursService = new CoursService();
        }
    }

    // ── AI Feature 1: Description Generator ──────────────────────────────────

    @FXML
    private void handleGenerateDescription() {
        String titre = titreField.getText().trim();
        if (titre.isEmpty()) {
            showAlert("Titre requis",
                    "Veuillez d'abord saisir le titre du cours avant de générer une description.",
                    Alert.AlertType.WARNING);
            return;
        }

        String audience = (aiAudienceField != null) ? aiAudienceField.getText().trim() : "";

        // Disable controls while working
        aiGenerateBtn.setDisable(true);
        aiGenerateBtn.setText("⏳ Génération...");
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("🤖 L'IA génère la description...");
            aiStatusLabel.setStyle("-fx-text-fill: #2979FF; -fx-font-size: 11px;");
        }

        Task<AnthropicAIService.CourseDescriptionResult> task = new Task<>() {
            @Override
            protected AnthropicAIService.CourseDescriptionResult call() {
                return AnthropicAIService.getInstance()
                        .generateCourseDescription(titre, audience);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AnthropicAIService.CourseDescriptionResult result = task.getValue();
            descriptionField.setText(result.getDescription());

            if (aiObjectivesArea != null && !result.getObjectives().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String obj : result.getObjectives()) {
                    sb.append("• ").append(obj).append("\n");
                }
                aiObjectivesArea.setText(sb.toString().trim());
            }

            if (aiStatusLabel != null) {
                aiStatusLabel.setText("✅ Description générée avec succès !");
                aiStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
            }
            resetAiButton();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            String msg = task.getException() != null
                    ? task.getException().getMessage()
                    : "Erreur inconnue";
            if (aiStatusLabel != null) {
                aiStatusLabel.setText("❌ Erreur : " + msg);
                aiStatusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
            }
            showAlert("Erreur IA", "Impossible de générer la description :\n" + msg,
                    Alert.AlertType.ERROR);
            resetAiButton();
        }));

        new Thread(task, "ai-description-thread").start();
    }

    private void resetAiButton() {
        aiGenerateBtn.setDisable(false);
        aiGenerateBtn.setText("✨ Générer avec l'IA");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void setupValidation() {
        saveButton.disableProperty().bind(
                titreField.textProperty().isEmpty()
                        .or(descriptionField.textProperty().isEmpty())
                        .or(titreField.styleProperty().isEqualTo("-fx-border-color: red;"))
        );
    }

    private void setupInputListeners() {
        titreField.textProperty().addListener((obs, o, n) -> validateTitre());
        descriptionField.textProperty().addListener((obs, o, n) -> validateDescription());
        imageField.textProperty().addListener((obs, o, n) -> validateImageUrl());
    }

    private void validateTitre() {
        String titre = titreField.getText();
        if (titre == null || titre.trim().isEmpty()) {
            titreField.setStyle("-fx-border-color: red; -fx-border-radius: 3;");
            setErrorLabel(titreErrorLabel, "Le titre est obligatoire");
            return;
        }
        if (!ValidationUtils.isValidTitre(titre)) {
            titreField.setStyle("-fx-border-color: red; -fx-border-radius: 3;");
            setErrorLabel(titreErrorLabel, "Le titre doit contenir entre 3 et 100 caractères");
            return;
        }
        titreField.setStyle("-fx-border-color: green; -fx-border-radius: 3;");
        setErrorLabel(titreErrorLabel, null);
    }

    private void validateDescription() {
        String description = descriptionField.getText();
        if (description == null || description.trim().isEmpty()) {
            descriptionField.setStyle("-fx-border-color: red; -fx-border-radius: 3;");
            setErrorLabel(descriptionErrorLabel, "La description est obligatoire");
            return;
        }
        if (!ValidationUtils.isValidDescription(description)) {
            descriptionField.setStyle("-fx-border-color: red; -fx-border-radius: 3;");
            setErrorLabel(descriptionErrorLabel, "La description doit contenir entre 10 et 500 caractères");
            return;
        }
        descriptionField.setStyle("-fx-border-color: green; -fx-border-radius: 3;");
        setErrorLabel(descriptionErrorLabel, null);
    }

    private void validateImageUrl() {
        String url = imageField.getText();
        if (url != null && !url.trim().isEmpty()) {
            if (!ValidationUtils.isValidImageUrl(url)) {
                imageField.setStyle("-fx-border-color: orange; -fx-border-radius: 3;");
                setErrorLabel(imageErrorLabel, "⚠️ URL d'image invalide (formats acceptés: jpg, png, gif, webp)");
                return;
            }
        }
        imageField.setStyle("-fx-border-color: green; -fx-border-radius: 3;");
        setErrorLabel(imageErrorLabel, null);
    }

    private void setErrorLabel(Label label, String message) {
        if (label == null) return;
        if (message != null) {
            label.setText(message);
            label.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
            label.setVisible(true);
        } else {
            label.setText("");
            label.setVisible(false);
        }
    }

    // ── File browser ──────────────────────────────────────────────────────────

    @FXML
    private void handleBrowseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = fc.showOpenDialog(imageField.getScene().getWindow());
        if (file != null) {
            String path = file.toURI().toString();
            imageField.setText(path);
            validateImageUrl();
            try {
                imagePreview.setImage(new Image(path, 100, 100, true, true));
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        validateTitre();
        validateDescription();
        validateImageUrl();

        if (!isFormValid()) {
            showAlert("Erreur de validation",
                    "Veuillez corriger les erreurs dans le formulaire :\n" + getValidationErrors(),
                    Alert.AlertType.ERROR);
            return;
        }

        try {
            String titre       = ValidationUtils.capitalizeFirstLetter(titreField.getText().trim());
            String description = ValidationUtils.sanitizeText(descriptionField.getText().trim());
            String image       = imageField.getText().trim();

            if (courseToUpdate != null) {
                courseToUpdate.setTitre(titre);
                courseToUpdate.setDescription(description);
                courseToUpdate.setImage(image.isEmpty() ? null : image);
                coursService.update(courseToUpdate);
                showAlert("Succès", "Cours modifié avec succès", Alert.AlertType.INFORMATION);
            } else {
                Cours cours = new Cours();
                cours.setTitre(titre);
                cours.setDescription(description);
                cours.setImage(image.isEmpty() ? null : image);
                cours.setUserId(currentUser != null ? currentUser.getId() : 1L);
                coursService.save(cours);
                showAlert("Succès", "Cours ajouté avec succès", Alert.AlertType.INFORMATION);
            }

            if (parentController != null) parentController.refreshCourses();
            closeWindow();

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'enregistrement : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private boolean isFormValid() {
        return ValidationUtils.isValidTitre(titreField.getText())
                && ValidationUtils.isValidDescription(descriptionField.getText())
                && (imageField.getText().isEmpty() || ValidationUtils.isValidImageUrl(imageField.getText()));
    }

    private String getValidationErrors() {
        StringBuilder sb = new StringBuilder();
        if (!ValidationUtils.isValidTitre(titreField.getText()))
            sb.append("• Titre invalide (3-100 caractères)\n");
        if (!ValidationUtils.isValidDescription(descriptionField.getText()))
            sb.append("• Description invalide (10-500 caractères)\n");
        if (!imageField.getText().isEmpty() && !ValidationUtils.isValidImageUrl(imageField.getText()))
            sb.append("• URL d'image invalide\n");
        return sb.toString();
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setCourseToUpdate(Cours cours) {
        this.courseToUpdate = cours;
        formTitle.setText("Modifier un Cours");
        titreField.setText(cours.getTitre());
        descriptionField.setText(cours.getDescription());
        imageField.setText(cours.getImage() != null ? cours.getImage() : "");
        if (cours.getImage() != null && !cours.getImage().isEmpty()) {
            try { imagePreview.setImage(new Image(cours.getImage(), 100, 100, true, true)); }
            catch (Exception ignored) {}
        }
        validateTitre();
        validateDescription();
        validateImageUrl();
    }

    public void setCoursService(CoursService svc)          { this.coursService = svc; }
    public void setParentController(CoursController ctrl)  { this.parentController = ctrl; }
    public void setCurrentUser(Utilisateur user)           { this.currentUser = user; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void closeWindow() {
        ((Stage) titreField.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
