package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.services.MedecinService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class AfficherMedecinController {

    @FXML private TableView<Medecin> tableView;
    @FXML private TableColumn<Medecin, Integer> colId;
    @FXML private TableColumn<Medecin, String> colNom;
    @FXML private TableColumn<Medecin, String> colPrenom;
    @FXML private TableColumn<Medecin, String> colEmail;
    @FXML private TableColumn<Medecin, String> colTelephone;
    @FXML private TableColumn<Medecin, String> colDisponibilite;

    @FXML private TextField rechercheTF;
    @FXML private ComboBox<String> filtreDispoCB;

    @FXML private TextField nomTF;
    @FXML private TextField prenomTF;
    @FXML private TextField emailTF;
    @FXML private TextField telephoneTF;
    @FXML private ComboBox<String> disponibiliteCB;

    private ObservableList<Medecin> listeComplete;
    private FilteredList<Medecin> filteredList;
    private SortedList<Medecin> sortedList;

    private final MedecinService service = new MedecinService();
    private Medecin selectedMedecin;

    @FXML
    public void initialize() {
        // Initialiser les colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colDisponibilite.setCellValueFactory(new PropertyValueFactory<>("disponibilite"));

        // Gestion de la sélection
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedMedecin = newSelection;
            if (newSelection != null && nomTF != null) {
                updateSidebarInfo(selectedMedecin);
            }
        });

        // Initialiser les filtres
        if (filtreDispoCB != null) {
            filtreDispoCB.getItems().addAll("Tous", "Disponible", "Indisponible");
            filtreDispoCB.setValue("Tous");
        }

        // Initialiser la ComboBox de disponibilité dans la sidebar
        if (disponibiliteCB != null) {
            disponibiliteCB.getItems().addAll("disponible", "indisponible");
        }

        loadData();
    }

    private void updateSidebarInfo(Medecin medecin) {
        if (medecin != null) {
            if (nomTF != null) nomTF.setText(medecin.getNom());
            if (prenomTF != null) prenomTF.setText(medecin.getPrenom());
            if (emailTF != null) emailTF.setText(medecin.getEmail());
            if (telephoneTF != null) telephoneTF.setText(medecin.getTelephone());
            if (disponibiliteCB != null) disponibiliteCB.setValue(medecin.getDisponibilite());
        }
    }

    private void loadData() {
        try {
            List<Medecin> list = service.getData();
            listeComplete = FXCollections.observableArrayList(list);

            // Créer une FilteredList
            filteredList = new FilteredList<>(listeComplete, p -> true);

            // Configurer les filtres
            setupFilters();

            // Créer une SortedList
            sortedList = new SortedList<>(filteredList);

            // Lier le comparateur
            sortedList.comparatorProperty().bind(tableView.comparatorProperty());

            // Afficher dans la TableView
            tableView.setItems(sortedList);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void setupFilters() {
        if (rechercheTF != null) {
            rechercheTF.textProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }

        if (filtreDispoCB != null) {
            filtreDispoCB.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
    }

    private void applyFilters() {
        String rechercheTexte = rechercheTF != null && rechercheTF.getText() != null ?
                rechercheTF.getText().trim().toLowerCase() : "";
        String filtreDispo = filtreDispoCB != null ? filtreDispoCB.getValue() : "Tous";

        filteredList.setPredicate(medecin -> {
            // Filtre de recherche
            boolean matchRecherche = true;
            if (!rechercheTexte.isEmpty()) {
                matchRecherche = (medecin.getNom() != null && medecin.getNom().toLowerCase().contains(rechercheTexte)) ||
                        (medecin.getPrenom() != null && medecin.getPrenom().toLowerCase().contains(rechercheTexte));
            }

            // Filtre de disponibilité
            boolean matchDispo = true;
            if (filtreDispo != null && !"Tous".equals(filtreDispo)) {
                if ("Disponible".equals(filtreDispo)) {
                    matchDispo = "disponible".equalsIgnoreCase(medecin.getDisponibilite());
                } else if ("Indisponible".equals(filtreDispo)) {
                    matchDispo = "indisponible".equalsIgnoreCase(medecin.getDisponibilite());
                }
            }

            return matchRecherche && matchDispo;
        });
    }

    @FXML
    void trierCroissant(ActionEvent event) {
        // Créer une nouvelle liste triée
        List<Medecin> sorted = filteredList.stream()
                .sorted(Comparator.comparing(Medecin::getNom, String.CASE_INSENSITIVE_ORDER))
                .collect(java.util.stream.Collectors.toList());

        // Mettre à jour la liste filtrée
        filteredList = new FilteredList<>(FXCollections.observableArrayList(sorted), p -> true);
        applyFilters(); // Réappliquer les filtres
        sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
    }

    @FXML
    void trierDecroissant(ActionEvent event) {
        // Créer une nouvelle liste triée
        List<Medecin> sorted = filteredList.stream()
                .sorted(Comparator.comparing(Medecin::getNom, String.CASE_INSENSITIVE_ORDER).reversed())
                .collect(java.util.stream.Collectors.toList());

        // Mettre à jour la liste filtrée
        filteredList = new FilteredList<>(FXCollections.observableArrayList(sorted), p -> true);
        applyFilters(); // Réappliquer les filtres
        sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
    }

    @FXML
    void modifier(ActionEvent event) {
        if (selectedMedecin == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Veuillez sélectionner un médecin à modifier !");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierMedecin.fxml"));
            javafx.scene.Parent root = loader.load();
            ModifierMedecinController controller = loader.getController();
            controller.setMedecin(selectedMedecin);

            Stage stage = new Stage();
            stage.setTitle("Modifier un Médecin");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.show();

            // Optionnel : fermer la fenêtre actuelle
            // Stage currentStage = (Stage) tableView.getScene().getWindow();
            // currentStage.close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification : " + e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        if (selectedMedecin == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Veuillez sélectionner un médecin !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le médecin " + selectedMedecin.getNom() + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.deleteEntity(selectedMedecin);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Médecin supprimé !");
                    loadData(); // Recharger les données
                    selectedMedecin = null;
                    clearSidebarInfo();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
                }
            }
        });
    }

    private void clearSidebarInfo() {
        if (nomTF != null) nomTF.clear();
        if (prenomTF != null) prenomTF.clear();
        if (emailTF != null) emailTF.clear();
        if (telephoneTF != null) telephoneTF.clear();
        if (disponibiliteCB != null) disponibiliteCB.setValue(null);
    }

    @FXML
    void actualiser(ActionEvent event) {
        loadData();
        if (rechercheTF != null) rechercheTF.clear();
        if (filtreDispoCB != null) filtreDispoCB.setValue("Tous");
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public void fermer(ActionEvent actionEvent) {
        Stage stage = (Stage) tableView.getScene().getWindow();
        stage.close();
    }
}