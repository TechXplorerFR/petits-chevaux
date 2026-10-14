package fr.utbm.petitschevaux;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;

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
    @FXML private VBox panneauSpecial;
    @FXML private VBox legende;
    @FXML private VBox journal;

    private Jeu jeu;
    private PlateauView plateauView;
    private DeView deView;
    /** Pause avant le prochain coup d'un bot, ou {@code null} si aucun bot n'attend. */
    private PauseTransition pauseBot;
    /** Pause avant l'écran de victoire, une fois la partie terminée. */
    private PauseTransition pauseVictoire;

    /**
     * Branche une partie (nouvelle ou chargée) sur la vue.
     * Appelé par {@link PetitsChevauxApp#afficherPartie} après le chargement du FXML.
     */
    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        plateauView = new PlateauView(jeu.getPlateau(), jeu.getJoueurs(), this::onChevalSelectionne);
        zonePlateau.getChildren().setAll(plateauView.getRoot());
        deView = new DeView();
        vueDe.getChildren().setAll(deView.getRoot());
        afficherJoueurs();
        boolean casesSpeciales = jeu.getMode() == ModeJeu.CASES_SPECIALES;
        panneauSpecial.setVisible(casesSpeciales);
        panneauSpecial.setManaged(casesSpeciales);
        if (casesSpeciales) {
            afficherLegende();
        }
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
        FileChooser choix = new FileChooser();
        choix.setTitle("Sauvegarder la partie");
        choix.setInitialFileName("partie." + Sauvegarde.EXTENSION);
        choix.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Partie de petits chevaux", "*." + Sauvegarde.EXTENSION));
        File fichier = choix.showSaveDialog(getStage());
        if (fichier == null) {
            return;
        }
        try {
            Sauvegarde.sauvegarder(jeu, fichier.toPath());
            information.setText("Partie sauvegardée.");
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
        if (jeu.getMode() == ModeJeu.CASES_SPECIALES) {
            afficherJournal();
        }
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
            Label nom = new Label(joueur.getNom() + (joueur.isBot() ? " (bot)" : ""));
            nom.setTextFill(Rendu.couleur(joueur.getCouleur()));
            joueursAffiches.getChildren().add(nom);
        }
    }

    private Stage getStage() {
        return (Stage) zonePlateau.getScene().getWindow();
    }
}
