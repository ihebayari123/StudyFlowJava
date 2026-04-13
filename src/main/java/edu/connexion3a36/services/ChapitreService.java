package edu.connexion3a36.services;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChapitreService {

    private Connection cnx;
    private CoursService coursService;

    public ChapitreService() {
        cnx = MyConnection.getInstance().getCnx();
        coursService = new CoursService();
    }

    public List<Chapitre> findByCourse(Long courseId) {
        List<Chapitre> chapitres = new ArrayList<>();
        String query = "SELECT * FROM chapitre WHERE course_id = ? ORDER BY ordre ASC";

        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setLong(1, courseId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Chapitre chapitre = mapRow(rs);
                chapitres.add(chapitre);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByCourse: " + e.getMessage());
            e.printStackTrace();
        }

        return chapitres;
    }

    public List<Chapitre> findAll() {
        List<Chapitre> chapitres = new ArrayList<>();
        String query = "SELECT * FROM chapitre ORDER BY course_id ASC, ordre ASC";

        try {
            Statement stmt = cnx.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                chapitres.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findAll: " + e.getMessage());
            e.printStackTrace();
        }

        return chapitres;
    }

    public Chapitre findById(Long id) {
        String query = "SELECT * FROM chapitre WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setLong(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void save(Chapitre chapitre) {
        String query = "INSERT INTO chapitre (titre, contenu, ordre, course_id, content_type, video_url, file_name, image_url, duration_minutes) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, chapitre.getTitre());
            pst.setString(2, chapitre.getContenu());
            pst.setInt(3, chapitre.getOrdre());
            pst.setLong(4, chapitre.getCourse().getId());
            pst.setString(5, chapitre.getContentType());
            pst.setString(6, chapitre.getVideoUrl());
            pst.setString(7, chapitre.getFileName());
            pst.setString(8, chapitre.getImageUrl());
            if (chapitre.getDurationMinutes() != null) {
                pst.setInt(9, chapitre.getDurationMinutes());
            } else {
                pst.setNull(9, Types.INTEGER);
            }

            int rows = pst.executeUpdate();
            if (rows > 0) {
                ResultSet keys = pst.getGeneratedKeys();
                if (keys.next()) chapitre.setId(keys.getLong(1));
                System.out.println("✅ Chapitre sauvegardé: " + chapitre.getTitre());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur save: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage(), e);
        }
    }

    public void update(Chapitre chapitre) {
        String query = "UPDATE chapitre SET titre=?, contenu=?, ordre=?, content_type=?, video_url=?, file_name=?, image_url=?, duration_minutes=? WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, chapitre.getTitre());
            pst.setString(2, chapitre.getContenu());
            pst.setInt(3, chapitre.getOrdre());
            pst.setString(4, chapitre.getContentType());
            pst.setString(5, chapitre.getVideoUrl());
            pst.setString(6, chapitre.getFileName());
            pst.setString(7, chapitre.getImageUrl());
            if (chapitre.getDurationMinutes() != null) {
                pst.setInt(8, chapitre.getDurationMinutes());
            } else {
                pst.setNull(8, Types.INTEGER);
            }
            pst.setLong(9, chapitre.getId());

            int rows = pst.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Chapitre mis à jour: " + chapitre.getTitre());
            } else {
                System.err.println("❌ Aucun chapitre trouvé avec ID: " + chapitre.getId());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur update: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    public void delete(Long id) {
        String query = "DELETE FROM chapitre WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setLong(1, id);
            int rows = pst.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Chapitre supprimé: ID " + id);
            } else {
                System.err.println("❌ Aucun chapitre trouvé avec ID: " + id);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur delete: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage(), e);
        }
    }

    public List<Chapitre> searchByTitle(Long courseId, String keyword) {
        List<Chapitre> chapitres = new ArrayList<>();
        String query = "SELECT * FROM chapitre WHERE course_id = ? AND titre LIKE ? ORDER BY ordre ASC";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setLong(1, courseId);
            pst.setString(2, "%" + keyword + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                chapitres.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur searchByTitle: " + e.getMessage());
            e.printStackTrace();
        }
        return chapitres;
    }

    public int getNextOrdre(Long courseId) {
        String query = "SELECT COALESCE(MAX(ordre), 0) + 1 FROM chapitre WHERE course_id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setLong(1, courseId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getNextOrdre: " + e.getMessage());
        }
        return 1;
    }

    private Chapitre mapRow(ResultSet rs) throws SQLException {
        Chapitre c = new Chapitre();
        c.setId(rs.getLong("id"));
        c.setTitre(rs.getString("titre"));
        c.setContenu(rs.getString("contenu"));
        c.setOrdre(rs.getInt("ordre"));
        c.setContentType(rs.getString("content_type"));
        c.setVideoUrl(rs.getString("video_url"));
        c.setFileName(rs.getString("file_name"));
        c.setImageUrl(rs.getString("image_url"));
        int dur = rs.getInt("duration_minutes");
        c.setDurationMinutes(rs.wasNull() ? null : dur);

        // Attach the parent course (lightweight stub)
        Cours cours = new Cours();
        cours.setId(rs.getLong("course_id"));
        c.setCourse(cours);

        return c;
    }
}
