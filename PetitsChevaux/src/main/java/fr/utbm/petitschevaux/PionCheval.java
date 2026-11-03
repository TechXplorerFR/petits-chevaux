package fr.utbm.petitschevaux;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

/**
 * Pion en forme de tête de cheval stylisée (façon cavalier d'échecs), dessiné en vectoriel
 * pour rester net à toutes les tailles.
 *
 * <p>Le dessin est centré sur l'origine du groupe : on le place avec
 * {@link #setLayoutX(double)} / {@link #setLayoutY(double)} sur le centre voulu.</p>
 */
public class PionCheval extends Group {
    /** Taille du dessin d'origine (carré de 100 × 100). */
    private static final double TAILLE_DESSIN = 100;

    /** Socle : plinthe puis collier. */
    private static final String SOCLE =
            "M 16 97 Q 16 89 24 89 L 76 89 Q 84 89 84 97 Z "
            + "M 24 89 L 27 81 L 73 81 L 76 89 Z";

    /** Silhouette : poitrail, gorge, museau, front, oreille, crinière, encolure. */
    private static final String SILHOUETTE =
            "M 29 81 "
            + "C 28 70 33 61 43 54 "
            + "C 36 55 29 59 22 61 "
            + "C 15 63 8 59 10 52 "
            + "C 11 47 15 44 19 40 "
            + "C 24 34 27 28 31 23 "
            + "L 33 10 "
            + "L 41 19 "
            + "C 60 15 78 29 79 52 "
            + "C 80 64 76 74 72 81 Z";

    private static final String CRINIERE = "M 46 23 C 62 25 71 38 70 56 C 69 66 67 73 64 79";
    private static final String BOUCHE = "M 11 54 C 14 55 17 55 20 54";

    private final SVGPath socle = new SVGPath();
    private final SVGPath silhouette = new SVGPath();
    private final SVGPath criniere = new SVGPath();
    private final DropShadow ombre = new DropShadow(4, 0, 1.5, Color.rgb(0, 0, 0, 0.45));
    /** Pulsation d'un cheval jouable, ou {@code null} s'il est au repos. */
    private Animation pulsation;

    /**
     * Crée un pion.
     *
     * @param couleur couleur du joueur
     * @param taille hauteur du pion en pixels
     */
    public PionCheval(Color couleur, double taille) {
        socle.setContent(SOCLE);
        silhouette.setContent(SILHOUETTE);
        criniere.setContent(CRINIERE);
        criniere.setFill(null);

        SVGPath bouche = new SVGPath();
        bouche.setContent(BOUCHE);
        bouche.setFill(null);
        bouche.setStroke(Rendu.TRAIT);
        bouche.setStrokeWidth(2.5);
        bouche.setStrokeLineCap(StrokeLineCap.ROUND);

        Circle oeil = new Circle(30, 35, 4.2, Color.web("#1f1c19"));
        Circle reflet = new Circle(31.3, 33.7, 1.3, Color.WHITE);
        Circle naseau = new Circle(15, 49, 2.2, Color.web("#1f1c19"));

        for (SVGPath trace : new SVGPath[] { socle, silhouette, criniere }) {
            trace.setStrokeLineJoin(StrokeLineJoin.ROUND);
            trace.setStrokeLineCap(StrokeLineCap.ROUND);
        }

        Group dessin = new Group(socle, silhouette, criniere, bouche, oeil, reflet, naseau);
        double echelle = taille / TAILLE_DESSIN;
        dessin.getTransforms().addAll(new Scale(echelle, echelle),
                new Translate(-TAILLE_DESSIN / 2, -TAILLE_DESSIN / 2));
        getChildren().add(dessin);

        // Toute la case du pion est cliquable, pas seulement le tracé.
        setPickOnBounds(true);
        setEffect(ombre);
        setCouleur(couleur);
        setMisEnAvant(false);
    }

    /**
     * Change la couleur du pion (par exemple gris pour un cheval arrivé).
     *
     * @param couleur nouvelle teinte
     */
    public final void setCouleur(Color couleur) {
        Color contour = couleur.deriveColor(0, 1, 0.45, 1);
        LinearGradient degrade = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, couleur.deriveColor(0, 0.75, 1.35, 1)),
                new Stop(0.55, couleur),
                new Stop(1, couleur.deriveColor(0, 1, 0.75, 1)));
        silhouette.setFill(degrade);
        silhouette.setStroke(contour);
        silhouette.setStrokeWidth(4);
        socle.setFill(couleur.deriveColor(0, 1, 0.8, 1));
        socle.setStroke(contour);
        socle.setStrokeWidth(4);
        criniere.setStroke(contour.deriveColor(0, 1, 1, 0.55));
        criniere.setStrokeWidth(3);
    }

    /**
     * Indique si le cheval peut être joué : halo, curseur main et pulsation
     * (il grossit et s'estompe en boucle). Sinon, le pion revient au repos.
     *
     * @param jouable vrai si le cheval peut être choisi
     */
    public void setJouable(boolean jouable) {
        arreterPulsation();
        setMisEnAvant(jouable);
        setCursor(jouable ? Cursor.HAND : Cursor.DEFAULT);
        if (!jouable) {
            return;
        }
        Duration demiPeriode = Duration.millis(450);
        FadeTransition fondu = new FadeTransition(demiPeriode, this);
        fondu.setFromValue(1);
        fondu.setToValue(0.55);
        ScaleTransition zoom = new ScaleTransition(demiPeriode, this);
        zoom.setFromX(1);
        zoom.setFromY(1);
        zoom.setToX(1.18);
        zoom.setToY(1.18);
        pulsation = new ParallelTransition(fondu, zoom);
        pulsation.setCycleCount(Animation.INDEFINITE);
        pulsation.setAutoReverse(true);
        pulsation.play();
    }

    /**
     * Arrête la pulsation et remet le pion à sa taille et son opacité normales.
     * À appeler avant de retirer le pion de la scène.
     */
    public void arreterPulsation() {
        if (pulsation != null) {
            pulsation.stop();
            pulsation = null;
        }
        setOpacity(1);
        setScaleX(1);
        setScaleY(1);
    }

    /**
     * Entoure le pion d'un halo lorsqu'il peut être joué.
     *
     * @param misEnAvant vrai pour afficher le halo
     */
    public final void setMisEnAvant(boolean misEnAvant) {
        if (misEnAvant) {
            ombre.setColor(Color.web("#1f1c19"));
            ombre.setRadius(12);
            ombre.setSpread(0.6);
            ombre.setOffsetY(0);
        } else {
            ombre.setColor(Color.rgb(0, 0, 0, 0.45));
            ombre.setRadius(4);
            ombre.setSpread(0);
            ombre.setOffsetY(1.5);
        }
    }
}
