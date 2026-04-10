package edu.connexion3a36.services;

import edu.connexion3a36.entities.StressSurvey;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StressSurveyService implements IService {
    private Connection cnx = MyConnection.getInstance().getCnx();

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
        String req = "DELETE FROM stress_survey WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, s.getId());
        pst.executeUpdate();
        pst.close();
        System.out.println("StressSurvey supprimé !");
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
