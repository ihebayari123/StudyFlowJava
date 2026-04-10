package edu.connexion3a36.entities;

public class TypeCategorie {
    protected int id;
    protected String nomCategorie;
    protected String description;

    public TypeCategorie() {}

    public TypeCategorie(String nomCategorie, String description) {
        this.nomCategorie = nomCategorie;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getNomCategorie() {
        return nomCategorie;
    }

    public String getDescription() {
        return description;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;  // ✅ corrigé
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        TypeCategorie typeCategorie = (TypeCategorie) object;
        return id == typeCategorie.id && java.util.Objects.equals(nomCategorie, typeCategorie.nomCategorie) && java.util.Objects.equals(description, typeCategorie.description);
    }

    @Override
    public String toString() {
        return "TypeCategorie{id=" + id + ", nomCategorie=" + nomCategorie + ", description=" + description + "}";
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), id, nomCategorie, description);
    }
}