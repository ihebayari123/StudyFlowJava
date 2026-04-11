package edu.connexion3a36.services;

import edu.connexion3a36.entities.Question;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements IService<Question> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    private static final int    TEXTE_MIN  = 10;
    private static final int    TEXTE_MAX  = 500;
    private static final int    INDICE_MAX = 200;
    private static final int    CHOIX_MIN  = 2;
    private static final int    CHOIX_MAX  = 200;
    private static final int    REP_MIN    = 2;

    private static final List<String> NIVEAUX = List.of("facile", "moyen", "difficile");
    private static final List<String> TYPES   = List.of("choix_multiple", "vrai_faux", "texte");

    // ── CREATE ────────────────────────────────────────────────

    @Override
    public void addEntity(Question q) throws SQLException {
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
    public List<Question> getData() throws SQLException {
        return executeQuery("SELECT * FROM question ORDER BY quiz_id, id");
    }

    // ── UPDATE ────────────────────────────────────────────────

    @Override
    public void updateEntity(int id, Question q) throws SQLException {
        if (id <= 0)
            throw new SQLException("ID de la question invalide pour la mise à jour.");
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
            ps.setInt(13, id);
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────

    @Override
    public void deleteEntity(Question q) throws SQLException {
        if (q.getId() <= 0)
            throw new SQLException("L'ID de la question à supprimer est invalide.");
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM question WHERE id=?")) {
            ps.setInt(1, q.getId());
            ps.executeUpdate();
        }
    }

    // ── READ BY QUIZ ID ───────────────────────────────────────

    public List<Question> getByQuizId(int quizId) throws SQLException {
        if (quizId <= 0) throw new SQLException("L'ID du quiz est invalide.");
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE quiz_id=? ORDER BY id")) {
            ps.setInt(1, quizId);
            return mapList(ps.executeQuery());
        }
    }

    // ── GET BY ID (utilitaire) ────────────────────────────────

    public Question getById(int id) throws SQLException {
        if (id <= 0) throw new SQLException("L'ID de la question doit être positif.");
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── SEARCH ────────────────────────────────────────────────

    public List<Question> search(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) return getData();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE texte LIKE ? ORDER BY id")) {
            ps.setString(1, "%" + keyword.trim() + "%");
            return mapList(ps.executeQuery());
        }
    }

    // ── FILTRE PAR NIVEAU ─────────────────────────────────────

    public List<Question> getByNiveau(String niveau) throws SQLException {
        if (niveau == null || !NIVEAUX.contains(niveau))
            throw new SQLException("Niveau invalide. Valeurs acceptées : " + NIVEAUX);
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE niveau=? ORDER BY id")) {
            ps.setString(1, niveau);
            return mapList(ps.executeQuery());
        }
    }

    // ── FILTRE PAR TYPE ───────────────────────────────────────

    public List<Question> getByType(String type) throws SQLException {
        if (type == null || !TYPES.contains(type))
            throw new SQLException("Type invalide. Valeurs acceptées : " + TYPES);
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM question WHERE type=? ORDER BY id")) {
            ps.setString(1, type);
            return mapList(ps.executeQuery());
        }
    }

    // ── TRI ───────────────────────────────────────────────────

    public List<Question> getAllSortedByNiveau() throws SQLException {
        return executeQuery("""
            SELECT * FROM question
            ORDER BY FIELD(niveau,'facile','moyen','difficile')
            """);
    }

    public List<Question> getAllSortedByType() throws SQLException {
        return executeQuery("SELECT * FROM question ORDER BY type ASC");
    }

    // ── VALIDATION MÉTIER ─────────────────────────────────────

    private void valider(Question q) throws SQLException {
        // Texte
        if (q.getTexte() == null || q.getTexte().trim().isEmpty())
            throw new SQLException("Le texte de la question est obligatoire.");
        String texte = q.getTexte().trim();
        if (texte.length() < TEXTE_MIN)
            throw new SQLException(
                "Le texte doit contenir au moins " + TEXTE_MIN + " caractères.");
        if (texte.length() > TEXTE_MAX)
            throw new SQLException(
                "Le texte ne peut pas dépasser " + TEXTE_MAX + " caractères.");

        // Niveau
        if (q.getNiveau() == null || q.getNiveau().trim().isEmpty())
            throw new SQLException("Le niveau est obligatoire.");
        if (!NIVEAUX.contains(q.getNiveau()))
            throw new SQLException("Niveau invalide. Valeurs acceptées : " + NIVEAUX);

        // Type
        if (q.getType() == null || q.getType().trim().isEmpty())
            throw new SQLException("Le type de question est obligatoire.");
        if (!TYPES.contains(q.getType()))
            throw new SQLException("Type invalide. Valeurs acceptées : " + TYPES);

        // Quiz ID
        if (q.getQuizId() <= 0)
            throw new SQLException(
                "L'identifiant du quiz associé est invalide (doit être > 0).");

        // Indice (optionnel)
        if (q.getIndice() != null && q.getIndice().trim().length() > INDICE_MAX)
            throw new SQLException(
                "L'indice ne peut pas dépasser " + INDICE_MAX + " caractères.");

        // Champs conditionnels
        switch (q.getType()) {
            case "choix_multiple" -> validerChoixMultiple(q);
            case "vrai_faux"      -> validerVraiFaux(q);
            case "texte"          -> validerTexteLibre(q);
        }
    }

    private void validerChoixMultiple(Question q) throws SQLException {
        if (q.getChoixA() == null || q.getChoixA().trim().isEmpty())
            throw new SQLException("Le choix A est obligatoire.");
        validerLongueurChoix(q.getChoixA(), "A");

        if (q.getChoixB() == null || q.getChoixB().trim().isEmpty())
            throw new SQLException("Le choix B est obligatoire.");
        validerLongueurChoix(q.getChoixB(), "B");

        if (q.getChoixC() != null && !q.getChoixC().trim().isEmpty())
            validerLongueurChoix(q.getChoixC(), "C");
        if (q.getChoixD() != null && !q.getChoixD().trim().isEmpty())
            validerLongueurChoix(q.getChoixD(), "D");

        if (q.getBonneReponseChoix() == null)
            throw new SQLException(
                "La bonne réponse est obligatoire pour le type choix_multiple.");
        if (!List.of("a","b","c","d").contains(q.getBonneReponseChoix()))
            throw new SQLException("La bonne réponse doit être 'a', 'b', 'c' ou 'd'.");

        if ("c".equals(q.getBonneReponseChoix()) &&
            (q.getChoixC() == null || q.getChoixC().trim().isEmpty()))
            throw new SQLException("La bonne réponse est 'c' mais le choix C est vide.");
        if ("d".equals(q.getBonneReponseChoix()) &&
            (q.getChoixD() == null || q.getChoixD().trim().isEmpty()))
            throw new SQLException("La bonne réponse est 'd' mais le choix D est vide.");
    }

    private void validerVraiFaux(Question q) throws SQLException {
        if (q.getBonneReponseBool() == null)
            throw new SQLException("Vous devez choisir Vrai ou Faux.");
    }

    private void validerTexteLibre(Question q) throws SQLException {
        if (q.getReponseAttendue() == null || q.getReponseAttendue().trim().isEmpty())
            throw new SQLException("La réponse attendue est obligatoire pour le type texte.");
        if (q.getReponseAttendue().trim().length() < REP_MIN)
            throw new SQLException(
                "La réponse attendue doit contenir au moins " + REP_MIN + " caractères.");
    }

    private void validerLongueurChoix(String choix, String lettre) throws SQLException {
        String c = choix.trim();
        if (c.length() < CHOIX_MIN)
            throw new SQLException(
                "Le choix " + lettre + " doit contenir au moins " + CHOIX_MIN + " caractères.");
        if (c.length() > CHOIX_MAX)
            throw new SQLException(
                "Le choix " + lettre + " ne peut pas dépasser " + CHOIX_MAX + " caractères.");
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
        ps.setString(12, q.getReponseAttendue() != null ?
                         q.getReponseAttendue().trim() : null);
    }

    private List<Question> executeQuery(String sql) throws SQLException {
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
