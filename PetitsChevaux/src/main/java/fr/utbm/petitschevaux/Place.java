package fr.utbm.petitschevaux;

import java.io.Serializable;

/**
 * Un joueur humain présent dans le salon d'une partie en ligne.
 *
 * @param nom nom du joueur
 * @param couleur couleur attribuée par l'hôte
 * @param hote vrai pour le joueur qui héberge la partie
 */
public record Place(String nom, Couleur couleur, boolean hote) implements Serializable {
}
