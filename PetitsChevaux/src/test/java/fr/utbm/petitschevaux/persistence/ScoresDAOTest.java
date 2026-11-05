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
    private String url;
    private ScoresDAO scores;

    @BeforeEach
    void creerBaseEnMemoire() {
        // Une base en mémoire différente par test, conservée tant que le test tourne.
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        scores = new ScoresDAO(new BaseDeDonnees(url));
    }

    /** Partie classique à deux humains (Alice jaune, Bob rouge) que {@code gagnant} vient de gagner. */
    private static Jeu partieGagneePar(String gagnant) {
        return partieGagneePar(gagnant, ModeJeu.CLASSIQUE);
    }

    private static Jeu partieGagneePar(String gagnant, ModeJeu mode) {
        Jeu jeu = new Jeu(List.of(new Joueur("Alice", Couleur.JAUNE), new Joueur("Bob", Couleur.ROUGE)),
                new DeTruque(6), mode);
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
        assertEquals(1, scores.dernieresParties(ModeJeu.CLASSIQUE, 10).size());
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

        List<ResumePartie> parties = scores.dernieresParties(ModeJeu.CLASSIQUE, 10);
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

        int id = scores.dernieresParties(ModeJeu.CLASSIQUE, 1).get(0).id();
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
        assertEquals(0, scores.bilanParties(ModeJeu.CLASSIQUE).parties());
        assertTrue(scores.moyennesStatistiques(ModeJeu.CLASSIQUE).isEmpty());
        assertTrue(scores.dernierePartie(ModeJeu.CLASSIQUE).isEmpty());

        scores.enregistrerPartie(partieGagneePar("Alice"));
        scores.enregistrerPartie(partieGagneePar("Bob"));

        BilanParties bilan = scores.bilanParties(ModeJeu.CLASSIQUE);
        assertEquals(2, bilan.parties());
        assertEquals(1.0, bilan.lancersMoyens(), 1e-9);
        assertEquals("Bob", scores.dernierePartie(ModeJeu.CLASSIQUE).orElseThrow().vainqueur());

        List<MoyennesStatistiques> moyennes = scores.moyennesStatistiques(ModeJeu.CLASSIQUE);
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
    void chaqueModeASesPropresStatistiques() throws SQLException {
        scores.enregistrerPartie(partieGagneePar("Alice", ModeJeu.CLASSIQUE));
        scores.enregistrerPartie(partieGagneePar("Bob", ModeJeu.CASES_SPECIALES));
        scores.enregistrerPartie(partieGagneePar("Bob", ModeJeu.CASES_SPECIALES));

        assertEquals(1, scores.bilanParties(ModeJeu.CLASSIQUE).parties());
        assertEquals(2, scores.bilanParties(ModeJeu.CASES_SPECIALES).parties());
        assertEquals("Alice", scores.dernierePartie(ModeJeu.CLASSIQUE).orElseThrow().vainqueur());
        assertEquals(2, scores.dernieresParties(ModeJeu.CASES_SPECIALES, 10).size());
        assertEquals(4, scores.moyennesStatistiques(ModeJeu.CLASSIQUE).get(0).participations());
        assertEquals(8, scores.moyennesStatistiques(ModeJeu.CASES_SPECIALES).get(0).participations());
    }

    @Test
    void lHistoriqueGardeDixPartiesMaisLesMoyennesLesComptentToutes() throws SQLException {
        int total = ScoresDAO.PARTIES_CONSERVEES + 3;
        for (int i = 0; i < total; i++) {
            scores.enregistrerPartie(partieGagneePar(i % 2 == 0 ? "Alice" : "Bob"));
        }
        scores.enregistrerPartie(partieGagneePar("Alice", ModeJeu.CASES_SPECIALES));

        List<ResumePartie> historique = scores.dernieresParties(ModeJeu.CLASSIQUE, 100);
        assertEquals(ScoresDAO.PARTIES_CONSERVEES, historique.size(), "les plus anciennes sont effacées");
        assertEquals(1, scores.dernieresParties(ModeJeu.CASES_SPECIALES, 100).size(),
                "l'autre mode n'est pas touché");
        try (var connexion = new BaseDeDonnees(url).ouvrir();
                var requete = connexion.createStatement();
                var ligne = requete.executeQuery("SELECT COUNT(*) FROM statistiques_joueur")) {
            ligne.next();
            assertEquals(4 * (ScoresDAO.PARTIES_CONSERVEES + 1), ligne.getInt(1),
                    "les statistiques détaillées partent avec leur partie");
        }

        assertEquals(total, scores.bilanParties(ModeJeu.CLASSIQUE).parties());
        MoyennesStatistiques vainqueurs = scores.moyennesStatistiques(ModeJeu.CLASSIQUE).get(3);
        assertEquals(total, vainqueurs.participations(), "toutes les parties depuis le début");
        assertEquals(total + 1, scores.profil("Alice").parties(),
                "le classement (tous modes) non plus n'oublie rien");
    }

    @Test
    void uneAncienneBaseRetrouveSesTotauxAPartirDeSesParties() throws SQLException {
        scores.enregistrerPartie(partieGagneePar("Alice"));
        scores.enregistrerPartie(partieGagneePar("Bob"));
        // Base créée avant les totaux : ils sont vides alors que des parties existent.
        try (var connexion = new BaseDeDonnees(url).ouvrir();
                var requete = connexion.createStatement()) {
            requete.execute("DELETE FROM cumul_parties");
            requete.execute("DELETE FROM cumul_joueurs");
        }

        ScoresDAO relance = new ScoresDAO(new BaseDeDonnees(url));

        assertEquals(2, relance.bilanParties(ModeJeu.CLASSIQUE).parties());
        List<MoyennesStatistiques> moyennes = relance.moyennesStatistiques(ModeJeu.CLASSIQUE);
        assertEquals(8, moyennes.get(0).participations());
        assertEquals(2, moyennes.get(3).participations());
        assertEquals(6.0, moyennes.get(0).moyenneDe(), 1e-9);
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
