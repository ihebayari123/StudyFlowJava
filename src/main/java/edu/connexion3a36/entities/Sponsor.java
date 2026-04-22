package edu.connexion3a36.entities;

public class Sponsor {

    private int id;
    private String nomSponsor;
    private String type;
    private int montant;
    private int eventTitreId;   // clé étrangère → event.id

    // ── Constructeurs ──────────────────────────────────────────────────────────

    public Sponsor() {}

    public Sponsor(int id, String nomSponsor, String type,
                   int montant, int eventTitreId) {
        this.id = id;
        this.nomSponsor = nomSponsor;
        this.type = type;
        this.montant = montant;
        this.eventTitreId = eventTitreId;
    }

    /** Constructeur sans id (pour l'insertion en BDD) */
    public Sponsor(String nomSponsor, String type,
                   int montant, int eventTitreId) {
        this.nomSponsor = nomSponsor;
        this.type = type;
        this.montant = montant;
        this.eventTitreId = eventTitreId;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomSponsor() { return nomSponsor; }
    public void setNomSponsor(String nomSponsor) { this.nomSponsor = nomSponsor; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getMontant() { return montant; }
    public void setMontant(int montant) { this.montant = montant; }

    public int getEventTitreId() { return eventTitreId; }
    public void setEventTitreId(int eventTitreId) { this.eventTitreId = eventTitreId; }

    // ── toString ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Sponsor{" +
                "id=" + id +
                ", nomSponsor='" + nomSponsor + '\'' +
                ", type='" + type + '\'' +
                ", montant=" + montant +
                ", eventTitreId=" + eventTitreId +
                '}';
    }
}