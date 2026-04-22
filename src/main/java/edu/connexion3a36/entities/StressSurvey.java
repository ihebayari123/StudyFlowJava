package edu.connexion3a36.entities;

import java.sql.Date;

public class StressSurvey {
    private int id;
    private Date date;
    private int sleep_hours;
    private int study_hours;
    private int user_id;

    public StressSurvey() {}

    public StressSurvey(Date date, int sleep_hours, int study_hours, int user_id) {
        this.date = date;
        this.sleep_hours = sleep_hours;
        this.study_hours = study_hours;
        this.user_id = user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public int getSleep_hours() { return sleep_hours; }
    public void setSleep_hours(int sleep_hours) { this.sleep_hours = sleep_hours; }
    public int getStudy_hours() { return study_hours; }
    public void setStudy_hours(int study_hours) { this.study_hours = study_hours; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() { return "Survey #" + id + " (" + date + ")"; }
}
