package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Produit;
import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.ProduitService;
import edu.connexion3a36.services.TypeCategorieService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class ModifierProduitController {

    @FXML private TextField nomField;
    @FXML private TextArea descField;
    @FXML private TextField prixField;
    @FXML private TextField imageField;
    @FXML private ComboBox<TypeCategorie> typeCategorieCombo;
    @FXML private ComboBox<Integer> userCombo;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;

    private ProduitService produitService = new ProduitService();
    private TypeCategorieService typeCategorieService = new TypeCategorieService();
    private Produit produit;
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        loadCategories();
        loadUsers();
        saveBtn.setOnAction(e -> sauvegarder());
        cancelBtn.setOnAction(e -> fermer());
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
            statusLabel.setText("❌ Erreur chargement catégories");
        }
    }

    private void loadUsers() {
        userCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    }

    public void setProduit(Produit p) {
        this.produit = p;
        nomField.setText(p.getNom());
        descField.setText(p.getDescription());
        prixField.setText(String.valueOf(p.getPrix()));
        imageField.setText(p.getImage());
        userCombo.setValue(p.getUserId());
        typeCategorieCombo.getItems().stream()
                .filter(tc -> tc.getId() == p.getTypeCategorieId())
                .findFirst()
                .ifPresent(tc -> typeCategorieCombo.setValue(tc));
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    private void sauvegarder() {
        String nom = nomField.getText().trim();
        String desc = descField.getText().trim();
        String prixStr = prixField.getText().trim();
        String image = imageField.getText().trim();
        TypeCategorie selectedCat = typeCategorieCombo.getValue();
        Integer selectedUser = userCombo.getValue();

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
        if (!nom.matches("[a-zA-ZÀ-ÿ0-9 ]+")) {
            showError("❌ Le nom ne doit pas contenir de caractères spéciaux !");
            setFieldError(nomField);
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
        if (!desc.matches("[a-zA-ZÀ-ÿ0-9 .,!?'-]+")) {
            showError("❌ La description ne doit pas contenir de caractères spéciaux !");
            setAreaError(descField);
            return;
        }

        // ✅ Contrôle prix
        if (prixStr.isEmpty()) {
            showError("❌ Le prix est obligatoire !");
            setFieldError(prixField);
            return;
        }
        int prix;
        try {
            prix = Integer.parseInt(prixStr);
            if (prix <= 0) {
                showError("❌ Le prix doit être un nombre positif !");
                setFieldError(prixField);
                return;
            }
            if (prix > 100000) {
                showError("❌ Le prix ne peut pas dépasser 100 000 DT !");
                setFieldError(prixField);
                return;
            }
        } catch (NumberFormatException e) {
            showError("❌ Le prix doit être un nombre entier (ex: 1200) !");
            setFieldError(prixField);
            return;
        }

        // ✅ Contrôle image
        if (image.isEmpty()) {
            showError("❌ L'image est obligatoire !");
            setFieldError(imageField);
            return;
        }

        // ✅ Contrôle catégorie
        if (selectedCat == null) {
            showError("❌ Veuillez sélectionner une catégorie !");
            typeCategorieCombo.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8;");
            return;
        }

        // ✅ Contrôle utilisateur
        if (selectedUser == null) {
            showError("❌ Veuillez sélectionner un utilisateur !");
            userCombo.setStyle("-fx-border-color: #F44336; -fx-border-radius: 8;");
            return;
        }

        // ✅ Tout valide → on sauvegarde
        try {
            produit.setNom(nom);
            produit.setDescription(desc);
            produit.setPrix(prix);
            produit.setImage(image);
            produit.setTypeCategorieId(selectedCat.getId());
            produit.setUserId(selectedUser);
            produitService.updateP(produit.getId(), produit);
            if (onSuccess != null) onSuccess.run();
            fermer();
        } catch (SQLException e) {
            showError("❌ Erreur BD : " + e.getMessage());
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

    // ✅ Reset tous les styles
    private void resetAllStyles() {
        String normal = "-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: transparent; -fx-padding: 10;";
        nomField.setStyle(normal);
        descField.setStyle(normal);
        prixField.setStyle(normal);
        imageField.setStyle(normal);
        typeCategorieCombo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");
        userCombo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");
    }

    private void fermer() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}