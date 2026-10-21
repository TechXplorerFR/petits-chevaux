package fr.utbm.petitschevaux;

/**
 * Bilan d'un joueur humain sur toutes ses parties terminées, identifié par son nom.
 *
 * @param nom nom du joueur
 * @param parties nombre de parties terminées
 * @param victoires nombre de parties gagnées
 */
public record Profil(String nom, int parties, int victoires) {

    /**
     * @return la part de parties gagnées, entre 0 et 1 (calculée, pas stockée)
     */
    public double ratioVictoires() {
        return parties == 0 ? 0 : (double) victoires / parties;
    }
}
