package edu.connexion3a36.entities;

import java.util.Objects;

public class Utilisateur {
    protected int id;
    protected String nom;
    protected String prenom;
    protected String email;
    protected String motDePasse;
    protected String role;
    protected String statutCompte;

    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, String email, String motDePasse, String role, String statutCompte) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.statutCompte = statutCompte;
    }

    public Utilisateur(int id, String nom, String prenom, String email, String motDePasse, String role, String statutCompte) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.statutCompte = statutCompte;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getMotDePasse() { return motDePasse; }
    public String getRole() { return role; }
    public String getStatutCompte() { return statutCompte; }

    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setEmail(String email) { this.email = email; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public void setRole(String role) { this.role = role; }
    public void setStatutCompte(String statutCompte) { this.statutCompte = statutCompte; }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Utilisateur utilisateur = (Utilisateur) object;
        return id == utilisateur.id &&
                Objects.equals(nom, utilisateur.nom) &&
                Objects.equals(prenom, utilisateur.prenom) &&
                Objects.equals(email, utilisateur.email) &&
                Objects.equals(role, utilisateur.role) &&
                Objects.equals(statutCompte, utilisateur.statutCompte);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, prenom, email, role, statutCompte);
    }

    @Override
    public String toString() {
        return "Utilisateur{id=" + id +
                ", nom=" + nom +
                ", prenom=" + prenom +
                ", email=" + email +
                ", role=" + role +
                ", statutCompte=" + statutCompte + "}";
    }
}