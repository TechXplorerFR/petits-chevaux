package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.List;

public class Plateau {
    public static final int NOMBRE_CASES = 56;
    public static final int NOMBRE_MARCHES = 6;
    public static final int POSITION_BAS_ESCALIER = NOMBRE_CASES;
    public static final int POSITION_ARRIVEE = NOMBRE_CASES + NOMBRE_MARCHES;
    private List<Case> cases = new ArrayList<>();

    public Plateau() {
        for (int i = 0; i <= POSITION_ARRIVEE; i++) {
            Case uneCase = new Case();
            uneCase.setNumero(i);
            uneCase.setTypeCase(i == 0 ? TypeCase.DEPART
                    : i == POSITION_ARRIVEE ? TypeCase.ARRIVEE
                    : i > POSITION_BAS_ESCALIER ? TypeCase.SECURISEE : TypeCase.NORMALE);
            cases.add(uneCase);
        }
    }

    public List<Case> getCases() {
        return cases;
    }

    public void setCases(List<Case> cases) {
        this.cases = cases;
    }

    public Case getCase(int index) {
        return cases.get(index);
    }

    public boolean estDeplacementValide(Cheval cheval, int count) {
        if (cheval == null || count < 1 || count > 6 || !cheval.isSorti() || cheval.estArrive()) {
            return false;
        }
        int currentPosition = cheval.getPosition();
        if (currentPosition == POSITION_ARRIVEE) {
            return count == 6;
        }
        if (currentPosition >= POSITION_BAS_ESCALIER && currentPosition < POSITION_ARRIVEE) {
            return currentPosition + count <= POSITION_ARRIVEE;
        }
        return true;
    }

    /**
     * Vérifie un déplacement en tenant compte des autres chevaux.
     *
     * @param joueur joueur qui joue
     * @param cheval cheval déplacé
     * @param count nombre de cases
     * @param joueurs joueurs de la partie
     * @return vrai si le déplacement est possible sans dépasser un cheval
     */
    public boolean estDeplacementValide(Joueur joueur, Cheval cheval, int count,
            List<Joueur> joueurs) {
        if (!estDeplacementValide(cheval, count) || joueur == null || joueurs == null) {
            return false;
        }
        int destination = positionRelativeSuivante(cheval, count);
        if (destination >= POSITION_BAS_ESCALIER) {
            return true;
        }
        int positionActuelle = positionAbsolue(joueur, cheval);
        for (int pas = 1; pas <= count; pas++) {
            int position = Math.floorMod(positionActuelle + pas, NOMBRE_CASES);
            if (pas < count && occupee(position, joueur, cheval, joueurs)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convertit une position relative au joueur en position absolue sur la piste.
     *
     * @param joueur propriétaire du cheval
     * @param cheval cheval concerné
     * @return position absolue, ou -1 si le cheval est dans son écurie
     */
    public int positionAbsolue(Joueur joueur, Cheval cheval) {
        if (joueur == null || cheval == null || !cheval.isSorti()
                || cheval.getPosition() >= POSITION_BAS_ESCALIER) {
            return -1;
        }
        return Math.floorMod(decalageDepart(joueur.getCouleur()) + cheval.getPosition() - 1,
                NOMBRE_CASES);
    }

    public void gererCapture(Cheval cheval, List<Joueur> joueurs) {
        if (cheval == null || joueurs == null || cheval.estArrive() || !cheval.isSorti()) {
            return;
        }
        Joueur proprietaire = trouverProprietaire(cheval, joueurs);
        int position = positionAbsolue(proprietaire, cheval);
        for (Joueur joueur : joueurs) {
            for (Cheval autre : joueur.getChevaux()) {
                if (autre != cheval && autre.isSorti() && !autre.isTermine()
                        && joueur != proprietaire
                        && positionAbsolue(joueur, autre) == position) {
                    autre.rentrerEcurie();
                }
            }
        }
    }

    public void gererCapture() {
        // Conservation de l'ancienne signature pour compatibilité.
    }

    private int positionRelativeSuivante(Cheval cheval, int count) {
        return cheval.getPosition() + count;
    }

    private boolean occupee(int position, Joueur joueur, Cheval cheval, List<Joueur> joueurs) {
        for (Joueur autreJoueur : joueurs) {
            for (Cheval autre : autreJoueur.getChevaux()) {
                if (autre != cheval && autre.isSorti() && !autre.isTermine()
                        && positionAbsolue(autreJoueur, autre) == position) {
                    return true;
                }
            }
        }
        return false;
    }

    private Joueur trouverProprietaire(Cheval cheval, List<Joueur> joueurs) {
        for (Joueur joueur : joueurs) {
            if (joueur.getChevaux().contains(cheval)) {
                return joueur;
            }
        }
        return null;
    }

    private int decalageDepart(Couleur couleur) {
        return switch (couleur) {
            case JAUNE -> 8;
            case ROUGE -> 22;
            case BLEU -> 36;
            case VERT -> 50;
        };
    }
}
