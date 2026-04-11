package edu.connexion3a36.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cours")
public class Cours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    @NotBlank(message = "Le titre du cours est obligatoire")
    @Size(min = 3, max = 255,
            message = "Le titre doit contenir entre {min} et {max} caractères")
    private String titre;

    @Column(length = 255, nullable = false)
    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 255,
            message = "La description doit contenir entre {min} et {max} caractères")
    private String description;

    @Column(length = 255)
    private String image;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Utilisateur user;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Chapitre> chapitres = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Quiz> quizzes = new ArrayList<>();

    // Constructeurs
    public Cours() {}

    // Getters et Setters (identiques à avant mais avec Long au lieu de Long)
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Utilisateur getUser() {
        return user;
    }

    public void setUser(Utilisateur user) {
        this.user = user;
    }

    public List<Chapitre> getChapitres() {
        return chapitres;
    }

    public void setChapitres(List<Chapitre> chapitres) {
        this.chapitres = chapitres;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

    // Méthodes utilitaires
    public void addChapitre(Chapitre chapitre) {
        chapitres.add(chapitre);
        chapitre.setCourse(this);
    }

    public void removeChapitre(Chapitre chapitre) {
        chapitres.remove(chapitre);
        chapitre.setCourse(null);
    }

    public void addQuiz(Quiz quiz) {
        quizzes.add(quiz);
        quiz.setCourse(this);
    }

    public void removeQuiz(Quiz quiz) {
        quizzes.remove(quiz);
        quiz.setCourse(null);
    }

    @Override
    public String toString() {
        return titre;
    }
}