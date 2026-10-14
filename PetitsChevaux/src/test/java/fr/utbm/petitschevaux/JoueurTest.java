package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoueurTest {

    @Test
    void unJoueurEstCreeAvecQuatreChevauxDansLEcurie() {
        Joueur joueur = new Joueur("Alice", Couleur.JAUNE);

        assertEquals("Alice", joueur.getNom());
        assertEquals(Couleur.JAUNE, joueur.getCouleur());
        assertFalse(joueur.isBot());
        assertEquals(Joueur.NOMBRE_CHEVAUX, joueur.getChevaux().size());
        for (int numero = 0; numero < Joueur.NOMBRE_CHEVAUX; numero++) {
            Cheval cheval = joueur.getChevaux().get(numero);
            assertEquals(numero, cheval.getNumero());
            assertEquals(Cheval.ECURIE, cheval.getPosition());
        }
    }

    @Test
    void unJoueurPeutEtreUnBot() {
        Joueur joueur = new Joueur("Bot", Couleur.ROUGE, true);

        assertTrue(joueur.isBot());
    }

    @Test
    void uneCouleurEstObligatoire() {
        assertThrows(IllegalArgumentException.class,
                () -> new Joueur("Alice", null));
    }

    @Test
    void lesChevauxDuJoueurNeSontPasModifiablesDepuisLaListe() {
        Joueur joueur = new Joueur("Alice", Couleur.JAUNE);

        assertThrows(UnsupportedOperationException.class,
                () -> joueur.getChevaux().clear());
    }

    @Test
    void unJoueurNeGagneQueLorsqueTousSesChevauxSontTermines() {
        Joueur joueur = new Joueur("Alice", Couleur.JAUNE);

        assertFalse(joueur.aGagne());
        joueur.getChevaux().forEach(Cheval::terminer);

        assertTrue(joueur.aGagne());
    }
}
