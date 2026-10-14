package fr.utbm.petitschevaux;

import java.util.Random;

public class De {
    private final Random random;
    private int valeur;

    public De() {
        this(new Random());
    }

    public De(Random random) {
        this.random = random;
    }

    public int lancer() {
        valeur = random.nextInt(6) + 1;
        return valeur;
    }

    /**
     * @return la dernière valeur lancée, ou 0 si le dé n'a pas encore été lancé
     */
    public int getValeur() {
        return valeur;
    }

    /**
     * Remet le dé sur une valeur donnée (chargement d'une sauvegarde).
     *
     * @param valeur valeur entre 0 (pas encore lancé) et 6
     */
    void restaurer(int valeur) {
        if (valeur < 0 || valeur > 6) {
            throw new IllegalArgumentException("Valeur de dé invalide : " + valeur);
        }
        this.valeur = valeur;
    }
}
