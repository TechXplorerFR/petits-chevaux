package fr.utbm.petitschevaux;

import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

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
     * @param couleur couleur d'un joueur
     * @return la teinte au format CSS (#rrggbb), pour les styles JavaFX
     */
    static String css(Couleur couleur) {
        Color teinte = couleur(couleur);
        return String.format("#%02x%02x%02x", Math.round(teinte.getRed() * 255),
                Math.round(teinte.getGreen() * 255), Math.round(teinte.getBlue() * 255));
    }

    /**
     * @return la couleur de fond d'une case spéciale selon sa catégorie
     *         (teintes pâles pour ne pas se confondre avec les couleurs des joueurs)
     */
    static Color couleur(CaseSpeciale.Categorie categorie) {
        return switch (categorie) {
            case BONUS -> Color.web("#bfe6c8");
            case MALUS -> Color.web("#f4c2c2");
            case NEUTRE -> Color.web("#d9cff3");
        };
    }

    /**
     * Crée une case spéciale centrée sur (x, y) : fond selon la catégorie, symbole de l'effet
     * et infobulle avec sa description.
     */
    static Group creerCaseSpeciale(double x, double y, CaseSpeciale effet) {
        Rectangle tuile = creerCase(x, y, couleur(effet.getCategorie()));
        Text symbole = new Text(effet.getSymbole());
        symbole.setFont(Font.font("System", FontWeight.BOLD, 13));
        symbole.setFill(TRAIT);
        symbole.setX(x - symbole.getLayoutBounds().getWidth() / 2);
        symbole.setY(y + symbole.getLayoutBounds().getHeight() / 4 + 1);
        Group groupe = new Group(tuile, symbole);
        Tooltip.install(groupe, new Tooltip(effet.getNom() + " : " + effet.getDescription()));
        return groupe;
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
