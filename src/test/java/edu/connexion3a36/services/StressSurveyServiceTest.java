package edu.connexion3a36.services;

import edu.connexion3a36.entities.StressSurvey;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the StressSurveyService class.
 * Note: These tests require a database connection. Use H2 in-memory for testing.
 */
public class StressSurveyServiceTest {

    private StressSurveyService service;
    private Connection testCnx;

    @BeforeEach
    public void setUp() throws Exception {
        // Use H2 in-memory database for testing with unique database name
        testCnx = DriverManager.getConnection("jdbc:h2:mem:testdb_stress;DB_CLOSE_DELAY=-1", "sa", "");
        
        // Initialize schema
        initializeSchema();
        
        // Pass the test connection to the service so it uses the same database
        service = new StressSurveyService(testCnx);
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = testCnx.createStatement()) {
            // Drop tables if exist to ensure clean state
            stmt.execute("DROP TABLE IF EXISTS consultation");
            stmt.execute("DROP TABLE IF EXISTS well_being_score");
            stmt.execute("DROP TABLE IF EXISTS stress_survey");
            stmt.execute("DROP TABLE IF EXISTS utilisateur");
            
            // Create utilisateur table first
            stmt.execute("CREATE TABLE utilisateur (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nom VARCHAR(255), " +
                    "prenom VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "mot_de_passe VARCHAR(255), " +
                    "role VARCHAR(50)" +
                    ")");
            
            // Create stress_survey table
            stmt.execute("CREATE TABLE stress_survey (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "date DATE, " +
                    "sleep_hours INT, " +
                    "study_hours INT, " +
                    "user_id INT" +
                    ")");
            
            // Create well_being_score table
            stmt.execute("CREATE TABLE well_being_score (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "survey_id INT UNIQUE, " +
                    "recommendation VARCHAR(500), " +
                    "action_plan VARCHAR(500), " +
                    "comment VARCHAR(500), " +
                    "score INT" +
                    ")");
            
            // Create consultation table
            stmt.execute("CREATE TABLE consultation (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "date_de_consultation TIMESTAMP, " +
                    "motif VARCHAR(500), " +
                    "genre VARCHAR(50), " +
                    "niveau VARCHAR(50), " +
                    "medecin_id INT, " +
                    "stress_survey_id INT" +
                    ")");
            
            // Insert test data (let AUTO_INCREMENT generate IDs)
            stmt.execute("INSERT INTO utilisateur (nom, prenom, email, role) VALUES ('Test', 'User', 'test@test.com', 'student')");
            stmt.execute("INSERT INTO stress_survey (date, sleep_hours, study_hours, user_id) VALUES ('2024-01-01', 7, 6, 1)");
            stmt.execute("INSERT INTO stress_survey (date, sleep_hours, study_hours, user_id) VALUES ('2024-01-02', 6, 8, 1)");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (testCnx != null && !testCnx.isClosed()) {
            try (Statement stmt = testCnx.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS consultation");
                stmt.execute("DROP TABLE IF EXISTS well_being_score");
                stmt.execute("DROP TABLE IF EXISTS stress_survey");
                stmt.execute("DROP TABLE IF EXISTS utilisateur");
            }
            testCnx.close();
        }
    }

    @Test
    public void testGetDataEmpty() throws SQLException {
        // Clear stress_survey table
        try (Statement stmt = testCnx.createStatement()) {
            stmt.execute("DELETE FROM stress_survey");
        }
        
        List<StressSurvey> list = service.getData();
        assertNotNull(list);
        assertTrue(list.isEmpty(), "List should be empty after DELETE");
    }

    @Test
    public void testGetDataWithData() throws SQLException {
        List<StressSurvey> list = service.getData();
        assertNotNull(list);
        // Initial data has 2 records from setup
        assertTrue(list.size() >= 2);
        
        StressSurvey first = list.get(0);
        assertEquals(1, first.getId());
        assertNotNull(first.getDate());
    }

    @Test
    public void testGetDataMultipleRecords() throws SQLException {
        // Insert another stress survey
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO stress_survey (date, sleep_hours, study_hours, user_id) VALUES (?, ?, ?, ?)")) {
            pst.setDate(1, Date.valueOf("2024-01-03"));
            pst.setInt(2, 8);
            pst.setInt(3, 4);
            pst.setInt(4, 1);
            pst.executeUpdate();
        }
        
        List<StressSurvey> list = service.getData();
        assertNotNull(list);
        assertTrue(list.size() >= 3);
    }

    @Test
    public void testStressSurveyEntityProperties() {
        StressSurvey ss = new StressSurvey();
        
        // Test setters
        ss.setId(1);
        ss.setSleep_hours(7);
        ss.setStudy_hours(8);
        ss.setUser_id(2);
        
        // Test getters
        assertEquals(1, ss.getId());
        assertEquals(2, ss.getUser_id());
    }

    @Test
    public void testStressSurveyEntityDefaultConstructor() {
        StressSurvey ss = new StressSurvey();
        
        assertNotNull(ss);
        assertEquals(0, ss.getId());
        assertNull(ss.getDate());
        assertEquals(0, ss.getSleep_hours());
        assertEquals(0, ss.getStudy_hours());
        assertEquals(0, ss.getUser_id());
    }

    @Test
    public void testStressSurveyEntityParameterizedConstructor() {
        Date testDate = Date.valueOf("2024-06-15");
        StressSurvey ss = new StressSurvey(testDate, 8, 6, 1);
        
        assertEquals(testDate, ss.getDate());
        assertEquals(8, ss.getSleep_hours());
        assertEquals(6, ss.getStudy_hours());
        assertEquals(1, ss.getUser_id());
    }

    @Test
    public void testStressSurveyEntityToString() {
        Date testDate = Date.valueOf("2024-06-15");
        StressSurvey ss = new StressSurvey();
        ss.setId(10);
        ss.setDate(testDate);
        
        String result = ss.toString();
        assertTrue(result.contains("10"));
        assertTrue(result.contains("2024-06-15"));
    }

    @Test
    public void testSleepHoursBoundaryValues() {
        StressSurvey ss = new StressSurvey();
        
        // Test minimum
        ss.setSleep_hours(0);
        assertEquals(0, ss.getSleep_hours());
        
        // Test normal
        ss.setSleep_hours(7);
        assertEquals(7, ss.getSleep_hours());
        
        // Test maximum
        ss.setSleep_hours(24);
        assertEquals(24, ss.getSleep_hours());
    }

    @Test
    public void testStudyHoursBoundaryValues() {
        StressSurvey ss = new StressSurvey();
        
        // Test no study
        ss.setStudy_hours(0);
        assertEquals(0, ss.getStudy_hours());
        
        // Test normal
        ss.setStudy_hours(6);
        assertEquals(6, ss.getStudy_hours());
        
        // Test high
        ss.setStudy_hours(16);
        assertEquals(16, ss.getStudy_hours());
    }

    @Test
    public void testDeleteById() throws SQLException {
        // Insert a new stress survey to delete
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO stress_survey (date, sleep_hours, study_hours, user_id) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setDate(1, Date.valueOf("2024-02-01"));
            pst.setInt(2, 7);
            pst.setInt(3, 5);
            pst.setInt(4, 1);
            pst.executeUpdate();
        }
        
        // Get current count
        List<StressSurvey> before = service.getData();
        int countBefore = before.size();
        
        // Delete the last one
        if (countBefore > 0) {
            StressSurvey toDelete = before.get(before.size() - 1);
            service.deleteEntity(toDelete);
        }
        
        List<StressSurvey> after = service.getData();
        assertEquals(countBefore - 1, after.size());
    }
}