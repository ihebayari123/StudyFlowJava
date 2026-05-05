package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.models.Course;
import edu.connexion3a36.services.AnthropicAIService;
import edu.connexion3a36.services.ChapitreService;
import edu.connexion3a36.services.CoursService;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class CoursController {

    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, Long>   idColumn;
    @FXML private TableColumn<Course, String> titleColumn;
    @FXML private TableColumn<Course, String> descriptionColumn;
    @FXML private TableColumn<Course, String> imageColumn;
    @FXML private TableColumn<Course, Void>   actionsColumn;
    @FXML private TextField searchField;
    @FXML private Label     statusLabel;
    @FXML private Button    addCourseBtn;

    // ── NEW: AI Outline button (add this in cours.fxml toolbar) ──────────────
    @FXML private Button    aiOutlineBtn;

    private ObservableList<Course> courseList   = FXCollections.observableArrayList();
    private FilteredList<Course>   filteredList;
    private CoursService           coursService;
    private ChapitreService        chapitreService;
    private DashboardController    dashboardController;

    @FXML
    public void initialize() {
        coursService    = new CoursService();
        chapitreService = new ChapitreService();
        setupTableColumns();
        setupActionsColumn();
        setupRowClick();
        setupSearchFilter();
        loadCoursesFromDatabase();
    }

    // ── Table setup ──────────────────────────────────────────────────────────

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<Course, Void>() {
            private final Button updateBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final Button chapBtn   = new Button("📖 Chapitres");
            private final Button outlineBtn= new Button("✨ Plan IA");
            private final HBox   buttons   = new HBox(5, chapBtn, outlineBtn, updateBtn, deleteBtn);

            {
                chapBtn.setStyle("-fx-background-color: #2979FF; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                outlineBtn.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                updateBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");

                chapBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    navigateToChapitres(course.getEntity());
                });
                outlineBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    openAIOutlineForCourse(course.getEntity());
                });
                updateBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    handleUpdateCourse(course);
                });
                deleteBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    handleDeleteCourse(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null
                        ? null : buttons);
            }
        });
    }

    private void setupRowClick() {
        coursesTable.setRowFactory(tv -> {
            TableRow<Course> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    navigateToChapitres(row.getItem().getEntity());
                }
            });
            return row;
        });
    }

    // ── Feature 2: AI Outline Generator ──────────────────────────────────────
    // Toolbar button handler (wire onAction="#handleAIOutline" in cours.fxml)
    @FXML
    private void handleAIOutline() {
        // If a course is selected, generate chapters for it; otherwise ask user to select first
        Course selected = coursesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise",
                    "Sélectionnez un cours dans la liste, puis cliquez sur ✨ Plan IA " +
                            "pour générer des chapitres avec l'IA.", Alert.AlertType.INFORMATION);
            return;
        }
        openAIOutlineForCourse(selected.getEntity());
    }

    /**
     * Opens the AI outline dialog for a specific course.
     * When the teacher confirms their selection, each chosen chapter title
     * is pre-filled into the AddChapitreController dialog.
     */
    private void openAIOutlineForCourse(Cours cours) {
        CourseOutlineDialog.show(
                coursesTable.getScene().getWindow(),
                selectedSuggestions -> {
                    // Open an AddChapitreController dialog pre-filled for each selected chapter
                    for (int i = 0; i < selectedSuggestions.size(); i++) {
                        AnthropicAIService.ChapterSuggestion suggestion = selectedSuggestions.get(i);
                        openChapitreFormWithSuggestion(cours, suggestion, i);
                    }
                }
        );
    }

    /**
     * Opens AddChapitre form pre-filled from an AI suggestion.
     */
    private void openChapitreFormWithSuggestion(Cours cours,
                                                AnthropicAIService.ChapterSuggestion suggestion,
                                                int offset) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add_chapitre.fxml"));
            VBox root = loader.load();

            ChapitreService localChapitreService = new ChapitreService();
            AddChapitreController ctrl = loader.getController();
            ctrl.setChapitreService(localChapitreService);
            ctrl.setCurrentCours(cours);

            // Pre-fill fields from AI suggestion WITHOUT triggering edit mode
            // (prefillForNew keeps chapitreToUpdate = null so handleSave does INSERT not UPDATE)
            int nextOrdre = localChapitreService.getNextOrdre(cours.getId()) + offset;
            ctrl.prefillForNew(suggestion.getTitre(), suggestion.getDescription(), nextOrdre);

            Stage stage = new Stage();
            stage.setTitle("Ajouter chapitre (suggestion IA) : " + suggestion.getTitre());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesTable.getScene().getWindow());
            stage.setScene(new Scene(root, 580, 680));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ── Navigation to chapters ────────────────────────────────────────────────

    private void navigateToChapitres(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chapitres.fxml"));
            Parent view = loader.load();

            ChapitreController ctrl = loader.getController();
            ctrl.setDashboardController(dashboardController);
            ctrl.setCours(cours);

            if (dashboardController != null) {
                StackPane contentArea = (StackPane) coursesTable.getScene().lookup("#contentArea");
                if (contentArea != null) {
                    contentArea.getChildren().setAll(view);
                    return;
                }
            }

            Stage stage = new Stage();
            stage.setTitle("Chapitres – " + cours.getTitre());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesTable.getScene().getWindow());
            stage.setScene(new Scene(view, 900, 620));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les chapitres : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearchFilter() {
        filteredList = new FilteredList<>(courseList, p -> true);
        searchField.textProperty().addListener((obs, oldV, newV) ->
                filteredList.setPredicate(course -> {
                    if (newV == null || newV.isEmpty()) return true;
                    String lf = newV.toLowerCase();
                    return course.getTitre().toLowerCase().contains(lf)
                            || course.getDescription().toLowerCase().contains(lf);
                })
        );
        coursesTable.setItems(filteredList);
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadCoursesFromDatabase() {
        try {
            List<Cours> courses = coursService.findAll();
            courseList.clear();
            if (courses != null && !courses.isEmpty()) {
                for (Cours c : courses) courseList.add(new Course(c));
                statusLabel.setText(courseList.size()
                        + " cours chargés  (double-clic pour voir les chapitres)");
            } else {
                statusLabel.setText("Aucun cours disponible");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement: " + e.getMessage());
            statusLabel.setText("Erreur: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les cours: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @FXML private void handleAddCourse() { openCourseForm(null); }
    @FXML private void handleSearch()    { statusLabel.setText("Recherche: " +
            (searchField.getText().isEmpty() ? "tous" : searchField.getText())); }
    @FXML private void handleShowAll()   { searchField.clear(); statusLabel.setText("Tous les cours"); }

    private void handleUpdateCourse(Course course) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Modification");
        confirm.setContentText("Modifier le cours « " + course.getTitre() + " » ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) openCourseForm(course.getEntity());
        });
    }

    private void handleDeleteCourse(Course course) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setContentText("Supprimer « " + course.getTitre()
                + " » ?\n\n⚠️ Les chapitres associés seront aussi supprimés.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    coursService.delete(course.getEntity().getId());
                    courseList.remove(course);
                    statusLabel.setText("Cours supprimé");
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage(),
                            Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void openCourseForm(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add_course.fxml"));
            VBox root = loader.load();
            AddCourseController ctrl = loader.getController();
            ctrl.setCoursService(coursService);
            ctrl.setParentController(this);
            if (cours != null) ctrl.setCourseToUpdate(cours);

            Stage stage = new Stage();
            stage.setTitle(cours == null ? "Ajouter un cours" : "Modifier un cours");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesTable.getScene().getWindow());
            stage.setScene(new Scene(root, 520, 600));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    public void refreshCourses()                         { loadCoursesFromDatabase(); }
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
}