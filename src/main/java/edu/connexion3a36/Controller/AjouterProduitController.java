package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Produit;
import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.ProduitService;
import edu.connexion3a36.services.TypeCategorieService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;

public class AjouterProduitController {

    @FXML private TextField nomField;
    @FXML private TextArea descField;
    @FXML private TextField prixField;
    @FXML private TextField imageField;
    @FXML private ComboBox<TypeCategorie> typeCategorieCombo;
    @FXML private ComboBox<Integer> userCombo;
    @FXML private Button ajouterBtn;
    @FXML private Label statusLabel;

    private ProduitService produitService = new ProduitService();
    private TypeCategorieService typeCategorieService = new TypeCategorieService();

    @FXML
    public void initialize() {
        loadCategories();
        loadUsers();
        ajouterBtn.setOnAction(e -> ajouterProduit());
    }

    private void loadCategories() {
        try {
            List<TypeCategorie> categories = typeCategorieService.getData();
            typeCategorieCombo.setItems(FXCollections.observableArrayList(categories));
            typeCategorieCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(TypeCategorie item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getNomCategorie());
                }
            });
            typeCategorieCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(TypeCategorie item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getNomCategorie());
                }
            });
        } catch (SQLException e) {
            statusLabel.setText("❌ Erreur chargement catégories : " + e.getMessage());
        }
    }

    private void loadUsers() {
        userCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    }

    private void ajouterProduit() {
        String nom = nomField.getText().trim();
        String desc = descField.getText().trim();
        String prixStr = prixField.getText().trim();
        String image = imageField.getText().trim();
        TypeCategorie selectedCat = typeCategorieCombo.getValue();
        Integer selectedUser = userCombo.getValue();

        // ✅ Réinitialiser les styles
        resetAllStyles();

        // ✅ Contrôle caractères spéciaux nom
        if (!nom.matches("[a-zA-ZÀ-ÿ0-9 ]+")) {
            showError(statusLabel, "❌ Le nom ne doit pas contenir de caractères spéciaux !");
            setFieldError(nomField);
            return;
        }

// ✅ Contrôle caractères spéciaux description
        if (!desc.matches("[a-zA-ZÀ-ÿ0-9 .,!?'-]+")) {
            showError(statusLabel, "❌ La description ne doit pas contenir de caractères spéciaux !");
            setAreaError(descField);
            return;
        }

        // ✅ Contrôle nom
        if (nom.isEmpty()) {
            showError(statusLabel, "❌ Le nom est obligatoire !");
            setFieldError(nomField);
            return;
        }
        if (nom.length() < 2) {
            showError(statusLabel, "❌ Le nom doit contenir au moins 2 caractères !");
            setFieldError(nomField);
            return;
        }
        if (nom.length() > 100) {
            showError(statusLabel, "❌ Le nom ne peut pas dépasser 100 caractères !");
            setFieldError(nomField);
            return;
        }

        // ✅ Contrôle description
        if (desc.isEmpty()) {
            showError(statusLabel, "❌ La description est obligatoire !");
            setAreaError(descField);
            return;
        }
        if (desc.length() < 5) {
            showError(statusLabel, "❌ La description doit contenir au moins 5 caractères !");
            setAreaError(descField);
            return;
        }

        // ✅ Contrôle prix
        if (prixStr.isEmpty()) {
            showError(statusLabel, "❌ Le prix est obligatoire !");
            setFieldError(prixField);
            return;
        }
        int prix;
        try {
            prix = Integer.parseInt(prixStr);
            if (prix <= 0) {
                showError(statusLabel, "❌ Le prix doit être un nombre positif !");
                setFieldError(prixField);
                return;
            }
            if (prix > 100000) {
                showError(statusLabel, "❌ Le prix ne peut pas dépasser 100 000 DT !");
                setFieldError(prixField);
                return;
            }
        } catch (NumberFormatException e) {
            showError(statusLabel, "❌ Le prix doit être un nombre entier (ex: 1200) !");
            setFieldError(prixField);
            return;
        }


        // ✅ Contrôle image
        if (image.isEmpty()) {
            showError(statusLabel, "❌ L'image est obligatoire !");
            setFieldError(imageField);
            return;
        }

        // ✅ Contrôle catégorie
        if (selectedCat == null) {
            showError(statusLabel, "❌ Veuillez sélectionner une catégorie !");
            typeCategorieCombo.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8;");
            return;
        }

        // ✅ Contrôle utilisateur
        if (selectedUser == null) {
            showError(statusLabel, "❌ Veuillez sélectionner un utilisateur !");
            userCombo.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8;");
            return;
        }

        // ✅ Tout valide → on ajoute
        try {
            Produit p = new Produit(nom, desc, prix, image, selectedCat.getId(), selectedUser);
            produitService.addP(p);
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13;");
            statusLabel.setText("✅ Produit ajouté avec succès !");
            clearFields();
        } catch (SQLException e) {
            showError(statusLabel, "❌ Erreur BD : " + e.getMessage());
        }
    }

    // ✅ Afficher message erreur en rouge
    private void showError(Label label, String message) {
        label.setStyle("-fx-text-fill: #F44336; -fx-font-size: 13;");
        label.setText(message);
    }

    // ✅ Bordure rouge sur TextField
    private void setFieldError(TextField field) {
        field.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF; -fx-padding: 10;");
    }

    // ✅ Bordure rouge sur TextArea
    private void setAreaError(TextArea area) {
        area.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF; -fx-padding: 10;");
    }

    // ✅ Vider les champs après ajout réussi
    private void clearFields() {
        nomField.clear();
        descField.clear();
        prixField.clear();
        imageField.clear();
        typeCategorieCombo.setValue(null);
        userCombo.setValue(null);
        // Reset styles après 2 secondes
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

    // ✅ Reset tous les styles à la normale
    private void resetAllStyles() {
        String normal = "-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: transparent; -fx-padding: 10;";
        nomField.setStyle(normal);
        descField.setStyle(normal);
        prixField.setStyle(normal);
        imageField.setStyle(normal);
        typeCategorieCombo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");
        userCombo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");
    }
}