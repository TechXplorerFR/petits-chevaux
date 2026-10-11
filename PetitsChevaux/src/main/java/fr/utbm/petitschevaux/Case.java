package fr.utbm.petitschevaux;

public class Case {
    private int numero;
    private TypeCase typeCase;

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public TypeCase getTypeCase() {
        return typeCase;
    }

    public void setTypeCase(TypeCase typeCase) {
        this.typeCase = typeCase;
    }

    public boolean estSecurisee() {
        return this.typeCase == TypeCase.SECURISEE;
    }
}
