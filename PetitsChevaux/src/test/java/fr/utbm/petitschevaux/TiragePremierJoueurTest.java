package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiragePremierJoueurTest {
    private final Joueur jaune = new Joueur("Jaune", Couleur.JAUNE);
    private final Joueur rouge = new Joueur("Rouge", Couleur.ROUGE);
    private final Joueur bleu = new Joueur("Bleu", Couleur.BLEU);
    private final Joueur vert = new Joueur("Vert", Couleur.VERT);
    private final List<Joueur> joueurs = List.of(jaune, rouge, bleu, vert);

    @Test
    void chacunLanceASonTourEtLePlusHautScoreCommence() {
        TiragePremierJoueur tirage = new TiragePremierJoueur(joueurs, new DeTruque(2, 6, 3, 1));

        assertSame(jaune, tirage.getLanceur());
        tirage.lancer();
        assertSame(rouge, tirage.getLanceur());
        assertEquals(2, tirage.getResultat(jaune));
        tirage.lancer();
        tirage.lancer();
        assertFalse(tirage.estTermine());
        tirage.lancer();

        assertTrue(tirage.estTermine());
        assertSame(rouge, tirage.getGagnant());
        assertNull(tirage.getLanceur());
        assertEquals(1, tirage.getManche());
    }

    @Test
    void enCasDEgaliteSeulsLesExAequoRelancent() {
        TiragePremierJoueur tirage = new TiragePremierJoueur(joueurs, new DeTruque(3, 5, 5, 2, 4, 6));
        for (int i = 0; i < 4; i++) {
            tirage.lancer();
        }

        assertFalse(tirage.estTermine());
        assertEquals(2, tirage.getManche());
        assertEquals(List.of(rouge, bleu), tirage.getDerniereEgalite());
        assertEquals(5, tirage.getValeurEgalite());
        assertFalse(tirage.estCandidat(jaune));
        assertEquals(3, tirage.getResultat(jaune), "les éliminés gardent leur score");
        assertEquals(0, tirage.getResultat(rouge), "les ex æquo doivent relancer");
        assertSame(rouge, tirage.getLanceur());

        tirage.lancer();
        tirage.lancer();
        assertSame(bleu, tirage.getGagnant());
    }

    @Test
    void onNePeutPlusLancerUneFoisLeTirageTermine() {
        TiragePremierJoueur tirage = new TiragePremierJoueur(joueurs, new DeTruque(6, 1, 1, 1));
        for (int i = 0; i < 4; i++) {
            tirage.lancer();
        }
        assertThrows(IllegalStateException.class, tirage::lancer);
    }
}
