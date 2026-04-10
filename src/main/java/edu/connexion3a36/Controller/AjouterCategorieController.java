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

        if (nom.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            statusLabel.setText("❌ Le nom est obligatoire !");
            return;
        }

        try {
            TypeCategorie tc = new TypeCategorie(nom, desc);
            service.addCat(tc);
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            statusLabel.setText("✅ Catégorie ajoutée avec succès !");
            nomField.clear();
            descField.clear();
        } catch (SQLException ex) {
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            statusLabel.setText("❌ Erreur : " + ex.getMessage());
        }
    }
}