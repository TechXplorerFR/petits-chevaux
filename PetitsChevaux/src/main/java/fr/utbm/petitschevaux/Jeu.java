package fr.utbm.petitschevaux;

import java.util.List;
import java.util.Scanner;

public class Jeu {
    private List<Joueur> joueurs;
    private Plateau plateau;
    private  De de;
    private int joueurCourant;

    public void jouerJeu(){

        demarrerPartie();
        do{
            jouerTour();
            passerAuJoueurSuivant();
        }while(verifierVictoire());
        // TODO : creer un class ou gerer les parti avec une methode fin de parti
    }

    public void demarrerPartie(){
        joueurCourant = 0;

        joueurs.add(new Joueur("joueur1", Couleur.BLEU));
        joueurs.add(new Joueur("joueur2", Couleur.VERT));
        joueurs.add(new Joueur("joueur3", Couleur.JAUNE));
        joueurs.add(new Joueur("joueur4", Couleur.ROUGE));


        for (Joueur joueur : joueurs) {
            for (int i = 0; i < 4; i++) {
                joueur.addCheval(new Cheval(i));
            }
        }

        plateau = new Plateau();
        de = new De();
        jouerTour();
    }

    public void jouerTour(){
        int valeurDe = de.lancer();
        int choix = 0;
        Joueur joueur = joueurs.get(joueurCourant);

        System.out.print("choisir l'option :");
        if(valeurDe == 6 && !joueur.toutChevalSorti()){
            System.out.print("1-sortir un cheval ou");
        }
        System.out.print(" 2-deplacer un cheval");
        choix = new Scanner(System.in).nextInt();
        if(choix == 0){
            Cheval cheval = joueur.choisirCheval();
            if(cheval.getNumero() == -1){
                System.out.print("il y a une erreur dans le choix du cheval");
            }
            else{
                joueur.deplacerCheval(cheval, valeurDe);
            }
        }
        else{
            joueur.sortirCheval();
        }

    }

    public void passerAuJoueurSuivant(){
        joueurCourant = (joueurCourant + 1) % 4;
    }

    public boolean verifierVictoire(){
        for(Joueur joueur : joueurs){
            if(joueur.aGagne()){
                return true;
            }
        }
        return false;
    }

    public void choisirPremierJoueur(){
        //TODO : creer une classe pour choisir le premier joueur par le lancer de de le plus fort
    }
}
