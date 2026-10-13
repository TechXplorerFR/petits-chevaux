package fr.utbm.petitschevaux;

import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Vue 2D d'un dé affichant ses points.
 */
public class DeView {
    private final StackPane racine = new StackPane();
    private final GridPane points = new GridPane();

    /**
     * Crée une vue de dé initialisée sur zéro.
     */
    public DeView() {
        Rectangle fond = new Rectangle(70, 70, Color.WHITE);
        fond.setArcWidth(12);
        fond.setArcHeight(12);
        fond.setStroke(Color.DARKSLATEGRAY);
        fond.setStrokeWidth(2);
        points.setAlignment(Pos.CENTER);
        points.setHgap(8);
        points.setVgap(8);
        racine.getChildren().addAll(fond, points);
        afficher(0);
    }

    /**
     * Retourne le composant JavaFX.
     *
     * @return vue du dé
     */
    public StackPane getRoot() {
        return racine;
    }

    /**
     * Affiche une valeur comprise entre zéro et six.
     *
     * @param valeur valeur à afficher
     */
    public void afficher(int valeur) {
        points.getChildren().clear();
        if (valeur < 1 || valeur > 6) {
            return;
        }
        int[][] positions = {
                { 0, 0 }, { 1, 0 }, { 2, 0 },
                { 0, 1 }, { 1, 1 }, { 2, 1 },
                { 0, 2 }, { 1, 2 }, { 2, 2 }
        };
        int[][] pointsParFace = {
                {}, { 4 }, { 0, 8 }, { 0, 4, 8 },
                { 0, 2, 6, 8 }, { 0, 2, 4, 6, 8 }, { 0, 2, 3, 5, 6, 8 }
        };
        for (int index : pointsParFace[valeur]) {
            Circle point = new Circle(5, Color.DARKSLATEGRAY);
            points.add(point, positions[index][0], positions[index][1]);
        }
    }
}
