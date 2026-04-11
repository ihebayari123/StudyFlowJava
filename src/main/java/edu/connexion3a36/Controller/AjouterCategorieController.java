package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.TypeCategorieService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class AjouterCategorieController {

    @FXML private TextField nomField;
    @FXML private TextArea descField;
    @FXML private Button ajouterBtn;
    @FXML private Label statusLabel;

    private TypeCategorieService service = new TypeCategorieService();

    @FXML
    public void initialize() {
        ajouterBtn.setOnAction(e -> ajouterCategorie());
    }

    private void ajouterCategorie() {
        String nom = nomField.getText().trim();
        String desc = descField.getText().trim();

        // ✅ Reset styles
        resetAllStyles();

        // ✅ Contrôle nom
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


        // ✅ Contrôle caractères spéciaux nom
        if (!nom.matches("[a-zA-ZÀ-ÿ0-9 ]+")) {
            showError("❌ Le nom ne doit pas contenir de caractères spéciaux !");
            setFieldError(nomField);
            return;
        }

// ✅ Contrôle caractères spéciaux description
        if (!desc.matches("[a-zA-ZÀ-ÿ0-9 .,!?'-]+")) {
            showError("❌ La description ne doit pas contenir de caractères spéciaux !");
            setAreaError(descField);
            return;
        }

        // ✅ Contrôle description
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

        // ✅ Tout valide → on ajoute
        try {
            TypeCategorie tc = new TypeCategorie(nom, desc);
            service.addCat(tc);
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13;");
            statusLabel.setText("✅ Catégorie ajoutée avec succès !");
            clearFields();
        } catch (SQLException ex) {
            showError("❌ Erreur : " + ex.getMessage());
        }
    }

    // ✅ Message erreur rouge
    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 13;");
        statusLabel.setText(message);
    }

    // ✅ Bordure rouge TextField
    private void setFieldError(TextField field) {
        field.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF; -fx-padding: 10;");
    }

    // ✅ Bordure rouge TextArea
    private void setAreaError(TextArea area) {
        area.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF; -fx-padding: 10;");
    }

    // ✅ Vider les champs après ajout réussi
    private void clearFields() {
        nomField.clear();
        descField.clear();
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("");
                    resetAllStyles();
                });
            } catch (InterruptedException ignored) {}
        }).start();
    }

    // ✅ Reset tous les styles
    private void resetAllStyles() {
        String normal = "-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: transparent; -fx-padding: 10;";
        nomField.setStyle(normal);
        descField.setStyle(normal);
    }
}