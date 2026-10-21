package fr.utbm.petitschevaux;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sauvegarde et chargement d'une partie dans un fichier texte (format Properties).
 */
public final class Sauvegarde {
    public static final String EXTENSION = "pcx";
    /** Dossier des sauvegardes, dans le dossier depuis lequel le jeu est lancé. */
    public static final Path DOSSIER = Path.of(System.getProperty("user.dir"), "sauvegardes");
    private static final DateTimeFormatter FORMAT_NOM =
            DateTimeFormatter.ofPattern("'partie_'yyyy-MM-dd_HH'h'mm'm'ss");

    private Sauvegarde() {
    }

    /**
     * Enregistre la partie dans le dossier des sauvegardes, sans rien demander au joueur.
     *
     * @param jeu partie à enregistrer
     * @param dossier dossier des sauvegardes (créé s'il n'existe pas)
     * @param fichierExistant fichier de cette partie s'il a déjà été enregistré ou chargé,
     *                        qui est alors mis à jour ; {@code null} pour en créer un nouveau
     * @return le fichier écrit
     */
    public static Path sauvegarderDansLeDossier(Jeu jeu, Path dossier, Path fichierExistant)
            throws IOException {
        Path fichier = fichierExistant;
        if (fichier == null) {
            Files.createDirectories(dossier);
            String nom = LocalDateTime.now().format(FORMAT_NOM);
            fichier = dossier.resolve(nom + "." + EXTENSION);
            // Deux sauvegardes dans la même seconde : on ajoute un numéro.
            for (int numero = 2; Files.exists(fichier); numero++) {
                fichier = dossier.resolve(nom + "_" + numero + "." + EXTENSION);
            }
        }
        sauvegarder(jeu, fichier);
        return fichier;
    }

    /**
     * @param dossier dossier des sauvegardes
     * @return les sauvegardes du dossier, de la plus récente à la plus ancienne
     *         (vide si le dossier n'existe pas encore)
     */
    public static List<Path> listerSauvegardes(Path dossier) throws IOException {
        if (!Files.isDirectory(dossier)) {
            return List.of();
        }
        try (Stream<Path> fichiers = Files.list(dossier)) {
            return fichiers
                    .filter(fichier -> fichier.getFileName().toString().endsWith("." + EXTENSION))
                    .sorted(Comparator.comparing(Sauvegarde::dateModification).reversed())
                    .toList();
        }
    }

    private static FileTime dateModification(Path fichier) {
        try {
            return Files.getLastModifiedTime(fichier);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    public static void sauvegarder(Jeu jeu, Path fichier) throws IOException {
        try (Writer writer = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            versProprietes(jeu).store(writer, "Partie de petits chevaux");
        }
    }

    public static Jeu charger(Path fichier) throws IOException {
        Properties donnees = new Properties();
        try (Reader reader = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            donnees.load(reader);
        }
        return depuisProprietes(donnees);
    }

    /**
     * Décrit toute la partie sous forme de texte (même contenu qu'un fichier de sauvegarde).
     * Sert à envoyer l'état de la partie aux joueurs en ligne.
     */
    public static String versTexte(Jeu jeu) {
        StringWriter texte = new StringWriter();
        try {
            versProprietes(jeu).store(texte, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e); // impossible avec un StringWriter
        }
        return texte.toString();
    }

    /**
     * Reconstruit une partie à partir du texte produit par {@link #versTexte(Jeu)}.
     *
     * @throws IOException si le texte est invalide
     */
    public static Jeu depuisTexte(String texte) throws IOException {
        Properties donnees = new Properties();
        donnees.load(new StringReader(texte));
        return depuisProprietes(donnees);
    }

    private static Properties versProprietes(Jeu jeu) {
        Properties donnees = new Properties();
        List<Joueur> joueurs = jeu.getJoueurs();
        donnees.setProperty("joueurs", String.valueOf(joueurs.size()));
        donnees.setProperty("joueurCourant", String.valueOf(jeu.getJoueurCourant()));
        donnees.setProperty("de", String.valueOf(jeu.getDe().getValeur()));
        donnees.setProperty("attenteChoix", String.valueOf(jeu.isAttenteChoix()));
        donnees.setProperty("duree", String.valueOf(jeu.getDuree().toMillis()));
        donnees.setProperty("mode", jeu.getMode().name());
        // Format « position:EFFET,position:EFFET », positions absolues sur la piste.
        donnees.setProperty("casesSpeciales", jeu.getPlateau().getCasesSpeciales().entrySet().stream()
                .map(entree -> entree.getKey() + ":" + entree.getValue().name())
                .collect(Collectors.joining(",")));
        for (int i = 0; i < joueurs.size(); i++) {
            Joueur joueur = joueurs.get(i);
            String prefixe = "joueur." + i + ".";
            donnees.setProperty(prefixe + "nom", joueur.getNom());
            donnees.setProperty(prefixe + "couleur", joueur.getCouleur().name());
            donnees.setProperty(prefixe + "bot", String.valueOf(joueur.isBot()));
            donnees.setProperty(prefixe + "malusDe", String.valueOf(jeu.getMalusDe(joueur)));
            donnees.setProperty(prefixe + "gel", String.valueOf(jeu.getToursGeles(joueur)));
            donnees.setProperty(prefixe + "stats", Arrays.stream(joueur.getStatistiques().versTableau())
                    .mapToObj(String::valueOf).collect(Collectors.joining(";")));
            List<Cheval> chevaux = joueur.getChevaux();
            for (int j = 0; j < chevaux.size(); j++) {
                donnees.setProperty(prefixe + "cheval." + j,
                        String.valueOf(chevaux.get(j).getPosition()));
            }
        }
        donnees.setProperty("resultatEnregistre", String.valueOf(jeu.isResultatEnregistre()));
        List<Jeu.Lancer> lancers = jeu.getHistorique();
        for (int i = 0; i < lancers.size(); i++) {
            donnees.setProperty("historique." + i + ".couleur", lancers.get(i).joueur().getCouleur().name());
            donnees.setProperty("historique." + i + ".valeur", String.valueOf(lancers.get(i).valeur()));
        }
        List<Jeu.Evenement> evenements = jeu.getJournal();
        for (int i = 0; i < evenements.size(); i++) {
            Jeu.Evenement evenement = evenements.get(i);
            donnees.setProperty("journal." + i + ".couleur", evenement.joueur().getCouleur().name());
            donnees.setProperty("journal." + i + ".effet", evenement.effet().name());
            donnees.setProperty("journal." + i + ".message", evenement.message());
        }
        return donnees;
    }

    private static Jeu depuisProprietes(Properties donnees) throws IOException {
        try {
            int nombreJoueurs = Integer.parseInt(lire(donnees, "joueurs"));
            List<Joueur> joueurs = new ArrayList<>();
            for (int i = 0; i < nombreJoueurs; i++) {
                String prefixe = "joueur." + i + ".";
                Joueur joueur = new Joueur(lire(donnees, prefixe + "nom"),
                        Couleur.valueOf(lire(donnees, prefixe + "couleur")),
                        Boolean.parseBoolean(donnees.getProperty(prefixe + "bot", "false")));
                for (Cheval cheval : joueur.getChevaux()) {
                    cheval.setPosition(lirePosition(lire(donnees,
                            prefixe + "cheval." + cheval.getNumero())));
                }
                String stats = donnees.getProperty(prefixe + "stats");
                if (stats != null) {
                    joueur.getStatistiques().restaurer(Arrays.stream(stats.split(";"))
                            .mapToInt(valeur -> Integer.parseInt(valeur.trim())).toArray());
                }
                joueurs.add(joueur);
            }
            ModeJeu mode = ModeJeu.valueOf(donnees.getProperty("mode", ModeJeu.CLASSIQUE.name()));
            Jeu jeu = new Jeu(joueurs, mode);
            restaurerCasesSpeciales(jeu.getPlateau(), donnees.getProperty("casesSpeciales", ""));
            for (int i = 0; i < nombreJoueurs; i++) {
                String prefixe = "joueur." + i + ".";
                jeu.restaurerEffets(joueurs.get(i),
                        Integer.parseInt(donnees.getProperty(prefixe + "malusDe", "0")),
                        Integer.parseInt(donnees.getProperty(prefixe + "gel", "0")));
            }
            // Le joueur courant est retrouvé par sa couleur : l'ordre du fichier peut différer.
            Couleur couleurCourante = joueurs.get(
                    Integer.parseInt(lire(donnees, "joueurCourant"))).getCouleur();
            // Un lancer en attente de choix est conservé : recharger ne permet pas de relancer.
            jeu.restaurerTour(couleurCourante.ordinal(),
                    Integer.parseInt(donnees.getProperty("de", "0")),
                    Boolean.parseBoolean(donnees.getProperty("attenteChoix", "false")));
            jeu.restaurerDuree(Long.parseLong(donnees.getProperty("duree", "0")));
            jeu.restaurerJournaux(lireHistorique(donnees, jeu), lireJournal(donnees, jeu),
                    Boolean.parseBoolean(donnees.getProperty("resultatEnregistre", "false")));
            return jeu;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new IOException("Fichier de sauvegarde invalide : " + e.getMessage(), e);
        }
    }

    private static List<Jeu.Lancer> lireHistorique(Properties donnees, Jeu jeu) {
        List<Jeu.Lancer> lancers = new ArrayList<>();
        for (int i = 0; donnees.containsKey("historique." + i + ".couleur"); i++) {
            Joueur joueur = joueurDeCouleur(jeu, donnees.getProperty("historique." + i + ".couleur"));
            lancers.add(new Jeu.Lancer(joueur,
                    Integer.parseInt(donnees.getProperty("historique." + i + ".valeur", "0"))));
        }
        return lancers;
    }

    private static List<Jeu.Evenement> lireJournal(Properties donnees, Jeu jeu) {
        List<Jeu.Evenement> evenements = new ArrayList<>();
        for (int i = 0; donnees.containsKey("journal." + i + ".couleur"); i++) {
            String prefixe = "journal." + i + ".";
            evenements.add(new Jeu.Evenement(
                    joueurDeCouleur(jeu, donnees.getProperty(prefixe + "couleur")),
                    CaseSpeciale.valueOf(donnees.getProperty(prefixe + "effet")),
                    donnees.getProperty(prefixe + "message", "")));
        }
        return evenements;
    }

    private static Joueur joueurDeCouleur(Jeu jeu, String couleur) {
        // Les joueurs de Jeu sont rangés dans l'ordre des couleurs.
        return jeu.getJoueurs().get(Couleur.valueOf(couleur).ordinal());
    }

    private static void restaurerCasesSpeciales(Plateau plateau, String valeur) {
        if (valeur.isBlank()) {
            return;
        }
        plateau.viderCasesSpeciales();
        for (String caseSpeciale : valeur.split(",")) {
            String[] parties = caseSpeciale.split(":");
            plateau.placerCaseSpeciale(Integer.parseInt(parties[0].trim()),
                    CaseSpeciale.valueOf(parties[1].trim()));
        }
    }

    /**
     * Lit la position d'un cheval. Accepte aussi l'ancien format « position;sorti;termine ».
     */
    private static int lirePosition(String valeur) {
        String[] etat = valeur.split(";");
        if (etat.length == 3 && Boolean.parseBoolean(etat[2])) {
            return Cheval.CENTRE;
        }
        if (etat.length == 3 && !Boolean.parseBoolean(etat[1])) {
            return Cheval.ECURIE;
        }
        return Integer.parseInt(etat[0].trim());
    }

    private static String lire(Properties donnees, String cle) throws IOException {
        String valeur = donnees.getProperty(cle);
        if (valeur == null) {
            throw new IOException("Donnée manquante dans la sauvegarde : " + cle);
        }
        return valeur;
    }
}
