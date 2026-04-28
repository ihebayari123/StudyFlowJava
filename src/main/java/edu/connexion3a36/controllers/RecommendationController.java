package edu.connexion3a36.controllers;

import edu.connexion3a36.models.Event;
import edu.connexion3a36.services.EventService;
import edu.connexion3a36.services.RecommendationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class RecommendationController {

    @FXML private TextArea  resultArea;
    @FXML private Button    btnRecommander;
    @FXML private Label     statusLabel;

    private final EventService          eventService          = new EventService();
    private final RecommendationService recommendationService = new RecommendationService();

    @FXML
    private void handleRecommander() {
        btnRecommander.setDisable(true);
        statusLabel.setText("⏳ Analyse en cours...");
        resultArea.clear();

        new Thread(() -> {
            try {
                List<Event> events = eventService.recupererTous();

                if (events.isEmpty()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("⚠️ Aucun événement en base.");
                        btnRecommander.setDisable(false);
                    });
                    return;
                }

                // Pass a status callback to show progress
                String resultat = recommendationService.recommander(events, (status) -> {
                    Platform.runLater(() -> statusLabel.setText(status));
                });

                Platform.runLater(() -> {
                    resultArea.setText(resultat);
                    statusLabel.setText("✅ Recommandation générée !");
                    btnRecommander.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ " + e.getMessage());
                    btnRecommander.setDisable(false);
                });
            }
        }).start();
    }
}