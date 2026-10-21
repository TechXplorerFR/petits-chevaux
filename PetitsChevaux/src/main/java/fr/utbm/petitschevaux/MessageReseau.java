package fr.utbm.petitschevaux;

import java.io.Serializable;
import java.util.List;

/**
 * Messages échangés entre l'hôte ({@link ServeurJeu}) et les invités ({@link ClientReseau}).
 *
 * <p>L'hôte est maître de la partie : les invités n'envoient que des intentions
 * ({@link Lancer}, {@link ChoisirCheval}, {@link Chat}) ; l'hôte les applique à son
 * {@link Jeu}, puis envoie à tous l'état complet de la partie ({@link EtatPartie}).</p>
 */
public sealed interface MessageReseau extends Serializable {

    /** Invité → hôte : demande à rejoindre le salon. */
    record Rejoindre(String nom, Couleur couleurSouhaitee) implements MessageReseau {
    }

    /** Hôte → invité : demande acceptée, avec la couleur attribuée. */
    record Bienvenue(Couleur couleur) implements MessageReseau {
    }

    /** Hôte → invité : demande refusée (salon complet, partie déjà commencée…). */
    record Refus(String raison) implements MessageReseau {
    }

    /** Hôte → invités : liste à jour des joueurs du salon. */
    record Salon(List<Place> places) implements MessageReseau {
    }

    /** Hôte → invités : la partie commence, avec son état initial ({@link Sauvegarde#versTexte}). */
    record DebutPartie(String etat) implements MessageReseau {
    }

    /** Hôte → invités : état de la partie après un coup. */
    record EtatPartie(String etat) implements MessageReseau {
    }

    /** Invité → hôte : le joueur veut lancer le dé. */
    record Lancer() implements MessageReseau {
    }

    /** Invité → hôte : le joueur choisit son cheval numéro {@code numero}. */
    record ChoisirCheval(int numero) implements MessageReseau {
    }

    /** Message de chat, dans les deux sens. */
    record Chat(Message message) implements MessageReseau {
    }

    /** Hôte → invités : la session est terminée (l'hôte a quitté). */
    record FinSession(String raison) implements MessageReseau {
    }
}
