package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
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
    @FXML private TableColumn<Utilisateur, String>  colNom;
    @FXML private TableColumn<Utilisateur, String>  colPrenom;
    @FXML private TableColumn<Utilisateur, String>  colEmail;
    @FXML private TableColumn<Utilisateur, String>  colRole;
    @FXML private TableColumn<Utilisateur, String>  colStatut;
    @FXML private TableColumn<Utilisateur, Void>    colActions;
    @FXML private TextField                          searchField;
    @FXML private ComboBox<String>                   filtreRole;
    @FXML private ComboBox<String>                   filtreStatut;
    @FXML private Label                              compteurLabel;

    UtilisateurService service = new UtilisateurService();
    ObservableList<Utilisateur> observableList = FXCollections.observableArrayList();
    FilteredList<Utilisateur> filteredList;

    // ═══════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════
    @FXML
    public void initialize() throws SQLException {
        // Colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutCompte"));

        // ComboBox filtres
        filtreRole.setItems(FXCollections.observableArrayList(
                "Tous les rôles", "ADMIN", "ENSEIGNANT", "ETUDIANT",
                "ROLE_ADMIN", "ROLE_ENSEIGNANT", "ROLE_ETUDIANT"));
        filtreRole.setValue("Tous les rôles");

        filtreStatut.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "ACTIF", "BLOQUE", "INACTIF"));
        filtreStatut.setValue("Tous les statuts");

        ajouterColonneActions();
        chargerDonnees();
        configurerFiltres();
    }

    // ═══════════════════════════════
    // CHARGER DONNEES
    // ═══════════════════════════════
    private void chargerDonnees() throws SQLException {
        observableList.setAll(service.getData());
        filteredList = new FilteredList<>(observableList, p -> true);
        tableUtilisateurs.setItems(filteredList);
        mettreAJourCompteur();
    }

    // ═══════════════════════════════
    // FILTRES COMBINES
    // ═══════════════════════════════
    private void configurerFiltres() {
        // Recherche texte
        searchField.textProperty().addListener((obs, old, val) -> appliquerFiltres());

        // Filtre rôle
        filtreRole.valueProperty().addListener((obs, old, val) -> appliquerFiltres());

        // Filtre statut
        filtreStatut.valueProperty().addListener((obs, old, val) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        String recherche = searchField.getText().toLowerCase().trim();
        String role      = filtreRole.getValue();
        String statut    = filtreStatut.getValue();

        filteredList.setPredicate(u -> {
            // Filtre recherche texte
            boolean matchRecherche = recherche.isEmpty()
                    || u.getNom().toLowerCase().contains(recherche)
                    || u.getPrenom().toLowerCase().contains(recherche)
                    || u.getEmail().toLowerCase().contains(recherche);

            // Filtre rôle
            boolean matchRole = role == null
                    || role.equals("Tous les rôles")
                    || u.getRole().equals(role);

            // Filtre statut
            boolean matchStatut = statut == null
                    || statut.equals("Tous les statuts")
                    || u.getStatutCompte().equals(statut);

            return matchRecherche && matchRole && matchStatut;
        });

        mettreAJourCompteur();
    }

    // ═══════════════════════════════
    // REINITIALISER FILTRES
    // ═══════════════════════════════
    @FXML
    void reinitialiserFiltres(ActionEvent event) {
        searchField.clear();
        filtreRole.setValue("Tous les rôles");
        filtreStatut.setValue("Tous les statuts");
        appliquerFiltres();
    }

    // ═══════════════════════════════
    // COMPTEUR RESULTATS
    // ═══════════════════════════════
    private void mettreAJourCompteur() {
        int total   = observableList.size();
        int affiche = filteredList.size();
        if (affiche == total) {
            compteurLabel.setText(total + " utilisateurs au total");
        } else {
            compteurLabel.setText(affiche + " résultat(s) sur " + total + " utilisateurs");
        }
    }

    // ═══════════════════════════════
    // COLONNE ACTIONS
    // ═══════════════════════════════
    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnModifier  = new Button("✏ Modifier");
            final Button btnSupprimer = new Button("🗑 Supprimer");
            final Button btnBloquer   = new Button();
            final HBox hbox = new HBox(6, btnModifier, btnSupprimer, btnBloquer);

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

                btnBloquer.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    try {
                        service.bloquerDebloquer(u);
                        chargerDonnees();
                        configurerFiltres();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    if (u.getStatutCompte().equals("ACTIF")) {
                        btnBloquer.setText("🔒 Bloquer");
                        btnBloquer.setStyle("-fx-background-color: #FF6F00; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 6; -fx-cursor: hand;");
                    } else {
                        btnBloquer.setText("🔓 Débloquer");
                        btnBloquer.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 6; -fx-cursor: hand;");
                    }
                    setGraphic(hbox);
                }
            }
        });
    }

    // ═══════════════════════════════
    // AJOUTER
    // ═══════════════════════════════
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

    // ═══════════════════════════════
    // MODIFIER
    // ═══════════════════════════════
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
            stage.setWidth(500);
            stage.setHeight(700);
            stage.setResizable(true);
            stage.showAndWait();
            chargerDonnees();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ═══════════════════════════════
    // SUPPRIMER
    // ═══════════════════════════════
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