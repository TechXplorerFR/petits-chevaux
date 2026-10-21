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
    void lePremierJoueurChoisiCommenceSansLancerDansLHistorique() {
        Jeu jeu = jeu(4);
        Joueur bleu = jeu.getJoueurs().get(2);
        jeu.definirPremierJoueur(bleu);

        assertSame(bleu, jeu.getJoueurActif());
        assertTrue(jeu.getHistorique().isEmpty());
        assertEquals(0, jeu.getDe().getValeur());
    }

    @Test
    void onNePeutPlusChoisirLePremierJoueurUneFoisLaPartieCommencee() {
        Jeu jeu = jeu(4);
        jeu.lancerDe();
        assertThrows(IllegalStateException.class,
                () -> jeu.definirPremierJoueur(jeu.getJoueurs().get(2)));
    }

    @Test
    void lesStatistiquesSuiventLesLancersDeplacementsEtCaptures() {
        Jeu jeu = jeu(6, 6, 3, 2);
        Joueur alice = jeu.getJoueurs().get(0);
        Joueur bob = jeu.getJoueurs().get(1);
        Cheval chasseur = alice.getChevaux().get(0);
        Cheval victime = bob.getChevaux().get(0);
        victime.setPosition(1); // case absolue 22, soit la position 15 pour Alice

        jeu.lancerDe();               // 6 : sortie
        jeu.jouerCheval(chasseur);
        jeu.lancerDe();               // 6 : 1 -> 7
        jeu.jouerCheval(chasseur);
        chasseur.setPosition(12);
        jeu.lancerDe();               // 3 : 12 -> 15, capture
        jeu.jouerCheval(chasseur);
        jeu.lancerDe();               // 2 pour Bob, aucun cheval jouable

        Statistiques stats = alice.getStatistiques();
        assertEquals(3, stats.getLancers());
        assertEquals(2, stats.getSix());
        assertEquals(5.0, stats.getMoyenneDe());
        assertEquals(1, stats.getSorties());
        assertEquals(9, stats.getCasesParcourues());
        assertEquals(1, stats.getCaptures());
        assertEquals(1, bob.getStatistiques().getChevauxPerdus());
        assertEquals(1, bob.getStatistiques().getToursPasses());
        assertEquals(4, jeu.getNombreLancers());
    }

    @Test
    void leClassementCommenceParLeVainqueurPuisLesChevauxArrives() {
        Jeu jeu = jeu(6);
        Joueur alice = jeu.getJoueurs().get(0);
        Joueur bleu = jeu.getJoueurs().get(2);
        bleu.getChevaux().get(0).terminer();
        bleu.getChevaux().get(1).terminer();
        alice.getChevaux().forEach(Cheval::terminer);
        alice.getChevaux().get(3).setPosition(Plateau.POSITION_ARRIVEE);
        jeu.lancerDe();
        jeu.jouerCheval(alice.getChevaux().get(3));

        List<Joueur> classement = jeu.getClassement();
        assertSame(alice, classement.get(0));
        assertSame(bleu, classement.get(1));
    }

    @Test
    void unBotQuiRemplaceUnJoueurFinitLeTourCommence() {
        Jeu jeu = jeu(6);
        Joueur alice = jeu.getJoueurActif();
        jeu.lancerDe(); // 6 : Alice doit choisir un cheval… puis se déconnecte
        assertTrue(jeu.isAttenteChoix());

        alice.remplacerParBot();
        assertEquals(6, jeu.jouerTourBot(), "le bot joue le 6 déjà lancé, sans relancer");

        assertEquals(1, alice.getChevaux().stream().filter(Cheval::isSorti).count());
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
