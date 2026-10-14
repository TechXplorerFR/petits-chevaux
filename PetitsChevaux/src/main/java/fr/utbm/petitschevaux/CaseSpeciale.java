package fr.utbm.petitschevaux;

/**
 * Effets des cases spéciales du mode {@link ModeJeu#CASES_SPECIALES}.
 * L'effet se déclenche quand un cheval s'arrête sur la case (pas en passant dessus).
 */
public enum CaseSpeciale {
    REJOUER(Categorie.BONUS, "Rejouer", "↻", "Tu rejoues immédiatement."),
    TURBO(Categorie.BONUS, "Turbo", "+3", "Ton cheval avance de 3 cases de plus."),
    CROC_EN_JAMBE(Categorie.MALUS, "Croc-en-jambe", "-4",
            "Le cheval adverse le plus avancé sur la piste recule de 4 cases."),
    DE_MAUDIT(Categorie.MALUS, "Dé maudit", "-2",
            "Le prochain lancer de chaque adversaire perd 2 points (minimum 1)."),
    GEL(Categorie.MALUS, "Gel", "❄", "Le joueur suivant passe son prochain tour."),
    COUP_DU_SORT(Categorie.NEUTRE, "Coup du sort", "?",
            "Un cheval au hasard sur la piste, peut-être le tien, retourne à l'écurie."),
    TREMBLEMENT(Categorie.NEUTRE, "Tremblement de terre", "↓",
            "Tous les chevaux sur la piste reculent de 2 cases."),
    VENT_ARRIERE(Categorie.NEUTRE, "Vent arrière", "↑",
            "Tous les chevaux sur la piste avancent de 2 cases.");

    /** Pour qui l'effet est-il favorable ? */
    public enum Categorie {
        /** Avantage le joueur qui s'arrête sur la case. */
        BONUS,
        /** Pénalise ses adversaires. */
        MALUS,
        /** Peut toucher tout le monde, y compris le joueur qui l'a déclenché. */
        NEUTRE
    }

    private final Categorie categorie;
    private final String nom;
    private final String symbole;
    private final String description;

    CaseSpeciale(Categorie categorie, String nom, String symbole, String description) {
        this.categorie = categorie;
        this.nom = nom;
        this.symbole = symbole;
        this.description = description;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public String getNom() {
        return nom;
    }

    /** @return texte court affiché sur la case */
    public String getSymbole() {
        return symbole;
    }

    public String getDescription() {
        return description;
    }
}
