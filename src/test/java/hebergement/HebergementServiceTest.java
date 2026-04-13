package hebergement;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tn.esprit.models.hebergements.Hebergement;
import tn.esprit.services.hebergement.Hebergement_service;

import java.sql.SQLException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HebergementServiceTest {

    Hebergement_service service;
    int idHebergementTest;
    String nomTest;

    @BeforeAll
    void setup() {
        service = new Hebergement_service();
        nomTest = "Hotel_" + String.valueOf(System.currentTimeMillis()).substring(8);
    }

    @Test
    @Order(1)
    void testAjouterHebergement() throws SQLException {
        // categorie_id=1 et propietaire_id=1 doivent exister dans ta BD
        Hebergement h = new Hebergement(0, nomTest, "Description test",
                "Adresse test", "Tunis", 3, null, null,
                36.8, 10.1, 1, 1, 1);
        int id = service.ajouterAvecId(h);
        idHebergementTest = id;

        System.out.println("nomTest = " + nomTest);
        System.out.println("idHebergementTest = " + idHebergementTest);

        assertTrue(idHebergementTest > 0);
    }

    @Test
    @Order(2)
    void testModifierHebergement() throws SQLException {
        Hebergement h = service.getById(idHebergementTest);
        assertNotNull(h);

        System.out.println("Avant : " + h.getNom());
        h.setNom(nomTest + "_modifie");
        h.setNb_etoiles(5);
        service.modifier(h);

        Hebergement updated = service.getById(idHebergementTest);
        System.out.println("Après : " + updated.getNom());

        assertEquals(nomTest + "_modifie", updated.getNom());
        assertEquals(5, updated.getNb_etoiles());
    }

    @Test
    @Order(3)
    void testSupprimerHebergement() throws SQLException {
        service.supprimer(idHebergementTest);
        assertNull(service.getById(idHebergementTest));
    }
}