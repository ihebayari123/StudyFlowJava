package edu.connexion3a36.services;

import edu.connexion3a36.entities.Consultation;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class ConsultationServiceTest {

    private ConsultationService service;
    private Connection testCnx;

    @BeforeEach
    public void setUp() throws Exception {
        // Use H2 in-memory database for testing with unique database name
        testCnx = DriverManager.getConnection("jdbc:h2:mem:testdb_consultation;DB_CLOSE_DELAY=-1", "sa", "");
        
        // Initialize schema
        initializeSchema();
        
        // Pass the test connection to the service so it uses the same database
        service = new ConsultationService(testCnx);
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = testCnx.createStatement()) {
            // Drop tables if exist to ensure clean state
            stmt.execute("DROP TABLE IF EXISTS consultation");
            stmt.execute("DROP TABLE IF EXISTS stress_survey");
            stmt.execute("DROP TABLE IF EXISTS utilisateur");
            stmt.execute("DROP TABLE IF EXISTS medecin");
            
            // Create medecin table first (foreign key)
            stmt.execute("CREATE TABLE medecin (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nom VARCHAR(255), " +
                    "prenom VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "telephone VARCHAR(255), " +
                    "disponibilite INT DEFAULT 0" +
                    ")");
            
            // Create utilisateur table (for stress_survey)
            stmt.execute("CREATE TABLE utilisateur (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nom VARCHAR(255), " +
                    "prenom VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "mot_de_passe VARCHAR(255), " +
                    "role VARCHAR(50)" +
                    ")");
            
            // Create stress_survey table (foreign key)
            stmt.execute("CREATE TABLE stress_survey (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "date DATE, " +
                    "sleep_hours INT, " +
                    "study_hours INT, " +
                    "user_id INT" +
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
            stmt.execute("INSERT INTO medecin (nom, prenom, email, telephone, disponibilite) VALUES ('Dupont', 'Jean', 'jean@email.com', '0123456789', 1)");
            stmt.execute("INSERT INTO utilisateur (nom, prenom, email, role) VALUES ('Test', 'User', 'test@test.com', 'student')");
            stmt.execute("INSERT INTO stress_survey (date, sleep_hours, study_hours, user_id) VALUES ('2024-01-01', 7, 6, 1)");
            stmt.execute("INSERT INTO stress_survey (date, sleep_hours, study_hours, user_id) VALUES ('2024-01-02', 6, 8, 1)");
            stmt.execute("INSERT INTO consultation (date_de_consultation, motif, genre, niveau, medecin_id, stress_survey_id) " +
                    "VALUES ('2024-01-15 10:00:00', 'Stress management', 'Feminin', 'Universite', 1, 1)");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (testCnx != null && !testCnx.isClosed()) {
            try (Statement stmt = testCnx.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS consultation");
                stmt.execute("DROP TABLE IF EXISTS stress_survey");
                stmt.execute("DROP TABLE IF EXISTS utilisateur");
                stmt.execute("DROP TABLE IF EXISTS medecin");
            }
            testCnx.close();
        }
    }

    @Test
    public void testGetDataEmpty() throws SQLException {
        // Clear consultation table
        try (Statement stmt = testCnx.createStatement()) {
            stmt.execute("DELETE FROM consultation");
        }
        
        List<Consultation> list = service.getData();
        assertNotNull(list);
        assertTrue(list.isEmpty(), "List should be empty after DELETE");
    }

    @Test
    public void testGetDataWithData() throws SQLException {
        List<Consultation> list = service.getData();
        assertNotNull(list);
        // Initial data has 1 record from setup - use >= for test isolation
        assertTrue(list.size() >= 1, "Should have at least 1 consultation record");
        
        // Verify at least one consultation exists
        Consultation first = list.get(0);
        assertNotNull(first.getMotif());
    }

    @Test
    public void testGetDataMultipleRecords() throws SQLException {
        // Insert another consultation
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO consultation (date_de_consultation, motif, genre, niveau, medecin_id, stress_survey_id) VALUES (?, ?, ?, ?, ?, ?)")) {
            pst.setTimestamp(1, Timestamp.valueOf("2024-02-01 14:00:00"));
            pst.setString(2, "Anxiety consultation");
            pst.setString(3, "Masculin");
            pst.setString(4, "Lycee");
            pst.setInt(5, 1);
            pst.setInt(6, 2);
            pst.executeUpdate();
        }
        
        List<Consultation> list = service.getData();
        assertNotNull(list);
        assertTrue(list.size() >= 2, "Should have at least 2 consultation records after insert");
    }

    @Test
    public void testConsultationEntityProperties() {
        Consultation c = new Consultation();
        
        // Test setters
        c.setId(1);
        c.setMotif("Test motif");
        c.setGenre("Feminin");
        c.setNiveau("Universite");
        c.setMedecin_id(1);
        c.setStress_survey_id(1);
        
        // Test getters
        assertEquals(1, c.getId());
        assertEquals("Test motif", c.getMotif());
        assertEquals("Feminin", c.getGenre());
        assertEquals("Universite", c.getNiveau());
        assertEquals(1, c.getMedecin_id());
        assertEquals(1, c.getStress_survey_id());
    }

    @Test
    public void testConsultationEntityDefaultConstructor() {
        Consultation c = new Consultation();
        
        assertNotNull(c);
        assertEquals(0, c.getId());
        assertNull(c.getDate_de_consultation());
        assertNull(c.getMotif());
        assertNull(c.getGenre());
        assertNull(c.getNiveau());
        assertEquals(0, c.getMedecin_id());
        assertEquals(0, c.getStress_survey_id());
    }

    @Test
    public void testConsultationEntityParameterizedConstructor() {
        Timestamp testDate = Timestamp.valueOf("2024-06-15 10:30:00");
        Consultation c = new Consultation(testDate, "Test Motif", "Masculin", "College", 2, 3);
        
        assertEquals(testDate, c.getDate_de_consultation());
        assertEquals("Test Motif", c.getMotif());
        assertEquals("Masculin", c.getGenre());
        assertEquals("College", c.getNiveau());
        assertEquals(2, c.getMedecin_id());
        assertEquals(3, c.getStress_survey_id());
    }

    @Test
    public void testConsultationEntityToString() {
        Consultation c = new Consultation();
        c.setId(10);
        c.setMotif("Depression consultation");
        
        String result = c.toString();;
        assertTrue(result.contains("10"));
        assertTrue(result.contains("Depression consultation"));
    }

    @Test
    public void testGenreValues() {
        Consultation c = new Consultation();
        
        // Test various genre values
        c.setGenre("Feminin");
        assertEquals("Feminin", c.getGenre());
        
        c.setGenre("Masculin");
        assertEquals("Masculin", c.getGenre());
        
        c.setGenre("Autre");
        assertEquals("Autre", c.getGenre());
        
        c.setGenre(null);
        assertNull(c.getGenre());
    }

    @Test
    public void testNiveauValues() {
        Consultation c = new Consultation();
        
        // Test various niveau values
        c.setNiveau("Universite");
        assertEquals("Universite", c.getNiveau());
        
        c.setNiveau("Lycee");
        assertEquals("Lycee", c.getNiveau());
        
        c.setNiveau("College");
        assertEquals("College", c.getNiveau());
        
        c.setNiveau("Ecole primaire");
        assertEquals("Ecole primaire", c.getNiveau());
        
        c.setNiveau(null);
        assertNull(c.getNiveau());
    }

    @Test
    public void testMotifValues() {
        Consultation c = new Consultation();
        
        // Test various motif values
        c.setMotif("Stress");
        assertEquals("Stress", c.getMotif());
        
        c.setMotif("Anxiety and panic attacks");
        assertEquals("Anxiety and panic attacks", c.getMotif());
        
        c.setMotif("");
        assertEquals("", c.getMotif());
        
        c.setMotif(null);
        assertNull(c.getMotif());
    }

    @Test
    public void testDeleteConsultation() throws SQLException {
        // Insert a new consultation to delete
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO consultation (date_de_consultation, motif, genre, niveau, medecin_id, stress_survey_id) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setTimestamp(1, Timestamp.valueOf("2024-03-01 10:00:00"));
            pst.setString(2, "To delete");
            pst.setString(3, "Feminin");
            pst.setString(4, "Universite");
            pst.setInt(5, 1);
            pst.setInt(6, 1);
            pst.executeUpdate();
        }
        
        // Get current count
        List<Consultation> before = service.getData();
        int countBefore = before.size();
        
        // Delete the last one
        if (countBefore > 0) {
            Consultation toDelete = before.get(before.size() - 1);
            service.deleteEntity(toDelete);
        }
        
        List<Consultation> after = service.getData();
        assertEquals(countBefore - 1, after.size());
    }

    @Test
    public void testTimestampWithDifferentTimes() {
        Consultation c = new Consultation();
        
        // Test morning timestamp
        Timestamp morning = Timestamp.valueOf("2024-03-15 09:00:00");
        c.setDate_de_consultation(morning);
        assertEquals(morning, c.getDate_de_consultation());
        
        // Test afternoon timestamp
        Timestamp afternoon = Timestamp.valueOf("2024-03-15 15:30:00");
        c.setDate_de_consultation(afternoon);
        assertEquals(afternoon, c.getDate_de_consultation());
        
        // Test null timestamp
        c.setDate_de_consultation(null);
        assertNull(c.getDate_de_consultation());
    }

    @Test
    public void testFullConsultationCreation() {
        Timestamp now = new Timestamp(System.currentTimeMillis());;
        Consultation c = new Consultation(
                now,
                "General wellness checkup",
                "Feminin",
                "Universite",
                1,
                1
        );
        
        assertNotNull(c.getDate_de_consultation());
        assertEquals("General wellness checkup", c.getMotif());
        assertEquals("Feminin", c.getGenre());
        assertEquals("Universite", c.getNiveau());
        assertEquals(1, c.getMedecin_id());
        assertEquals(1, c.getStress_survey_id());
    }
}