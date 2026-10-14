package fr.utbm.petitschevaux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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

    private final List<Joueur> joueurs;
    private final Plateau plateau = new Plateau();
    private final De de;
    private final LinkedList<Lancer> historique = new LinkedList<>();
    private int joueurCourant;
    private Etat etat = Etat.ATTENTE_LANCER;
    private Joueur vainqueur;
    /** Chronomètre : temps des sessions précédentes (sauvegarde) + session en cours. */
    private long dureeAvantChargement;
    private long debutChrono = System.currentTimeMillis();
    private Long finChrono;

    /**
     * Crée une partie. Les couleurs absentes sont complétées par des bots,
     * et les joueurs sont rangés dans l'ordre du plateau.
     *
     * @param joueurs joueurs choisis (une couleur chacun)
     */
    public Jeu(List<Joueur> joueurs) {
        this(joueurs, new De());
    }

    /**
     * @param joueurs joueurs choisis (une couleur chacun)
     * @param de dé utilisé (permet un dé truqué dans les tests)
     */
    public Jeu(List<Joueur> joueurs, De de) {
        this.de = de;
        this.joueurs = new ArrayList<>(joueurs == null ? List.of() : joueurs);
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
        ajouterHistorique(new Lancer(getJoueurActif(), valeur));
        getJoueurActif().getStatistiques().enregistrerLancer(valeur);
        if (calculerChevauxDeplacables().isEmpty()) {
            getJoueurActif().getStatistiques().enregistrerTourPasse();
            finirTour(valeur);
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
        if (!verifierVictoire()) {
            finirTour(de.getValeur());
        }
        return true;
    }

    /**
     * Joue entièrement le tour du joueur courant lorsqu'il s'agit d'un bot :
     * il lance le dé et avance le cheval le plus avancé possible.
     *
     * @return résultat du dé, ou zéro si le joueur courant ne peut pas jouer automatiquement
     */
    public int jouerTourBot() {
        if (!getJoueurActif().isBot() || !peutLancer()) {
            return 0;
        }
        int valeur = lancerDe();
        choisirChevalBot().ifPresent(this::jouerCheval);
        return valeur;
    }

    /**
     * Stratégie du bot : capturer si possible, sinon sortir un cheval,
     * sinon avancer le cheval le plus proche de l'arrivée.
     */
    private Optional<Cheval> choisirChevalBot() {
        Joueur bot = getJoueurActif();
        int valeur = de.getValeur();
        return getChevauxDeplacables().stream().max(Comparator
                .comparing((Cheval cheval) -> plateau.capturerait(bot, cheval, valeur, joueurs))
                .thenComparing(cheval -> !cheval.isSorti())
                .thenComparingInt(Cheval::getPosition));
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

    private void ajouterHistorique(Lancer lancer) {
        historique.addFirst(lancer);
        if (historique.size() > TAILLE_HISTORIQUE) {
            historique.removeLast();
        }
    }

    private void finirTour(int valeur) {
        etat = Etat.ATTENTE_LANCER;
        if (valeur != 6) {
            joueurCourant = (joueurCourant + 1) % joueurs.size();
        }
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
