package edu.connexion3a36.services;

import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StressSurveyService implements IService {
    private Connection cnx;

    // Default constructor uses MyConnection
    public StressSurveyService() {
        this.cnx = MyConnection.getInstance().getCnx();
    }

    // Constructor for testing - allows injecting a custom connection
    public StressSurveyService(Connection testConnection) {
        this.cnx = testConnection;
    }

    @Override
    public void addEntity(Object o) throws SQLException {
        StressSurvey s = (StressSurvey) o;
        String req = "INSERT INTO stress_survey (date, sleep_hours, study_hours, user_id) VALUES (?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setDate(1, s.getDate());
        pst.setInt(2, s.getSleep_hours());
        pst.setInt(3, s.getStudy_hours());
        pst.setInt(4, s.getUser_id());
        pst.executeUpdate();
        pst.close();
        System.out.println("StressSurvey ajouté !");
    }

    @Override
    public void deleteEntity(Object o) throws SQLException {
        StressSurvey s = (StressSurvey) o;
        int surveyId = s.getId();
        
        // Enable foreign key checks temporarily to allow manual cascade
        cnx.setAutoCommit(false);
        
        try {
            // 1. Supprimer les consultations liées (CASCADE)
            String deleteConsultations = "DELETE FROM consultation WHERE stress_survey_id = ?";
            try (PreparedStatement pstConsult = cnx.prepareStatement(deleteConsultations)) {
                pstConsult.setInt(1, surveyId);
                int consultationsDeleted = pstConsult.executeUpdate();
                System.out.println(consultationsDeleted + " Consultation(s) supprimée(s) en cascade");
            }
            
            // 2. Supprimer les well_being_score liés (CASCADE)
            String deleteWellBeing = "DELETE FROM well_being_score WHERE survey_id = ?";
            try (PreparedStatement pstWellBeing = cnx.prepareStatement(deleteWellBeing)) {
                pstWellBeing.setInt(1, surveyId);
                int wellBeingDeleted = pstWellBeing.executeUpdate();
                System.out.println(wellBeingDeleted + " WellBeingScore(s) supprimé(s) en cascade");
            }
            
            // 3. Supprimer le StressSurvey lui-même
            String req = "DELETE FROM stress_survey WHERE id = ?";
            try (PreparedStatement pst = cnx.prepareStatement(req)) {
                pst.setInt(1, surveyId);
                pst.executeUpdate();
            }
            
            // Valider la transaction
            cnx.commit();
            System.out.println("StressSurvey supprimé avec succès (suppression en cascade complète) !");
            
        } catch (SQLException e) {
            // Annuler la transaction en cas d'erreur
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }
    
    /**
     * Supprime un StressSurvey par son ID (méthode alternative pour les contrôles)
     */
    public void deleteById(int id) throws SQLException {
        StressSurvey s = new StressSurvey();
        s.setId(id);
        deleteEntity(s);
    }

    @Override
    public void updateEntity(int id, Object o) throws SQLException {
        StressSurvey s = (StressSurvey) o;
        String req = "UPDATE stress_survey SET date=?, sleep_hours=?, study_hours=?, user_id=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setDate(1, s.getDate());
        pst.setInt(2, s.getSleep_hours());
        pst.setInt(3, s.getStudy_hours());
        pst.setInt(4, s.getUser_id());
        pst.setInt(5, id);
        pst.executeUpdate();
        pst.close();
        System.out.println("StressSurvey modifié !");
    }

    /**
     * Récupère un StressSurvey par son ID. Retourne null si non trouvé.
     */
    public StressSurvey getById(int id) throws SQLException {
        String req = "SELECT * FROM stress_survey WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    StressSurvey s = new StressSurvey(
                            rs.getDate("date"),
                            rs.getInt("sleep_hours"),
                            rs.getInt("study_hours"),
                            rs.getInt("user_id")
                    );
                    s.setId(rs.getInt("id"));
                    return s;
                }
            }
        }
        return null;
    }

    @Override
    public List<StressSurvey> getData() throws SQLException {
        List<StressSurvey> list = new ArrayList<>();
        String req = "SELECT * FROM stress_survey";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            StressSurvey s = new StressSurvey(
                    rs.getDate("date"),
                    rs.getInt("sleep_hours"),
                    rs.getInt("study_hours"),
                    rs.getInt("user_id")
            );
            s.setId(rs.getInt("id"));
            list.add(s);
        }
        rs.close();
        st.close();
        return list;
    }
}
