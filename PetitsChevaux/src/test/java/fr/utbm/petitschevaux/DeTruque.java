package fr.utbm.petitschevaux;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Dé qui renvoie une suite de valeurs fixée à l'avance, pour des tests reproductibles.
 */
final class DeTruque extends De {
    DeTruque(int... valeurs) {
        super(new Suite(valeurs));
    }

    private static final class Suite extends Random {
        private final Deque<Integer> valeurs = new ArrayDeque<>();

        Suite(int... valeurs) {
            for (int valeur : valeurs) {
                this.valeurs.add(valeur);
            }
        }

        @Override
        public int nextInt(int borne) {
            if (valeurs.isEmpty()) {
                throw new IllegalStateException("Plus de valeurs de dé prévues dans le test.");
            }
            return valeurs.poll() - 1;
        }
    }
}
