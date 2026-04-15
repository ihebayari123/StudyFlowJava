package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.ChapitreService;
import edu.connexion3a36.services.CoursService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CoursesFrontController {
    @FXML private Label subtitleLabel;
    @FXML private TextField searchField;
    @FXML private Label lblTotalCourses;
    @FXML private Label lblTotalChapitres;
    @FXML private Label statusLabel;
    @FXML private FlowPane coursesGrid;
    @FXML private Button filterAll;
    @FXML private Button filterRecent;

    private CoursService coursService;
    private ChapitreService chapitreService;
    private List<Cours> allCourses;

    private static final String[] EMOJIS  = {"🎓","📐","🔬","🌍","💻","🎨","📖","🧮","🏛️","🧬"};
    private static final String[] COLORS  = {
            "#e8f0fe","#fce4ec","#e8f5e9","#fff3e0","#f3e5f5",
            "#e0f7fa","#fff8e1","#fbe9e7","#ede7f6","#e0f2f1"
    };
    private static final String[] BORDER_COLORS = {
            "#90caf9","#f48fb1","#a5d6a7","#ffcc80","#ce93d8",
            "#80deea","#ffe082","#ffab91","#b39ddb","#80cbc4"
    };

    @FXML
    public void initialize() {
        coursService    = new CoursService();
        chapitreService = new ChapitreService();
        loadCourses();
    }

    // ── Data Loading ──────────────────────────────────────────────────
    private void loadCourses() {
        try {
            allCourses = coursService.findAll();
            renderCards(allCourses);
            updateStats(allCourses);
            statusLabel.setText(allCourses.size() + " cours chargé(s) — cliquez sur un cours pour voir ses chapitres");
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStats(List<Cours> courses) {
        lblTotalCourses.setText(String.valueOf(courses.size()));
        int total = courses.stream()
                .mapToInt(c -> chapitreService.findByCourse(c.getId()).size())
                .sum();
        lblTotalChapitres.setText(String.valueOf(total));
    }

    // ── Card Rendering ────────────────────────────────────────────────
    private void renderCards(List<Cours> courses) {
        coursesGrid.getChildren().clear();
        if (courses.isEmpty()) {
            Label empty = new Label("Aucun cours disponible.\nCliquez sur « + Nouveau cours » pour commencer.");
            empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px; -fx-text-alignment: center;");
            coursesGrid.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < courses.size(); i++) {
            coursesGrid.getChildren().add(buildCard(courses.get(i), i));
        }
    }

    private VBox buildCard(Cours cours, int index) {
        int ci      = index % EMOJIS.length;
        String bg     = COLORS[ci];
        String border = BORDER_COLORS[ci];
        String emoji  = EMOJIS[ci];

        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 4);" +
                        "-fx-cursor: hand;"
        );

        // Hover effects (store base style to toggle shadow)
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("rgba(0,0,0,0.07)", "rgba(0,0,0,0.14)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("rgba(0,0,0,0.14)", "rgba(0,0,0,0.07)")));

        // Single click → open chapters inline
        card.setOnMouseClicked(e -> openChapitresInline(cours, index));

        // ── Banner ──
        StackPane banner = new StackPane();
        banner.setPrefHeight(100);
        banner.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 16 16 0 0;");

        if (cours.getImage() != null && !cours.getImage().isEmpty()) {
            try {
                ImageView iv = new ImageView(new Image(cours.getImage(), 220, 100, true, true));
                iv.setFitWidth(220); iv.setFitHeight(100);
                iv.setPreserveRatio(false);
                banner.getChildren().add(iv);
            } catch (Exception ex) {
                Label el = new Label(emoji);
                el.setStyle("-fx-font-size: 40px;");
                banner.getChildren().add(el);
            }
        } else {
            Label el = new Label(emoji);
            el.setStyle("-fx-font-size: 40px;");
            banner.getChildren().add(el);
        }

        // ── Body ──
        VBox body = new VBox(6);
        body.setStyle("-fx-padding: 0 14 14 14;");

        Label titleLbl = new Label(cours.getTitre());
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-wrap-text: true;");
        titleLbl.setMaxWidth(192);

        String desc = cours.getDescription() != null ? cours.getDescription() : "";
        if (desc.length() > 80) desc = desc.substring(0, 80) + "…";
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-wrap-text: true;");
        descLbl.setMaxWidth(192);

        int chapCount = chapitreService.findByCourse(cours.getId()).size();
        Label chapBadge = new Label("📖 " + chapCount + " chapitre" + (chapCount > 1 ? "s" : ""));
        chapBadge.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: #444; -fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-padding: 3 10; -fx-background-radius: 20;"
        );

        // ── Action buttons ──
        HBox actions = new HBox(6);
        actions.setStyle("-fx-padding: 6 0 0 0;");

        Button chapBtn = new Button("Chapitres");
        chapBtn.setStyle(
                "-fx-background-color: #111111; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-font-size: 11px;" +
                        "-fx-padding: 6 14; -fx-cursor: hand;"
        );
        chapBtn.setOnAction(e -> { e.consume(); openChapitresInline(cours, index); });

        Button editBtn = new Button("✏️");
        editBtn.setStyle(
                "-fx-background-color: #fff3e0; -fx-text-fill: #e65100;" +
                        "-fx-background-radius: 8; -fx-font-size: 11px;" +
                        "-fx-padding: 6 10; -fx-cursor: hand;"
        );
        editBtn.setOnAction(e -> { e.consume(); openEditCourse(cours); });

        Button delBtn = new Button("🗑️");
        delBtn.setStyle(
                "-fx-background-color: #fce4ec; -fx-text-fill: #c62828;" +
                        "-fx-background-radius: 8; -fx-font-size: 11px;" +
                        "-fx-padding: 6 10; -fx-cursor: hand;"
        );
        delBtn.setOnAction(e -> { e.consume(); deleteCourse(cours); });

        actions.getChildren().addAll(chapBtn, editBtn, delBtn);
        body.getChildren().addAll(titleLbl, descLbl, chapBadge, actions);
        card.getChildren().addAll(banner, body);
        return card;
    }

    // ── Open Chapitres inline inside the same StackPane ───────────────
    /**
     * Finds the root StackPane of the dashboard and replaces the content
     * with a chapter-list view built entirely in code (no extra FXML needed).
     */
    private void openChapitresInline(Cours cours, int courseIndex) {
        StackPane root = findRootStackPane();
        if (root == null) {
            // Fallback: modal window
            openChapitresModal(cours);
            return;
        }
        root.getChildren().setAll(buildChapitresView(cours, courseIndex, root));
    }

    /**
     * Builds the full "Chapitres" screen as a VBox, matching the dashboard style.
     */
    private VBox buildChapitresView(Cours cours, int courseIndex, StackPane rootPane) {
        int ci         = courseIndex % COLORS.length;
        String accentBg = COLORS[ci];
        String accentBorder = BORDER_COLORS[ci];

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f5f5f5;");

        // ── Header ──────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setPrefHeight(90);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, " + accentBg + ", #fce4ec);" +
                        "-fx-border-color: transparent transparent #e0e0e0 transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        header.setPadding(new Insets(0, 28, 0, 28));

        Button backBtn = new Button("← Retour");
        backBtn.setStyle(
                "-fx-background-color: white; -fx-border-color: #ddd;" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;" +
                        "-fx-padding: 8 14; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        backBtn.setOnAction(e -> rootPane.getChildren().setAll(buildCoursesRoot()));

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("📚 " + cours.getTitre());
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Georgia'; -fx-text-fill: #1a237e;");
        String desc = cours.getDescription() != null ? cours.getDescription() : "";
        if (desc.length() > 80) desc = desc.substring(0, 80) + "…";
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c6bc0;");
        titleBox.getChildren().addAll(titleLbl, descLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label icon = new Label("📖");
        icon.setStyle("-fx-font-size: 40px;");

        header.getChildren().addAll(backBtn, titleBox, spacer, icon);

        // ── Chapter list ─────────────────────────────────────────────
        List<Chapitre> chapitres = chapitreService.findByCourse(cours.getId());

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(28));

        if (chapitres.isEmpty()) {
            Label empty = new Label("Aucun chapitre pour ce cours.");
            empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
            content.getChildren().add(empty);
        } else {
            // Stats row
            HBox stats = new HBox(12);
            stats.setPadding(new Insets(0, 0, 8, 0));

            HBox statBox1 = makeStatBox(String.valueOf(chapitres.size()), "Chapitres", "#1565c0");
            long withVideo = chapitres.stream().filter(ch -> ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty()).count();
            HBox statBox2 = makeStatBox(String.valueOf(withVideo), "Avec vidéo", "#6a1b9a");
            long withLinks = chapitres.stream().filter(ch -> ch.getLinks() != null && !ch.getLinks().isEmpty()).count();
            HBox statBox3 = makeStatBox(String.valueOf(withLinks), "Avec liens", "#2e7d32");
            stats.getChildren().addAll(statBox1, statBox2, statBox3);
            content.getChildren().add(stats);

            // Chapter cards
            for (int i = 0; i < chapitres.size(); i++) {
                content.getChildren().add(buildChapitreCard(chapitres.get(i), i, accentBg, accentBorder, cours, courseIndex, rootPane));
            }
        }

        scroll.setContent(content);
        root.getChildren().addAll(header, scroll);
        return root;
    }

    private HBox makeStatBox(String value, String label, String color) {
        HBox box = new HBox(8);
        box.setPrefWidth(160);
        box.setPrefHeight(64);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #eeeeee; -fx-border-radius: 14;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        VBox lv = new VBox();
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        lv.getChildren().add(lbl);
        box.getChildren().addAll(val, lv);
        return box;
    }

    /**
     * Builds a single chapitre card with an expand-on-click detail panel.
     */
    private VBox buildChapitreCard(Chapitre ch, int idx, String accentBg, String accentBorder,
                                   Cours cours, int courseIndex, StackPane rootPane) {
        VBox wrapper = new VBox(0);

        // ── Card header row ──
        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + accentBorder + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);" +
                        "-fx-cursor: hand;"
        );

        // Order badge
        Label orderBadge = new Label(String.valueOf(ch.getOrdre() != null ? ch.getOrdre() : idx + 1));
        orderBadge.setMinWidth(34); orderBadge.setMinHeight(34);
        orderBadge.setAlignment(Pos.CENTER);
        orderBadge.setStyle(
                "-fx-background-color: " + accentBg + ";" +
                        "-fx-text-fill: #333; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;"
        );

        // Title + meta
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label chTitre = new Label(ch.getTitre());
        chTitre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        HBox tags = new HBox(6);
        if (ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty()) {
            tags.getChildren().add(makeTag("▶ Vidéo", "#e3f2fd", "#1565c0"));
        }
        if (ch.getImageUrl() != null && !ch.getImageUrl().isEmpty()) {
            tags.getChildren().add(makeTag("🖼 Image", "#f3e5f5", "#6a1b9a"));
        }
        if (ch.getLinks() != null && !ch.getLinks().isEmpty()) {
            tags.getChildren().add(makeTag("🔗 " + ch.getLinks().size() + " lien(s)", "#e8f5e9", "#2e7d32"));
        }
        if (ch.getFileName() != null && !ch.getFileName().isEmpty()) {
            tags.getChildren().add(makeTag("📎 Fichier", "#fff3e0", "#e65100"));
        }
        if (ch.getDurationMinutes() != null) {
            tags.getChildren().add(makeTag("⏱ " + ch.getDurationMinutes() + " min", "#fce4ec", "#c62828"));
        }
        info.getChildren().addAll(chTitre, tags);

        // Expand arrow
        Label arrow = new Label("▼");
        arrow.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");

        row.getChildren().addAll(orderBadge, info, arrow);

        // ── Detail panel (hidden by default) ──
        VBox detail = buildChapitreDetail(ch, accentBg);
        detail.setVisible(false);
        detail.setManaged(false);

        // Toggle on click
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle()
                .replace("rgba(0,0,0,0.06)", "rgba(0,0,0,0.12)")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle()
                .replace("rgba(0,0,0,0.12)", "rgba(0,0,0,0.06)")));
        row.setOnMouseClicked(e -> {
            boolean open = detail.isVisible();
            detail.setVisible(!open);
            detail.setManaged(!open);
            arrow.setText(open ? "▼" : "▲");
        });

        wrapper.getChildren().addAll(row, detail);
        return wrapper;
    }

    /**
     * The expanded detail panel shown when a chapter is clicked.
     */
    private VBox buildChapitreDetail(Chapitre ch, String accentBg) {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(0, 20, 20, 20));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 0 1 1 1;" +
                        "-fx-border-radius: 0 0 16 16;" +
                        "-fx-background-radius: 0 0 16 16;"
        );

        // ── Contenu (description text) ───────────────────────────────
        if (ch.getContenu() != null && !ch.getContenu().isEmpty()) {
            VBox section = new VBox(6);
            Label sectionTitle = makeSectionTitle("📝 Contenu");
            Label contenu = new Label(ch.getContenu());
            contenu.setStyle("-fx-font-size: 13px; -fx-text-fill: #444; -fx-wrap-text: true;");
            contenu.setMaxWidth(Double.MAX_VALUE);
            section.getChildren().addAll(sectionTitle, contenu);
            panel.getChildren().add(section);
        }

        // ── Video ────────────────────────────────────────────────────
        if (ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty()) {
            VBox section = new VBox(8);
            Label sectionTitle = makeSectionTitle("▶ Vidéo");

            // Show URL with open button (WebView is heavy; a button to open in browser is cleaner)
            HBox videoRow = new HBox(10);
            videoRow.setAlignment(Pos.CENTER_LEFT);
            videoRow.setPadding(new Insets(10, 14, 10, 14));
            videoRow.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10;");

            Label urlLbl = new Label(ch.getVideoUrl());
            urlLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #1565c0; -fx-wrap-text: true;");
            HBox.setHgrow(urlLbl, Priority.ALWAYS);

            Button openBtn = new Button("▶ Ouvrir");
            openBtn.setStyle(
                    "-fx-background-color: #1565c0; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"
            );
            openBtn.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(ch.getVideoUrl()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            videoRow.getChildren().addAll(urlLbl, openBtn);
            section.getChildren().addAll(sectionTitle, videoRow);
            panel.getChildren().add(section);
        }

        // ── Image ────────────────────────────────────────────────────
        if (ch.getImageUrl() != null && !ch.getImageUrl().isEmpty()) {
            VBox section = new VBox(8);
            Label sectionTitle = makeSectionTitle("🖼 Image");
            try {
                ImageView iv = new ImageView(new Image(ch.getImageUrl(), 460, 240, true, true));
                iv.setFitWidth(460); iv.setFitHeight(240);
                iv.setPreserveRatio(true);
                iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
                section.getChildren().addAll(sectionTitle, iv);
            } catch (Exception ex) {
                Label err = new Label("⚠ Image introuvable : " + ch.getImageUrl());
                err.setStyle("-fx-text-fill: #c62828; -fx-font-size: 12px;");
                section.getChildren().addAll(sectionTitle, err);
            }
            panel.getChildren().add(section);
        }

        // ── Fichier attaché ──────────────────────────────────────────
        if (ch.getFileName() != null && !ch.getFileName().isEmpty()) {
            VBox section = new VBox(6);
            Label sectionTitle = makeSectionTitle("📎 Fichier attaché");
            HBox fileRow = new HBox(10);
            fileRow.setAlignment(Pos.CENTER_LEFT);
            fileRow.setPadding(new Insets(10, 14, 10, 14));
            fileRow.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 10;");
            Label fileLbl = new Label("📄 " + ch.getFileName());
            fileLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #e65100;");
            fileRow.getChildren().add(fileLbl);
            section.getChildren().addAll(sectionTitle, fileRow);
            panel.getChildren().add(section);
        }

        // ── Links ────────────────────────────────────────────────────
        if (ch.getLinks() != null && !ch.getLinks().isEmpty()) {
            VBox section = new VBox(6);
            Label sectionTitle = makeSectionTitle("🔗 Ressources & Liens");
            VBox linksList = new VBox(6);
            for (String link : ch.getLinks()) {
                HBox linkRow = new HBox(10);
                linkRow.setAlignment(Pos.CENTER_LEFT);
                linkRow.setPadding(new Insets(8, 12, 8, 12));
                linkRow.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 8; -fx-cursor: hand;");

                Label linkLbl = new Label("🔗 " + link);
                linkLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2e7d32; -fx-wrap-text: true;");
                HBox.setHgrow(linkLbl, Priority.ALWAYS);

                Button openLink = new Button("Ouvrir");
                openLink.setStyle(
                        "-fx-background-color: #2e7d32; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand;"
                );
                openLink.setOnAction(e -> {
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(link));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                linkRow.getChildren().addAll(linkLbl, openLink);
                linksList.getChildren().add(linkRow);
            }
            section.getChildren().addAll(sectionTitle, linksList);
            panel.getChildren().add(section);
        }

        // ── Duration ─────────────────────────────────────────────────
        if (ch.getDurationMinutes() != null) {
            HBox dur = new HBox(8);
            dur.setAlignment(Pos.CENTER_LEFT);
            dur.setPadding(new Insets(8, 14, 8, 14));
            dur.setStyle("-fx-background-color: #fce4ec; -fx-background-radius: 10;");
            Label durLbl = new Label("⏱ Durée estimée : " + ch.getDurationMinutes() + " minute(s)");
            durLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c62828;");
            dur.getChildren().add(durLbl);
            panel.getChildren().add(dur);
        }

        // Empty state
        if (panel.getChildren().isEmpty()) {
            Label empty = new Label("Ce chapitre n'a pas encore de contenu détaillé.");
            empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px;");
            panel.getChildren().add(empty);
        }

        return panel;
    }

    private Label makeSectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333; -fx-padding: 4 0 2 0;");
        return l;
    }

    private Label makeTag(String text, String bg, String color) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-padding: 2 8; -fx-background-radius: 20;"
        );
        return l;
    }

    // ── Utility: rebuild the courses root VBox (used by "Back" button) ──
    private VBox buildCoursesRoot() {
        // Just reload the FXML — cleanest approach
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coursesfront.fxml"));
            return loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            Label err = new Label("Erreur de rechargement: " + e.getMessage());
            VBox v = new VBox(err);
            return v;
        }
    }

    // ── Find root StackPane ───────────────────────────────────────────
    private StackPane findRootStackPane() {
        javafx.scene.Node node = coursesGrid;
        while (node != null) {
            if (node instanceof StackPane sp) return sp;
            node = node.getParent();
        }
        return null;
    }

    // ── Modal fallback ────────────────────────────────────────────────
    private void openChapitresModal(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chapitres.fxml"));
            javafx.scene.Parent view = loader.load();
            ChapitreController ctrl = loader.getController();
            ctrl.setCours(cours);
            Stage stage = new Stage();
            stage.setTitle("Chapitres – " + cours.getTitre());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesGrid.getScene().getWindow());
            stage.setScene(new Scene(view, 900, 620));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ── FXML Handlers ────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            renderCards(allCourses);
            statusLabel.setText(allCourses.size() + " cours");
            return;
        }
        List<Cours> filtered = allCourses.stream()
                .filter(c -> c.getTitre().toLowerCase().contains(q)
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderCards(filtered);
        statusLabel.setText(filtered.size() + " résultat(s) pour « " + q + " »");
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        renderCards(allCourses);
        statusLabel.setText(allCourses.size() + " cours");
        setActiveFilter(filterAll);
    }

    @FXML
    private void handleFilter(javafx.event.ActionEvent e) {
        Button src = (Button) e.getSource();
        setActiveFilter(src);
        String tag = (String) src.getUserData();
        if ("recent".equals(tag)) {
            List<Cours> recent = allCourses.stream().limit(5).collect(Collectors.toList());
            renderCards(recent);
            statusLabel.setText("5 cours les plus récents");
        } else {
            renderCards(allCourses);
            statusLabel.setText(allCourses.size() + " cours");
        }
    }

    private void setActiveFilter(Button active) {
        for (Button b : new Button[]{filterAll, filterRecent}) {
            b.setStyle(b.getStyle()
                    .replace("-fx-background-color: #1565c0; -fx-text-fill: white;",
                            "-fx-background-color: #eeeeee; -fx-text-fill: #555;"));
        }
        active.setStyle(active.getStyle()
                .replace("-fx-background-color: #eeeeee; -fx-text-fill: #555;",
                        "-fx-background-color: #1565c0; -fx-text-fill: white;"));
    }

    @FXML
    private void handleAddCourse() {
        openCourseForm(null);
    }

    private void openEditCourse(Cours cours) {
        openCourseForm(cours);
    }

    private void openCourseForm(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add_course.fxml"));
            VBox formRoot = loader.load();
            AddCourseController ctrl = loader.getController();
            ctrl.setCoursService(coursService);
            ctrl.setParentController(new CoursController() {
                @Override public void refreshCourses() { loadCourses(); }
            });
            if (cours != null) ctrl.setCourseToUpdate(cours);
            Stage stage = new Stage();
            stage.setTitle(cours == null ? "Ajouter un cours" : "Modifier un cours");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesGrid.getScene().getWindow());
            stage.setScene(new Scene(formRoot));
            stage.showAndWait();
            loadCourses();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteCourse(Cours cours) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer « " + cours.getTitre() + " » ?\n\n⚠️ Les chapitres associés seront aussi supprimés.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == javafx.scene.control.ButtonType.OK) {
                coursService.delete(cours.getId());
                loadCourses();
            }
        });
    }

    public void refreshCourses() {
        loadCourses();
    }
}