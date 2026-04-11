package edu.connexion3a36.services;

import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.entities.Question;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements IService<Question> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── Constantes de validation métier ───────────────────────
    private static final int TEXTE_MIN  = 10;
    private static final int TEXTE_MAX  = 500;
    private static final int INDICE_MAX = 200;
    private static final int CHOIX_MIN  = 2;
    private static final int CHOIX_MAX  = 200;
    private static final int REP_MIN    = 2;

    private static final List<String> NIVEAUX_VALIDES = List.of("facile", "moyen", "difficile");
    private static final List<String> TYPES_VALIDES   = List.of("choix_multiple", "vrai_faux", "texte");

    // ── CREATE ────────────────────────────────────────────────

    @Override
    public void add(Question q) throws Exception {
        valider(q);
        String sql = """
            INSERT INTO question
            (texte, niveau, indice, quiz_id, type,
             choix_a, choix_b, choix_c, choix_d,
             bonne_reponse_choix, bonne_reponse_bool, reponse_attendue)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            remplirPS(ps, q);
            ps.executeUpdate();
        }
    }

    // ── READ ALL ──────────────────────────────────────────────

    @Override
    public List<Question> getAll() throws Exception {
        return executeQuery("SELECT * FROM question ORDER BY quiz_id, id");
    }

    // ── READ BY ID ────────────────────────────────────────────

    @Override
    public Question getById(int id) throws Exception {
        if (id <= 0) throw new Exception("L'ID de la question doit être un entier positif.");
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM question WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── READ BY QUIZ ──────────────────────────────────────────

    public List<Question> getByQuizId(int quizId) throws Exception {
        if (quizId <= 0) throw new Exception("L'ID du quiz est invalide.");
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE quiz_id=? ORDER BY id")) {
            ps.setInt(1, quizId);
            return mapList(ps.executeQuery());
        }
    }

    // ── UPDATE ────────────────────────────────────────────────

    @Override
    public void update(Question q) throws Exception {
        if (q.getId() <= 0)
            throw new Exception("ID de la question invalide pour la mise à jour.");
        valider(q);
        String sql = """
            UPDATE question SET
              texte=?, niveau=?, indice=?, quiz_id=?, type=?,
              choix_a=?, choix_b=?, choix_c=?, choix_d=?,
              bonne_reponse_choix=?, bonne_reponse_bool=?, reponse_attendue=?
            WHERE id=?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            remplirPS(ps, q);
            ps.setInt(13, q.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────

    @Override
    public void delete(int id) throws Exception {
        if (id <= 0) throw new Exception("L'ID de la question à supprimer est invalide.");
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM question WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── SEARCH ────────────────────────────────────────────────

    public List<Question> search(String keyword) throws Exception {
        if (keyword == null || keyword.trim().isEmpty())
            return getAll();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE texte LIKE ? ORDER BY id")) {
            ps.setString(1, "%" + keyword.trim() + "%");
            return mapList(ps.executeQuery());
        }
    }

    // ── FILTRE PAR NIVEAU ─────────────────────────────────────

    public List<Question> getByNiveau(String niveau) throws Exception {
        if (niveau == null || !NIVEAUX_VALIDES.contains(niveau))
            throw new Exception("Niveau invalide. Valeurs acceptées : " + NIVEAUX_VALIDES);
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE niveau=? ORDER BY id")) {
            ps.setString(1, niveau);
            return mapList(ps.executeQuery());
        }
    }

    // ── FILTRE PAR TYPE ───────────────────────────────────────

    public List<Question> getByType(String type) throws Exception {
        if (type == null || !TYPES_VALIDES.contains(type))
            throw new Exception("Type invalide. Valeurs acceptées : " + TYPES_VALIDES);
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE type=? ORDER BY id")) {
            ps.setString(1, type);
            return mapList(ps.executeQuery());
        }
    }

    // ── TRI ───────────────────────────────────────────────────

    public List<Question> getAllSortedByNiveau() throws Exception {
        return executeQuery("""
            SELECT * FROM question
            ORDER BY FIELD(niveau,'facile','moyen','difficile')
            """);
    }

    public List<Question> getAllSortedByType() throws Exception {
        return executeQuery("SELECT * FROM question ORDER BY type ASC");
    }

    // ── VALIDATION MÉTIER ─────────────────────────────────────

    /**
     * Validation complète d'un objet Question.
     * Couvre les champs communs et les champs conditionnels selon le type.
     */
    private void valider(Question q) throws Exception {

        // --- Texte ---
        if (q.getTexte() == null || q.getTexte().trim().isEmpty())
            throw new Exception("Le texte de la question est obligatoire.");
        String texte = q.getTexte().trim();
        if (texte.length() < TEXTE_MIN)
            throw new Exception("Le texte doit contenir au moins " + TEXTE_MIN + " caractères.");
        if (texte.length() > TEXTE_MAX)
            throw new Exception("Le texte ne peut pas dépasser " + TEXTE_MAX + " caractères.");

        // --- Niveau ---
        if (q.getNiveau() == null || q.getNiveau().trim().isEmpty())
            throw new Exception("Le niveau est obligatoire.");
        if (!NIVEAUX_VALIDES.contains(q.getNiveau()))
            throw new Exception("Niveau invalide. Valeurs acceptées : " + NIVEAUX_VALIDES);

        // --- Type ---
        if (q.getType() == null || q.getType().trim().isEmpty())
            throw new Exception("Le type de question est obligatoire.");
        if (!TYPES_VALIDES.contains(q.getType()))
            throw new Exception("Type invalide. Valeurs acceptées : " + TYPES_VALIDES);

        // --- Quiz ID ---
        if (q.getQuizId() <= 0)
            throw new Exception("L'identifiant du quiz associé est invalide (doit être > 0).");

        // --- Indice (optionnel mais limité) ---
        if (q.getIndice() != null && q.getIndice().trim().length() > INDICE_MAX)
            throw new Exception("L'indice ne peut pas dépasser " + INDICE_MAX + " caractères.");

        // --- Champs conditionnels selon le type ---
        switch (q.getType()) {
            case "choix_multiple" -> validerChoixMultiple(q);
            case "vrai_faux"      -> validerVraiFaux(q);
            case "texte"          -> validerTexteLibre(q);
        }
    }

    /**
     * Règles pour le type choix_multiple :
     * - A et B obligatoires, C et D optionnels
     * - Chaque choix renseigné : entre CHOIX_MIN et CHOIX_MAX caractères
     * - La bonne réponse doit être parmi {a, b, c, d}
     */
    private void validerChoixMultiple(Question q) throws Exception {
        // Choix A — obligatoire
        if (q.getChoixA() == null || q.getChoixA().trim().isEmpty())
            throw new Exception("Le choix A est obligatoire.");
        validerLongueurChoix(q.getChoixA(), "A", true);

        // Choix B — obligatoire
        if (q.getChoixB() == null || q.getChoixB().trim().isEmpty())
            throw new Exception("Le choix B est obligatoire.");
        validerLongueurChoix(q.getChoixB(), "B", true);

        // Choix C — optionnel
        if (q.getChoixC() != null && !q.getChoixC().trim().isEmpty())
            validerLongueurChoix(q.getChoixC(), "C", false);

        // Choix D — optionnel
        if (q.getChoixD() != null && !q.getChoixD().trim().isEmpty())
            validerLongueurChoix(q.getChoixD(), "D", false);

        // Bonne réponse
        if (q.getBonneReponseChoix() == null)
            throw new Exception("La bonne réponse est obligatoire pour le type choix_multiple.");
        if (!List.of("a", "b", "c", "d").contains(q.getBonneReponseChoix()))
            throw new Exception("La bonne réponse doit être 'a', 'b', 'c' ou 'd'.");

        // Cohérence : la bonne réponse ne peut pas pointer vers un choix vide
        if ("c".equals(q.getBonneReponseChoix()) && (q.getChoixC() == null || q.getChoixC().trim().isEmpty()))
            throw new Exception("La bonne réponse est 'c' mais le choix C est vide.");
        if ("d".equals(q.getBonneReponseChoix()) && (q.getChoixD() == null || q.getChoixD().trim().isEmpty()))
            throw new Exception("La bonne réponse est 'd' mais le choix D est vide.");
    }

    /**
     * Règles pour le type vrai_faux :
     * - La valeur booléenne de la bonne réponse est obligatoire.
     */
    private void validerVraiFaux(Question q) throws Exception {
        if (q.getBonneReponseBool() == null)
            throw new Exception("Vous devez choisir Vrai ou Faux comme bonne réponse.");
    }

    /**
     * Règles pour le type texte libre :
     * - La réponse attendue est obligatoire et doit avoir au moins REP_MIN caractères.
     */
    private void validerTexteLibre(Question q) throws Exception {
        if (q.getReponseAttendue() == null || q.getReponseAttendue().trim().isEmpty())
            throw new Exception("La réponse attendue est obligatoire pour le type texte.");
        if (q.getReponseAttendue().trim().length() < REP_MIN)
            throw new Exception("La réponse attendue doit contenir au moins " + REP_MIN + " caractères.");
    }

    /**
     * Vérifie la longueur d'un choix (min et max).
     */
    private void validerLongueurChoix(String choix, String lettre, boolean obligatoire) throws Exception {
        String c = choix.trim();
        if (obligatoire && c.length() < CHOIX_MIN)
            throw new Exception("Le choix " + lettre + " doit contenir au moins " + CHOIX_MIN + " caractères.");
        if (c.length() > CHOIX_MAX)
            throw new Exception("Le choix " + lettre + " ne peut pas dépasser " + CHOIX_MAX + " caractères.");
    }

    // ── HELPERS ───────────────────────────────────────────────

    private void remplirPS(PreparedStatement ps, Question q) throws SQLException {
        ps.setString(1,  q.getTexte().trim());
        ps.setString(2,  q.getNiveau());
        ps.setString(3,  q.getIndice() != null ? q.getIndice().trim() : null);
        ps.setInt   (4,  q.getQuizId());
        ps.setString(5,  q.getType());
        ps.setString(6,  q.getChoixA() != null ? q.getChoixA().trim() : null);
        ps.setString(7,  q.getChoixB() != null ? q.getChoixB().trim() : null);
        ps.setString(8,  q.getChoixC() != null ? q.getChoixC().trim() : null);
        ps.setString(9,  q.getChoixD() != null ? q.getChoixD().trim() : null);
        ps.setString(10, q.getBonneReponseChoix());
        if (q.getBonneReponseBool() != null)
            ps.setBoolean(11, q.getBonneReponseBool());
        else
            ps.setNull(11, Types.BOOLEAN);
        ps.setString(12, q.getReponseAttendue() != null ? q.getReponseAttendue().trim() : null);
    }

    private List<Question> executeQuery(String sql) throws Exception {
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    private List<Question> mapList(ResultSet rs) throws SQLException {
        List<Question> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setId    (rs.getInt   ("id"));
        q.setTexte (rs.getString("texte"));
        q.setNiveau(rs.getString("niveau"));
        q.setIndice(rs.getString("indice"));
        q.setQuizId(rs.getInt   ("quiz_id"));
        q.setType  (rs.getString("type"));
        q.setChoixA(rs.getString("choix_a"));
        q.setChoixB(rs.getString("choix_b"));
        q.setChoixC(rs.getString("choix_c"));
        q.setChoixD(rs.getString("choix_d"));
        q.setBonneReponseChoix(rs.getString("bonne_reponse_choix"));
        boolean bv = rs.getBoolean("bonne_reponse_bool");
        if (!rs.wasNull()) q.setBonneReponseBool(bv);
        q.setReponseAttendue(rs.getString("reponse_attendue"));
        return q;
    }
}
