package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeuTest {

    private static Jeu jeu(int... des) {
        return new Jeu(List.of(new Joueur("Alice", Couleur.JAUNE),
                new Joueur("Bob", Couleur.ROUGE)), new DeTruque(des));
    }

    @Test
    void lesCouleursLibresSontCompleteesParDesBotsDansLOrdreDuPlateau() {
        Jeu jeu = new Jeu(List.of(new Joueur("Alice", Couleur.VERT)));
        List<Joueur> joueurs = jeu.getJoueurs();
        assertEquals(4, joueurs.size());
        assertEquals(List.of(Couleur.JAUNE, Couleur.ROUGE, Couleur.BLEU, Couleur.VERT),
                joueurs.stream().map(Joueur::getCouleur).toList());
        assertEquals(3, joueurs.stream().filter(Joueur::isBot).count());
        assertFalse(joueurs.get(3).isBot());
    }

    @Test
    void deuxJoueursDeLaMemeCouleurSontRefuses() {
        assertThrows(IllegalArgumentException.class, () -> new Jeu(List.of(
                new Joueur("A", Couleur.JAUNE), new Joueur("B", Couleur.JAUNE))));
    }

    @Test
    void sansChevalJouableLeTourPasseAuJoueurSuivant() {
        Jeu jeu = jeu(3);
        assertEquals(3, jeu.lancerDe());
        assertTrue(jeu.peutLancer());
        assertEquals(1, jeu.getJoueurCourant());
    }

    @Test
    void unSixSansChevalJouablePermetDeRelancer() {
        Jeu jeu = jeu(6);
        Joueur alice = jeu.getJoueurActif();
        alice.getChevaux().forEach(Cheval::terminer);
        alice.getChevaux().get(0).setPosition(Plateau.POSITION_BAS_ESCALIER + 1);
        jeu.lancerDe();
        assertTrue(jeu.peutLancer());
        assertSame(alice, jeu.getJoueurActif());
    }

    @Test
    void onNePeutPasRelancerAvantDAvoirJoue() {
        Jeu jeu = jeu(6, 2);
        jeu.lancerDe();
        assertTrue(jeu.isAttenteChoix());
        assertEquals(0, jeu.lancerDe());
        assertEquals(6, jeu.getDe().getValeur());
    }

    @Test
    void onNePeutJouerQueSesPropresChevauxApresAvoirLance() {
        Jeu jeu = jeu(6);
        Cheval chevalDAlice = jeu.getJoueurs().get(0).getChevaux().get(0);
        Cheval chevalDeBob = jeu.getJoueurs().get(1).getChevaux().get(0);
        assertFalse(jeu.jouerCheval(chevalDAlice), "le dé n'a pas été lancé");
        jeu.lancerDe();
        assertFalse(jeu.jouerCheval(chevalDeBob));
        assertTrue(jeu.jouerCheval(chevalDAlice));
        assertEquals(1, chevalDAlice.getPosition());
    }

    @Test
    void unSixFaitRejouerLeMemeJoueur() {
        Jeu jeu = jeu(6, 4);
        Cheval cheval = jeu.getJoueurActif().getChevaux().get(0);
        jeu.lancerDe();
        jeu.jouerCheval(cheval);
        assertEquals(0, jeu.getJoueurCourant());

        jeu.lancerDe();
        jeu.jouerCheval(cheval);
        assertEquals(5, cheval.getPosition());
        assertEquals(1, jeu.getJoueurCourant());
    }

    @Test
    void ramenerSesQuatreChevauxTermineLaPartie() {
        Jeu jeu = jeu(6);
        Joueur alice = jeu.getJoueurActif();
        alice.getChevaux().forEach(Cheval::terminer);
        Cheval dernier = alice.getChevaux().get(3);
        dernier.setPosition(Plateau.POSITION_ARRIVEE);
        assertNull(jeu.getVainqueur());

        jeu.lancerDe();
        assertTrue(jeu.jouerCheval(dernier));

        assertTrue(jeu.estTerminee());
        assertSame(alice, jeu.getVainqueur());
        assertFalse(jeu.peutLancer());
    }

    @Test
    void leBotPrefereCapturer() {
        Jeu jeu = new Jeu(List.of(new Joueur("Bob", Couleur.ROUGE)), new DeTruque(3));
        Joueur bot = jeu.getJoueurActif();
        assertTrue(bot.isBot());
        Cheval avance = bot.getChevaux().get(0);
        avance.setPosition(40);
        Cheval chasseur = bot.getChevaux().get(1);
        chasseur.setPosition(12);
        Cheval victime = jeu.getJoueurs().get(1).getChevaux().get(0);
        victime.setPosition(1);

        jeu.jouerTourBot();

        assertEquals(15, chasseur.getPosition());
        assertEquals(40, avance.getPosition());
        assertEquals(Cheval.ECURIE, victime.getPosition());
    }

    @Test
    void lePremierJoueurEstLePlusFortAuDeAvecRelanceDesExAequo() {
        Jeu jeu = new Jeu(List.of(new Joueur("Alice", Couleur.JAUNE)), new DeTruque(3, 5, 5, 2, 4, 6));
        Joueur premier = jeu.choisirPremierJoueur();
        assertSame(jeu.getJoueurs().get(2), premier);
        assertEquals(2, jeu.getJoueurCourant());
        assertEquals(6, jeu.getHistorique().get(0).valeur());
        assertTrue(jeu.peutLancer());
    }

    @Test
    void lHistoriqueGardeLesDerniersLancers() {
        Jeu jeu = jeu(1, 2, 3, 4, 5, 1, 2, 3, 4);
        for (int i = 0; i < 9; i++) {
            jeu.lancerDe();
        }
        assertEquals(Jeu.TAILLE_HISTORIQUE, jeu.getHistorique().size());
        assertEquals(4, jeu.getHistorique().get(0).valeur());
    }
}
