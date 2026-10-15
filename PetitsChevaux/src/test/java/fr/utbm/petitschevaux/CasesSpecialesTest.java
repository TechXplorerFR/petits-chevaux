package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasesSpecialesTest {
    /** Le cheval jaune testé part de la position 10 et fait 3 : il s'arrête en position 13. */
    private static final int DEPART = 10;
    private static final int ARRIVEE = 13;

    @TempDir
    Path dossier;

    private Jeu jeu;
    private Joueur jaune;
    private Joueur rouge;
    private Joueur bleu;
    private Cheval cheval;

    /**
     * Partie en mode cases spéciales avec une seule case spéciale, là où le cheval jaune s'arrête.
     */
    private void preparer(CaseSpeciale effet, int... des) {
        jeu = new Jeu(List.of(new Joueur("Jaune", Couleur.JAUNE), new Joueur("Rouge", Couleur.ROUGE)),
                new DeTruque(des), ModeJeu.CASES_SPECIALES);
        jaune = jeu.getJoueurs().get(0);
        rouge = jeu.getJoueurs().get(1);
        bleu = jeu.getJoueurs().get(2);
        cheval = jaune.getChevaux().get(0);
        cheval.setPosition(DEPART);
        jeu.getPlateau().viderCasesSpeciales();
        jeu.getPlateau().placerCaseSpeciale(absoluePourJaune(ARRIVEE), effet);
    }

    private static int absoluePourJaune(int positionRelative) {
        return Plateau.decalageDepart(Couleur.JAUNE) + positionRelative - 1;
    }

    private void jouerLeChevalJaune() {
        jeu.lancerDe();
        assertTrue(jeu.jouerCheval(cheval));
    }

    @Test
    void lesCasesSpecialesSontPlaceesHorsDesDepartsEtDesPiedsDEscalier() {
        Plateau plateau = new Plateau();
        plateau.placerCasesSpeciales(new Random(7));

        assertEquals(Plateau.NOMBRE_CASES_SPECIALES, plateau.getCasesSpeciales().size());
        assertEquals(EnumSet.allOf(CaseSpeciale.class), Set.copyOf(plateau.getCasesSpeciales().values()),
                "chaque effet est présent au moins une fois");
        for (Couleur couleur : Couleur.values()) {
            int depart = Plateau.decalageDepart(couleur);
            assertFalse(plateau.getCasesSpeciales().containsKey(depart));
            assertFalse(plateau.getCasesSpeciales().containsKey(depart - 1));
        }
    }

    @Test
    void enModeClassiqueIlNYAPasDeCaseSpeciale() {
        Jeu classique = new Jeu(List.of(new Joueur("A", Couleur.JAUNE)));
        assertTrue(classique.getPlateau().getCasesSpeciales().isEmpty());
    }

    @Test
    void rejouerGardeLaMainMemeSansSix() {
        preparer(CaseSpeciale.REJOUER, 3);
        jouerLeChevalJaune();

        assertSame(jaune, jeu.getJoueurActif());
        assertTrue(jeu.peutLancer());
        assertEquals(1, jaune.getStatistiques().getCasesSpeciales());
        assertEquals(CaseSpeciale.REJOUER, jeu.getJournal().get(0).effet());
    }

    @Test
    void turboAvanceDeTroisCasesDePlus() {
        preparer(CaseSpeciale.TURBO, 3);
        jouerLeChevalJaune();

        assertEquals(ARRIVEE + EffetsSpeciaux.CASES_TURBO, cheval.getPosition());
        assertSame(rouge, jeu.getJoueurActif());
    }

    @Test
    void crocEnJambeFaitReculerLeChevalAdverseLePlusAvance() {
        preparer(CaseSpeciale.CROC_EN_JAMBE, 3);
        Cheval rougeAvance = rouge.getChevaux().get(0);
        rougeAvance.setPosition(20);
        Cheval bleuEnRetard = bleu.getChevaux().get(0);
        bleuEnRetard.setPosition(5);

        jouerLeChevalJaune();

        assertEquals(20 - EffetsSpeciaux.CASES_CROC_EN_JAMBE, rougeAvance.getPosition());
        assertEquals(5, bleuEnRetard.getPosition());
    }

    @Test
    void deMauditRetireDeuxPointsAuProchainLancerDesAdversaires() {
        preparer(CaseSpeciale.DE_MAUDIT, 3, 5);
        jouerLeChevalJaune();

        assertEquals(EffetsSpeciaux.MALUS_DE, jeu.getMalusDe(rouge));
        assertEquals(0, jeu.getMalusDe(jaune), "le joueur qui déclenche n'est pas puni");
        assertEquals(3, jeu.lancerDe(), "5 - 2 = 3");
        assertEquals(3, jeu.getDe().getValeur());
        assertEquals(0, jeu.getMalusDe(rouge), "le malus ne sert qu'une fois");
    }

    @Test
    void gelFaitPasserSonTourAuJoueurSuivant() {
        preparer(CaseSpeciale.GEL, 3);
        jouerLeChevalJaune();

        assertSame(bleu, jeu.getJoueurActif(), "Rouge est gelé, Bleu joue");
        assertEquals(0, jeu.getToursGeles(rouge));
        assertEquals(CaseSpeciale.GEL, jeu.getJournal().get(0).effet());
    }

    @Test
    void coupDuSortRenvoieUnChevalAuHasardALEcurie() {
        preparer(CaseSpeciale.COUP_DU_SORT, 3);
        Cheval victime = rouge.getChevaux().get(0);
        victime.setPosition(30);
        jeu.setHasard(new Random() {
            @Override
            public int nextInt(int borne) {
                return borne - 1; // le dernier candidat : le cheval rouge
            }
        });

        jouerLeChevalJaune();

        assertEquals(Cheval.ECURIE, victime.getPosition());
        assertEquals(ARRIVEE, cheval.getPosition());
        assertEquals(1, rouge.getStatistiques().getChevauxPerdus());
    }

    @Test
    void tremblementFaitReculerTousLesChevauxDeLaPiste() {
        preparer(CaseSpeciale.TREMBLEMENT, 3);
        Cheval rougeSurPiste = rouge.getChevaux().get(0);
        rougeSurPiste.setPosition(10);
        Cheval rougeDansEscalier = rouge.getChevaux().get(1);
        rougeDansEscalier.setPosition(Plateau.POSITION_BAS_ESCALIER + 2);

        jouerLeChevalJaune();

        assertEquals(ARRIVEE - 2, cheval.getPosition());
        assertEquals(8, rougeSurPiste.getPosition());
        assertEquals(Plateau.POSITION_BAS_ESCALIER + 2, rougeDansEscalier.getPosition(),
                "l'escalier n'est pas touché");
    }

    @Test
    void ventArriereFaitAvancerSansEntrerDansLEscalier() {
        preparer(CaseSpeciale.VENT_ARRIERE, 3);
        Cheval bleuPresDeLArrivee = bleu.getChevaux().get(0);
        bleuPresDeLArrivee.setPosition(Plateau.POSITION_BAS_ESCALIER - 1);

        jouerLeChevalJaune();

        assertEquals(ARRIVEE + 2, cheval.getPosition());
        assertEquals(Plateau.POSITION_BAS_ESCALIER, bleuPresDeLArrivee.getPosition());
    }

    @Test
    void unDeplacementForceCaptureLAdversaireSurLaCaseDArrivee() {
        preparer(CaseSpeciale.TURBO, 3);
        Cheval victime = rouge.getChevaux().get(0);
        // Case absolue où le turbo amène le cheval jaune, exprimée pour Rouge.
        int absolue = absoluePourJaune(ARRIVEE + EffetsSpeciaux.CASES_TURBO);
        victime.setPosition(Math.floorMod(absolue - Plateau.decalageDepart(Couleur.ROUGE), Plateau.NOMBRE_CASES) + 1);

        jouerLeChevalJaune();

        assertEquals(Cheval.ECURIE, victime.getPosition());
        assertEquals(1, jaune.getStatistiques().getCaptures());
    }

    @Test
    void unTurboQuiArriveSurUneAutreCaseLaDeclencheAussi() {
        preparer(CaseSpeciale.TURBO, 3);
        jeu.getPlateau().placerCaseSpeciale(absoluePourJaune(ARRIVEE + EffetsSpeciaux.CASES_TURBO),
                CaseSpeciale.GEL);

        jouerLeChevalJaune();

        assertSame(bleu, jeu.getJoueurActif(), "le gel déclenché en chaîne fait sauter Rouge");
        assertEquals(2, jaune.getStatistiques().getCasesSpeciales());
        // Journal, du plus récent au plus ancien : Rouge passe son tour, le gel en chaîne, le turbo.
        Jeu.Evenement gelEnChaine = jeu.getJournal().get(1);
        assertEquals(CaseSpeciale.GEL, gelEnChaine.effet());
        assertTrue(gelEnChaine.message().startsWith("Réaction en chaîne"));
        assertEquals(CaseSpeciale.TURBO, jeu.getJournal().get(2).effet());
    }

    @Test
    void unChevalAdverseDeplaceDeclencheLaCaseAuNomDeSonProprietaire() {
        preparer(CaseSpeciale.CROC_EN_JAMBE, 3);
        Cheval chevalRouge = rouge.getChevaux().get(0);
        chevalRouge.setPosition(20);
        // Le croc-en-jambe ramène le cheval rouge en position 16, soit la case absolue 37.
        jeu.getPlateau().placerCaseSpeciale(Plateau.decalageDepart(Couleur.ROUGE) + 15, CaseSpeciale.DE_MAUDIT);

        jouerLeChevalJaune();

        assertEquals(16, chevalRouge.getPosition());
        assertEquals(1, rouge.getStatistiques().getCasesSpeciales());
        assertEquals(0, jeu.getMalusDe(rouge), "Rouge a déclenché le dé maudit : il n'est pas puni");
        assertEquals(EffetsSpeciaux.MALUS_DE, jeu.getMalusDe(jaune));
        assertEquals(EffetsSpeciaux.MALUS_DE, jeu.getMalusDe(bleu));
    }

    @Test
    void rejouerDeclencheParLeChevalDUnAutreJoueurNaPasDEffet() {
        preparer(CaseSpeciale.CROC_EN_JAMBE, 3);
        rouge.getChevaux().get(0).setPosition(20);
        jeu.getPlateau().placerCaseSpeciale(Plateau.decalageDepart(Couleur.ROUGE) + 15, CaseSpeciale.REJOUER);

        jouerLeChevalJaune();

        assertSame(rouge, jeu.getJoueurActif(), "Jaune ne rejoue pas, la main passe normalement");
        assertTrue(jeu.getJournal().get(0).message().contains("aucun effet"));
    }

    @Test
    void uneReactionEnChaineNeTournePasEnBoucle() {
        preparer(CaseSpeciale.VENT_ARRIERE, 3);
        cheval.setPosition(20);
        jeu.getPlateau().viderCasesSpeciales();
        // Vent arrière en 23 → avance en 25 (Tremblement) → recule en 23 (déjà déclenchée) : fin.
        jeu.getPlateau().placerCaseSpeciale(absoluePourJaune(23), CaseSpeciale.VENT_ARRIERE);
        jeu.getPlateau().placerCaseSpeciale(absoluePourJaune(25), CaseSpeciale.TREMBLEMENT);

        jouerLeChevalJaune();

        assertEquals(23, cheval.getPosition());
        assertEquals(2, jaune.getStatistiques().getCasesSpeciales());
        assertEquals(2, jeu.getJournal().size());
    }

    @Test
    void laSauvegardeConserveLeModeLesCasesEtLesEffetsEnAttente() throws IOException {
        preparer(CaseSpeciale.GEL, 3);
        jeu.getPlateau().placerCaseSpeciale(30, CaseSpeciale.VENT_ARRIERE);
        jeu.restaurerEffets(bleu, 2, 1);
        Path fichier = dossier.resolve("special.pcx");

        Sauvegarde.sauvegarder(jeu, fichier);
        Jeu charge = Sauvegarde.charger(fichier);

        assertEquals(ModeJeu.CASES_SPECIALES, charge.getMode());
        assertEquals(jeu.getPlateau().getCasesSpeciales(), charge.getPlateau().getCasesSpeciales());
        Joueur bleuCharge = charge.getJoueurs().get(2);
        assertEquals(2, charge.getMalusDe(bleuCharge));
        assertEquals(1, charge.getToursGeles(bleuCharge));
        assertNotEquals(0, charge.getPlateau().getCasesSpeciales().size());
    }
}
