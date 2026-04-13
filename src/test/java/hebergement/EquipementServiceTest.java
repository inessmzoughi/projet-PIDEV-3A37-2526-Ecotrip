package hebergement;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tn.esprit.models.hebergements.Equipement;
import tn.esprit.services.hebergement.Equipement_service;

import java.sql.SQLException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EquipementServiceTest {

    Equipement_service service;
    int idEquipementTest;
    String nomTest;

    @BeforeAll
    void setup() {
        service = new Equipement_service();
        nomTest = "Equip_" + String.valueOf(System.currentTimeMillis()).substring(8);
    }

    @Test
    @Order(1)
    void testAjouterEquipement() throws SQLException {
        Equipement e = new Equipement(0, nomTest, "Description test");
        service.ajouter(e);

        idEquipementTest = service.getAll().stream()
                .filter(eq -> eq.getNom().equals(nomTest))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Equipement non trouvé"))
                .getId();

        System.out.println("nomTest = " + nomTest);
        System.out.println("idEquipementTest = " + idEquipementTest);

        assertTrue(idEquipementTest > 0);
    }

    @Test
    @Order(2)
    void testModifierEquipement() throws SQLException {
        Equipement e = service.getById(idEquipementTest);
        assertNotNull(e);

        System.out.println("Avant : " + e.getNom());
        e.setNom(nomTest + "_modifie");
        service.modifier(e);

        Equipement updated = service.getById(idEquipementTest);
        System.out.println("Après : " + updated.getNom());

        assertEquals(nomTest + "_modifie", updated.getNom());
    }

    @Test
    @Order(3)
    void testSupprimerEquipement() throws SQLException {
        service.supprimer(idEquipementTest);
        assertNull(service.getById(idEquipementTest));
    }
}