package edu.connexion3a36.services;

import edu.connexion3a36.entities.CartItem;
import edu.connexion3a36.entities.Produit;
import java.util.*;

public class CartService {
    private static CartService instance;
    private final List<CartItem> items = new ArrayList<>();

    private CartService() {}

    public static CartService getInstance() {
        if (instance == null) instance = new CartService();
        return instance;
    }

    public void ajouterProduit(Produit p) {
        for (CartItem item : items) {
            if (item.getProduit().getId() == p.getId()) {
                item.incrementer();
                return;
            }
        }
        items.add(new CartItem(p));
    }

    public List<CartItem> getItems() { return items; }

    public double getTotal() {
        return items.stream().mapToDouble(CartItem::getSousTotal).sum();
    }

    public int getNombreArticles() {
        return items.stream().mapToInt(CartItem::getQuantite).sum();
    }

    public void vider() { items.clear(); }
    public void supprimerItem(CartItem item) { items.remove(item); }
}