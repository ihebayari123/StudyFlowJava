package edu.connexion3a36.entities;

import org.junit.jupiter.api.Test;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the StressSurvey entity class
 */
public class StressSurveyTest {

    @Test
    public void testDefaultConstructor() {
        StressSurvey ss = new StressSurvey();
        assertNotNull(ss);
        assertEquals(0, ss.getId());
        assertNull(ss.getDate());
        assertEquals(0, ss.getSleep_hours());
        assertEquals(0, ss.getStudy_hours());
        assertEquals(0, ss.getUser_id());
    }

    @Test
    public void testParameterizedConstructor() {
        Date testDate = Date.valueOf("2024-01-15");
        StressSurvey ss = new StressSurvey(testDate, 7, 8, 1);

        assertNotNull(ss);
        assertEquals(testDate, ss.getDate());
        assertEquals(7, ss.getSleep_hours());
        assertEquals(8, ss.getStudy_hours());
        assertEquals(1, ss.getUser_id());
        assertEquals(0, ss.getId()); // ID should be 0 for new entity
    }

    @Test
    public void testSettersAndGetters() {
        StressSurvey ss = new StressSurvey();
        Date testDate = Date.valueOf("2024-02-20");
        
        ss.setId(5);
        ss.setDate(testDate);
        ss.setSleep_hours(6);
        ss.setStudy_hours(10);
        ss.setUser_id(2);

        assertEquals(5, ss.getId());
        assertEquals(testDate, ss.getDate());
        assertEquals(6, ss.getSleep_hours());
        assertEquals(10, ss.getStudy_hours());
        assertEquals(2, ss.getUser_id());
    }

    @Test
    public void testToString() {
        Date testDate = Date.valueOf("2024-03-10");
        StressSurvey ss = new StressSurvey();
        ss.setId(15);
        ss.setDate(testDate);

        String result = ss.toString();
        
        assertTrue(result.contains("15"));
        assertTrue(result.contains("2024-03-10"));
    }

    @Test
    public void testSleepHoursBoundaryValues() {
        StressSurvey ss = new StressSurvey();
        
        // Test minimum sleep hours (0)
        ss.setSleep_hours(0);
        assertEquals(0, ss.getSleep_hours());
        
        // Test normal sleep hours
        ss.setSleep_hours(8);
        assertEquals(8, ss.getSleep_hours());
        
        // Test maximum reasonable sleep hours
        ss.setSleep_hours(24);
        assertEquals(24, ss.getSleep_hours());
    }

    @Test
    public void testStudyHoursBoundaryValues() {
        StressSurvey ss = new StressSurvey();
        
        // Test no study hours
        ss.setStudy_hours(0);
        assertEquals(0, ss.getStudy_hours());
        
        // Test normal study hours
        ss.setStudy_hours(6);
        assertEquals(6, ss.getStudy_hours());
        
        // Test high study hours
        ss.setStudy_hours(16);
        assertEquals(16, ss.getStudy_hours());
    }

    @Test
    public void testNullDate() {
        StressSurvey ss = new StressSurvey();
        
        // Date should allow null
        ss.setDate(null);
        
        assertNull(ss.getDate());
    }

    @Test
    public void testUserIdAssignment() {
        StressSurvey ss = new StressSurvey();
        
        // Test different user IDs
        ss.setUser_id(1);
        assertEquals(1, ss.getUser_id());
        
        ss.setUser_id(100);
        assertEquals(100, ss.getUser_id());
        
        // Test user ID 0 (default)
        ss.setUser_id(0);
        assertEquals(0, ss.getUser_id());
    }
}