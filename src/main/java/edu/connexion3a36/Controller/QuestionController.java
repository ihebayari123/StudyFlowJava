package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Question;
import edu.connexion3a36.services.QuestionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class QuestionController {

    // ── Table ──────────────────────────────────────────────────
    @FXML private Label                       lblTitreQuiz;
    @FXML private TableView<Question>         tableQuestion;
    @FXML private TableColumn<Question, Integer> colId;
    @FXML private TableColumn<Question, String>  colTexte;
    @FXML private TableColumn<Question, String>  colNiveau;
    @FXML private TableColumn<Question, String>  colType;

    // ── Formulaire commun ────────────────────────────────────
    @FXML private TextArea         taTexte;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField        tfIndice;
    @FXML private Label            lblErreurTexte;
    @FXML private Label            lblErreurType;

    // ── Panneaux dynamiques ───────────────────────────────────
    @FXML private VBox vboxChoix;
    @FXML private VBox vboxVraiFaux;
    @FXML private VBox vboxTexteLibre;

    @FXML private TextField        tfChoixA, tfChoixB, tfChoixC, tfChoixD;
    @FXML private ComboBox<String> cbBonneReponse;
    @FXML private ComboBox<String> cbVraiFaux;
    @FXML private TextField        tfReponseAttendue;

    // ── Recherche, Filtre & Tri ───────────────────────────────
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbFiltreNiveau;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private ComboBox<String> cbTri;

    // ── Stats ─────────────────────────────────────────────────
    @FXML private Label lblTotalQuestions;

    // ── Etat ──────────────────────────────────────────────────
    private final QuestionService service = new QuestionService();
    private ObservableList<Question> questionList = FXCollections.observableArrayList();
    private int    quizId    = 0;
    private String quizTitre = "";

    @FXML
    public void initialize() {
        colId.setCellValueFactory    (new PropertyValueFactory<>("id"));
        colTexte.setCellValueFactory (new PropertyValueFactory<>("texte"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colType.setCellValueFactory  (new PropertyValueFactory<>("type"));

        cbNiveau.setItems      (FXCollections.observableArrayList("facile","moyen","difficile"));
        cbType.setItems        (FXCollections.observableArrayList("choix_multiple","vrai_faux","texte"));
        cbBonneReponse.setItems(FXCollections.observableArrayList("a","b","c","d"));
        cbVraiFaux.setItems    (FXCollections.observableArrayList("Vrai","Faux"));

        cbFiltreNiveau.setItems(FXCollections.observableArrayList("Tous","facile","moyen","difficile"));
        cbFiltreNiveau.setValue("Tous");
        cbFiltreType.setItems(FXCollections.observableArrayList("Tous","choix_multiple","vrai_faux","texte"));
        cbFiltreType.setValue("Tous");
        cbTri.setItems(FXCollections.observableArrayList("ID","Niveau","Type"));
        cbTri.setValue("ID");

        cacherTousPanneaux();

        // Affichage dynamique du panneau selon le type choisi
        cbType.setOnAction(e -> afficherPanneau(cbType.getValue()));

        // Remplissage formulaire au clic
        tableQuestion.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) remplirFormulaire(sel); }
        );

        // Recherche + Filtre + Tri en temps réel
        tfRecherche.textProperty().addListener((obs, old, val) -> appliquerFiltres());
        cbFiltreNiveau.setOnAction(e -> appliquerFiltres());
        cbFiltreType  .setOnAction(e -> appliquerFiltres());
        cbTri         .setOnAction(e -> appliquerFiltres());
    }

    // Appelé depuis QuizController après navigation
    public void setQuizId(int quizId, String titre) {
        this.quizId    = quizId;
        this.quizTitre = titre;
        if (lblTitreQuiz != null)
            lblTitreQuiz.setText("Questions — " + titre);
        chargerQuestions();
    }

    // ── CRUD ──────────────────────────────────────────────────

    @FXML
    public void ajouterQuestion() {
        if (!validerFormulaire()) return;
        try {
            service.addEntity(construireQuestion());    // ← addEntity
            chargerQuestions();
            viderFormulaire();
            succes("Question ajoutée !");
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    @FXML
    public void modifierQuestion() {
        Question sel = tableQuestion.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Sélectionnez une question."); return; }
        if (!validerFormulaire()) return;
        try {
            Question q = construireQuestion();
            service.updateEntity(sel.getId(), q);       // ← updateEntity(id, objet)
            chargerQuestions();
            viderFormulaire();
            succes("Question modifiée !");
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    @FXML
    public void supprimerQuestion() {
        Question sel = tableQuestion.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Sélectionnez une question."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette question ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.deleteEntity(sel);          // ← deleteEntity(objet)
                    chargerQuestions();
                    viderFormulaire();
                } catch (Exception e) { erreur(e.getMessage()); }
            }
        });
    }

    @FXML
    public void retourQuiz() {
        try {
            Node vue = FXMLLoader.load(getClass().getResource("/QuizView.fxml"));
            StackPane parent = (StackPane) tableQuestion.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (Exception e) { erreur("Erreur retour : " + e.getMessage()); }
    }

    // ── Filtre + Tri appliqués ensemble ──────────────────────

    private void appliquerFiltres() {
        try {
            // Base : questions du quiz ou toutes
            ObservableList<Question> base = FXCollections.observableArrayList(
                quizId > 0 ? service.getByQuizId(quizId) : service.getData()  // ← getData()
            );

            // Recherche textuelle
            String kw = tfRecherche.getText().trim().toLowerCase();
            if (!kw.isEmpty())
                base.removeIf(q -> !q.getTexte().toLowerCase().contains(kw));

            // Filtre niveau
            String fn = cbFiltreNiveau.getValue();
            if (fn != null && !fn.equals("Tous"))
                base.removeIf(q -> !q.getNiveau().equals(fn));

            // Filtre type
            String ft = cbFiltreType.getValue();
            if (ft != null && !ft.equals("Tous"))
                base.removeIf(q -> !q.getType().equals(ft));

            // Tri
            String tri = cbTri.getValue();
            if ("Niveau".equals(tri)) {
                base.sort((a, b) -> {
                    String[] ord = {"facile","moyen","difficile"};
                    int ia = 0, ib = 0;
                    for (int i = 0; i < ord.length; i++) {
                        if (ord[i].equals(a.getNiveau())) ia = i;
                        if (ord[i].equals(b.getNiveau())) ib = i;
                    }
                    return ia - ib;
                });
            } else if ("Type".equals(tri)) {
                base.sort((a, b) -> a.getType().compareTo(b.getType()));
            } else {
                base.sort((a, b) -> a.getId() - b.getId());
            }

            questionList.setAll(base);
            tableQuestion.setItems(questionList);
            if (lblTotalQuestions != null)
                lblTotalQuestions.setText(String.valueOf(questionList.size()));

        } catch (Exception e) { erreur(e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────

    private void chargerQuestions() {
        try {
            questionList.setAll(
                quizId > 0 ? service.getByQuizId(quizId) : service.getData()  // ← getData()
            );
            tableQuestion.setItems(questionList);
            if (lblTotalQuestions != null)
                lblTotalQuestions.setText(String.valueOf(questionList.size()));
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    private Question construireQuestion() {
        Question q = new Question();
        q.setTexte  (taTexte.getText().trim());
        q.setNiveau (cbNiveau.getValue());
        q.setIndice (tfIndice.getText().trim());
        q.setQuizId (quizId);
        q.setType   (cbType.getValue());
        switch (cbType.getValue()) {
            case "choix_multiple" -> {
                q.setChoixA(tfChoixA.getText().trim());
                q.setChoixB(tfChoixB.getText().trim());
                q.setChoixC(tfChoixC.getText().trim());
                q.setChoixD(tfChoixD.getText().trim());
                q.setBonneReponseChoix(cbBonneReponse.getValue());
            }
            case "vrai_faux" ->
                q.setBonneReponseBool("Vrai".equals(cbVraiFaux.getValue()));
            case "texte" ->
                q.setReponseAttendue(tfReponseAttendue.getText().trim());
        }
        return q;
    }

    private void remplirFormulaire(Question q) {
        taTexte.setText (q.getTexte());
        cbNiveau.setValue(q.getNiveau());
        cbType.setValue  (q.getType());
        tfIndice.setText (q.getIndice() != null ? q.getIndice() : "");
        afficherPanneau  (q.getType());
        clearErreur(lblErreurTexte);
        clearErreur(lblErreurType);
        switch (q.getType()) {
            case "choix_multiple" -> {
                tfChoixA.setText(nvl(q.getChoixA()));
                tfChoixB.setText(nvl(q.getChoixB()));
                tfChoixC.setText(nvl(q.getChoixC()));
                tfChoixD.setText(nvl(q.getChoixD()));
                cbBonneReponse.setValue(q.getBonneReponseChoix());
            }
            case "vrai_faux" ->
                cbVraiFaux.setValue(Boolean.TRUE.equals(q.getBonneReponseBool()) ? "Vrai" : "Faux");
            case "texte" ->
                tfReponseAttendue.setText(nvl(q.getReponseAttendue()));
        }
    }

    private void viderFormulaire() {
        taTexte.clear(); tfIndice.clear();
        cbNiveau.setValue(null); cbType.setValue(null);
        tfChoixA.clear(); tfChoixB.clear(); tfChoixC.clear(); tfChoixD.clear();
        tfReponseAttendue.clear();
        cbBonneReponse.setValue(null); cbVraiFaux.setValue(null);
        clearErreur(lblErreurTexte); clearErreur(lblErreurType);
        cacherTousPanneaux();
        tableQuestion.getSelectionModel().clearSelection();
    }

    private void afficherPanneau(String type) {
        cacherTousPanneaux();
        if (type == null) return;
        switch (type) {
            case "choix_multiple" -> { vboxChoix.setVisible(true);     vboxChoix.setManaged(true); }
            case "vrai_faux"      -> { vboxVraiFaux.setVisible(true);  vboxVraiFaux.setManaged(true); }
            case "texte"          -> { vboxTexteLibre.setVisible(true); vboxTexteLibre.setManaged(true); }
        }
    }

    private void cacherTousPanneaux() {
        vboxChoix.setVisible(false);     vboxChoix.setManaged(false);
        vboxVraiFaux.setVisible(false);  vboxVraiFaux.setManaged(false);
        vboxTexteLibre.setVisible(false); vboxTexteLibre.setManaged(false);
    }

    private boolean validerFormulaire() {
        boolean ok = true;
        clearErreur(lblErreurTexte); clearErreur(lblErreurType);
        if (taTexte.getText().trim().isEmpty()) {
            setErreur(lblErreurTexte, "Le texte est obligatoire."); ok = false;
        }
        if (cbType.getValue() == null) {
            setErreur(lblErreurType, "Choisissez un type."); ok = false;
        }
        if (cbNiveau.getValue() == null) {
            erreur("Choisissez un niveau."); ok = false;
        }
        return ok;
    }

    private String nvl(String s)        { return s != null ? s : ""; }
    private void setErreur  (Label l, String m) { if (l != null) { l.setText(m); l.setStyle("-fx-text-fill: red;"); } }
    private void clearErreur(Label l)            { if (l != null) { l.setText(""); l.setStyle(""); } }
    private void succes(String m) { new Alert(Alert.AlertType.INFORMATION, m).show(); }
    private void erreur(String m) { new Alert(Alert.AlertType.ERROR,       m).show(); }
}
