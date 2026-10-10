package fr.utbm.petitschevaux;

import java.util.Random;

public class De {

    public int lancer(){
        Random rand = new Random();
        int valeur = rand.nextInt(6) + 1;
        return valeur;
    }
}
