package edu.connexion3a36.entities;

import java.sql.Timestamp;

public class Consultation {
    private int id;
    private Timestamp date_de_consultation;
    private String motif;
    private String genre;
    private String niveau;
    private int medecin_id;
    private int stress_survey_id;

    public Consultation() {}

    public Consultation(Timestamp date_de_consultation, String motif, String genre,
                        String niveau, int medecin_id, int stress_survey_id) {
        this.date_de_consultation = date_de_consultation;
        this.motif = motif;
        this.genre = genre;
        this.niveau = niveau;
        this.medecin_id = medecin_id;
        this.stress_survey_id = stress_survey_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Timestamp getDate_de_consultation() { return date_de_consultation; }
    public void setDate_de_consultation(Timestamp date_de_consultation) { this.date_de_consultation = date_de_consultation; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public int getMedecin_id() { return medecin_id; }
    public void setMedecin_id(int medecin_id) { this.medecin_id = medecin_id; }
    public int getStress_survey_id() { return stress_survey_id; }
    public void setStress_survey_id(int stress_survey_id) { this.stress_survey_id = stress_survey_id; }

    @Override
    public String toString() {
        return "Consultation #" + id + " - " + motif;
    }
}
