package fr.utbm.petitschevaux;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void unFichierInvalideEstRefuse() throws IOException {
        Path fichier = dossier.resolve("abime.pcx");
        Files.writeString(fichier, "joueurs=deux\n");
        assertThrows(IOException.class, () -> Sauvegarde.charger(fichier));
    }
}
