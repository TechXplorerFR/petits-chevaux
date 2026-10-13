package fr.utbm.petitschevaux;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class Jeu {
    private List<Joueur> joueurs;
    private Plateau plateau;
    private De de;
    private int joueurCourant;

    public Jeu(List<Joueur> joueursReels) {
        joueurs = joueursReels == null ? new ArrayList<>() : new ArrayList<>(joueursReels);
    }

    public List<Joueur> getJoueurs() {
        return joueurs;
    }

    public void setJoueurs(List<Joueur> joueurs) {
        this.joueurs = joueurs;
    }

    public Plateau getPlateau() {
        return plateau;
    }

    public void setPlateau(Plateau plateau) {
        this.plateau = plateau;
    }

    public De getDe() {
        return de;
    }

    public void setDe(De de) {
        this.de = de;
    }

    public int getJoueurCourant() {
        return joueurCourant;
    }

    public void setJoueurCourant(int joueurCourant) {
        this.joueurCourant = joueurCourant;
    }

    public void demarrerPartie() {
        completerAvecBots();
        if (joueurs.size() < 2 || joueurs.size() > 4) {
            throw new IllegalStateException("Une partie nécessite entre 2 et 4 joueurs.");
        }
        setPlateau(new Plateau());
        setDe(new De());
        setJoueurCourant(0);
    }

    private void completerAvecBots() {
        List<String> nomsBots = new ArrayList<>(List.of(
                "Botox", "Botanic", "Botassium", "Botulisme",
                "Botentiel", "Botocopy", "Botomic", "Botéine"));
        Collections.shuffle(nomsBots);
        for (Couleur couleur : Couleur.values()) {
            if (joueurs.stream().noneMatch(joueur -> joueur.getCouleur() == couleur)) {
                String nomBot = nomsBots.remove(0);
                joueurs.add(new Joueur(nomBot, couleur, true));
            }
        }
    }

    public void jouerTour() {
        int rolledNumber = lancerDe();
        Joueur joueur = joueurs.get(joueurCourant);
        if (rolledNumber == 6) {
            for (Cheval cheval : joueur.getChevaux()) {
                if (!cheval.isSorti() && !cheval.isTermine()) {
                    cheval.sortir();
                    plateau.gererCapture(cheval, joueurs);
                    return;
                }

            }
        }
        for (Cheval cheval : joueur.getChevaux()) {
            if (plateau.estDeplacementValide(joueur, cheval, rolledNumber, joueurs)) {
                deplacer(joueur, cheval, rolledNumber);
                break;
            }
        }
        if (rolledNumber != 6) {
            passerAuJoueurSuivant();
        }
    }

    /**
     * Lance le dé sans déplacer automatiquement un cheval.
     *
     * @return résultat du lancer
     */
    public int lancerDe() {
        return de.lancer();
    }

    public void deplacer(Joueur joueur, Cheval cheval, int nombre) {
        if (!plateau.estDeplacementValide(joueur, cheval, nombre, joueurs)) {
            return;
        }
        if (cheval.getPosition() == Plateau.POSITION_ARRIVEE && nombre == 6) {
            cheval.terminer();
            return;
        }
        int destination = cheval.getPosition() + nombre;
        cheval.setPosition(destination);
        plateau.gererCapture(cheval, joueurs);
    }

    /**
     * Retourne les chevaux du joueur courant qui peuvent jouer le résultat du dé.
     *
     * @return chevaux déplaçables
     */
    public List<Cheval> getChevauxDeplacables() {
        if (de == null || joueurs.isEmpty()) {
            return List.of();
        }
        Joueur joueur = joueurs.get(joueurCourant);
        return joueur.getChevaux().stream()
                .filter(cheval -> (!cheval.isSorti() && !cheval.isTermine()
                        && de.getValeur() == 6)
                        || plateau.estDeplacementValide(
                                joueur, cheval, de.getValeur(), joueurs))
                .collect(Collectors.toList());
    }

    /**
     * Déplace le cheval sélectionné et passe au joueur suivant si nécessaire.
     *
     * @param cheval cheval sélectionné
     * @return vrai si le déplacement a été effectué
     */
    public boolean jouerCheval(Cheval cheval) {
        if (cheval == null || de == null || joueurs.isEmpty()) {
            return false;
        }
        Joueur joueur = joueurs.get(joueurCourant);
        if (!cheval.isSorti() && !cheval.isTermine() && de.getValeur() == 6
                && joueur.getChevaux().contains(cheval)) {
            cheval.sortir();
            plateau.gererCapture(cheval, joueurs);
            return true;
        }

        if (!plateau.estDeplacementValide(joueur, cheval, de.getValeur(), joueurs)) {
            return false;
        }
        int valeur = de.getValeur();
        deplacer(joueur, cheval, valeur);
        if (valeur != 6) {
            passerAuJoueurSuivant();
        }
        return true;
    }

    /**
     * Joue automatiquement le tour du joueur courant lorsqu'il s'agit d'un bot.
     *
     * @return résultat du dé, ou zéro si le joueur courant n'est pas un bot
     */
    public int jouerTourBot() {
        if (joueurs.isEmpty() || !joueurs.get(joueurCourant).isBot()) {
            return 0;
        }
        int valeur = lancerDe();
        List<Cheval> deplacables = getChevauxDeplacables();
        Cheval choix = deplacables.stream()
                .max((premier, second) -> Integer.compare(
                        progression(premier), progression(second)))
                .orElse(null);
        if (choix != null) {
            jouerCheval(choix);
        } else {
            passerAuJoueurSuivant();
        }
        return valeur;
    }

    private int progression(Cheval cheval) {
        return cheval.isSorti() ? cheval.getPosition() : -1;
    }

    public void passerAuJoueurSuivant() {
        setJoueurCourant((getJoueurCourant() + 1) % joueurs.size());
        return;
    }

    public boolean verifierVictoire() {
        for (Joueur joueur : joueurs) {
            if (joueur.aGagne()) {
                return true;
            }
        }
        return false;
    }

    public void choisirPremierJoueur(){
        //TODO : creer une classe pour choisir le premier joueur par le lancer de de le plus fort
    }
}
