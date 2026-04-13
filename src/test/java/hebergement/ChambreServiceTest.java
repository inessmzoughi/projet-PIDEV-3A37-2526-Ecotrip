package hebergement;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tn.esprit.models.hebergements.Chambre;
import tn.esprit.services.hebergement.Chambre_service;

import java.sql.SQLException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChambreServiceTest {

    Chambre_service service;
    int idChambreTest;
    String numeroTest;

    @BeforeAll
    void setup() {
        service = new Chambre_service();
        numeroTest = String.valueOf(System.currentTimeMillis()).substring(8);
    }

    @Test
    @Order(1)
    void testAjouterChambre() throws SQLException {
        Chambre c = new Chambre(0, 1, 1, "Chambre test", 80.0, 2, "Simple", numeroTest);
        service.ajouter(c);

        List<Chambre> list = service.getAll();
        System.out.println("=== Liste après ajout ===");
        list.forEach(ch -> System.out.println("ID=" + ch.getId() + " | Numero=" + ch.getNumero()));
        System.out.println("numeroTest = " + numeroTest);
        System.out.println("idChambreTest avant filtre = " + idChambreTest);

        idChambreTest = list.stream()
                .filter(ch -> ch.getNumero().equals(numeroTest))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Chambre non trouvée"))
                .getId();

        System.out.println("idChambreTest après filtre = " + idChambreTest);
        assertTrue(idChambreTest > 0);
    }

    @Test
    @Order(2)
    void testModifierChambre() throws SQLException {
        Chambre c = service.getById(idChambreTest);
        System.out.println("Avant : " + c.getNumero());
        c.setNumero("000");
        System.out.println("Après setter : " + c.getNumero()); // doit afficher 000
        service.modifier(c);
        Chambre updated = service.getById(idChambreTest);
        System.out.println("En BD : " + updated.getNumero()); // doit afficher 000
    }

    @Test
    @Order(3)
    void testSupprimerChambre() throws SQLException {
        service.supprimer(idChambreTest);
        assertNull(service.getById(idChambreTest));
    }
}