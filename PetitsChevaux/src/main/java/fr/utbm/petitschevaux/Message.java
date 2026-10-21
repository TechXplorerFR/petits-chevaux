package fr.utbm.petitschevaux;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Message du chat d'une partie en ligne.
 *
 * @param auteur nom de l'auteur
 * @param couleur couleur de l'auteur, ou {@code null} pour un message de la partie elle-même
 * @param texte contenu du message
 * @param heure heure d'envoi
 */
public record Message(String auteur, Couleur couleur, String texte, LocalTime heure) implements Serializable {
    /** Longueur maximale d'un message. */
    public static final int LONGUEUR_MAX = 300;
    /** Auteur des messages envoyés par la partie (tirage, départs…). */
    public static final String AUTEUR_PARTIE = "Partie";

    /**
     * @return un message de la partie, sans couleur
     */
    public static Message systeme(String texte) {
        return new Message(AUTEUR_PARTIE, null, texte, LocalTime.now());
    }

    /**
     * @return vrai pour un message envoyé par la partie plutôt que par un joueur
     */
    public boolean estSysteme() {
        return couleur == null;
    }
}
