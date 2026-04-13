import edu.connexion3a36.entities.Produit;
import edu.connexion3a36.services.ProduitService;
import org.junit.jupiter.api.Test;

public class ProduitServiceTest {

    @Test
    public void testAddProduit() throws Exception {
        ProduitService service = new ProduitService();
        Produit p = new Produit();
        p.setNom("Laptop");
        p.setDescription("Ordinateur portable");
        p.setPrix(1200);
        p.setImage("laptop.png");
        p.setTypeCategorieId(1);
        p.setUserId(1);
        service.addP(p);
    }
}