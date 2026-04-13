package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.models.Course;
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

    private ObservableList<Course> courseList   = FXCollections.observableArrayList();
    private FilteredList<Course>   filteredList;
    private CoursService           coursService;
    private DashboardController    dashboardController;

    // ════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        System.out.println("CoursController initialisé");
        coursService = new CoursService();
        setupTableColumns();
        setupActionsColumn();
        setupRowClick();       // ← new
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
            private final Button updateBtn  = new Button("✏️ Modifier");
            private final Button deleteBtn  = new Button("🗑️ Supprimer");
            private final Button chapBtn    = new Button("📖 Chapitres");
            private final HBox   buttons    = new HBox(5, chapBtn, updateBtn, deleteBtn);

            {
                chapBtn.setStyle("-fx-background-color: #2979FF; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                updateBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 6;");

                chapBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    navigateToChapitres(course.getEntity());
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
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : buttons);
            }
        });
    }

    /** Double-click on a row also navigates to its chapters. */
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

    // ── Navigation to chapters ───────────────────────────────────────────────
    private void navigateToChapitres(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chapitres.fxml"));
            Parent view = loader.load();

            ChapitreController ctrl = loader.getController();
            ctrl.setDashboardController(dashboardController);
            ctrl.setCours(cours);   // triggers data load inside ChapitreController

            // Replace content in the dashboard's StackPane
            if (dashboardController != null) {
                // Reach the contentArea via the dashboard
                StackPane contentArea = (StackPane) coursesTable.getScene().lookup("#contentArea");
                if (contentArea != null) {
                    contentArea.getChildren().setAll(view);
                    return;
                }
            }
            // Fallback: open in a new window
            Stage stage = new Stage();
            stage.setTitle("Chapitres – " + cours.getTitre());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesTable.getScene().getWindow());
            stage.setScene(new Scene(view, 900, 620));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les chapitres: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────
    private void setupSearchFilter() {
        filteredList = new FilteredList<>(courseList, p -> true);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredList.setPredicate(course -> {
                if (newV == null || newV.isEmpty()) return true;
                String lf = newV.toLowerCase();
                return course.getTitre().toLowerCase().contains(lf) ||
                        course.getDescription().toLowerCase().contains(lf);
            });
        });
        coursesTable.setItems(filteredList);
    }

    // ── Data ─────────────────────────────────────────────────────────────────
    private void loadCoursesFromDatabase() {
        try {
            List<Cours> courses = coursService.findAll();
            courseList.clear();
            if (courses != null && !courses.isEmpty()) {
                for (Cours c : courses) courseList.add(new Course(c));
                statusLabel.setText(courseList.size() + " cours chargés  (double-clic pour voir les chapitres)");
            } else {
                statusLabel.setText("Aucun cours disponible");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Erreur: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les cours: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
    @FXML private void handleAddCourse()  { openCourseForm(null); }
    @FXML private void handleSearch()     { statusLabel.setText("Recherche: " + (searchField.getText().isEmpty() ? "tous" : searchField.getText())); }
    @FXML private void handleShowAll()    { searchField.clear(); statusLabel.setText("Tous les cours"); }

    private void handleUpdateCourse(Course course) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Modification");
        confirm.setContentText("Modifier le cours « " + course.getTitre() + " » ?");
        confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) openCourseForm(course.getEntity()); });
    }

    private void handleDeleteCourse(Course course) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setContentText("Supprimer « " + course.getTitre() + " » ?\n\n⚠️ Les chapitres associés seront aussi supprimés.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    coursService.delete(course.getEntity().getId());
                    courseList.remove(course);
                    statusLabel.setText("Cours supprimé");
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage(), Alert.AlertType.ERROR);
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
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void refreshCourses() { loadCoursesFromDatabase(); }

    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
}