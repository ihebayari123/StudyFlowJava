package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Question;
import edu.connexion3a36.entities.Quiz;
import edu.connexion3a36.services.QuestionService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserQuizController {

    @FXML private Label       lblQuizTitle;
    @FXML private Label       lblQCounter;
    @FXML private Button      btnRetour;
    @FXML private Label       lblTimerVal;
    @FXML private ProgressBar pbTimer;
    @FXML private HBox        progressRow;
    @FXML private Label       lblQType;
    @FXML private Label       lblQTexte;
    @FXML private HBox        hintBox;
    @FXML private Label       lblHint;
    @FXML private VBox        optionsArea;
    @FXML private HBox        tfArea;
    @FXML private VBox        texteArea;
    @FXML private TextArea    taReponse;
    @FXML private VBox        feedbackBox;
    @FXML private Label       lblFeedback;
    @FXML private Button      btnValider;
    @FXML private Button      btnSuivant;

    private Quiz              quiz;
    private List<Question>    questions  = new ArrayList<>();
    private int               qIndex     = 0;
    private int               timePerQ   = 30;
    private int               timeLeft   = 0;
    private Timeline          timerAnim;
    private boolean           answered   = false;
    private String            selectedChoice = null;
    private Boolean           selectedTF     = null;
    private int               scoreCorrect   = 0;
    private int               scorePoints    = 0;
    private List<ResultItem>  resultItems    = new ArrayList<>();

    private StackPane         contentArea;
    private final QuestionService qService = new QuestionService();

    public void setQuiz(Quiz q)              { this.quiz = q; }
    public void setContentArea(StackPane sp) { this.contentArea = sp; }

    @FXML
    public void initialize() {
        btnValider.setOnAction(e -> valider());
        btnSuivant.setOnAction(e -> suivant());
        btnRetour .setOnAction(e -> retourHome());
    }

    // ── Point d'entrée appelé après setQuiz() ─────────────────
    public void demarrer() {
        try {
            questions = qService.getByQuizId(quiz.getId());
            if (questions.isEmpty()) {
                new Alert(Alert.AlertType.WARNING,
                    "Ce quiz ne contient aucune question.").show();
                retourHome(); return;
            }
            timePerQ = Math.max(10, (quiz.getDuree() * 60) / questions.size());
            lblQuizTitle.setText(quiz.getTitre().toUpperCase());
            construireProgressRow();
            afficherQuestion();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                "Erreur chargement questions : " + e.getMessage()).show();
        }
    }

    // ── Afficher une question ─────────────────────────────────

    private void afficherQuestion() {
        answered = false; selectedChoice = null; selectedTF = null;
        Question q = questions.get(qIndex);

        lblQCounter.setText((qIndex + 1) + " / " + questions.size());
        lblQType   .setText(getTypeLabel(q.getType()));
        lblQTexte  .setText(q.getTexte());

        if (q.getIndice() != null && !q.getIndice().isBlank()) {
            hintBox.setVisible(true); hintBox.setManaged(true);
            lblHint.setText(q.getIndice());
        } else {
            hintBox.setVisible(false); hintBox.setManaged(false);
        }

        cacherZones();
        switch (q.getType()) {
            case "choix_multiple" -> construireChoix(q);
            case "vrai_faux"      -> construireVraiFaux();
            case "texte"          -> construireTexte();
        }

        feedbackBox.setVisible(false); feedbackBox.setManaged(false);
        btnValider.setVisible(true);   btnValider.setManaged(true);
        btnValider.setDisable(true);
        btnSuivant.setVisible(false);  btnSuivant.setManaged(false);
        btnSuivant.setText(qIndex < questions.size() - 1 ? "SUIVANT >>" : "RESULTATS >>");

        mettreAJourProgressRow();
        demarrerTimer();
    }

    private void cacherZones() {
        optionsArea.setVisible(false); optionsArea.setManaged(false);
        tfArea     .setVisible(false); tfArea     .setManaged(false);
        texteArea  .setVisible(false); texteArea  .setManaged(false);
    }

    // ── Zones de réponse ─────────────────────────────────────

    private void construireChoix(Question q) {
        optionsArea.setVisible(true); optionsArea.setManaged(true);
        optionsArea.getChildren().clear();
        String[] keys = {"a","b","c","d"};
        String[] vals = {q.getChoixA(), q.getChoixB(), q.getChoixC(), q.getChoixD()};
        for (int i = 0; i < 4; i++) {
            if (vals[i] == null || vals[i].isBlank()) continue;
            final String key = keys[i]; final String val = vals[i];
            HBox opt = new HBox(12); opt.setAlignment(Pos.CENTER_LEFT);
            opt.setId("opt-" + key);
            opt.setStyle(styleOpt(false));
            Label kl = new Label(key.toUpperCase());
            kl.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-font-weight:bold;" +
                        "-fx-background-color:#DDDDDD; -fx-text-fill:#555555; -fx-padding:3 8;");
            Label vl = new Label(val);
            vl.setStyle("-fx-font-family:'Courier New'; -fx-font-size:13; -fx-text-fill:#1A1A1A;");
            vl.setWrapText(true);
            opt.getChildren().addAll(kl, vl);
            opt.setOnMouseClicked(e -> selectionnerChoix(key));
            opt.setOnMouseEntered(e -> { if (!answered && !key.equals(selectedChoice))
                opt.setStyle(opt.getStyle().replace("#F5F5F5","#EAEAEA")); });
            opt.setOnMouseExited(e -> { if (!answered && !key.equals(selectedChoice))
                opt.setStyle(opt.getStyle().replace("#EAEAEA","#F5F5F5")); });
            optionsArea.getChildren().add(opt);
        }
    }

    private void construireVraiFaux() {
        tfArea.setVisible(true); tfArea.setManaged(true);
        tfArea.getChildren().clear();
        tfArea.getChildren().addAll(btnTF(">> VRAI", true), btnTF(">> FAUX", false));
    }

    private Button btnTF(String label, boolean val) {
        Button b = new Button(label); b.setId("tf-" + val);
        b.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(b, Priority.ALWAYS);
        b.setStyle("-fx-font-family:'Courier New'; -fx-font-size:14; -fx-font-weight:bold;" +
                   "-fx-background-color:#F5F5F5; -fx-border-color:#CCCCCC; -fx-border-width:2;" +
                   "-fx-background-radius:0; -fx-border-radius:0; -fx-padding:16; -fx-cursor:hand;" +
                   "-fx-text-fill:#1A1A1A;");
        b.setOnAction(e -> selectionnerTF(val));
        return b;
    }

    private void construireTexte() {
        texteArea.setVisible(true); texteArea.setManaged(true);
        taReponse.clear();
        taReponse.textProperty().addListener((obs, old, v) ->
            btnValider.setDisable(v.trim().length() < 2));
    }

    // ── Sélection ─────────────────────────────────────────────

    private void selectionnerChoix(String key) {
        if (answered) return;
        selectedChoice = key;
        optionsArea.getChildren().forEach(n -> {
            String k = n.getId() == null ? "" : n.getId().replace("opt-","");
            n.setStyle(k.equals(key) ? styleOptSelected() : styleOpt(false));
        });
        btnValider.setDisable(false);
    }

    private void selectionnerTF(boolean val) {
        if (answered) return;
        selectedTF = val;
        tfArea.getChildren().forEach(n -> {
            Button b = (Button) n;
            b.setStyle(b.getId().equals("tf-" + val) ? styleTFSelected() : styleTF());
        });
        btnValider.setDisable(false);
    }

    // ── Validation ────────────────────────────────────────────

    @FXML private void valider() {
        if (answered) return;
        answered = true; stopTimer();
        Question q = questions.get(qIndex);
        boolean correct; String userAnswer;

        switch (q.getType()) {
            case "choix_multiple" -> {
                userAnswer = selectedChoice;
                correct    = q.getBonneReponseChoix() != null
                          && q.getBonneReponseChoix().equals(selectedChoice);
                coloriserChoix(q.getBonneReponseChoix(), selectedChoice, correct);
            }
            case "vrai_faux" -> {
                userAnswer = selectedTF == null ? "?" : (selectedTF ? "Vrai" : "Faux");
                correct    = selectedTF != null && selectedTF.equals(q.getBonneReponseBool());
                coloriserTF(q.getBonneReponseBool(), selectedTF);
            }
            default -> {
                userAnswer = taReponse.getText().trim();
                correct    = verifierTexte(userAnswer, q.getReponseAttendue());
                taReponse.setStyle(taReponse.getStyle() +
                    (correct ? "-fx-border-color:#0A8A5A;" : "-fx-border-color:#C0222E;"));
            }
        }

        int pts = correct ? getPoints(q.getNiveau()) : 0;
        if (correct) { scoreCorrect++; scorePoints += pts; }
        resultItems.add(new ResultItem(q, userAnswer, correct, pts));
        afficherFeedback(correct, q, pts);

        btnValider.setVisible(false); btnValider.setManaged(false);
        btnSuivant.setVisible(true);  btnSuivant.setManaged(true);
    }

    private void coloriserChoix(String bonne, String choisie, boolean correct) {
        optionsArea.getChildren().forEach(n -> {
            if (n.getId() == null) return;
            String k = n.getId().replace("opt-","");
            if (k.equals(bonne))
                n.setStyle("-fx-background-color:#D4F5E4; -fx-border-color:#0A8A5A; -fx-border-width:3; -fx-padding:12 14;");
            else if (k.equals(choisie) && !correct)
                n.setStyle("-fx-background-color:#FFE0E3; -fx-border-color:#C0222E; -fx-border-width:3; -fx-padding:12 14;");
        });
    }

    private void coloriserTF(Boolean bonne, Boolean choisie) {
        tfArea.getChildren().forEach(n -> {
            Button b = (Button) n;
            boolean isOk    = b.getId().equals("tf-" + bonne);
            boolean isWrong = choisie != null && !choisie.equals(bonne)
                           && b.getId().equals("tf-" + choisie);
            String base = "-fx-font-family:'Courier New'; -fx-font-size:14; -fx-font-weight:bold;" +
                          "-fx-background-radius:0; -fx-border-radius:0; -fx-padding:16; -fx-text-fill:#1A1A1A;";
            if      (isOk)    b.setStyle(base + "-fx-background-color:#D4F5E4; -fx-border-color:#0A8A5A; -fx-border-width:3;");
            else if (isWrong) b.setStyle(base + "-fx-background-color:#FFE0E3; -fx-border-color:#C0222E; -fx-border-width:3;");
        });
    }

    private void afficherFeedback(boolean correct, Question q, int pts) {
        feedbackBox.setVisible(true); feedbackBox.setManaged(true);
        feedbackBox.setStyle("-fx-background-color:" + (correct ? "#D4F5E4" : "#FFE0E3") + ";" +
                             "-fx-border-color:" + (correct ? "#0A8A5A" : "#C0222E") + ";" +
                             "-fx-border-width:0 0 0 4; -fx-padding:10 14;");
        lblFeedback.setText(correct
            ? "OK — Bonne reponse ! (+" + pts + " pts)"
            : "ERREUR — Bonne reponse : " + getBonneLabel(q));
        lblFeedback.setStyle("-fx-font-family:'Courier New'; -fx-font-size:12; -fx-font-weight:bold;" +
                             "-fx-text-fill:" + (correct ? "#0A8A5A" : "#C0222E") + ";");
    }

    @FXML private void suivant() {
        qIndex++;
        if (qIndex >= questions.size()) afficherResultats();
        else afficherQuestion();
    }

    // ── Timer ─────────────────────────────────────────────────

    private void demarrerTimer() {
        stopTimer();
        timeLeft = timePerQ;
        pbTimer.setProgress(1.0);
        updateTimerUI();
        timerAnim = new Timeline(new KeyFrame(Duration.seconds(timePerQ),
            new KeyValue(pbTimer.progressProperty(), 0.0)));
        timerAnim.play();

        javafx.animation.AnimationTimer cd = new javafx.animation.AnimationTimer() {
            long last = 0;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                if (now - last >= 1_000_000_000L) {
                    timeLeft--; last = now;
                    Platform.runLater(() -> updateTimerUI());
                    if (timeLeft <= 0) { stop(); Platform.runLater(() -> expirer()); }
                }
            }
        };
        cd.start();
        pbTimer.setUserData(cd);
    }

    private void stopTimer() {
        if (timerAnim != null) timerAnim.stop();
        if (pbTimer.getUserData() instanceof javafx.animation.AnimationTimer at) at.stop();
    }

    private void updateTimerUI() {
        lblTimerVal.setText(String.format("%d:%02d", timeLeft / 60, timeLeft % 60));
        double pct = (double) timeLeft / timePerQ;
        String col = pct > 0.5 ? "#C47A00" : pct > 0.25 ? "#CC6600" : "#C0222E";
        lblTimerVal.setStyle("-fx-font-family:'Courier New'; -fx-font-size:16; " +
                             "-fx-font-weight:bold; -fx-text-fill:" + col + ";");
        pbTimer.setStyle("-fx-pref-height:6; -fx-accent:" + col + "; -fx-background-color:#DDDDDD;");
    }

    private void expirer() {
        if (answered) return;
        answered = true;
        resultItems.add(new ResultItem(questions.get(qIndex), "—", false, 0));
        feedbackBox.setVisible(true); feedbackBox.setManaged(true);
        feedbackBox.setStyle("-fx-background-color:#FFE0E3; -fx-border-color:#C0222E;" +
                             "-fx-border-width:0 0 0 4; -fx-padding:10 14;");
        lblFeedback.setText("TEMPS ECOULE — Bonne reponse : " + getBonneLabel(questions.get(qIndex)));
        lblFeedback.setStyle("-fx-font-family:'Courier New'; -fx-font-size:12; " +
                             "-fx-font-weight:bold; -fx-text-fill:#C0222E;");
        btnValider.setVisible(false); btnValider.setManaged(false);
        btnSuivant.setVisible(true);  btnSuivant.setManaged(true);
    }

    // ── Progress dots ─────────────────────────────────────────

    private void construireProgressRow() {
        progressRow.getChildren().clear();
        for (int i = 0; i < questions.size(); i++) {
            Region dot = new Region(); dot.setId("dot-" + i);
            dot.setPrefHeight(4); HBox.setHgrow(dot, Priority.ALWAYS);
            dot.setStyle("-fx-background-color:#DDDDDD;");
            progressRow.getChildren().add(dot);
        }
    }

    private void mettreAJourProgressRow() {
        for (int i = 0; i < questions.size(); i++) {
            Node d = progressRow.lookup("#dot-" + i);
            if (d == null) continue;
            String col = i < qIndex ? "#C47A00" : (i == qIndex ? "#1A1A1A" : "#DDDDDD");
            d.setStyle("-fx-background-color:" + col + ";");
        }
    }

    // ── Résultats ─────────────────────────────────────────────

    private void afficherResultats() {
        resolveCA();
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/UserResultsView.fxml")
            );
            Node vue = loader.load();
            UserResultsController ctrl = loader.getController();
            ctrl.setData(quiz, resultItems, scoreCorrect, scorePoints);
            ctrl.setContentArea(contentArea);
            contentArea.getChildren().setAll(vue);
            ctrl.afficher();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                "Erreur affichage résultats : " + e.getMessage()).show();
        }
    }

    @FXML private void retourHome() {
        stopTimer(); resolveCA();
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/UserHomeView.fxml")
            );
            Node vue = loader.load();
            UserHomeController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            contentArea.getChildren().setAll(vue);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur retour : " + e.getMessage()).show();
        }
    }

    // ← null-safe lookup
    private void resolveCA() {
        if (contentArea == null && progressRow.getScene() != null)
            contentArea = (StackPane) progressRow.getScene().lookup("#contentArea");
    }

    // ── Style helpers ─────────────────────────────────────────

    private String styleOpt(boolean hover) {
        return "-fx-background-color:" + (hover ? "#EAEAEA" : "#F5F5F5") +
               "; -fx-border-color:#CCCCCC; -fx-border-width:2; -fx-padding:12 14; -fx-cursor:hand;";
    }
    private String styleOptSelected() {
        return "-fx-background-color:#FFF0CC; -fx-border-color:#C47A00; -fx-border-width:3; -fx-padding:12 14; -fx-cursor:hand;";
    }
    private String styleTF() {
        return "-fx-font-family:'Courier New'; -fx-font-size:14; -fx-font-weight:bold;" +
               "-fx-background-color:#F5F5F5; -fx-border-color:#CCCCCC; -fx-border-width:2;" +
               "-fx-background-radius:0; -fx-border-radius:0; -fx-padding:16; -fx-cursor:hand; -fx-text-fill:#1A1A1A;";
    }
    private String styleTFSelected() {
        return "-fx-font-family:'Courier New'; -fx-font-size:14; -fx-font-weight:bold;" +
               "-fx-background-color:#FFF0CC; -fx-border-color:#C47A00; -fx-border-width:3;" +
               "-fx-background-radius:0; -fx-border-radius:0; -fx-padding:16; -fx-cursor:hand; -fx-text-fill:#1A1A1A;";
    }

    // ── Helpers métier ────────────────────────────────────────

    private String getTypeLabel(String t) {
        return switch (t) {
            case "choix_multiple" -> "CHOIX MULTIPLE";
            case "vrai_faux"      -> "VRAI / FAUX";
            case "texte"          -> "TEXTE LIBRE";
            default               -> t.toUpperCase();
        };
    }

    private int getPoints(String n) {
        return switch (n == null ? "" : n) {
            case "moyen"     -> 2;
            case "difficile" -> 3;
            default          -> 1;
        };
    }

    private boolean verifierTexte(String rep, String att) {
        if (rep == null || att == null) return false;
        String r = rep.trim().toLowerCase(), a = att.trim().toLowerCase();
        if (r.equals(a)) return true;
        String[] mots = a.split("\\s+"); int m = 0;
        for (String w : mots) if (w.length() > 3 && r.contains(w)) m++;
        return m >= Math.max(1, mots.length / 3);
    }

    private String getBonneLabel(Question q) {
        return switch (q.getType()) {
            case "choix_multiple" -> switch (q.getBonneReponseChoix() == null ? "" : q.getBonneReponseChoix()) {
                case "a" -> q.getChoixA(); case "b" -> q.getChoixB();
                case "c" -> q.getChoixC(); case "d" -> q.getChoixD();
                default  -> "?";
            };
            case "vrai_faux" -> Boolean.TRUE.equals(q.getBonneReponseBool()) ? "Vrai" : "Faux";
            default -> q.getReponseAttendue() != null ? q.getReponseAttendue() : "?";
        };
    }

    public record ResultItem(Question question, String userAnswer, boolean correct, int points) {}
}
