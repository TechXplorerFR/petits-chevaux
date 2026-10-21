package fr.utbm.petitschevaux;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Serveur lancé par le joueur qui héberge une partie en ligne.
 *
 * <p>Il accueille jusqu'à trois invités, leur attribue une couleur, relaie le chat et
 * transmet leurs actions à l'hôte. Il ne connaît pas les règles : c'est l'hôte qui applique
 * les actions à son {@link Jeu} et renvoie l'état de la partie avec {@link #diffuser}.</p>
 *
 * <p>Chaque invité est lu par son propre fil d'exécution ; les événements sont remis à
 * l'{@link Ecouteur} via l'{@link Executor} fourni (le fil JavaFX dans le jeu).</p>
 */
public class ServeurJeu {
    public static final int PORT_PAR_DEFAUT = 5555;
    private static final int DELAI_PRESENTATION_MS = 10_000;
    private static final int LONGUEUR_MAX_NOM = 20;

    /** Événements du serveur, toujours appelés via l'executor. */
    public interface Ecouteur {
        /** La liste des joueurs du salon a changé. */
        void salonModifie(List<Place> places);

        /** Un invité a envoyé une action ({@code Lancer}, {@code ChoisirCheval}) ou un message de chat. */
        void messageRecu(Place auteur, MessageReseau message);

        /** Un invité s'est déconnecté. */
        void depart(Place place);
    }

    private record Invite(Place place, Connexion connexion) {
    }

    private final Executor executeur;
    private final Ecouteur ecouteur;
    private final Map<Couleur, Invite> invites = new ConcurrentHashMap<>();
    private ServerSocket serveur;
    private Place hote;
    private volatile boolean salonOuvert = true;
    private volatile boolean arrete;

    public ServeurJeu(Executor executeur, Ecouteur ecouteur) {
        this.executeur = executeur;
        this.ecouteur = ecouteur;
    }

    /**
     * Ouvre le serveur et commence à accepter les invités.
     *
     * @param port port d'écoute (0 pour laisser le système en choisir un, utile dans les tests)
     * @param hote place du joueur qui héberge (sa couleur ne sera pas attribuée aux invités)
     */
    public void demarrer(int port, Place hote) throws IOException {
        this.hote = hote;
        this.serveur = new ServerSocket(port);
        Thread accueil = new Thread(this::accepterInvites, "serveur-accueil");
        accueil.setDaemon(true);
        accueil.start();
    }

    public int getPort() {
        return serveur.getLocalPort();
    }

    /**
     * @return l'hôte puis les invités, dans l'ordre des couleurs
     */
    public synchronized List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();
        places.add(hote);
        invites.values().stream()
                .map(Invite::place)
                .sorted(Comparator.comparing(Place::couleur))
                .forEach(places::add);
        return List.copyOf(places);
    }

    /**
     * Ferme le salon : la partie commence, plus personne ne peut entrer.
     */
    public void fermerSalon() {
        salonOuvert = false;
    }

    /**
     * Envoie un message à tous les invités. Un invité injoignable est déconnecté.
     */
    public void diffuser(MessageReseau message) {
        for (Invite invite : invites.values()) {
            try {
                invite.connexion().envoyer(message);
            } catch (IOException e) {
                invite.connexion().close();
            }
        }
    }

    /**
     * Prévient les invités puis ferme toutes les connexions.
     */
    public void arreter(String raison) {
        arrete = true;
        diffuser(new MessageReseau.FinSession(raison));
        invites.values().forEach(invite -> invite.connexion().close());
        invites.clear();
        try {
            if (serveur != null) {
                serveur.close();
            }
        } catch (IOException ignoree) {
            // Déjà fermé.
        }
    }

    private void accepterInvites() {
        while (!arrete) {
            try {
                Socket socket = serveur.accept();
                Thread lecture = new Thread(() -> accueillir(socket), "serveur-invite");
                lecture.setDaemon(true);
                lecture.start();
            } catch (IOException e) {
                return; // Serveur fermé.
            }
        }
    }

    private void accueillir(Socket socket) {
        Invite invite;
        try {
            Connexion connexion = new Connexion(socket);
            socket.setSoTimeout(DELAI_PRESENTATION_MS);
            if (!(connexion.recevoir() instanceof MessageReseau.Rejoindre demande)) {
                connexion.close();
                return;
            }
            socket.setSoTimeout(0);
            invite = inscrire(connexion, demande);
            if (invite == null) {
                return;
            }
            connexion.envoyer(new MessageReseau.Bienvenue(invite.place().couleur()));
        } catch (IOException e) {
            // Délai de présentation dépassé, ou connexion qui n'est pas celle d'un joueur.
            fermer(socket);
            return;
        }
        annoncerSalon();
        ecouter(invite);
    }

    /**
     * Attribue une place à l'invité, ou lui envoie un refus.
     *
     * @return l'invité inscrit, ou {@code null} s'il a été refusé
     */
    private synchronized Invite inscrire(Connexion connexion, MessageReseau.Rejoindre demande) throws IOException {
        String refus = !salonOuvert ? "La partie a déjà commencé."
                : invites.size() >= Jeu.MAX_JOUEURS - 1 ? "La partie est complète." : null;
        Couleur couleur = refus == null ? couleurLibre(demande.couleurSouhaitee()) : null;
        if (refus != null || couleur == null) {
            connexion.envoyer(new MessageReseau.Refus(refus != null ? refus : "La partie est complète."));
            connexion.close();
            return null;
        }
        Invite invite = new Invite(new Place(nomValide(demande.nom()), couleur, false), connexion);
        invites.put(couleur, invite);
        return invite;
    }

    private Couleur couleurLibre(Couleur souhaitee) {
        if (souhaitee != null && estLibre(souhaitee)) {
            return souhaitee;
        }
        for (Couleur couleur : Couleur.values()) {
            if (estLibre(couleur)) {
                return couleur;
            }
        }
        return null;
    }

    private boolean estLibre(Couleur couleur) {
        return couleur != hote.couleur() && !invites.containsKey(couleur);
    }

    /** Nom nettoyé, raccourci, et rendu unique dans le salon. */
    private String nomValide(String demande) {
        String nom = demande == null ? "" : demande.strip();
        if (nom.isEmpty()) {
            nom = "Invité";
        }
        if (nom.length() > LONGUEUR_MAX_NOM) {
            nom = nom.substring(0, LONGUEUR_MAX_NOM);
        }
        String unique = nom;
        for (int numero = 2; nomPris(unique); numero++) {
            unique = nom + " " + numero;
        }
        return unique;
    }

    private boolean nomPris(String nom) {
        return hote.nom().equalsIgnoreCase(nom)
                || invites.values().stream().anyMatch(invite -> invite.place().nom().equalsIgnoreCase(nom));
    }

    private void ecouter(Invite invite) {
        Place place = invite.place();
        try {
            // On lit jusqu'à la coupure de la connexion. Ne pas s'arrêter sur « arrete » :
            // ce fil fermerait la connexion avant que arreter() ait envoyé FinSession.
            while (true) {
                MessageReseau message = invite.connexion().recevoir();
                if (message instanceof MessageReseau.Chat chat) {
                    // L'auteur est celui de la connexion : un invité ne peut pas parler au nom d'un autre.
                    Message propre = new Message(place.nom(), place.couleur(), texteValide(chat.message()),
                            LocalTime.now());
                    if (!propre.texte().isEmpty()) {
                        diffuser(new MessageReseau.Chat(propre));
                        executeur.execute(() -> ecouteur.messageRecu(place, new MessageReseau.Chat(propre)));
                    }
                } else if (message instanceof MessageReseau.Lancer
                        || message instanceof MessageReseau.ChoisirCheval) {
                    executeur.execute(() -> ecouteur.messageRecu(place, message));
                }
            }
        } catch (IOException e) {
            // Invité parti ou connexion coupée.
        }
        invites.remove(place.couleur());
        invite.connexion().close();
        if (!arrete) {
            executeur.execute(() -> ecouteur.depart(place));
            annoncerSalon();
        }
    }

    private static String texteValide(Message message) {
        String texte = message == null || message.texte() == null ? "" : message.texte().strip();
        return texte.length() > Message.LONGUEUR_MAX ? texte.substring(0, Message.LONGUEUR_MAX) : texte;
    }

    private void annoncerSalon() {
        List<Place> places = getPlaces();
        diffuser(new MessageReseau.Salon(places));
        executeur.execute(() -> ecouteur.salonModifie(places));
    }

    private static void fermer(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignoree) {
            // Déjà fermée.
        }
    }
}
