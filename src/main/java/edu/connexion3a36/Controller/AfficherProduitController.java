package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Produit;
import edu.connexion3a36.services.ProduitService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class AfficherProduitController {

    @FXML private TableView<Produit> tableView;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colDescription;
    @FXML private TableColumn<Produit, Integer> colPrix;
    @FXML private TableColumn<Produit, String> colImage;
    @FXML private TableColumn<Produit, Integer> colTypeCategorie;
    @FXML private TableColumn<Produit, Integer> colUser;
    @FXML private TableColumn<Produit, Void> colActions;
    @FXML private Button refreshBtn;
    @FXML private Button triBtn;         // ✅ bouton tri
    @FXML private TextField searchField; // ✅ champ recherche
    @FXML private Label statusLabel;

    private ProduitService service = new ProduitService();
    private ObservableList<Produit> masterData = FXCollections.observableArrayList();
    private boolean triAscendant = true; // ✅ état du tri

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colTypeCategorie.setCellValueFactory(new PropertyValueFactory<>("typeCategorieId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        setupActionsColumn();
        loadData();
        setupSearch();  // ✅
        setupTri();     // ✅
        refreshBtn.setOnAction(e -> loadData());
    }

    private void loadData() {
        try {
            List<Produit> data = service.getData();
            masterData.setAll(data);
            statusLabel.setText(data.size() + " produit(s) trouvé(s)");
        } catch (SQLException e) {
            statusLabel.setText("❌ Erreur : " + e.getMessage());
        }
    }

    // ✅ Recherche en temps réel par nom
    private void setupSearch() {
        FilteredList<Produit> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(produit -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String filtre = newValue.toLowerCase();
                return produit.getNom().toLowerCase().contains(filtre);
            });
            statusLabel.setText(filteredData.size() + " produit(s) trouvé(s)");
        });

        SortedList<Produit> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);
    }

    // ✅ Tri par nom A→Z / Z→A
    private void setupTri() {
        triBtn.setOnAction(e -> {
            if (triAscendant) {
                masterData.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
                triBtn.setText("🔽 Tri Z→A");
            } else {
                masterData.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
                triBtn.setText("🔼 Tri A→Z");
            }
            triAscendant = !triAscendant;
        });
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑️ Supprimer");
            private final Button btnUpdate = new Button("✏️ Modifier");
            private final HBox box = new HBox(8, btnUpdate, btnDelete);

            {
                btnDelete.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btnUpdate.setStyle("-fx-background-color: #2979FF; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");

                btnDelete.setOnAction(e -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Supprimer \"" + p.getNom() + "\" ?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.YES) {
                            try {
                                service.deleteP(p);
                                loadData();
                                statusLabel.setText("✅ Produit supprimé !");
                            } catch (SQLException ex) {
                                statusLabel.setText("❌ Erreur : " + ex.getMessage());
                            }
                        }
                    });
                });

                btnUpdate.setOnAction(e -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    openUpdateDialog(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void openUpdateDialog(Produit p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierProduit.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Produit");
            stage.setScene(new Scene(loader.load()));
            ModifierProduitController ctrl = loader.getController();
            ctrl.setProduit(p);
            ctrl.setOnSuccess(() -> loadData());
            stage.showAndWait();
        } catch (Exception ex) {
            statusLabel.setText("❌ Erreur : " + ex.getMessage());
        }
    }
}