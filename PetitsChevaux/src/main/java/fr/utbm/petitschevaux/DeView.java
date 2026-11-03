package fr.utbm.petitschevaux;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Random;

/**
 * Vue 2D d'un dé affichant ses points.
 *
 * <p>{@link #lancer(int, Runnable)} fait défiler des faces au hasard, de plus en plus
 * lentement, avant de s'arrêter sur le résultat (comme une roulette).</p>
 */
public class DeView {
    /** Nombre de faces affichées avant le résultat. */
    private static final int FACES_DEFILEES = 14;
    /** Durée d'affichage de la première face, puis de la dernière : le dé ralentit. */
    private static final double INTERVALLE_DEBUT_MS = 30;
    private static final double INTERVALLE_FIN_MS = 130;
    private static final double SECOUSSE_MAX_DEGRES = 14;
    private static final Color COULEUR_POINT = Color.DARKSLATEGRAY;

    private static final int[][] POSITIONS = {
            { 0, 0 }, { 1, 0 }, { 2, 0 },
            { 0, 1 }, { 1, 1 }, { 2, 1 },
            { 0, 2 }, { 1, 2 }, { 2, 2 }
    };
    private static final int[][] POINTS_PAR_FACE = {
            {}, { 4 }, { 0, 8 }, { 0, 4, 8 },
            { 0, 2, 6, 8 }, { 0, 2, 4, 6, 8 }, { 0, 2, 3, 5, 6, 8 }
    };

    private final StackPane racine = new StackPane();
    private final GridPane points = new GridPane();
    private final Rectangle fond = new Rectangle(70, 70, Color.WHITE);
    private final Random hasard = new Random();
    /** Lancer en cours, ou {@code null}. */
    private Animation animation;

    /**
     * Crée une vue de dé initialisée sur zéro.
     */
    public DeView() {
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
     * Affiche directement une valeur comprise entre zéro et six (zéro : dé vide),
     * en interrompant un éventuel lancer.
     *
     * @param valeur valeur à afficher
     */
    public void afficher(int valeur) {
        arreter();
        dessinerFace(valeur);
    }

    /**
     * Anime un lancer : les faces défilent au hasard en ralentissant, le dé tremble,
     * puis il s'arrête sur {@code valeur} avec un petit rebond.
     *
     * @param valeur résultat à afficher à la fin
     * @param fin action lancée une fois le résultat affiché (peut être {@code null})
     */
    public void lancer(int valeur, Runnable fin) {
        arreter();
        Timeline defilement = new Timeline();
        double temps = 0;
        int face = 0;
        for (int i = 0; i < FACES_DEFILEES; i++) {
            face = faceDifferente(face, i == FACES_DEFILEES - 1 ? valeur : 0);
            int faceAffichee = face;
            double inclinaison = (hasard.nextDouble() * 2 - 1) * SECOUSSE_MAX_DEGRES
                    * (1 - (double) i / FACES_DEFILEES);
            defilement.getKeyFrames().add(new KeyFrame(Duration.millis(temps), event -> {
                dessinerFace(faceAffichee);
                racine.setRotate(inclinaison);
            }));
            // Interpolation quadratique : les faces s'enchaînent vite puis ralentissent.
            double progression = (double) i / (FACES_DEFILEES - 1);
            temps += INTERVALLE_DEBUT_MS
                    + (INTERVALLE_FIN_MS - INTERVALLE_DEBUT_MS) * progression * progression;
        }
        // Dernière image : le résultat, le dé bien droit.
        defilement.getKeyFrames().add(new KeyFrame(Duration.millis(temps), event -> {
            dessinerFace(valeur);
            racine.setRotate(0);
        }));
        // Le dé est un peu plus gros pendant qu'il roule.
        defilement.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(racine.scaleXProperty(), 1),
                        new KeyValue(racine.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(120),
                        new KeyValue(racine.scaleXProperty(), 1.12, Interpolator.EASE_OUT),
                        new KeyValue(racine.scaleYProperty(), 1.12, Interpolator.EASE_OUT)));

        ScaleTransition rebond = new ScaleTransition(Duration.millis(260), racine);
        rebond.setFromX(1.3);
        rebond.setFromY(1.3);
        rebond.setToX(1);
        rebond.setToY(1);
        rebond.setInterpolator(Interpolator.EASE_OUT);

        SequentialTransition lancer = new SequentialTransition(defilement, rebond);
        lancer.setOnFinished(event -> {
            animation = null;
            fond.setStroke(Color.DARKSLATEGRAY);
            if (fin != null) {
                fin.run();
            }
        });
        animation = lancer;
        fond.setStroke(Color.web("#c9a227"));
        lancer.play();
    }

    /**
     * @return vrai pendant l'animation d'un lancer
     */
    public boolean estEnCours() {
        return animation != null;
    }

    /**
     * Interrompt le lancer en cours sans exécuter son action de fin.
     */
    public void arreter() {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
        racine.setRotate(0);
        racine.setScaleX(1);
        racine.setScaleY(1);
        fond.setStroke(Color.DARKSLATEGRAY);
    }

    /**
     * Tire une face au hasard, différente de la précédente (sinon on ne verrait pas
     * le changement) et de la face finale pour l'avant-dernière image.
     */
    private int faceDifferente(int precedente, int aEviter) {
        int face;
        do {
            face = 1 + hasard.nextInt(6);
        } while (face == precedente || face == aEviter);
        return face;
    }

    private void dessinerFace(int valeur) {
        points.getChildren().clear();
        if (valeur < 1 || valeur > 6) {
            return;
        }
        for (int index : POINTS_PAR_FACE[valeur]) {
            points.add(new Circle(5, COULEUR_POINT), POSITIONS[index][0], POSITIONS[index][1]);
        }
    }
}
