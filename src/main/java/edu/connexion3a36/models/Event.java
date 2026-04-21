package edu.connexion3a36.models;

import java.time.LocalDateTime;

public class Event {

    private int id;
    private String titre;
    private String description;
    private LocalDateTime dateCreation;
    private String type;
    private String image;
    private int userId;

    // ── Constructeur vide
    public Event() {}

    // ── Constructeur sans id (pour Ajouter)
    public Event(String titre, String description, LocalDateTime dateCreation,
                 String type, String image, int userId) {
        this.titre = titre;
        this.description = description;
        this.dateCreation = dateCreation;
        this.type = type;
        this.image = image;
        this.userId = userId;
    }

    // ── Constructeur avec id (pour Afficher / Modifier)
    public Event(int id, String titre, String description, LocalDateTime dateCreation,
                 String type, String image, int userId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateCreation = dateCreation;
        this.type = type;
        this.image = image;
        this.userId = userId;
    }

    // ── Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}