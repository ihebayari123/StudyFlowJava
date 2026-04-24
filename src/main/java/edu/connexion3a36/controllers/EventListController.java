package edu.connexion3a36.controllers;


import edu.connexion3a36.models.Event;
import edu.connexion3a36.models.Sponsor;
import edu.connexion3a36.services.EventService;
import edu.connexion3a36.services.SponsorService;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import java.sql.SQLException;

public class EventListController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;

    private List<Event> tousLesEvents;
    private final EventService   eventService   = new EventService();
    private final SponsorService sponsorService = new SponsorService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerEvents();
    }

    private void chargerEvents() {
        try {
            tousLesEvents = eventService.recupererTous();
            afficherCartes(tousLesEvents);
        } catch (SQLException e) {
            System.err.println("Erreur chargement events : " + e.getMessage());
        }
    }


    @FXML
    private void handleRecommandations() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/recommendation.fxml")            );
            Node page = loader.load();
            StackPane contentArea = (StackPane)
                    searchField.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(page);
        } catch (Exception e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }
    private void afficherCartes(List<Event> events) {
        cardsContainer.getChildren().clear();
        for (int i = 0; i < events.size(); i++) {
            VBox carte = creerCarte(events.get(i));
            carte.setOpacity(0);
            carte.setTranslateY(30);
            cardsContainer.getChildren().add(carte);

            FadeTransition fade = new FadeTransition(Duration.millis(400), carte);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * 80));

            TranslateTransition slide = new TranslateTransition(Duration.millis(400), carte);
            slide.setToY(0);
            slide.setDelay(Duration.millis(i * 80));

            fade.play();
            slide.play();
        }
    }

    private VBox creerCarte(Event event) {
        VBox carte = new VBox();
        carte.setPrefWidth(260);
        carte.setMaxWidth(260);
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 4);" +
                        "-fx-cursor: hand;"
        );

        // ── IMAGE
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        imageContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2979FF, #1565C0);" +
                        "-fx-background-radius: 16 16 0 0;"
        );

        File imgFile = new File("src/main/resources/images/" + event.getImage());
        if (imgFile.exists()) {
            ImageView iv = new ImageView(new Image(imgFile.toURI().toString()));
            iv.setFitWidth(260);
            iv.setFitHeight(160);
            iv.setPreserveRatio(false);
            Rectangle clip = new Rectangle(260, 160);
            clip.setArcWidth(32);
            clip.setArcHeight(32);
            iv.setClip(clip);
            imageContainer.getChildren().add(iv);
        } else {
            Label emoji = new Label("🎉");
            emoji.setStyle("-fx-font-size: 48px;");
            imageContainer.getChildren().add(emoji);
        }

        // Badge type
        Label badge = new Label(event.getType().toUpperCase());
        badge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.25);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 10;" +
                        "-fx-background-radius: 10;"
        );
        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new javafx.geometry.Insets(10, 10, 0, 0));
        imageContainer.getChildren().add(badge);

        // ── CONTENU
        VBox content = new VBox(8);
        content.setStyle("-fx-padding: 16 16 12 16;");

        Label titre = new Label(event.getTitre());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        titre.setWrapText(true);

        Label date = new Label("📅  " + (event.getDateCreation() != null ?
                event.getDateCreation().toLocalDate().toString() : ""));
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #2979FF;");

        Label desc = new Label(event.getDescription());
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-wrap-text: true;");
        desc.setWrapText(true);
        desc.setMaxHeight(50);

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #f0f0f0;");

        Label userId = new Label("👤  User #" + event.getUserId());
        userId.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");

        content.getChildren().addAll(titre, date, desc, sep, userId);

        // ── SECTION SPONSORS
        VBox sponsorSection = creerSectionSponsors(event.getId());
        if (sponsorSection != null) {
            content.getChildren().add(sponsorSection);
        }

        carte.getChildren().addAll(imageContainer, content);

        // ── HOVER EFFECT
        carte.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), carte);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
            carte.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 16;" +
                            "-fx-effect: dropshadow(gaussian, rgba(41,121,255,0.25), 20, 0, 0, 8);" +
                            "-fx-cursor: hand;"
            );
        });

        carte.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), carte);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            carte.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 16;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 4);" +
                            "-fx-cursor: hand;"
            );
        });

        return carte;
    }

    // ── CRÉE LA SECTION SPONSORS D'UN ÉVÉNEMENT
    private VBox creerSectionSponsors(int eventId) {
        List<Sponsor> sponsors;
        try {
            sponsors = sponsorService.recupererParEvent(eventId);
        } catch (SQLException e) {
            System.err.println("Erreur chargement sponsors event " + eventId + " : " + e.getMessage());
            return null;
        }

        VBox section = new VBox(6);
        section.setStyle("-fx-padding: 8 0 0 0;");

        // Séparateur + titre de la section
        Region sep2 = new Region();
        sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color: #f0f0f0;");

        HBox titreSponsors = new HBox(6);
        titreSponsors.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icone  = new Label("💼");
        icone.setStyle("-fx-font-size: 11px;");
        Label labelS = new Label(sponsors.isEmpty() ? "Aucun sponsor" : "Sponsors");
        labelS.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #888;");
        titreSponsors.getChildren().addAll(icone, labelS);

        section.getChildren().addAll(sep2, titreSponsors);

        if (sponsors.isEmpty()) {
            Label aucun = new Label("Pas encore de sponsor pour cet événement.");
            aucun.setStyle("-fx-font-size: 10px; -fx-text-fill: #bbb; -fx-wrap-text: true;");
            aucun.setWrapText(true);
            section.getChildren().add(aucun);
        } else {
            // Afficher chaque sponsor avec un badge coloré selon son type
            FlowPane badgesFlow = new FlowPane(6, 4);
            for (Sponsor s : sponsors) {
                HBox sponsorBadge = new HBox(4);
                sponsorBadge.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                sponsorBadge.setStyle(
                        "-fx-background-color: " + getCouleurType(s.getType()) + "18;" +
                                "-fx-border-color: " + getCouleurType(s.getType()) + "55;" +
                                "-fx-border-width: 1;" +
                                "-fx-background-radius: 6;" +
                                "-fx-border-radius: 6;" +
                                "-fx-padding: 3 7;"
                );

                Label nomLabel = new Label(s.getNomSponsor());
                nomLabel.setStyle(
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                                "-fx-text-fill: " + getCouleurType(s.getType()) + ";"
                );

                Label typeLabel = new Label("· " + s.getType());
                typeLabel.setStyle(
                        "-fx-font-size: 9px;" +
                                "-fx-text-fill: " + getCouleurType(s.getType()) + "99;"
                );

                Label montantLabel = new Label(s.getMontant() + " DT");
                montantLabel.setStyle(
                        "-fx-font-size: 9px;" +
                                "-fx-text-fill: #27ae60;" +
                                "-fx-font-weight: bold;"
                );

                sponsorBadge.getChildren().addAll(nomLabel, typeLabel, montantLabel);
                badgesFlow.getChildren().add(sponsorBadge);
            }
            section.getChildren().add(badgesFlow);
        }

        return section;
    }

    // ── COULEUR PAR TYPE DE SPONSOR
    private String getCouleurType(String type) {
        if (type == null) return "#888888";
        switch (type.toLowerCase()) {
            case "or":       return "#e6a817";
            case "argent":   return "#7f8c8d";
            case "bronze":   return "#c0652b";
            case "platine":  return "#2979FF";
            default:         return "#27ae60";
        }
    }

    @FXML
    private void handleRecherche() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            afficherCartes(tousLesEvents);
        } else {
            List<Event> filtres = tousLesEvents.stream()
                    .filter(e -> e.getTitre().toLowerCase().contains(query)
                            || e.getType().toLowerCase().contains(query)
                            || e.getDescription().toLowerCase().contains(query))
                    .collect(Collectors.toList());
            afficherCartes(filtres);
        }
    }

    @FXML
    private void handleRecommandationsIA() {
        try {
            List<Event> events = eventService.recupererTous();
            if (events.isEmpty()) {
                showAlert("Aucun événement disponible.");
                return;
            }
            new Thread(() -> {
                try {
                    edu.connexion3a36.services.RecommendationService rs =
                            new edu.connexion3a36.services.RecommendationService();
                    String resultat = rs.recommander(events);
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("🤖 Recommandations IA");
                        alert.setHeaderText(null);
                        alert.setContentText(resultat);
                        alert.getDialogPane().setPrefWidth(600);
                        alert.showAndWait();
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() ->
                            showAlert("❌ Erreur : " + e.getMessage()));
                }
            }).start();
        } catch (SQLException e) {
            showAlert("❌ Erreur : " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void handleGerer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event.fxml"));
            Node page = loader.load();
            StackPane contentArea = (StackPane) cardsContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(page);
            }
        } catch (Exception e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }



}



