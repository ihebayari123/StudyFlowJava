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
    @FXML private Label                      lblTitreQuiz;
    @FXML private TableView<Question>        tableQuestion;
    @FXML private TableColumn<Question, Integer> colId;
    @FXML private TableColumn<Question, String>  colTexte;
    @FXML private TableColumn<Question, String>  colNiveau;
    @FXML private TableColumn<Question, String>  colType;

    // ── Formulaire commun ─────────────────────────────────────
    @FXML private TextArea         taTexte;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField        tfIndice;
    @FXML private Label            lblErreurTexte;
    @FXML private Label            lblErreurType;
    @FXML private Label            lblErreurNiveau;   // ← nouveau (à ajouter dans le FXML)
    @FXML private Label            lblErreurIndice;   // ← nouveau (optionnel)

    // ── Panneaux dynamiques ───────────────────────────────────
    @FXML private VBox vboxChoix;
    @FXML private VBox vboxVraiFaux;
    @FXML private VBox vboxTexteLibre;

    // Choix multiple
    @FXML private TextField        tfChoixA, tfChoixB, tfChoixC, tfChoixD;
    @FXML private ComboBox<String> cbBonneReponse;
    @FXML private Label            lblErreurChoix;        // ← nouveau
    @FXML private Label            lblErreurBonneReponse; // ← nouveau

    // Vrai/Faux
    @FXML private ComboBox<String> cbVraiFaux;
    @FXML private Label            lblErreurVraiFaux;     // ← nouveau

    // Texte libre
    @FXML private TextField tfReponseAttendue;
    @FXML private Label     lblErreurReponseAttendue;     // ← nouveau

    // ── Recherche & Filtre & Tri ──────────────────────────────
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbFiltreNiveau;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private ComboBox<String> cbTri;

    // ── Stats ─────────────────────────────────────────────────
    @FXML private Label lblTotalQuestions;

    // ── Constantes de validation ──────────────────────────────
    private static final int TEXTE_MIN   = 10;
    private static final int TEXTE_MAX   = 500;
    private static final int INDICE_MAX  = 200;
    private static final int CHOIX_MIN   = 2;
    private static final int CHOIX_MAX   = 200;
    private static final int REP_MIN     = 2;

    // ── État ──────────────────────────────────────────────────
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

        cbNiveau.setItems(FXCollections.observableArrayList("facile", "moyen", "difficile"));
        cbType.setItems  (FXCollections.observableArrayList("choix_multiple", "vrai_faux", "texte"));
        cbBonneReponse.setItems(FXCollections.observableArrayList("a", "b", "c", "d"));
        cbVraiFaux.setItems    (FXCollections.observableArrayList("Vrai", "Faux"));

        cbFiltreNiveau.setItems(FXCollections.observableArrayList("Tous", "facile", "moyen", "difficile"));
        cbFiltreNiveau.setValue("Tous");
        cbFiltreType.setItems(FXCollections.observableArrayList("Tous", "choix_multiple", "vrai_faux", "texte"));
        cbFiltreType.setValue("Tous");
        cbTri.setItems(FXCollections.observableArrayList("ID", "Niveau", "Type"));
        cbTri.setValue("ID");

        cacherTousPanneaux();

        // Validation en temps réel
        taTexte.textProperty().addListener((obs, old, val) -> validerTexte(val));
        tfIndice.textProperty().addListener((obs, old, val) -> validerIndice(val));
        tfChoixA.textProperty().addListener((obs, old, val) -> validerChoixObligatoire(val, "A"));
        tfChoixB.textProperty().addListener((obs, old, val) -> validerChoixObligatoire(val, "B"));
        tfChoixC.textProperty().addListener((obs, old, val) -> validerChoixOptionnel(val, "C"));
        tfChoixD.textProperty().addListener((obs, old, val) -> validerChoixOptionnel(val, "D"));
        tfReponseAttendue.textProperty().addListener((obs, old, val) -> validerReponseAttendue(val));

        cbType.setOnAction(e -> {
            afficherPanneau(cbType.getValue());
            clearErreur(lblErreurType);
        });
        cbNiveau.setOnAction(e -> clearErreur(lblErreurNiveau));

        tableQuestion.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) remplirFormulaire(sel); }
        );

        tfRecherche.textProperty().addListener((obs, old, val) -> filtrerEtTrier());
        cbFiltreNiveau.setOnAction(e -> filtrerEtTrier());
        cbFiltreType  .setOnAction(e -> filtrerEtTrier());
        cbTri         .setOnAction(e -> filtrerEtTrier());
    }

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
            service.add(construireQuestion());
            chargerQuestions();
            viderFormulaire();
            succes("Question ajoutée !");
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    @FXML
    public void modifierQuestion() {
        Question sel = tableQuestion.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Sélectionnez une question dans le tableau."); return; }
        if (!validerFormulaire()) return;
        try {
            Question q = construireQuestion();
            q.setId(sel.getId());
            service.update(q);
            chargerQuestions();
            viderFormulaire();
            succes("Question modifiée !");
        } catch (Exception e) { erreur(e.getMessage()); }
    }

    @FXML
    public void supprimerQuestion() {
        Question sel = tableQuestion.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Sélectionnez une question dans le tableau."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette question ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.delete(sel.getId());
                    chargerQuestions();
                    viderFormulaire();
                } catch (Exception e) { erreur(e.getMessage()); }
            }
        });
    }

    @FXML
    public void retourQuiz() {
        try {
            Node vue = FXMLLoader.load(getClass().getResource("/views/QuizView.fxml"));
            StackPane parent = (StackPane) tableQuestion.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (Exception e) { erreur("Erreur retour : " + e.getMessage()); }
    }

    // ── Validation en temps réel ───────────────────────────────

    /**
     * Texte de la question : obligatoire, entre TEXTE_MIN et TEXTE_MAX caractères.
     */
    private boolean validerTexte(String val) {
        String v = (val == null) ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurTexte, "Le texte de la question est obligatoire.");
            return false;
        }
        if (v.length() < TEXTE_MIN) {
            setErreur(lblErreurTexte, "Minimum " + TEXTE_MIN + " caractères requis.");
            return false;
        }
        if (v.length() > TEXTE_MAX) {
            setErreur(lblErreurTexte, "Maximum " + TEXTE_MAX + " caractères autorisés.");
            return false;
        }
        clearErreur(lblErreurTexte);
        return true;
    }

    /**
     * Indice : champ optionnel, mais ne peut pas dépasser INDICE_MAX caractères.
     */
    private boolean validerIndice(String val) {
        if (lblErreurIndice == null) return true;
        String v = (val == null) ? "" : val.trim();
        if (v.length() > INDICE_MAX) {
            setErreur(lblErreurIndice, "L'indice ne peut pas dépasser " + INDICE_MAX + " caractères.");
            return false;
        }
        clearErreur(lblErreurIndice);
        return true;
    }

    /**
     * Choix A et B : obligatoires pour choix_multiple, entre CHOIX_MIN et CHOIX_MAX.
     */
    private boolean validerChoixObligatoire(String val, String lettre) {
        if (!"choix_multiple".equals(cbType.getValue())) return true;
        if (lblErreurChoix == null) return true;
        String v = (val == null) ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurChoix, "Le choix " + lettre + " est obligatoire.");
            return false;
        }
        if (v.length() < CHOIX_MIN) {
            setErreur(lblErreurChoix, "Choix " + lettre + " : minimum " + CHOIX_MIN + " caractères.");
            return false;
        }
        if (v.length() > CHOIX_MAX) {
            setErreur(lblErreurChoix, "Choix " + lettre + " : maximum " + CHOIX_MAX + " caractères.");
            return false;
        }
        clearErreur(lblErreurChoix);
        return true;
    }

    /**
     * Choix C et D : optionnels, mais respectent la limite max si renseignés.
     */
    private boolean validerChoixOptionnel(String val, String lettre) {
        if (!"choix_multiple".equals(cbType.getValue())) return true;
        if (lblErreurChoix == null) return true;
        String v = (val == null) ? "" : val.trim();
        if (!v.isEmpty() && v.length() > CHOIX_MAX) {
            setErreur(lblErreurChoix, "Choix " + lettre + " : maximum " + CHOIX_MAX + " caractères.");
            return false;
        }
        clearErreur(lblErreurChoix);
        return true;
    }

    /**
     * Réponse attendue (type texte) : obligatoire, au moins REP_MIN caractères.
     */
    private boolean validerReponseAttendue(String val) {
        if (!"texte".equals(cbType.getValue())) return true;
        if (lblErreurReponseAttendue == null) return true;
        String v = (val == null) ? "" : val.trim();
        if (v.isEmpty()) {
            setErreur(lblErreurReponseAttendue, "La réponse attendue est obligatoire.");
            return false;
        }
        if (v.length() < REP_MIN) {
            setErreur(lblErreurReponseAttendue, "Minimum " + REP_MIN + " caractères requis.");
            return false;
        }
        clearErreur(lblErreurReponseAttendue);
        return true;
    }

    /**
     * Validation globale avant soumission — vérifie tous les champs,
     * y compris les panneaux dynamiques selon le type sélectionné.
     */
    private boolean validerFormulaire() {
        boolean ok = true;

        // --- Champs communs ---
        if (!validerTexte(taTexte.getText())) ok = false;

        if (cbNiveau.getValue() == null) {
            setErreur(lblErreurNiveau, "Choisissez un niveau.");
            ok = false;
        } else {
            clearErreur(lblErreurNiveau);
        }

        if (cbType.getValue() == null) {
            setErreur(lblErreurType, "Choisissez un type de question.");
            ok = false;
        } else {
            clearErreur(lblErreurType);
        }

        if (!validerIndice(tfIndice.getText())) ok = false;

        if (quizId <= 0) {
            erreur("Aucun quiz associé. Retournez à la liste des quiz.");
            return false;
        }

        // --- Champs conditionnels par type ---
        if ("choix_multiple".equals(cbType.getValue())) {
            if (!validerChoixObligatoire(tfChoixA.getText(), "A")) ok = false;
            if (!validerChoixObligatoire(tfChoixB.getText(), "B")) ok = false;
            if (!validerChoixOptionnel (tfChoixC.getText(), "C")) ok = false;
            if (!validerChoixOptionnel (tfChoixD.getText(), "D")) ok = false;
            if (cbBonneReponse.getValue() == null) {
                setErreur(lblErreurBonneReponse, "Choisissez la bonne réponse.");
                ok = false;
            } else {
                clearErreur(lblErreurBonneReponse);
            }
        } else if ("vrai_faux".equals(cbType.getValue())) {
            if (cbVraiFaux.getValue() == null) {
                setErreur(lblErreurVraiFaux, "Choisissez Vrai ou Faux.");
                ok = false;
            } else {
                clearErreur(lblErreurVraiFaux);
            }
        } else if ("texte".equals(cbType.getValue())) {
            if (!validerReponseAttendue(tfReponseAttendue.getText())) ok = false;
        }

        return ok;
    }

    // ── Filtrer & Trier ───────────────────────────────────────

    private void filtrerEtTrier() {
        try {
            ObservableList<Question> base = FXCollections.observableArrayList(
                quizId > 0 ? service.getByQuizId(quizId) : service.getAll()
            );

            String kw = tfRecherche.getText().trim().toLowerCase();
            if (!kw.isEmpty())
                base.removeIf(q -> !q.getTexte().toLowerCase().contains(kw));

            String fn = cbFiltreNiveau.getValue();
            if (fn != null && !fn.equals("Tous"))
                base.removeIf(q -> !q.getNiveau().equals(fn));

            String ft = cbFiltreType.getValue();
            if (ft != null && !ft.equals("Tous"))
                base.removeIf(q -> !q.getType().equals(ft));

            String tri = cbTri.getValue();
            if ("Niveau".equals(tri)) {
                String[] niv = {"facile", "moyen", "difficile"};
                base.sort((a, b) -> {
                    int ia = 0, ib = 0;
                    for (int i = 0; i < niv.length; i++) {
                        if (niv[i].equals(a.getNiveau())) ia = i;
                        if (niv[i].equals(b.getNiveau())) ib = i;
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
                quizId > 0 ? service.getByQuizId(quizId) : service.getAll()
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
        q.setIndice (tfIndice.getText().trim().isEmpty() ? null : tfIndice.getText().trim());
        q.setQuizId (quizId);
        q.setType   (cbType.getValue());
        switch (cbType.getValue()) {
            case "choix_multiple" -> {
                q.setChoixA(tfChoixA.getText().trim());
                q.setChoixB(tfChoixB.getText().trim());
                q.setChoixC(tfChoixC.getText().trim().isEmpty() ? null : tfChoixC.getText().trim());
                q.setChoixD(tfChoixD.getText().trim().isEmpty() ? null : tfChoixD.getText().trim());
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
        taTexte.setText  (q.getTexte());
        cbNiveau.setValue(q.getNiveau());
        cbType.setValue  (q.getType());
        tfIndice.setText (q.getIndice() != null ? q.getIndice() : "");
        afficherPanneau  (q.getType());
        effacerToutesErreurs();

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
        effacerToutesErreurs();
        cacherTousPanneaux();
        tableQuestion.getSelectionModel().clearSelection();
    }

    private void effacerToutesErreurs() {
        clearErreur(lblErreurTexte);
        clearErreur(lblErreurType);
        clearErreur(lblErreurNiveau);
        clearErreur(lblErreurIndice);
        clearErreur(lblErreurChoix);
        clearErreur(lblErreurBonneReponse);
        clearErreur(lblErreurVraiFaux);
        clearErreur(lblErreurReponseAttendue);
    }

    private void afficherPanneau(String type) {
        cacherTousPanneaux();
        if (type == null) return;
        switch (type) {
            case "choix_multiple" -> { vboxChoix.setVisible(true);      vboxChoix.setManaged(true);      }
            case "vrai_faux"      -> { vboxVraiFaux.setVisible(true);   vboxVraiFaux.setManaged(true);   }
            case "texte"          -> { vboxTexteLibre.setVisible(true); vboxTexteLibre.setManaged(true); }
        }
    }

    private void cacherTousPanneaux() {
        vboxChoix.setVisible(false);      vboxChoix.setManaged(false);
        vboxVraiFaux.setVisible(false);   vboxVraiFaux.setManaged(false);
        vboxTexteLibre.setVisible(false); vboxTexteLibre.setManaged(false);
    }

    // ── Utilitaires ───────────────────────────────────────────

    private void setErreur(Label lbl, String msg) {
        if (lbl != null) { lbl.setText(msg); lbl.setStyle("-fx-text-fill: red;"); }
    }

    private void clearErreur(Label lbl) {
        if (lbl != null) { lbl.setText(""); lbl.setStyle(""); }
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private void succes(String m) { new Alert(Alert.AlertType.INFORMATION, m).show(); }
    private void erreur (String m) { new Alert(Alert.AlertType.ERROR, m).show(); }
}
