package edu.connexion3a36.tests.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for setting up in-memory H2 database for unit testing.
 * This allows tests to run without requiring a real MySQL database.
 */
public class TestDBUtil {
    
    private static Connection connection;
    
    /**
     * Get or create an in-memory H2 database connection
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        }
        return connection;
    }
    
    /**
     * Initialize the database schema (tables)
     */
    public static void initializeSchema() throws SQLException {
        Connection cnx = getConnection();
        
        try (Statement stmt = cnx.createStatement()) {
            // Create medecin table
            stmt.execute("CREATE TABLE IF NOT EXISTS medecin (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nom VARCHAR(255), " +
                    "prenom VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "telephone VARCHAR(255), " +
                    "disponibilite INT DEFAULT 0" +
                    ")");
            
            // Create utilisateur (user) table
            stmt.execute("CREATE TABLE IF NOT EXISTS utilisateur (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nom VARCHAR(255), " +
                    "prenom VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "mot_de_passe VARCHAR(255), " +
                    "role VARCHAR(50)" +
                    ")");
            
            // Create stress_survey table
            stmt.execute("CREATE TABLE IF NOT EXISTS stress_survey (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "date DATE, " +
                    "sleep_hours INT, " +
                    "study_hours INT, " +
                    "user_id INT" +
                    ")");
            
            // Create well_being_score table (with unique constraint on survey_id)
            stmt.execute("CREATE TABLE IF NOT EXISTS well_being_score (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "survey_id INT UNIQUE, " +
                    "recommendation VARCHAR(500), " +
                    "action_plan VARCHAR(500), " +
                    "comment VARCHAR(500), " +
                    "score INT" +
                    ")");
            
            // Create consultation table
            stmt.execute("CREATE TABLE IF NOT EXISTS consultation (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "date_de_consultation TIMESTAMP, " +
                    "motif VARCHAR(500), " +
                    "genre VARCHAR(50), " +
                    "niveau VARCHAR(50), " +
                    "medecin_id INT, " +
                    "stress_survey_id INT" +
                    ")");
            
            System.out.println("H2 Database schema initialized successfully!");
        }
    }
    
    /**
     * Clear all data from tables (for test isolation)
     */
    public static void clearTables() throws SQLException {
        Connection cnx = getConnection();
        
        try (Statement stmt = cnx.createStatement()) {
            stmt.execute("DELETE FROM consultation");
            stmt.execute("DELETE FROM well_being_score");
            stmt.execute("DELETE FROM stress_survey");
            stmt.execute("DELETE FROM medecin");
            stmt.execute("DELETE FROM utilisateur");
        }
    }
    
    /**
     * Close the database connection
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Reset the database completely (clear and reinitialize schema)
     */
    public static void resetDatabase() throws SQLException {
        clearTables();
    }
}