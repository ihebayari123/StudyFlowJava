package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.services.MedecinService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    private final MedecinService service = new MedecinService();
    private Medecin selectedMedecin;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colDisponibilite.setCellValueFactory(new PropertyValueFactory<>("disponibilite"));

        // Set up table selection
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedMedecin = newSelection;
                // Update the fields in the sidebar with selected doctor's info if needed
                updateSidebarInfo(selectedMedecin);
            }
        });

        // Initialiser les filtres
        filtreDispoCB.getItems().addAll("Tous", "Disponible", "Indisponible");
        filtreDispoCB.setValue("Tous");

        // Listener recherche dynamique
        rechercheTF.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        // Listener filtre disponibilité
        filtreDispoCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        loadData();
    }

    private void updateSidebarInfo(Medecin medecin) {
        // This could be used to display doctor info in the sidebar
        // For now, we'll just store the selectedMedecin
    }

    private void loadData() {
        try {
            List<Medecin> list = service.getData();
            listeComplete = FXCollections.observableArrayList(list);
            tableView.setItems(listeComplete);
            updateSummaryStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void trierCroissant(ActionEvent event) {
        ObservableList<Medecin> listeActuelle = tableView.getItems();
        listeActuelle.sort(Comparator.comparing(Medecin::getNom, String.CASE_INSENSITIVE_ORDER));
    }

    @FXML
    void trierDecroissant(ActionEvent event) {
        ObservableList<Medecin> listeActuelle = tableView.getItems();
        listeActuelle.sort(Comparator.comparing(Medecin::getNom, String.CASE_INSENSITIVE_ORDER).reversed());
    }

    private void appliquerFiltres() {
        String recherche = rechercheTF.getText().trim().toLowerCase();
        String filtreDispo = filtreDispoCB.getValue();

        List<Medecin> resultat = listeComplete.stream()
                .filter(m -> m.getNom().toLowerCase().contains(recherche) || m.getPrenom().toLowerCase().contains(recherche))
                .filter(m -> {
                    if ("Tous".equals(filtreDispo)) return true;
                    if ("Disponible".equals(filtreDispo)) return "disponible".equalsIgnoreCase(m.getDisponibilite());
                    if ("Indisponible".equals(filtreDispo)) return "indisponible".equalsIgnoreCase(m.getDisponibilite());
                    return true;
                })
                .collect(Collectors.toList());

        tableView.setItems(FXCollections.observableArrayList(resultat));
    }

    @FXML
    void modifier(ActionEvent event) {
        if (selectedMedecin == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Veuillez sélectionner un médecin à modifier !");
            return;
        }

        try {
            // Load the modifier form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierMedecin.fxml"));
            javafx.scene.Parent root = loader.load();
            ModifierMedecinController controller = loader.getController();
            controller.setMedecin(selectedMedecin); // Pass the selected doctor

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modifier un Médecin");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.show();
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
                    loadData();
                    selectedMedecin = null;
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage());
                }
            }
        });
    }

    @FXML
    void actualiser(ActionEvent event) { 
        loadData(); 
    }

    private void updateSummaryStats() {
        // Update the summary labels in the sidebar
        // This would require adding fx:id to the labels in the FXML
        // For simplicity, we'll leave them as "0" for now
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    public void fermer(javafx.event.ActionEvent actionEvent) {
        // Close the current window
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node)actionEvent.getSource()).getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            // Fallback: just hide the table view if we can't get the stage
            if (actionEvent.getSource() instanceof javafx.scene.Node) {
                javafx.scene.Node source = (javafx.scene.Node) actionEvent.getSource();
                javafx.stage.Window window = source.getScene().getWindow();
                if (window instanceof javafx.stage.Stage) {
                    ((javafx.stage.Stage) window).close();
                } else {
                    window.hide();
                }
            }
        }
    }
}