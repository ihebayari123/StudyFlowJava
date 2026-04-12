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
        String accent      = q.getDuree() <= 15 ? "#0A8A5A" : q.getDuree() <= 30 ? "#C47A00" : "#C0222E";
        String accentLight = q.getDuree() <= 15 ? "#D4F5E4" : q.getDuree() <= 30 ? "#FFF0CC" : "#FFE0E3";
        String diffLabel   = q.getDuree() <= 15 ? "FACILE"  : q.getDuree() <= 30 ? "MOYEN"   : "DIFFICILE";

        // Offset shadow wrapper
        StackPane wrap = new StackPane();
        wrap.setPrefWidth(265);

        VBox shadow = new VBox();
        shadow.setStyle("-fx-background-color:" + accent + ";");
        shadow.setPrefWidth(265); shadow.setPrefHeight(180);
        shadow.setTranslateX(5); shadow.setTranslateY(5);

        // Card
        VBox card = new VBox(0);
        card.setPrefWidth(265);
        card.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:#1A1A1A; -fx-border-width:3; -fx-cursor:hand;");

        // Top color bar
        HBox bar = new HBox();
        bar.setStyle("-fx-background-color:" + accent + "; -fx-min-height:5;");
        bar.setMaxWidth(Double.MAX_VALUE);

        // Content
        VBox top = new VBox(8);
        top.setStyle("-fx-padding:14 14 12 14; -fx-background-color:#FFFFFF;");

        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(diffLabel);
        badge.setStyle("-fx-font-family:'Courier New'; -fx-font-size:8; -fx-font-weight:bold;" +
                       "-fx-padding:2 8; -fx-background-color:" + accentLight + ";" +
                       "-fx-text-fill:" + accent + "; -fx-border-color:" + accent + "; -fx-border-width:2;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label icon = new Label(getIcon(q)); icon.setStyle("-fx-font-size:18;");
        badgeRow.getChildren().addAll(badge, sp, icon);

        Label titre = new Label(q.getTitre().toUpperCase());
        titre.setStyle("-fx-font-family:'Courier New'; -fx-font-size:13; -fx-font-weight:bold;" +
                       "-fx-text-fill:#1A1A1A;"); titre.setMaxWidth(237); titre.setWrapText(true);

        HBox meta = new HBox(12);
        Label md = new Label(">> " + q.getDuree() + " MIN");
        md.setStyle("-fx-font-family:'Courier New'; -fx-font-size:9; -fx-text-fill:#777;");
        Label mc = new Label("COURS #" + q.getCourseId());
        mc.setStyle("-fx-font-family:'Courier New'; -fx-font-size:9; -fx-text-fill:#777;");
        meta.getChildren().addAll(md, mc);
        top.getChildren().addAll(badgeRow, titre, meta);

        // Sep
        Region sep = new Region(); sep.setPrefHeight(2);
        sep.setStyle("-fx-background-color:#EBEBEB;");

        // Footer
        HBox footer = new HBox(10); footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-background-color:#F8F8F8; -fx-padding:10 14;" +
                        "-fx-border-color:#EBEBEB; -fx-border-width:2 0 0 0;");
        Label ql = new Label("? QUESTIONS");
        ql.setStyle("-fx-font-family:'Courier New'; -fx-font-size:9; -fx-text-fill:#999;");
        Region fsp = new Region(); HBox.setHgrow(fsp, Priority.ALWAYS);
        Button btn = new Button("[ GO ]");
        btn.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-font-weight:bold;" +
                     "-fx-background-color:" + accent + "; -fx-text-fill:#FFFFFF;" +
                     "-fx-border-color:#1A1A1A; -fx-border-width:2;" +
                     "-fx-background-radius:0; -fx-border-radius:0; -fx-padding:6 16; -fx-cursor:hand;");
        btn.setOnAction(e -> lancerQuiz(q));
        footer.getChildren().addAll(ql, fsp, btn);

        card.getChildren().addAll(bar, top, sep, footer);

        // Hover
        card.setOnMouseEntered(e ->
            card.setStyle(card.getStyle().replace("#FFFFFF;", accentLight + ";")));
        card.setOnMouseExited(e ->
            card.setStyle(card.getStyle().replace(accentLight + ";", "#FFFFFF;")));

        wrap.getChildren().addAll(shadow, card);
        return new VBox(wrap);
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
