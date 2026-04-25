package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Question;
import edu.connexion3a36.services.SmartValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * QuestionTexteLibreController
 * ════════════════════════════
 * Controller pour l'affichage et la correction d'une question de type "texte".
 *
 * Intégration dans votre FXML de passage de quiz :
 *   - Lier les fx:id ci-dessous à votre vue
 *   - Appeler setQuestion(q) depuis le controller parent
 *   - Appeler getResult() pour récupérer le score (0 ou 1)
 *
 * La correction est asynchrone : l'UI n'est jamais bloquée pendant
 * l'appel à l'API Groq.
 */
public class QuestionTexteLibreController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label     lblQuestion;      // texte de la question
    @FXML private Label     lblIndice;        // indice (facultatif)
    @FXML private TextArea  taReponse;        // saisie étudiant
    @FXML private Button    btnValider;       // bouton Valider
    @FXML private VBox      vboxResultat;     // zone résultat (cachée au départ)
    @FXML private Label     lblIcone;         // ✅ ou ❌
    @FXML private Label     lblExplication;   // explication de la correction
    @FXML private Label     lblSource;        // "Corrigé par IA" ou "Corrigé littéralement"
    @FXML private ProgressIndicator piCharge; // spinner pendant l'appel IA

    // ── État ──────────────────────────────────────────────────────────────────
    private Question question;
    private SmartValidator.Result dernierResultat;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        vboxResultat.setVisible(false);
        piCharge.setVisible(false);
        btnValider.setDisable(true);

        // Active le bouton dès que l'étudiant tape quelque chose
        taReponse.textProperty().addListener((obs, old, val) ->
            btnValider.setDisable(val == null || val.trim().isEmpty())
        );
    }

    /**
     * Injecte la question à afficher — à appeler depuis le controller parent.
     */
    public void setQuestion(Question q) {
        this.question = q;
        lblQuestion.setText(q.getTexte());

        if (q.getIndice() != null && !q.getIndice().isBlank()) {
            lblIndice.setText("💡 Indice : " + q.getIndice());
            lblIndice.setVisible(true);
        } else {
            lblIndice.setVisible(false);
        }
    }

    // ── Action Valider ────────────────────────────────────────────────────────

    @FXML
    public void validerReponse() {
        if (question == null) return;

        String reponseDonnee   = taReponse.getText().trim();
        String reponseAttendue = question.getReponseAttendue();

        // UI — mode chargement
        btnValider.setDisable(true);
        taReponse.setEditable(false);
        vboxResultat.setVisible(false);
        piCharge.setVisible(true);

        // Appel asynchrone pour ne pas bloquer le fil JavaFX
        new Thread(() -> {
            SmartValidator.Result result =
                SmartValidator.valider(reponseAttendue, reponseDonnee);

            // Retour sur le fil UI
            Platform.runLater(() -> afficherResultat(result));
        }).start();
    }

    // ── Affichage du résultat ─────────────────────────────────────────────────

    private void afficherResultat(SmartValidator.Result result) {
        this.dernierResultat = result;

        piCharge.setVisible(false);
        vboxResultat.setVisible(true);

        if (result.correct()) {
            lblIcone.setText("✅");
            lblIcone.setStyle("-fx-font-size:32; -fx-text-fill:#2E7D32;");
            vboxResultat.setStyle(
                "-fx-background-color:#E8F5E9; -fx-border-color:#2E7D32;" +
                "-fx-border-radius:8; -fx-background-radius:8; -fx-padding:12;"
            );
        } else {
            lblIcone.setText("❌");
            lblIcone.setStyle("-fx-font-size:32; -fx-text-fill:#C62828;");
            vboxResultat.setStyle(
                "-fx-background-color:#FFEBEE; -fx-border-color:#C62828;" +
                "-fx-border-radius:8; -fx-background-radius:8; -fx-padding:12;"
            );
        }

        lblExplication.setText(result.explication());

        lblSource.setText(
            "AI".equals(result.source())
                ? "🤖 Corrigé par intelligence artificielle"
                : "📝 Corrigé par comparaison de texte"
        );
        lblSource.setStyle("-fx-font-size:10; -fx-text-fill:#757575; -fx-font-style:italic;");
    }

    // ── Accesseurs pour le controller parent ──────────────────────────────────

    /**
     * Retourne le score : 1 si correct, 0 sinon.
     * Retourne -1 si l'étudiant n'a pas encore répondu.
     */
    public int getScore() {
        if (dernierResultat == null) return -1;
        return dernierResultat.correct() ? 1 : 0;
    }

    /** Retourne true si la question a été répondue et validée. */
    public boolean estValidee() {
        return dernierResultat != null;
    }
}
