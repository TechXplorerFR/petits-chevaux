package fr.utbm.petitschevaux;

/**
 * Modes de jeu proposés dans le menu.
 */
public enum ModeJeu {
    CLASSIQUE("Classique", "Les règles traditionnelles des petits chevaux."),
    CASES_SPECIALES("Cases spéciales",
            "Des cases bonus, malus et neutres sont cachées sur la piste : s'arrêter dessus déclenche un effet.");

    private final String libelle;
    private final String description;

    ModeJeu(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }
}
