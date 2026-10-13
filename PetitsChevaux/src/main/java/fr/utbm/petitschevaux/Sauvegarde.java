package fr.utbm.petitschevaux;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Sauvegarde et chargement d'une partie dans un fichier texte (format Properties).
 */
public final class Sauvegarde {
    public static final String EXTENSION = "pcx";

    private Sauvegarde() {
    }

    public static void sauvegarder(Jeu jeu, Path fichier) throws IOException {
        Properties donnees = new Properties();
        List<Joueur> joueurs = jeu.getJoueurs();
        donnees.setProperty("joueurs", String.valueOf(joueurs.size()));
        donnees.setProperty("joueurCourant", String.valueOf(jeu.getJoueurCourant()));
        for (int i = 0; i < joueurs.size(); i++) {
            Joueur joueur = joueurs.get(i);
            String prefixe = "joueur." + i + ".";
            donnees.setProperty(prefixe + "nom", joueur.getNom());
            donnees.setProperty(prefixe + "couleur", joueur.getCouleur().name());
            List<Cheval> chevaux = joueur.getChevaux();
            for (int j = 0; j < chevaux.size(); j++) {
                Cheval cheval = chevaux.get(j);
                donnees.setProperty(prefixe + "cheval." + j,
                        cheval.getPosition() + ";" + cheval.isSorti() + ";" + cheval.isTermine());
            }
        }
        try (Writer writer = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            donnees.store(writer, "Partie de petits chevaux");
        }
    }

    public static Jeu charger(Path fichier) throws IOException {
        Properties donnees = new Properties();
        try (Reader reader = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            donnees.load(reader);
        }
        try {
            int nombreJoueurs = Integer.parseInt(lire(donnees, "joueurs"));
            List<Joueur> joueurs = new ArrayList<>();
            for (int i = 0; i < nombreJoueurs; i++) {
                String prefixe = "joueur." + i + ".";
                Joueur joueur = new Joueur(lire(donnees, prefixe + "nom"),
                        Couleur.valueOf(lire(donnees, prefixe + "couleur")));
                List<Cheval> chevaux = joueur.getChevaux();
                for (int j = 0; j < chevaux.size(); j++) {
                    String[] etat = lire(donnees, prefixe + "cheval." + j).split(";");
                    Cheval cheval = chevaux.get(j);
                    cheval.setPosition(Integer.parseInt(etat[0]));
                    cheval.setSorti(Boolean.parseBoolean(etat[1]));
                    cheval.setTermine(Boolean.parseBoolean(etat[2]));
                }
                joueurs.add(joueur);
            }
            Jeu jeu = new Jeu(joueurs);
            jeu.demarrerPartie();
            int joueurCourant = Integer.parseInt(lire(donnees, "joueurCourant"));
            if (joueurCourant < 0 || joueurCourant >= nombreJoueurs) {
                throw new IOException("Joueur courant invalide : " + joueurCourant);
            }
            jeu.setJoueurCourant(joueurCourant);
            return jeu;
        } catch (IllegalArgumentException | IllegalStateException | ArrayIndexOutOfBoundsException e) {
            throw new IOException("Fichier de sauvegarde invalide : " + e.getMessage(), e);
        }
    }

    private static String lire(Properties donnees, String cle) throws IOException {
        String valeur = donnees.getProperty(cle);
        if (valeur == null) {
            throw new IOException("Donnée manquante dans la sauvegarde : " + cle);
        }
        return valeur;
    }
}
