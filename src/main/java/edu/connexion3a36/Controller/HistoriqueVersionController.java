package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.ChapitreVersion;
import edu.connexion3a36.services.ChapitreVersionService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoriqueVersionController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox  versionsContainer;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void setChapitre(Chapitre chapitre) {
        titleLabel.setText(chapitre.getTitre());
        ChapitreVersionService svc = new ChapitreVersionService();
        List<ChapitreVersion> versions = svc.findByChapitreId(chapitre.getId());
        subtitleLabel.setText(chapitre.getTitre() + " · " + versions.size() + " version(s)");
        versionsContainer.getChildren().clear();

        if (versions.isEmpty()) {
            Label empty = new Label("Aucune version disponible.\nModifiez ce chapitre pour créer la première version.");
            empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px; -fx-text-alignment: center;");
            empty.setAlignment(Pos.CENTER);
            versionsContainer.getChildren().add(empty);
        } else {
            for (ChapitreVersion v : versions) {
                versionsContainer.getChildren().add(buildVersionCard(v));
            }
        }
    }

    private VBox buildVersionCard(ChapitreVersion v) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 12;
            -fx-border-color: #e0e0e0;
            -fx-border-radius: 12;
            -fx-border-width: 1;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);
            """);

        // ── Top row: version badge + date ────────────────────────────────
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("v" + v.getVersionNumber());
        badge.setStyle("""
            -fx-background-color: #2979FF;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-font-size: 12px;
            -fx-padding: 3 10;
            -fx-background-radius: 20;
            """);

        Label dateLabel = new Label(v.getCreatedAt() != null ? v.getCreatedAt().format(FMT) : "—");
        dateLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pct = new Label(v.getModificationPercentage() + "% modifié");
        pct.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 11px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(badge, dateLabel, spacer, pct);

        // ── Change description ────────────────────────────────────────────
        Label desc = new Label(v.getChangeDescription() != null ? v.getChangeDescription() : "—");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-wrap-text: true;");
        desc.setMaxWidth(Double.MAX_VALUE);

        // ── Titre snapshot ────────────────────────────────────────────────
        Label titreSnap = new Label("Titre : " + v.getTitre());
        titreSnap.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        // ── Modified by ───────────────────────────────────────────────────
        Label by = new Label("Par : " + (v.getModifiedBy() != null ? v.getModifiedBy() : "—"));
        by.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");

        card.getChildren().addAll(topRow, desc, titreSnap, by);
        return card;
    }

    @FXML
    private void handleClose() {
        ((Stage) versionsContainer.getScene().getWindow()).close();
    }
}