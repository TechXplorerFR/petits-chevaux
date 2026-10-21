package fr.utbm.petitschevaux;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Résumé d'une partie terminée, tel qu'enregistré dans l'historique.
 *
 * @param fin date et heure de fin de la partie
 * @param mode mode de jeu
 * @param vainqueur nom du gagnant
 * @param vainqueurBot vrai si le gagnant était un bot
 * @param duree temps de jeu
 * @param lancers nombre total de lancers de dé
 */
public record ResumePartie(LocalDateTime fin, ModeJeu mode, String vainqueur, boolean vainqueurBot,
        Duration duree, int lancers) {
}
