package edu.connexion3a36.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100,
            message = "Le nom doit contenir entre {min} et {max} caractères")
    private String nom;

    @Column(length = 100, nullable = false)
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100,
            message = "Le prénom doit contenir entre {min} et {max} caractères")
    private String prenom;

    @Column(length = 150, unique = true, nullable = false)
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Veuillez entrer un email valide")
    private String email;

    @Column(length = 255, nullable = false)
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    @Column(length = 20)
    private String role; // ADMIN, FORMATEUR, ETUDIANT

    @Column(length = 20)
    private String status; // ACTIF, INACTIF, BLOQUE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_frequency")
    private int loginFrequency;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts;

    @Column(length = 255)
    private String avatar;

    @Column(length = 15)
    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "face_encoding", columnDefinition = "LONGTEXT")
    private String faceEncoding;

    @Column(name = "face_attempts")
    private int faceAttempts;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Cours> cours = new ArrayList<>();

    // Constructeurs
    public Utilisateur() {
        this.createdAt = LocalDateTime.now();
        this.role = "ETUDIANT";
        this.status = "ACTIF";
    }

    public Utilisateur(String nom, String prenom, String email, String password) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.role = "ETUDIANT";
        this.status = "ACTIF";
    }

    public Utilisateur(String nom, String prenom, String email, String password, String role) {
        this(nom, prenom, email, password);
        this.role = role;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Alias for older code still calling getMotDePasse() / setMotDePasse()
    public String getMotDePasse() { return password; }
    public void setMotDePasse(String motDePasse) { this.password = motDePasse; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Alias for older code still calling getStatutCompte() / setStatutCompte()
    public String getStatutCompte() { return status; }
    public void setStatutCompte(String statutCompte) { this.status = statutCompte; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getFaceEncoding() { return faceEncoding; }
    public void setFaceEncoding(String faceEncoding) { this.faceEncoding = faceEncoding; }

    public int getFaceAttempts() { return faceAttempts; }
    public void setFaceAttempts(int faceAttempts) { this.faceAttempts = faceAttempts; }

    public List<Cours> getCours() { return cours; }
    public void setCours(List<Cours> cours) { this.cours = cours; }

    public int getLoginFrequency() { return loginFrequency; }
    public void setLoginFrequency(int loginFrequency) { this.loginFrequency = loginFrequency; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    // Méthodes utilitaires
    public String getFullName() { return prenom + " " + nom; }

    public void addCours(Cours cours) {
        this.cours.add(cours);
        cours.setUser(this);
    }

    public void removeCours(Cours cours) {
        this.cours.remove(cours);
        cours.setUser(null);
    }

    public boolean isAdmin() { return "ADMIN".equals(this.role); }
    public boolean isFormateur() { return "FORMATEUR".equals(this.role); }
    public boolean isEtudiant() { return "ETUDIANT".equals(this.role); }
    public boolean isActive() { return "ACTIF".equals(this.status); }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return getFullName() + " (" + email + ")";
    }
}