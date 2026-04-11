package edu.connexion3a36.services;

import edu.connexion3a36.entities.Quiz;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuizService implements IService<Quiz> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── Constantes de validation métier ───────────────────────
    private static final int TITRE_MIN  = 3;
    private static final int TITRE_MAX  = 100;
    private static final int DUREE_MIN  = 1;
    private static final int DUREE_MAX  = 180;

    // ── CREATE ────────────────────────────────────────────────

    @Override
    public void add(Quiz q) throws Exception {
        valider(q);
        String sql = "INSERT INTO quiz (titre, duree, date_creation, course_id) VALUES (?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString   (1, q.getTitre().trim());
            ps.setInt      (2, q.getDuree());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt      (4, q.getCourseId());
            ps.executeUpdate();
        }
    }

    // ── READ ALL ──────────────────────────────────────────────

    @Override
    public List<Quiz> getAll() throws Exception {
        return executeQuery("SELECT * FROM quiz ORDER BY date_creation DESC");
    }

    // ── READ BY ID ────────────────────────────────────────────

    @Override
    public Quiz getById(int id) throws Exception {
        if (id <= 0) throw new Exception("L'ID doit être un entier positif.");
        String sql = "SELECT * FROM quiz WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── UPDATE ────────────────────────────────────────────────

    @Override
    public void update(Quiz q) throws Exception {
        if (q.getId() <= 0)
            throw new Exception("ID du quiz invalide pour la mise à jour.");
        valider(q);
        String sql = "UPDATE quiz SET titre=?, duree=?, course_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getTitre().trim());
            ps.setInt   (2, q.getDuree());
            ps.setInt   (3, q.getCourseId());
            ps.setInt   (4, q.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────

    @Override
    public void delete(int id) throws Exception {
        if (id <= 0) throw new Exception("L'ID du quiz à supprimer est invalide.");
        // Supprimer d'abord les questions liées
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM question WHERE quiz_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM quiz WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── SEARCH ────────────────────────────────────────────────

    public List<Quiz> search(String keyword) throws Exception {
        if (keyword == null || keyword.trim().isEmpty())
            return getAll();
        String sql = "SELECT * FROM quiz WHERE titre LIKE ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword.trim() + "%");
            return mapList(ps.executeQuery());
        }
    }

    // ── TRI ───────────────────────────────────────────────────

    public List<Quiz> getAllSortedByTitre() throws Exception {
        return executeQuery("SELECT * FROM quiz ORDER BY titre ASC");
    }

    public List<Quiz> getAllSortedByDuree() throws Exception {
        return executeQuery("SELECT * FROM quiz ORDER BY duree ASC");
    }

    public List<Quiz> getAllSortedByDate() throws Exception {
        return executeQuery("SELECT * FROM quiz ORDER BY date_creation DESC");
    }

    // ── VALIDATION MÉTIER ─────────────────────────────────────

    /**
     * Validation complète d'un objet Quiz.
     * Les règles sont regroupées ici (côté service) pour être
     * indépendantes du Controller (API, tests, etc.).
     */
    private void valider(Quiz q) throws Exception {

        // --- Titre ---
        if (q.getTitre() == null || q.getTitre().trim().isEmpty())
            throw new Exception("Le titre du quiz est obligatoire.");
        String titre = q.getTitre().trim();
        if (titre.length() < TITRE_MIN)
            throw new Exception("Le titre doit contenir au moins " + TITRE_MIN + " caractères.");
        if (titre.length() > TITRE_MAX)
            throw new Exception("Le titre ne peut pas dépasser " + TITRE_MAX + " caractères.");

        // --- Durée ---
        if (q.getDuree() < DUREE_MIN || q.getDuree() > DUREE_MAX)
            throw new Exception(
                "La durée doit être comprise entre " + DUREE_MIN + " et " + DUREE_MAX + " minutes."
            );

        // --- Course ID ---
        if (q.getCourseId() <= 0)
            throw new Exception("L'identifiant du cours associé est invalide (doit être > 0).");
    }

    // ── HELPERS ───────────────────────────────────────────────

    private List<Quiz> executeQuery(String sql) throws Exception {
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    private List<Quiz> mapList(ResultSet rs) throws SQLException {
        List<Quiz> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Quiz mapRow(ResultSet rs) throws SQLException {
        return new Quiz(
            rs.getInt   ("id"),
            rs.getString("titre"),
            rs.getInt   ("duree"),
            rs.getTimestamp("date_creation").toLocalDateTime(),
            rs.getInt   ("course_id")
        );
    }
}
