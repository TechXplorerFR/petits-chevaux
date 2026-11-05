package fr.utbm.petitschevaux.model;

import java.time.Duration;

/**
 * Moyennes sur l'ensemble des parties terminées.
 *
 * @param parties nombre de parties terminées
 * @param dureeMoyenne durée moyenne d'une partie
 * @param lancersMoyens nombre moyen de lancers de dé par partie (tous joueurs confondus)
 */
public record BilanParties(int parties, Duration dureeMoyenne, double lancersMoyens) {
}
