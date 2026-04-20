package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.services.MedecinService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AjouterMedecinController {

    @FXML private TextField nomTF;
    @FXML private TextField prenomTF;
    @FXML private TextField emailTF;
    @FXML private TextField telephoneTF;
    @FXML private ComboBox<String> disponibiliteCB;
    @FXML private Label drapeauLabel;

    private final MedecinService service = new MedecinService();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@gmail\\.com$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+\\d{3}\\d{8}$");

    private final Map<String, String> paysDrapeaux = new HashMap<>();

    @FXML
    public void initialize() {
        // Initialize combo box
        disponibiliteCB.getItems().addAll("disponible", "indisponible");


        paysDrapeaux.put("+212", "🇲🇦"); // Maroc
        paysDrapeaux.put("+337", "🇫🇷"); // France
        paysDrapeaux.put("+491", "🇩🇪"); // Allemagne
        paysDrapeaux.put("+346", "🇪🇸"); // Espagne
        paysDrapeaux.put("+447", "🇬🇧"); // Royaume Uni
        paysDrapeaux.put("+120", "🇺🇸"); // USA
        paysDrapeaux.put("+216", "Tn");
        // Convertir automatiquement nom en majuscule
        nomTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(newValue.toUpperCase())) {
                nomTF.setText(newValue.toUpperCase());
            }
        });

        // Convertir automatiquement prenom en majuscule
        prenomTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(newValue.toUpperCase())) {
                prenomTF.setText(newValue.toUpperCase());
            }
        });

        // Detection pays et affichage drapeau pour telephone
        telephoneTF.textProperty().addListener((observable, oldValue, newValue) -> {
            drapeauLabel.setText("");
            if (newValue.length() >= 4 && newValue.startsWith("+")) {
                String codePays = newValue.substring(0, 4);
                if (paysDrapeaux.containsKey(codePays)) {
                    drapeauLabel.setText(paysDrapeaux.get(codePays));
                }
            }
        });
    }

    @FXML
    private void ajouter(ActionEvent event) {
        String nom = nomTF.getText().trim();
        String prenom = prenomTF.getText().trim();
        String email = emailTF.getText().trim();
        String telephone = telephoneTF.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || telephone.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "Veuillez remplir tous les champs !");
            return;
        }

        // Validation email
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showAlert(Alert.AlertType.WARNING, "Email invalide", "L'email doit se terminer par @gmail.com !");
            emailTF.requestFocus();
            return;
        }

        // Validation telephone
        if (!PHONE_PATTERN.matcher(telephone).matches()) {
            showAlert(Alert.AlertType.WARNING, "Téléphone invalide", "Le numéro doit être au format +XXX followed by 8 chiffres !");
            telephoneTF.requestFocus();
            return;
        }

        try {
            Medecin medecin = new Medecin();
            medecin.setNom(nom.toUpperCase());
            medecin.setPrenom(prenom.toUpperCase());
            medecin.setEmail(email);
            medecin.setTelephone(telephone);
            medecin.setDisponibilite(disponibiliteCB.getValue());

            service.addEntity(medecin);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Médecin ajouté avec succès !");
            reinitialiser(event);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
        }
    }

    @FXML
    private void reinitialiser(ActionEvent event) {
        nomTF.clear();
        prenomTF.clear();
        emailTF.clear();
        telephoneTF.clear();
        disponibiliteCB.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void consulterListe(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/AfficherMedecin.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Liste des Médecins");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (java.io.IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la liste : " + e.getMessage());
        }
    }
}