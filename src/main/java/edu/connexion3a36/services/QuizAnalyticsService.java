package edu.connexion3a36.services;

import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * QuizAnalyticsService — Moteur d'analyse local (zéro API externe)
 * ══════════════════════════════════════════════════════════════════
 * Exploite la table quiz_attempt pour produire des insights utiles.
 *
 * Fonctionnalités :
 *   1. Tableau de bord étudiant    — progression, meilleurs/pires quiz
 *   2. Tableau de bord enseignant  — quiz les plus difficiles, taux de réussite
 *   3. Détection de quiz abandonnés (started_at non null, finished_at null)
 *   4. Détection d'étudiants en difficulté (score < 40% sur 3+ tentatives)
 *   5. Classement des étudiants par quiz (leaderboard)
 *   6. Analyse du temps moyen par quiz
 *   7. Courbe de progression d'un étudiant sur un quiz
 *   8. Quiz jamais tentés par un étudiant
 */
public class QuizAnalyticsService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ══════════════════════════════════════════════════════════════════════════
    // DTOs
    // ══════════════════════════════════════════════════════════════════════════

    public record QuizStat(
            int    quizId,
            String quizTitre,
            int    nbTentatives,
            double scoreMoyen,       // en %
            int    meilleurScore,    // en %
            double tempsMoyenMin     // durée moyenne en minutes
    ) {}

    public record EtudiantStat(
            long   userId,
            String nomComplet,
            int    nbQuizTentes,
            double scoreMoyen,
            int    nbQuizReussis,    // score >= 60%
            String tendance          // "progression" | "stable" | "régression"
    ) {}

    public record ProgressionPoint(
            int      tentative,
            int      score,          // en %
            String   date
    ) {}

    public record LeaderboardEntry(
            int    rang,
            long   userId,
            String nomComplet,
            int    meilleurScore,    // en %
            int    nbTentatives
    ) {}

    public record AlerteEtudiant(
            long   userId,
            String nomComplet,
            String email,
            int    quizId,
            String quizTitre,
            double scoreMoyen,
            int    nbTentatives,
            String type             // "EN_DIFFICULTE" | "ABANDONS" | "INACTIF"
    ) {}

    // ══════════════════════════════════════════════════════════════════════════
    // 1. TABLEAU DE BORD ÉTUDIANT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Résumé global d'un étudiant : score moyen, nb quiz, tendance.
     */
    public Map<String, Object> getResumEtudiant(long userId) {
        Map<String, Object> resume = new LinkedHashMap<>();

        String sql = """
            SELECT
                COUNT(*)                                    AS nb_tentatives,
                COUNT(DISTINCT quiz_id)                     AS nb_quiz_distincts,
                ROUND(AVG(score_questions * 100.0 / NULLIF(total_questions,0)), 1)
                                                            AS score_moyen,
                MAX(score_questions * 100 / NULLIF(total_questions,1))
                                                            AS meilleur_pct,
                MIN(score_questions * 100 / NULLIF(total_questions,1))
                                                            AS pire_pct,
                SUM(CASE WHEN score_questions * 100.0 / NULLIF(total_questions,0) >= 60
                         THEN 1 ELSE 0 END)                AS nb_reussis
            FROM quiz_attempt
            WHERE user_id = ? AND finished_at IS NOT NULL
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int nb = rs.getInt("nb_tentatives");
                resume.put("nb_tentatives",    nb);
                resume.put("nb_quiz_distincts", rs.getInt("nb_quiz_distincts"));
                resume.put("score_moyen_pct",   rs.getDouble("score_moyen"));
                resume.put("meilleur_pct",      rs.getInt("meilleur_pct"));
                resume.put("pire_pct",          rs.getInt("pire_pct"));
                resume.put("nb_reussis",        rs.getInt("nb_reussis"));
                resume.put("taux_reussite_pct",
                        nb > 0 ? Math.round(rs.getInt("nb_reussis") * 100.0 / nb) : 0);
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] resumEtudiant: " + e.getMessage());
        }

        // Tendance : comparer les 3 dernières tentatives aux 3 précédentes
        resume.put("tendance", calculerTendance(userId));
        return resume;
    }

    private String calculerTendance(long userId) {
        String sql = """
            SELECT score_questions * 100.0 / NULLIF(total_questions,0) AS pct
            FROM quiz_attempt
            WHERE user_id = ? AND finished_at IS NOT NULL
            ORDER BY finished_at DESC LIMIT 6
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            List<Double> scores = new ArrayList<>();
            while (rs.next()) scores.add(rs.getDouble("pct"));
            if (scores.size() < 4) return "insuffisant";

            double recent = scores.subList(0, 3).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            double ancien = scores.subList(3, scores.size()).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);

            if (recent > ancien + 5)  return "progression";
            if (recent < ancien - 5)  return "régression";
            return "stable";
        } catch (SQLException e) {
            return "inconnu";
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. MEILLEURS ET PIRES QUIZ POUR UN ÉTUDIANT
    // ══════════════════════════════════════════════════════════════════════════

    public List<QuizStat> getMeilleursQuizEtudiant(long userId, int limit) {
        return getQuizStatsEtudiant(userId, "DESC", limit);
    }

    public List<QuizStat> getPiresQuizEtudiant(long userId, int limit) {
        return getQuizStatsEtudiant(userId, "ASC", limit);
    }

    private List<QuizStat> getQuizStatsEtudiant(long userId, String order, int limit) {
        String sql = """
            SELECT q.id, q.titre, q.duree,
                   COUNT(a.id)              AS nb_tentatives,
                   ROUND(AVG(a.score_questions * 100.0 / NULLIF(a.total_questions,0)), 1)
                                            AS score_moyen,
                   MAX(a.score_questions * 100 / NULLIF(a.total_questions,1))
                                            AS meilleur_pct,
                   AVG(TIMESTAMPDIFF(MINUTE, a.started_at, a.finished_at))
                                            AS temps_moyen_min
            FROM quiz_attempt a
            JOIN quiz q ON a.quiz_id = q.id
            WHERE a.user_id = ? AND a.finished_at IS NOT NULL
            GROUP BY q.id, q.titre, q.duree
            ORDER BY score_moyen """ + " " + order + " LIMIT ?" ;
        List<QuizStat> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt (2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new QuizStat(
                        rs.getInt("id"), rs.getString("titre"),
                        rs.getInt("nb_tentatives"),
                        rs.getDouble("score_moyen"),
                        rs.getInt("meilleur_pct"),
                        rs.getDouble("temps_moyen_min")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] quizStatsEtudiant: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. COURBE DE PROGRESSION (historique des scores sur un quiz)
    // ══════════════════════════════════════════════════════════════════════════

    public List<ProgressionPoint> getProgression(long userId, int quizId) {
        String sql = """
            SELECT
                ROW_NUMBER() OVER (ORDER BY finished_at) AS tentative,
                score_questions * 100 / NULLIF(total_questions,1)  AS pct,
                DATE_FORMAT(finished_at, '%d/%m/%Y')               AS date_fmt
            FROM quiz_attempt
            WHERE user_id = ? AND quiz_id = ? AND finished_at IS NOT NULL
            ORDER BY finished_at
            """;
        List<ProgressionPoint> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt (2, quizId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new ProgressionPoint(
                        rs.getInt("tentative"),
                        rs.getInt("pct"),
                        rs.getString("date_fmt")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] progression: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. QUIZ JAMAIS TENTÉS PAR UN ÉTUDIANT
    // ══════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getQuizNonTentes(long userId) {
        String sql = """
            SELECT q.id, q.titre, q.duree,
                   (SELECT COUNT(*) FROM question WHERE quiz_id = q.id) AS nb_questions
            FROM quiz q
            WHERE q.id NOT IN (
                SELECT DISTINCT quiz_id FROM quiz_attempt WHERE user_id = ?
            )
            ORDER BY q.titre
            """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           rs.getInt("id"));
                m.put("titre",        rs.getString("titre"));
                m.put("duree",        rs.getInt("duree"));
                m.put("nb_questions", rs.getInt("nb_questions"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] quizNonTentes: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. TABLEAU DE BORD ENSEIGNANT — stats globales par quiz
    // ══════════════════════════════════════════════════════════════════════════

    public List<QuizStat> getStatsGlobalesParQuiz() {
        String sql = """
            SELECT q.id, q.titre, q.duree,
                   COUNT(a.id)  AS nb_tentatives,
                   ROUND(AVG(a.score_questions * 100.0 / NULLIF(a.total_questions,0)), 1)
                                AS score_moyen,
                   MAX(a.score_questions * 100 / NULLIF(a.total_questions,1))
                                AS meilleur_pct,
                   AVG(TIMESTAMPDIFF(MINUTE, a.started_at, a.finished_at))
                                AS temps_moyen_min
            FROM quiz q
            LEFT JOIN quiz_attempt a ON a.quiz_id = q.id AND a.finished_at IS NOT NULL
            GROUP BY q.id, q.titre, q.duree
            ORDER BY score_moyen ASC
            """;
        List<QuizStat> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new QuizStat(
                        rs.getInt("id"), rs.getString("titre"),
                        rs.getInt("nb_tentatives"),
                        rs.getDouble("score_moyen"),
                        rs.getInt("meilleur_pct"),
                        rs.getDouble("temps_moyen_min")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] statsGlobales: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. LEADERBOARD — classement par quiz
    // ══════════════════════════════════════════════════════════════════════════

    public List<LeaderboardEntry> getLeaderboard(int quizId, int limit) {
        String sql = """
            SELECT
                ROW_NUMBER() OVER (ORDER BY MAX(a.score_questions * 100.0
                    / NULLIF(a.total_questions,0)) DESC, MIN(a.finished_at)) AS rang,
                u.id AS user_id,
                CONCAT(u.prenom, ' ', u.nom)                  AS nom_complet,
                MAX(a.score_questions * 100 / NULLIF(a.total_questions,1))
                                                               AS meilleur_pct,
                COUNT(a.id)                                    AS nb_tentatives
            FROM quiz_attempt a
            JOIN utilisateur u ON u.id = a.user_id
            WHERE a.quiz_id = ? AND a.finished_at IS NOT NULL
            GROUP BY u.id, u.prenom, u.nom
            ORDER BY meilleur_pct DESC, nb_tentatives ASC
            LIMIT ?
            """;
        List<LeaderboardEntry> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            int rang = 1;
            while (rs.next()) {
                list.add(new LeaderboardEntry(
                        rang++,
                        rs.getLong("user_id"),
                        rs.getString("nom_complet"),
                        rs.getInt("meilleur_pct"),
                        rs.getInt("nb_tentatives")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] leaderboard: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. ALERTES — étudiants en difficulté
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne les étudiants avec score moyen < seuilPct% sur nb_min tentatives ou plus.
     */
    public List<AlerteEtudiant> getEtudiantsEnDifficulte(double seuilPct, int nbMinTentatives) {
        String sql = """
            SELECT
                u.id, CONCAT(u.prenom,' ',u.nom) AS nom_complet, u.email,
                q.id AS quiz_id, q.titre AS quiz_titre,
                ROUND(AVG(a.score_questions * 100.0 / NULLIF(a.total_questions,0)), 1)
                        AS score_moyen,
                COUNT(a.id) AS nb_tentatives
            FROM quiz_attempt a
            JOIN utilisateur u ON u.id = a.user_id
            JOIN quiz q        ON q.id = a.quiz_id
            WHERE a.finished_at IS NOT NULL
            GROUP BY u.id, u.nom, u.prenom, u.email, q.id, q.titre
            HAVING score_moyen < ? AND nb_tentatives >= ?
            ORDER BY score_moyen ASC
            """;
        List<AlerteEtudiant> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDouble(1, seuilPct);
            ps.setInt   (2, nbMinTentatives);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new AlerteEtudiant(
                        rs.getLong  ("id"),
                        rs.getString("nom_complet"),
                        rs.getString("email"),
                        rs.getInt   ("quiz_id"),
                        rs.getString("quiz_titre"),
                        rs.getDouble("score_moyen"),
                        rs.getInt   ("nb_tentatives"),
                        "EN_DIFFICULTE"
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] enDifficulte: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. STATISTIQUES GLOBALES RAPIDES (pour dashboard enseignant)
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getStatsGlobalesRapides() {
        Map<String, Object> stats = new LinkedHashMap<>();
        String sql = """
            SELECT
                COUNT(*)                           AS total_tentatives,
                COUNT(DISTINCT user_id)            AS nb_etudiants_actifs,
                COUNT(DISTINCT quiz_id)            AS nb_quiz_tentes,
                ROUND(AVG(score_questions * 100.0
                    / NULLIF(total_questions,0)),1) AS score_moyen_global,
                SUM(CASE WHEN score_questions * 100.0
                    / NULLIF(total_questions,0) >= 60 THEN 1 ELSE 0 END)
                                                   AS nb_reussis,
                AVG(TIMESTAMPDIFF(MINUTE, started_at, finished_at))
                                                   AS temps_moyen_min
            FROM quiz_attempt
            WHERE finished_at IS NOT NULL
            """;
        try (Statement st  = cnx.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total_tentatives");
                int reuss = rs.getInt("nb_reussis");
                stats.put("total_tentatives",     total);
                stats.put("nb_etudiants_actifs",  rs.getInt("nb_etudiants_actifs"));
                stats.put("nb_quiz_tentes",       rs.getInt("nb_quiz_tentes"));
                stats.put("score_moyen_global",   rs.getDouble("score_moyen_global"));
                stats.put("taux_reussite_global", total > 0
                        ? Math.round(reuss * 100.0 / total) : 0);
                stats.put("temps_moyen_min",      Math.round(rs.getDouble("temps_moyen_min")));
            }
        } catch (SQLException e) {
            System.err.println("[Analytics] statsRapides: " + e.getMessage());
        }

        // Quiz le plus difficile (score moyen le plus bas)
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("""
                SELECT q.titre,
                    ROUND(AVG(a.score_questions*100.0/NULLIF(a.total_questions,0)),1) AS moy
                FROM quiz_attempt a JOIN quiz q ON q.id=a.quiz_id
                WHERE a.finished_at IS NOT NULL
                GROUP BY q.id, q.titre ORDER BY moy ASC LIMIT 1
             """)) {
            if (rs.next()) {
                stats.put("quiz_plus_difficile", rs.getString("titre"));
                stats.put("quiz_plus_difficile_score", rs.getDouble("moy"));
            }
        } catch (SQLException ignored) {}

        // Quiz le plus populaire (plus de tentatives)
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("""
                SELECT q.titre, COUNT(*) AS nb
                FROM quiz_attempt a JOIN quiz q ON q.id=a.quiz_id
                WHERE a.finished_at IS NOT NULL
                GROUP BY q.id, q.titre ORDER BY nb DESC LIMIT 1
             """)) {
            if (rs.next()) {
                stats.put("quiz_plus_populaire", rs.getString("titre"));
                stats.put("quiz_plus_populaire_nb", rs.getInt("nb"));
            }
        } catch (SQLException ignored) {}

        return stats;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. CONSEIL LOCAL (zéro API) basé sur les stats
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Génère un conseil textuel entièrement local basé sur les données BDD.
     * Zéro appel réseau, zéro API.
     */
    public String genererConseilLocal(long userId) {
        Map<String, Object> resume   = getResumEtudiant(userId);
        List<QuizStat>      pires    = getPiresQuizEtudiant(userId, 1);
        List<QuizStat>      meilleurs = getMeilleursQuizEtudiant(userId, 1);
        List<Map<String,Object>> nonTentes = getQuizNonTentes(userId);

        StringBuilder sb = new StringBuilder();

        double moy      = (Double) resume.getOrDefault("score_moyen_pct", 0.0);
        String tendance = (String) resume.getOrDefault("tendance", "inconnu");
        int    nbTentes = (int)    resume.getOrDefault("nb_quiz_distincts", 0);
        int    nbTotal  = nonTentes.size() + nbTentes;

        // Intro selon le score moyen
        if      (moy >= 80) sb.append("Excellente performance ! Votre moyenne de ").append(Math.round(moy)).append("% est remarquable.\n\n");
        else if (moy >= 60) sb.append("Bon travail ! Avec ").append(Math.round(moy)).append("% de moyenne, vous êtes sur la bonne voie.\n\n");
        else if (moy >= 40) sb.append("Résultat correct, mais vous pouvez faire mieux. Moyenne actuelle : ").append(Math.round(moy)).append("%.\n\n");
        else                sb.append("Vos résultats méritent attention. Moyenne de ").append(Math.round(moy)).append("% — ne vous découragez pas !\n\n");

        // Tendance
        switch (tendance) {
            case "progression" -> sb.append("📈 Votre tendance est positive : vos dernières tentatives sont meilleures que les précédentes. Continuez !\n\n");
            case "régression"  -> sb.append("📉 Attention : vos scores récents sont en baisse. Prenez le temps de réviser avant votre prochaine tentative.\n\n");
            case "stable"      -> sb.append("📊 Votre niveau est stable. Pour progresser, essayez les quiz que vous n'avez pas encore faits.\n\n");
        }

        // Pire quiz
        if (!pires.isEmpty()) {
            QuizStat pire = pires.get(0);
            sb.append("⚠️ Votre point faible : le quiz *").append(pire.quizTitre())
                    .append("* (").append(Math.round(pire.scoreMoyen())).append("% de moyenne sur ")
                    .append(pire.nbTentatives()).append(" tentative(s)). ")
                    .append("Revoyez ce sujet en priorité.\n\n");
        }

        // Meilleur quiz
        if (!meilleurs.isEmpty()) {
            QuizStat best = meilleurs.get(0);
            sb.append("✅ Votre point fort : le quiz *").append(best.quizTitre())
                    .append("* avec ").append(Math.round(best.scoreMoyen())).append("% de moyenne.\n\n");
        }

        // Quiz non tentés
        if (!nonTentes.isEmpty()) {
            sb.append("📋 Vous n'avez pas encore tenté ");
            if (nonTentes.size() == 1) {
                sb.append("le quiz *").append(nonTentes.get(0).get("titre")).append("*. Essayez-le !");
            } else {
                sb.append(nonTentes.size()).append(" quiz : ")
                        .append(nonTentes.stream().limit(3)
                                .map(m -> "*" + m.get("titre") + "*")
                                .collect(Collectors.joining(", ")));
                if (nonTentes.size() > 3) sb.append("...");
                sb.append(".");
            }
            sb.append("\n\n");
        }

        // Conseil final selon le score
        if      (moy >= 80) sb.append("Défi : essayez d'atteindre 100% sur votre quiz le plus difficile !");
        else if (moy >= 60) sb.append("Objectif : dépasser 80% de moyenne. Vous en êtes capable !");
        else if (moy >= 40) sb.append("Conseil : retentez chaque quiz raté au moins une fois après révision.");
        else                sb.append("Plan d'action : commencez par les quiz faciles pour reprendre confiance.");

        return sb.toString();
    }
}