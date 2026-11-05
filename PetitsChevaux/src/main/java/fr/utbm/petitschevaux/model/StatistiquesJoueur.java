package fr.utbm.petitschevaux.model;

/**
 * Statistiques d'un joueur (humain ou bot) à la fin d'une partie, telles qu'enregistrées
 * dans la base de données avec le résumé de la partie ({@link ResumePartie}).
 *
 * @param rang place au classement final (1 = vainqueur)
 * @param nom nom du joueur
 * @param couleur couleur du joueur
 * @param bot vrai si c'était un bot
 * @param chevauxArrives chevaux arrivés au centre
 * @param lancers nombre de lancers de dé
 * @param sommeDes somme des valeurs obtenues au dé
 * @param six nombre de 6 obtenus
 * @param toursPasses lancers sans aucun cheval jouable
 * @param sorties chevaux sortis de l'écurie
 * @param casesParcourues cases parcourues par ses chevaux
 * @param captures chevaux adverses renvoyés à l'écurie
 * @param chevauxPerdus chevaux renvoyés à l'écurie par un adversaire
 * @param casesSpeciales cases spéciales sur lesquelles il s'est arrêté
 */
public record StatistiquesJoueur(int rang, String nom, Couleur couleur, boolean bot, int chevauxArrives,
        int lancers, int sommeDes, int six, int toursPasses, int sorties, int casesParcourues,
        int captures, int chevauxPerdus, int casesSpeciales) {

    /**
     * Relève les statistiques d'un joueur à la fin d'une partie.
     *
     * @param rang place du joueur au classement final
     * @param joueur joueur de la partie
     * @return ses statistiques
     */
    public static StatistiquesJoueur de(int rang, Joueur joueur) {
        Statistiques stats = joueur.getStatistiques();
        return new StatistiquesJoueur(rang, joueur.getNom(), joueur.getCouleur(), joueur.isBot(),
                joueur.getChevauxArrives(), stats.getLancers(), stats.getSommeDes(), stats.getSix(),
                stats.getToursPasses(), stats.getSorties(), stats.getCasesParcourues(),
                stats.getCaptures(), stats.getChevauxPerdus(), stats.getCasesSpeciales());
    }

    /** @return vrai pour le vainqueur de la partie */
    public boolean vainqueur() {
        return rang == 1;
    }

    /** @return moyenne des lancers, ou 0 sans lancer */
    public double moyenneDe() {
        return lancers == 0 ? 0 : (double) sommeDes / lancers;
    }
}
