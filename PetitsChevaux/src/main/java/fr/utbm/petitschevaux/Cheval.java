package fr.utbm.petitschevaux;

public class Cheval {
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
    }

    public void deplacer(int cases) {
        setPosition(getPosition() + cases);
    }

    public void rentrerEcurie() {
        setSorti(false);
        setPosition(0);
    }

    public void estArrive() {
        setTermine(true);
    }
}
