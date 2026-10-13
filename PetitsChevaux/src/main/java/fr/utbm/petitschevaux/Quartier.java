package fr.utbm.petitschevaux;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Représente la zone de départ d'une couleur et son rendu graphique.
 *
 * <p>Un quartier possède sa propre vue graphique afin que {@link PlateauView}
 * ne soit responsable que de l'assemblage du plateau.</p>
 */
public class Quartier {
    private static final double CENTRE = 300;
    private static final int NOMBRE_MARCHES = Plateau.NOMBRE_MARCHES;
    private static final double TAILLE_CASE = 30;
    private static final double PAS = 34;

    private final Couleur couleur;
    private final Group dessin = new Group();

    /**
     * Crée un quartier pour une couleur donnée.
     *
     * @param couleur couleur du quartier
     * @throws IllegalArgumentException si la couleur est absente
     */
    public Quartier(Couleur couleur) {
        if (couleur == null) {
            throw new IllegalArgumentException("La couleur d'un quartier est obligatoire.");
        }
        this.couleur = couleur;
        construireDessin();
    }

    /**
     * Retourne la couleur associée au quartier.
     *
     * @return couleur du quartier
     */
    public Couleur getCouleur() {
        return couleur;
    }

    /**
     * Retourne le groupe JavaFX représentant le quartier.
     *
     * @return dessin du quartier
     */
    public Group getDessin() {
        return dessin;
    }

    private void construireDessin() {
        Color teinte = couleurPour(couleur);
        Color fondTeinte = teinte.deriveColor(0, 0.7, 1.15, 1);
        Color fondEscalier = fondTeinte.interpolate(Color.WHITE, 0.35);

        for (int i = 0; i < NOMBRE_MARCHES; i++) {
            double x = CENTRE;
            double y = CENTRE - (NOMBRE_MARCHES - i) * PAS;
            dessin.getChildren().add(creerCase(x, y, fondEscalier));
        }
    }

    private Rectangle creerCase(double x, double y, Color couleurCase) {
        Rectangle tuile = new Rectangle(
                x - TAILLE_CASE / 2,
                y - TAILLE_CASE / 2,
                TAILLE_CASE,
                TAILLE_CASE);
        tuile.setFill(couleurCase);
        tuile.setStroke(Color.web("#514c45"));
        tuile.setStrokeWidth(1.5);
        return tuile;
    }

    private Color couleurPour(Couleur couleur) {
        return switch (couleur) {
            case ROUGE -> Color.CRIMSON;
            case BLEU -> Color.DODGERBLUE;
            case JAUNE -> Color.GOLD;
            case VERT -> Color.LIMEGREEN;
        };
    }

}
