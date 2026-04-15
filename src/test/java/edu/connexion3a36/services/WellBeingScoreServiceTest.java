package edu.connexion3a36.services;

import edu.connexion3a36.entities.WellBeingScore;
import edu.connexion3a36.tools.MyConnection;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the WellBeingScoreService class.
 * Note: These tests require a database connection. Use H2 in-memory for testing.
 */
public class WellBeingScoreServiceTest {

    private WellBeingScoreService service;
    private Connection testCnx;

    @BeforeEach
    public void setUp() throws Exception {
        // Use H2 in-memory database for testing with unique database name
        testCnx = DriverManager.getConnection("jdbc:h2:mem:testdb_wbs;DB_CLOSE_DELAY=-1", "sa", "");
        
        // Initialize schema
        initializeSchema();
        
        // Pass the test connection to the service so it uses the same database
        service = new WellBeingScoreService(testCnx);
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = testCnx.createStatement()) {
            // Drop tables if exist to ensure clean state
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
                stmt.execute("DROP TABLE IF EXISTS well_being_score");
                stmt.execute("DROP TABLE IF EXISTS stress_survey");
                stmt.execute("DROP TABLE IF EXISTS utilisateur");
            }
            testCnx.close();
        }
    }

    @Test
    public void testGetDataEmpty() throws SQLException {
        // Clear well_being_score table
        try (Statement stmt = testCnx.createStatement()) {
            stmt.execute("DELETE FROM well_being_score");
        }
        
        List<WellBeingScore> list = service.getData();
        assertNotNull(list);
        assertTrue(list.isEmpty(), "List should be empty after DELETE");
    }

    @Test
    public void testGetDataWithData() throws SQLException {
        // Insert a well being score directly
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO well_being_score (survey_id, recommendation, action_plan, comment, score) VALUES (?, ?, ?, ?, ?)")) {
            pst.setInt(1, 1);
            pst.setString(2, "Take more breaks");
            pst.setString(3, "Schedule breaks every hour");
            pst.setString(4, "Good progress");
            pst.setInt(5, 75);
            pst.executeUpdate();
        }
        
        List<WellBeingScore> list = service.getData();
        assertNotNull(list);
        assertTrue(list.size() >= 1, "Should have at least 1 WellBeingScore record");
        
        // Verify data was retrieved correctly
        WellBeingScore wbs = list.get(0);
        assertNotNull(wbs.getRecommendation());
    }

    @Test
    public void testGetDataMultipleRecords() throws SQLException {
        // Insert multiple well being scores
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO well_being_score (survey_id, recommendation, action_plan, comment, score) VALUES (?, ?, ?, ?, ?)")) {
            // First record
            pst.setInt(1, 1);
            pst.setString(2, "Recommendation 1");
            pst.setString(3, "Action 1");
            pst.setString(4, "Comment 1");
            pst.setInt(5, 60);
            pst.executeUpdate();
            
            // Second record
            pst.setInt(1, 2);
            pst.setString(2, "Recommendation 2");
            pst.setString(3, "Action 2");
            pst.setString(4, "Comment 2");
            pst.setInt(5, 80);
            pst.executeUpdate();
        }
        
        List<WellBeingScore> list = service.getData();
        assertNotNull(list);
        assertEquals(2, list.size());
    }

    @Test
    public void testWellBeingScoreEntityProperties() {
        WellBeingScore wbs = new WellBeingScore();
        
        // Test setters
        wbs.setId(1);
        wbs.setSurvey_id(5);
        wbs.setRecommendation("Test recommendation");
        wbs.setAction_plan("Test action plan");
        wbs.setComment("Test comment");
        wbs.setScore(85);
        
        // Test getters
        assertEquals(1, wbs.getId());
        assertEquals(5, wbs.getSurvey_id());
        assertEquals("Test recommendation", wbs.getRecommendation());
        assertEquals("Test action plan", wbs.getAction_plan());
        assertEquals("Test comment", wbs.getComment());
        assertEquals(85, wbs.getScore());
    }

    @Test
    public void testWellBeingScoreEntityToString() {
        WellBeingScore wbs = new WellBeingScore("Rec", "Action", "Comment", 50);
        wbs.setId(10);
        wbs.setSurvey_id(5);
        
        String result = wbs.toString();
        assertTrue(result.contains("id=10"));
        assertTrue(result.contains("survey_id=5"));
        assertTrue(result.contains("score=50"));
    }

    @Test
    public void testWellBeingScoreEntityDefaultConstructor() {
        WellBeingScore wbs = new WellBeingScore();
        
        assertNotNull(wbs);
        assertEquals(0, wbs.getId());
        assertEquals(0, wbs.getSurvey_id());
        assertNull(wbs.getRecommendation());
        assertNull(wbs.getAction_plan());
        assertNull(wbs.getComment());
        assertEquals(0, wbs.getScore());
    }

    @Test
    public void testWellBeingScoreEntityParameterizedConstructor() {
        WellBeingScore wbs = new WellBeingScore("Test Rec", "Test Action", "Test Comment", 90);
        
        assertEquals("Test Rec", wbs.getRecommendation());
        assertEquals("Test Action", wbs.getAction_plan());
        assertEquals("Test Comment", wbs.getComment());
        assertEquals(90, wbs.getScore());
    }

    @Test
    public void testScoreBoundaryValues() {
        WellBeingScore wbs = new WellBeingScore();
        
        // Test minimum
        wbs.setScore(0);
        assertEquals(0, wbs.getScore());
        
        // Test maximum
        wbs.setScore(100);
        assertEquals(100, wbs.getScore());
        
        // Test negative
        wbs.setScore(-5);
        assertEquals(-5, wbs.getScore());
    }
}