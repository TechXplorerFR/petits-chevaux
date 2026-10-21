package fr.utbm.petitschevaux;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Partie en ligne vue depuis un joueur : soit l'hôte (avec un {@link ServeurJeu}),
 * soit un invité (avec un {@link ClientReseau}).
 *
 * <p>Elle survit aux changements d'écran (salon → partie → victoire) et garde le chat.
 * Chaque écran s'abonne aux événements qui le concernent avec les méthodes {@code sur…} ;
 * les événements arrivent sur le fil de l'executor fourni (le fil JavaFX dans le jeu).</p>
 */
public final class SessionEnLigne implements ServeurJeu.Ecouteur, ClientReseau.Ecouteur {
    private final ServeurJeu serveur;
    private final ClientReseau client;
    private final String nomLocal;
    private final ModeJeu mode;
    private Couleur couleurLocale;
    private List<Place> places = List.of();
    private final List<Message> messages = new ArrayList<>();
    private boolean fermee;

    private Consumer<List<Place>> surSalon = places -> { };
    private Consumer<Message> surChat = message -> { };
    private BiConsumer<Place, MessageReseau> surAction = (place, message) -> { };
    private Consumer<Place> surDepart = place -> { };
    private Consumer<String> surDebutPartie = etat -> { };
    private Consumer<String> surEtat = etat -> { };
    private Consumer<String> surFin = raison -> { };

    private SessionEnLigne(Executor executeur, boolean hote, String nomLocal, Couleur couleur, ModeJeu mode) {
        this.nomLocal = nomLocal;
        this.couleurLocale = couleur;
        this.mode = mode;
        this.serveur = hote ? new ServeurJeu(executeur, this) : null;
        this.client = hote ? null : new ClientReseau(executeur, this);
    }

    /**
     * Héberge une partie : ouvre le serveur sur le port donné.
     *
     * @param mode règles de la partie (classique ou cases spéciales)
     */
    public static SessionEnLigne heberger(Executor executeur, int port, String nom, Couleur couleur,
            ModeJeu mode) throws IOException {
        SessionEnLigne session = new SessionEnLigne(executeur, true, nom, couleur, mode);
        session.serveur.demarrer(port, new Place(nom, couleur, true));
        session.places = session.serveur.getPlaces();
        return session;
    }

    /**
     * Rejoint la partie hébergée à l'adresse donnée. La couleur souhaitée est attribuée si elle
     * est libre, sinon l'hôte en choisit une autre.
     */
    public static SessionEnLigne rejoindre(Executor executeur, String adresse, int port, String nom,
            Couleur couleurSouhaitee) throws IOException {
        SessionEnLigne session = new SessionEnLigne(executeur, false, nom, couleurSouhaitee, null);
        session.client.connecter(adresse, port, nom, couleurSouhaitee);
        return session;
    }

    public boolean estHote() {
        return serveur != null;
    }

    public String getNomLocal() {
        return nomLocal;
    }

    /** @return la couleur jouée sur cet ordinateur (attribuée par l'hôte pour un invité) */
    public Couleur getCouleurLocale() {
        return couleurLocale;
    }

    /** @return les règles choisies par l'hôte ({@code null} côté invité : elles arrivent avec la partie) */
    public ModeJeu getMode() {
        return mode;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /** @return le port d'écoute de l'hôte */
    public int getPort() {
        return serveur.getPort();
    }

    /**
     * @return les adresses IPv4 de cet ordinateur sur le réseau local, à donner aux invités
     */
    public static List<String> adressesLocales() {
        List<String> adresses = new ArrayList<>();
        try {
            for (NetworkInterface carte : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!carte.isUp() || carte.isLoopback() || carte.isVirtual()) {
                    continue;
                }
                for (InetAddress adresse : Collections.list(carte.getInetAddresses())) {
                    if (adresse instanceof Inet4Address) {
                        adresses.add(adresse.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // Pas d'accès aux cartes réseau : on propose seulement l'ordinateur lui-même.
        }
        if (adresses.isEmpty()) {
            adresses.add("localhost");
        }
        return adresses;
    }

    // --- Abonnements des écrans ---

    public void surSalon(Consumer<List<Place>> action) {
        surSalon = action;
    }

    public void surChat(Consumer<Message> action) {
        surChat = action;
    }

    /** Hôte : action d'un invité ({@code Lancer} ou {@code ChoisirCheval}). */
    public void surAction(BiConsumer<Place, MessageReseau> action) {
        surAction = action;
    }

    /** Hôte : un invité est parti. */
    public void surDepart(Consumer<Place> action) {
        surDepart = action;
    }

    /** Invité : la partie commence (état initial de la partie). */
    public void surDebutPartie(Consumer<String> action) {
        surDebutPartie = action;
    }

    /** Invité : nouvel état de la partie. */
    public void surEtat(Consumer<String> action) {
        surEtat = action;
    }

    /** Invité : session terminée (refus, hôte parti, connexion perdue). */
    public void surFin(Consumer<String> action) {
        surFin = action;
    }

    // --- Envois ---

    /**
     * Envoie un message de chat à tous les joueurs.
     */
    public void envoyerChat(String texte) {
        String propre = texte == null ? "" : texte.strip();
        if (propre.isEmpty() || fermee) {
            return;
        }
        if (propre.length() > Message.LONGUEUR_MAX) {
            propre = propre.substring(0, Message.LONGUEUR_MAX);
        }
        Message message = new Message(nomLocal, couleurLocale, propre, LocalTime.now());
        if (estHote()) {
            serveur.diffuser(new MessageReseau.Chat(message));
            recevoirChat(message);
        } else {
            // L'hôte renvoie le message à tout le monde, nous compris.
            client.envoyer(new MessageReseau.Chat(message));
        }
    }

    /**
     * Hôte : message de la partie (tirage, départ d'un joueur…), envoyé à tous.
     */
    public void messagePartie(String texte) {
        Message message = Message.systeme(texte);
        serveur.diffuser(new MessageReseau.Chat(message));
        recevoirChat(message);
    }

    /**
     * Invité : envoie une action à l'hôte.
     */
    public void envoyerAction(MessageReseau action) {
        client.envoyer(action);
    }

    /**
     * Hôte : ferme le salon et envoie la partie aux invités.
     */
    public void demarrerPartie(Jeu jeu) {
        serveur.fermerSalon();
        serveur.diffuser(new MessageReseau.DebutPartie(Sauvegarde.versTexte(jeu)));
    }

    /**
     * Hôte : envoie l'état de la partie aux invités.
     */
    public void diffuserEtat(Jeu jeu) {
        serveur.diffuser(new MessageReseau.EtatPartie(Sauvegarde.versTexte(jeu)));
    }

    /**
     * Quitte la session : l'hôte ferme le serveur (les invités sont prévenus),
     * un invité se déconnecte (l'hôte le remplace par un bot).
     */
    public void fermer() {
        if (fermee) {
            return;
        }
        fermee = true;
        if (estHote()) {
            serveur.arreter("L'hôte a quitté la partie.");
        } else {
            client.deconnecter();
        }
    }

    private void recevoirChat(Message message) {
        messages.add(message);
        surChat.accept(message);
    }

    // --- Événements du serveur (hôte) ---

    @Override
    public void salonModifie(List<Place> places) {
        this.places = places;
        surSalon.accept(places);
    }

    @Override
    public void messageRecu(Place auteur, MessageReseau message) {
        if (message instanceof MessageReseau.Chat chat) {
            recevoirChat(chat.message());
        } else {
            surAction.accept(auteur, message);
        }
    }

    @Override
    public void depart(Place place) {
        surDepart.accept(place);
    }

    // --- Événements du client (invité) ---

    @Override
    public void messageRecu(MessageReseau message) {
        // Les autres messages (Lancer, ChoisirCheval…) sont destinés à l'hôte : ignorés.
        if (message instanceof MessageReseau.Bienvenue bienvenue) {
            couleurLocale = bienvenue.couleur();
        } else if (message instanceof MessageReseau.Salon salon) {
            places = salon.places();
            surSalon.accept(places);
        } else if (message instanceof MessageReseau.Chat chat) {
            recevoirChat(chat.message());
        } else if (message instanceof MessageReseau.DebutPartie debut) {
            surDebutPartie.accept(debut.etat());
        } else if (message instanceof MessageReseau.EtatPartie etat) {
            surEtat.accept(etat.etat());
        } else if (message instanceof MessageReseau.Refus refus) {
            terminer(refus.raison());
        } else if (message instanceof MessageReseau.FinSession fin) {
            terminer(fin.raison());
        }
    }

    @Override
    public void connexionPerdue(String raison) {
        terminer(raison);
    }

    private void terminer(String raison) {
        if (!fermee) {
            fermee = true;
            surFin.accept(raison);
        }
    }
}
