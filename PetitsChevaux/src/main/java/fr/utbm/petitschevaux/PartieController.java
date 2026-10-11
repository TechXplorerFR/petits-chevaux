package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;

public class PartieController {
    private Jeu jeu;
    @FXML
    private Label information;

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
        if (information != null) {
            Joueur joueur = jeu.getJoueurs().get(jeu.getJoueurCourant());
            information.setText(joueur.getNom() + " - lancez le dé");
        }
    }
}
