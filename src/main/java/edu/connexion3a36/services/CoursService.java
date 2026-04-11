package edu.connexion3a36.services;

import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.tools.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoursService {

    private Connection cnx;

    public CoursService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public List<Cours> findAll() {
        List<Cours> courses = new ArrayList<>();
        String query = "SELECT * FROM cours ORDER BY id DESC";

        try {
            Statement stmt = cnx.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Cours cours = new Cours();
                cours.setId(rs.getLong("id"));
                cours.setTitre(rs.getString("titre"));
                cours.setDescription(rs.getString("description"));
                cours.setImage(rs.getString("image"));
                courses.add(cours);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findAll: " + e.getMessage());
            e.printStackTrace();
        }

        return courses;
    }

    public Cours findById(Long id) {
        String query = "SELECT * FROM cours WHERE id = ?";

        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setLong(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                Cours cours = new Cours();
                cours.setId(rs.getLong("id"));
                cours.setTitre(rs.getString("titre"));
                cours.setDescription(rs.getString("description"));
                cours.setImage(rs.getString("image"));
                return cours;
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public void save(Cours cours) {
        String query = "INSERT INTO cours (titre, description, image, user_id) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, cours.getTitre());
            pst.setString(2, cours.getDescription());
            pst.setString(3, cours.getImage());
            pst.setLong(4, cours.getUserId() != null ? cours.getUserId() : 1L);

            int affectedRows = pst.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = pst.getGeneratedKeys();
                if (rs.next()) {
                    cours.setId(rs.getLong(1));
                }
                System.out.println("✅ Cours sauvegardé avec succès: " + cours.getTitre());
            } else {
                System.err.println("❌ Aucune ligne insérée pour le cours: " + cours.getTitre());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur save: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage(), e);
        }
    }

    public void update(Cours cours) {
        String query = "UPDATE cours SET titre = ?, description = ?, image = ? WHERE id = ?";

        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, cours.getTitre());
            pst.setString(2, cours.getDescription());
            pst.setString(3, cours.getImage());
            pst.setLong(4, cours.getId());  // Important: utiliser l'ID existant

            int affectedRows = pst.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Cours mis à jour: ID " + cours.getId() + " - " + cours.getTitre());
            } else {
                System.err.println("❌ Aucun cours trouvé avec ID: " + cours.getId());
            }

            pst.close();
        } catch (SQLException e) {
            System.err.println("❌ Erreur update: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    public void delete(Long id) {
        // D'abord supprimer les chapitres liés
        String deleteChapitres = "DELETE FROM chapitre WHERE course_id = ?";
        // Ensuite supprimer les quiz liés
        String deleteQuizzes = "DELETE FROM quiz WHERE course_id = ?";
        // Enfin supprimer le cours
        String deleteCours = "DELETE FROM cours WHERE id = ?";

        try {
            // Désactiver temporairement les vérifications de clés étrangères (MySQL)
            Statement stmt = cnx.createStatement();
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Supprimer les chapitres
            PreparedStatement pstChapitres = cnx.prepareStatement(deleteChapitres);
            pstChapitres.setLong(1, id);
            int chapitresSupprimes = pstChapitres.executeUpdate();
            System.out.println("📖 Chapitres supprimés: " + chapitresSupprimes);

            // Supprimer les quiz
            PreparedStatement pstQuizzes = cnx.prepareStatement(deleteQuizzes);
            pstQuizzes.setLong(1, id);
            int quizzesSupprimes = pstQuizzes.executeUpdate();
            System.out.println("❓ Quiz supprimés: " + quizzesSupprimes);

            // Supprimer le cours
            PreparedStatement pstCours = cnx.prepareStatement(deleteCours);
            pstCours.setLong(1, id);
            int coursSupprime = pstCours.executeUpdate();

            if (coursSupprime > 0) {
                System.out.println("✅ Cours supprimé: ID " + id);
            } else {
                System.err.println("❌ Aucun cours trouvé avec ID: " + id);
            }

            // Réactiver les vérifications
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

        } catch (SQLException e) {
            System.err.println("❌ Erreur delete: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage(), e);
        }
    }

    public List<Cours> searchByTitle(String keyword) {
        List<Cours> courses = new ArrayList<>();
        String query = "SELECT * FROM cours WHERE titre LIKE ? ORDER BY id DESC";

        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, "%" + keyword + "%");
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Cours cours = new Cours();
                cours.setId(rs.getLong("id"));
                cours.setTitre(rs.getString("titre"));
                cours.setDescription(rs.getString("description"));
                cours.setImage(rs.getString("image"));
                courses.add(cours);
            }
        } catch (SQLException e) {
            System.err.println("Erreur searchByTitle: " + e.getMessage());
            e.printStackTrace();
        }

        return courses;
    }
}