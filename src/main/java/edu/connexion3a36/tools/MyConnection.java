package edu.connexion3a36.tools;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;




public class MyConnection {
    private String url="jdbc:mysql://localhost:3307/studyflow";
    private String login="root";
    private String pwd="";
    private static Connection cnx;
    private static MyConnection instance;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(url, login, pwd);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static Connection getCnx() {
        return cnx;
    }
    public static MyConnection getInstance(){
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;

    }
}
