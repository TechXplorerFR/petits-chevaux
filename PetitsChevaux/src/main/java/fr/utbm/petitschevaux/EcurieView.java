package fr.utbm.petitschevaux;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * Vue d'une écurie et des quatre chevaux qui lui sont associés.
 */
public class EcurieView {
    private final Joueur joueur;
    private final Group dessin = new Group();
    private final Text nomJoueur = new Text();
    private List<Cheval> chevauxSelectionnables = List.of();

    /**
     * Crée une écurie.
     *
     * @param joueur joueur propriétaire de l'écurie
     * @param selection action appelée lorsqu'un cheval est sélectionné
     */
    public EcurieView(Joueur joueur, Consumer<Cheval> selection) {
        this.joueur = joueur;
        construire(selection);
    }

    /**
     * Retourne le groupe de l'écurie.
     *
     * @return dessin de l'écurie
     */
    public Group getDessin() {
        return dessin;
    }

    /**
     * Rafraîchit les chevaux présents dans l'écurie.
     *
     * @param selectionnable cheval pouvant être choisi
     */
    public void actualiser(Cheval selectionnable) {
        actualiser(selectionnable == null ? List.of() : List.of(selectionnable));
    }

    /**
     * Rafraîchit l'état des chevaux et indique ceux qui peuvent être joués.
     *
     * @param chevauxSelectionnables chevaux sélectionnables au prochain clic
     */
    public void actualiser(List<Cheval> chevauxSelectionnables) {
        this.chevauxSelectionnables = chevauxSelectionnables == null
                ? List.of() : List.copyOf(chevauxSelectionnables);
        List<Cheval> chevaux = joueur.getChevaux();
        for (int i = 0; i < chevaux.size(); i++) {
            Cheval cheval = chevaux.get(i);
            Circle emplacement = (Circle) dessin.getChildren().get(i + 3);
            emplacement.setFill(cheval.isTermine() ? Color.GRAY : couleurPour(joueur.getCouleur()));
            emplacement.setVisible(!cheval.isSorti() || cheval.isTermine());
            if (this.chevauxSelectionnables.contains(cheval)) {
                emplacement.setStroke(Color.BLACK);
                emplacement.setStrokeWidth(4);
            } else {
                emplacement.setStroke(Color.WHITE);
                emplacement.setStrokeWidth(2);
            }
        }
    }

    private void construire(Consumer<Cheval> selection) {
        Color couleur = couleurPour(joueur.getCouleur());
        Rectangle maison = new Rectangle(420, 20, 160, 160);
        maison.setFill(couleur.deriveColor(0, 0.7, 1.15, 1));
        maison.setStroke(couleur.darker());
        maison.setStrokeWidth(3);
        dessin.getChildren().addAll(maison,
                new Rectangle(440, 40, 120, 120));
        nomJoueur.setText(joueur.getNom());
        nomJoueur.setFill(couleur);
        dessin.getChildren().add(nomJoueur);
        double[] xs = { 475, 525 };
        double[] ys = { 75, 125 };
        int index = 0;
        for (double x : xs) {
            for (double y : ys) {
                final int chevalIndex = index++;
                Circle emplacement = new Circle(x, y, 17, couleur);
                emplacement.setStroke(Color.WHITE);
                emplacement.setStrokeWidth(2);
                emplacement.setOnMouseClicked(event -> {
                    if (chevalIndex < joueur.getChevaux().size()) {
                        Cheval cheval = joueur.getChevaux().get(chevalIndex);
                        if (chevauxSelectionnables.contains(cheval)) {
                            selection.accept(cheval);
                        }
                    }
                });
                dessin.getChildren().add(emplacement);
            }
        }
    }

    /**
     * Maintient le nom horizontal lorsque l'écurie est tournée sur le plateau.
     *
     * @param rotation rotation appliquée à l'écurie
     */
    public void orienterNom(double rotation, boolean dessous) {
        nomJoueur.applyCss();
        nomJoueur.setX(500 - nomJoueur.getLayoutBounds().getWidth() / 2);
        nomJoueur.setY(dessous ? 205 : 5);
        nomJoueur.setRotate(-rotation);
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
