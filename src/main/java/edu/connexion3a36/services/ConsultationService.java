package edu.connexion3a36.services;

import edu.connexion3a36.entities.Consultation;
import edu.connexion3a36.interfaces.IService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService implements IService {
    private Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Object o) throws SQLException {
        Consultation c = (Consultation) o;
        String req = "INSERT INTO consultation (date_de_consultation, motif, genre, niveau, medecin_id, stress_survey_id) VALUES (?,?,?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setTimestamp(1, c.getDate_de_consultation());
        pst.setString(2, c.getMotif());
        pst.setString(3, c.getGenre());
        pst.setString(4, c.getNiveau());
        pst.setInt(5, c.getMedecin_id());
        pst.setInt(6, c.getStress_survey_id());
        pst.executeUpdate();
        pst.close();
        System.out.println("Consultation ajoutée !");
    }

    @Override
    public void deleteEntity(Object o) throws SQLException {
        Consultation c = (Consultation) o;
        String req = "DELETE FROM consultation WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, c.getId());
        pst.executeUpdate();
        pst.close();
        System.out.println("Consultation supprimée !");
    }

    @Override
    public void updateEntity(int id, Object o) throws SQLException {
        Consultation c = (Consultation) o;
        String req = "UPDATE consultation SET date_de_consultation=?, motif=?, genre=?, niveau=?, medecin_id=?, stress_survey_id=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setTimestamp(1, c.getDate_de_consultation());
        pst.setString(2, c.getMotif());
        pst.setString(3, c.getGenre());
        pst.setString(4, c.getNiveau());
        pst.setInt(5, c.getMedecin_id());
        pst.setInt(6, c.getStress_survey_id());
        pst.setInt(7, id);
        pst.executeUpdate();
        pst.close();
        System.out.println("Consultation modifiée !");
    }

    @Override
    public List<Consultation> getData() throws SQLException {
        List<Consultation> list = new ArrayList<>();
        String req = "SELECT * FROM consultation";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            Consultation c = new Consultation(
                    rs.getTimestamp("date_de_consultation"),
                    rs.getString("motif"),
                    rs.getString("genre"),
                    rs.getString("niveau"),
                    rs.getInt("medecin_id"),
                    rs.getInt("stress_survey_id")
            );
            c.setId(rs.getInt("id"));
            list.add(c);
        }
        rs.close();
        st.close();
        return list;
    }
}
