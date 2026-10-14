package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChevalTest {

    @Test
    void unChevalEstInitialementDansLEcurie() {
        Cheval cheval = new Cheval(2);

        assertEquals(2, cheval.getNumero());
        assertEquals(Cheval.ECURIE, cheval.getPosition());
        assertFalse(cheval.isSorti());
        assertFalse(cheval.estEnJeu());
        assertFalse(cheval.isTermine());
    }

    @Test
    void sortirPlaceLeChevalSurLaPremiereCase() {
        Cheval cheval = new Cheval(0);

        cheval.sortir();

        assertEquals(1, cheval.getPosition());
        assertTrue(cheval.isSorti());
        assertTrue(cheval.estEnJeu());
        assertFalse(cheval.isTermine());
    }

    @Test
    void terminerPlaceLeChevalAuCentre() {
        Cheval cheval = new Cheval(0);

        cheval.terminer();

        assertEquals(Cheval.CENTRE, cheval.getPosition());
        assertTrue(cheval.isSorti());
        assertTrue(cheval.isTermine());
        assertFalse(cheval.estEnJeu());
    }

    @Test
    void rentrerEcurieRemetLeChevalDansSonEtatInitial() {
        Cheval cheval = new Cheval(0);
        cheval.terminer();

        cheval.rentrerEcurie();

        assertEquals(Cheval.ECURIE, cheval.getPosition());
        assertFalse(cheval.isSorti());
        assertFalse(cheval.isTermine());
        assertFalse(cheval.estEnJeu());
    }

    @Test
    void unePositionHorsLimitesEstRefusee() {
        Cheval cheval = new Cheval(0);

        assertThrows(IllegalArgumentException.class, () -> cheval.setPosition(-1));
        assertThrows(IllegalArgumentException.class,
                () -> cheval.setPosition(Cheval.CENTRE + 1));
    }
}
