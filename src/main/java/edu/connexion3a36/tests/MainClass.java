package edu.connexion3a36.tests;

import edu.connexion3a36.entities.Personne;
import edu.connexion3a36.services.PersonneService;
import edu.connexion3a36.tools.MyConnection;
import java.sql.Connection;

public class MainClass {
    public static void main(String[] args) {
        Personne p = new Personne("ali", "ali");
        PersonneService ps = new PersonneService();
        try {
            // Tester la connexion à la base
            MyConnection.getInstance();
            Connection cnx = MyConnection.getCnx();
            if (cnx != null && !cnx.isClosed()) {
                System.out.println("✅ Connexion à la base studyflow réussie !");
            } else {
                System.out.println("❌ Échec de la connexion à la base studyflow !");
            }

            // Tester les services
            //ps.addEntity2(p);
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
