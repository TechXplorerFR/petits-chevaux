package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SauvegardeTest {
    @TempDir
    Path dossier;

    @Test
    void unePartieRechargeeEstIdentique() throws IOException {
        Jeu jeu = new Jeu(List.of(new Joueur("Alice = é", Couleur.BLEU)), new DeTruque(6));
        jeu.getJoueurs().get(0).getChevaux().get(1).setPosition(30);
        jeu.getJoueurs().get(1).getChevaux().get(2).setPosition(Plateau.POSITION_ARRIVEE);
        jeu.getJoueurs().get(2).getChevaux().get(3).terminer();
        jeu.restaurerTour(2, 0, false);
        jeu.lancerDe(); // 6 : Alice doit choisir un cheval
        Path fichier = dossier.resolve("partie.pcx");

        Sauvegarde.sauvegarder(jeu, fichier);
        Jeu charge = Sauvegarde.charger(fichier);

        for (int i = 0; i < 4; i++) {
            Joueur attendu = jeu.getJoueurs().get(i);
            Joueur obtenu = charge.getJoueurs().get(i);
            assertEquals(attendu.getNom(), obtenu.getNom());
            assertEquals(attendu.getCouleur(), obtenu.getCouleur());
            assertEquals(attendu.isBot(), obtenu.isBot());
            for (int c = 0; c < Joueur.NOMBRE_CHEVAUX; c++) {
                assertEquals(attendu.getChevaux().get(c).getPosition(),
                        obtenu.getChevaux().get(c).getPosition());
            }
        }
        for (int i = 0; i < 4; i++) {
            assertArrayEquals(jeu.getJoueurs().get(i).getStatistiques().versTableau(),
                    charge.getJoueurs().get(i).getStatistiques().versTableau());
        }
        assertEquals(1, charge.getJoueurs().get(2).getStatistiques().getSix());
        assertTrue(charge.getDuree().compareTo(jeu.getDuree().minusSeconds(1)) >= 0,
                "le temps de jeu est conservé");
        assertEquals(2, charge.getJoueurCourant());
        assertTrue(charge.isAttenteChoix(), "le lancer en attente est conservé");
        assertEquals(6, charge.getDe().getValeur());
    }

    @Test
    void lAncienFormatResteLisible() throws IOException {
        StringBuilder contenu = new StringBuilder("joueurs=2\njoueurCourant=1\n");
        String[] couleurs = { "ROUGE", "BLEU" };
        for (int i = 0; i < 2; i++) {
            contenu.append("joueur.").append(i).append(".nom=J").append(i).append('\n')
                    .append("joueur.").append(i).append(".couleur=").append(couleurs[i]).append('\n');
            contenu.append("joueur.").append(i).append(".cheval.0=12;true;false\n")
                    .append("joueur.").append(i).append(".cheval.1=40;true;true\n")
                    .append("joueur.").append(i).append(".cheval.2=0;false;false\n")
                    .append("joueur.").append(i).append(".cheval.3=0;false;false\n");
        }
        Path fichier = dossier.resolve("ancien.pcx");
        Files.writeString(fichier, contenu, StandardCharsets.UTF_8);

        Jeu jeu = Sauvegarde.charger(fichier);

        Joueur bleu = jeu.getJoueurs().get(Couleur.BLEU.ordinal());
        assertEquals("J1", bleu.getNom());
        assertFalse(bleu.isBot());
        assertEquals(12, bleu.getChevaux().get(0).getPosition());
        assertTrue(bleu.getChevaux().get(1).isTermine());
        assertEquals(Couleur.BLEU.ordinal(), jeu.getJoueurCourant());
        assertTrue(jeu.peutLancer());
    }

    @Test
    void laSauvegardeAutomatiqueCreeLeDossierPuisMetAJourLeMemeFichier() throws IOException {
        Path sauvegardes = dossier.resolve("sauvegardes");
        Jeu jeu = new Jeu(List.of(new Joueur("Alice", Couleur.VERT)));

        Path premier = Sauvegarde.sauvegarderDansLeDossier(jeu, sauvegardes, null);
        jeu.getJoueurs().get(3).getChevaux().get(0).setPosition(12);
        Path deuxieme = Sauvegarde.sauvegarderDansLeDossier(jeu, sauvegardes, premier);

        assertEquals(sauvegardes, premier.getParent());
        assertTrue(premier.getFileName().toString().startsWith("partie_"));
        assertEquals(premier, deuxieme, "la même partie réutilise son fichier");
        assertEquals(List.of(premier), Sauvegarde.listerSauvegardes(sauvegardes));
        assertEquals(12, Sauvegarde.charger(premier).getJoueurs().get(3).getChevaux().get(0).getPosition());
    }

    @Test
    void deuxPartiesDifferentesOntDeuxFichiersListesDuPlusRecentAuPlusAncien() throws IOException {
        Path sauvegardes = dossier.resolve("sauvegardes");
        Path ancienne = Sauvegarde.sauvegarderDansLeDossier(new Jeu(List.of()), sauvegardes, null);
        Files.setLastModifiedTime(ancienne, FileTime.fromMillis(0));
        Path recente = Sauvegarde.sauvegarderDansLeDossier(new Jeu(List.of()), sauvegardes, null);
        Files.writeString(sauvegardes.resolve("notes.txt"), "pas une sauvegarde");

        assertNotEquals(ancienne, recente);
        assertEquals(List.of(recente, ancienne), Sauvegarde.listerSauvegardes(sauvegardes));
    }

    @Test
    void sansDossierIlNYAAucuneSauvegarde() throws IOException {
        assertTrue(Sauvegarde.listerSauvegardes(dossier.resolve("inexistant")).isEmpty());
    }

    @Test
    void laVersionTexteConserveLHistoriqueEtLeJournal() throws IOException {
        Jeu jeu = new Jeu(List.of(new Joueur("Alice", Couleur.JAUNE)), new DeTruque(3, 4, 2), ModeJeu.CASES_SPECIALES);
        jeu.lancerDe();
        jeu.lancerDe();
        jeu.lancerDe();
        jeu.restaurerJournaux(jeu.getHistorique(), List.of(new Jeu.Evenement(
                jeu.getJoueurs().get(1), CaseSpeciale.GEL, "Gel : message avec = et : et accents é")), false);

        Jeu copie = Sauvegarde.depuisTexte(Sauvegarde.versTexte(jeu));

        assertEquals(3, copie.getHistorique().size());
        assertEquals(2, copie.getHistorique().get(0).valeur());
        assertEquals(Couleur.BLEU, copie.getHistorique().get(0).joueur().getCouleur());
        assertEquals(jeu.getJournal().get(0).message(), copie.getJournal().get(0).message());
        assertEquals(CaseSpeciale.GEL, copie.getJournal().get(0).effet());
        assertEquals(jeu.getPlateau().getCasesSpeciales(), copie.getPlateau().getCasesSpeciales());
        assertEquals(jeu.getJoueurCourant(), copie.getJoueurCourant());
    }

    @Test
    void unFichierInvalideEstRefuse() throws IOException {
        Path fichier = dossier.resolve("abime.pcx");
        Files.writeString(fichier, "joueurs=deux\n");
        assertThrows(IOException.class, () -> Sauvegarde.charger(fichier));
    }
}
