package fr.utbm.petitschevaux;

public class Cheval {
    public static final int ECURIE = 0;
    private int position;
    private boolean sorti, termine;

    public Cheval() {
        setSorti(false);
        setTermine(false);
        setPosition(0);
    }

    public void setPosition(int value) {
        this.position = value;
    }

    public int getPosition() {
        return position;
    }

    public boolean isTermine() {
        return termine;
    }

    public void setTermine(boolean termine) {
        this.termine = termine;
    }

    public boolean isSorti() {
        return sorti;
    }

    public void setSorti(boolean sorti) {
        this.sorti = sorti;
    }

    public void sortir() {
        setSorti(true);
        setPosition(1);
    }

    public void deplacer(int cases) {
        if (!sorti || termine || cases < 0) {
            return;
        }
        setPosition(getPosition() + cases);
    }

    public void rentrerEcurie() {
        setSorti(false);
        setTermine(false);
        setPosition(0);
    }

    public boolean estArrive() {
        return this.isTermine();
    }

    public void terminer() {
        setTermine(true);
        setSorti(true);
    }
}
