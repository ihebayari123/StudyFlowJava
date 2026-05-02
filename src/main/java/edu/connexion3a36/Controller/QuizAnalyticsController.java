package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.QuizAnalyticsService;
import edu.connexion3a36.services.QuizAnalyticsService.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

/**
 * QuizAnalyticsController
 * ═══════════════════════
 * Tableau de bord d'analyse local — zéro API.
 * Affiche des insights tirés de quiz_attempt.
 *
 * Mode ÉTUDIANT  → stats personnelles, progression, conseils
 * Mode ENSEIGNANT→ stats globales, alertes, leaderboard
 *
 * Appelé depuis FitnessDashboardController ou DashboardController :
 *   ctrl.setUtilisateur(u, contentArea);
 */
public class QuizAnalyticsController {

    // ── FXML communs ──────────────────────────────────────────────────────────
    @FXML private Label       lblTitreDashboard;
    @FXML private HBox        hboxKpis;
    @FXML private VBox        vboxConseils;
    @FXML private Label       lblConseils;
    @FXML private VBox        vboxAlertes;
    @FXML private LineChart<String, Number> chartProgression;
    @FXML private BarChart<String, Number>  chartQuiz;
    @FXML private VBox        vboxLeaderboard;
    @FXML private VBox        vboxQuizNonTentes;

    // ── État ──────────────────────────────────────────────────────────────────
    private Utilisateur                utilisateur;
    private StackPane                  contentArea;
    private final QuizAnalyticsService analytics = new QuizAnalyticsService();

    // ── Init ──────────────────────────────────────────────────────────────────

    public void setUtilisateur(Utilisateur u, StackPane contentArea) {
        this.utilisateur  = u;
        this.contentArea  = contentArea;
        String role = u != null ? u.getRole() : "";
        boolean isEtudiant = role.contains("ETUDIANT") || role.contains("etudiant");

        if (isEtudiant) {
            u.setId(1L);
            afficherDashboardEtudiant(u.getId());
        } else {
            afficherDashboardEnseignant();
        }
    }

    @FXML
    private void retourAccueil() {
        if (contentArea == null) return;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/views/UserHomeView.fxml")
            );
            javafx.scene.Node vue = loader.load();
            UserHomeController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setUtilisateur(utilisateur);
            contentArea.getChildren().setAll(vue);
        } catch (java.io.IOException e) {
            new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Erreur retour : " + e.getMessage()
            ).show();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DASHBOARD ÉTUDIANT
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherDashboardEtudiant(long userId) {
        if (lblTitreDashboard != null)
            lblTitreDashboard.setText("📊 Mon tableau de bord");

        Map<String, Object> resume = analytics.getResumEtudiant(userId);
        afficherKpisEtudiant(resume);

        List<QuizStat> pires    = analytics.getPiresQuizEtudiant(userId, 5);
        List<QuizStat> meilleurs = analytics.getMeilleursQuizEtudiant(userId, 5);
        afficherBarreQuiz(meilleurs, "Meilleurs quiz (score moyen %)");

        afficherQuizNonTentes(analytics.getQuizNonTentes(userId));

        String conseil = analytics.genererConseilLocal(userId);
        if (lblConseils != null) lblConseils.setText(conseil);

        if (vboxConseils != null) {
            vboxConseils.setVisible(true);
            vboxConseils.setManaged(true);
        }
    }

    private void afficherKpisEtudiant(Map<String, Object> r) {
        if (hboxKpis == null) return;
        hboxKpis.getChildren().clear();

        hboxKpis.getChildren().addAll(
                kpi("🎯", "Score moyen",
                        Math.round((Double) r.getOrDefault("score_moyen_pct", 0.0)) + "%",
                        colorScore((Double) r.getOrDefault("score_moyen_pct", 0.0))),
                kpi("📝", "Tentatives",
                        String.valueOf(r.getOrDefault("nb_tentatives", 0)), "#1565C0"),
                kpi("📚", "Quiz tentés",
                        String.valueOf(r.getOrDefault("nb_quiz_distincts", 0)), "#4A148C"),
                kpi("✅", "Taux de réussite",
                        r.getOrDefault("taux_reussite_pct", 0) + "%",
                        colorScore(((Number) r.getOrDefault("taux_reussite_pct", 0)).doubleValue())),
                kpi("📈", "Tendance",
                        tendanceLabel((String) r.getOrDefault("tendance", "?")),
                        tendanceCouleur((String) r.getOrDefault("tendance", "?")))
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DASHBOARD ENSEIGNANT
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherDashboardEnseignant() {
        if (lblTitreDashboard != null)
            lblTitreDashboard.setText("📊 Tableau de bord — Vue enseignant");

        Map<String, Object> global = analytics.getStatsGlobalesRapides();
        afficherKpisEnseignant(global);

        List<QuizStat> stats = analytics.getStatsGlobalesParQuiz();
        afficherBarreQuiz(stats, "Taux de réussite par quiz (%)");

        List<AlerteEtudiant> alertes = analytics.getEtudiantsEnDifficulte(40.0, 2);
        afficherAlertes(alertes);

        if (vboxConseils != null) {
            vboxConseils.setVisible(false);
            vboxConseils.setManaged(false);
        }
    }

    private void afficherKpisEnseignant(Map<String, Object> g) {
        if (hboxKpis == null) return;
        hboxKpis.getChildren().clear();

        hboxKpis.getChildren().addAll(
                kpi("👥", "Étudiants actifs",
                        String.valueOf(g.getOrDefault("nb_etudiants_actifs", 0)), "#0D47A1"),
                kpi("📝", "Total tentatives",
                        String.valueOf(g.getOrDefault("total_tentatives", 0)), "#1B5E20"),
                kpi("🎯", "Score moyen global",
                        Math.round(((Number) g.getOrDefault("score_moyen_global", 0)).doubleValue()) + "%",
                        colorScore(((Number) g.getOrDefault("score_moyen_global", 0)).doubleValue())),
                kpi("✅", "Taux de réussite",
                        g.getOrDefault("taux_reussite_global", 0) + "%",
                        colorScore(((Number) g.getOrDefault("taux_reussite_global", 0)).doubleValue())),
                kpi("⏱️", "Temps moyen",
                        g.getOrDefault("temps_moyen_min", 0) + " min", "#4A148C")
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ALERTES
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherAlertes(List<AlerteEtudiant> alertes) {
        if (vboxAlertes == null) return;
        vboxAlertes.getChildren().clear();

        if (alertes.isEmpty()) {
            Label ok = new Label("✅ Aucun étudiant en difficulté détecté.");
            ok.setStyle("-fx-font-size:13; -fx-text-fill:-color-text-secondary;");
            vboxAlertes.getChildren().add(ok);
            return;
        }

        Label titre = new Label("⚠️ Étudiants en difficulté (" + alertes.size() + ")");
        titre.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill:#C62828;");
        vboxAlertes.getChildren().add(titre);

        for (AlerteEtudiant a : alertes) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#FFEBEE; -fx-background-radius:8; -fx-padding:10 14;");

            VBox info = new VBox(3);
            Label nom = new Label(a.nomComplet() + " — " + a.quizTitre());
            nom.setStyle("-fx-font-size:13; -fx-font-weight:bold; -fx-text-fill:#B71C1C;");
            Label det = new Label(Math.round(a.scoreMoyen()) + "% de moyenne sur " + a.nbTentatives() + " tentatives");
            det.setStyle("-fx-font-size:11; -fx-text-fill:#C62828;");
            info.getChildren().addAll(nom, det);

            row.getChildren().add(info);
            vboxAlertes.getChildren().add(row);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GRAPHIQUE BARRE
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherBarreQuiz(List<QuizStat> stats, String axeLabel) {
        if (chartQuiz == null || stats.isEmpty()) return;
        chartQuiz.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(axeLabel);

        for (QuizStat q : stats) {
            String label = q.quizTitre().length() > 14
                    ? q.quizTitre().substring(0, 12) + "…"
                    : q.quizTitre();
            series.getData().add(new XYChart.Data<>(label, Math.round(q.scoreMoyen())));
        }
        chartQuiz.getData().add(series);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // QUIZ NON TENTÉS
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherQuizNonTentes(List<Map<String, Object>> liste) {
        if (vboxQuizNonTentes == null) return;
        vboxQuizNonTentes.getChildren().clear();

        if (liste.isEmpty()) {
            Label ok = new Label("✅ Vous avez tenté tous les quiz disponibles !");
            ok.setStyle("-fx-font-size:13;");
            vboxQuizNonTentes.getChildren().add(ok);
            return;
        }

        Label titre = new Label("📋 Quiz non encore tentés (" + liste.size() + ")");
        titre.setStyle("-fx-font-size:14; -fx-font-weight:bold;");
        vboxQuizNonTentes.getChildren().add(titre);

        for (Map<String, Object> q : liste) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#E3F2FD; -fx-background-radius:8; -fx-padding:8 14;");

            Label nom  = new Label("📝 " + q.get("titre"));
            nom.setStyle("-fx-font-size:13; -fx-font-weight:bold; -fx-text-fill:#0D47A1;");
            Label info = new Label(q.get("duree") + " min · " + q.get("nb_questions") + " questions");
            info.setStyle("-fx-font-size:11; -fx-text-fill:#1565C0;");

            HBox.setHgrow(nom, Priority.ALWAYS);
            row.getChildren().addAll(nom, info);
            vboxQuizNonTentes.getChildren().add(row);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private VBox kpi(String emoji, String label, String valeur, String couleur) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(130);
        box.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#EEEEEE; -fx-border-radius:16; -fx-border-width:1.5;"
                + "-fx-padding:18 12;");

        Label ico = new Label(emoji);
        ico.setStyle("-fx-font-size:22;");

        Label val = new Label(valeur);
        val.setStyle("-fx-font-size:22; -fx-font-weight:bold; -fx-text-fill:" + couleur + ";");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:11; -fx-text-fill:#757575;");

        box.getChildren().addAll(ico, val, lbl);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String colorScore(double pct) {
        if (pct >= 80) return "#2E7D32";
        if (pct >= 60) return "#1565C0";
        if (pct >= 40) return "#E65100";
        return "#C62828";
    }

    private String tendanceLabel(String t) {
        return switch (t) {
            case "progression" -> "↑ +";
            case "régression"  -> "↓ -";
            case "stable"      -> "→ =";
            default            -> "?";
        };
    }


    private String tendanceCouleur(String t) {
        return switch (t) {
            case "progression" -> "#2E7D32";
            case "régression"  -> "#C62828";
            default            -> "#757575";
        };
    }
}