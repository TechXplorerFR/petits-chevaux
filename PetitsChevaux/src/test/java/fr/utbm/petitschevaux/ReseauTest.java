package fr.utbm.petitschevaux;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du serveur et du client avec de vraies connexions sur cet ordinateur.
 * Les événements sont remis directement (executor {@code Runnable::run}) et rangés dans des files.
 */
class ReseauTest {
    private static final Place HOTE = new Place("Hôte", Couleur.JAUNE, true);

    /** Écouteur de l'hôte qui range tout ce qu'il reçoit. */
    private static final class EcouteurHote implements ServeurJeu.Ecouteur {
        final BlockingQueue<List<Place>> salons = new LinkedBlockingQueue<>();
        final BlockingQueue<Object[]> messages = new LinkedBlockingQueue<>();
        final BlockingQueue<Place> departs = new LinkedBlockingQueue<>();

        @Override
        public void salonModifie(List<Place> places) {
            salons.add(places);
        }

        @Override
        public void messageRecu(Place auteur, MessageReseau message) {
            messages.add(new Object[] { auteur, message });
        }

        @Override
        public void depart(Place place) {
            departs.add(place);
        }
    }

    /** Écouteur d'un invité. */
    private static final class EcouteurInvite implements ClientReseau.Ecouteur {
        final BlockingQueue<MessageReseau> recus = new LinkedBlockingQueue<>();
        final BlockingQueue<String> pertes = new LinkedBlockingQueue<>();

        @Override
        public void messageRecu(MessageReseau message) {
            recus.add(message);
        }

        @Override
        public void connexionPerdue(String raison) {
            pertes.add(raison);
        }

        /** Attend le prochain message du type donné (en ignorant les autres). */
        <T extends MessageReseau> T attendre(Class<T> type) throws InterruptedException {
            while (true) {
                MessageReseau message = recus.poll(5, TimeUnit.SECONDS);
                assertNotNull(message, "aucun " + type.getSimpleName() + " reçu");
                if (type.isInstance(message)) {
                    return type.cast(message);
                }
            }
        }
    }

    private final EcouteurHote hote = new EcouteurHote();
    private final ServeurJeu serveur = new ServeurJeu(Runnable::run, hote);
    private final List<ClientReseau> clients = new ArrayList<>();

    @BeforeEach
    void demarrerServeur() throws IOException {
        serveur.demarrer(0, HOTE);
    }

    @AfterEach
    void arreter() {
        clients.forEach(ClientReseau::deconnecter);
        serveur.arreter("fin du test");
    }

    private EcouteurInvite rejoindre(String nom, Couleur couleur) throws IOException {
        EcouteurInvite ecouteur = new EcouteurInvite();
        ClientReseau client = new ClientReseau(Runnable::run, ecouteur);
        client.connecter("localhost", serveur.getPort(), nom, couleur);
        clients.add(client);
        return ecouteur;
    }

    @Test
    void unInviteRecoitSaCouleurEtLaListeDuSalon() throws Exception {
        EcouteurInvite alice = rejoindre("Alice", Couleur.BLEU);

        assertEquals(Couleur.BLEU, alice.attendre(MessageReseau.Bienvenue.class).couleur());
        List<Place> places = alice.attendre(MessageReseau.Salon.class).places();
        assertEquals(List.of(HOTE, new Place("Alice", Couleur.BLEU, false)), places);
        assertEquals(places, hote.salons.poll(5, TimeUnit.SECONDS));
    }

    @Test
    void uneCouleurDejaPriseEstRemplaceeEtLesNomsSontRendusUniques() throws Exception {
        EcouteurInvite intrus = rejoindre("hôte", Couleur.JAUNE);

        Couleur couleur = intrus.attendre(MessageReseau.Bienvenue.class).couleur();
        assertNotEquals(Couleur.JAUNE, couleur, "la couleur de l'hôte n'est jamais donnée");
        Place place = intrus.attendre(MessageReseau.Salon.class).places().get(1);
        assertEquals("hôte 2", place.nom());
    }

    @Test
    void auDelaDeTroisInvitesLeSalonEstComplet() throws Exception {
        for (String nom : List.of("A", "B", "C")) {
            rejoindre(nom, null).attendre(MessageReseau.Bienvenue.class);
        }
        EcouteurInvite enTrop = rejoindre("D", null);

        assertEquals("La partie est complète.", enTrop.attendre(MessageReseau.Refus.class).raison());
    }

    @Test
    void uneFoisLaPartieCommenceePersonneNePeutEntrer() throws Exception {
        serveur.fermerSalon();
        EcouteurInvite enRetard = rejoindre("Tardif", null);

        assertEquals("La partie a déjà commencé.", enRetard.attendre(MessageReseau.Refus.class).raison());
    }

    @Test
    void leChatEstRelayeATousAvecLeVraiNomDeLAuteur() throws Exception {
        EcouteurInvite alice = rejoindre("Alice", Couleur.ROUGE);
        EcouteurInvite bob = rejoindre("Bob", Couleur.BLEU);
        alice.attendre(MessageReseau.Bienvenue.class);
        bob.attendre(MessageReseau.Bienvenue.class);

        // Alice essaie de se faire passer pour Bob : le serveur remet son vrai nom.
        clients.get(0).envoyer(new MessageReseau.Chat(new Message("Bob", Couleur.BLEU, "  salut  ", null)));

        Message recuParBob = bob.attendre(MessageReseau.Chat.class).message();
        assertEquals("Alice", recuParBob.auteur());
        assertEquals(Couleur.ROUGE, recuParBob.couleur());
        assertEquals("salut", recuParBob.texte());
        Object[] recuParHote = hote.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(recuParHote);
        assertInstanceOf(MessageReseau.Chat.class, recuParHote[1]);
    }

    @Test
    void lesActionsDesInvitesSontTransmisesALHote() throws Exception {
        EcouteurInvite alice = rejoindre("Alice", Couleur.ROUGE);
        alice.attendre(MessageReseau.Bienvenue.class);

        clients.get(0).envoyer(new MessageReseau.ChoisirCheval(2));

        Object[] recu = hote.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(recu);
        assertEquals(Couleur.ROUGE, ((Place) recu[0]).couleur());
        assertEquals(new MessageReseau.ChoisirCheval(2), recu[1]);
    }

    @Test
    void leDepartDUnInviteEstSignaleALHote() throws Exception {
        EcouteurInvite alice = rejoindre("Alice", Couleur.ROUGE);
        alice.attendre(MessageReseau.Bienvenue.class);

        clients.get(0).deconnecter();

        Place partie = hote.departs.poll(5, TimeUnit.SECONDS);
        assertNotNull(partie);
        assertEquals("Alice", partie.nom());
    }

    @Test
    void quandLHoteArreteLesInvitesSontPrevenus() throws Exception {
        EcouteurInvite alice = rejoindre("Alice", Couleur.ROUGE);
        alice.attendre(MessageReseau.Bienvenue.class);

        serveur.arreter("L'hôte a quitté la partie.");

        assertEquals("L'hôte a quitté la partie.", alice.attendre(MessageReseau.FinSession.class).raison());
    }

    @Test
    void unObjetEtrangerEstRefuseParLeFiltre() throws Exception {
        boolean coupe = false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", serveur.getPort()), 5000);
            socket.setSoTimeout(5000);
            try {
                ObjectOutputStream sortie = new ObjectOutputStream(socket.getOutputStream());
                // Une classe hors du jeu (java.net) : le serveur doit refuser de la recréer et couper.
                sortie.writeObject(new InetSocketAddress("exemple.org", 80));
                sortie.flush();
                while (socket.getInputStream().read() != -1) {
                    // L'en-tête du serveur, puis la fermeture.
                }
                coupe = true;
            } catch (IOException e) {
                coupe = true; // Coupé pendant l'envoi : refus tout aussi net.
            }
        }
        assertTrue(coupe, "le serveur a coupé la connexion");
        assertTrue(hote.salons.isEmpty(), "personne n'est entré dans le salon");
    }

    @Test
    void uneSessionInviteRecoitLaPartieDeLHote() throws Exception {
        SessionEnLigne sessionHote = SessionEnLigne.heberger(Runnable::run, 0, "Hôte", Couleur.JAUNE,
                ModeJeu.CASES_SPECIALES);
        try {
            BlockingQueue<String> debuts = new LinkedBlockingQueue<>();
            SessionEnLigne invite = SessionEnLigne.rejoindre(Runnable::run, "localhost",
                    sessionHote.getPort(), "Alice", Couleur.VERT);
            invite.surDebutPartie(debuts::add);
            BlockingQueue<List<Place>> salons = new LinkedBlockingQueue<>();
            sessionHote.surSalon(salons::add);
            assertEquals(2, salons.poll(5, TimeUnit.SECONDS).size());

            Jeu jeu = new Jeu(sessionHote.getPlaces().stream()
                    .map(place -> new Joueur(place.nom(), place.couleur())).toList(), sessionHote.getMode());
            sessionHote.demarrerPartie(jeu);

            Jeu recu = Sauvegarde.depuisTexte(debuts.poll(5, TimeUnit.SECONDS));
            assertEquals(Couleur.VERT, invite.getCouleurLocale());
            assertEquals("Alice", recu.getJoueurs().get(Couleur.VERT.ordinal()).getNom());
            assertEquals(jeu.getPlateau().getCasesSpeciales(), recu.getPlateau().getCasesSpeciales());
            invite.fermer();
        } finally {
            sessionHote.fermer();
        }
    }
}
