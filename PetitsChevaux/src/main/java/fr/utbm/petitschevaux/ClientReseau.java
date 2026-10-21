package fr.utbm.petitschevaux;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * Connexion d'un invité à la partie hébergée par un autre joueur ({@link ServeurJeu}).
 *
 * <p>Les messages reçus sont remis à l'{@link Ecouteur} via l'{@link Executor} fourni
 * (le fil JavaFX dans le jeu).</p>
 */
public class ClientReseau {
    private static final int DELAI_CONNEXION_MS = 5_000;

    /** Événements de la connexion, toujours appelés via l'executor. */
    public interface Ecouteur {
        void messageRecu(MessageReseau message);

        /** La connexion a été coupée sans que l'invité l'ait demandé. */
        void connexionPerdue(String raison);
    }

    private final Executor executeur;
    private final Ecouteur ecouteur;
    private Connexion connexion;
    private volatile boolean fermeeVolontairement;

    public ClientReseau(Executor executeur, Ecouteur ecouteur) {
        this.executeur = executeur;
        this.ecouteur = ecouteur;
    }

    /**
     * Se connecte à l'hôte et demande à rejoindre son salon. La réponse
     * ({@code Bienvenue} ou {@code Refus}) arrive ensuite par l'écouteur.
     */
    public void connecter(String adresse, int port, String nom, Couleur couleurSouhaitee) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(adresse, port), DELAI_CONNEXION_MS);
            connexion = new Connexion(socket);
            connexion.envoyer(new MessageReseau.Rejoindre(nom, couleurSouhaitee));
        } catch (IOException e) {
            socket.close();
            throw e;
        }
        Thread lecture = new Thread(this::ecouter, "client-lecture");
        lecture.setDaemon(true);
        lecture.start();
    }

    /**
     * Envoie une action ou un message de chat à l'hôte.
     */
    public void envoyer(MessageReseau message) {
        try {
            connexion.envoyer(message);
        } catch (IOException e) {
            connexion.close(); // Le fil de lecture signalera la perte de connexion.
        }
    }

    public void deconnecter() {
        fermeeVolontairement = true;
        if (connexion != null) {
            connexion.close();
        }
    }

    private void ecouter() {
        try {
            while (true) {
                MessageReseau message = connexion.recevoir();
                executeur.execute(() -> ecouteur.messageRecu(message));
                if (message instanceof MessageReseau.Refus || message instanceof MessageReseau.FinSession) {
                    fermeeVolontairement = true;
                    connexion.close();
                    return;
                }
            }
        } catch (IOException e) {
            if (!fermeeVolontairement) {
                executeur.execute(() -> ecouteur.connexionPerdue("La connexion avec l'hôte a été perdue."));
            }
        }
    }
}
