package fr.utbm.petitschevaux;

/**
 * Statistiques d'un joueur au cours d'une partie, affichées sur l'écran de victoire.
 * Elles sont tenues à jour par {@link Jeu}.
 */
public class Statistiques {
    private int lancers;
    private int sommeDes;
    private int six;
    private int toursPasses;
    private int sorties;
    private int casesParcourues;
    private int captures;
    private int chevauxPerdus;

    /** @return nombre de lancers de dé pendant la partie */
    public int getLancers() {
        return lancers;
    }

    /** @return nombre de 6 obtenus */
    public int getSix() {
        return six;
    }

    /** @return moyenne des lancers, ou 0 sans lancer */
    public double getMoyenneDe() {
        return lancers == 0 ? 0 : (double) sommeDes / lancers;
    }

    /** @return nombre de lancers sans aucun cheval jouable */
    public int getToursPasses() {
        return toursPasses;
    }

    /** @return nombre de chevaux sortis de l'écurie (un même cheval peut sortir plusieurs fois) */
    public int getSorties() {
        return sorties;
    }

    /** @return nombre de cases parcourues par les chevaux du joueur */
    public int getCasesParcourues() {
        return casesParcourues;
    }

    /** @return nombre de chevaux adverses renvoyés à l'écurie */
    public int getCaptures() {
        return captures;
    }

    /** @return nombre de chevaux du joueur renvoyés à l'écurie par un adversaire */
    public int getChevauxPerdus() {
        return chevauxPerdus;
    }

    void enregistrerLancer(int valeur) {
        lancers++;
        sommeDes += valeur;
        if (valeur == 6) {
            six++;
        }
    }

    void enregistrerTourPasse() {
        toursPasses++;
    }

    void enregistrerSortie() {
        sorties++;
    }

    void enregistrerDeplacement(int cases) {
        casesParcourues += cases;
    }

    void enregistrerCapture() {
        captures++;
    }

    void enregistrerChevalPerdu() {
        chevauxPerdus++;
    }

    /**
     * @return les compteurs, dans l'ordre attendu par {@link #restaurer(int[])}
     */
    int[] versTableau() {
        return new int[] { lancers, sommeDes, six, toursPasses, sorties,
                casesParcourues, captures, chevauxPerdus };
    }

    /**
     * Restaure les compteurs lus dans une sauvegarde.
     *
     * @param valeurs compteurs dans l'ordre de {@link #versTableau()}
     */
    void restaurer(int[] valeurs) {
        if (valeurs.length != 8) {
            throw new IllegalArgumentException("Statistiques incomplètes.");
        }
        lancers = valeurs[0];
        sommeDes = valeurs[1];
        six = valeurs[2];
        toursPasses = valeurs[3];
        sorties = valeurs[4];
        casesParcourues = valeurs[5];
        captures = valeurs[6];
        chevauxPerdus = valeurs[7];
    }
}
