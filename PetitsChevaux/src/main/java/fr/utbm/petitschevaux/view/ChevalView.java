package fr.utbm.petitschevaux.view;

import fr.utbm.petitschevaux.model.Cheval;
import fr.utbm.petitschevaux.model.Couleur;

import java.util.function.Consumer;

/**
 * Pion d'un cheval sur la piste ou dans l'escalier.
 */
public class ChevalView {
    /** Hauteur du pion : un peu plus que la case pour bien voir la tête du cheval. */
    private static final double TAILLE_PION = 32;

    private final PionCheval pion;

    /**
     * Crée la vue d'un cheval.
     *
     * @param cheval cheval représenté
     * @param couleur couleur de son joueur
     * @param selection action appelée au clic
     */
    public ChevalView(Cheval cheval, Couleur couleur, Consumer<Cheval> selection) {
        this.pion = new PionCheval(Rendu.couleur(couleur), TAILLE_PION);
        pion.setOnMouseClicked(event -> selection.accept(cheval));
    }

    /**
     * Retourne le pion JavaFX.
     *
     * @return pion du cheval
     */
    public PionCheval getNode() {
        return pion;
    }

    /**
     * Place le centre du pion sur un point du plateau.
     *
     * @param x abscisse du centre
     * @param y ordonnée du centre
     */
    public void placer(double x, double y) {
        pion.setLayoutX(x);
        pion.setLayoutY(y);
    }

    /**
     * Fait pulser le pion s'il peut être joué.
     *
     * @param selectionnable vrai si le cheval peut être choisi
     */
    public void afficherSelection(boolean selectionnable) {
        pion.setJouable(selectionnable);
    }

    /**
     * Arrête l'animation du pion, à appeler avant de le retirer de la scène.
     */
    public void arreter() {
        pion.arreterPulsation();
    }
}
