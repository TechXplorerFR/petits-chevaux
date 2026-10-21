package fr.utbm.petitschevaux;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Canal d'échange de {@link MessageReseau} sur une socket, commun à l'hôte et aux invités.
 *
 * <p>Les objets reçus passent par un filtre : seules les classes du jeu et quelques classes
 * de base de Java peuvent être recréées. Sans ce filtre, n'importe qui sur le réseau pourrait
 * faire instancier des classes arbitraires (faille classique de la désérialisation Java).</p>
 */
final class Connexion implements Closeable {
    private static final ObjectInputFilter FILTRE = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxrefs=2000;maxarray=10000;"
                    + "fr.utbm.petitschevaux.*;java.lang.*;java.util.*;java.time.*;!*");

    private final Socket socket;
    private final ObjectOutputStream sortie;
    private final ObjectInputStream entree;

    Connexion(Socket socket) throws IOException {
        this.socket = socket;
        // La sortie d'abord : chaque côté envoie son en-tête avant de lire celui de l'autre.
        this.sortie = new ObjectOutputStream(socket.getOutputStream());
        sortie.flush();
        this.entree = new ObjectInputStream(socket.getInputStream());
        entree.setObjectInputFilter(FILTRE);
    }

    Socket getSocket() {
        return socket;
    }

    synchronized void envoyer(MessageReseau message) throws IOException {
        // Oublie les objets déjà envoyés : chaque message est relu en entier de l'autre côté.
        // Le marqueur de remise à zéro est écrit AVANT le message pour être lu avec lui :
        // écrit après, il resterait en attente chez le destinataire, et fermer une socket
        // contenant des données non lues la coupe brutalement (le dernier message serait perdu).
        sortie.reset();
        sortie.writeObject(message);
        sortie.flush();
    }

    /**
     * Attend le prochain message.
     *
     * @throws IOException si la connexion est coupée ou si l'objet reçu est refusé
     */
    MessageReseau recevoir() throws IOException {
        try {
            Object objet = entree.readObject();
            if (objet instanceof MessageReseau message) {
                return message;
            }
            throw new IOException("Message inattendu : " + objet);
        } catch (ClassNotFoundException e) {
            throw new IOException("Message illisible", e);
        }
    }

    @Override
    public void close() {
        try {
            // Fin d'envoi propre d'abord : sinon le dernier message (par exemple FinSession)
            // peut être perdu si la socket est fermée juste après l'envoi.
            if (!socket.isClosed() && !socket.isOutputShutdown()) {
                socket.shutdownOutput();
            }
        } catch (IOException ignoree) {
            // Connexion déjà coupée par l'autre côté.
        }
        try {
            socket.close();
        } catch (IOException ignoree) {
            // La connexion est déjà fermée : rien à faire.
        }
    }
}
