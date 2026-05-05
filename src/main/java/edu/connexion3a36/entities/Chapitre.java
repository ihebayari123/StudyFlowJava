package edu.connexion3a36.entities;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "chapitre")
public class Chapitre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @Column(nullable = false)
    private Integer ordre;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Cours course;

    @Column(length = 50)
    private String contentType;

    @Column(length = 500)
    private String videoUrl;

    @Column(length = 255)
    private String fileName;

    @Column(columnDefinition = "json")
    private List<String> links;

    @Column(length = 500)
    private String imageUrl;

    private Integer durationMinutes;

    // ── NEW: AI-assigned difficulty level ────────────────────────────────────
    @Column(length = 30)
    private String difficulty;   // "Débutant" | "Intermédiaire" | "Avancé"

    // Constructeurs
    public Chapitre() {}

    public Chapitre(String titre, String contenu, Integer ordre, Cours course) {
        this.titre   = titre;
        this.contenu = contenu;
        this.ordre   = ordre;
        this.course  = course;
    }

    // Getters et Setters
    public Long getId()            { return id; }
    public void setId(Long id)     { this.id = id; }

    public String getTitre()       { return titre; }
    public void setTitre(String t) { this.titre = t; }

    public String getContenu()        { return contenu; }
    public void setContenu(String c)  { this.contenu = c; }

    public Integer getOrdre()         { return ordre; }
    public void setOrdre(Integer o)   { this.ordre = o; }

    public Cours getCourse()          { return course; }
    public void setCourse(Cours c)    { this.course = c; }

    public String getContentType()            { return contentType; }
    public void setContentType(String type)   { this.contentType = type; }

    public String getVideoUrl()               { return videoUrl; }
    public void setVideoUrl(String url)       { this.videoUrl = url; }

    public String getFileName()               { return fileName; }
    public void setFileName(String name)      { this.fileName = name; }

    public List<String> getLinks()            { return links; }
    public void setLinks(List<String> links)  { this.links = links; }

    public String getImageUrl()               { return imageUrl; }
    public void setImageUrl(String url)       { this.imageUrl = url; }

    public Integer getDurationMinutes()           { return durationMinutes; }
    public void setDurationMinutes(Integer mins)  { this.durationMinutes = mins; }

    // ── Difficulty ────────────────────────────────────────────────────────────
    public String getDifficulty()             { return difficulty; }
    public void setDifficulty(String diff)    { this.difficulty = diff; }

    @Override
    public String toString() { return titre; }
}
