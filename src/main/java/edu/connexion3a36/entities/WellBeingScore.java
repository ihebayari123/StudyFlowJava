package edu.connexion3a36.entities;

public class WellBeingScore {
    private int id;
    private int survey_id;
    private String recommendation;
    private String action_plan;
    private String comment;
    private int score;

    public WellBeingScore() {}

    public WellBeingScore(String recommendation, String action_plan, String comment, int score) {
        this.recommendation = recommendation;
        this.action_plan = action_plan;
        this.comment = comment;
        this.score = score;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSurvey_id() { return survey_id; }
    public void setSurvey_id(int survey_id) { this.survey_id = survey_id; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getAction_plan() { return action_plan; }
    public void setAction_plan(String action_plan) { this.action_plan = action_plan; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    @Override
    public String toString() {
        return "WellBeingScore{id=" + id + ", survey_id=" + survey_id + ", score=" + score + "}";
    }
}
