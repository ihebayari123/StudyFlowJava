package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Quiz;
import edu.connexion3a36.services.QuizService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;

public class UserHomeController {

    @FXML private Label            lblTotalQuiz;
    @FXML private Label            lblCompletes;
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbDifficulte;
    @FXML private FlowPane         quizGrid;

    private final QuizService service = new QuizService();
    private ObservableList<Quiz> allQuizzes = FXCollections.observableArrayList();

    // ← fourni par DashboardController avant d'afficher la vue
    private StackPane contentArea;

    public void setContentArea(StackPane ca) {
        this.contentArea = ca;
    }

    @FXML
    public void initialize() {
        cbDifficulte.setItems(FXCollections.observableArrayList(
            "TOUS", "FACILE (<=15 min)", "MOYEN (15-30 min)", "DIFFICILE (>30 min)"
        ));
        cbDifficulte.setValue("TOUS");
        tfRecherche.textProperty().addListener((obs, old, val) -> filtrer());
        cbDifficulte.setOnAction(e -> filtrer());
        chargerQuiz();
    }

    // ── Chargement ────────────────────────────────────────────

    private void chargerQuiz() {
        try {
            allQuizzes.setAll(service.getData());
            if (lblTotalQuiz != null) lblTotalQuiz.setText(String.valueOf(allQuizzes.size()));
            if (lblCompletes != null) lblCompletes.setText("0");
            afficherQuiz(allQuizzes);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement quiz : " + e.getMessage()).show();
        }
    }

    // ── Filtre ────────────────────────────────────────────────

    private void filtrer() {
        String kw   = tfRecherche.getText().trim().toLowerCase();
        String diff = cbDifficulte.getValue();
        List<Quiz> result = allQuizzes.filtered(q -> {
            boolean mk = kw.isEmpty() || q.getTitre().toLowerCase().contains(kw);
            boolean md = switch (diff == null ? "TOUS" : diff) {
                case "FACILE (<=15 min)"   -> q.getDuree() <= 15;
                case "MOYEN (15-30 min)"   -> q.getDuree() > 15 && q.getDuree() <= 30;
                case "DIFFICILE (>30 min)" -> q.getDuree() > 30;
                default -> true;
            };
            return mk && md;
        });
        afficherQuiz(result);
    }

    // ── Cartes Brutalism Light ────────────────────────────────

    private void afficherQuiz(List<Quiz> quizzes) {
        quizGrid.getChildren().clear();
        if (quizzes.isEmpty()) {
            Label e = new Label("// AUCUN QUIZ DISPONIBLE");
            e.setStyle("-fx-font-family:'Courier New'; -fx-font-size:12; -fx-text-fill:#999;");
            quizGrid.getChildren().add(e);
            return;
        }
        for (Quiz q : quizzes) quizGrid.getChildren().add(creerCarte(q));
    }

    private VBox creerCarte(Quiz q) {
        // Palette selon difficulté — alignée sur le dashboard
        String accent      = q.getDuree() <= 15 ? "#2e7d32" : q.getDuree() <= 30 ? "#e65100" : "#c62828";
        String accentLight = q.getDuree() <= 15 ? "#e8f5e9" : q.getDuree() <= 30 ? "#fff3e0" : "#fce4ec";
        String diffLabel   = q.getDuree() <= 15 ? "Facile"  : q.getDuree() <= 30 ? "Moyen"   : "Difficile";

        // Card principale — style dashboard (white card, border-radius 16, dropshadow)
        VBox card = new VBox(0);
        card.setPrefWidth(260);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 16;" +
            "-fx-border-color: #eeeeee; -fx-border-radius: 16; -fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 4);" +
            "-fx-cursor: hand;"
        );

        // Barre top colorée
        HBox bar = new HBox();
        bar.setPrefHeight(5);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 16 16 0 0;");

        // Body
        VBox body = new VBox(10);
        body.setStyle("-fx-padding: 16 16 12 16; -fx-background-color: transparent;");

        // Badge + icône
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(diffLabel);
        badge.setStyle(
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 10;" +
            "-fx-background-color: " + accentLight + "; -fx-text-fill: " + accent + ";" +
            "-fx-background-radius: 20;"
        );
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label icon = new Label(getIcon(q));
        icon.setStyle("-fx-font-size: 20px; -fx-background-color: #f5f5f5;" +
                      "-fx-background-radius: 10; -fx-padding: 4 8;");
        badgeRow.getChildren().addAll(badge, sp, icon);

        // Titre
        Label titre = new Label(q.getTitre());
        titre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        titre.setMaxWidth(236); titre.setWrapText(true);

        // Meta (durée + cours)
        HBox meta = new HBox(14);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label md = new Label("⏱ " + q.getDuree() + " min");
        md.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        Label mc = new Label("Cours #" + q.getCourseId());
        mc.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        meta.getChildren().addAll(md, mc);

        body.getChildren().addAll(badgeRow, titre, meta);

        // Séparateur
        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #eeeeee;");

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
            "-fx-background-color: #f9f9f9; -fx-padding: 10 16;" +
            "-fx-background-radius: 0 0 16 16;"
        );
        Label ql = new Label("? questions");
        ql.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaaaaa;");
        Region fsp = new Region(); HBox.setHgrow(fsp, Priority.ALWAYS);
        Button btn = new Button("GO →");
        btn.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-color: " + accent + "; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-border-radius: 8;" +
            "-fx-padding: 7 18; -fx-cursor: hand;"
        );
        btn.setOnAction(e -> lancerQuiz(q));
        footer.getChildren().addAll(ql, fsp, btn);

        card.getChildren().addAll(bar, body, sep, footer);

        // Hover — léger lift
        String baseStyle = card.getStyle();
        String hoverStyle = baseStyle.replace(
            "dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 4)",
            "dropshadow(gaussian, rgba(0,0,0,0.14), 20, 0, 0, 7)"
        ).replace("white;", "#fafafa;");
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        VBox wrapper = new VBox(card);
        wrapper.setStyle("-fx-padding: 0;");
        return wrapper;
    }

    // ── Lancer quiz ───────────────────────────────────────────

    private void lancerQuiz(Quiz q) {
        resolveContentArea();
        if (contentArea == null) {
            new Alert(Alert.AlertType.ERROR,
                "Erreur navigation : contentArea introuvable.\n" +
                "Vérifiez que DashboardController appelle setContentArea().").show();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/UserQuizView.fxml")
            );
            Node vue = loader.load();
            UserQuizController ctrl = loader.getController();
            ctrl.setQuiz(q);
            ctrl.setContentArea(contentArea);
            contentArea.getChildren().setAll(vue);
            ctrl.demarrer();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lancement quiz : " + e.getMessage()).show();
        }
    }

    // ← null-safe : essaie le lookup si contentArea non injecté
    private void resolveContentArea() {
        if (contentArea == null && quizGrid.getScene() != null)
            contentArea = (StackPane) quizGrid.getScene().lookup("#contentArea");
    }

    private String getIcon(Quiz q) {
        return switch (q.getCourseId() % 6) {
            case 0 -> "⚡"; case 1 -> "🌐"; case 2 -> "🌿";
            case 3 -> "📜"; case 4 -> "📐"; default -> "❓";
        };
    }
}
