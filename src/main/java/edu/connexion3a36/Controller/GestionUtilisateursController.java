package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class GestionUtilisateursController {

    @FXML private TableView<Utilisateur> tableUtilisateurs;
    @FXML private TableColumn<Utilisateur, Integer> colId;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colStatut;
    @FXML private TableColumn<Utilisateur, Void> colActions;
    @FXML private TextField searchField;

    UtilisateurService service = new UtilisateurService();
    ObservableList<Utilisateur> observableList = FXCollections.observableArrayList();

    @FXML
    public void initialize() throws SQLException {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutCompte"));

        ajouterColonneActions();
        chargerDonnees();
        configurerRecherche();
    }

    private void chargerDonnees() throws SQLException {
        observableList.setAll(service.getData());
        tableUtilisateurs.setItems(observableList);
    }

    private void configurerRecherche() {
        FilteredList<Utilisateur> filteredList = new FilteredList<>(observableList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(u -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filtre = newVal.toLowerCase();
                return u.getNom().toLowerCase().contains(filtre)
                        || u.getPrenom().toLowerCase().contains(filtre)
                        || u.getEmail().toLowerCase().contains(filtre)
                        || u.getRole().toLowerCase().contains(filtre);
            });
        });
        tableUtilisateurs.setItems(filteredList);
    }

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnModifier = new Button("✏ Modifier");
            final Button btnSupprimer = new Button("🗑 Supprimer");
            final HBox hbox = new HBox(6, btnModifier, btnSupprimer);

            {
                btnModifier.setStyle("-fx-background-color: #2979FF; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 6; -fx-cursor: hand;");
                btnSupprimer.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 6; -fx-cursor: hand;");

                btnModifier.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    ouvrirModifier(u);
                });

                btnSupprimer.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    confirmerSuppression(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    @FXML
    void ouvrirAjouter() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterUtilisateur.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("Ajouter un utilisateur");
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
        try { chargerDonnees(); } catch (SQLException e) { e.printStackTrace(); }
    }

    private void ouvrirModifier(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierUtilisateur.fxml"));
            Parent root = loader.load();
            ModifierUtilisateurController controller = loader.getController();
            controller.setUtilisateur(u);
            Stage stage = new Stage();
            stage.setTitle("Modifier un utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            chargerDonnees();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void confirmerSuppression(Utilisateur u) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer " + u.getNom() + " " + u.getPrenom() + " ?");
        alert.setContentText("Cette action est irréversible.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.deleteEntity(u);
                chargerDonnees();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}