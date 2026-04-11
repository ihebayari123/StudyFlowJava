package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Quiz;
import edu.connexion3a36.services.QuizService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

public class QuizController {

    // ── Table ──────────────────────────────────────────────────
    @FXML private TableView<Quiz>            tableQuiz;
    @FXML private TableColumn<Quiz, Integer> colId;
    @FXML private TableColumn<Quiz, String>  colTitre;
    @FXML private TableColumn<Quiz, Integer> colDuree;
    @FXML private TableColumn<Quiz, Integer> colCourseId;

    // ── Formulaire ────────────────────────────────────────────
    @FXML private TextField tfTitre;
    @FXML private TextField tfDuree;
    @FXML private TextField tfCourseId;
    @FXML private Label     lblErreurTitre;
    @FXML private Label     lblErreurDuree;
    @FXML private Label     lblErreurCourseId;   // ← nouveau label (à ajouter dans le FXML)

    // ── Recherche & Tri ───────────────────────────────────────
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbTri;

    // ── Statistiques ──────────────────────────────────────────
    @FXML private Label lblTotalQuiz;
    @FXML private Label lblMoyenneDuree;

    // ── Constantes de validation ──────────────────────────────
    private static final int TITRE_MIN   = 3;
    private static final int TITRE_MAX   = 100;
    private static final int DUREE_MIN   = 1;
    private static final int DUREE_MAX   = 180;

    // ── Service ───────────────────────────────────────────────
    private final QuizService service = new QuizService();
    private ObservableList<Quiz> quizList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory      (new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory   (new PropertyValueFactory<>("titre"));
        colDuree.setCellValueFactory   (new PropertyValueFactory<>("duree"));
        colCourseId.setCellValueFactory(new PropertyValueFactory<>("courseId"));

        cbTri.setItems(FXCollections.observableArrayList(
            "Date (récent)", "Titre A→Z", "Durée (croissant)"
        ));
        cbTri.setValue("Date (récent)");

        chargerQuiz();

        tableQuiz.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) remplirFormulaire(sel); }
        );

        // Validation en temps réel sur chaque champ
        tfTitre.textProperty().addListener((obs, old, val) -> validerTitre(val));
        tfDuree.textProperty().addListener((obs, old, val) -> validerDuree(val));
        tfCourseId.textProperty().addListener((obs, old, val) -> validerCourseId(val));

        tfRecherche.textProperty().addListener((obs, old, val) -> filtrer(val));
        cbTri.setOnAction(e -> trierQuiz());
    }

    // ── CRUD ──────────────────────────────────────────────────

    @FXML
    public void ajouterQuiz() {
        if (!validerFormulaire()) return;
        try {
            Quiz q = new Quiz(
                tfTitre.getText().trim(),
                Integer.parseInt(tfDuree.getText().trim()),
                Integer.parseInt(tfCourseId.getText().trim())
            );
            service.add(q);
            chargerQuiz();
            viderFormulaire();
            succes("Quiz ajouté avec succès !");
        } catch (Exception e) {
            erreur(e.getMessage());
        }
    }

    @FXML
    public void modifierQuiz() {
        Quiz sel = tableQuiz.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Sélectionnez un quiz dans le tableau."); return; }
        if (!validerFormulaire()) return;
        try {
            sel.setTitre   (tfTitre.getText().trim());
            sel.setDuree   (Integer.parseInt(tfDuree.getText().trim()));
            sel.setCourseId(Integer.parseInt(tfCourseId.getText().trim()));
            service.update(sel);
            chargerQuiz();
            viderFormulaire();
            succes("Quiz modifié !");
        } catch (Exception e) {
            erreur(e.getMessage());
        }
    }

    @FXML
    public void supprimerQuiz() {
        Quiz sel = tableQuiz.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Sélectionnez un quiz dans le tableau."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer \"" + sel.getTitre() + "\" et toutes ses questions ?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.delete(sel.getId());
                    chargerQuiz();
                    viderFormulaire();
                    succes("Quiz supprimé !");
                } catch (Exception e) { erreur(e.getMessage()); }
            }
        });
    }

    @FXML
    public void voirQuestions() {
        Quiz sel = tableQuiz.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Sélectionnez un quiz dans le tableau."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/QuestionView.fxml")
            );
            Node vue = loader.load();
            QuestionController qc = loader.getController();
            qc.setQuizId(sel.getId(), sel.getTitre());

            StackPane parent = (StackPane) tableQuiz.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (Exception e) {
            erreur("Erreur navigation : " + e.getMessage());
        }
    }

    // ── Validation en temps réel ───────────────────────────────

    /**
     * Valide le titre : non vide, entre TITRE_MIN et TITRE_MAX caractères,
     * pas uniquement des espaces, pas de caractères spéciaux dangereux.
     */
    private boolean validerTitre(String val) {
        String v = (val == null) ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurTitre, "Le titre est obligatoire.");
            return false;
        }
        if (v.length() < TITRE_MIN) {
            setErreur(lblErreurTitre, "Le titre doit avoir au moins " + TITRE_MIN + " caractères.");
            return false;
        }
        if (v.length() > TITRE_MAX) {
            setErreur(lblErreurTitre, "Le titre ne peut pas dépasser " + TITRE_MAX + " caractères.");
            return false;
        }
        clearErreur(lblErreurTitre);
        return true;
    }

    /**
     * Valide la durée : doit être un entier entre DUREE_MIN et DUREE_MAX.
     */
    private boolean validerDuree(String val) {
        String v = (val == null) ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurDuree, "La durée est obligatoire.");
            return false;
        }
        // Accepter uniquement des chiffres
        if (!v.matches("\\d+")) {
            setErreur(lblErreurDuree, "La durée doit être un nombre entier positif.");
            return false;
        }
        try {
            int d = Integer.parseInt(v);
            if (d < DUREE_MIN || d > DUREE_MAX) {
                setErreur(lblErreurDuree,
                    "La durée doit être entre " + DUREE_MIN + " et " + DUREE_MAX + " min.");
                return false;
            }
        } catch (NumberFormatException e) {
            setErreur(lblErreurDuree, "Valeur numérique trop grande.");
            return false;
        }
        clearErreur(lblErreurDuree);
        return true;
    }

    /**
     * Valide le courseId : doit être un entier strictement positif.
     */
    private boolean validerCourseId(String val) {
        if (lblErreurCourseId == null) return true; // label optionnel dans le FXML
        String v = (val == null) ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurCourseId, "L'ID du cours est obligatoire.");
            return false;
        }
        if (!v.matches("\\d+")) {
            setErreur(lblErreurCourseId, "L'ID du cours doit être un nombre entier positif.");
            return false;
        }
        try {
            int id = Integer.parseInt(v);
            if (id <= 0) {
                setErreur(lblErreurCourseId, "L'ID du cours doit être supérieur à 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            setErreur(lblErreurCourseId, "Valeur numérique trop grande.");
            return false;
        }
        clearErreur(lblErreurCourseId);
        return true;
    }

    /**
     * Validation globale avant soumission : vérifie tous les champs ensemble.
     */
    private boolean validerFormulaire() {
        boolean titre    = validerTitre   (tfTitre.getText());
        boolean duree    = validerDuree   (tfDuree.getText());
        boolean courseId = validerCourseId(tfCourseId.getText());
        return titre && duree && courseId;
    }

    // ── Recherche & Tri ───────────────────────────────────────

    private void filtrer(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                quizList.setAll(service.getAll());
            } else {
                quizList.setAll(service.search(keyword.trim()));
            }
            tableQuiz.setItems(quizList);
            mettreAJourStats();
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    private void trierQuiz() {
        try {
            String tri = cbTri.getValue();
            if      (tri == null)              quizList.setAll(service.getAll());
            else if (tri.startsWith("Titre"))  quizList.setAll(service.getAllSortedByTitre());
            else if (tri.startsWith("Durée"))  quizList.setAll(service.getAllSortedByDuree());
            else                               quizList.setAll(service.getAllSortedByDate());
            tableQuiz.setItems(quizList);
            mettreAJourStats();
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────

    private void chargerQuiz() {
        try {
            quizList.setAll(service.getAll());
            tableQuiz.setItems(quizList);
            mettreAJourStats();
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    private void mettreAJourStats() {
        if (lblTotalQuiz != null)
            lblTotalQuiz.setText(String.valueOf(quizList.size()));
        if (lblMoyenneDuree != null) {
            double moy = quizList.stream().mapToInt(Quiz::getDuree).average().orElse(0);
            lblMoyenneDuree.setText(String.format("%.0f min", moy));
        }
    }

    private void remplirFormulaire(Quiz q) {
        tfTitre.setText   (q.getTitre());
        tfDuree.setText   (String.valueOf(q.getDuree()));
        tfCourseId.setText(String.valueOf(q.getCourseId()));
        clearErreur(lblErreurTitre);
        clearErreur(lblErreurDuree);
        if (lblErreurCourseId != null) clearErreur(lblErreurCourseId);
    }

    private void viderFormulaire() {
        tfTitre.clear(); tfDuree.clear(); tfCourseId.clear();
        clearErreur(lblErreurTitre);
        clearErreur(lblErreurDuree);
        if (lblErreurCourseId != null) clearErreur(lblErreurCourseId);
        tableQuiz.getSelectionModel().clearSelection();
    }

    // ── Utilitaires d'affichage d'erreurs ─────────────────────

    private void setErreur(Label lbl, String msg) {
        if (lbl != null) { lbl.setText(msg); lbl.setStyle("-fx-text-fill: red;"); }
    }

    private void clearErreur(Label lbl) {
        if (lbl != null) { lbl.setText(""); lbl.setStyle(""); }
    }

    private void succes(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).show();
    }

    private void erreur(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}
