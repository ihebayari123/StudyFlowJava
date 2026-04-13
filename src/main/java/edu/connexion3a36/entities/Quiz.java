package edu.connexion3a36.entities;

import java.time.LocalDateTime;

public class Quiz {

    private int           id;
    private String        titre;
    private int           duree;
    private LocalDateTime dateCreation;
    private int           courseId;

    public Quiz() { this.dateCreation = LocalDateTime.now(); }

    public Quiz(String titre, int duree, int courseId) {
        this.titre        = titre;
        this.duree        = duree;
        this.courseId     = courseId;
        this.dateCreation = LocalDateTime.now();
    }

    public Quiz(int id, String titre, int duree, LocalDateTime dateCreation, int courseId) {
        this.id           = id;
        this.titre        = titre;
        this.duree        = duree;
        this.dateCreation = dateCreation;
        this.courseId     = courseId;
    }

    public int           getId()                          { return id; }
    public void          setId(int id)                    { this.id = id; }
    public String        getTitre()                       { return titre; }
    public void          setTitre(String t)               { this.titre = t; }
    public int           getDuree()                       { return duree; }
    public void          setDuree(int d)                  { this.duree = d; }
    public LocalDateTime getDateCreation()                { return dateCreation; }
    public void          setDateCreation(LocalDateTime d) { this.dateCreation = d; }
    public int           getCourseId()                    { return courseId; }
    public void          setCourseId(int c)               { this.courseId = c; }

    @Override
    public String toString() { return titre + " (" + duree + " min)"; }
}

