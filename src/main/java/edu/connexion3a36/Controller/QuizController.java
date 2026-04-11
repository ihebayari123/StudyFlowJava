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
    @FXML private Label     lblErreurCourseId;

    // ── Recherche & Tri ───────────────────────────────────────
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbTri;

    // ── Statistiques ──────────────────────────────────────────
    @FXML private Label lblTotalQuiz;
    @FXML private Label lblMoyenneDuree;

    // ── Constantes ────────────────────────────────────────────
    private static final int TITRE_MIN = 3;
    private static final int TITRE_MAX = 100;
    private static final int DUREE_MIN = 1;
    private static final int DUREE_MAX = 180;

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

        // Remplissage formulaire au clic sur une ligne
        tableQuiz.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) remplirFormulaire(sel); }
        );

        // Validation en temps réel
        tfTitre.textProperty()   .addListener((obs, old, val) -> validerTitre(val));
        tfDuree.textProperty()   .addListener((obs, old, val) -> validerDuree(val));
        tfCourseId.textProperty().addListener((obs, old, val) -> validerCourseId(val));

        // Recherche & Tri
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
            service.addEntity(q);           // ← addEntity
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
            Quiz updated = new Quiz(
                tfTitre.getText().trim(),
                Integer.parseInt(tfDuree.getText().trim()),
                Integer.parseInt(tfCourseId.getText().trim())
            );
            service.updateEntity(sel.getId(), updated);  // ← updateEntity(id, objet)
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
                    service.deleteEntity(sel);           // ← deleteEntity(objet)
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
                getClass().getResource("/QuestionView.fxml")
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

    private boolean validerTitre(String val) {
        String v = val == null ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurTitre, "Le titre est obligatoire."); return false;
        }
        if (v.length() < TITRE_MIN) {
            setErreur(lblErreurTitre, "Minimum " + TITRE_MIN + " caractères."); return false;
        }
        if (v.length() > TITRE_MAX) {
            setErreur(lblErreurTitre, "Maximum " + TITRE_MAX + " caractères."); return false;
        }
        clearErreur(lblErreurTitre);
        return true;
    }

    private boolean validerDuree(String val) {
        String v = val == null ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurDuree, "La durée est obligatoire."); return false;
        }
        if (!v.matches("\\d+")) {
            setErreur(lblErreurDuree, "Entier positif requis."); return false;
        }
        try {
            int d = Integer.parseInt(v);
            if (d < DUREE_MIN || d > DUREE_MAX) {
                setErreur(lblErreurDuree,
                    "Durée entre " + DUREE_MIN + " et " + DUREE_MAX + " min.");
                return false;
            }
        } catch (NumberFormatException e) {
            setErreur(lblErreurDuree, "Valeur trop grande."); return false;
        }
        clearErreur(lblErreurDuree);
        return true;
    }

    private boolean validerCourseId(String val) {
        if (lblErreurCourseId == null) return true;
        String v = val == null ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurCourseId, "ID du cours obligatoire."); return false;
        }
        if (!v.matches("\\d+")) {
            setErreur(lblErreurCourseId, "Entier positif requis."); return false;
        }
        try {
            if (Integer.parseInt(v) <= 0) {
                setErreur(lblErreurCourseId, "L'ID doit être > 0."); return false;
            }
        } catch (NumberFormatException e) {
            setErreur(lblErreurCourseId, "Valeur trop grande."); return false;
        }
        clearErreur(lblErreurCourseId);
        return true;
    }

    private boolean validerFormulaire() {
        return validerTitre   (tfTitre.getText())
            && validerDuree   (tfDuree.getText())
            && validerCourseId(tfCourseId.getText());
    }

    // ── Recherche & Tri ───────────────────────────────────────

    private void filtrer(String keyword) {
        try {
            quizList.setAll(
                (keyword == null || keyword.trim().isEmpty())
                    ? service.getData()                     // ← getData()
                    : service.search(keyword.trim())
            );
            tableQuiz.setItems(quizList);
            mettreAJourStats();
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    private void trierQuiz() {
        try {
            String tri = cbTri.getValue();
            if      (tri == null)              quizList.setAll(service.getData());
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
            quizList.setAll(service.getData());              // ← getData()
            tableQuiz.setItems(quizList);
            mettreAJourStats();
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    private void mettreAJourStats() {
        if (lblTotalQuiz    != null) lblTotalQuiz.setText(String.valueOf(quizList.size()));
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

    private void setErreur  (Label l, String m) { if (l != null) { l.setText(m); l.setStyle("-fx-text-fill: red;"); } }
    private void clearErreur(Label l)            { if (l != null) { l.setText(""); l.setStyle(""); } }
    private void succes(String m) { new Alert(Alert.AlertType.INFORMATION, m).show(); }
    private void erreur(String m) { new Alert(Alert.AlertType.ERROR,       m).show(); }
}
