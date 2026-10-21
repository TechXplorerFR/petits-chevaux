package fr.utbm.petitschevaux;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Déroulement d'une partie : joueurs, tour courant, dé et règles de tour.
 *
 * <p>Un tour suit les états {@link Etat#ATTENTE_LANCER} → {@link Etat#ATTENTE_CHOIX}
 * → (joueur suivant, ou même joueur sur un 6). La partie passe à {@link Etat#TERMINEE}
 * dès qu'un joueur a ramené ses quatre chevaux.</p>
 */
public class Jeu {
    public static final int MAX_JOUEURS = Couleur.values().length;
    public static final int TAILLE_HISTORIQUE = 7;
    private static final List<String> NOMS_BOTS = List.of(
            "Botox", "Botanic", "Botassium", "Botulisme",
            "Botentiel", "Botocopy", "Botomic", "Botéine");

    /** États d'un tour de jeu. */
    public enum Etat {
        ATTENTE_LANCER, ATTENTE_CHOIX, TERMINEE
    }

    /** Un lancer de dé, pour l'historique affiché. */
    public record Lancer(Joueur joueur, int valeur) {
    }

    /** Un effet de case spéciale qui s'est produit, pour le journal affiché. */
    public record Evenement(Joueur joueur, CaseSpeciale effet, String message) {
    }

    public static final int TAILLE_JOURNAL = 5;

    private final List<Joueur> joueurs;
    private final Plateau plateau = new Plateau();
    private final De de;
    private final ModeJeu mode;
    private final EffetsSpeciaux effets;
    private final LinkedList<Lancer> historique = new LinkedList<>();
    private final LinkedList<Evenement> journal = new LinkedList<>();
    private int joueurCourant;
    private Etat etat = Etat.ATTENTE_LANCER;
    private Joueur vainqueur;
    /** Chronomètre : temps des sessions précédentes (sauvegarde) + session en cours. */
    private long dureeAvantChargement;
    private long debutChrono = System.currentTimeMillis();
    private Long finChrono;
    /** Vrai une fois le résultat compté dans la base des scores (pour ne pas le compter deux fois). */
    private boolean resultatEnregistre;

    /**
     * Crée une partie. Les couleurs absentes sont complétées par des bots,
     * et les joueurs sont rangés dans l'ordre du plateau.
     *
     * @param joueurs joueurs choisis (une couleur chacun)
     */
    public Jeu(List<Joueur> joueurs) {
        this(joueurs, new De(), ModeJeu.CLASSIQUE);
    }

    /**
     * @param joueurs joueurs choisis (une couleur chacun)
     * @param mode mode de jeu ; en mode cases spéciales, elles sont placées au hasard
     */
    public Jeu(List<Joueur> joueurs, ModeJeu mode) {
        this(joueurs, new De(), mode);
    }

    /**
     * @param joueurs joueurs choisis (une couleur chacun)
     * @param de dé utilisé (permet un dé truqué dans les tests)
     */
    public Jeu(List<Joueur> joueurs, De de) {
        this(joueurs, de, ModeJeu.CLASSIQUE);
    }

    /**
     * @param joueurs joueurs choisis (une couleur chacun)
     * @param de dé utilisé (permet un dé truqué dans les tests)
     * @param mode mode de jeu ; en mode cases spéciales, elles sont placées au hasard
     */
    public Jeu(List<Joueur> joueurs, De de, ModeJeu mode) {
        this.de = de;
        this.mode = mode;
        this.joueurs = new ArrayList<>(joueurs == null ? List.of() : joueurs);
        this.effets = new EffetsSpeciaux(plateau, this.joueurs);
        if (mode == ModeJeu.CASES_SPECIALES) {
            plateau.placerCasesSpeciales(new Random());
        }
        completerAvecBots();
        this.joueurs.sort(Comparator.comparing(Joueur::getCouleur));
        if (this.joueurs.size() != MAX_JOUEURS
                || this.joueurs.stream().map(Joueur::getCouleur).distinct().count() != MAX_JOUEURS) {
            throw new IllegalArgumentException("Chaque couleur doit avoir exactement un joueur.");
        }
        verifierVictoire();
    }

    private void completerAvecBots() {
        List<String> nomsBots = new ArrayList<>(NOMS_BOTS);
        Collections.shuffle(nomsBots);
        for (Couleur couleur : Couleur.values()) {
            if (joueurs.stream().noneMatch(joueur -> joueur.getCouleur() == couleur)) {
                joueurs.add(new Joueur(nomsBots.remove(0), couleur, true));
            }
        }
    }

    public List<Joueur> getJoueurs() {
        return Collections.unmodifiableList(joueurs);
    }

    public Plateau getPlateau() {
        return plateau;
    }

    public De getDe() {
        return de;
    }

    public int getJoueurCourant() {
        return joueurCourant;
    }

    /**
     * @return le joueur dont c'est le tour
     */
    public Joueur getJoueurActif() {
        return joueurs.get(joueurCourant);
    }

    /**
     * Fixe le joueur dont c'est le tour (début de partie ou chargement d'une sauvegarde).
     */
    void setJoueurCourant(int joueurCourant) {
        if (joueurCourant < 0 || joueurCourant >= joueurs.size()) {
            throw new IllegalArgumentException("Joueur courant invalide : " + joueurCourant);
        }
        this.joueurCourant = joueurCourant;
    }

    public Etat getEtat() {
        return etat;
    }

    /**
     * @return vrai si le joueur courant peut lancer le dé
     */
    public boolean peutLancer() {
        return etat == Etat.ATTENTE_LANCER;
    }

    /**
     * @return vrai après un lancer qui permet au moins un déplacement
     */
    public boolean isAttenteChoix() {
        return etat == Etat.ATTENTE_CHOIX;
    }

    /**
     * @return vrai si un joueur a ramené ses quatre chevaux
     */
    public boolean estTerminee() {
        return etat == Etat.TERMINEE;
    }

    /**
     * @return le gagnant, ou {@code null} si la partie continue
     */
    public Joueur getVainqueur() {
        return vainqueur;
    }

    /**
     * @return les derniers lancers, du plus récent au plus ancien
     */
    public List<Lancer> getHistorique() {
        return Collections.unmodifiableList(historique);
    }

    /**
     * Lance le dé pour le joueur courant. Si aucun cheval ne peut jouer,
     * le tour passe directement (sauf sur un 6, où le joueur relance).
     *
     * @return résultat du lancer, ou 0 si le lancer n'est pas permis
     */
    public int lancerDe() {
        if (!peutLancer()) {
            return 0;
        }
        int valeur = de.lancer();
        int valeurMaudite = effets.appliquerMalusDe(getJoueurActif(), valeur);
        if (valeurMaudite != valeur) {
            ajouterEvenement(new Evenement(getJoueurActif(), CaseSpeciale.DE_MAUDIT,
                    "Dé maudit : " + getJoueurActif().getNom() + " obtient " + valeurMaudite
                            + " au lieu de " + valeur + "."));
            valeur = valeurMaudite;
            de.restaurer(valeur);
        }
        ajouterHistorique(new Lancer(getJoueurActif(), valeur));
        getJoueurActif().getStatistiques().enregistrerLancer(valeur);
        if (calculerChevauxDeplacables().isEmpty()) {
            getJoueurActif().getStatistiques().enregistrerTourPasse();
            finirTour(valeur, false);
        } else {
            etat = Etat.ATTENTE_CHOIX;
        }
        return valeur;
    }

    /**
     * Retourne les chevaux du joueur courant qui peuvent jouer le résultat du dé.
     *
     * @return chevaux déplaçables, vide si le dé n'a pas encore été lancé
     */
    public List<Cheval> getChevauxDeplacables() {
        return isAttenteChoix() ? calculerChevauxDeplacables() : List.of();
    }

    private List<Cheval> calculerChevauxDeplacables() {
        Joueur joueur = getJoueurActif();
        return joueur.getChevaux().stream()
                .filter(cheval -> plateau.estDeplacementValide(
                        joueur, cheval, de.getValeur(), joueurs))
                .toList();
    }

    /**
     * Déplace le cheval choisi, puis passe au joueur suivant sauf sur un 6.
     *
     * @param cheval cheval sélectionné
     * @return vrai si le déplacement a été effectué
     */
    public boolean jouerCheval(Cheval cheval) {
        if (!getChevauxDeplacables().contains(cheval)) {
            return false;
        }
        Joueur joueur = getJoueurActif();
        Statistiques statistiques = joueur.getStatistiques();
        if (!cheval.isSorti()) {
            statistiques.enregistrerSortie();
        } else {
            // Dans l'escalier et vers le centre, le cheval n'avance que d'une case.
            boolean surLaPiste = cheval.getPosition() < Plateau.POSITION_BAS_ESCALIER;
            statistiques.enregistrerDeplacement(surLaPiste ? de.getValeur() : 1);
        }
        cheval.setPosition(plateau.destination(cheval, de.getValeur()));
        for (Joueur victime : plateau.gererCapture(joueur, cheval, joueurs)) {
            statistiques.enregistrerCapture();
            victime.getStatistiques().enregistrerChevalPerdu();
        }
        boolean rejouer = declencherCasesSpeciales(joueur, cheval);
        if (!verifierVictoire()) {
            finirTour(de.getValeur(), rejouer);
        }
        return true;
    }

    /**
     * Applique l'effet de la case spéciale où le cheval vient de s'arrêter, puis celui des
     * cases où arrivent les chevaux déplacés par cet effet, et ainsi de suite (réaction en
     * chaîne). Chaque case ne se déclenche qu'une fois par chaîne, ce qui garantit qu'elle
     * se termine.
     *
     * @return vrai si le joueur courant doit rejouer (case « Rejouer » atteinte par un de ses chevaux)
     */
    private boolean declencherCasesSpeciales(Joueur joueur, Cheval cheval) {
        if (mode != ModeJeu.CASES_SPECIALES) {
            return false;
        }
        boolean rejouer = false;
        Set<Integer> casesDeclenchees = new HashSet<>();
        Deque<EffetsSpeciaux.Arrivee> aVerifier = new ArrayDeque<>();
        aVerifier.add(new EffetsSpeciaux.Arrivee(joueur, cheval));
        while (!aVerifier.isEmpty()) {
            EffetsSpeciaux.Arrivee arrivee = aVerifier.poll();
            // La position est relue maintenant : le cheval a pu être capturé ou déplacé entre-temps.
            int position = plateau.positionAbsolue(arrivee.joueur(), arrivee.cheval());
            CaseSpeciale effet = plateau.getCaseSpeciale(position);
            if (effet == null || !casesDeclenchees.add(position)) {
                continue;
            }
            Joueur declencheur = arrivee.joueur();
            boolean sonTour = declencheur == joueur;
            declencheur.getStatistiques().enregistrerCaseSpeciale();
            String message = effets.appliquer(effet, declencheur, arrivee.cheval(),
                    joueurSuivant(joueurs.indexOf(declencheur)), sonTour);
            String prefixe = casesDeclenchees.size() > 1 ? "Réaction en chaîne – " : "";
            ajouterEvenement(new Evenement(declencheur, effet, prefixe + effet.getNom() + " ! " + message));
            rejouer |= effet == CaseSpeciale.REJOUER && sonTour;
            aVerifier.addAll(effets.recupererArrivees());
        }
        return rejouer;
    }

    /**
     * Joue entièrement le tour du joueur courant lorsqu'il s'agit d'un bot : il lance le dé
     * (sauf si le dé a déjà été lancé, par exemple par un joueur en ligne qui vient d'être
     * remplacé par un bot) puis choisit un cheval.
     *
     * @return résultat du dé, ou zéro si le joueur courant ne peut pas jouer automatiquement
     */
    public int jouerTourBot() {
        if (!getJoueurActif().isBot() || estTerminee()) {
            return 0;
        }
        int valeur = peutLancer() ? lancerDe() : de.getValeur();
        choisirChevalBot().ifPresent(this::jouerCheval);
        return valeur;
    }

    /**
     * @return vrai si le résultat de cette partie a déjà été compté dans les scores
     */
    public boolean isResultatEnregistre() {
        return resultatEnregistre;
    }

    /**
     * Note que le résultat a été compté dans les scores (appelé par {@link ScoresDAO}).
     */
    void marquerResultatEnregistre() {
        resultatEnregistre = true;
    }

    /**
     * Stratégie du bot : capturer si possible, sinon viser une case bonus ou malus
     * (et éviter les neutres), sinon sortir un cheval, sinon avancer le plus avancé.
     */
    private Optional<Cheval> choisirChevalBot() {
        Joueur bot = getJoueurActif();
        int valeur = de.getValeur();
        return getChevauxDeplacables().stream().max(Comparator
                .comparing((Cheval cheval) -> plateau.capturerait(bot, cheval, valeur, joueurs))
                .thenComparingInt(cheval -> interetCase(plateau.caseSpecialeAtteinte(bot, cheval, valeur)))
                .thenComparing(cheval -> !cheval.isSorti())
                .thenComparingInt(Cheval::getPosition));
    }

    private int interetCase(CaseSpeciale effet) {
        if (mode != ModeJeu.CASES_SPECIALES || effet == null) {
            return 0;
        }
        return effet.getCategorie() == CaseSpeciale.Categorie.NEUTRE ? -1 : 1;
    }

    public ModeJeu getMode() {
        return mode;
    }

    /**
     * @return les derniers effets de cases spéciales, du plus récent au plus ancien
     */
    public List<Evenement> getJournal() {
        return Collections.unmodifiableList(journal);
    }

    /**
     * @return points retirés au prochain lancer du joueur (dé maudit), 0 sinon
     */
    public int getMalusDe(Joueur joueur) {
        return effets.getMalusDe(joueur);
    }

    /**
     * @return nombre de tours que le joueur doit encore passer (gel), 0 sinon
     */
    public int getToursGeles(Joueur joueur) {
        return effets.getToursGeles(joueur);
    }

    /**
     * Restaure les effets en attente d'un joueur, lus dans une sauvegarde.
     */
    void restaurerEffets(Joueur joueur, int malusDe, int toursGeles) {
        effets.restaurer(joueur, malusDe, toursGeles);
    }

    /**
     * Remplace le hasard des cases spéciales (tests).
     */
    void setHasard(Random hasard) {
        effets.setHasard(hasard);
    }

    /**
     * Désigne le joueur qui commence (résultat de {@link TiragePremierJoueur})
     * et lance le chronomètre de la partie.
     *
     * @param premier joueur qui joue en premier
     * @throws IllegalStateException si la partie a déjà commencé
     */
    public void definirPremierJoueur(Joueur premier) {
        if (!historique.isEmpty() || estTerminee()) {
            throw new IllegalStateException("La partie a déjà commencé.");
        }
        setJoueurCourant(joueurs.indexOf(premier));
        debutChrono = System.currentTimeMillis();
    }

    /**
     * @return temps de jeu (arrêté à la fin de la partie)
     */
    public Duration getDuree() {
        long fin = finChrono != null ? finChrono : System.currentTimeMillis();
        return Duration.ofMillis(dureeAvantChargement + fin - debutChrono);
    }

    /**
     * @return nombre total de lancers de dé de la partie
     */
    public int getNombreLancers() {
        return joueurs.stream().mapToInt(joueur -> joueur.getStatistiques().getLancers()).sum();
    }

    /**
     * Classement : le vainqueur, puis par chevaux arrivés et enfin par avancée sur le plateau.
     *
     * @return les joueurs du premier au dernier
     */
    public List<Joueur> getClassement() {
        return joueurs.stream()
                .sorted(Comparator.comparing((Joueur joueur) -> joueur != vainqueur)
                        .thenComparing(Comparator.comparingInt(Joueur::getChevauxArrives).reversed())
                        .thenComparing(Comparator.comparingInt(Jeu::avancee).reversed()))
                .toList();
    }

    private static int avancee(Joueur joueur) {
        return joueur.getChevaux().stream().mapToInt(Cheval::getPosition).sum();
    }

    /**
     * Restaure le temps déjà joué, lu dans une sauvegarde.
     */
    void restaurerDuree(long millisecondes) {
        dureeAvantChargement = millisecondes;
        debutChrono = System.currentTimeMillis();
        if (estTerminee()) {
            finChrono = debutChrono;
        }
    }

    /**
     * Restaure l'état d'un tour sauvegardé : joueur courant, valeur du dé
     * et éventuel choix de cheval en attente.
     */
    void restaurerTour(int joueurCourant, int valeurDe, boolean attenteChoix) {
        setJoueurCourant(joueurCourant);
        de.restaurer(valeurDe);
        if (estTerminee()) {
            return;
        }
        etat = Etat.ATTENTE_CHOIX;
        if (!attenteChoix || calculerChevauxDeplacables().isEmpty()) {
            etat = Etat.ATTENTE_LANCER;
        }
    }

    /**
     * Restaure l'historique des lancers, l'historique des effets et l'indicateur de scores
     * lus dans une sauvegarde (ou reçus de l'hôte d'une partie en ligne).
     *
     * @param lancers lancers, du plus récent au plus ancien
     * @param evenements effets, du plus récent au plus ancien
     */
    void restaurerJournaux(List<Lancer> lancers, List<Evenement> evenements, boolean resultatEnregistre) {
        // Copies d'abord : les listes reçues peuvent être des vues sur historique ou journal.
        List<Lancer> nouveauxLancers = List.copyOf(lancers);
        List<Evenement> nouveauxEvenements = List.copyOf(evenements);
        historique.clear();
        nouveauxLancers.stream().limit(TAILLE_HISTORIQUE).forEach(historique::addLast);
        journal.clear();
        nouveauxEvenements.stream().limit(TAILLE_JOURNAL).forEach(journal::addLast);
        this.resultatEnregistre = resultatEnregistre;
    }

    private void ajouterHistorique(Lancer lancer) {
        historique.addFirst(lancer);
        if (historique.size() > TAILLE_HISTORIQUE) {
            historique.removeLast();
        }
    }

    private void ajouterEvenement(Evenement evenement) {
        journal.addFirst(evenement);
        if (journal.size() > TAILLE_JOURNAL) {
            journal.removeLast();
        }
    }

    /**
     * Termine le tour : le même joueur rejoue sur un 6 (ou une case « Rejouer »),
     * sinon la main passe au suivant, en sautant les joueurs gelés.
     */
    private void finirTour(int valeur, boolean rejouer) {
        etat = Etat.ATTENTE_LANCER;
        if (valeur == 6 || rejouer) {
            return;
        }
        joueurCourant = (joueurCourant + 1) % joueurs.size();
        while (effets.consommerGel(getJoueurActif())) {
            ajouterEvenement(new Evenement(getJoueurActif(), CaseSpeciale.GEL,
                    "Gel : " + getJoueurActif().getNom() + " passe son tour."));
            joueurCourant = (joueurCourant + 1) % joueurs.size();
        }
    }

    private Joueur joueurSuivant(int index) {
        return joueurs.get((index + 1) % joueurs.size());
    }

    private boolean verifierVictoire() {
        for (Joueur joueur : joueurs) {
            if (joueur.aGagne()) {
                vainqueur = joueur;
                etat = Etat.TERMINEE;
                finChrono = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }
}
