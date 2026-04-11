package edu.connexion3a36.entities;

public class Cours {

    private Long id;
    private String titre;
    private String description;
    private String image;
    private Long userId;  // ou Utilisateur user

    // Constructeurs
    public Cours() {}

    public Cours(String titre, String description, String image) {
        this.titre = titre;
        this.description = description;
        this.image = image;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // Ajoutez cette méthode si vous utilisez un objet Utilisateur
    public void setUser(Utilisateur user) {
        if (user != null) {
            this.userId = user.getId();
        }
    }

    // Ou si vous préférez stocker l'objet Utilisateur directement
    // private Utilisateur user;
    // public void setUser(Utilisateur user) {
    //     this.user = user;
    // }
    // public Utilisateur getUser() {
    //     return user;
    // }

    @Override
    public String toString() {
        return titre;
    }
}