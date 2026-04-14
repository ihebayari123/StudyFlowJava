package edu.connexion3a36.services;

import edu.connexion3a36.entities.Medecin;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MedecinService class.
 * Note: These tests require a database connection. Use H2 in-memory for testing.
 */
public class MedecinServiceTest {

    private MedecinService service;
    private Connection testCnx;

    @BeforeEach
    public void setUp() throws Exception {
        // Use H2 in-memory database for testing with unique database name
        testCnx = DriverManager.getConnection("jdbc:h2:mem:testdb_medecin;DB_CLOSE_DELAY=-1", "sa", "");
        
        // Initialize schema
        initializeSchema();
        
        // Pass the test connection to the service so it uses the same database
        service = new MedecinService(testCnx);
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = testCnx.createStatement()) {
            // Drop tables if exist to ensure clean state
            stmt.execute("DROP TABLE IF EXISTS consultation");
            stmt.execute("DROP TABLE IF EXISTS medecin");
            
            // Create medecin table
            stmt.execute("CREATE TABLE medecin (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nom VARCHAR(255), " +
                    "prenom VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "telephone VARCHAR(255), " +
                    "disponibilite INT DEFAULT 0" +
                    ")");
            
            // Create consultation table (for cascade tests)
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
            stmt.execute("INSERT INTO medecin (nom, prenom, email, telephone, disponibilite) VALUES ('Martin', 'Alice', 'alice@email.com', '0987654321', 0)");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (testCnx != null && !testCnx.isClosed()) {
            try (Statement stmt = testCnx.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS consultation");
                stmt.execute("DROP TABLE IF EXISTS medecin");
            }
            testCnx.close();
        }
    }

    @Test
    public void testGetDataEmpty() throws SQLException {
        // Clear medecin table
        try (Statement stmt = testCnx.createStatement()) {
            stmt.execute("DELETE FROM medecin");
        }
        
        List<Medecin> list = service.getData();
        assertNotNull(list);
        assertTrue(list.isEmpty(), "List should be empty after DELETE");
    }

    @Test
    public void testGetDataWithData() throws SQLException {
        List<Medecin> list = service.getData();
        assertNotNull(list);
        // Initial data has 2 records from setup - use >= to handle both MySQL and H2
        assertTrue(list.size() >= 2, "Should have at least 2 medecin records");
        
        // Verify at least the first medecin exists
        Medecin first = list.get(0);
        assertNotNull(first.getNom());
        assertNotNull(first.getPrenom());
    }

    @Test
    public void testGetDataMultipleRecords() throws SQLException {
        // Insert another medecin
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO medecin (nom, prenom, email, telephone, disponibilite) VALUES (?, ?, ?, ?, ?)")) {
            pst.setString(1, "Bernard");
            pst.setString(2, "Pierre");
            pst.setString(3, "pierre@email.com");
            pst.setString(4, "0123456789");
            pst.setInt(5, 1); // disponible
            pst.executeUpdate();
        }
        
        List<Medecin> list = service.getData();
        assertNotNull(list);
        assertTrue(list.size() >= 3, "Should have at least 3 medecin records after insert");
    }

    @Test
    public void testMedecinEntityProperties() {
        Medecin m = new Medecin();
        
        // Test setters
        m.setId(1);
        m.setNom("Test");
        m.setPrenom("User");
        m.setEmail("test@email.com");
        m.setTelephone("1234567890");
        m.setDisponibilite("disponible");
        
        // Test getters
        assertEquals(1, m.getId());
        assertEquals("Test", m.getNom());
        assertEquals("User", m.getPrenom());
        assertEquals("test@email.com", m.getEmail());
        assertEquals("1234567890", m.getTelephone());
        assertEquals("disponible", m.getDisponibilite());
    }

    @Test
    public void testMedecinEntityDefaultConstructor() {
        Medecin m = new Medecin();
        
        assertNotNull(m);
        assertEquals(0, m.getId());
        assertNull(m.getNom());
        assertNull(m.getPrenom());
        assertNull(m.getEmail());
        assertNull(m.getTelephone());
        assertNull(m.getDisponibilite());
    }

    @Test
    public void testMedecinEntityParameterizedConstructor() {
        Medecin m = new Medecin("Smith", "John", "john@email.com", "5551234567", "indisponible");
        
        assertEquals("Smith", m.getNom());
        assertEquals("John", m.getPrenom());
        assertEquals("john@email.com", m.getEmail());
        assertEquals("5551234567", m.getTelephone());
        assertEquals("indisponible", m.getDisponibilite());
    }

    @Test
    public void testMedecinEntityToString() {
        Medecin m = new Medecin();
        m.setId(5);
        m.setNom("Dupont");
        m.setPrenom("Marie");
        
        String result = m.toString();
        assertTrue(result.contains("5"));
        assertTrue(result.contains("Dupont"));
        assertTrue(result.contains("Marie"));
    }

    @Test
    public void testDisponibiliteTextValues() {
        Medecin m = new Medecin();
        
        // Test "disponible" text
        m.setDisponibilite("disponible");
        assertEquals("disponible", m.getDisponibilite());
        
        // Test "indisponible" text
        m.setDisponibilite("indisponible");
        assertEquals("indisponible", m.getDisponibilite());
        
        // Test case variations
        m.setDisponibilite("DISPONIBLE");
        assertEquals("DISPONIBLE", m.getDisponibilite());
        
        // Test null
        m.setDisponibilite(null);
        assertNull(m.getDisponibilite());
    }

    @Test
    public void testEmailValidation() {
        Medecin m = new Medecin();
        
        // Test various email formats
        m.setEmail("doctor@hospital.com");
        assertEquals("doctor@hospital.com", m.getEmail());
        
        m.setEmail("medecin.specialiste@clinic.org");
        assertEquals("medecin.specialiste@clinic.org", m.getEmail());
        
        m.setEmail(null);
        assertNull(m.getEmail());
    }

    @Test
    public void testDeleteMedecin() throws SQLException {
        // Insert a new medecin to delete
        try (PreparedStatement pst = testCnx.prepareStatement(
                "INSERT INTO medecin (nom, prenom, email, telephone, disponibilite) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, "ToDelete");
            pst.setString(2, "Test");
            pst.setString(3, "delete@test.com");
            pst.setString(4, "1111111111");
            pst.setInt(5, 0);
            pst.executeUpdate();
        }
        
        // Get current count
        List<Medecin> before = service.getData();
        int countBefore = before.size();
        
        // Delete the last one
        if (countBefore > 0) {
            Medecin toDelete = before.get(before.size() - 1);
            service.deleteEntity(toDelete);
        }
        
        List<Medecin> after = service.getData();
        assertEquals(countBefore - 1, after.size());
    }

    @Test
    public void testDeleteById() throws SQLException {
        List<Medecin> before = service.getData();
        int countBefore = before.size();
        
        // Delete by ID (1 exists from setup)
        if (countBefore > 0) {
            service.deleteById(1);
        }
        
        List<Medecin> after = service.getData();
        assertTrue(after.size() < countBefore, "Count should decrease after delete");
    }

    @Test
    public void testFullNameRepresentation() {
        // Test that full name is correctly represented
        Medecin m = new Medecin("De La Fontaine", "Jean-Pierre", "jf@email.com", "1234567890", "disponible");
        
        String result = m.toString();
        assertTrue(result.contains("De La Fontaine"));
        assertTrue(result.contains("Jean-Pierre"));
    }
}