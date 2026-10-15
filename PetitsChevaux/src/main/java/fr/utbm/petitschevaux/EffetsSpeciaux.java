package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Applique les effets des cases spéciales et garde en mémoire ceux qui agissent plus tard
 * (dé maudit, gel). Utilisé par {@link Jeu} en mode {@link ModeJeu#CASES_SPECIALES}.
 *
 * <p>Les déplacements forcés (turbo, croc-en-jambe, tremblement, vent arrière) restent sur
 * la piste : ils ne font jamais entrer un cheval dans l'escalier, mais capturent les
 * adversaires sur la case d'arrivée. Un cheval qui tomberait sur un autre cheval de son
 * joueur ne bouge pas. Les chevaux déplacés sont signalés via {@link #recupererArrivees()}
 * pour que {@link Jeu} déclenche la case spéciale où ils s'arrêtent (réaction en chaîne).</p>
 */
class EffetsSpeciaux {
    static final int CASES_TURBO = 3;
    static final int CASES_CROC_EN_JAMBE = 4;
    static final int CASES_SECOUSSE = 2;
    static final int MALUS_DE = 2;

    /** Un cheval qui vient d'arriver sur une case suite à un déplacement forcé. */
    record Arrivee(Joueur joueur, Cheval cheval) {
    }

    private final Plateau plateau;
    private final List<Joueur> joueurs;
    private Random hasard = new Random();
    /** Chevaux déplacés par le dernier effet, en attente de vérification de leur case. */
    private final List<Arrivee> arrivees = new ArrayList<>();
    /** Points retirés au prochain lancer de chaque joueur. */
    private final Map<Joueur, Integer> malusDe = new HashMap<>();
    /** Nombre de tours que chaque joueur doit encore passer. */
    private final Map<Joueur, Integer> toursGeles = new HashMap<>();

    EffetsSpeciaux(Plateau plateau, List<Joueur> joueurs) {
        this.plateau = plateau;
        this.joueurs = joueurs;
    }

    void setHasard(Random hasard) {
        this.hasard = hasard;
    }

    /**
     * Applique l'effet de la case où le cheval vient de s'arrêter.
     *
     * @param effet effet de la case
     * @param joueur joueur qui a déclenché la case
     * @param cheval cheval arrivé sur la case
     * @param suivant joueur qui jouera après lui
     * @param sonTour vrai si c'est le tour de ce joueur ; « Rejouer » n'a d'effet que dans ce cas
     * @return description de ce qui s'est passé, pour les joueurs
     */
    String appliquer(CaseSpeciale effet, Joueur joueur, Cheval cheval, Joueur suivant, boolean sonTour) {
        return switch (effet) {
            case REJOUER -> sonTour
                    ? joueur.getNom() + " rejoue !"
                    : "Ce n'est pas le tour de " + joueur.getNom() + " : aucun effet.";
            case TURBO -> turbo(joueur, cheval);
            case CROC_EN_JAMBE -> crocEnJambe(joueur);
            case DE_MAUDIT -> deMaudit(joueur);
            case GEL -> {
                toursGeles.merge(suivant, 1, Integer::sum);
                yield suivant.getNom() + " passera son prochain tour.";
            }
            case COUP_DU_SORT -> coupDuSort(joueur);
            case TREMBLEMENT -> secousse(-CASES_SECOUSSE, "reculent");
            case VENT_ARRIERE -> secousse(CASES_SECOUSSE, "avancent");
        };
    }

    private String turbo(Joueur joueur, Cheval cheval) {
        List<Joueur> victimes = deplacerForce(joueur, cheval, CASES_TURBO);
        if (victimes == null) {
            return "Turbo… mais la route du cheval de " + joueur.getNom() + " est bloquée.";
        }
        victimes.forEach(victime -> joueur.getStatistiques().enregistrerCapture());
        return "Le cheval de " + joueur.getNom() + " avance de " + CASES_TURBO + " cases"
                + texteCaptures(victimes) + ".";
    }

    private String crocEnJambe(Joueur joueur) {
        Joueur cibleJoueur = null;
        Cheval cible = null;
        for (Joueur adversaire : joueurs) {
            if (adversaire == joueur) {
                continue;
            }
            for (Cheval cheval : adversaire.getChevaux()) {
                if (surLaPiste(cheval) && (cible == null || cheval.getPosition() > cible.getPosition())) {
                    cible = cheval;
                    cibleJoueur = adversaire;
                }
            }
        }
        if (cible == null) {
            return "Croc-en-jambe… mais aucun adversaire n'est sur la piste.";
        }
        List<Joueur> victimes = deplacerForce(cibleJoueur, cible, -CASES_CROC_EN_JAMBE);
        if (victimes == null) {
            return "Croc-en-jambe… mais le cheval de " + cibleJoueur.getNom() + " tient bon.";
        }
        return "Le cheval le plus avancé de " + cibleJoueur.getNom() + " recule de "
                + CASES_CROC_EN_JAMBE + " cases" + texteCaptures(victimes) + ".";
    }

    private String deMaudit(Joueur joueur) {
        List<Joueur> adversaires = joueurs.stream().filter(autre -> autre != joueur).toList();
        adversaires.forEach(adversaire -> malusDe.merge(adversaire, MALUS_DE, Integer::sum));
        return "Le prochain lancer de " + noms(adversaires) + " perd " + MALUS_DE + " points.";
    }

    private String coupDuSort(Joueur declencheur) {
        List<Joueur> proprietaires = new ArrayList<>();
        List<Cheval> chevaux = new ArrayList<>();
        for (Joueur joueur : joueurs) {
            for (Cheval cheval : joueur.getChevaux()) {
                if (surLaPiste(cheval)) {
                    proprietaires.add(joueur);
                    chevaux.add(cheval);
                }
            }
        }
        int tirage = hasard.nextInt(chevaux.size());
        Joueur victime = proprietaires.get(tirage);
        chevaux.get(tirage).rentrerEcurie();
        victime.getStatistiques().enregistrerChevalPerdu();
        return victime == declencheur
                ? declencheur.getNom() + " n'a pas de chance : un de ses propres chevaux retourne à l'écurie."
                : "Un cheval de " + victime.getNom() + " retourne à l'écurie.";
    }

    private String secousse(int cases, String verbe) {
        List<Joueur> victimes = new ArrayList<>();
        for (Joueur joueur : joueurs) {
            for (Cheval cheval : joueur.getChevaux()) {
                if (surLaPiste(cheval)) {
                    List<Joueur> capturees = deplacerForce(joueur, cheval, cases);
                    if (capturees != null) {
                        victimes.addAll(capturees);
                    }
                }
            }
        }
        return "Tous les chevaux sur la piste " + verbe + " de " + Math.abs(cases) + " cases"
                + texteCaptures(victimes) + ".";
    }

    /**
     * Déplace un cheval de la piste sans passer par les règles du dé.
     *
     * @return les propriétaires des chevaux capturés, ou {@code null} si le cheval n'a pas bougé
     */
    private List<Joueur> deplacerForce(Joueur joueur, Cheval cheval, int cases) {
        int position = cheval.getPosition();
        int destination = Math.max(1, Math.min(Plateau.POSITION_BAS_ESCALIER, position + cases));
        if (destination == position || !plateau.estLibrePour(joueur, cheval, destination)) {
            return null;
        }
        cheval.setPosition(destination);
        arrivees.add(new Arrivee(joueur, cheval));
        List<Joueur> victimes = plateau.gererCapture(joueur, cheval, joueurs);
        victimes.forEach(victime -> victime.getStatistiques().enregistrerChevalPerdu());
        return victimes;
    }

    private static boolean surLaPiste(Cheval cheval) {
        return cheval.estEnJeu() && cheval.getPosition() <= Plateau.POSITION_BAS_ESCALIER;
    }

    private static String texteCaptures(List<Joueur> victimes) {
        if (victimes.isEmpty()) {
            return "";
        }
        return victimes.size() == 1
                ? " et renvoie un cheval de " + victimes.get(0).getNom() + " à l'écurie"
                : " et renvoie " + victimes.size() + " chevaux à l'écurie";
    }

    private static String noms(List<Joueur> joueurs) {
        return joueurs.stream().map(Joueur::getNom).collect(Collectors.joining(", "));
    }

    /**
     * @return les chevaux déplacés par les effets appliqués depuis le dernier appel,
     *         dans l'ordre des déplacements (la liste interne est ensuite vidée)
     */
    List<Arrivee> recupererArrivees() {
        List<Arrivee> copie = List.copyOf(arrivees);
        arrivees.clear();
        return copie;
    }

    /**
     * Retire au lancer le malus de dé en attente pour ce joueur.
     *
     * @return la valeur effective du lancer (au minimum 1)
     */
    int appliquerMalusDe(Joueur joueur, int valeur) {
        Integer malus = malusDe.remove(joueur);
        return malus == null ? valeur : Math.max(1, valeur - malus);
    }

    /**
     * Consomme un tour de gel du joueur s'il en a un.
     *
     * @return vrai si le joueur doit passer son tour
     */
    boolean consommerGel(Joueur joueur) {
        Integer tours = toursGeles.get(joueur);
        if (tours == null) {
            return false;
        }
        if (tours <= 1) {
            toursGeles.remove(joueur);
        } else {
            toursGeles.put(joueur, tours - 1);
        }
        return true;
    }

    int getMalusDe(Joueur joueur) {
        return malusDe.getOrDefault(joueur, 0);
    }

    int getToursGeles(Joueur joueur) {
        return toursGeles.getOrDefault(joueur, 0);
    }

    /**
     * Restaure les effets en attente d'un joueur (chargement d'une sauvegarde).
     */
    void restaurer(Joueur joueur, int malus, int tours) {
        if (malus > 0) {
            malusDe.put(joueur, malus);
        }
        if (tours > 0) {
            toursGeles.put(joueur, tours);
        }
    }
}
