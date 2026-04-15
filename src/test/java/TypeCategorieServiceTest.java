import edu.connexion3a36.entities.TypeCategorie;
import edu.connexion3a36.services.TypeCategorieService;
import org.junit.jupiter.api.Test;

public class TypeCategorieServiceTest {

    @Test
    public void testAddCat() throws Exception {
        TypeCategorieService service = new TypeCategorieService();  // ✅
        TypeCategorie tc = new TypeCategorie();
        tc.setNomCategorie("Electronique");
        tc.setDescription("Produits electroniques");
        service.addCat(tc);
    }
}