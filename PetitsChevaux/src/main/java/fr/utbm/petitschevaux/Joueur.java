package fr.utbm.petitschevaux;

import java.util.ArrayList;
import java.util.List;

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



    public Joueur(String nom, Couleur couleur) {}
}
