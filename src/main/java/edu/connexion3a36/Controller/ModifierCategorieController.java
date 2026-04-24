package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.TypeCategorieService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ModifierCategorieController {

    @FXML private TextField nomField;
    @FXML private TextArea descField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;

    private TypeCategorieService service = new TypeCategorieService();
    private TypeCategorie typeCategorie;
    private Runnable onSuccess;

    public void setTypeCategorie(TypeCategorie tc) {
        this.typeCategorie = tc;
        nomField.setText(tc.getNomCategorie());
        descField.setText(tc.getDescription());
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        saveBtn.setOnAction(e -> sauvegarder());
        cancelBtn.setOnAction(e -> fermer());
    }

    private void sauvegarder() {
        String nom = nomField.getText().trim();
        String desc = descField.getText().trim();


        resetAllStyles();


        if (nom.isEmpty()) {
            showError("❌ Le nom est obligatoire !");
            setFieldError(nomField);
            return;
        }
        if (nom.length() < 2) {
            showError("❌ Le nom doit contenir au moins 2 caractères !");
            setFieldError(nomField);
            return;
        }
        if (nom.length() > 100) {
            showError("❌ Le nom ne peut pas dépasser 100 caractères !");
            setFieldError(nomField);
            return;
        }
        if (!nom.matches("[a-zA-ZÀ-ÿ0-9 ]+")) {
            showError("❌ Le nom ne doit pas contenir de caractères spéciaux !");
            setFieldError(nomField);
            return;
        }


        if (desc.isEmpty()) {
            showError("❌ La description est obligatoire !");
            setAreaError(descField);
            return;
        }
        if (desc.length() < 5) {
            showError("❌ La description doit contenir au moins 5 caractères !");
            setAreaError(descField);
            return;
        }
        if (!desc.matches("[a-zA-ZÀ-ÿ0-9 .,!?'-]+")) {
            showError("❌ La description ne doit pas contenir de caractères spéciaux !");
            setAreaError(descField);
            return;
        }

        // Tout valide → on sauvegarde
        try {
            typeCategorie.setNomCategorie(nom);
            typeCategorie.setDescription(desc);
            service.updateCat(typeCategorie.getId(), typeCategorie);
            if (onSuccess != null) onSuccess.run();
            fermer();
        } catch (SQLException ex) {
            showError("❌ Erreur : " + ex.getMessage());
        }
    }


    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 13;");
        statusLabel.setText(message);
    }


    private void setFieldError(TextField field) {
        field.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF; -fx-padding: 10;");
    }


    private void setAreaError(TextArea area) {
        area.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF; -fx-padding: 10;");
    }


    private void resetAllStyles() {
        String normal = "-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: transparent; -fx-padding: 10;";
        nomField.setStyle(normal);
        descField.setStyle(normal);
    }

    private void fermer() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}