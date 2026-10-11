package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.List;

public class Plateau {
    private List<Case> cases = new ArrayList<>();

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
        int currentPosition = cheval.getPosition();
        TypeCase currentCaseType = this.getCases().get(currentPosition).getTypeCase();

        if(cheval.estArrive()) return false;

        // On retire vérifie qu'on est sur une case normale, et que notre position n'excède pas les cases sécurisées
        if (currentCaseType == TypeCase.NORMALE && currentPosition + count <= this.getCases().size() - 7) {
            return true;
        }
        // On vérifie que le cheval est sur une case sécurisée et que le compte correspond à la position du cheval pour qu'il puisse aller sur la case suivante
        if(currentCaseType == TypeCase.SECURISEE && count == 6 - this.getCases().size() - 2 - currentPosition) {
            return true;
        }
        return false;
    }

    public void gererCapture() {
        //TODO: gérer la capture d'un cheval par un autre cheval
    }
}
