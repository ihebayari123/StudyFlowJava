package edu.connexion3a36.services;

import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizAttemptService
 * ══════════════════
 * Gère la table quiz_attempt :
 *   - Enregistrer un résultat de quiz
 *   - Récupérer l'historique d'un étudiant
 *
 * Structure de la table :
 *   id, score_questions, score_points, total_questions,
 *   started_at, finished_at, user_id, quiz_id
 */
public class QuizAttemptService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── DTO résultat ──────────────────────────────────────────────────────────

    public record AttemptResult(
        int    id,
        int    scoreQuestions,   // nb bonnes réponses
        int    scorePoints,      // score en points (= scoreQuestions ici)
        int    totalQuestions,
        String quizTitre,
        int    quizId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
    ) {
        public double pourcentage() {
            return totalQuestions > 0 ? (scoreQuestions * 100.0 / totalQuestions) : 0;
        }
        public String mention() {
            double p = pourcentage();
            if (p >= 80) return "Excellent";
            if (p >= 60) return "Bien";
            if (p >= 40) return "Passable";
            return "À revoir";
        }
    }

    // ── Enregistrer un résultat ───────────────────────────────────────────────

    /**
     * Sauvegarde le résultat d'un quiz passé par un étudiant.
     *
     * @param userId         ID de l'étudiant (Long depuis Utilisateur.getId())
     * @param quizId         ID du quiz
     * @param scoreQuestions Nombre de bonnes réponses
     * @param totalQuestions Nombre total de questions
     * @param startedAt      Heure de début du quiz
     * @return l'ID de l'attempt créé, ou -1 en cas d'erreur
     */
    public int sauvegarder(long userId, int quizId, int scoreQuestions,
                            int totalQuestions, LocalDateTime startedAt) {
        String sql = """
            INSERT INTO quiz_attempt
              (score_questions, score_points, total_questions, started_at, finished_at, user_id, quiz_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt      (1, scoreQuestions);
            ps.setInt      (2, scoreQuestions);   // score_points = même valeur
            ps.setInt      (3, totalQuestions);
            ps.setTimestamp(4, Timestamp.valueOf(startedAt));
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong     (6, userId);
            ps.setInt      (7, quizId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[QuizAttemptService] Erreur sauvegarde : " + e.getMessage());
        }
        return -1;
    }

    // ── Historique d'un étudiant ──────────────────────────────────────────────

    /**
     * Retourne tous les quiz passés par un étudiant, du plus récent au plus ancien.
     */
    public List<AttemptResult> getHistorique(long userId) {
        List<AttemptResult> list = new ArrayList<>();
        String sql = """
            SELECT a.id, a.score_questions, a.score_points, a.total_questions,
                   a.started_at, a.finished_at, a.quiz_id,
                   q.titre AS quiz_titre
            FROM quiz_attempt a
            LEFT JOIN quiz q ON a.quiz_id = q.id
            WHERE a.user_id = ?
            ORDER BY a.finished_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp fin   = rs.getTimestamp("finished_at");
                Timestamp debut = rs.getTimestamp("started_at");
                list.add(new AttemptResult(
                    rs.getInt   ("id"),
                    rs.getInt   ("score_questions"),
                    rs.getInt   ("score_points"),
                    rs.getInt   ("total_questions"),
                    rs.getString("quiz_titre"),
                    rs.getInt   ("quiz_id"),
                    debut != null ? debut.toLocalDateTime() : null,
                    fin   != null ? fin.toLocalDateTime()   : null
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizAttemptService] Erreur historique : " + e.getMessage());
        }
        return list;
    }

    // ── Meilleur score d'un étudiant sur un quiz ──────────────────────────────

    public int getMeilleurScore(long userId, int quizId) {
        String sql = """
            SELECT MAX(score_questions) FROM quiz_attempt
            WHERE user_id = ? AND quiz_id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt (2, quizId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[QuizAttemptService] Erreur meilleurScore : " + e.getMessage());
        }
        return 0;
    }

    // ── Nombre de tentatives sur un quiz ─────────────────────────────────────

    public int getNbTentatives(long userId, int quizId) {
        String sql = "SELECT COUNT(*) FROM quiz_attempt WHERE user_id = ? AND quiz_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt (2, quizId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[QuizAttemptService] Erreur nbTentatives : " + e.getMessage());
        }
        return 0;
    }
}
