package edu.connexion3a36.services;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WellBeingScoreService implements IService {
    private Connection cnx;

    // Default constructor uses MyConnection
    public WellBeingScoreService() {
        this.cnx = MyConnection.getInstance().getCnx();
    }

    // Constructor for testing - allows injecting a custom connection
    public WellBeingScoreService(Connection testConnection) {
        this.cnx = testConnection;
    }

    @Override
    public void addEntity(Object o) throws SQLException {
        WellBeingScore wbs = (WellBeingScore) o;

        // Trouver le premier stress_survey.id sans well_being_score associé (contrainte UNIQUE)
        String query = "SELECT ss.id FROM stress_survey ss " +
                "LEFT JOIN well_being_score wbs ON ss.id = wbs.survey_id " +
                "WHERE wbs.survey_id IS NULL ORDER BY ss.id ASC LIMIT 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);
        int surveyId = 0;
        if (rs.next()) surveyId = rs.getInt(1);
        rs.close();
        st.close();

        if (surveyId == 0) {
            throw new SQLException(
                    "Tous les surveys ont déjà un score.\nVeuillez d'abord créer un nouveau StressSurvey."
            );
        }

        String req = "INSERT INTO well_being_score (survey_id, recommendation, action_plan, comment, score) VALUES (?,?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, surveyId);
        pst.setString(2, wbs.getRecommendation());
        pst.setString(3, wbs.getAction_plan());
        pst.setString(4, wbs.getComment());
        pst.setInt(5, wbs.getScore());
        pst.executeUpdate();
        pst.close();
        System.out.println("WellBeingScore ajouté ! survey_id = " + surveyId);
    }

    @Override
    public void deleteEntity(Object o) throws SQLException {
        WellBeingScore wbs = (WellBeingScore) o;
        String req = "DELETE FROM well_being_score WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, wbs.getId());
        pst.executeUpdate();
        pst.close();
        System.out.println("WellBeingScore supprimé !");
    }

    @Override
    public void updateEntity(int id, Object o) throws SQLException {
        WellBeingScore wbs = (WellBeingScore) o;
        String req = "UPDATE well_being_score SET recommendation=?, action_plan=?, comment=?, score=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, wbs.getRecommendation());
        pst.setString(2, wbs.getAction_plan());
        pst.setString(3, wbs.getComment());
        pst.setInt(4, wbs.getScore());
        pst.setInt(5, id);
        pst.executeUpdate();
        pst.close();
        System.out.println("WellBeingScore modifié !");
    }

    @Override
    public List<WellBeingScore> getData() throws SQLException {
        List<WellBeingScore> list = new ArrayList<>();
        String req = "SELECT * FROM well_being_score";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            WellBeingScore wbs = new WellBeingScore(
                    rs.getString("recommendation"),
                    rs.getString("action_plan"),
                    rs.getString("comment"),
                    rs.getInt("score")
            );
            wbs.setId(rs.getInt("id"));
            wbs.setSurvey_id(rs.getInt("survey_id"));
            list.add(wbs);
        }
        rs.close();
        st.close();
        return list;
    }
}
