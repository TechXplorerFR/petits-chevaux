package fr.utbm.petitschevaux;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Constantes et utilitaires graphiques partagés par les vues du plateau.
 */
final class Rendu {
    /** Taille du plateau en pixels. */
    static final double TAILLE = 600;
    static final double CENTRE = TAILLE / 2;
    /** Distance entre les centres de deux cases voisines. */
    static final double PAS = 34;
    static final double TAILLE_CASE = 30;
    static final Color FOND = Color.web("#f4f0e7");
    static final Color TRAIT = Color.web("#514c45");

    private Rendu() {
    }

    /**
     * @param couleur couleur d'un joueur
     * @return teinte JavaFX correspondante
     */
    static Color couleur(Couleur couleur) {
        return switch (couleur) {
            case ROUGE -> Color.CRIMSON;
            case BLEU -> Color.DODGERBLUE;
            case JAUNE -> Color.GOLD;
            case VERT -> Color.LIMEGREEN;
        };
    }

    /**
     * Crée une case carrée centrée sur (x, y).
     */
    static Rectangle creerCase(double x, double y, Color remplissage) {
        Rectangle tuile = new Rectangle(
                x - TAILLE_CASE / 2,
                y - TAILLE_CASE / 2,
                TAILLE_CASE,
                TAILLE_CASE);
        tuile.setFill(remplissage);
        tuile.setStroke(TRAIT);
        tuile.setStrokeWidth(1.5);
        return tuile;
    }
}
