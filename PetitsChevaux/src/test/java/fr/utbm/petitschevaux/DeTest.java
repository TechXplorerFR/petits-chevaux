package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeTest {

    @Test
    void unDeEstInitialementAZero() {
        De de = new De(new Random(42));

        assertEquals(0, de.getValeur());
    }

    @Test
    void lancerProduitUneValeurEntreUnEtSix() {
        De de = new De(new Random(42));

        for (int i = 0; i < 100; i++) {
            int valeur = de.lancer();
            assertTrue(valeur >= 1 && valeur <= 6);
            assertEquals(valeur, de.getValeur());
        }
    }

    @Test
    void restaurerAccepteLesValeursAutorisees() {
        De de = new De(new Random(42));

        de.restaurer(0);
        assertEquals(0, de.getValeur());
        de.restaurer(6);
        assertEquals(6, de.getValeur());
    }

    @Test
    void restaurerRefuseUneValeurHorsLimites() {
        De de = new De(new Random(42));

        assertThrows(IllegalArgumentException.class, () -> de.restaurer(-1));
        assertThrows(IllegalArgumentException.class, () -> de.restaurer(7));
    }
}
