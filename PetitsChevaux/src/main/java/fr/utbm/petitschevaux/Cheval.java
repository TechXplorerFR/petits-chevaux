package fr.utbm.petitschevaux;

/**
 * Un cheval, entièrement décrit par sa position relative à son joueur
 * (voir {@link Plateau} pour la numérotation).
 */
public class Cheval {
    public static final int ECURIE = 0;
    public static final int CENTRE = Plateau.POSITION_ARRIVEE + 1;

    private final int numero;
    private int position = ECURIE;

    public Cheval(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    public int getPosition() {
        return position;
    }

    /**
     * Place le cheval sur une position donnée (déplacement ou chargement d'une sauvegarde).
     *
     * @param position position entre {@link #ECURIE} et {@link #CENTRE}
     */
    public void setPosition(int position) {
        if (position < ECURIE || position > CENTRE) {
            throw new IllegalArgumentException("Position de cheval invalide : " + position);
        }
        this.position = position;
    }

    /**
     * @return vrai si le cheval a quitté l'écurie (il peut être sur la piste, l'escalier ou arrivé)
     */
    public boolean isSorti() {
        return position != ECURIE;
    }

    /**
     * @return vrai si le cheval est arrivé au centre
     */
    public boolean isTermine() {
        return position == CENTRE;
    }

    /**
     * @return vrai si le cheval est sur la piste ou dans l'escalier
     */
    public boolean estEnJeu() {
        return isSorti() && !isTermine();
    }

    public void sortir() {
        setPosition(1);
    }

    public void rentrerEcurie() {
        setPosition(ECURIE);
    }

    public void terminer() {
        setPosition(CENTRE);
    }
}
