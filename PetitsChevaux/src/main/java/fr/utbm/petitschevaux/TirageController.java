package fr.utbm.petitschevaux;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur de l'écran « Qui commence ? » : chaque joueur lance le dé à son tour
 * (automatiquement pour les bots), puis la partie démarre avec le gagnant du tirage.
 */
public class TirageController {
    private static final Duration DELAI_BOT = Duration.millis(700);

    /** Éléments graphiques de la carte d'un joueur. */
    private record Carte(VBox racine, DeView de, Label resultat, Button bouton) {
    }

    @FXML private HBox cartes;
    @FXML private Label message;
    @FXML private Button boutonCommencer;

    private Jeu jeu;
    private TiragePremierJoueur tirage;
    private final Map<Joueur, Carte> cartesParJoueur = new LinkedHashMap<>();
    private PauseTransition pauseBot;

    /**
     * Prépare le tirage pour une nouvelle partie.
     */
    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        this.tirage = new TiragePremierJoueur(jeu.getJoueurs(), new De());
        for (Joueur joueur : jeu.getJoueurs()) {
            Carte carte = creerCarte(joueur);
            cartesParJoueur.put(joueur, carte);
            cartes.getChildren().add(carte.racine());
        }
        actualiser();
    }

    private Carte creerCarte(Joueur joueur) {
        Label nom = new Label(joueur.getNom());
        nom.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        nom.setTextFill(Rendu.couleur(joueur.getCouleur()));
        Label type = new Label(joueur.isBot() ? "bot" : "humain");
        type.setStyle("-fx-text-fill: #6b665e;");
        DeView de = new DeView();
        Label resultat = new Label();
        Button bouton = new Button("Lancer le dé");
        bouton.setOnAction(event -> lancer());
        bouton.setVisible(!joueur.isBot());
        VBox racine = new VBox(10, nom, type, de.getRoot(), resultat, bouton);
        racine.setAlignment(Pos.CENTER);
        racine.setPrefWidth(170);
        return new Carte(racine, de, resultat, bouton);
    }

    private void lancer() {
        if (!tirage.estTermine()) {
            tirage.lancer();
            actualiser();
        }
    }

    private void actualiser() {
        Joueur lanceur = tirage.getLanceur();
        for (Map.Entry<Joueur, Carte> entree : cartesParJoueur.entrySet()) {
            Joueur joueur = entree.getKey();
            Carte carte = entree.getValue();
            int valeur = tirage.getResultat(joueur);
            carte.de().afficher(valeur);
            carte.resultat().setText(valeur == 0 ? " " : "Score : " + valeur);
            carte.bouton().setDisable(joueur != lanceur);
            boolean enJeu = tirage.estCandidat(joueur);
            carte.racine().setOpacity(enJeu ? 1 : 0.4);
            boolean miseEnAvant = joueur == lanceur || joueur == tirage.getGagnant();
            carte.racine().setStyle("-fx-background-color: white; -fx-background-radius: 10;"
                    + " -fx-border-radius: 10; -fx-padding: 16;"
                    + " -fx-border-width: " + (miseEnAvant ? 4 : 1) + ";"
                    + " -fx-border-color: " + (miseEnAvant ? Rendu.css(joueur.getCouleur()) : "#c6c0b5") + ";");
        }
        boutonCommencer.setDisable(!tirage.estTermine());
        afficherMessage(lanceur);
        if (lanceur != null && lanceur.isBot()) {
            faireLancerLeBot();
        }
    }

    private void afficherMessage(Joueur lanceur) {
        if (tirage.estTermine()) {
            Joueur gagnant = tirage.getGagnant();
            message.setText(gagnant.getNom() + " commence la partie !");
            message.setTextFill(Rendu.couleur(gagnant.getCouleur()));
            return;
        }
        String texte = lanceur.isBot()
                ? lanceur.getNom() + " lance le dé…"
                : "À " + lanceur.getNom() + " de lancer le dé.";
        if (tirage.getManche() > 1) {
            String exAequo = tirage.getDerniereEgalite().stream()
                    .map(Joueur::getNom).collect(Collectors.joining(", "));
            texte = "Égalité à " + tirage.getValeurEgalite() + " entre " + exAequo
                    + " : ils relancent ! " + texte;
        }
        message.setText(texte);
        message.setTextFill(Rendu.couleur(lanceur.getCouleur()));
    }

    private void faireLancerLeBot() {
        if (pauseBot != null) {
            return;
        }
        pauseBot = new PauseTransition(DELAI_BOT);
        pauseBot.setOnFinished(event -> {
            pauseBot = null;
            lancer();
        });
        pauseBot.play();
    }

    @FXML
    private void onCommencer() {
        if (!tirage.estTermine()) {
            return;
        }
        jeu.definirPremierJoueur(tirage.getGagnant());
        try {
            PetitsChevauxApp.afficherPartie(getStage(), jeu);
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir la partie", e);
        }
    }

    @FXML
    private void onRetourMenu() {
        if (pauseBot != null) {
            pauseBot.stop();
            pauseBot = null;
        }
        try {
            PetitsChevauxApp.afficherMenu(getStage());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le menu", e);
        }
    }

    private Stage getStage() {
        return (Stage) cartes.getScene().getWindow();
    }
}
