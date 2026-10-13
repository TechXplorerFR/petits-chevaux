package fr.utbm.petitschevaux;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlateauView {
    private static final double TAILLE = 600;
    private static final double CENTRE = TAILLE / 2;
    private static final double PAS = 34;
    private static final double TAILLE_CASE = 30;
    private static final int MARCHES = Plateau.NOMBRE_MARCHES;
    private static final int CASES_PAR_QUARTIER = Plateau.NOMBRE_CASES / 4;
    private static final int LONGUEUR_COULOIR = (CASES_PAR_QUARTIER - 2) / 2; // 6 si 56 cases
    private static final Couleur[] COULEURS_QUARTIERS = {
            Couleur.JAUNE, Couleur.ROUGE, Couleur.BLEU, Couleur.VERT
    };
    private static final double[][] CASES_QUART = {
            { 320, 200 }, { 360, 200 }, { 400, 200 },
            { 400, 240 }, { 400, 280 }, { 400, 320 }
    };
    private static final double[][] LIGNE_ARRIVEE = {
            { 300, 100 }, { 300, 132 }, { 300, 164 },
            { 300, 196 }, { 300, 228 }, { 300, 260 }
    };

    private final Plateau plateau;
    private final List<Joueur> joueurs;
    private final Pane racine = new Pane();
    private final List<double[]> positionsCases = new ArrayList<>();
    private final Map<Couleur, List<double[]>> positionsArrivee = new HashMap<>();

    public PlateauView(Plateau plateau) {
        this(plateau, List.of());
    }

    public PlateauView(Plateau plateau, List<Joueur> joueurs) {
        this.plateau = plateau;
        this.joueurs = joueurs;
        racine.setPrefSize(TAILLE, TAILLE);
        racine.setMinSize(TAILLE, TAILLE);
    }

    public Pane getRoot() {
        return racine;
    }

    public void dessinerPlateau() {
        racine.getChildren().clear();
        positionsCases.clear();
        positionsArrivee.clear();

        Rectangle fond = new Rectangle(TAILLE, TAILLE, Color.web("#f4f0e7"));
        fond.setStroke(Color.web("#514c45"));
        fond.setStrokeWidth(3);
        racine.getChildren().add(fond);

        dessinerCentre();

        for (int quartier = 0; quartier < COULEURS_QUARTIERS.length; quartier++) {
            Couleur couleur = COULEURS_QUARTIERS[quartier];
            Group dessinQuartier = creerQuartier(couleur);
            dessinQuartier.getTransforms().add(new Rotate(quartier * 90, CENTRE, CENTRE));
            racine.getChildren().add(dessinQuartier);

            for (int[] g : cheminBrasDuHaut()) {
                positionsCases.add(enPixels(tournerGrille(g, quartier)));
            }
            List<double[]> arrivee = new ArrayList<>();
            for (int i = 0; i < MARCHES; i++) {
                arrivee.add(enPixels(tournerGrille(new int[] { 0, -(MARCHES - i) }, quartier)));
            }
            positionsArrivee.put(couleur, arrivee);
        }

        dessinerAnneau();
        afficherChevaux();
    }

    public void afficherChevaux() {
        for (Joueur joueur : joueurs) {
            for (Cheval cheval : joueur.getChevaux()) {
                if (!cheval.isSorti() || cheval.isTermine()) {
                    continue;
                }

                double[] position;
                if (cheval.getPosition() < Plateau.NOMBRE_CASES) {
                    int index = Math.floorMod(
                            decalageDepart(joueur.getCouleur()) + cheval.getPosition() - 1,
                            positionsCases.size());
                    position = positionsCases.get(index);
                    racine.getChildren().add(creerPion(position[0], position[1], joueur.getCouleur()));
                } else {
                    List<double[]> ligne = positionsArrivee.get(joueur.getCouleur());
                    int index = cheval.getPosition() - Plateau.POSITION_BAS_ESCALIER;
                    if (ligne != null && index >= 0 && index < ligne.size()) {
                        position = ligne.get(index);
                        racine.getChildren().add(creerPion(position[0], position[1], joueur.getCouleur()));
                    }
                }
            }
        }
    }

    public void mettreAJourPlateau() {
        dessinerPlateau();
    }

    private Group creerQuartier(Couleur couleur) {
        Group quartier = new Group();
        Color teinte = couleurPour(couleur);
        Color fondTeinte = teinte.deriveColor(0, 0.7, 1.15, 1);

        Rectangle maison = new Rectangle(420, 20, 160, 160);
        maison.setArcWidth(8);
        maison.setArcHeight(8);
        maison.setFill(fondTeinte);
        maison.setStroke(teinte.darker());
        maison.setStrokeWidth(3);
        quartier.getChildren().add(maison);

        Rectangle ecurie = new Rectangle(440, 40, 120, 120);
        ecurie.setArcWidth(12);
        ecurie.setArcHeight(12);
        ecurie.setFill(Color.WHITE);
        ecurie.setStroke(teinte.darker());
        ecurie.setStrokeWidth(2);
        quartier.getChildren().add(ecurie);

        for (double x : new double[] { 475, 525 }) {
            for (double y : new double[] { 75, 125 }) {
                Circle placeCheval = new Circle(x, y, 17, teinte);
                placeCheval.setStroke(Color.WHITE);
                placeCheval.setStrokeWidth(2);
                quartier.getChildren().add(placeCheval);
            }
        }

        for (int i = 0; i < MARCHES; i++) {
            double[] p = enPixels(new int[] { 0, -(MARCHES - i) });
            quartier.getChildren().add(creerCase(p[0], p[1], fondTeinte));
        }

        return quartier;
    }

    private Rectangle creerCase(double x, double y, Color couleur) {
        return creerCase(x, y, couleur, TAILLE_CASE);
    }

    private Rectangle creerCase(double x, double y, Color couleur, double taille) {
        Rectangle tuile = new Rectangle(
                x - taille / 2,
                y - taille / 2,
                taille,
                taille);
        tuile.setFill(couleur);
        tuile.setStroke(Color.web("#514c45"));
        tuile.setStrokeWidth(1.5);
        return tuile;
    }

    private Circle creerPion(double x, double y, Couleur couleur) {
        Circle pion = new Circle(x, y, 12, couleurPour(couleur));
        pion.setStroke(Color.WHITE);
        pion.setStrokeWidth(2);
        return pion;
    }

    private void dessinerCentre() {
        Circle centreRond = new Circle(CENTRE, CENTRE, 16, Color.web("#f4f0e7"));
        centreRond.setStroke(Color.web("#514c45"));
        centreRond.setStrokeWidth(2);
        racine.getChildren().add(centreRond);
    }

    /**
     * Cases du bras du haut, dans le sens horaire : coin, couloir gauche, bout,
     * couloir droit.
     */
    private List<int[]> cheminBrasDuHaut() {
        int n = LONGUEUR_COULOIR;
        List<int[]> chemin = new ArrayList<>();
        chemin.add(new int[] { -1, -1 });
        for (int k = 2; k <= n + 1; k++)
            chemin.add(new int[] { -1, -k });
        chemin.add(new int[] { 0, -(n + 1) });
        for (int k = n + 1; k >= 2; k--)
            chemin.add(new int[] { 1, -k });
        return chemin;
    }

    private int[] tournerGrille(int[] g, int quart) {
        int x = g[0], y = g[1];
        for (int i = 0; i < quart; i++) {
            int ancienX = x;
            x = -y;
            y = ancienX;
        }
        return new int[] { x, y };
    }

    private double[] enPixels(int[] g) {
        return new double[] { CENTRE + g[0] * PAS, CENTRE + g[1] * PAS };
    }

    private int decalageDepart(Couleur couleur) {
        int quartier = Arrays.asList(COULEURS_QUARTIERS).indexOf(couleur);
        return Math.floorMod(quartier * CASES_PAR_QUARTIER + LONGUEUR_COULOIR + 3,
                Plateau.NOMBRE_CASES);
    }

    private void dessinerAnneau() {
        for (int i = 0; i < positionsCases.size(); i++) {
            double[] p = positionsCases.get(i);
            Color fond = Color.WHITE;
            for (Couleur couleur : COULEURS_QUARTIERS) {
                if (i == decalageDepart(couleur))
                    fond = couleurPour(couleur);
            }
            racine.getChildren().add(creerCase(p[0], p[1], fond));
        }
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
