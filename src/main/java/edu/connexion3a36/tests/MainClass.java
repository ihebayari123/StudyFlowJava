package edu.connexion3a36.tests;

import edu.connexion3a36.entities.Personne;
import edu.connexion3a36.services.PersonneService;
import edu.connexion3a36.tools.MyConnection;

public class MainClass {
    public static void main(String[] args) {
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
