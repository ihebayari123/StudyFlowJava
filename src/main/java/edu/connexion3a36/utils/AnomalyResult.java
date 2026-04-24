package edu.connexion3a36.utils;

import java.util.List;

public class AnomalyResult {
    private long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String statut;
    private double score;
    private String niveau;
    private List<String> raisons;

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public List<String> getRaisons() { return raisons; }
    public void setRaisons(List<String> raisons) { this.raisons = raisons; }

    // Utilitaire pour affichage
    public String getNiveauEmoji() {
        return switch (niveau) {
            case "HIGH"   -> "🔴 HIGH";
            case "MEDIUM" -> "🟡 MEDIUM";
            default       -> "🟢 LOW";
        };
    }

    public String getRaisonsFormatees() {
        if (raisons == null || raisons.isEmpty()) return "—";
        return String.join("\n", raisons);
    }
}