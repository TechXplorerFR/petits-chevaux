package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.util.List;

public class PartieController {
    private Jeu jeu;
    private PlateauView plateauView;

    @FXML
    private Pane zonePlateau;

    @FXML
    private Label information;

    @FXML private Label resultatDe;

    public PartieController() {
        this.initialiser();
    }

    public void initialiser() {
        jeu = new Jeu(List.of(
                new Joueur("Joueur 1", Couleur.ROUGE),
                new Joueur("Joueur 2", Couleur.BLEU)));
        jeu.demarrerPartie();
    }

    @FXML
    private void initialize() {
        plateauView = new PlateauView(jeu.getPlateau(), jeu.getJoueurs());
        zonePlateau.getChildren().setAll(plateauView.getRoot());
        actualiserVue();
    }

    @FXML
    public void onLancerDe() {
        jeu.jouerTour();
        actualiserVue();
    }

    @FXML
    public void onChoisirCheval() {
        // Le premier cheval déplaçable est choisi automatiquement dans cette version.
        actualiserVue();
    }

    public void actualiserVue() {
        if (plateauView != null) {
            plateauView.dessinerPlateau();
        }
        if (information != null) {
            Joueur joueur = jeu.getJoueurs().get(jeu.getJoueurCourant());
            information.setText(joueur.getNom() + " - lancez le dé");
        }
        if (resultatDe != null) {
            int valeurDe = jeu.getDe().getValeur();
            resultatDe.setText("Résultat du dé : " + valeurDe);
        }
    }
}
