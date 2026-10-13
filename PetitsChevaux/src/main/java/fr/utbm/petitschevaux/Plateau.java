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
            return count == currentPosition - POSITION_BAS_ESCALIER + 1;
        }
        return true;
    }

    public void gererCapture(Cheval cheval, List<Joueur> joueurs) {
        if (cheval == null || joueurs == null || cheval.estArrive() || !cheval.isSorti()) {
            return;
        }
        for (Joueur joueur : joueurs) {
            for (Cheval autre : joueur.getChevaux()) {
                if (autre != cheval && autre.isSorti() && !autre.isTermine()
                        && autre.getPosition() == cheval.getPosition()) {
                    autre.rentrerEcurie();
                }
            }
        }
    }

    public void gererCapture() {
        // Conservation de l'ancienne signature pour compatibilité.
    }
}
