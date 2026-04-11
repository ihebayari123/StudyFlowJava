package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.models.Course;
import edu.connexion3a36.services.CoursService;
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

public class CoursController {

    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, Long> idColumn;
    @FXML private TableColumn<Course, String> titleColumn;
    @FXML private TableColumn<Course, String> descriptionColumn;
    @FXML private TableColumn<Course, String> imageColumn;
    @FXML private TableColumn<Course, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    @FXML private Button addCourseBtn;

    private ObservableList<Course> courseList = FXCollections.observableArrayList();
    private FilteredList<Course> filteredList;
    private CoursService coursService;
    private DashboardController dashboardController;

    @FXML
    public void initialize() {
        System.out.println("CoursController initialisé");
        coursService = new CoursService();
        setupTableColumns();
        setupActionsColumn();
        setupSearchFilter();
        loadCoursesFromDatabase();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<Course, Void>() {
            private final Button updateBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox buttons = new HBox(5, updateBtn, deleteBtn);

            {
                updateBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");

                updateBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    handleUpdateCourse(course);
                });

                deleteBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    handleDeleteCourse(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }

    private void setupSearchFilter() {
        filteredList = new FilteredList<>(courseList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(course -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return course.getTitre().toLowerCase().contains(lowerCaseFilter) ||
                        course.getDescription().toLowerCase().contains(lowerCaseFilter);
            });
        });
        coursesTable.setItems(filteredList);
    }

    private void loadCoursesFromDatabase() {
        try {
            List<Cours> courses = coursService.findAll();
            courseList.clear();
            if (courses != null && !courses.isEmpty()) {
                for (Cours cours : courses) {
                    courseList.add(new Course(cours));
                }
                statusLabel.setText(courseList.size() + " cours chargés");
                System.out.println("Chargé " + courseList.size() + " cours depuis la base");
            } else {
                statusLabel.setText("Aucun cours disponible");
                System.out.println("Aucun cours trouvé");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Erreur: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les cours: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAddCourse() {
        openCourseForm(null);
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText();
        statusLabel.setText("Recherche: " + (searchText.isEmpty() ? "tous les cours" : searchText));
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        statusLabel.setText("Tous les cours affichés");
    }

    private void handleUpdateCourse(Course course) {
        System.out.println("Modification du cours: " + course.getTitre());
        openCourseForm(course.getEntity());
    }

    private void handleDeleteCourse(Course course) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le cours");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer le cours \"" + course.getTitre() + "\" ?\n\n" +
                "⚠️ Attention: Tous les chapitres et quiz associés seront également supprimés !");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    coursService.delete(course.getEntity().getId());
                    courseList.remove(course);
                    statusLabel.setText("Cours supprimé avec succès");
                    System.out.println("Cours supprimé: " + course.getTitre());
                } catch (Exception e) {
                    System.err.println("Erreur suppression: " + e.getMessage());
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void openCourseForm(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add_course.fxml"));
            VBox root = loader.load();

            AddCourseController controller = loader.getController();
            controller.setCoursService(coursService);
            controller.setParentController(this);

            if (cours != null) {
                controller.setCourseToUpdate(cours);
            }

            Stage stage = new Stage();
            stage.setTitle(cours == null ? "Ajouter un cours" : "Modifier un cours");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void refreshCourses() {
        loadCoursesFromDatabase();
    }

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}