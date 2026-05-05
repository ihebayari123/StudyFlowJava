package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.ChapitreService;
import edu.connexion3a36.services.ChapitreVersionService;
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
    private ChapitreVersionService versionService;
    private DashboardController dashboardController;
    private Cours currentCours;

    // ════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        chapitreService = new ChapitreService();
        versionService  = new ChapitreVersionService();
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
            private final Button editBtn    = new Button("✏️ Modifier");
            private final Button deleteBtn  = new Button("🗑️ Supprimer");
            private final Button historyBtn = new Button("📋 Historique");
            private final HBox   box        = new HBox(6, editBtn, historyBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                historyBtn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");

                editBtn.setOnAction(e -> {
                    Chapitre ch = getTableView().getItems().get(getIndex());
                    openChapitreForm(ch);
                });
                deleteBtn.setOnAction(e -> {
                    Chapitre ch = getTableView().getItems().get(getIndex());
                    handleDelete(ch);
                });
                historyBtn.setOnAction(e -> {                                  // ← ADD
                    Chapitre ch = getTableView().getItems().get(getIndex());
                    openHistorique(ch);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : box);
            }
        });
    }

    private void openHistorique(Chapitre chapitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/historique_version.fxml"));
            VBox root = loader.load();
            HistoriqueVersionController ctrl = loader.getController();
            ctrl.setChapitre(chapitre);

            Stage stage = new Stage();
            stage.setTitle("Historique des versions – " + chapitre.getTitre());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(chapitresTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'historique: " + e.getMessage(), Alert.AlertType.ERROR);
        }
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

    // ── Version history modal ─────────────────────────────────────────────────
    private void openVersionHistory(Chapitre ch) {
        try {
            List<edu.connexion3a36.entities.ChapitreVersion> versions =
                    versionService.findByChapitreId(ch.getId());

            Stage stage = new Stage();
            stage.setTitle("📋 Historique des versions – " + ch.getTitre());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(chapitresTable.getScene().getWindow());

            VBox root = new VBox(0);
            root.setStyle("-fx-background-color: #F5F6FA;");

            // ── Header ────────────────────────────────────────────────────
            HBox header = new HBox();
            header.setStyle("-fx-background-color: #1A1A2E; -fx-padding: 18 24;");
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            header.setSpacing(12);
            Label icon = new Label("📋");
            icon.setStyle("-fx-font-size: 22px;");
            VBox titleBox = new VBox(2);
            Label titleLbl = new Label("Historique des versions");
            titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
            Label subLbl = new Label(ch.getTitre() + " · " + versions.size() + " version(s)");
            subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #9E9E9E;");
            titleBox.getChildren().addAll(titleLbl, subLbl);
            header.getChildren().addAll(icon, titleBox);

            // ── Versions list ─────────────────────────────────────────────
            ScrollPane scroll = new ScrollPane();
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

            VBox list = new VBox(10);
            list.setStyle("-fx-padding: 20 24;");

            if (versions.isEmpty()) {
                Label empty = new Label(
                        "Aucune version disponible.\nModifiez ce chapitre pour créer la première version.");
                empty.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 13px; -fx-padding: 40; -fx-alignment: CENTER;");
                empty.setWrapText(true);
                list.getChildren().add(empty);
            } else {
                for (edu.connexion3a36.entities.ChapitreVersion v : versions) {
                    list.getChildren().add(buildVersionCard(v));
                }
            }

            scroll.setContent(list);
            root.getChildren().addAll(header, scroll);

            stage.setScene(new Scene(root, 680, 560));
            stage.show();

        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir l'historique: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox buildVersionCard(edu.connexion3a36.entities.ChapitreVersion v) {
        double pct = v.getModificationPercentage();

        // Colour based on change intensity
        String accentColor = pct >= 60 ? "#e74c3c" : pct >= 30 ? "#f39c12" : "#27ae60";
        String bgAccent    = pct >= 60 ? "#fdecea" : pct >= 30 ? "#fff8e1" : "#e8f5e9";

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + "-fx-border-color: " + bgAccent + "; -fx-border-radius: 12; -fx-border-width: 1.5;"
                + "-fx-padding: 16; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        // ── Top row: version badge + date + percentage bar ─────────────
        HBox topRow = new HBox(10);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label vBadge = new Label("v" + v.getVersionNumber());
        vBadge.setStyle("-fx-background-color: #1A1A2E; -fx-text-fill: white; -fx-font-size: 12px;"
                + "-fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 20;");

        Label dateLbl = new Label(
                v.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm")));
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #9E9E9E;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Percentage pill
        Label pctLbl = new Label(
                String.format("%.1f%% modifié", pct));
        pctLbl.setStyle("-fx-background-color: " + bgAccent + "; -fx-text-fill: " + accentColor + ";"
                + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 20;");

        topRow.getChildren().addAll(vBadge, dateLbl, spacer, pctLbl);

        // ── Progress bar ───────────────────────────────────────────────
        ProgressBar bar = new ProgressBar(pct / 100.0);
        bar.setPrefWidth(Double.MAX_VALUE);
        bar.setPrefHeight(6);
        bar.setStyle("-fx-accent: " + accentColor + "; -fx-background-color: #eeeeee; -fx-background-radius: 4;");

        // ── Summary line ──────────────────────────────────────────────
        Label summaryLbl = new Label(
                "📝 " + (v.getChangeDescription() != null ? v.getChangeDescription() : "—"));
        summaryLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-wrap-text: true;");
        summaryLbl.setMaxWidth(Double.MAX_VALUE);

        // ── Field-level changes from JSON ─────────────────────────────
        VBox changesBox = new VBox(6);
        if (v.getChangesDetected() != null && !v.getChangesDetected().equals("[]")) {
            parseAndRenderChanges(v.getChangesDetected(), changesBox, accentColor, bgAccent);
        }

        // ── Modified by ───────────────────────────────────────────────
        Label byLbl = new Label(
                "👤 " + (v.getModifiedBy() != null ? v.getModifiedBy() : "Inconnu"));
        byLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #BDBDBD;");

        card.getChildren().addAll(topRow, bar, summaryLbl);
        if (!changesBox.getChildren().isEmpty()) card.getChildren().add(changesBox);
        card.getChildren().add(byLbl);
        return card;
    }

    private void parseAndRenderChanges(String json, VBox box,
                                       String accentColor, String bgAccent) {
        // Simple JSON array parser — no external lib needed
        // Format: [{"field":"x","old":"a","new":"b","note":"n"}, ...]
        String[] entries = json.replaceAll("^\\[|\\]$", "").split("\\},\\{");
        for (String entry : entries) {
            entry = entry.replaceAll("[\\[\\]{}]", "");
            String field = extractJsonValue(entry, "field");
            String oldV  = extractJsonValue(entry, "old");
            String newV  = extractJsonValue(entry, "new");
            String note  = extractJsonValue(entry, "note");
            if (field.isEmpty()) continue;

            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: " + bgAccent + "; -fx-background-radius: 8; -fx-padding: 8 12;");

            Label fieldLbl = new Label(fieldLabel(field));
            fieldLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";"
                    + "-fx-min-width: 80;");

            Label arrow = new Label("→");
            arrow.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 12px;");

            String oldDisplay = oldV.length() > 35 ? oldV.substring(0, 35) + "…" : oldV;
            String newDisplay = newV.length() > 35 ? newV.substring(0, 35) + "…" : newV;

            Label oldLbl = new Label(oldDisplay.isEmpty() ? "—" : oldDisplay);
            oldLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575; -fx-strikethrough: true;");

            Label newLbl = new Label(newDisplay.isEmpty() ? "—" : newDisplay);
            newLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #333333; -fx-font-weight: bold;");

            javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
            HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);

            Label noteLbl = new Label(note);
            noteLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #BDBDBD;");

            row.getChildren().addAll(fieldLbl, oldLbl, arrow, newLbl, sp, noteLbl);
            box.getChildren().add(row);
        }
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end)
                .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "titre"          -> "Titre";
            case "contenu"        -> "Contenu";
            case "ordre"          -> "Ordre";
            case "type_contenu"   -> "Type";
            case "video_url"      -> "Vidéo";
            case "image_url"      -> "Image";
            case "fichier"        -> "Fichier";
            case "duree_minutes"  -> "Durée";
            default               -> field;
        };
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