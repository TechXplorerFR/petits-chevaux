package fr.utbm.petitschevaux;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoresDAOTest {
    private ScoresDAO scores;

    @BeforeEach
    void creerBaseEnMemoire() {
        // Une base en mémoire différente par test, conservée tant que le test tourne.
        scores = new ScoresDAO(new BaseDeDonnees(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"));
    }

    /** Partie à deux humains (Alice jaune, Bob rouge) qu'Alice vient de gagner. */
    private static Jeu partieGagneePar(String gagnant) {
        Jeu jeu = new Jeu(List.of(new Joueur("Alice", Couleur.JAUNE), new Joueur("Bob", Couleur.ROUGE)),
                new DeTruque(6), ModeJeu.CLASSIQUE);
        Joueur vainqueur = jeu.getJoueurs().stream()
                .filter(joueur -> joueur.getNom().equals(gagnant)).findFirst().orElseThrow();
        jeu.definirPremierJoueur(vainqueur);
        vainqueur.getChevaux().forEach(Cheval::terminer);
        Cheval dernier = vainqueur.getChevaux().get(3);
        dernier.setPosition(Plateau.POSITION_ARRIVEE);
        jeu.lancerDe();
        jeu.jouerCheval(dernier);
        assertTrue(jeu.estTerminee());
        return jeu;
    }

    @Test
    void unePartieTermineeCompteUnePartiePourChaqueHumainEtUneVictoirePourLeGagnant() throws SQLException {
        assertTrue(scores.enregistrerPartie(partieGagneePar("Alice")));

        assertEquals(new Profil("Alice", 1, 1), scores.profil("Alice"));
        assertEquals(new Profil("Bob", 1, 0), scores.profil("Bob"));
        assertEquals(2, scores.meilleursJoueurs(10).size(), "les bots n'ont pas de profil");
    }

    @Test
    void unePartieNEstCompteeQuUneFois() throws SQLException {
        Jeu jeu = partieGagneePar("Alice");
        assertTrue(scores.enregistrerPartie(jeu));
        assertFalse(scores.enregistrerPartie(jeu));
        assertEquals(1, scores.profil("Alice").parties());
        assertEquals(1, scores.dernieresParties(10).size());
    }

    @Test
    void unePartieEnCoursNEstPasEnregistree() throws SQLException {
        Jeu enCours = new Jeu(List.of(new Joueur("Alice", Couleur.JAUNE)));
        assertFalse(scores.enregistrerPartie(enCours));
        assertEquals(0, scores.profil("Alice").parties());
    }

    @Test
    void leClassementTrieParVictoiresPuisRatio() throws SQLException {
        scores.enregistrerPartie(partieGagneePar("Alice"));
        scores.enregistrerPartie(partieGagneePar("Bob"));
        scores.enregistrerPartie(partieGagneePar("Bob"));

        List<Profil> classement = scores.meilleursJoueurs(10);
        assertEquals("Bob", classement.get(0).nom());
        assertEquals(2.0 / 3, classement.get(0).ratioVictoires(), 1e-9);
        assertEquals("Alice", classement.get(1).nom());
    }

    @Test
    void lHistoriqueDonneLesDernieresPartiesEnPremier() throws SQLException {
        scores.enregistrerPartie(partieGagneePar("Alice"));
        scores.enregistrerPartie(partieGagneePar("Bob"));

        List<ResumePartie> parties = scores.dernieresParties(10);
        assertEquals("Bob", parties.get(0).vainqueur());
        assertEquals("Alice", parties.get(1).vainqueur());
        assertFalse(parties.get(0).vainqueurBot());
        assertEquals(ModeJeu.CLASSIQUE, parties.get(0).mode());
        assertEquals(1, parties.get(0).lancers());
    }

    @Test
    void leDrapeauDEnregistrementSurvitALaSauvegarde() throws Exception {
        Jeu jeu = partieGagneePar("Alice");
        scores.enregistrerPartie(jeu);

        Jeu recharge = Sauvegarde.depuisTexte(Sauvegarde.versTexte(jeu));

        assertTrue(recharge.isResultatEnregistre());
        assertFalse(scores.enregistrerPartie(recharge), "recharger une partie finie ne la recompte pas");
    }
}
