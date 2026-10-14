package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tirage du premier joueur : chacun lance le dé à tour de rôle, le plus haut score commence.
 * En cas d'égalité au plus haut score, seuls les ex æquo relancent (nouvelle manche).
 */
public class TiragePremierJoueur {
    private final List<Joueur> joueurs;
    private final De de;
    /** Dernier résultat de chaque joueur ; absent tant qu'il n'a pas lancé dans sa manche. */
    private final Map<Joueur, Integer> resultats = new HashMap<>();
    private List<Joueur> candidats;
    private int indexLanceur;
    private int manche = 1;
    private List<Joueur> derniereEgalite = List.of();
    private int valeurEgalite;
    private Joueur gagnant;

    /**
     * @param joueurs joueurs de la partie, dans l'ordre où ils lancent
     * @param de dé utilisé pour le tirage
     */
    public TiragePremierJoueur(List<Joueur> joueurs, De de) {
        if (joueurs.isEmpty()) {
            throw new IllegalArgumentException("Il faut au moins un joueur pour le tirage.");
        }
        this.joueurs = List.copyOf(joueurs);
        this.de = de;
        this.candidats = new ArrayList<>(joueurs);
        if (candidats.size() == 1) {
            gagnant = candidats.get(0);
        }
    }

    public List<Joueur> getJoueurs() {
        return joueurs;
    }

    /**
     * @return le joueur qui doit lancer, ou {@code null} si le tirage est terminé
     */
    public Joueur getLanceur() {
        return estTermine() ? null : candidats.get(indexLanceur);
    }

    /**
     * Fait lancer le dé au joueur dont c'est le tour.
     *
     * @return la valeur obtenue
     * @throws IllegalStateException si le tirage est déjà terminé
     */
    public int lancer() {
        if (estTermine()) {
            throw new IllegalStateException("Le tirage est terminé.");
        }
        int valeur = de.lancer();
        resultats.put(getLanceur(), valeur);
        indexLanceur++;
        if (indexLanceur == candidats.size()) {
            terminerManche();
        }
        return valeur;
    }

    private void terminerManche() {
        int meilleur = candidats.stream().mapToInt(resultats::get).max().orElse(0);
        List<Joueur> meilleurs = candidats.stream()
                .filter(joueur -> resultats.get(joueur) == meilleur)
                .toList();
        if (meilleurs.size() == 1) {
            gagnant = meilleurs.get(0);
            return;
        }
        derniereEgalite = meilleurs;
        valeurEgalite = meilleur;
        candidats = new ArrayList<>(meilleurs);
        candidats.forEach(resultats::remove);
        indexLanceur = 0;
        manche++;
    }

    public boolean estTermine() {
        return gagnant != null;
    }

    /**
     * @return le joueur qui commence, ou {@code null} tant que le tirage n'est pas fini
     */
    public Joueur getGagnant() {
        return gagnant;
    }

    /**
     * @return vrai si le joueur peut encore gagner le tirage
     */
    public boolean estCandidat(Joueur joueur) {
        return candidats.contains(joueur);
    }

    /**
     * @return le dernier résultat du joueur, ou 0 s'il n'a pas encore lancé dans cette manche
     */
    public int getResultat(Joueur joueur) {
        return resultats.getOrDefault(joueur, 0);
    }

    /** @return numéro de la manche en cours (1 au départ, +1 à chaque égalité) */
    public int getManche() {
        return manche;
    }

    /** @return les ex æquo de la dernière manche à égalité (vide s'il n'y en a pas eu) */
    public List<Joueur> getDerniereEgalite() {
        return derniereEgalite;
    }

    /** @return la valeur sur laquelle a eu lieu la dernière égalité */
    public int getValeurEgalite() {
        return valeurEgalite;
    }
}
