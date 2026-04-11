package edu.connexion3a36.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "quiz")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    @NotBlank(message = "Le titre du quiz est obligatoire")
    @Size(min = 3, max = 255,
            message = "Le titre doit contenir entre {min} et {max} caractères")
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Cours course;

    @Column(nullable = false)
    private Integer ordre = 0;

    @Column(name = "passing_score")
    private Integer passingScore = 70; // Score minimum pour réussir (%)

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes; // Limite de temps en minutes (optionnel)

    @Column(name = "is_published")
    private Boolean isPublished = false;

    // Constructeurs
    public Quiz() {}

    public Quiz(String titre, Cours course) {
        this.titre = titre;
        this.course = course;
    }

    public Quiz(String titre, String description, Cours course, Integer ordre) {
        this.titre = titre;
        this.description = description;
        this.course = course;
        this.ordre = ordre;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Cours getCourse() {
        return course;
    }

    public void setCourse(Cours course) {
        this.course = course;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public void setOrdre(Integer ordre) {
        this.ordre = ordre;
    }

    public Integer getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(Integer passingScore) {
        this.passingScore = passingScore;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    @Override
    public String toString() {
        return titre;
    }
}