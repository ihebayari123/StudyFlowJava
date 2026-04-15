package edu.connexion3a36.Controller;
import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.ChapitreService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
public class ChapitresFrontController {

    @FXML private Button btnBack;
    @FXML private Label courseTitle;
    @FXML private Label courseDescription;
    @FXML private Label courseIcon;
    @FXML private Label lblTotalChapitres;
    @FXML private Label lblWithVideo;
    @FXML private VBox chapitresContainer;

    private ChapitreService chapitreService;
    private Cours currentCourse;
    private Runnable onBackAction;

    private static final String[] COLORS = {
            "#e8f0fe", "#fce4ec", "#e8f5e9", "#fff3e0", "#f3e5f5",
            "#e0f7fa", "#fff8e1", "#fbe9e7", "#ede7f6", "#e0f2f1"
    };

    @FXML
    public void initialize() {
        chapitreService = new ChapitreService();
        btnBack.setOnAction(e -> {
            if (onBackAction != null) onBackAction.run();
        });
    }

    public void setCours(Cours cours, Runnable onBack) {
        this.currentCourse = cours;
        this.onBackAction = onBack;
        loadChapitres();
    }

    private void loadChapitres() {
        if (currentCourse == null) return;

        // Update header
        courseTitle.setText(currentCourse.getTitre());
        String desc = currentCourse.getDescription() != null ? currentCourse.getDescription() : "";
        if (desc.length() > 100) desc = desc.substring(0, 100) + "...";
        courseDescription.setText(desc);
        courseIcon.setText(getCourseIcon(currentCourse.getTitre()));

        // Load chapters
        List<Chapitre> chapitres = chapitreService.findByCourse(currentCourse.getId());

        // Update stats
        lblTotalChapitres.setText(String.valueOf(chapitres.size()));
        long withVideo = chapitres.stream()
                .filter(ch -> ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty())
                .count();
        lblWithVideo.setText(String.valueOf(withVideo));

        // Render chapters
        chapitresContainer.getChildren().clear();

        if (chapitres.isEmpty()) {
            Label emptyLabel = new Label("Aucun chapitre disponible pour ce cours.");
            emptyLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
            emptyLabel.setPadding(new Insets(40));
            chapitresContainer.getChildren().add(emptyLabel);
        } else {
            for (int i = 0; i < chapitres.size(); i++) {
                chapitresContainer.getChildren().add(buildChapitreCard(chapitres.get(i), i));
            }
        }
    }

    private VBox buildChapitreCard(Chapitre ch, int index) {
        int colorIndex = index % COLORS.length;
        String bgColor = COLORS[colorIndex];

        VBox card = new VBox(0);
        card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");

        // Header row (always visible)
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + bgColor + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;"
        );

        // Order number
        Label orderLabel = new Label(String.valueOf(ch.getOrdre() != null ? ch.getOrdre() : index + 1));
        orderLabel.setMinWidth(34);
        orderLabel.setMinHeight(34);
        orderLabel.setAlignment(Pos.CENTER);
        orderLabel.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: #333; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;"
        );

        // Title and meta info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLabel = new Label(ch.getTitre());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        HBox tags = new HBox(6);
        if (ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty()) {
            tags.getChildren().add(createTag("▶ Vidéo", "#e3f2fd", "#1565c0"));
        }
        if (ch.getDurationMinutes() != null) {
            tags.getChildren().add(createTag("⏱ " + ch.getDurationMinutes() + " min", "#fce4ec", "#c62828"));
        }
        if (ch.getLinks() != null && !ch.getLinks().isEmpty()) {
            tags.getChildren().add(createTag("🔗 " + ch.getLinks().size() + " liens", "#e8f5e9", "#2e7d32"));
        }

        info.getChildren().addAll(titleLabel, tags);

        // Expand/collapse arrow
        Label arrowLabel = new Label("▼");
        arrowLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");

        header.getChildren().addAll(orderLabel, info, arrowLabel);

        // Detail panel (hidden by default)
        VBox detailPanel = createDetailPanel(ch, bgColor);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);

        // Toggle on click
        header.setOnMouseClicked(e -> {
            boolean isVisible = detailPanel.isVisible();
            detailPanel.setVisible(!isVisible);
            detailPanel.setManaged(!isVisible);
            arrowLabel.setText(isVisible ? "▼" : "▲");
        });

        // Hover effect
        header.setOnMouseEntered(e -> header.setStyle(
                "-fx-background-color: #fafafa;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + bgColor + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;"
        ));
        header.setOnMouseExited(e -> header.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + bgColor + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;"
        ));

        card.getChildren().addAll(header, detailPanel);
        return card;
    }

    private VBox createDetailPanel(Chapitre ch, String bgColor) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(0, 20, 20, 20));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + bgColor + ";" +
                        "-fx-border-width: 0 1.5 1.5 1.5;" +
                        "-fx-border-radius: 0 0 16 16;" +
                        "-fx-background-radius: 0 0 16 16;"
        );

        // Content
        if (ch.getContenu() != null && !ch.getContenu().isEmpty()) {
            Label contentTitle = new Label("📝 Contenu");
            contentTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333;");

            Label contentText = new Label(ch.getContenu());
            contentText.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-wrap-text: true;");
            contentText.setMaxWidth(Double.MAX_VALUE);

            panel.getChildren().addAll(contentTitle, contentText);
        }

        // Video
        if (ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty()) {
            Label videoTitle = new Label("▶ Vidéo");
            videoTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333; -fx-padding: 8 0 0 0;");

            HBox videoBox = new HBox(10);
            videoBox.setAlignment(Pos.CENTER_LEFT);
            videoBox.setPadding(new Insets(10, 14, 10, 14));
            videoBox.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10;");

            Label urlLabel = new Label(ch.getVideoUrl());
            urlLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1565c0;");
            HBox.setHgrow(urlLabel, Priority.ALWAYS);

            Button openBtn = new Button("Ouvrir");
            openBtn.setStyle(
                    "-fx-background-color: #1565c0; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"
            );
            openBtn.setOnAction(e -> openUrl(ch.getVideoUrl()));

            videoBox.getChildren().addAll(urlLabel, openBtn);
            panel.getChildren().addAll(videoTitle, videoBox);
        }

        // Links
        if (ch.getLinks() != null && !ch.getLinks().isEmpty()) {
            Label linksTitle = new Label("🔗 Ressources");
            linksTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333; -fx-padding: 8 0 0 0;");
            panel.getChildren().add(linksTitle);

            for (String link : ch.getLinks()) {
                HBox linkBox = new HBox(10);
                linkBox.setAlignment(Pos.CENTER_LEFT);
                linkBox.setPadding(new Insets(8, 12, 8, 12));
                linkBox.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 8; -fx-cursor: hand;");

                Label linkLabel = new Label("🔗 " + link);
                linkLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2e7d32;");
                HBox.setHgrow(linkLabel, Priority.ALWAYS);

                Button openLink = new Button("Ouvrir");
                openLink.setStyle(
                        "-fx-background-color: #2e7d32; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand;"
                );
                openLink.setOnAction(e -> openUrl(link));

                linkBox.getChildren().addAll(linkLabel, openLink);
                panel.getChildren().add(linkBox);
            }
        }

        // Image
        if (ch.getImageUrl() != null && !ch.getImageUrl().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(ch.getImageUrl(), 400, 200, true, true));
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
                panel.getChildren().add(imageView);
            } catch (Exception e) {
                Label error = new Label("⚠️ Image non disponible");
                error.setStyle("-fx-text-fill: #c62828; -fx-font-size: 12px; -fx-padding: 8 0 0 0;");
                panel.getChildren().add(error);
            }
        }

        if (panel.getChildren().isEmpty()) {
            Label empty = new Label("Aucun contenu détaillé pour ce chapitre.");
            empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px; -fx-padding: 12 0;");
            panel.getChildren().add(empty);
        }

        return panel;
    }

    private Label createTag(String text, String bgColor, String textColor) {
        Label tag = new Label(text);
        tag.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-padding: 2 8; -fx-background-radius: 20;"
        );
        return tag;
    }

    private String getCourseIcon(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("python") || lowerTitle.contains("java")) return "🐍";
        if (lowerTitle.contains("design") || lowerTitle.contains("ui")) return "🎨";
        if (lowerTitle.contains("data")) return "📊";
        if (lowerTitle.contains("english")) return "🇬🇧";
        if (lowerTitle.contains("spanish")) return "🇪🇸";
        if (lowerTitle.contains("guitar")) return "🎸";
        if (lowerTitle.contains("photo")) return "📸";
        return "📚";
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
