package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Joueur {
    private String nom;
    private Couleur couleur;
    private List<Cheval> chevaux = new ArrayList<>();

    public List<Cheval> getChevaux() {
        return chevaux;
    }

    public void setChevaux(List<Cheval> chevaux) {
        this.chevaux = chevaux;
    }

    public void addCheval(Cheval chev) {
        this.chevaux.add(chev);
    }

    public void removeCheval(Cheval chev) {
        this.chevaux.remove(chev);
    }

    public Couleur getCouleur() {
        return couleur;
    }

    public void setCouleur(Couleur couleur) {
        this.couleur = couleur;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }


    public Joueur(String nom, Couleur couleur) {
        this.nom = nom;
        this.couleur = couleur;
        for (int i = 0; i < 4; i++) {
            chevaux.add(new Cheval(i));
        }
    }

    public int sortirCheval(){
        for(Cheval cheval : chevaux){
            if (!cheval.isSorti() && !cheval.isTermine()) {
                cheval.sortir();
                return 0;
            }
        }
        return 1;
    }

    public boolean toutChevalSorti(){
        for(Cheval cheval : chevaux){
            if(!cheval.isSorti()){
                return false;
            }
        }
        return true;
    }

    public Cheval choisirCheval(){
        List<Cheval> chevauxSorti = new ArrayList<>();
        System.out.print("voici la liste des chevaux disponible \n");
        for(Cheval cheval : chevaux){
            if(cheval.isSorti()){
                chevauxSorti.add(cheval);
                System.out.printf("cheval %d est disponible", cheval.getNumero());
            }
        }
        System.out.print("chosi le numero du cheval que tu veu bouger");
        int choix = new Scanner(System.in).nextInt();
        for(Cheval cheval : chevauxSorti){
            if(cheval.getNumero() == choix){
                return cheval;
            }
        }
        return new Cheval(-1);
    }

    public void deplacerCheval(Cheval cheval, int distance){
        if (chevaux.contains(cheval)) {
            cheval.deplacer(distance);
        }
    }

    public boolean aGagne() {
        for (Cheval cheval : chevaux) {
            if (!cheval.isTermine()) {
                return false;
            }
        }
        return true;
    }
}
