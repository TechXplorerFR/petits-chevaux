package fr.utbm.petitschevaux;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateauTest {
    private final Plateau plateau = new Plateau();
    private Joueur jaune;
    private Joueur rouge;
    private List<Joueur> joueurs;

    @BeforeEach
    void creerJoueurs() {
        jaune = new Joueur("Jaune", Couleur.JAUNE);
        rouge = new Joueur("Rouge", Couleur.ROUGE);
        joueurs = List.of(jaune, rouge);
    }

    private Cheval cheval(Joueur joueur, int numero, int position) {
        Cheval cheval = joueur.getChevaux().get(numero);
        cheval.setPosition(position);
        return cheval;
    }

    @Test
    void unChevalNeSortQueSurUnSix() {
        Cheval cheval = cheval(jaune, 0, Cheval.ECURIE);
        for (int valeur = 1; valeur <= 5; valeur++) {
            assertFalse(plateau.estDeplacementValide(jaune, cheval, valeur, joueurs));
        }
        assertTrue(plateau.estDeplacementValide(jaune, cheval, 6, joueurs));
        assertEquals(1, plateau.destination(cheval, 6));
    }

    @Test
    void onNeSortPasSurSonPropreCheval() {
        cheval(jaune, 0, 1);
        assertFalse(plateau.estDeplacementValide(jaune, cheval(jaune, 1, Cheval.ECURIE), 6, joueurs));
    }

    @Test
    void unChevalAvanceDuNombreDeCases() {
        Cheval cheval = cheval(jaune, 0, 10);
        assertTrue(plateau.estDeplacementValide(jaune, cheval, 4, joueurs));
        assertEquals(14, plateau.destination(cheval, 4));
    }

    @Test
    void unDeTropFortFaitReculerDepuisLePiedDeLEscalier() {
        Cheval cheval = cheval(jaune, 0, 55);
        assertEquals(54, plateau.destination(cheval, 3));
        assertEquals(Plateau.POSITION_BAS_ESCALIER, plateau.destination(cheval(jaune, 1, 50), 6));
    }

    @Test
    void chaqueMarcheSeMonteAvecSonNumero() {
        Cheval cheval = cheval(jaune, 0, Plateau.POSITION_BAS_ESCALIER);
        assertTrue(plateau.estDeplacementValide(jaune, cheval, 1, joueurs));
        assertFalse(plateau.estDeplacementValide(jaune, cheval, 2, joueurs));

        cheval.setPosition(Plateau.POSITION_BAS_ESCALIER + 1);
        assertTrue(plateau.estDeplacementValide(jaune, cheval, 2, joueurs));
        assertFalse(plateau.estDeplacementValide(jaune, cheval, 1, joueurs));
        assertEquals(Plateau.POSITION_BAS_ESCALIER + 2, plateau.destination(cheval, 2));
    }

    @Test
    void unSixDepuisLaDerniereMarcheFaitEntrerAuCentre() {
        Cheval cheval = cheval(jaune, 0, Plateau.POSITION_ARRIVEE);
        assertFalse(plateau.estDeplacementValide(jaune, cheval, 5, joueurs));
        assertTrue(plateau.estDeplacementValide(jaune, cheval, 6, joueurs));
        assertEquals(Cheval.CENTRE, plateau.destination(cheval, 6));
    }

    @Test
    void onNeMonteSurUneMarcheOccupeeParSonCheval() {
        cheval(jaune, 0, Plateau.POSITION_BAS_ESCALIER + 1);
        Cheval cheval = cheval(jaune, 1, Plateau.POSITION_BAS_ESCALIER);
        assertFalse(plateau.estDeplacementValide(jaune, cheval, 1, joueurs));
    }

    @Test
    void onNePeutNiDoublerNiSArreterSurSonCheval() {
        cheval(jaune, 0, 5);
        Cheval cheval = cheval(jaune, 1, 3);
        assertFalse(plateau.estDeplacementValide(jaune, cheval, 2, joueurs), "arrêt sur son cheval");
        assertFalse(plateau.estDeplacementValide(jaune, cheval, 4, joueurs), "dépassement");
        assertTrue(plateau.estDeplacementValide(jaune, cheval, 1, joueurs));
    }

    @Test
    void onNePeutPasDoublerUnAdversaire() {
        // Rouge sort en case absolue 22, soit la position 15 pour Jaune.
        cheval(rouge, 0, 1);
        Cheval cheval = cheval(jaune, 0, 12);
        assertFalse(plateau.estDeplacementValide(jaune, cheval, 5, joueurs));
        assertTrue(plateau.estDeplacementValide(jaune, cheval, 3, joueurs));
    }

    @Test
    void arriverSurUnAdversaireLeRenvoieALEcurie() {
        Cheval victime = cheval(rouge, 0, 1);
        Cheval cheval = cheval(jaune, 0, 12);
        assertTrue(plateau.capturerait(jaune, cheval, 3, joueurs));

        cheval.setPosition(plateau.destination(cheval, 3));
        plateau.gererCapture(jaune, cheval, joueurs);

        assertEquals(Cheval.ECURIE, victime.getPosition());
        assertEquals(15, cheval.getPosition());
    }

    @Test
    void entrerDansSonEscalierNeCaptureRienDansLesAutresEscaliers() {
        Cheval rougeDansEscalier = cheval(rouge, 0, Plateau.POSITION_BAS_ESCALIER + 2);
        Cheval cheval = cheval(jaune, 0, Plateau.POSITION_BAS_ESCALIER + 1);
        cheval.setPosition(plateau.destination(cheval, 2));
        plateau.gererCapture(jaune, cheval, joueurs);
        assertEquals(Plateau.POSITION_BAS_ESCALIER + 2, rougeDansEscalier.getPosition());
    }

    @Test
    void lesCasesDeDepartSontEspaceesDUnQuartier() {
        assertEquals(8, Plateau.decalageDepart(Couleur.JAUNE));
        assertEquals(22, Plateau.decalageDepart(Couleur.ROUGE));
        assertEquals(36, Plateau.decalageDepart(Couleur.BLEU));
        assertEquals(50, Plateau.decalageDepart(Couleur.VERT));
    }
}
