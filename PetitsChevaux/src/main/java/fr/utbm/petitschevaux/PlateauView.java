package fr.utbm.petitschevaux;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Vue du plateau complet : piste, quatre quartiers (escalier + écurie) et chevaux.
 *
 * <p>Chaque quartier est dessiné une fois « en haut » puis tourné de 90° par couleur,
 * dans l'ordre de {@link Couleur}.</p>
 */
public class PlateauView {
    private static final int LONGUEUR_COULOIR = (Plateau.CASES_PAR_QUARTIER - 2) / 2;

    private final Plateau plateau;
    private final List<Joueur> joueurs;
    private final Consumer<Cheval> selection;
    private final Map<Couleur, Quartier> quartiers = new EnumMap<>(Couleur.class);
    private final Map<Couleur, EcurieView> ecuries = new EnumMap<>(Couleur.class);
    private final Pane racine = new Pane();
    /** Centre de chaque case de la piste, indexé par position absolue. */
    private final List<double[]> positionsCases = new ArrayList<>();
    /** Centre de chaque marche, par couleur. */
    private final Map<Couleur, List<double[]>> positionsMarches = new EnumMap<>(Couleur.class);
    private final List<ChevalView> vuesChevaux = new ArrayList<>();
    private List<Cheval> chevauxDeplacables = List.of();
    private int valeurDe;

    /**
     * Crée la vue du plateau.
     *
     * @param plateau plateau métier
     * @param joueurs joueurs de la partie (un par couleur)
     * @param selection action appelée lors du clic sur un cheval
     */
    public PlateauView(Plateau plateau, List<Joueur> joueurs, Consumer<Cheval> selection) {
        this.plateau = plateau;
        this.joueurs = List.copyOf(joueurs);
        this.selection = selection;
        for (Joueur joueur : joueurs) {
            quartiers.put(joueur.getCouleur(), new Quartier(joueur.getCouleur()));
            ecuries.put(joueur.getCouleur(), new EcurieView(joueur, selection));
        }
        calculerPositions();
        racine.setPrefSize(Rendu.TAILLE, Rendu.TAILLE);
        racine.setMinSize(Rendu.TAILLE, Rendu.TAILLE);
    }

    public Pane getRoot() {
        return racine;
    }

    /**
     * Définit les chevaux jouables et la valeur du dé, puis redessine le plateau.
     *
     * @param chevaux chevaux déplaçables après le lancer
     * @param valeurDe valeur du dé utilisée pour calculer les destinations
     */
    public void afficherPossibilites(List<Cheval> chevaux, int valeurDe) {
        this.chevauxDeplacables = List.copyOf(chevaux);
        this.valeurDe = valeurDe;
        dessinerPlateau();
    }

    public void dessinerPlateau() {
        vuesChevaux.forEach(ChevalView::arreter);
        vuesChevaux.clear();
        racine.getChildren().clear();

        Rectangle fond = new Rectangle(Rendu.TAILLE, Rendu.TAILLE, Rendu.FOND);
        fond.setStroke(Rendu.TRAIT);
        fond.setStrokeWidth(3);
        racine.getChildren().add(fond);

        Circle centre = new Circle(Rendu.CENTRE, Rendu.CENTRE, 16, Rendu.FOND);
        centre.setStroke(Rendu.TRAIT);
        centre.setStrokeWidth(2);
        racine.getChildren().add(centre);

        for (Couleur couleur : Couleur.values()) {
            double rotation = couleur.ordinal() * 90;
            Group escalier = quartiers.get(couleur).getDessin();
            escalier.getTransforms().setAll(new Rotate(rotation, Rendu.CENTRE, Rendu.CENTRE));
            EcurieView ecurie = ecuries.get(couleur);
            ecurie.getDessin().getTransforms().setAll(new Rotate(rotation, Rendu.CENTRE, Rendu.CENTRE));
            boolean nomDessous = rotation == 0 || rotation == 270;
            ecurie.orienterNom(rotation, nomDessous);
            ecurie.actualiser(chevauxDeplacables);
            racine.getChildren().addAll(escalier, ecurie.getDessin());
        }

        dessinerPiste();
        dessinerChevaux();
        dessinerDestinations();
    }

    /**
     * Calcule une fois pour toutes le centre des cases de la piste et des marches.
     */
    private void calculerPositions() {
        for (Couleur couleur : Couleur.values()) {
            int quart = couleur.ordinal();
            for (int[] g : cheminBrasDuHaut()) {
                positionsCases.add(enPixels(tournerGrille(g, quart)));
            }
            List<double[]> marches = new ArrayList<>();
            for (int i = 0; i < Plateau.NOMBRE_MARCHES; i++) {
                marches.add(enPixels(tournerGrille(new int[] { 0, -(Plateau.NOMBRE_MARCHES - i) }, quart)));
            }
            positionsMarches.put(couleur, marches);
        }
    }

    private void dessinerChevaux() {
        for (Joueur joueur : joueurs) {
            for (Cheval cheval : joueur.getChevaux()) {
                double[] position = coordonnees(joueur, cheval.getPosition());
                if (!cheval.estEnJeu() || position == null) {
                    continue;
                }
                ChevalView chevalView = new ChevalView(cheval, joueur.getCouleur(), selection);
                chevalView.afficherSelection(chevauxDeplacables.contains(cheval));
                chevalView.getNode().setCenterX(position[0]);
                chevalView.getNode().setCenterY(position[1]);
                vuesChevaux.add(chevalView);
                racine.getChildren().add(chevalView.getNode());
            }
        }
    }

    /**
     * Marque d'un point noir la case où arriverait chaque cheval jouable.
     */
    private void dessinerDestinations() {
        for (Joueur joueur : joueurs) {
            for (Cheval cheval : joueur.getChevaux()) {
                if (!chevauxDeplacables.contains(cheval)) {
                    continue;
                }
                double[] destination = coordonnees(joueur, plateau.destination(cheval, valeurDe));
                if (destination != null) {
                    Circle repere = new Circle(destination[0], destination[1], 7, Color.BLACK);
                    repere.setOpacity(0.55);
                    repere.setMouseTransparent(true);
                    racine.getChildren().add(repere);
                }
            }
        }
    }

    /**
     * Coordonnées à l'écran d'une position relative d'un joueur (piste ou escalier).
     *
     * @return le centre de la case, ou {@code null} pour l'écurie et le centre du plateau
     */
    private double[] coordonnees(Joueur joueur, int positionRelative) {
        if (positionRelative >= 1 && positionRelative <= Plateau.POSITION_BAS_ESCALIER) {
            int index = Math.floorMod(
                    Plateau.decalageDepart(joueur.getCouleur()) + positionRelative - 1,
                    Plateau.NOMBRE_CASES);
            return positionsCases.get(index);
        }
        List<double[]> marches = positionsMarches.get(joueur.getCouleur());
        int index = positionRelative - Plateau.POSITION_BAS_ESCALIER - 1;
        if (index >= 0 && index < marches.size()) {
            return marches.get(index);
        }
        return null;
    }

    /**
     * Cases du bras du haut, dans le sens horaire : coin, couloir gauche, bout,
     * couloir droit.
     */
    private List<int[]> cheminBrasDuHaut() {
        int n = LONGUEUR_COULOIR;
        List<int[]> chemin = new ArrayList<>();
        chemin.add(new int[] { -1, -1 });
        for (int k = 2; k <= n + 1; k++) {
            chemin.add(new int[] { -1, -k });
        }
        chemin.add(new int[] { 0, -(n + 1) });
        for (int k = n + 1; k >= 2; k--) {
            chemin.add(new int[] { 1, -k });
        }
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
        return new double[] { Rendu.CENTRE + g[0] * Rendu.PAS, Rendu.CENTRE + g[1] * Rendu.PAS };
    }

    private void dessinerPiste() {
        for (int i = 0; i < positionsCases.size(); i++) {
            double[] p = positionsCases.get(i);
            Color fond = Color.WHITE;
            for (Couleur couleur : Couleur.values()) {
                if (i == Plateau.decalageDepart(couleur)) {
                    fond = Rendu.couleur(couleur);
                }
            }
            CaseSpeciale effet = plateau.getCaseSpeciale(i);
            racine.getChildren().add(effet == null
                    ? Rendu.creerCase(p[0], p[1], fond)
                    : Rendu.creerCaseSpeciale(p[0], p[1], effet));
        }

        for (Couleur couleur : Couleur.values()) {
            int indexDepart = Plateau.decalageDepart(couleur);
            double[] depart = positionsCases.get(indexDepart);
            double[] suivante = positionsCases.get((indexDepart + 1) % positionsCases.size());
            racine.getChildren().add(creerFleche(depart, suivante));
        }
    }

    private Polygon creerFleche(double[] depart, double[] suivante) {
        double dx = suivante[0] - depart[0];
        double dy = suivante[1] - depart[1];
        double longueur = Math.hypot(dx, dy);
        double ux = dx / longueur;
        double uy = dy / longueur;
        double px = -uy;
        double py = ux;
        double x = depart[0];
        double y = depart[1];

        Polygon fleche = new Polygon(
                x + ux * 10, y + uy * 10,
                x + ux * 2 + px * 7, y + uy * 2 + py * 7,
                x + ux * 2 + px * 2.5, y + uy * 2 + py * 2.5,
                x - ux * 9 + px * 2.5, y - uy * 9 + py * 2.5,
                x - ux * 9 - px * 2.5, y - uy * 9 - py * 2.5,
                x + ux * 2 - px * 2.5, y + uy * 2 - py * 2.5,
                x + ux * 2 - px * 7, y + uy * 2 - py * 7);
        fleche.setFill(Color.WHITE);
        fleche.setStroke(Rendu.TRAIT);
        fleche.setStrokeWidth(1);
        return fleche;
    }
}
