package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Medecin;
import edu.connexion3a36.services.MedecinService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class ListeMedecinController {

    @FXML private TableView<Medecin> tableView;
    @FXML private TableColumn<Medecin, Integer> colId;
    @FXML private TableColumn<Medecin, String> colNom;
    @FXML private TableColumn<Medecin, String> colPrenom;
    @FXML private TableColumn<Medecin, String> colEmail;
    @FXML private TableColumn<Medecin, String> colTelephone;
    @FXML private TableColumn<Medecin, String> colDisponibilite;

    // Composants pour la recherche et le filtrage
    @FXML private TextField rechercheTF;
    @FXML private ComboBox<String> filtreDisponibiliteCB;
    @FXML private ComboBox<String> triCB;
    @FXML private ComboBox<String> ordreCB;
    @FXML private Label resultatsLabel;

    private final MedecinService service = new MedecinService();
    private ObservableList<Medecin> masterData;
    private FilteredList<Medecin> filteredData;
    private SortedList<Medecin> sortedData;

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colDisponibilite.setCellValueFactory(new PropertyValueFactory<>("disponibilite"));

        // Configuration des ComboBox
        if (filtreDisponibiliteCB != null) {
            filtreDisponibiliteCB.getItems().addAll("Tous", "Disponible", "Indisponible");
            filtreDisponibiliteCB.setValue("Tous");
        }

        if (triCB != null) {
            triCB.getItems().addAll("Nom", "Prénom", "Email", "Téléphone", "Disponibilité");
            triCB.setValue("Nom");
        }

        if (ordreCB != null) {
            ordreCB.getItems().addAll("Croissant", "Décroissant");
            ordreCB.setValue("Croissant");
        }

        // Configuration des listeners
        if (rechercheTF != null) {
            rechercheTF.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        }

        if (filtreDisponibiliteCB != null) {
            filtreDisponibiliteCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        }

        if (triCB != null) {
            triCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerTri());
        }

        if (ordreCB != null) {
            ordreCB.valueProperty().addListener((obs, oldVal, newVal) -> appliquerTri());
        }

        // Chargement des données
        loadData();
    }

    private void loadData() {
        try {
            List<Medecin> list = service.getData();
            masterData = FXCollections.observableArrayList(list);

            // Création de la liste filtrée
            filteredData = new FilteredList<>(masterData, p -> true);

            // Création de la liste triée
            sortedData = new SortedList<>(filteredData);

            // Application des filtres et du tri initiaux
            appliquerFiltres();
            appliquerTri();

            // Liaison à la TableView
            tableView.setItems(sortedData);

            // Mise à jour des statistiques
            updateResultatsLabel();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void appliquerFiltres() {
        if (filteredData == null) return;

        String recherche = rechercheTF != null && rechercheTF.getText() != null ?
                rechercheTF.getText().trim().toLowerCase() : "";
        String filtreDispo = filtreDisponibiliteCB != null ?
                filtreDisponibiliteCB.getValue() : "Tous";

        filteredData.setPredicate(medecin -> {
            // Filtre de recherche textuelle (nom, prénom, email, téléphone)
            if (!recherche.isEmpty()) {
                boolean matchNom = medecin.getNom() != null &&
                        medecin.getNom().toLowerCase().contains(recherche);
                boolean matchPrenom = medecin.getPrenom() != null &&
                        medecin.getPrenom().toLowerCase().contains(recherche);
                boolean matchEmail = medecin.getEmail() != null &&
                        medecin.getEmail().toLowerCase().contains(recherche);
                boolean matchTelephone = medecin.getTelephone() != null &&
                        medecin.getTelephone().contains(recherche);

                if (!(matchNom || matchPrenom || matchEmail || matchTelephone)) {
                    return false;
                }
            }

            // Filtre de disponibilité
            if (!"Tous".equals(filtreDispo)) {
                if ("Disponible".equals(filtreDispo)) {
                    if (!"disponible".equalsIgnoreCase(medecin.getDisponibilite())) {
                        return false;
                    }
                } else if ("Indisponible".equals(filtreDispo)) {
                    if (!"indisponible".equalsIgnoreCase(medecin.getDisponibilite())) {
                        return false;
                    }
                }
            }

            return true;
        });

        // Mise à jour des statistiques après filtrage
        updateResultatsLabel();
    }

    private void appliquerTri() {
        if (sortedData == null) return;

        String champTri = triCB != null ? triCB.getValue() : "Nom";
        String ordre = ordreCB != null ? ordreCB.getValue() : "Croissant";

        if (champTri == null || ordre == null) {
            sortedData.setComparator(null);
            return;
        }

        Comparator<Medecin> comparator = null;

        switch (champTri) {
            case "Nom":
                comparator = Comparator.comparing(Medecin::getNom,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Prénom":
                comparator = Comparator.comparing(Medecin::getPrenom,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Email":
                comparator = Comparator.comparing(Medecin::getEmail,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Téléphone":
                comparator = Comparator.comparing(Medecin::getTelephone,
                        Comparator.nullsLast(String::compareTo));
                break;
            case "Disponibilité":
                comparator = Comparator.comparing(Medecin::getDisponibilite,
                        Comparator.nullsLast(String::compareTo));
                break;
        }

        if (comparator != null) {
            if ("Décroissant".equals(ordre)) {
                comparator = comparator.reversed();
            }
            sortedData.setComparator(comparator);
        }
    }

    private void updateResultatsLabel() {
        if (resultatsLabel != null && filteredData != null && masterData != null) {
            int total = masterData.size();
            int affiches = filteredData.size();
            resultatsLabel.setText("Affichage de " + affiches + " médecin(s) sur " + total);
        }
    }

    @FXML
    private void resetFiltres() {
        if (rechercheTF != null) rechercheTF.clear();
        if (filtreDisponibiliteCB != null) filtreDisponibiliteCB.setValue("Tous");
        if (triCB != null) triCB.setValue("Nom");
        if (ordreCB != null) ordreCB.setValue("Croissant");

        if (filteredData != null) {
            filteredData.setPredicate(p -> true);
        }

        appliquerTri();
        updateResultatsLabel();

        showAlert(Alert.AlertType.INFORMATION, "Filtres réinitialisés",
                "Tous les filtres ont été réinitialisés.");
    }

    @FXML
    private void actualiser() {
        loadData();
        resetFiltres();
    }

    @FXML
    private void fermer() {
        tableView.getScene().getWindow().hide();
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}