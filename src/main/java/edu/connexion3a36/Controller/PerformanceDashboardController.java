package edu.connexion3a36.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PerformanceDashboardController {

    // ── Métriques ──────────────────────────────────────────────
    @FXML private Label totalProduitsLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label totalPredictionsLabel;
    @FXML private Label maeLabel;
    @FXML private Label maeStatusLabel;
    @FXML private Label produitStatusLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label noDataLabel;

    // ── Graphiques ─────────────────────────────────────────────
    @FXML private ScatterChart<Number, Number> scatterChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis barYAxis;

    // ── Tableau ────────────────────────────────────────────────
    @FXML private VBox logContainer;
    @FXML private Button refreshBtn;

    @FXML
    public void initialize() {
        // Style des graphiques
        scatterChart.setAnimated(true);
        barChart.setAnimated(true);
        scatterChart.setStyle("-fx-background-color: transparent;");
        barChart.setStyle("-fx-background-color: transparent;");

        refreshBtn.setOnAction(e -> chargerStats());
        chargerStats();
    }

    private void chargerStats() {
        lastUpdateLabel.setText("⏳ Chargement...");
        refreshBtn.setDisable(true);

        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:5000/stats");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8")
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json     = new JSONObject(sb.toString());
                int totalProduits   = json.getInt("total_produits");
                int totalCategories = json.getInt("total_categories");
                int totalPredictions= json.getInt("total_predictions");
                double mae          = json.getDouble("mae");
                JSONArray derniers  = json.getJSONArray("derniers");
                JSONArray parCat    = json.getJSONArray("par_categorie");

                Platform.runLater(() -> {
                    afficherMetriques(totalProduits, totalCategories, totalPredictions, mae);
                    afficherBarChart(parCat);
                    afficherScatterChart(derniers);
                    afficherTableau(derniers);

                    String now = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    lastUpdateLabel.setText("Dernière mise à jour : " + now);
                    refreshBtn.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lastUpdateLabel.setText("❌ Flask non lancé — python app.py");
                    refreshBtn.setDisable(false);
                });
            }
        }).start();
    }

    // ── Métriques ──────────────────────────────────────────────
    private void afficherMetriques(int produits, int categories, int predictions, double mae) {
        totalProduitsLabel.setText(String.valueOf(produits));
        totalCategoriesLabel.setText(String.valueOf(categories));
        totalPredictionsLabel.setText(String.valueOf(predictions));

        // Statut produits
        if (produits < 10) {
            produitStatusLabel.setText("⚠ Ajoutez plus de produits");
            produitStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #F57C00;");
        } else if (produits < 50) {
            produitStatusLabel.setText("✅ Données correctes");
            produitStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #4CAF50;");
        } else {
            produitStatusLabel.setText("🎯 Très bonnes données");
            produitStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #2979FF;");
        }

        // MAE
        if (mae == 0) {
            maeLabel.setText("N/A");
            maeStatusLabel.setText("Faites des prédictions d'abord");
        } else {
            maeLabel.setText((int) mae + " DT");
            if (mae < 50) {
                maeStatusLabel.setText("🎯 Très bonne précision !");
                maeStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #4CAF50;");
            } else if (mae < 150) {
                maeStatusLabel.setText("⚠ Précision moyenne");
                maeStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #F57C00;");
            } else {
                maeStatusLabel.setText("❌ Ajoutez plus de données");
                maeStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #F44336;");
            }
        }
    }

    // ── BarChart catégories ────────────────────────────────────
    private void afficherBarChart(JSONArray parCat) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Produits");

        for (int i = 0; i < parCat.length(); i++) {
            JSONObject c  = parCat.getJSONObject(i);
            String nom    = c.getString("nom_categorie");
            int total     = c.getInt("total");
            series.getData().add(new XYChart.Data<>(nom, total));
        }
        barChart.getData().add(series);

        // Couleur des barres
        barChart.lookupAll(".default-color0.chart-bar").forEach(node ->
                node.setStyle("-fx-bar-fill: #2979FF;")
        );
    }

    // ── ScatterChart prix prédit vs réel ──────────────────────
    private void afficherScatterChart(JSONArray derniers) {
        scatterChart.getData().clear();

        if (derniers.length() == 0) return;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Prédictions");

        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;

        for (int i = 0; i < derniers.length(); i++) {
            JSONObject d = derniers.getJSONObject(i);
            int prixReel   = d.getInt("prix_reel");
            int prixPredit = d.getInt("prix_predit");
            if (prixReel > 0) {
                series.getData().add(new XYChart.Data<>(prixReel, prixPredit));
                minVal = Math.min(minVal, Math.min(prixReel, prixPredit));
                maxVal = Math.max(maxVal, Math.max(prixReel, prixPredit));
            }
        }

        // Ligne diagonale idéale
        XYChart.Series<Number, Number> diagonale = new XYChart.Series<>();
        diagonale.setName("Idéal");
        if (minVal != Double.MAX_VALUE) {
            diagonale.getData().add(new XYChart.Data<>(minVal, minVal));
            diagonale.getData().add(new XYChart.Data<>(maxVal, maxVal));
        }

        scatterChart.getData().addAll(series, diagonale);
    }

    // ── Tableau des prédictions ────────────────────────────────
    private void afficherTableau(JSONArray derniers) {
        logContainer.getChildren().clear();

        if (derniers.length() == 0) {
            noDataLabel.setVisible(true);
            return;
        }
        noDataLabel.setVisible(false);

        for (int i = 0; i < derniers.length(); i++) {
            JSONObject d   = derniers.getJSONObject(i);
            String nom     = d.getString("nom_produit");
            String cat     = d.optString("categorie", "—");
            int prixPredit = d.getInt("prix_predit");
            int prixReel   = d.getInt("prix_reel");
            int ecart      = d.getInt("ecart");
            String date    = d.getString("date_prediction").substring(0, 16);

            // Couleur selon écart
            String ecartColor = ecart >= 0 ? "#4CAF50" : "#F44336";
            String ecartStr   = (ecart >= 0 ? "+" : "") + ecart + " DT";

            // Fond alternant
            String bg = (i % 2 == 0) ? "#FAFAFA" : "#FFFFFF";

            HBox row = new HBox();
            row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 6; -fx-padding: 10 14;");

            Label lNom     = makeCell(nom, 200, "#1A1A2E", true);
            Label lCat     = makeCell(cat, 120, "#555", false);
            Label lPredit  = makeCell(prixPredit + " DT", 100, "#2979FF", false);
            Label lReel    = makeCell(prixReel + " DT", 100, "#555", false);
            Label lEcart   = makeCell(ecartStr, 90, ecartColor, true);
            Label lDate    = makeCell(date, 150, "#999", false);

            row.getChildren().addAll(lNom, lCat, lPredit, lReel, lEcart, lDate);
            logContainer.getChildren().add(row);
        }
    }

    private Label makeCell(String text, double minWidth, String color, boolean bold) {
        Label l = new Label(text);
        l.setMinWidth(minWidth);
        l.setStyle("-fx-font-size: 12; -fx-text-fill: " + color + ";"
                + (bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
