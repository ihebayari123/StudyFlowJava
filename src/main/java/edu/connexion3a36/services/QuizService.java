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

    private static final int TITRE_MIN = 3;
    private static final int TITRE_MAX = 100;
    private static final int DUREE_MIN = 1;
    private static final int DUREE_MAX = 180;

    // ── CREATE ────────────────────────────────────────────────

    @Override
    public void addEntity(Quiz q) throws SQLException {
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
    public List<Quiz> getData() throws SQLException {
        return executeQuery("SELECT * FROM quiz ORDER BY date_creation DESC");
    }

    // ── UPDATE ────────────────────────────────────────────────

    @Override
    public void updateEntity(int id, Quiz q) throws SQLException {
        if (id <= 0)
            throw new SQLException("ID du quiz invalide pour la mise à jour.");
        valider(q);
        String sql = "UPDATE quiz SET titre=?, duree=?, course_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getTitre().trim());
            ps.setInt   (2, q.getDuree());
            ps.setInt   (3, q.getCourseId());
            ps.setInt   (4, id);
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────

    @Override
    public void deleteEntity(Quiz q) throws SQLException {
        if (q.getId() <= 0)
            throw new SQLException("L'ID du quiz à supprimer est invalide.");
        // Cascade : supprimer les questions liées d'abord
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM question WHERE quiz_id=?")) {
            ps.setInt(1, q.getId());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM quiz WHERE id=?")) {
            ps.setInt(1, q.getId());
            ps.executeUpdate();
        }
    }

    // ── SEARCH ────────────────────────────────────────────────

    public List<Quiz> search(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty())
            return getData();
        String sql = "SELECT * FROM quiz WHERE titre LIKE ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword.trim() + "%");
            return mapList(ps.executeQuery());
        }
    }

    // ── TRI ───────────────────────────────────────────────────

    public List<Quiz> getAllSortedByTitre() throws SQLException {
        return executeQuery("SELECT * FROM quiz ORDER BY titre ASC");
    }

    public List<Quiz> getAllSortedByDuree() throws SQLException {
        return executeQuery("SELECT * FROM quiz ORDER BY duree ASC");
    }

    public List<Quiz> getAllSortedByDate() throws SQLException {
        return executeQuery("SELECT * FROM quiz ORDER BY date_creation DESC");
    }

    // ── GET BY ID (utilitaire) ────────────────────────────────

    public Quiz getById(int id) throws SQLException {
        if (id <= 0) throw new SQLException("L'ID doit être un entier positif.");
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM quiz WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── VALIDATION MÉTIER ─────────────────────────────────────

    private void valider(Quiz q) throws SQLException {
        if (q.getTitre() == null || q.getTitre().trim().isEmpty())
            throw new SQLException("Le titre du quiz est obligatoire.");
        String titre = q.getTitre().trim();
        if (titre.length() < TITRE_MIN)
            throw new SQLException(
                "Le titre doit contenir au moins " + TITRE_MIN + " caractères.");
        if (titre.length() > TITRE_MAX)
            throw new SQLException(
                "Le titre ne peut pas dépasser " + TITRE_MAX + " caractères.");
        if (q.getDuree() < DUREE_MIN || q.getDuree() > DUREE_MAX)
            throw new SQLException(
                "La durée doit être comprise entre " + DUREE_MIN +
                " et " + DUREE_MAX + " minutes.");
        if (q.getCourseId() <= 0)
            throw new SQLException(
                "L'identifiant du cours associé est invalide (doit être > 0).");
    }

    // ── HELPERS ───────────────────────────────────────────────

    private List<Quiz> executeQuery(String sql) throws SQLException {
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
            rs.getInt      ("id"),
            rs.getString   ("titre"),
            rs.getInt      ("duree"),
            rs.getTimestamp("date_creation").toLocalDateTime(),
            rs.getInt      ("course_id")
        );
    }
}
