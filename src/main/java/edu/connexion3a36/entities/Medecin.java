package edu.connexion3a36.entities;

public class Medecin {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String disponibilite;

    public Medecin() {}

    public Medecin(String nom, String prenom, String email, String telephone, String disponibilite) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.disponibilite = disponibilite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getDisponibilite() { return disponibilite; }
    public void setDisponibilite(String disponibilite) { this.disponibilite = disponibilite; }

    @Override
    public String toString() { return id + " - " + nom + " " + prenom; }
}