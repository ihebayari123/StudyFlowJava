package edu.connexion3a36.models;

public class Sponsor {

    private int id;
    private String nomSponsor;
    private String type;
    private int montant;
    private int eventTitreId;

    // ── Constructeur pour AJOUTER (sans id, l'auto-increment s'en charge)
    public Sponsor(String nomSponsor, String type, int montant, int eventTitreId) {
        this.nomSponsor   = nomSponsor;
        this.type         = type;
        this.montant      = montant;
        this.eventTitreId = eventTitreId;
    }

    // ── Constructeur complet (pour LIRE depuis la BDD)
    public Sponsor(int id, String nomSponsor, String type, int montant, int eventTitreId) {
        this.id           = id;
        this.nomSponsor   = nomSponsor;
        this.type         = type;
        this.montant      = montant;
        this.eventTitreId = eventTitreId;
    }

    // ── Getters
    public int    getId()           { return id; }
    public String getNomSponsor()   { return nomSponsor; }
    public String getType()         { return type; }
    public int    getMontant()      { return montant; }
    public int    getEventTitreId() { return eventTitreId; }

    // ── Setters
    public void setId(int id)                     { this.id = id; }
    public void setNomSponsor(String nomSponsor)   { this.nomSponsor = nomSponsor; }
    public void setType(String type)               { this.type = type; }
    public void setMontant(int montant)            { this.montant = montant; }
    public void setEventTitreId(int eventTitreId) { this.eventTitreId = eventTitreId; }

    @Override
    public String toString() {
        return nomSponsor + " (" + type + ")";
    }
}