package edu.connexion3a36.Controller;

import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.AnomalyResult;
import edu.connexion3a36.utils.AnomalyService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;

public class AnomalyDashboardController {

    @FXML private TableView<AnomalyResult>          tableAnomalies;
    @FXML private TableColumn<AnomalyResult, String> colNom;
    @FXML private TableColumn<AnomalyResult, String> colPrenom;
    @FXML private TableColumn<AnomalyResult, String> colEmail;
    @FXML private TableColumn<AnomalyResult, String> colRole;
    @FXML private TableColumn<AnomalyResult, String> colStatut;
    @FXML private TableColumn<AnomalyResult, Double> colScore;
    @FXML private TableColumn<AnomalyResult, String> colNiveau;
    @FXML private TableColumn<AnomalyResult, String> colRaisons;
    @FXML private TableColumn<AnomalyResult, Void>   colAction;

    @FXML private Label countHigh;
    @FXML private Label countMedium;
    @FXML private Label countLow;
    @FXML private Label countTotal;
    @FXML private Label statusLabel;
    @FXML private Button btnAnalyser;
    @FXML private Button btnBloquerAuto;

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private ObservableList<AnomalyResult> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        tableAnomalies.setItems(data);
    }

    private void setupColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colRaisons.setCellValueFactory(new PropertyValueFactory<>("raisonsFormatees"));

        // Colonne Niveau avec couleur
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauEmoji"));
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("HIGH"))
                        setStyle("-fx-text-fill: #E53935; -fx-font-weight: bold;");
                    else if (item.contains("MEDIUM"))
                        setStyle("-fx-text-fill: #F57F17; -fx-font-weight: bold;");
                    else
                        setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                }
            }
        });

        // Colonne Score avec couleur
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    if (item >= 0.55)
                        setStyle("-fx-text-fill: #E53935; -fx-font-weight: bold;");
                    else if (item >= 0.30)
                        setStyle("-fx-text-fill: #F57F17; -fx-font-weight: bold;");
                    else
                        setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                }
            }
        });

        // Colonne Action — bouton Bloquer
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("🔒 Bloquer");
            {
                btn.setStyle(
                        "-fx-background-color: #E53935; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-font-size: 11;" +
                                "-fx-cursor: hand;"
                );
                btn.setOnAction(e -> {
                    AnomalyResult r = getTableView().getItems().get(getIndex());
                    bloquerUtilisateur(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AnomalyResult r = getTableView().getItems().get(getIndex());
                    if ("BLOQUE".equals(r.getStatut())) {
                        Button btnD = new Button("✅ Débloquer");
                        btnD.setStyle(
                                "-fx-background-color: #2E7D32; -fx-text-fill: white;" +
                                        "-fx-background-radius: 6; -fx-font-size: 11;" +
                                        "-fx-cursor: hand;"
                        );
                        btnD.setOnAction(e -> bloquerUtilisateur(r));
                        setGraphic(btnD);
                    } else {
                        setGraphic(btn);
                    }
                }
            }
        });
    }

    @FXML
    void lancerAnalyse() {
        btnAnalyser.setDisable(true);
        statusLabel.setText("⏳ Analyse en cours...");
        statusLabel.setStyle("-fx-text-fill: #2979FF;");
        data.clear();

        // Lancer dans un thread séparé pour ne pas bloquer l'UI
        Executors.newSingleThreadExecutor().submit(() -> {
            List<AnomalyResult> results = AnomalyService.detectAnomalies();
            Platform.runLater(() -> {
                data.setAll(results);
                updateStats(results);
                btnAnalyser.setDisable(false);
                statusLabel.setText("✅ Analyse terminée — " +
                        results.size() + " utilisateurs analysés");
                statusLabel.setStyle("-fx-text-fill: #2E7D32;");
            });
        });
    }

    @FXML
    void bloquerComptesHigh() {
        long count = data.stream()
                .filter(r -> "HIGH".equals(r.getNiveau()))
                .count();

        if (count == 0) {
            statusLabel.setText("ℹ️ Aucun compte HIGH à bloquer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bloquer les comptes HIGH");
        confirm.setHeaderText("⚠️ Confirmer le blocage");
        confirm.setContentText("Voulez-vous bloquer les " + count +
                " compte(s) à risque HIGH ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                data.stream()
                        .filter(r -> "HIGH".equals(r.getNiveau())
                                && !"BLOQUE".equals(r.getStatut()))
                        .forEach(r -> bloquerUtilisateur(r));
                statusLabel.setText("🔒 " + count + " compte(s) HIGH bloqué(s).");
            }
        });
    }

    private void bloquerUtilisateur(AnomalyResult r) {
        try {
            utilisateurService.bloquerDebloquerParId(r.getId());
            // Mettre à jour le statut localement
            String nouveau = "BLOQUE".equals(r.getStatut()) ? "ACTIF" : "BLOQUE";
            r.setStatut(nouveau);
            tableAnomalies.refresh();
            updateStats(data);
        } catch (SQLException e) {
            statusLabel.setText("❌ Erreur : " + e.getMessage());
        }
    }

    private void updateStats(List<AnomalyResult> results) {
        long high   = results.stream().filter(r -> "HIGH".equals(r.getNiveau())).count();
        long medium = results.stream().filter(r -> "MEDIUM".equals(r.getNiveau())).count();
        long low    = results.stream().filter(r -> "LOW".equals(r.getNiveau())).count();

        countHigh.setText(String.valueOf(high));
        countMedium.setText(String.valueOf(medium));
        countLow.setText(String.valueOf(low));
        countTotal.setText(String.valueOf(results.size()));
    }
}