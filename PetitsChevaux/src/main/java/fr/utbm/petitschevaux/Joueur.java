package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Joueur {
    public static final int NOMBRE_CHEVAUX = 4;

    private final String nom;
    private final Couleur couleur;
    private final boolean bot;
    private final List<Cheval> chevaux;
    private final Statistiques statistiques = new Statistiques();

    public Joueur(String nom, Couleur couleur) {
        this(nom, couleur, false);
    }

    /**
     * Crée un joueur humain ou contrôlé par l'ordinateur.
     *
     * @param nom nom affiché
     * @param couleur couleur du joueur
     * @param bot vrai si le joueur joue automatiquement
     */
    public Joueur(String nom, Couleur couleur, boolean bot) {
        if (couleur == null) {
            throw new IllegalArgumentException("La couleur d'un joueur est obligatoire.");
        }
        this.nom = nom;
        this.couleur = couleur;
        this.bot = bot;
        List<Cheval> liste = new ArrayList<>();
        for (int i = 0; i < NOMBRE_CHEVAUX; i++) {
            liste.add(new Cheval(i));
        }
        this.chevaux = Collections.unmodifiableList(liste);
    }

    public String getNom() {
        return nom;
    }

    public Couleur getCouleur() {
        return couleur;
    }

    /**
     * Indique si ce joueur est un bot.
     *
     * @return vrai pour un joueur automatique
     */
    public boolean isBot() {
        return bot;
    }

    /**
     * @return les quatre chevaux du joueur (liste non modifiable)
     */
    public List<Cheval> getChevaux() {
        return chevaux;
    }

    /**
     * @return les statistiques du joueur pour la partie en cours
     */
    public Statistiques getStatistiques() {
        return statistiques;
    }

    /**
     * @return le nombre de chevaux arrivés au centre
     */
    public int getChevauxArrives() {
        return (int) chevaux.stream().filter(Cheval::isTermine).count();
    }

    public boolean aGagne() {
        return chevaux.stream().allMatch(Cheval::isTermine);
    }
}
