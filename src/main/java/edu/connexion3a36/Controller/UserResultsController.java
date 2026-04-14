package edu.connexion3a36.Controller;

import edu.connexion3a36.Controller.UserQuizController.ResultItem;
import edu.connexion3a36.entities.Quiz;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.List;

public class UserResultsController {

    @FXML private Label lblScorePct;
    @FXML private Label lblResultTitle;
    @FXML private Label lblResultMsg;
    @FXML private Label lblTotal;
    @FXML private Label lblCorrect;
    @FXML private Label lblWrong;
    @FXML private Label lblPoints;
    @FXML private VBox  reviewList;

    private Quiz             quiz;
    private List<ResultItem> items;
    private int              scoreCorrect;
    private int              scorePoints;
    private StackPane        contentArea;

    public void setContentArea(StackPane sp) { this.contentArea = sp; }
    public void setData(Quiz q, List<ResultItem> it, int sc, int sp) {
        this.quiz = q; this.items = it; this.scoreCorrect = sc; this.scorePoints = sp;
    }

    @FXML public void initialize() {}

    // Appelé après setData() depuis UserQuizController
    public void afficher() {
        if (items == null || items.isEmpty()) return;
        int total = items.size();
        int wrong = total - scoreCorrect;
        int pct   = (int) Math.round((scoreCorrect * 100.0) / total);

        lblScorePct.setText(pct + "%");
        String[] msg = getMessage(pct);
        lblResultTitle.setText(msg[0]);
        lblResultMsg  .setText(msg[1]);

        String col = pct >= 80 ? "#C47A00" : pct >= 50 ? "#0A5A8A" : "#C0222E";
        lblScorePct.setStyle("-fx-font-family:'Courier New'; -fx-font-size:38;" +
                             "-fx-font-weight:bold; -fx-text-fill:" + col + ";");

        lblTotal  .setText(String.valueOf(total));
        lblCorrect.setText(String.valueOf(scoreCorrect));
        lblWrong  .setText(String.valueOf(wrong));
        lblPoints .setText(scorePoints + " pts");

        construireRecap();
    }

    private void construireRecap() {
        reviewList.getChildren().clear();
        for (int i = 0; i < items.size(); i++) {
            ResultItem item = items.get(i);
            HBox row = new HBox(12); row.setAlignment(Pos.TOP_LEFT);
            row.setStyle("-fx-padding:12 0; -fx-border-color:transparent transparent #EBEBEB transparent; -fx-border-width:1;");

            Circle dot = new Circle(5);
            dot.setFill(item.correct() ? Color.web("#0A8A5A")
                : (item.userAnswer().equals("—") ? Color.web("#AAAAAA") : Color.web("#C0222E")));
            dot.setTranslateY(5);

            VBox content = new VBox(3); HBox.setHgrow(content, Priority.ALWAYS);
            String texteQ = item.question().getTexte();
            Label qLbl = new Label((i+1) + ". " + (texteQ.length() > 80 ? texteQ.substring(0,80) + "..." : texteQ));
            qLbl.setStyle("-fx-font-family:'Courier New'; -fx-font-size:12; -fx-font-weight:bold;" +
                          "-fx-text-fill:#1A1A1A;"); qLbl.setWrapText(true);

            HBox ansRow = new HBox(6); ansRow.setAlignment(Pos.CENTER_LEFT);
            if (item.correct()) {
                Label a = new Label("OK -- " + fmt(item));
                a.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-text-fill:#0A8A5A;");
                ansRow.getChildren().add(a);
            } else if (item.userAnswer().equals("—")) {
                Label a = new Label("TEMPS -- bonne: " + getBonne(item));
                a.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-text-fill:#AAAAAA;");
                ansRow.getChildren().add(a);
            } else {
                Label a = new Label("ERR -- " + fmt(item));
                a.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-text-fill:#C0222E;");
                Label ar = new Label(" >> " + getBonne(item));
                ar.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-text-fill:#0A8A5A;");
                ansRow.getChildren().addAll(a, ar);
            }
            content.getChildren().addAll(qLbl, ansRow);

            Label pts = new Label("+" + item.points() + "pt");
            pts.setStyle("-fx-font-family:'Courier New'; -fx-font-size:9; -fx-font-weight:bold; -fx-padding:2 6;" +
                         "-fx-background-color:" + (item.correct() ? "#FFF0CC" : "#F0F0F0") + ";" +
                         "-fx-border-color:" + (item.correct() ? "#C47A00" : "#CCCCCC") + ";" +
                         "-fx-border-width:1; -fx-text-fill:" + (item.correct() ? "#C47A00" : "#999999") + ";");

            row.getChildren().addAll(dot, content, pts);
            reviewList.getChildren().add(row);
        }
    }

    @FXML public void rejouer() {
        resolveCA(); if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/UserQuizView.fxml")
            );
            Node vue = loader.load();
            UserQuizController ctrl = loader.getController();
            ctrl.setQuiz(quiz); ctrl.setContentArea(contentArea);
            contentArea.getChildren().setAll(vue);
            ctrl.demarrer();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur rejouer : " + e.getMessage()).show();
        }
    }

    @FXML public void retourHome() {
        resolveCA(); if (contentArea == null) return;
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

    private void resolveCA() {
        if (contentArea == null && lblScorePct.getScene() != null)
            contentArea = (StackPane) lblScorePct.getScene().lookup("#contentArea");
    }

    private String[] getMessage(int p) {
        if (p >= 80) return new String[]{"EXCELLENT !", "Parfaite maitrise. Continuez !"};
        if (p >= 60) return new String[]{"BIEN !", "Bon resultat. Quelques points a revoir."};
        if (p >= 40) return new String[]{"A REVOIR", "Revisez et reessayez !"};
        return new String[]{"COURAGE !", "N'abandonnez pas !"};
    }

    private String fmt(ResultItem item) {
        String a = item.userAnswer();
        if (a == null || a.isBlank()) return "--";
        if ("choix_multiple".equals(item.question().getType()))
            return switch (a) {
                case "a" -> item.question().getChoixA(); case "b" -> item.question().getChoixB();
                case "c" -> item.question().getChoixC(); case "d" -> item.question().getChoixD();
                default  -> a;
            };
        return a.length() > 50 ? a.substring(0,50) + "..." : a;
    }

    private String getBonne(ResultItem item) {
        return switch (item.question().getType()) {
            case "choix_multiple" -> switch (item.question().getBonneReponseChoix() == null ? "" : item.question().getBonneReponseChoix()) {
                case "a" -> item.question().getChoixA(); case "b" -> item.question().getChoixB();
                case "c" -> item.question().getChoixC(); case "d" -> item.question().getChoixD();
                default  -> "?";
            };
            case "vrai_faux" -> Boolean.TRUE.equals(item.question().getBonneReponseBool()) ? "Vrai" : "Faux";
            default -> item.question().getReponseAttendue() != null ? item.question().getReponseAttendue() : "?";
        };
    }
}
