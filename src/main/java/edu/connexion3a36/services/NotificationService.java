package edu.connexion3a36.services;

import edu.connexion3a36.entities.Notification;
import edu.connexion3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    Connection cnx = MyConnection.getInstance().getCnx();

    public void addNotification(Notification n) throws SQLException {
        String requete = "INSERT INTO notification (title, message, type, is_read, created_at, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setString(1, n.getTitle());
        pst.setString(2, n.getMessage());
        pst.setString(3, n.getType());
        pst.setBoolean(4, false);
        pst.setTimestamp(5, Timestamp.valueOf(n.getCreatedAt()));
        pst.setInt(6, n.getUserId());
        pst.executeUpdate();
    }

    public List<Notification> getUnreadByUserId(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String requete = "SELECT * FROM notification WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            Notification n = new Notification();
            n.setId(rs.getInt("id"));
            n.setTitle(rs.getString("title"));
            n.setMessage(rs.getString("message"));
            n.setType(rs.getString("type"));
            n.setRead(rs.getBoolean("is_read"));
            n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            n.setUserId(rs.getInt("user_id"));
            list.add(n);
        }
        return list;
    }

    public void markAllAsRead(int userId) throws SQLException {
        String requete = "UPDATE notification SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setInt(1, userId);
        pst.executeUpdate();
    }

    public int countUnread(int userId) throws SQLException {
        String requete = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        rs.next();
        return rs.getInt(1);
    }
}