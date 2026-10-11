package fr.utbm.petitschevaux;

import java.util.Random;

public class De {
    private final Random random;
    private int valeur;

    public int lancer(){
        valeur = random.nextInt(6) + 1;
        return valeur;
    }

    public De() {
        this(new Random());
    }

    public De(Random random) {
        this.random = random;
    }

    public int getValeur() {
        return valeur;
    }
}
