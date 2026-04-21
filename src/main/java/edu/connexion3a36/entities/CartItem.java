package edu.connexion3a36.entities;

public class CartItem {
    private Produit produit;
    private int quantite;

    public CartItem(Produit produit) {
        this.produit = produit;
        this.quantite = 1;
    }

    public Produit getProduit() { return produit; }
    public int getQuantite() { return quantite; }
    public void incrementer() { quantite++; }
    public double getSousTotal() { return produit.getPrix() * quantite; }
}