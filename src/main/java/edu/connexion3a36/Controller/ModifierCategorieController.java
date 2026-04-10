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

        if (nom.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            statusLabel.setText("❌ Le nom est obligatoire !");
            return;
        }

        try {
            typeCategorie.setNomCategorie(nom);
            typeCategorie.setDescription(desc);
            service.updateCat(typeCategorie.getId(), typeCategorie);
            if (onSuccess != null) onSuccess.run();
            fermer();
        } catch (SQLException ex) {
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            statusLabel.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void fermer() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}