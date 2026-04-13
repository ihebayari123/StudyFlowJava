package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.CoursService;
import edu.connexion3a36.utils.ValidationUtils;
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

    // Labels pour les messages d'erreur
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

    private void setupValidation() {
        // Désactiver le bouton save tant que les champs ne sont pas valides
        saveButton.disableProperty().bind(
                titreField.textProperty().isEmpty()
                        .or(descriptionField.textProperty().isEmpty())
                        .or(titreField.styleProperty().isEqualTo("-fx-border-color: red;"))
        );
    }

    private void setupInputListeners() {
        // Validation en temps réel du titre
        titreField.textProperty().addListener((obs, oldVal, newVal) -> validateTitre());

        // Validation en temps réel de la description
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> validateDescription());

        // Validation en temps réel de l'URL image
        imageField.textProperty().addListener((obs, oldVal, newVal) -> validateImageUrl());
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

        // Validation réussie
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
        if (label != null) {
            if (message != null) {
                label.setText(message);
                label.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
                label.setVisible(true);
            } else {
                label.setText("");
                label.setVisible(false);
            }
        }
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(imageField.getScene().getWindow());
        if (selectedFile != null) {
            String imagePath = selectedFile.toURI().toString();
            imageField.setText(imagePath);
            validateImageUrl();
            try {
                Image image = new Image(imagePath, 100, 100, true, true);
                imagePreview.setImage(image);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image", Alert.AlertType.ERROR);
            }
        }
    }
    @FXML
    private void handleCancel() {
        closeWindow();
    }


    @FXML
    private void handleSave() {
        // Valider tous les champs avant sauvegarde
        validateTitre();
        validateDescription();
        validateImageUrl();

        if (!isFormValid()) {
            showAlert("Erreur de validation",
                    "Veuillez corriger les erreurs dans le formulaire:\n" +
                            getValidationErrors(),
                    Alert.AlertType.ERROR);
            return;
        }

        try {
            String titre = ValidationUtils.capitalizeFirstLetter(titreField.getText().trim());
            String description = ValidationUtils.sanitizeText(descriptionField.getText().trim());
            String image = imageField.getText().trim();

            if (courseToUpdate != null) {
                // Mise à jour
                courseToUpdate.setTitre(titre);
                courseToUpdate.setDescription(description);
                courseToUpdate.setImage(image.isEmpty() ? null : image);
                coursService.update(courseToUpdate);
                showAlert("Succès", "Cours modifié avec succès", Alert.AlertType.INFORMATION);
            } else {
                // Création
                Cours cours = new Cours();
                cours.setTitre(titre);
                cours.setDescription(description);
                cours.setImage(image.isEmpty() ? null : image);
                cours.setUserId(currentUser != null ? currentUser.getId() : 1L);
                coursService.save(cours);
                showAlert("Succès", "Cours ajouté avec succès", Alert.AlertType.INFORMATION);
            }

            if (parentController != null) {
                parentController.refreshCourses();
            }
            closeWindow();

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            showAlert("Erreur", "Erreur lors de l'enregistrement: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean isFormValid() {
        return ValidationUtils.isValidTitre(titreField.getText()) &&
                ValidationUtils.isValidDescription(descriptionField.getText()) &&
                (imageField.getText().isEmpty() || ValidationUtils.isValidImageUrl(imageField.getText()));
    }

    private String getValidationErrors() {
        StringBuilder errors = new StringBuilder();
        if (!ValidationUtils.isValidTitre(titreField.getText())) {
            errors.append("• Titre invalide (3-100 caractères)\n");
        }
        if (!ValidationUtils.isValidDescription(descriptionField.getText())) {
            errors.append("• Description invalide (10-500 caractères)\n");
        }
        if (!imageField.getText().isEmpty() && !ValidationUtils.isValidImageUrl(imageField.getText())) {
            errors.append("• URL d'image invalide\n");
        }
        return errors.toString();
    }

    public void setCourseToUpdate(Cours cours) {
        this.courseToUpdate = cours;
        formTitle.setText("Modifier un Cours");
        titreField.setText(cours.getTitre());
        descriptionField.setText(cours.getDescription());
        imageField.setText(cours.getImage());
        if (cours.getImage() != null && !cours.getImage().isEmpty()) {
            try {
                imagePreview.setImage(new Image(cours.getImage(), 100, 100, true, true));
            } catch (Exception e) {
                // Image invalide
            }
        }
        validateTitre();
        validateDescription();
        validateImageUrl();
    }

    public void setCoursService(CoursService coursService) {
        this.coursService = coursService;
    }

    public void setParentController(CoursController parentController) {
        this.parentController = parentController;
    }

    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
    }

    private void closeWindow() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}