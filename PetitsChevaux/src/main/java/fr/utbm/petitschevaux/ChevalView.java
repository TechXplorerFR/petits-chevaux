package fr.utbm.petitschevaux;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Vue graphique indépendante d'un cheval.
 */
public class ChevalView {
    private final Cheval cheval;
    private final Couleur couleur;
    private final Circle pion;

    /**
     * Crée la vue d'un cheval.
     *
     * @param cheval cheval représenté
     * @param couleur couleur de son joueur
     * @param selection action appelée au clic
     */
    public ChevalView(Cheval cheval, Couleur couleur, Consumer<Cheval> selection) {
        this.cheval = cheval;
        this.couleur = couleur;
        this.pion = new Circle(12, couleurPour(couleur));
        pion.setStroke(Color.WHITE);
        pion.setStrokeWidth(2);
        pion.setOnMouseClicked(event -> selection.accept(this.cheval));
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
     * Met à jour l'état visuel du cheval.
     *
     * @param selectionnable vrai si le cheval peut être choisi
     */
    public void afficherSelection(boolean selectionnable) {
        arreterClignotement();
        if (selectionnable) {
            FadeTransition clignotement = new FadeTransition(Duration.millis(450), pion);
            clignotement.setFromValue(1);
            clignotement.setToValue(0.35);
            clignotement.setCycleCount(Animation.INDEFINITE);
            clignotement.setAutoReverse(true);
            pion.getProperties().put("clignotement", clignotement);
            clignotement.play();
        }
    }

    /**
     * Rend le cheval terminé visuellement grisé.
     */
    public void afficherTermine() {
        arreterClignotement();
        pion.setFill(Color.GRAY);
        pion.setOpacity(1);
    }

    private void arreterClignotement() {
        if (pion.getProperties().containsKey("clignotement")) {
            ((FadeTransition) pion.getProperties().remove("clignotement")).stop();
        }
        pion.setOpacity(1);
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
