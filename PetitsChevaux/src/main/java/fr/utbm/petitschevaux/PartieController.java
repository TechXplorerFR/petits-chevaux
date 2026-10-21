package fr.utbm.petitschevaux;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur de l'écran de partie : relaie les actions du joueur au {@link Jeu}
 * et met la vue à jour.
 *
 * <p>En ligne ({@link SessionEnLigne}), l'hôte est maître de la partie : il applique ses coups
 * et ceux des invités, fait jouer les bots et envoie l'état de la partie après chaque coup.
 * Un invité n'envoie que ses intentions (lancer, choisir un cheval) et affiche l'état reçu.</p>
 */
public class PartieController {
    private static final Duration DELAI_BOT = Duration.millis(700);
    private static final Duration DELAI_VICTOIRE = Duration.millis(1500);

    @FXML private Pane zonePlateau;
    @FXML private Label information;
    @FXML private Label resultatDe;
    @FXML private Button boutonLancer;
    @FXML private Button boutonSauvegarder;
    @FXML private VBox historiqueLancers;
    @FXML private VBox joueursAffiches;
    @FXML private StackPane vueDe;
    @FXML private TabPane panneauGauche;
    @FXML private Tab ongletCases;
    @FXML private Tab ongletChat;
    @FXML private VBox legende;
    @FXML private VBox journal;
    @FXML private StackPane zoneChat;

    private Jeu jeu;
    private PlateauView plateauView;
    private DeView deView;
    /** Pause avant le prochain coup d'un bot, ou {@code null} si aucun bot n'attend. */
    private PauseTransition pauseBot;
    /** Pause avant l'écran de victoire, une fois la partie terminée. */
    private PauseTransition pauseVictoire;
    /** Fichier de cette partie dans le dossier des sauvegardes, ou {@code null} s'il n'existe pas encore. */
    private Path fichierSauvegarde;
    /** Partie en ligne, ou {@code null} pour une partie sur cet ordinateur. */
    private SessionEnLigne session;
    private ChatView chat;
    private int messagesNonLus;

    /**
     * Branche une partie locale (nouvelle ou chargée) sur la vue.
     *
     * @param fichierSauvegarde fichier d'où la partie a été chargée (mis à jour à la prochaine
     *                          sauvegarde), ou {@code null} pour une nouvelle partie
     */
    public void setJeu(Jeu jeu, Path fichierSauvegarde) {
        setJeu(jeu, fichierSauvegarde, null);
    }

    /**
     * Branche une partie sur la vue.
     * Appelé par {@link PetitsChevauxApp#afficherPartie} après le chargement du FXML.
     *
     * @param session partie en ligne (hôte ou invité), ou {@code null} pour une partie locale
     */
    public void setJeu(Jeu jeu, Path fichierSauvegarde, SessionEnLigne session) {
        this.fichierSauvegarde = fichierSauvegarde;
        this.session = session;
        deView = new DeView();
        vueDe.getChildren().setAll(deView.getRoot());
        installerJeu(jeu);
        configurerPanneauGauche();
        if (session != null) {
            ecouterSession();
            boutonSauvegarder.setDisable(true); // Une partie en ligne ne se sauvegarde pas.
        }
        actualiserVue();
        jouerLesBots();
    }

    /** Affiche cette partie (au départ, puis côté invité à chaque état reçu). */
    private void installerJeu(Jeu nouveau) {
        if (plateauView != null) {
            plateauView.arreter();
        }
        jeu = nouveau;
        plateauView = new PlateauView(jeu.getPlateau(), jeu.getJoueurs(), this::onChevalSelectionne);
        zonePlateau.getChildren().setAll(plateauView.getRoot());
        afficherJoueurs();
    }

    /** Onglets de gauche : la légende des cases spéciales et/ou le chat, sinon rien. */
    private void configurerPanneauGauche() {
        if (jeu.getMode() == ModeJeu.CASES_SPECIALES) {
            afficherLegende();
        } else {
            panneauGauche.getTabs().remove(ongletCases);
        }
        if (session == null) {
            panneauGauche.getTabs().remove(ongletChat);
        } else {
            chat = new ChatView(session::envoyerChat);
            chat.afficher(session.getMessages());
            zoneChat.getChildren().setAll(chat.getRoot());
            ongletChat.setOnSelectionChanged(evenement -> {
                if (ongletChat.isSelected()) {
                    messagesNonLus = 0;
                    ongletChat.setText("Chat");
                }
            });
        }
        boolean visible = !panneauGauche.getTabs().isEmpty();
        panneauGauche.setVisible(visible);
        panneauGauche.setManaged(visible);
    }

    private void ecouterSession() {
        session.surChat(message -> {
            chat.ajouter(message);
            if (!ongletChat.isSelected()) {
                messagesNonLus++;
                ongletChat.setText("Chat (" + messagesNonLus + ")");
            }
        });
        if (session.estHote()) {
            session.surAction(this::appliquerActionDistante);
            session.surDepart(this::remplacerParBot);
        } else {
            session.surEtat(this::recevoirEtat);
            session.surFin(this::finDeSession);
        }
    }

    /** Vrai si le joueur se joue depuis cet ordinateur (ni bot, ni joueur distant). */
    private boolean estLocal(Joueur joueur) {
        return !joueur.isBot() && (session == null || joueur.getCouleur() == session.getCouleurLocale());
    }

    private boolean estInvite() {
        return session != null && !session.estHote();
    }

    @FXML
    private void onLancerDe() {
        if (!jeu.peutLancer() || !estLocal(jeu.getJoueurActif())) {
            return;
        }
        if (estInvite()) {
            boutonLancer.setDisable(true); // Réactivé par le prochain état reçu de l'hôte.
            session.envoyerAction(new MessageReseau.Lancer());
            return;
        }
        jeu.lancerDe();
        apresCoup();
    }

    private void onChevalSelectionne(Cheval cheval) {
        if (!estLocal(jeu.getJoueurActif()) || !jeu.getChevauxDeplacables().contains(cheval)) {
            return;
        }
        if (estInvite()) {
            session.envoyerAction(new MessageReseau.ChoisirCheval(cheval.getNumero()));
            return;
        }
        if (jeu.jouerCheval(cheval)) {
            apresCoup();
        }
    }

    /**
     * Hôte : applique l'action d'un invité, si c'est bien à lui de jouer.
     */
    private void appliquerActionDistante(Place auteur, MessageReseau action) {
        Joueur actif = jeu.getJoueurActif();
        if (actif.isBot() || actif.getCouleur() != auteur.couleur() || jeu.estTerminee()) {
            return; // Pas son tour : l'action est ignorée.
        }
        if (action instanceof MessageReseau.Lancer && jeu.peutLancer()) {
            jeu.lancerDe();
            apresCoup();
        } else if (action instanceof MessageReseau.ChoisirCheval choix
                && choix.numero() >= 0 && choix.numero() < Joueur.NOMBRE_CHEVAUX
                && jeu.jouerCheval(actif.getChevaux().get(choix.numero()))) {
            apresCoup();
        }
    }

    /**
     * Hôte : un invité est parti, un bot reprend ses chevaux et la partie continue.
     */
    private void remplacerParBot(Place place) {
        if (jeu.estTerminee()) {
            return;
        }
        jeu.getJoueurs().get(place.couleur().ordinal()).remplacerParBot();
        session.messagePartie(place.nom() + " a quitté la partie : un bot prend sa place.");
        afficherJoueurs();
        apresCoup();
    }

    /**
     * Invité : affiche le nouvel état envoyé par l'hôte.
     */
    private void recevoirEtat(String etat) {
        try {
            installerJeu(Sauvegarde.depuisTexte(etat));
            actualiserVue();
        } catch (IOException e) {
            finDeSession("État de la partie illisible : " + e.getMessage());
        }
    }

    /**
     * Invité : l'hôte est parti ou la connexion est perdue.
     */
    private void finDeSession(String raison) {
        if (jeu.estTerminee()) {
            return; // L'écran de victoire arrive : rien à signaler.
        }
        arreterPauses();
        Alert alerte = new Alert(Alert.AlertType.INFORMATION, raison);
        alerte.setHeaderText("Partie en ligne terminée");
        alerte.show();
        ouvrirMenu();
    }

    private void apresCoup() {
        actualiserVue();
        jouerLesBots();
    }

    /**
     * Fait jouer les bots les uns après les autres, avec une courte pause entre
     * chaque coup pour que les joueurs humains puissent suivre. Chez un invité,
     * ce sont les bots de l'hôte qui jouent.
     */
    private void jouerLesBots() {
        if (estInvite() || pauseBot != null || jeu.estTerminee() || !jeu.getJoueurActif().isBot()) {
            return;
        }
        pauseBot = new PauseTransition(DELAI_BOT);
        pauseBot.setOnFinished(event -> {
            pauseBot = null;
            jeu.jouerTourBot();
            apresCoup();
        });
        pauseBot.play();
    }

    @FXML
    private void onSauvegarder() {
        if (session != null) {
            return;
        }
        try {
            fichierSauvegarde = Sauvegarde.sauvegarderDansLeDossier(jeu, Sauvegarde.DOSSIER, fichierSauvegarde);
            information.setText("Partie sauvegardée : " + fichierSauvegarde.getFileName());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible de sauvegarder", e);
        }
    }

    @FXML
    private void onRetourMenu() {
        arreterPauses();
        if (session != null) {
            session.fermer();
        }
        ouvrirMenu();
    }

    private void arreterPauses() {
        for (PauseTransition pause : new PauseTransition[] { pauseBot, pauseVictoire }) {
            if (pause != null) {
                pause.stop();
            }
        }
        pauseBot = null;
        pauseVictoire = null;
    }

    private void ouvrirMenu() {
        plateauView.arreter();
        try {
            PetitsChevauxApp.afficherMenu(getStage());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le menu", e);
        }
    }

    private void actualiserVue() {
        int valeurDe = jeu.getDe().getValeur();
        Joueur actif = jeu.getJoueurActif();
        // Seuls les chevaux qu'on peut cliquer depuis cet ordinateur clignotent.
        plateauView.afficherPossibilites(estLocal(actif) ? jeu.getChevauxDeplacables() : List.of(), valeurDe);
        boolean enCours = !jeu.estTerminee();
        if (enCours && actif.isBot()) {
            information.setText(actif.getNom() + " (bot) réfléchit…");
        } else if (enCours && !estLocal(actif)) {
            information.setText(actif.getNom() + " joue (en ligne)…");
        } else {
            information.setText(switch (jeu.getEtat()) {
                case ATTENTE_LANCER -> actif.getNom() + " - lancez le dé";
                case ATTENTE_CHOIX -> actif.getNom() + " - cliquez sur un cheval qui clignote";
                case TERMINEE -> jeu.getVainqueur().getNom() + " a gagné la partie !";
            });
        }
        information.setTextFill(Rendu.couleur(jeu.estTerminee()
                ? jeu.getVainqueur().getCouleur() : actif.getCouleur()));
        boutonLancer.setDisable(!jeu.peutLancer() || !estLocal(actif));
        resultatDe.setText("Résultat du dé : " + (valeurDe == 0 ? "-" : valeurDe));
        deView.afficher(valeurDe);
        afficherHistorique();
        if (jeu.getMode() == ModeJeu.CASES_SPECIALES) {
            afficherJournal();
        }
        if (session != null && session.estHote()) {
            session.diffuserEtat(jeu);
        }
        if (jeu.estTerminee()) {
            afficherVictoireBientot();
        }
    }

    /**
     * Enregistre le résultat dans les scores (en local ou chez l'hôte : une seule fois par
     * partie), laisse voir le dernier coup un instant, puis passe à l'écran de victoire.
     */
    private void afficherVictoireBientot() {
        if (pauseVictoire != null) {
            return;
        }
        if (!estInvite()) {
            enregistrerScores();
        }
        pauseVictoire = new PauseTransition(DELAI_VICTOIRE);
        pauseVictoire.setOnFinished(event -> {
            plateauView.arreter();
            try {
                PetitsChevauxApp.afficherVictoire(getStage(), jeu, session);
            } catch (IOException e) {
                PetitsChevauxApp.afficherErreur("Impossible d'afficher la victoire", e);
            }
        });
        pauseVictoire.play();
    }

    private void enregistrerScores() {
        try {
            new ScoresDAO(BaseDeDonnees.parDefaut()).enregistrerPartie(jeu);
        } catch (SQLException e) {
            // Les scores sont un plus : la partie et l'écran de victoire continuent sans eux.
            Alert alerte = new Alert(Alert.AlertType.WARNING,
                    "Le résultat n'a pas pu être ajouté aux scores :\n" + e.getMessage());
            alerte.setHeaderText("Scores indisponibles");
            alerte.show();
        }
    }

    private void afficherHistorique() {
        historiqueLancers.getChildren().setAll(
                new Label(Jeu.TAILLE_HISTORIQUE + " derniers lancers"));
        for (Jeu.Lancer lancer : jeu.getHistorique()) {
            Label ligne = new Label("• " + lancer.joueur().getNom() + " : " + lancer.valeur());
            ligne.setTextFill(Rendu.couleur(lancer.joueur().getCouleur()));
            historiqueLancers.getChildren().add(ligne);
        }
    }

    /**
     * Une ligne par effet : la case telle qu'elle apparaît sur le plateau, son nom et son effet.
     */
    private void afficherLegende() {
        legende.getChildren().clear();
        for (CaseSpeciale effet : CaseSpeciale.values()) {
            double demiCase = Rendu.TAILLE_CASE / 2;
            Pane apercu = new Pane(Rendu.creerCaseSpeciale(demiCase, demiCase, effet));
            apercu.setMinSize(Rendu.TAILLE_CASE, Rendu.TAILLE_CASE);
            Label nom = new Label(effet.getNom());
            nom.setStyle("-fx-font-weight: bold;");
            Label description = new Label(effet.getDescription());
            description.setWrapText(true);
            description.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b665e;");
            VBox texte = new VBox(1, nom, description);
            HBox.setHgrow(texte, Priority.ALWAYS);
            HBox ligne = new HBox(8, apercu, texte);
            ligne.setAlignment(Pos.CENTER_LEFT);
            legende.getChildren().add(ligne);
        }
    }

    /**
     * Derniers effets déclenchés, puis les effets encore en attente (gel, dé maudit).
     */
    private void afficherJournal() {
        journal.getChildren().clear();
        for (Jeu.Evenement evenement : jeu.getJournal()) {
            Label ligne = new Label(evenement.message());
            ligne.setWrapText(true);
            ligne.setTextFill(Rendu.couleur(evenement.joueur().getCouleur()).darker());
            ligne.setStyle("-fx-font-size: 11px;");
            journal.getChildren().add(ligne);
        }
        if (jeu.getJournal().isEmpty()) {
            Label vide = new Label("Aucun effet pour l'instant.");
            vide.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b665e;");
            journal.getChildren().add(vide);
        }
        for (Joueur joueur : jeu.getJoueurs()) {
            StringBuilder attente = new StringBuilder();
            if (jeu.getToursGeles(joueur) > 0) {
                attente.append("passe son prochain tour (gel) ");
            }
            if (jeu.getMalusDe(joueur) > 0) {
                attente.append("dé maudit (-").append(jeu.getMalusDe(joueur)).append(")");
            }
            if (!attente.isEmpty()) {
                Label ligne = new Label(joueur.getNom() + " : " + attente.toString().trim());
                ligne.setWrapText(true);
                ligne.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
                ligne.setTextFill(Rendu.couleur(joueur.getCouleur()).darker());
                journal.getChildren().add(ligne);
            }
        }
    }

    private void afficherJoueurs() {
        joueursAffiches.getChildren().setAll(new Label("Joueurs"));
        for (Joueur joueur : jeu.getJoueurs()) {
            String precision = joueur.isBot() ? " (bot)"
                    : session == null ? ""
                    : joueur.getCouleur() == session.getCouleurLocale() ? " (vous)" : " (en ligne)";
            Label nom = new Label(joueur.getNom() + precision);
            nom.setTextFill(Rendu.couleur(joueur.getCouleur()));
            joueursAffiches.getChildren().add(nom);
        }
    }

    private Stage getStage() {
        return (Stage) zonePlateau.getScene().getWindow();
    }
}
