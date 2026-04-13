package edu.connexion3a36.tests;

import edu.connexion3a36.entities.Personne;
import edu.connexion3a36.entities.Produit;
import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.PersonneService;
import edu.connexion3a36.services.ProduitService;
import edu.connexion3a36.services.TypeCategorieService;
import edu.connexion3a36.tools.MyConnection;
import java.sql.Connection;

public class MainClass {
    public static void main(String[] args) {
        // Test TypeCategorie
        TypeCategorie typeCategorie = new TypeCategorie("science", "3ouloum");
        TypeCategorieService typeCategorieService = new TypeCategorieService();
        try {
            typeCategorieService.addCat(typeCategorie);
            System.out.println(typeCategorieService.getData());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Test Produit
        Produit produit = new Produit("chatgpt", "outils fait des resume", 70, "https://www.the-intl.com/post/chatgbt", 1, 1);
        ProduitService produitService = new ProduitService();
        try {
            produitService.addP(produit);
            System.out.println(produitService.getData());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Test Personne
        Personne p = new Personne("ali", "ali");
        PersonneService ps = new PersonneService();
        try {
            MyConnection.getInstance();
            Connection cnx = MyConnection.getCnx();
            if (cnx != null && !cnx.isClosed()) {
                System.out.println("✅ Connexion à la base studyflow réussie !");
            } else {
                System.out.println("❌ Échec de la connexion à la base studyflow !");
            }

            System.out.println(ps.getData());

            // Vérifier le singleton
            MyConnection mc1 = MyConnection.getInstance();
            MyConnection mc2 = MyConnection.getInstance();
            System.out.println(mc1.hashCode() + " - " + mc2.hashCode());

        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}