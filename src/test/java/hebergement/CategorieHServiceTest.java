package hebergement;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tn.esprit.models.hebergements.Categorie_hebergement;
import tn.esprit.services.hebergement.CategorieH_service;

import java.sql.SQLException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategorieHServiceTest {

    CategorieH_service service;
    int idCategorieTest;
    String nomTest;

    @BeforeAll
    void setup() {
        service = new CategorieH_service();
        nomTest = "Cat_" + String.valueOf(System.currentTimeMillis()).substring(8);
    }

    @Test
    @Order(1)
    void testAjouterCategorie() throws SQLException {
        Categorie_hebergement c = new Categorie_hebergement(0, nomTest, "Description test");
        service.ajouter(c);

        idCategorieTest = service.getAll().stream()
                .filter(cat -> cat.getNom().equals(nomTest))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Catégorie non trouvée"))
                .getId();

        System.out.println("nomTest = " + nomTest);
        System.out.println("idCategorieTest = " + idCategorieTest);

        assertTrue(idCategorieTest > 0);
    }

    @Test
    @Order(2)
    void testModifierCategorie() throws SQLException {
        Categorie_hebergement c = service.getById(idCategorieTest);
        assertNotNull(c);

        System.out.println("Avant : " + c.getNom());
        c.setNom(nomTest + "_modifie");
        service.modifier(c);

        Categorie_hebergement updated = service.getById(idCategorieTest);
        System.out.println("Après : " + updated.getNom());

        assertEquals(nomTest + "_modifie", updated.getNom());
    }

    @Test
    @Order(3)
    void testSupprimerCategorie() throws SQLException {
        service.supprimer(idCategorieTest);
        assertNull(service.getById(idCategorieTest));
    }
}