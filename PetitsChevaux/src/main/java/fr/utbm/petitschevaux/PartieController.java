package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class PartieController {
    private Jeu jeu;
    private PlateauView plateauView;

    @FXML
    private Pane zonePlateau;

    @FXML
    private Label information;

    @FXML private Label resultatDe;
    @FXML private VBox historiqueLancers;
    @FXML private VBox joueursAffiches;
    @FXML private StackPane vueDe;
    private final Deque<String> historique = new ArrayDeque<>();
    private DeView deView;

    public PartieController() {
        this.initialiser();
    }

    public void initialiser() {
        jeu = new Jeu(List.of(
                new Joueur("Toto", Couleur.ROUGE),
                new Joueur("biloute", Couleur.BLEU)));
        jeu.demarrerPartie();
    }

    @FXML
    private void initialize() {
        plateauView = new PlateauView(jeu.getPlateau(), jeu.getJoueurs(),
                this::onChevalSelectionne);
        zonePlateau.getChildren().setAll(plateauView.getRoot());
        deView = new DeView();
        vueDe.getChildren().setAll(deView.getRoot());
        afficherJoueurs();
        actualiserVue();
    }

    @FXML
    public void onLancerDe() {
        if (!jeu.getChevauxDeplacables().isEmpty()) {
            effectuerDeplacementParDefaut();
            return;
        }
        Joueur joueur = jeu.getJoueurs().get(jeu.getJoueurCourant());
        int valeur = jeu.lancerDe();
        historique.addFirst(joueur.getNom() + " : " + valeur);
        while (historique.size() > 7) {
            historique.removeLast();
        }
        List<Cheval> deplacables = jeu.getChevauxDeplacables();
        if (deplacables.isEmpty()) {
            jeu.passerAuJoueurSuivant();
            jouerLesBots();
        }
        plateauView.afficherPossibilites(deplacables);
        actualiserVue();
    }

    private void effectuerDeplacementParDefaut() {
        List<Cheval> deplacables = jeu.getChevauxDeplacables();
        Cheval cheval = deplacables.stream()
                .max((premier, second) -> Integer.compare(
                        progression(premier), progression(second)))
                .orElse(null);
        if (cheval != null && jeu.jouerCheval(cheval)) {
            plateauView.afficherPossibilites(List.of());
            jouerLesBots();
            actualiserVue();
        }
    }

    private int progression(Cheval cheval) {
        return cheval.isSorti() ? cheval.getPosition() : -1;
    }

    private void jouerLesBots() {
        int tours = 0;
        while (jeu.getJoueurs().get(jeu.getJoueurCourant()).isBot() && tours < 100) {
            Joueur bot = jeu.getJoueurs().get(jeu.getJoueurCourant());
            int valeur = jeu.jouerTourBot();
            historique.addFirst(bot.getNom() + " : " + valeur);
            while (historique.size() > 7) {
                historique.removeLast();
            }
            tours++;
        }
    }

    private void onChevalSelectionne(Cheval cheval) {
        if (jeu.jouerCheval(cheval)) {
            plateauView.afficherPossibilites(List.of());
            jouerLesBots();
            actualiserVue();
        }
    }

    public void actualiserVue() {
        if (plateauView != null) {
            plateauView.dessinerPlateau();
        }
        if (information != null) {
            Joueur joueur = jeu.getJoueurs().get(jeu.getJoueurCourant());
            if (!jeu.getChevauxDeplacables().isEmpty()) {
                information.setText(joueur.getNom()
                        + " - choisissez un cheval marqué en noir");
            } else {
                information.setText(joueur.getNom() + " - lancez le dé");
            }
        }
        if (resultatDe != null) {
            int valeurDe = jeu.getDe().getValeur();
            resultatDe.setText("Résultat du dé : " + valeurDe);
            deView.afficher(valeurDe);
        }
        afficherHistorique();
    }

    private void afficherHistorique() {
        if (historiqueLancers == null) {
            return;
        }
        historiqueLancers.getChildren().clear();
        historiqueLancers.getChildren().add(new Label("7 derniers lancers"));
        for (String lancer : historique) {
            historiqueLancers.getChildren().add(new Label("• " + lancer));
        }
    }

    private void afficherJoueurs() {
        if (joueursAffiches == null) {
            return;
        }
        joueursAffiches.getChildren().clear();
        joueursAffiches.getChildren().add(new Label("Joueurs"));
        for (int i = 0; i < 4; i++) {
            String nom = jeu.getJoueurs().get(i).getNom();
            Label joueur = new Label(nom);
            joueur.setTextFill(couleurPour(i < jeu.getJoueurs().size()
                    ? jeu.getJoueurs().get(i).getCouleur()
                    : Couleur.values()[i]));
            joueursAffiches.getChildren().add(joueur);
        }
    }

    private javafx.scene.paint.Color couleurPour(Couleur couleur) {
        return switch (couleur) {
            case ROUGE -> javafx.scene.paint.Color.CRIMSON;
            case BLEU -> javafx.scene.paint.Color.DODGERBLUE;
            case JAUNE -> javafx.scene.paint.Color.GOLD;
            case VERT -> javafx.scene.paint.Color.LIMEGREEN;
        };
    }
}
