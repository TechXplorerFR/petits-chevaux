package fr.utbm.petitschevaux;

import java.util.List;
import java.util.ArrayList;

public class Jeu {
    private List<Joueur> joueurs;
    private Plateau plateau;
    private De de;
    private int joueurCourant;

    public Jeu(List<Joueur> joueursReels) {
        joueurs = joueursReels == null ? new ArrayList<>() : joueursReels;
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
        if (joueurs.size() < 2 || joueurs.size() > 4) {
            throw new IllegalStateException("Une partie nécessite entre 2 et 4 joueurs.");
        }
        setPlateau(new Plateau());
        setDe(new De());
        setJoueurCourant(0);
    }

    public void jouerTour() {
        int rolledNumber = de.lancer();
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
            if (plateau.estDeplacementValide(cheval, rolledNumber)) {
                deplacer(joueur, cheval, rolledNumber);
                break;
            }
        }
        if (rolledNumber != 6) {
            passerAuJoueurSuivant();
        }
    }

    public void deplacer(Joueur joueur, Cheval cheval, int nombre) {
        if (!plateau.estDeplacementValide(cheval, nombre)) {
            return;
        }
        if (cheval.getPosition() == Plateau.POSITION_ARRIVEE && nombre == 6) {
            cheval.terminer();
            return;
        }
        int destination = cheval.getPosition() >= Plateau.POSITION_BAS_ESCALIER
                ? cheval.getPosition() + 1
                : cheval.getPosition() + nombre;
        if (cheval.getPosition() < Plateau.POSITION_BAS_ESCALIER
                && destination > Plateau.POSITION_BAS_ESCALIER) {
            destination = Plateau.POSITION_BAS_ESCALIER
                    - (destination - Plateau.POSITION_BAS_ESCALIER);
        }
        cheval.setPosition(destination);
        plateau.gererCapture(cheval, joueurs);
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
}
