package fr.utbm.petitschevaux.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.List;

/**
 * Petites animations de l'interface : boutons qui réagissent au survol et au clic,
 * apparition en cascade des éléments d'un écran.
 */
public final class Animations {
    private static final Duration DUREE_SURVOL = Duration.millis(120);
    private static final Duration DUREE_APPARITION = Duration.millis(380);
    private static final Duration DECALAGE_CASCADE = Duration.millis(55);
    private static final double ECHELLE_SURVOL = 1.05;
    private static final double ECHELLE_APPUI = 0.93;
    /** Rebond un peu au-delà de la taille de survol au relâchement du clic. */
    private static final Interpolator REBOND = new Interpolator() {
        @Override
        protected double curve(double t) {
            // « Ease out back » : dépasse légèrement la cible puis revient.
            double c = 2.2;
            double u = t - 1;
            return 1 + (c + 1) * u * u * u + c * u * u;
        }
    };

    private Animations() {
    }

    /**
     * Anime tous les boutons déjà présents sous ce nœud.
     *
     * @param racine racine d'un écran
     */
    public static void animerBoutons(Parent racine) {
        racine.applyCss();
        for (Node noeud : racine.lookupAll(".button")) {
            if (noeud instanceof ButtonBase bouton) {
                animerBouton(bouton);
            }
        }
    }

    /**
     * Le bouton grossit légèrement au survol, s'enfonce quand on clique
     * et rebondit au relâchement.
     *
     * @param bouton bouton à animer
     */
    public static void animerBouton(ButtonBase bouton) {
        if (bouton.getProperties().containsKey(Animations.class)) {
            return; // Déjà animé.
        }
        ScaleTransition echelle = new ScaleTransition(DUREE_SURVOL, bouton);
        bouton.getProperties().put(Animations.class, echelle);
        bouton.addEventHandler(MouseEvent.MOUSE_ENTERED,
                event -> redimensionner(echelle, ECHELLE_SURVOL, Interpolator.EASE_OUT));
        bouton.addEventHandler(MouseEvent.MOUSE_EXITED,
                event -> redimensionner(echelle, 1, Interpolator.EASE_OUT));
        bouton.addEventHandler(MouseEvent.MOUSE_PRESSED,
                event -> redimensionner(echelle, ECHELLE_APPUI, Interpolator.EASE_OUT));
        bouton.addEventHandler(MouseEvent.MOUSE_RELEASED,
                event -> redimensionner(echelle, bouton.isHover() ? ECHELLE_SURVOL : 1, REBOND));
        // Un bouton désactivé (pendant un survol par exemple) reprend sa taille normale.
        bouton.disabledProperty().addListener((obs, avant, desactive) -> {
            if (desactive) {
                redimensionner(echelle, 1, Interpolator.EASE_OUT);
            }
        });
    }

    private static void redimensionner(ScaleTransition echelle, double cible, Interpolator interpolateur) {
        echelle.stop();
        echelle.setDuration(interpolateur == REBOND ? DUREE_SURVOL.multiply(2.5) : DUREE_SURVOL);
        echelle.setInterpolator(interpolateur);
        echelle.setFromX(echelle.getNode().getScaleX());
        echelle.setFromY(echelle.getNode().getScaleY());
        echelle.setToX(cible);
        echelle.setToY(cible);
        echelle.play();
    }

    /**
     * Fait apparaître les nœuds les uns après les autres : fondu, léger glissement vers
     * le haut et petit grossissement.
     *
     * @param noeuds nœuds à faire apparaître, dans l'ordre
     */
    public static void apparitionEnCascade(List<? extends Node> noeuds) {
        for (int i = 0; i < noeuds.size(); i++) {
            Node noeud = noeuds.get(i);
            noeud.setOpacity(0);

            FadeTransition fondu = new FadeTransition(DUREE_APPARITION, noeud);
            fondu.setFromValue(0);
            fondu.setToValue(1);

            TranslateTransition glissement = new TranslateTransition(DUREE_APPARITION, noeud);
            glissement.setFromY(18);
            glissement.setToY(0);

            ScaleTransition zoom = new ScaleTransition(DUREE_APPARITION, noeud);
            zoom.setFromX(0.94);
            zoom.setFromY(0.94);
            zoom.setToX(1);
            zoom.setToY(1);

            for (var transition : List.of(fondu, glissement, zoom)) {
                transition.setInterpolator(Interpolator.EASE_OUT);
            }
            ParallelTransition apparition = new ParallelTransition(fondu, glissement, zoom);
            apparition.setDelay(DECALAGE_CASCADE.multiply(i));
            apparition.play();
        }
    }
}
