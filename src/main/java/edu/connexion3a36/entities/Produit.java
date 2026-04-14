package edu.connexion3a36.entities;

public class Produit {
    protected int id;
    protected String nom;
    protected String description;
    protected int prix;
    protected String image;
    protected int typeCategorieId;
    protected int userId;

    public Produit() {}

    public Produit(String nom, String description, int prix, String image, int typeCategorieId, int userId) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.image = image;
        this.typeCategorieId = typeCategorieId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public int getPrix() { return prix; }
    public String getImage() { return image; }
    public int getTypeCategorieId() { return typeCategorieId; }
    public int getUserId() { return userId; }

    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setDescription(String description) { this.description = description; }
    public void setPrix(int prix) { this.prix = prix; }
    public void setImage(String image) { this.image = image; }
    public void setTypeCategorieId(int typeCategorieId) { this.typeCategorieId = typeCategorieId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        Produit produit = (Produit) object;
        return id == produit.id &&
                prix == produit.prix &&
                typeCategorieId == produit.typeCategorieId &&
                userId == produit.userId &&
                java.util.Objects.equals(nom, produit.nom) &&
                java.util.Objects.equals(description, produit.description) &&
                java.util.Objects.equals(image, produit.image);
    }

    @Override
    public String toString() {
        return "Produit{id=" + id + ", nom=" + nom + ", description=" + description +
                ", prix=" + prix + ", image=" + image +
                ", typeCategorieId=" + typeCategorieId + ", userId=" + userId + "}";
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), id, nom, description, prix, image, typeCategorieId, userId);
    }
}