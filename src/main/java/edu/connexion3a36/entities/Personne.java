package edu.connexion3a36.entities;

public class Personne {
    protected int id;
    protected  String nom;
    protected  String prenom;

    public Personne() {}

    public Personne(String nom, String prenom) {
        this.nom = nom;
        this.prenom = prenom;
    }

    public int getId() {
        return id;
    }

    public  String getNom() {
        return nom;
    }

    public  String getPrenom() {
        return prenom;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        Personne personne = (Personne) object;
        return id == personne.id && java.util.Objects.equals(nom, personne.nom) && java.util.Objects.equals(prenom, personne.prenom);
    }

    @Override
    public String toString() {
        return "Personne{id=" + id + ", nom=" + nom + ", prenom=" + prenom + "}";
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), id, nom, prenom);
    }
}
