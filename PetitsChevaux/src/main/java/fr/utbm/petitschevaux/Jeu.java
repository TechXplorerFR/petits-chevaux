package fr.utbm.petitschevaux;

import java.util.List;

public class Jeu {
    private List<Joueur> joueurs;
    private Plateau plateau;
    private De de;
    private int joueurCourant;

    public Jeu(List<Joueur> joueursReels) {
        joueurs = joueursReels;
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
        // Initialiser les joueurs (joueurs réels + 4 - joueurs réels) = 4 faux joueurs
        while (joueurs.size() < 4) {
            joueurs.add(new Joueur("Joueur " + (joueurs.size() + 1), Couleur.values()[joueurs.size()]));
        }
        // Initialiser le plateau
        setPlateau(new Plateau());
        // Initialiser le dé
        setDe(new De());
        // Définir le joueur courant
        setJoueurCourant(0);
        // Boucle de jeu jusqu'à ce qu'un joueur gagne
        while (!verifierVictoire()) {
            jouerTour();
        }
    }

    public void jouerTour() {
        int rolledNumber = de.lancer();

        // Tir de dés, et attentes choix cheval par joueur

        List<Cheval> playerChevaux = this.joueurs.get(joueurCourant).getChevaux();
        List<Cheval> chevauxDeplacables = playerChevaux.stream()
                .filter(cheval -> plateau.estDeplacementValide(cheval, rolledNumber)) // Deplacement valide selon le
                                                                                      // plateau
                .filter(cheval -> !cheval.isTermine()) // Cheval pas encore arrivé
                .filter(Cheval::isSorti)
                .filter(cheval -> rolledNumber == 6 && !cheval.isSorti()) // Cheval sorti ou lancé
                                                                          // un 6
                // cheval est sur une case sécurisée et le compte correspond à la position du
                // cheval pour qu'il puisse aller sur la case suivante
                .filter(cheval -> {
                    int currentPosition = cheval.getPosition();
                    TypeCase currentCaseType = plateau.getCases().get(currentPosition).getTypeCase();
                    return !(currentCaseType == TypeCase.SECURISEE
                            && rolledNumber != (6 - plateau.getCases().size() - 2 - currentPosition)); // TODO
                })
                .toList();

        if (!chevauxDeplacables.isEmpty()) {
            // TODO: permettre au joueur de choisir un cheval à déplacer
            Cheval chevalChoisi = chevauxDeplacables.get(0); // Pour l'instant, on choisit le premier cheval déplaçable
            chevalChoisi.deplacer(rolledNumber);
        }

        if (rolledNumber != 6) {
            passerAuJoueurSuivant();
        }

        return;
    }

    public void passerAuJoueurSuivant() {
        setJoueurCourant((getJoueurCourant() + 1) % 4);
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
