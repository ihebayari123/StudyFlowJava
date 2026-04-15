package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.ChapitreService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ChapitreController {

    // ── Header ──────────────────────────────────────────────────────────────
    @FXML private Label courseTitleLabel;
    @FXML private Label courseSubtitleLabel;
    @FXML private Button backButton;

    // ── Toolbar ──────────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Button addChapitreBtn;
    @FXML private Label statusLabel;

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<Chapitre> chapitresTable;
    @FXML private TableColumn<Chapitre, Long>    idColumn;
    @FXML private TableColumn<Chapitre, Integer> ordreColumn;
    @FXML private TableColumn<Chapitre, String>  titreColumn;
    @FXML private TableColumn<Chapitre, String>  contenuColumn;
    @FXML private TableColumn<Chapitre, String>  contentTypeColumn;
    @FXML private TableColumn<Chapitre, Integer> durationColumn;
    @FXML private TableColumn<Chapitre, Void>    actionsColumn;

    // ── State ────────────────────────────────────────────────────────────────
    private ObservableList<Chapitre> chapitreList = FXCollections.observableArrayList();
    private FilteredList<Chapitre>   filteredList;
    private ChapitreService chapitreService;
    private DashboardController dashboardController;
    private Cours currentCours;

    // ════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        chapitreService = new ChapitreService();
        setupTableColumns();
        setupActionsColumn();
        setupSearchFilter();
    }

    // Called by CoursController after loading this view
    public void setCours(Cours cours) {
        this.currentCours = cours;
        courseTitleLabel.setText("📖 Chapitres – " + cours.getTitre());
        courseSubtitleLabel.setText("Cours sélectionné : " + cours.getTitre());
        loadChapitres();
    }

    // ── Table setup ──────────────────────────────────────────────────────────
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        ordreColumn.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        contenuColumn.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        contentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("contentType"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");

                editBtn.setOnAction(e -> {
                    Chapitre ch = getTableView().getItems().get(getIndex());
                    openChapitreForm(ch);
                });
                deleteBtn.setOnAction(e -> {
                    Chapitre ch = getTableView().getItems().get(getIndex());
                    handleDelete(ch);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : box);
            }
        });
    }

    private void setupSearchFilter() {
        filteredList = new FilteredList<>(chapitreList, p -> true);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredList.setPredicate(ch -> {
                if (newV == null || newV.isBlank()) return true;
                String f = newV.toLowerCase();
                return ch.getTitre().toLowerCase().contains(f)
                        || (ch.getContenu() != null && ch.getContenu().toLowerCase().contains(f));
            });
            statusLabel.setText(filteredList.size() + " chapitre(s) affiché(s)");
        });
        chapitresTable.setItems(filteredList);
    }

    // ── Data loading ─────────────────────────────────────────────────────────
    private void loadChapitres() {
        try {
            List<Chapitre> list = chapitreService.findByCourse(currentCours.getId());
            chapitreList.setAll(list);
            statusLabel.setText(list.size() + " chapitre(s) chargé(s)");
        } catch (Exception e) {
            System.err.println("Erreur chargement chapitres: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Erreur: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les chapitres: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void refreshChapitres() {
        loadChapitres();
    }

    // ── Handlers ─────────────────────────────────────────────────────────────
    @FXML
    private void handleAddChapitre() {
        openChapitreForm(null);
    }

    @FXML
    private void handleSearch() {
        // driven by listener – nothing extra needed
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        statusLabel.setText(chapitreList.size() + " chapitre(s)");
    }

    @FXML
    private void handleBack() {
        if (dashboardController != null) {
            dashboardController.navigateTo("cours");
        }
    }

    private void handleDelete(Chapitre ch) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le chapitre");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer « " + ch.getTitre() + " » ?");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    chapitreService.delete(ch.getId());
                    chapitreList.remove(ch);
                    statusLabel.setText("Chapitre supprimé avec succès");
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ── Form modal ───────────────────────────────────────────────────────────
    private void openChapitreForm(Chapitre chapitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add_chapitre.fxml"));
            VBox root = loader.load();

            AddChapitreController ctrl = loader.getController();
            ctrl.setChapitreService(chapitreService);
            ctrl.setParentController(this);
            ctrl.setCurrentCours(currentCours);

            if (chapitre != null) {
                ctrl.setChapitreToUpdate(chapitre);
            }

            Stage stage = new Stage();
            stage.setTitle(chapitre == null ? "Ajouter un chapitre" : "Modifier un chapitre");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(chapitresTable.getScene().getWindow());
            stage.setScene(new Scene(root, 560, 620));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Injections ───────────────────────────────────────────────────────────
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // ── Utils ────────────────────────────────────────────────────────────────
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
