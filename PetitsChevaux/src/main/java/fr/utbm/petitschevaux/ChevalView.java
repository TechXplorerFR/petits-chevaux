package fr.utbm.petitschevaux;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Pion d'un cheval sur la piste ou dans l'escalier.
 */
public class ChevalView {
    private final Circle pion;
    private FadeTransition clignotement;

    /**
     * Crée la vue d'un cheval.
     *
     * @param cheval cheval représenté
     * @param couleur couleur de son joueur
     * @param selection action appelée au clic
     */
    public ChevalView(Cheval cheval, Couleur couleur, Consumer<Cheval> selection) {
        this.pion = new Circle(12, Rendu.couleur(couleur));
        pion.setStroke(Color.WHITE);
        pion.setStrokeWidth(2);
        pion.setOnMouseClicked(event -> selection.accept(cheval));
    }

    /**
     * Retourne le pion JavaFX.
     *
     * @return pion du cheval
     */
    public Circle getNode() {
        return pion;
    }

    /**
     * Fait clignoter le pion s'il peut être joué.
     *
     * @param selectionnable vrai si le cheval peut être choisi
     */
    public void afficherSelection(boolean selectionnable) {
        arreter();
        pion.setCursor(selectionnable ? Cursor.HAND : Cursor.DEFAULT);
        if (selectionnable) {
            clignotement = new FadeTransition(Duration.millis(450), pion);
            clignotement.setFromValue(1);
            clignotement.setToValue(0.35);
            clignotement.setCycleCount(Animation.INDEFINITE);
            clignotement.setAutoReverse(true);
            clignotement.play();
        }
    }

    /**
     * Arrête l'animation du pion, à appeler avant de le retirer de la scène.
     */
    public void arreter() {
        if (clignotement != null) {
            clignotement.stop();
            clignotement = null;
        }
        pion.setOpacity(1);
    }
}
