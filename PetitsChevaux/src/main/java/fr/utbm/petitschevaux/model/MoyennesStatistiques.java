package fr.utbm.petitschevaux.model;

/**
 * Statistiques moyennes d'un joueur sur une partie, calculées sur toutes les parties
 * enregistrées pour un groupe de joueurs (tous, humains, bots, vainqueurs…).
 *
 * @param groupe libellé du groupe de joueurs
 * @param participations nombre de lignes prises en compte (un joueur dans une partie)
 * @param chevauxArrives chevaux arrivés au centre, en moyenne
 * @param lancers lancers de dé par partie, en moyenne
 * @param moyenneDe valeur moyenne obtenue au dé (total des points / total des lancers)
 * @param six six obtenus par partie, en moyenne
 * @param toursPasses tours passés par partie, en moyenne
 * @param sorties sorties d'écurie par partie, en moyenne
 * @param casesParcourues cases parcourues par partie, en moyenne
 * @param captures captures par partie, en moyenne
 * @param chevauxPerdus chevaux perdus par partie, en moyenne
 * @param casesSpeciales cases spéciales déclenchées par partie, en moyenne
 */
public record MoyennesStatistiques(String groupe, int participations, double chevauxArrives, double lancers,
        double moyenneDe, double six, double toursPasses, double sorties, double casesParcourues,
        double captures, double chevauxPerdus, double casesSpeciales) {
}
