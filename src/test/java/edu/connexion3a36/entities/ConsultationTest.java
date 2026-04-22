package edu.connexion3a36.entities;

import org.junit.jupiter.api.Test;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Consultation entity class
 */
public class ConsultationTest {

    @Test
    public void testDefaultConstructor() {
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
    public void testParameterizedConstructor() {
        Timestamp testDate = Timestamp.valueOf("2024-01-15 10:30:00");
        Consultation c = new Consultation(
                testDate, 
                "Stress management", 
                "Feminin", 
                "Universite",
                1, 
                5
        );

        assertNotNull(c);
        assertEquals(testDate, c.getDate_de_consultation());
        assertEquals("Stress management", c.getMotif());
        assertEquals("Feminin", c.getGenre());
        assertEquals("Universite", c.getNiveau());
        assertEquals(1, c.getMedecin_id());
        assertEquals(5, c.getStress_survey_id());
        assertEquals(0, c.getId()); // ID should be 0 for new entity
    }

    @Test
    public void testSettersAndGetters() {
        Consultation c = new Consultation();
        Timestamp testDate = Timestamp.valueOf("2024-02-20 14:45:00");
        
        c.setId(10);
        c.setDate_de_consultation(testDate);
        c.setMotif("Anxiety");
        c.setGenre("Masculin");
        c.setNiveau("Lycee");
        c.setMedecin_id(2);
        c.setStress_survey_id(8);

        assertEquals(10, c.getId());
        assertEquals(testDate, c.getDate_de_consultation());
        assertEquals("Anxiety", c.getMotif());
        assertEquals("Masculin", c.getGenre());
        assertEquals("Lycee", c.getNiveau());
        assertEquals(2, c.getMedecin_id());
        assertEquals(8, c.getStress_survey_id());
    }

    @Test
    public void testToString() {
        Consultation c = new Consultation();
        c.setId(15);
        c.setMotif("Depression consultation");

        String result = c.toString();
        
        assertTrue(result.contains("15"));
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
        
        // Test null
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
        
        // Test null
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
        
        c.setMotif("Sleep problems");
        assertEquals("Sleep problems", c.getMotif());
        
        c.setMotif("Academic pressure");
        assertEquals("Academic pressure", c.getMotif());
        
        // Test empty string
        c.setMotif("");
        assertEquals("", c.getMotif());
        
        // Test null
        c.setMotif(null);
        assertNull(c.getMotif());
    }

    @Test
    public void testForeignKeyIds() {
        Consultation c = new Consultation();
        
        // Test medecin_id
        c.setMedecin_id(1);
        assertEquals(1, c.getMedecin_id());
        
        c.setMedecin_id(100);
        assertEquals(100, c.getMedecin_id());
        
        // Default should be 0
        assertEquals(0, c.getMedecin_id());
        
        // Test stress_survey_id
        c.setStress_survey_id(5);
        assertEquals(5, c.getStress_survey_id());
        
        c.setStress_survey_id(50);
        assertEquals(50, c.getStress_survey_id());
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
        
        // Test evening timestamp
        Timestamp evening = Timestamp.valueOf("2024-03-15 20:00:00");
        c.setDate_de_consultation(evening);
        assertEquals(evening, c.getDate_de_consultation());
        
        // Test null timestamp
        c.setDate_de_consultation(null);
        assertNull(c.getDate_de_consultation());
    }

    @Test
    public void testFullConsultationCreation() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
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