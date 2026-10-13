package fr.utbm.petitschevaux;

import javafx.scene.Group;
import javafx.scene.paint.Color;

/**
 * Escalier d'une couleur, dessiné pour le quartier du haut
 * (la rotation est appliquée par {@link PlateauView}).
 */
public class Quartier {
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
        Color fondEscalier = Rendu.couleur(couleur)
                .deriveColor(0, 0.7, 1.15, 1)
                .interpolate(Color.WHITE, 0.35);
        for (int i = 0; i < Plateau.NOMBRE_MARCHES; i++) {
            double y = Rendu.CENTRE - (Plateau.NOMBRE_MARCHES - i) * Rendu.PAS;
            dessin.getChildren().add(Rendu.creerCase(Rendu.CENTRE, y, fondEscalier));
        }
    }
}
