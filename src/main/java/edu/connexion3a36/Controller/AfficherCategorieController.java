package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.TypeCategorieService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AfficherCategorieController {

    @FXML private TableView<TypeCategorie> tableView;
    @FXML private TableColumn<TypeCategorie, Integer> colId;
    @FXML private TableColumn<TypeCategorie, String> colNom;
    @FXML private TableColumn<TypeCategorie, String> colDescription;
    @FXML private TableColumn<TypeCategorie, Void> colActions;
    @FXML private Button refreshBtn;
    @FXML private Label statusLabel;

    // 🔍 + 🔽
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    private TypeCategorieService service = new TypeCategorieService();
    private FilteredList<TypeCategorie> filteredData;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomCategorie"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        setupActionsColumn();
        setupSearchAndSort();
        loadData();

        refreshBtn.setOnAction(e -> loadData());
    }

    private void loadData() {
        try {
            List<TypeCategorie> data = service.getData();

            filteredData = new FilteredList<>(
                    FXCollections.observableArrayList(data), p -> true
            );

            SortedList<TypeCategorie> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(tableView.comparatorProperty());

            tableView.setItems(sortedData);

            statusLabel.setText(data.size() + " catégorie(s)");

        } catch (SQLException e) {
            statusLabel.setText("❌ Erreur : " + e.getMessage());
        }
    }

    private void setupSearchAndSort() {

        //  RECHERCHE
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredData == null) return;

            filteredData.setPredicate(tc -> {
                if (newVal == null || newVal.isEmpty()) return true;

                String search = newVal.toLowerCase();

                return tc.getNomCategorie().toLowerCase().contains(search)
                        || tc.getDescription().toLowerCase().contains(search);
            });
        });

        // 🔽 TRI
        sortCombo.setItems(FXCollections.observableArrayList(
                "Nom A-Z",
                "Nom Z-A"
        ));

        sortCombo.setOnAction(e -> {
            String choice = sortCombo.getValue();

            if (choice == null) return;

            tableView.getSortOrder().clear();

            if (choice.equals("Nom A-Z")) {
                colNom.setSortType(TableColumn.SortType.ASCENDING);
                tableView.getSortOrder().add(colNom);
            } else if (choice.equals("Nom Z-A")) {
                colNom.setSortType(TableColumn.SortType.DESCENDING);
                tableView.getSortOrder().add(colNom);
            }
        });
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button btnDelete = new Button("🗑️");
            private final Button btnUpdate = new Button("✏️");
            private final HBox box = new HBox(8, btnUpdate, btnDelete);

            {
                btnDelete.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                btnUpdate.setStyle("-fx-background-color: #2979FF; -fx-text-fill: white;");

                btnDelete.setOnAction(e -> {
                    TypeCategorie tc = getTableView().getItems().get(getIndex());

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Supprimer \"" + tc.getNomCategorie() + "\" ?",
                            ButtonType.YES, ButtonType.NO);

                    alert.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.YES) {
                            try {
                                service.deleteCat(tc);
                                loadData();
                                statusLabel.setText("✅ Supprimé !");
                            } catch (SQLException ex) {
                                statusLabel.setText("❌ " + ex.getMessage());
                            }
                        }
                    });
                });

                btnUpdate.setOnAction(e -> {
                    TypeCategorie tc = getTableView().getItems().get(getIndex());
                    openUpdateDialog(tc);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void openUpdateDialog(TypeCategorie tc) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierCategorie.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Catégorie");
            stage.setScene(new Scene(loader.load()));

            ModifierCategorieController ctrl = loader.getController();
            ctrl.setTypeCategorie(tc);
            ctrl.setOnSuccess(this::loadData);

            stage.showAndWait();

        } catch (Exception ex) {
            statusLabel.setText("❌ " + ex.getMessage());
        }
    }
}