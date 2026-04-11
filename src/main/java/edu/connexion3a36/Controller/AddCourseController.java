package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.CoursService;
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

    private CoursService coursService;
    private CoursController parentController;
    private Cours courseToUpdate;
    private Utilisateur currentUser;

    @FXML
    public void initialize() {
        setupValidation();
        if (coursService == null) {
            coursService = new CoursService();
        }
    }

    private void setupValidation() {
        saveButton.disableProperty().bind(
                titreField.textProperty().isEmpty()
                        .or(descriptionField.textProperty().isEmpty())
        );
    }

    public void setCoursService(CoursService coursService) {
        this.coursService = coursService;
    }

    public void setParentController(CoursController parentController) {
        this.parentController = parentController;
    }

    public void setCourseToUpdate(Cours cours) {
        this.courseToUpdate = cours;
        formTitle.setText("Modifier un Cours");
        titreField.setText(cours.getTitre());
        descriptionField.setText(cours.getDescription());
        imageField.setText(cours.getImage());
        if (cours.getImage() != null && !cours.getImage().isEmpty()) {
            try {
                imagePreview.setImage(new Image(cours.getImage(), true));
            } catch (Exception e) {
                // Image invalide
            }
        }
    }

    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(imageField.getScene().getWindow());
        if (selectedFile != null) {
            String imagePath = selectedFile.toURI().toString();
            imageField.setText(imagePath);
            try {
                imagePreview.setImage(new Image(imagePath, true));
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInputs()) {
            return;
        }

        try {
            if (courseToUpdate != null) {
                // MISE À JOUR - Modifier le cours existant
                System.out.println("Mise à jour du cours ID: " + courseToUpdate.getId());

                courseToUpdate.setTitre(titreField.getText().trim());
                courseToUpdate.setDescription(descriptionField.getText().trim());
                courseToUpdate.setImage(imageField.getText().trim());

                // Appeler update au lieu de save
                coursService.update(courseToUpdate);
                showAlert("Succès", "Cours modifié avec succès", Alert.AlertType.INFORMATION);
            } else {
                // CRÉATION - Ajouter un nouveau cours
                System.out.println("Création d'un nouveau cours");

                Cours cours = new Cours();
                cours.setTitre(capitalizeTitle(titreField.getText().trim()));
                cours.setDescription(descriptionField.getText().trim());
                cours.setImage(imageField.getText().trim());

                if (currentUser != null) {
                    cours.setUserId(currentUser.getId());
                } else {
                    cours.setUserId(1L);
                }
                if (!isValidImageUrl(imageField.getText().trim())) {
                    showAlert("Attention", "L'URL de l'image n'est pas valide", Alert.AlertType.WARNING);
                    return;
                }

                coursService.save(cours);
                showAlert("Succès", "Cours ajouté avec succès", Alert.AlertType.INFORMATION);
            }

            // Rafraîchir la table parente
            if (parentController != null) {
                parentController.refreshCourses();
            }
            closeWindow();

        } catch (Exception e) {
            System.err.println("Erreur dans handleSave: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'enregistrement: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validateInputs() {
        if (titreField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le titre est obligatoire", Alert.AlertType.ERROR);
            return false;
        }
        if (descriptionField.getText().trim().isEmpty()) {
            showAlert("Erreur", "La description est obligatoire", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
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
    // Ajouter cette méthode pour valider le format de l'URL de l'image
    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) return true;
        return url.matches("^(https?://.*\\.(png|jpg|jpeg|gif|webp))|(file:/.*)$");
    }

    // Ajouter cette méthode pour capitaliser la première lettre du titre
    private String capitalizeTitle(String title) {
        if (title == null || title.isEmpty()) return title;
        return title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
    }

}