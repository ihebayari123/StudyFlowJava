package edu.connexion3a36.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Medecin entity class
 */
public class MedecinTest {

    @Test
    public void testDefaultConstructor() {
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
    public void testParameterizedConstructor() {
        Medecin m = new Medecin("Dupont", "Jean", "jean.dupont@email.com", "0123456789", "disponible");

        assertNotNull(m);
        assertEquals("Dupont", m.getNom());
        assertEquals("Jean", m.getPrenom());
        assertEquals("jean.dupont@email.com", m.getEmail());
        assertEquals("0123456789", m.getTelephone());
        assertEquals("disponible", m.getDisponibilite());
        assertEquals(0, m.getId()); // ID should be 0 for new entity
    }

    @Test
    public void testSettersAndGetters() {
        Medecin m = new Medecin();
        
        m.setId(10);
        m.setNom("Martin");
        m.setPrenom("Pierre");
        m.setEmail("pierre.martin@email.com");
        m.setTelephone("0987654321");
        m.setDisponibilite("indisponible");

        assertEquals(10, m.getId());
        assertEquals("Martin", m.getNom());
        assertEquals("Pierre", m.getPrenom());
        assertEquals("pierre.martin@email.com", m.getEmail());
        assertEquals("0987654321", m.getTelephone());
        assertEquals("indisponible", m.getDisponibilite());
    }

    @Test
    public void testToString() {
        Medecin m = new Medecin();
        m.setId(5);
        m.setNom("Bernard");
        m.setPrenom("Alice");

        String result = m.toString();
        
        assertTrue(result.contains("5"));
        assertTrue(result.contains("Bernard"));
        assertTrue(result.contains("Alice"));
    }

    @Test
    public void testDisponibiliteValues() {
        Medecin m = new Medecin();
        
        // Test "disponible" value
        m.setDisponibilite("disponible");
        assertEquals("disponible", m.getDisponibilite());
        
        // Test "indisponible" value
        m.setDisponibilite("indisponible");
        assertEquals("indisponible", m.getDisponibilite());
        
        // Test case variations
        m.setDisponibilite("DISPONIBLE");
        assertEquals("DISPONIBLE", m.getDisponibilite());
        
        // Test null value
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
        
        m.setEmail("test@domain.co.uk");
        assertEquals("test@domain.co.uk", m.getEmail());
        
        // Test null email
        m.setEmail(null);
        assertNull(m.getEmail());
    }

    @Test
    public void testTelephoneFormat() {
        Medecin m = new Medecin();
        
        // Test various telephone formats
        m.setTelephone("0123456789");
        assertEquals("0123456789", m.getTelephone());
        
        m.setTelephone("+33 1 23 45 67 89");
        assertEquals("+33 1 23 45 67 89", m.getTelephone());
        
        m.setTelephone("06 12 34 56 78");
        assertEquals("06 12 34 56 78", m.getTelephone());
        
        // Test null telephone
        m.setTelephone(null);
        assertNull(m.getTelephone());
    }

    @Test
    public void testNomAndPrenom() {
        Medecin m = new Medecin();
        
        // Test various name formats
        m.setNom("De La Maison");
        m.setPrenom("Jean-Pierre");
        
        assertEquals("De La Maison", m.getNom());
        assertEquals("Jean-Pierre", m.getPrenom());
        
        // Test special characters
        m.setNom("O'Brien");
        m.setPrenom("D'Angelo");
        
        assertEquals("O'Brien", m.getNom());
        assertEquals("D'Angelo", m.getPrenom());
    }

    @Test
    public void testFullName() {
        Medecin m = new Medecin("Smith", "John", "john.smith@email.com", "1234567890", "disponible");
        
        assertEquals("Smith", m.getNom());
        assertEquals("John", m.getPrenom());
        
        // Full name is represented in toString
        String result = m.toString();
        assertTrue(result.contains("Smith"));
        assertTrue(result.contains("John"));
    }
}