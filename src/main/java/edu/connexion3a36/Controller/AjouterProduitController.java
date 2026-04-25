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
    @FXML private Button predireBtn;
    @FXML private Button dashboardBtn;

    private ProduitService produitService = new ProduitService();
    private TypeCategorieService typeCategorieService = new TypeCategorieService();
    //------------
    private int dernierPrixPredit = 0;

    @FXML
    public void initialize() {
        loadCategories();
        loadUsers();
        ajouterBtn.setOnAction(e -> ajouterProduit());
        predireBtn.setOnAction(e -> predirePrix());
        dashboardBtn.setOnAction(e -> ouvrirDashboard());
    }

    //-----------------------dashborad-----------------------
    private void ouvrirDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/PerformanceDashboard.fxml")
            );

            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("📊 Dashboard de performance");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError(statusLabel, "❌ Erreur ouverture dashboard : " + e.getMessage());
        }
    }

    //-------------------------------------PREDICTION---------------------------------------------------
    private void logPrediction(String nom, String desc, String categorie, int prixPredit, int prixReel) {
        new Thread(() -> {
            try {
                String json = String.format(
                        "{\"nom\":\"%s\",\"description\":\"%s\",\"categorie\":\"%s\",\"prix_predit\":%d,\"prix_reel\":%d}",
                        nom.replace("\"", "\\\""),
                        desc.replace("\"", "\\\""),
                        categorie.replace("\"", "\\\""),
                        prixPredit, prixReel
                );
                java.net.URL url = new java.net.URL("http://localhost:5000/log");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes("UTF-8"));
                }
                conn.getResponseCode(); // déclenche l'envoi
            } catch (Exception ignored) {}
        }).start();
    }

    //                    ------------                           --------------
    private void predirePrix() {
        String nom  = nomField.getText().trim();
        String desc = descField.getText().trim();
        TypeCategorie cat = typeCategorieCombo.getValue();



        if (nom.isEmpty() || desc.isEmpty() || cat == null) {
            showError(statusLabel, "❌ Remplissez nom, description et catégorie d'abord !");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #7C3AED; -fx-font-size: 13;");
        statusLabel.setText("⏳ Prédiction en cours...");

        new Thread(() -> {
            try {
                String json = String.format(
                        "{\"nom\":\"%s\",\"description\":\"%s\",\"categorie\":\"%s\"}",
                        nom.replace("\"", "\\\""),
                        desc.replace("\"", "\\\""),
                        cat.getNomCategorie().replace("\"", "\\\"")
                );

                java.net.URL url = new java.net.URL("http://localhost:5000/predict");
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes("UTF-8"));
                }

                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "UTF-8")
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                String body = response.toString();
                double prix = Double.parseDouble(
                        body.replaceAll(".*\"prix\"\\s*:\\s*([\\d.]+).*", "$1")
                );

                javafx.application.Platform.runLater(() -> {
                    prixField.setText(String.valueOf((int) prix));
                    dernierPrixPredit = (int) prix;
                    statusLabel.setStyle("-fx-text-fill: #7C3AED; -fx-font-size: 13;");
                    statusLabel.setText("Prix predit : " + (int) prix + " DT");
                });

            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    showError(statusLabel, "❌ Erreur : Flask est-il lancé ? " + ex.getMessage());
                });
            }
        }).start();
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



            //------------------------------
            // Loguer la prédiction si elle a été faite
            String prixPreditStr = prixField.getText().trim();
            if (dernierPrixPredit > 0) {
                logPrediction(nom, desc, selectedCat.getNomCategorie(), dernierPrixPredit, prix);
            }
            //-----------------------



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