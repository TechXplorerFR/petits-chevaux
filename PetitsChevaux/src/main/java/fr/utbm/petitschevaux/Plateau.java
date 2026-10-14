package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.List;

/**
 * Règles de déplacement sur le plateau.
 *
 * <p>La position d'un cheval est relative à son joueur :</p>
 * <ul>
 *     <li>0 : dans l'écurie ;</li>
 *     <li>1 à 56 : sur la piste (1 = case de départ, 56 = pied de l'escalier) ;</li>
 *     <li>57 à 62 : marches 1 à 6 de l'escalier ;</li>
 *     <li>63 : au centre, le cheval a terminé.</li>
 * </ul>
 * <p>Depuis la marche 6, un 6 fait entrer le cheval au centre.</p>
 */
public class Plateau {
    public static final int NOMBRE_CASES = 56;
    public static final int NOMBRE_MARCHES = 6;
    public static final int POSITION_BAS_ESCALIER = NOMBRE_CASES;
    public static final int POSITION_ARRIVEE = NOMBRE_CASES + NOMBRE_MARCHES;
    public static final int CASES_PAR_QUARTIER = NOMBRE_CASES / Couleur.values().length;
    /** Case de départ du premier quartier, sur la piste absolue. */
    private static final int PREMIER_DEPART = CASES_PAR_QUARTIER / 2 + 1;

    /**
     * Case absolue de la piste où sortent les chevaux d'une couleur.
     *
     * @param couleur couleur du joueur
     * @return index de la case de départ sur la piste (0 à 55)
     */
    public static int decalageDepart(Couleur couleur) {
        return couleur.ordinal() * CASES_PAR_QUARTIER + PREMIER_DEPART;
    }

    /**
     * Vérifie un déplacement en tenant compte des autres chevaux :
     * on ne peut ni dépasser un cheval, ni s'arrêter sur un de ses propres chevaux.
     *
     * @param joueur joueur qui joue
     * @param cheval cheval déplacé
     * @param valeur valeur du dé
     * @param joueurs joueurs de la partie
     * @return vrai si le déplacement est autorisé
     */
    public boolean estDeplacementValide(Joueur joueur, Cheval cheval, int valeur,
            List<Joueur> joueurs) {
        if (joueur == null || cheval == null || joueurs == null || valeur < 1 || valeur > 6
                || !joueur.getChevaux().contains(cheval) || cheval.isTermine()) {
            return false;
        }
        int position = cheval.getPosition();
        if (!cheval.isSorti()) {
            return valeur == 6 && !occupeeParLeJoueur(joueur, cheval, 1);
        }
        if (position == POSITION_ARRIVEE) {
            return valeur == 6;
        }
        if (position >= POSITION_BAS_ESCALIER) {
            // Chaque marche se monte avec le dé égal à son numéro, si elle est libre.
            return valeur == position - POSITION_BAS_ESCALIER + 1
                    && !occupeeParLeJoueur(joueur, cheval, position + 1);
        }
        List<Integer> chemin = chemin(position, valeur);
        for (int i = 0; i < chemin.size() - 1; i++) {
            if (chevalSur(absolue(joueur, chemin.get(i)), cheval, joueurs) != null) {
                return false;
            }
        }
        return !occupeeParLeJoueur(joueur, cheval, chemin.get(chemin.size() - 1));
    }

    /**
     * Position relative atteinte par un cheval avec la valeur donnée, sans vérifier la validité.
     * Sur la piste, un dé trop fort fait reculer depuis le pied de l'escalier.
     *
     * @param cheval cheval déplacé
     * @param valeur valeur du dé
     * @return position relative d'arrivée
     */
    public int destination(Cheval cheval, int valeur) {
        int position = cheval.getPosition();
        if (!cheval.isSorti()) {
            return 1;
        }
        if (position >= POSITION_BAS_ESCALIER) {
            return Math.min(position + 1, Cheval.CENTRE);
        }
        List<Integer> chemin = chemin(position, valeur);
        return chemin.get(chemin.size() - 1);
    }

    /**
     * Convertit la position d'un cheval en position absolue sur la piste.
     *
     * @param joueur propriétaire du cheval
     * @param cheval cheval concerné
     * @return position absolue, ou -1 si le cheval n'est pas sur la piste
     */
    public int positionAbsolue(Joueur joueur, Cheval cheval) {
        if (joueur == null || cheval == null || !cheval.estEnJeu()) {
            return -1;
        }
        return absolue(joueur, cheval.getPosition());
    }

    /**
     * Indique si le déplacement enverrait un cheval adverse à l'écurie.
     *
     * @param joueur propriétaire du cheval
     * @param cheval cheval déplacé
     * @param valeur valeur du dé
     * @param joueurs joueurs de la partie
     * @return vrai si un adversaire occupe la case d'arrivée
     */
    public boolean capturerait(Joueur joueur, Cheval cheval, int valeur, List<Joueur> joueurs) {
        Cheval occupant = chevalSur(absolue(joueur, destination(cheval, valeur)), cheval, joueurs);
        return occupant != null && !joueur.getChevaux().contains(occupant);
    }

    /**
     * Renvoie à l'écurie les chevaux adverses présents sur la case du cheval.
     *
     * @param joueur propriétaire du cheval
     * @param cheval cheval qui vient d'arriver
     * @param joueurs joueurs de la partie
     * @return le propriétaire de chaque cheval renvoyé à l'écurie (vide si aucune capture)
     */
    public List<Joueur> gererCapture(Joueur joueur, Cheval cheval, List<Joueur> joueurs) {
        List<Joueur> victimes = new ArrayList<>();
        int position = positionAbsolue(joueur, cheval);
        if (position < 0) {
            return victimes;
        }
        for (Joueur autreJoueur : joueurs) {
            if (autreJoueur == joueur) {
                continue;
            }
            for (Cheval autre : autreJoueur.getChevaux()) {
                if (positionAbsolue(autreJoueur, autre) == position) {
                    autre.rentrerEcurie();
                    victimes.add(autreJoueur);
                }
            }
        }
        return victimes;
    }

    /**
     * Positions relatives traversées case par case, avec rebond au pied de l'escalier.
     */
    private List<Integer> chemin(int position, int valeur) {
        List<Integer> chemin = new ArrayList<>();
        int courante = position;
        int sens = 1;
        for (int pas = 0; pas < valeur; pas++) {
            if (courante == POSITION_BAS_ESCALIER) {
                sens = -1;
            }
            courante += sens;
            chemin.add(courante);
        }
        return chemin;
    }

    private int absolue(Joueur joueur, int positionRelative) {
        if (positionRelative < 1 || positionRelative > POSITION_BAS_ESCALIER) {
            return -1;
        }
        return Math.floorMod(decalageDepart(joueur.getCouleur()) + positionRelative - 1,
                NOMBRE_CASES);
    }

    private boolean occupeeParLeJoueur(Joueur joueur, Cheval cheval, int positionRelative) {
        return joueur.getChevaux().stream().anyMatch(autre -> autre != cheval
                && autre.estEnJeu() && autre.getPosition() == positionRelative);
    }

    private Cheval chevalSur(int positionAbsolue, Cheval ignore, List<Joueur> joueurs) {
        if (positionAbsolue < 0) {
            return null;
        }
        for (Joueur joueur : joueurs) {
            for (Cheval autre : joueur.getChevaux()) {
                if (autre != ignore && positionAbsolue(joueur, autre) == positionAbsolue) {
                    return autre;
                }
            }
        }
        return null;
    }
}
