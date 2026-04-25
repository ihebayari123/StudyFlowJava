package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Question;
import edu.connexion3a36.entities.Quiz;
import edu.connexion3a36.services.AIFeedbackService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserResultsController {

    // ── FXML — Score Hero ─────────────────────────────────────────────────────
    @FXML private Label  lblScorePct;
    @FXML private Label  lblResultTitle;
    @FXML private Label  lblResultMsg;
    @FXML private Label  lblTotal;
    @FXML private Label  lblPoints;
    @FXML private Label  lblCorrect;
    @FXML private Label  lblWrong;

    // ── FXML — Récapitulatif ──────────────────────────────────────────────────
    @FXML private VBox   reviewList;

    // ── FXML — Section AI Feedback ────────────────────────────────────────────
    @FXML private VBox              vboxAIFeedback;
    @FXML private ProgressIndicator piAILoading;
    @FXML private Label             lblAIMention;
    @FXML private Label             lblAIEncouragement;
    @FXML private Label             lblAIPointsForts;
    @FXML private Label             lblAIPointsFaibles;
    @FXML private Label             lblAIConseil;
    @FXML private Label             lblAISource;

    // ── État ──────────────────────────────────────────────────────────────────
    private Quiz                                    quiz;
    private List<Question>                          questions;
    private List<UserQuizController.ResultItem>     resultItems;
    private Map<Integer, Boolean>                   reponsesMap;
    private int                                     score;
    private int                                     scorePoints;
    private StackPane                               contentArea;

    // =========================================================================
    // ── API appelée par UserQuizController ───────────────────────────────────
    // =========================================================================

    /**
     * Point d'entrée appelé depuis UserQuizController après la fin du quiz.
     *
     * @param quiz         Quiz qui vient d'être passé
     * @param resultItems  Liste des résultats (une entrée par question)
     * @param scoreCorrect Nombre de bonnes réponses
     * @param scorePoints  Total de points accumulés
     */
    public void setData(Quiz quiz,
                        List<UserQuizController.ResultItem> resultItems,
                        int scoreCorrect,
                        int scorePoints) {
        this.quiz        = quiz;
        this.resultItems = resultItems;
        this.score       = scoreCorrect;
        this.scorePoints = scorePoints;

        // Construire questions et reponsesMap à partir de resultItems
        this.questions = resultItems.stream()
                .map(UserQuizController.ResultItem::question)
                .collect(Collectors.toList());

        this.reponsesMap = new HashMap<>();
        for (UserQuizController.ResultItem item : resultItems) {
            reponsesMap.put(item.question().getId(), item.correct());
        }
    }

    /**
     * Ancienne API conservée pour compatibilité éventuelle.
     */
    public void setDonnees(Quiz quiz, List<Question> questions,
                           Map<Integer, Boolean> reponsesMap,
                           int score, StackPane contentArea) {
        this.quiz        = quiz;
        this.questions   = questions;
        this.reponsesMap = reponsesMap;
        this.score       = score;
        this.contentArea = contentArea;
    }

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    /**
     * Appelé après setData() pour déclencher l'affichage.
     */
    public void afficher() {
        afficherStats();
        construireRecapitulatif();
        lancerFeedbackIA();
    }

    // =========================================================================
    // ── Statistiques ─────────────────────────────────────────────────────────
    // =========================================================================

    private void afficherStats() {
        int total   = questions.size();
        int correct = score;
        int wrong   = total - correct;
        double pct  = total > 0 ? (correct * 100.0 / total) : 0;

        lblTotal  .setText(String.valueOf(total));
        lblPoints .setText(scorePoints + " pts");
        lblCorrect.setText(String.valueOf(correct));
        lblWrong  .setText(String.valueOf(wrong));
        lblScorePct.setText(String.format("%.0f%%", pct));

        if (pct >= 80) {
            lblResultTitle.setText("EXCELLENT !");
            lblResultMsg.setText("Performance remarquable. Vous maîtrisez très bien ce sujet.");
        } else if (pct >= 60) {
            lblResultTitle.setText("BIEN JOUÉ");
            lblResultMsg.setText("Bon résultat ! Quelques notions à consolider.");
        } else if (pct >= 40) {
            lblResultTitle.setText("PEUT MIEUX FAIRE");
            lblResultMsg.setText("Résultat passable. Revoyez le cours avant de retenter.");
        } else {
            lblResultTitle.setText("À REVOIR");
            lblResultMsg.setText("Ne vous découragez pas. Relisez le cours et réessayez !");
        }
    }

    // =========================================================================
    // ── Récapitulatif détaillé ────────────────────────────────────────────────
    // =========================================================================

    private void construireRecapitulatif() {
        reviewList.getChildren().clear();

        for (int i = 0; i < questions.size(); i++) {
            Question q      = questions.get(i);
            boolean correct = Boolean.TRUE.equals(reponsesMap.get(q.getId()));

            // Séparateur (sauf premier élément)
            if (i > 0) {
                Region sep = new Region();
                sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: #f0f0f0;");
                reviewList.getChildren().add(sep);
            }

            // Ligne question
            HBox ligne = new HBox(14);
            ligne.setStyle("-fx-padding: 14 0;");
            ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Pastille numéro
            StackPane badge  = new StackPane();
            Circle    cercle = new Circle(16);
            cercle.setFill(Color.web(correct ? "#e8f5e9" : "#fce4ec"));
            cercle.setStroke(Color.web(correct ? "#a5d6a7" : "#f48fb1"));
            cercle.setStrokeWidth(1.5);
            Label num = new Label(String.valueOf(i + 1));
            num.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "
                    + (correct ? "#2e7d32" : "#c62828") + ";");
            badge.getChildren().addAll(cercle, num);

            // Icône résultat
            Label icone = new Label(correct ? "✅" : "❌");
            icone.setStyle("-fx-font-size: 16px;");

            // Réponse de l'utilisateur (si disponible via resultItems)
            String userAnswer = "";
            if (resultItems != null && i < resultItems.size()) {
                userAnswer = resultItems.get(i).userAnswer();
            }

            // Texte question + type + réponse
            VBox infos = new VBox(3);
            HBox.setHgrow(infos, Priority.ALWAYS);

            Label txtQ = new Label(q.getTexte());
            txtQ.setWrapText(true);
            txtQ.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

            HBox badges = new HBox(6);
            badges.getChildren().addAll(
                    pillBadge(q.getType(),   "#e3f2fd", "#1565c0"),
                    pillBadge(q.getNiveau(), niveauBg(q.getNiveau()), niveauFg(q.getNiveau()))
            );

            infos.getChildren().addAll(txtQ, badges);

            // Afficher la réponse donnée si incorrect
            if (!correct && !userAnswer.isBlank() && !"—".equals(userAnswer)) {
                Label rep = new Label("Votre réponse : " + userAnswer);
                rep.setStyle("-fx-font-size: 11px; -fx-text-fill: #c62828;");
                infos.getChildren().add(rep);
            }

            ligne.getChildren().addAll(badge, icone, infos);
            reviewList.getChildren().add(ligne);
        }
    }

    // =========================================================================
    // ── Feedback IA — asynchrone ──────────────────────────────────────────────
    // =========================================================================

    private void lancerFeedbackIA() {
        piAILoading.setVisible(true);
        cacherContenuIA();

        new Thread(() -> {
            AIFeedbackService.FeedbackResult r = AIFeedbackService.generer(
                    quiz, questions, reponsesMap, score, questions.size()
            );
            Platform.runLater(() -> afficherFeedback(r));
        }).start();
    }

    private void afficherFeedback(AIFeedbackService.FeedbackResult r) {
        piAILoading.setVisible(false);

        String mentionColor = switch (r.mention()) {
            case "Excellent" -> "#2e7d32";
            case "Bien"      -> "#1565c0";
            case "Passable"  -> "#e65100";
            default          -> "#c62828";
        };
        String mentionBg = switch (r.mention()) {
            case "Excellent" -> "#e8f5e9";
            case "Bien"      -> "#e3f2fd";
            case "Passable"  -> "#fff3e0";
            default          -> "#fce4ec";
        };

        lblAIMention.setText(r.mention());
        lblAIMention.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;"
                        + "-fx-text-fill: " + mentionColor + ";"
                        + "-fx-background-color: " + mentionBg + ";"
                        + "-fx-padding: 6 16; -fx-background-radius: 20;"
        );

        lblAIEncouragement.setText("💬  " + r.encouragement());
        lblAIPointsForts  .setText(r.pointsForts());
        lblAIPointsFaibles.setText(r.pointsFaibles());
        lblAIConseil      .setText("👉  " + r.conseil());

        lblAISource.setText(
                "AI".equals(r.source())
                        ? "🤖 Généré par intelligence artificielle (Groq / LLaMA-3)"
                        : "📝 Analyse automatique locale"
        );

        vboxAIFeedback.setVisible(true);
        vboxAIFeedback.setManaged(true);
    }

    private void cacherContenuIA() {
        lblAIMention      .setText("");
        lblAIEncouragement.setText("");
        lblAIPointsForts  .setText("");
        lblAIPointsFaibles.setText("");
        lblAIConseil      .setText("");
        lblAISource       .setText("");
    }

    // =========================================================================
    // ── Helpers visuels ───────────────────────────────────────────────────────
    // =========================================================================

    private Label pillBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;"
                + "-fx-background-color: " + bg + ";"
                + "-fx-text-fill: " + fg + ";"
                + "-fx-padding: 3 10; -fx-background-radius: 10;");
        return l;
    }

    private String niveauBg(String n) {
        return switch (n) {
            case "facile"    -> "#e8f5e9";
            case "moyen"     -> "#fff8e1";
            case "difficile" -> "#fce4ec";
            default          -> "#f5f5f5";
        };
    }

    private String niveauFg(String n) {
        return switch (n) {
            case "facile"    -> "#2e7d32";
            case "moyen"     -> "#f57f17";
            case "difficile" -> "#c62828";
            default          -> "#757575";
        };
    }

    // =========================================================================
    // ── Actions boutons ───────────────────────────────────────────────────────
    // =========================================================================

    @FXML
    public void rejouer() {
        if (quiz == null || contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserQuizView.fxml")
            );
            Node vue = loader.load();
            UserQuizController ctrl = loader.getController();
            // ✅ CORRIGÉ : utiliser les méthodes séparées au lieu de setQuiz(quiz, contentArea)
            ctrl.setQuiz(quiz);
            ctrl.setContentArea(contentArea);
            contentArea.getChildren().setAll(vue);
            ctrl.demarrer();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur relance quiz : " + e.getMessage()).show();
        }
    }

    @FXML
    public void retourHome() {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/UserHomeView.fxml")
            );
            Node vue = loader.load();
            UserHomeController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            contentArea.getChildren().setAll(vue);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur retour accueil : " + e.getMessage()).show();
        }
    }
}