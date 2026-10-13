package fr.utbm.petitschevaux;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Écurie d'un joueur, dessinée dans le coin haut-droit du plateau
 * (la rotation est appliquée par {@link PlateauView}). Un emplacement par cheval :
 * plein si le cheval est à l'écurie, grisé s'il est arrivé, vide s'il est en jeu.
 */
public class EcurieView {
    private static final double MAISON_X = 420;
    private static final double MAISON_Y = 20;
    private static final double MAISON_TAILLE = 160;
    private static final double MARGE = 20;
    private static final double RAYON_EMPLACEMENT = 17;

    private final Joueur joueur;
    private final Group dessin = new Group();
    private final Text nomJoueur = new Text();
    private final List<Circle> emplacements = new ArrayList<>();
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
     * Rafraîchit l'état des chevaux et indique ceux qui peuvent être joués.
     *
     * @param chevauxSelectionnables chevaux sélectionnables au prochain clic
     */
    public void actualiser(List<Cheval> chevauxSelectionnables) {
        this.chevauxSelectionnables = List.copyOf(chevauxSelectionnables);
        List<Cheval> chevaux = joueur.getChevaux();
        for (int i = 0; i < chevaux.size(); i++) {
            Cheval cheval = chevaux.get(i);
            Circle emplacement = emplacements.get(i);
            boolean selectionnable = this.chevauxSelectionnables.contains(cheval);
            emplacement.setFill(cheval.isTermine() ? Color.GRAY : Rendu.couleur(joueur.getCouleur()));
            emplacement.setVisible(!cheval.estEnJeu());
            emplacement.setStroke(selectionnable ? Color.BLACK : Color.WHITE);
            emplacement.setStrokeWidth(selectionnable ? 4 : 2);
            emplacement.setCursor(selectionnable ? Cursor.HAND : Cursor.DEFAULT);
        }
    }

    /**
     * Place le nom du joueur à côté de l'écurie et le garde horizontal
     * malgré la rotation du quartier.
     *
     * @param rotation rotation appliquée à l'écurie, en degrés
     * @param dessous vrai pour écrire le nom sous l'écurie, faux pour l'écrire au-dessus
     */
    public void orienterNom(double rotation, boolean dessous) {
        nomJoueur.applyCss();
        nomJoueur.setX(MAISON_X + MAISON_TAILLE / 2 - nomJoueur.getLayoutBounds().getWidth() / 2);
        nomJoueur.setY(dessous ? MAISON_Y + MAISON_TAILLE + 25 : MAISON_Y - 15);
        nomJoueur.setRotate(-rotation);
    }

    private void construire(Consumer<Cheval> selection) {
        Color couleur = Rendu.couleur(joueur.getCouleur());
        Rectangle maison = new Rectangle(MAISON_X, MAISON_Y, MAISON_TAILLE, MAISON_TAILLE);
        maison.setArcWidth(8);
        maison.setArcHeight(8);
        maison.setFill(couleur.deriveColor(0, 0.7, 1.15, 1));
        maison.setStroke(couleur.darker());
        maison.setStrokeWidth(3);

        double interieurTaille = MAISON_TAILLE - 2 * MARGE;
        Rectangle interieur = new Rectangle(MAISON_X + MARGE, MAISON_Y + MARGE,
                interieurTaille, interieurTaille);
        interieur.setArcWidth(12);
        interieur.setArcHeight(12);
        interieur.setFill(Color.WHITE);
        interieur.setStroke(couleur.darker());
        interieur.setStrokeWidth(2);

        nomJoueur.setText(joueur.getNom());
        nomJoueur.setFill(couleur);
        dessin.getChildren().addAll(maison, interieur, nomJoueur);

        double centreX = MAISON_X + MAISON_TAILLE / 2;
        double centreY = MAISON_Y + MAISON_TAILLE / 2;
        for (Cheval cheval : joueur.getChevaux()) {
            int i = cheval.getNumero();
            double x = centreX + (i < 2 ? -25 : 25);
            double y = centreY + (i % 2 == 0 ? -25 : 25);
            Circle emplacement = new Circle(x, y, RAYON_EMPLACEMENT, couleur);
            emplacement.setOnMouseClicked(event -> {
                if (chevauxSelectionnables.contains(cheval)) {
                    selection.accept(cheval);
                }
            });
            emplacements.add(emplacement);
            dessin.getChildren().add(emplacement);
        }
    }
}
