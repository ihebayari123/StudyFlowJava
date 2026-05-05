package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.ChapitreService;
import edu.connexion3a36.services.CoursService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CoursFrontController {

    @FXML private TextField searchField;
    @FXML private Label lblSubtitle;
    @FXML private Label lblTotalCours;
    @FXML private Label lblTotalChapitres;
    @FXML private VBox coursesContainer;
    @FXML private Button filterAll;
    @FXML private Button filterRecent;

    private CoursService coursService;
    private ChapitreService chapitreService;
    private List<Cours> allCourses;
    private String activeFilter = "all";

    // Same colour palette as fitness_dashboard2 / chapitrefront
    private static final String[] ACCENT_COLORS = {
            "#e8f0fe", "#fce4ec", "#e8f5e9", "#fff3e0", "#f3e5f5",
            "#e0f7fa", "#fff8e1", "#fbe9e7", "#ede7f6", "#e0f2f1"
    };

    @FXML
    public void initialize() {
        coursService    = new CoursService();
        chapitreService = new ChapitreService();
        // Defer DB load to after the scene graph is fully ready
        Platform.runLater(this::loadCourses);
    }

    // Called by DashboardController via reflection — keeps nav wiring intact
    public void setDashboardController(Object dc) {
        // No dashboard reference needed for read-only front view
    }

    // ── Data ─────────────────────────────────────────────────────────────────
    private void loadCourses() {
        try {
            allCourses = coursService.findAll();
            System.out.println("✅ CoursFrontController: " + allCourses.size() + " cours chargés depuis la BDD");
            allCourses.forEach(c -> System.out.println("   - [" + c.getId() + "] " + c.getTitre()));
        } catch (Exception e) {
            System.err.println("❌ CoursFrontController: erreur chargement cours: " + e.getMessage());
            e.printStackTrace();
            allCourses = new java.util.ArrayList<>();
        }
        updateStats();
        renderCourses(allCourses);
    }

    private void updateStats() {
        lblTotalCours.setText(String.valueOf(allCourses.size()));
        int total = 0;
        for (Cours c : allCourses) {
            try {
                total += chapitreService.findByCourse(c.getId()).size();
            } catch (Exception e) {
                System.err.println("⚠️ Erreur chapitres pour cours " + c.getId() + ": " + e.getMessage());
            }
        }
        lblTotalChapitres.setText(String.valueOf(total));
    }

    private void renderCourses(List<Cours> courses) {
        coursesContainer.getChildren().clear();

        if (courses == null || courses.isEmpty()) {
            Label empty = new Label("Aucun cours disponible pour le moment.");
            empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
            empty.setPadding(new Insets(40));
            coursesContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < courses.size(); i++) {
            coursesContainer.getChildren().add(buildCourseCard(courses.get(i), i));
        }
    }

    // ── Card builder ─────────────────────────────────────────────────────────
    private HBox buildCourseCard(Cours cours, int index) {
        String accent = ACCENT_COLORS[index % ACCENT_COLORS.length];

        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + accent + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );

        // ── Left: icon badge ───────────────────────────────────────────────
        Label iconLabel = new Label(getCourseIcon(cours.getTitre()));
        iconLabel.setMinWidth(52);
        iconLabel.setMinHeight(52);
        iconLabel.setAlignment(Pos.CENTER);
        iconLabel.setStyle(
                "-fx-background-color: " + accent + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-font-size: 24px;"
        );

        // ── Centre: title + description + tags ────────────────────────────
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLabel = new Label(cours.getTitre());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        String desc = cours.getDescription() != null ? cours.getDescription() : "";
        if (desc.length() > 90) desc = desc.substring(0, 90) + "…";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777; -fx-wrap-text: true;");

        // Chapter count tag
        int chapCount = 0;
        try {
            chapCount = chapitreService.findByCourse(cours.getId()).size();
        } catch (Exception e) {
            System.err.println("⚠️ Erreur chapCount pour cours " + cours.getId() + ": " + e.getMessage());
        }
        HBox tags = new HBox(6);
        tags.getChildren().add(createTag("📖 " + chapCount + " chapitres", accent, "#333333"));

        info.getChildren().addAll(titleLabel, descLabel, tags);

        // ── Right: "Voir les chapitres" button ────────────────────────────
        Button viewBtn = new Button("Voir les chapitres →");
        viewBtn.setStyle(
                "-fx-background-color: #111111;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18;" +
                        "-fx-cursor: hand;"
        );
        viewBtn.setOnAction(e -> openChapitresFront(cours));

        // Hover on the whole card
        card.setOnMouseEntered(ev -> card.setStyle(
                "-fx-background-color: #fafafa;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + accent + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 0, 4);"
        ));
        card.setOnMouseExited(ev -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + accent + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        ));

        // Clicking anywhere on the card also navigates
        card.setOnMouseClicked(ev -> openChapitresFront(cours));

        card.getChildren().addAll(iconLabel, info, viewBtn);
        return card;
    }

    // ── Navigation ───────────────────────────────────────────────────────────
    private void openChapitresFront(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chapitrefront.fxml"));
            Parent view = loader.load();

            ChapitresFrontController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Chapitres – " + cours.getTitre());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(coursesContainer.getScene().getWindow());
            stage.setScene(new Scene(view, 860, 640));

            ctrl.setCours(cours, stage::close);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        List<Cours> filtered = kw.isEmpty() ? allCourses
                : allCourses.stream()
                .filter(c -> c.getTitre().toLowerCase().contains(kw)
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
        renderCourses(applyFilter(filtered));
    }

    // ── Filters ──────────────────────────────────────────────────────────────
    @FXML
    private void handleFilter(javafx.event.ActionEvent event) {
        Button src = (Button) event.getSource();
        activeFilter = (String) src.getUserData();

        // Update tab styles
        String active   = "-fx-background-color: #111111; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 8 18; -fx-background-radius: 20;";
        String inactive = "-fx-background-color: #eeeeee; -fx-text-fill: #888; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 8 18; -fx-background-radius: 20;";
        filterAll.setStyle("all".equals(activeFilter)    ? active : inactive);
        filterRecent.setStyle("recent".equals(activeFilter) ? active : inactive);

        List<Cours> result = applyFilter(allCourses);
        String kw = searchField.getText().trim().toLowerCase();
        if (!kw.isEmpty()) {
            result = result.stream()
                    .filter(c -> c.getTitre().toLowerCase().contains(kw)
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }
        renderCourses(result);
    }

    private List<Cours> applyFilter(List<Cours> courses) {
        if ("recent".equals(activeFilter)) {
            // Show last 5 (already ordered by id DESC from service)
            return courses.stream().limit(5).collect(Collectors.toList());
        }
        return courses;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
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
        if (title == null) return "📚";
        String t = title.toLowerCase();
        if (t.contains("python") || t.contains("java"))    return "🐍";
        if (t.contains("design") || t.contains("ui"))      return "🎨";
        if (t.contains("data"))                             return "📊";
        if (t.contains("english"))                          return "🇬🇧";
        if (t.contains("spanish") || t.contains("espagnol")) return "🇪🇸";
        if (t.contains("guitar") || t.contains("musique")) return "🎸";
        if (t.contains("photo"))                            return "📸";
        if (t.contains("math"))                             return "🔢";
        if (t.contains("web") || t.contains("html"))       return "🌐";
        return "📚";
    }
}