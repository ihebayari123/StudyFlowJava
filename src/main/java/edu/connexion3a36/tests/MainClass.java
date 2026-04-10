package edu.connexion3a36.tests;

import edu.connexion3a36.entities.Personne;
import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.PersonneService;
import edu.connexion3a36.services.TypeCategorieService;
import edu.connexion3a36.tools.MyConnection;

import java.sql.Connection;

public class MainClass {
    public static void main(String[] args) {


        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx != null) {
            System.out.println("Connexion réussie !");
        } else {
            System.out.println("Connexion échouée !");
        }

        TypeCategorie typeCategorie = new TypeCategorie("science" , "3ouloum");
        TypeCategorieService typeCategorieService = new TypeCategorieService();
        try {
            typeCategorieService.addCat(typeCategorie);
            System.out.println(typeCategorieService.getData());
            MyConnection mc1 = MyConnection.getInstance();
            MyConnection mc2 = MyConnection.getInstance();
            System.out.println(mc1.hashCode()+" - "+mc2.hashCode());

        }catch (Exception e){
            System.out.println(e.getMessage());
        }








        //MyConnection mc = new MyConnection();
        Personne p = new Personne("ali", "ali");
        PersonneService ps = new PersonneService();
        try {
            //ps.addEntity2(p);
            System.out.println(ps.getData());
            MyConnection mc1 = MyConnection.getInstance();
            MyConnection mc2 = MyConnection.getInstance();
            System.out.println(mc1.hashCode()+" - "+mc2.hashCode());

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
