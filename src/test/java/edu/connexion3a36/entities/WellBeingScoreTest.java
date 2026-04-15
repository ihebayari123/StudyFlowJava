package edu.connexion3a36.entities;

import org.junit.jupiter.api.Test;
import java.sql.Date;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the WellBeingScore entity class
 */
public class WellBeingScoreTest {

    @Test
    public void testDefaultConstructor() {
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
    public void testParameterizedConstructor() {
        WellBeingScore wbs = new WellBeingScore(
                "Practice mindfulness",
                "Meditate 10 minutes daily",
                "Good progress",
                75
        );

        assertNotNull(wbs);
        assertEquals("Practice mindfulness", wbs.getRecommendation());
        assertEquals("Meditate 10 minutes daily", wbs.getAction_plan());
        assertEquals("Good progress", wbs.getComment());
        assertEquals(75, wbs.getScore());
        assertEquals(0, wbs.getId()); // ID should be 0 for new entity
    }

    @Test
    public void testSettersAndGetters() {
        WellBeingScore wbs = new WellBeingScore();
        
        wbs.setId(1);
        wbs.setSurvey_id(5);
        wbs.setRecommendation("Sleep more");
        wbs.setAction_plan("Go to bed earlier");
        wbs.setComment("Tired");
        wbs.setScore(50);

        assertEquals(1, wbs.getId());
        assertEquals(5, wbs.getSurvey_id());
        assertEquals("Sleep more", wbs.getRecommendation());
        assertEquals("Go to bed earlier", wbs.getAction_plan());
        assertEquals("Tired", wbs.getComment());
        assertEquals(50, wbs.getScore());
    }

    @Test
    public void testToString() {
        WellBeingScore wbs = new WellBeingScore();
        wbs.setId(10);
        wbs.setSurvey_id(5);
        wbs.setScore(80);

        String result = wbs.toString();
        
        assertTrue(result.contains("id=10"));
        assertTrue(result.contains("survey_id=5"));
        assertTrue(result.contains("score=80"));
    }

    @Test
    public void testScoreBoundaryValues() {
        WellBeingScore wbs = new WellBeingScore();
        
        // Test minimum score
        wbs.setScore(0);
        assertEquals(0, wbs.getScore());
        
        // Test maximum reasonable score
        wbs.setScore(100);
        assertEquals(100, wbs.getScore());
        
        // Test negative score (should be allowed based on entity design)
        wbs.setScore(-10);
        assertEquals(-10, wbs.getScore());
    }

    @Test
    public void testNullValues() {
        WellBeingScore wbs = new WellBeingScore();
        
        // All string fields should allow null
        wbs.setRecommendation(null);
        wbs.setAction_plan(null);
        wbs.setComment(null);
        
        assertNull(wbs.getRecommendation());
        assertNull(wbs.getAction_plan());
        assertNull(wbs.getComment());
    }
}