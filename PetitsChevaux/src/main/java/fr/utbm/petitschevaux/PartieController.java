package fr.utbm.petitschevaux;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contrôleur de l'écran de partie : relaie les actions du joueur au {@link Jeu}
 * et met la vue à jour.
 */
public class PartieController {
    private static final Duration DELAI_BOT = Duration.millis(700);
    private static final Duration DELAI_VICTOIRE = Duration.millis(1500);

    @FXML private Pane zonePlateau;
    @FXML private Label information;
    @FXML private Label resultatDe;
    @FXML private Button boutonLancer;
    @FXML private VBox historiqueLancers;
    @FXML private VBox joueursAffiches;
    @FXML private StackPane vueDe;

    private Jeu jeu;
    private PlateauView plateauView;
    private DeView deView;
    /** Pause avant le prochain coup d'un bot, ou {@code null} si aucun bot n'attend. */
    private PauseTransition pauseBot;
    /** Pause avant l'écran de victoire, une fois la partie terminée. */
    private PauseTransition pauseVictoire;
    /** Fichier de cette partie dans le dossier des sauvegardes, ou {@code null} s'il n'existe pas encore. */
    private Path fichierSauvegarde;

    /**
     * Branche une partie (nouvelle ou chargée) sur la vue.
     * Appelé par {@link PetitsChevauxApp#afficherPartie} après le chargement du FXML.
     *
     * @param fichierSauvegarde fichier d'où la partie a été chargée (mis à jour à la prochaine
     *                          sauvegarde), ou {@code null} pour une nouvelle partie
     */
    public void setJeu(Jeu jeu, Path fichierSauvegarde) {
        this.jeu = jeu;
        this.fichierSauvegarde = fichierSauvegarde;
        plateauView = new PlateauView(jeu.getPlateau(), jeu.getJoueurs(), this::onChevalSelectionne);
        zonePlateau.getChildren().setAll(plateauView.getRoot());
        deView = new DeView();
        vueDe.getChildren().setAll(deView.getRoot());
        afficherJoueurs();
        actualiserVue();
        jouerLesBots();
    }

    @FXML
    private void onLancerDe() {
        if (!jeu.peutLancer() || jeu.getJoueurActif().isBot()) {
            return;
        }
        jeu.lancerDe();
        // Si aucun cheval n'était jouable, le tour est peut-être passé à un bot.
        actualiserVue();
        jouerLesBots();
    }

    private void onChevalSelectionne(Cheval cheval) {
        if (!jeu.getJoueurActif().isBot() && jeu.jouerCheval(cheval)) {
            actualiserVue();
            jouerLesBots();
        }
    }

    /**
     * Fait jouer les bots les uns après les autres, avec une courte pause entre
     * chaque coup pour que les joueurs humains puissent suivre.
     */
    private void jouerLesBots() {
        if (pauseBot != null || !jeu.peutLancer() || !jeu.getJoueurActif().isBot()) {
            return;
        }
        pauseBot = new PauseTransition(DELAI_BOT);
        pauseBot.setOnFinished(event -> {
            pauseBot = null;
            jeu.jouerTourBot();
            actualiserVue();
            jouerLesBots();
        });
        pauseBot.play();
    }

    @FXML
    private void onSauvegarder() {
        try {
            fichierSauvegarde = Sauvegarde.sauvegarderDansLeDossier(jeu, Sauvegarde.DOSSIER, fichierSauvegarde);
            information.setText("Partie sauvegardée : " + fichierSauvegarde.getFileName());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible de sauvegarder", e);
        }
    }

    @FXML
    private void onRetourMenu() {
        for (PauseTransition pause : new PauseTransition[] { pauseBot, pauseVictoire }) {
            if (pause != null) {
                pause.stop();
            }
        }
        pauseBot = null;
        pauseVictoire = null;
        try {
            PetitsChevauxApp.afficherMenu(getStage());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le menu", e);
        }
    }

    private void actualiserVue() {
        int valeurDe = jeu.getDe().getValeur();
        plateauView.afficherPossibilites(jeu.getChevauxDeplacables(), valeurDe);
        Joueur actif = jeu.getJoueurActif();
        boolean tourDeBot = actif.isBot() && !jeu.estTerminee();
        information.setText(tourDeBot ? actif.getNom() + " (bot) réfléchit…"
                : switch (jeu.getEtat()) {
                    case ATTENTE_LANCER -> actif.getNom() + " - lancez le dé";
                    case ATTENTE_CHOIX -> actif.getNom() + " - cliquez sur un cheval qui clignote";
                    case TERMINEE -> jeu.getVainqueur().getNom() + " a gagné la partie !";
                });
        information.setTextFill(Rendu.couleur(jeu.estTerminee()
                ? jeu.getVainqueur().getCouleur() : actif.getCouleur()));
        boutonLancer.setDisable(!jeu.peutLancer() || tourDeBot);
        resultatDe.setText("Résultat du dé : " + (valeurDe == 0 ? "-" : valeurDe));
        deView.afficher(valeurDe);
        afficherHistorique();
        if (jeu.estTerminee()) {
            afficherVictoireBientot();
        }
    }

    /**
     * Laisse voir le dernier coup un instant, puis passe à l'écran de victoire.
     */
    private void afficherVictoireBientot() {
        if (pauseVictoire != null) {
            return;
        }
        pauseVictoire = new PauseTransition(DELAI_VICTOIRE);
        pauseVictoire.setOnFinished(event -> {
            try {
                PetitsChevauxApp.afficherVictoire(getStage(), jeu);
            } catch (IOException e) {
                PetitsChevauxApp.afficherErreur("Impossible d'afficher la victoire", e);
            }
        });
        pauseVictoire.play();
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

    private void afficherJoueurs() {
        joueursAffiches.getChildren().setAll(new Label("Joueurs"));
        for (Joueur joueur : jeu.getJoueurs()) {
            Label nom = new Label(joueur.getNom() + (joueur.isBot() ? " (bot)" : ""));
            nom.setTextFill(Rendu.couleur(joueur.getCouleur()));
            joueursAffiches.getChildren().add(nom);
        }
    }

    private Stage getStage() {
        return (Stage) zonePlateau.getScene().getWindow();
    }
}
