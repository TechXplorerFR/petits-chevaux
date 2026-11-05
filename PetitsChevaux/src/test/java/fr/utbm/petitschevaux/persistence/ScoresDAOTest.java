package fr.utbm.petitschevaux.persistence;

import fr.utbm.petitschevaux.model.BilanParties;
import fr.utbm.petitschevaux.model.Cheval;
import fr.utbm.petitschevaux.model.Couleur;
import fr.utbm.petitschevaux.model.DeTruque;
import fr.utbm.petitschevaux.model.Jeu;
import fr.utbm.petitschevaux.model.Joueur;
import fr.utbm.petitschevaux.model.ModeJeu;
import fr.utbm.petitschevaux.model.MoyennesStatistiques;
import fr.utbm.petitschevaux.model.Plateau;
import fr.utbm.petitschevaux.model.Profil;
import fr.utbm.petitschevaux.model.ResumePartie;
import fr.utbm.petitschevaux.model.StatistiquesJoueur;

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
    void toutesLesStatistiquesDesJoueursSontEnregistrees() throws SQLException {
        Jeu jeu = partieGagneePar("Bob");
        scores.enregistrerPartie(jeu);

        int id = scores.dernieresParties(1).get(0).id();
        List<StatistiquesJoueur> lignes = scores.statistiquesPartie(id);

        assertEquals(4, lignes.size(), "une ligne par joueur, bots compris");
        StatistiquesJoueur bob = lignes.get(0);
        assertEquals(StatistiquesJoueur.de(1, jeu.getJoueurs().get(Couleur.ROUGE.ordinal())), bob);
        assertTrue(bob.vainqueur());
        assertEquals(4, bob.chevauxArrives());
        assertEquals(1, bob.lancers());
        assertEquals(1, bob.six());
        assertEquals(6.0, bob.moyenneDe(), 1e-9);
        assertEquals(List.of(1, 2, 3, 4), lignes.stream().map(StatistiquesJoueur::rang).toList());
        assertEquals(2, lignes.stream().filter(StatistiquesJoueur::bot).count());
        assertTrue(scores.statistiquesPartie(id + 1).isEmpty());
    }

    @Test
    void lesMoyennesPortentSurToutesLesParties() throws SQLException {
        assertEquals(0, scores.bilanParties().parties());
        assertTrue(scores.moyennesStatistiques().isEmpty());
        assertTrue(scores.dernierePartie().isEmpty());

        scores.enregistrerPartie(partieGagneePar("Alice"));
        scores.enregistrerPartie(partieGagneePar("Bob"));

        BilanParties bilan = scores.bilanParties();
        assertEquals(2, bilan.parties());
        assertEquals(1.0, bilan.lancersMoyens(), 1e-9);
        assertEquals("Bob", scores.dernierePartie().orElseThrow().vainqueur());

        List<MoyennesStatistiques> moyennes = scores.moyennesStatistiques();
        assertEquals(List.of("Tous les joueurs", "Humains", "Bots", "Vainqueurs"),
                moyennes.stream().map(MoyennesStatistiques::groupe).toList());
        MoyennesStatistiques tous = moyennes.get(0);
        assertEquals(8, tous.participations(), "4 joueurs dans chacune des 2 parties");
        assertEquals(0.25, tous.lancers(), 1e-9, "un seul lancer par partie, pour 4 joueurs");
        assertEquals(6.0, tous.moyenneDe(), 1e-9);
        MoyennesStatistiques vainqueurs = moyennes.get(3);
        assertEquals(2, vainqueurs.participations());
        assertEquals(4.0, vainqueurs.chevauxArrives(), 1e-9);
        assertEquals(4, moyennes.get(1).participations(), "Alice et Bob, deux fois");
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
