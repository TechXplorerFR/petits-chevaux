package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * Contrôleur de l'écran de fin de partie : vainqueur, classement et statistiques.
 */
public class VictoireController {
    private static final String[] COLONNES = {
            "", "Joueur", "Arrivés", "Lancers", "Moy. dé", "6 obtenus",
            "Cases", "Captures", "Perdus", "Tours passés" };

    @FXML private Label titre;
    @FXML private Label sousTitre;
    @FXML private HBox resume;
    @FXML private GridPane tableau;
    @FXML private FlowPane distinctions;

    private Jeu jeu;

    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        Joueur vainqueur = jeu.getVainqueur();
        titre.setText(vainqueur.getNom() + " a gagné !");
        titre.setTextFill(Rendu.couleur(vainqueur.getCouleur()));
        int lancers = vainqueur.getStatistiques().getLancers();
        sousTitre.setText("Ses quatre chevaux sont arrivés au centre en "
                + lancers + (lancers > 1 ? " lancers." : " lancer."));
        afficherResume();
        afficherTableau();
        afficherDistinctions();
    }

    private void afficherResume() {
        int captures = jeu.getJoueurs().stream()
                .mapToInt(joueur -> joueur.getStatistiques().getCaptures()).sum();
        int six = jeu.getJoueurs().stream()
                .mapToInt(joueur -> joueur.getStatistiques().getSix()).sum();
        resume.getChildren().setAll(
                tuile("Durée", formaterDuree(jeu.getDuree())),
                tuile("Lancers de dé", String.valueOf(jeu.getNombreLancers())),
                tuile("6 obtenus", String.valueOf(six)),
                tuile("Chevaux capturés", String.valueOf(captures)));
    }

    private VBox tuile(String libelle, String valeur) {
        Label nombre = new Label(valeur);
        nombre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label texte = new Label(libelle);
        texte.setStyle("-fx-text-fill: #6b665e;");
        VBox tuile = new VBox(2, nombre, texte);
        tuile.setAlignment(Pos.CENTER);
        tuile.setPrefWidth(150);
        tuile.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10;");
        return tuile;
    }

    private boolean casesSpeciales() {
        return jeu.getMode() == ModeJeu.CASES_SPECIALES;
    }

    private void afficherTableau() {
        List<String> colonnes = new ArrayList<>(List.of(COLONNES));
        if (casesSpeciales()) {
            colonnes.add("Spéciales");
        }
        for (int colonne = 0; colonne < colonnes.size(); colonne++) {
            Label entete = cellule(colonnes.get(colonne), true);
            entete.setStyle(entete.getStyle() + " -fx-text-fill: #6b665e;");
            tableau.add(entete, colonne, 0);
        }
        List<Joueur> classement = jeu.getClassement();
        for (int rang = 0; rang < classement.size(); rang++) {
            Joueur joueur = classement.get(rang);
            Statistiques stats = joueur.getStatistiques();
            Label nom = cellule(joueur.getNom() + (joueur.isBot() ? " (bot)" : ""), true);
            nom.setTextFill(Rendu.couleur(joueur.getCouleur()));
            String[] valeurs = {
                    (rang + 1) + (rang == 0 ? "er" : "e"), null,
                    joueur.getChevauxArrives() + "/" + Joueur.NOMBRE_CHEVAUX,
                    String.valueOf(stats.getLancers()),
                    String.format(Locale.FRANCE, "%.2f", stats.getMoyenneDe()),
                    String.valueOf(stats.getSix()),
                    String.valueOf(stats.getCasesParcourues()),
                    String.valueOf(stats.getCaptures()),
                    String.valueOf(stats.getChevauxPerdus()),
                    String.valueOf(stats.getToursPasses()),
                    String.valueOf(stats.getCasesSpeciales()) };
            for (int colonne = 0; colonne < colonnes.size(); colonne++) {
                tableau.add(colonne == 1 ? nom : cellule(valeurs[colonne], rang == 0), colonne, rang + 1);
            }
        }
    }

    private Label cellule(String texte, boolean gras) {
        Label label = new Label(texte);
        label.setStyle("-fx-padding: 8 10 8 10;" + (gras ? " -fx-font-weight: bold;" : ""));
        return label;
    }

    /**
     * Petites récompenses pour animer la fin de partie.
     */
    private void afficherDistinctions() {
        distinctions.getChildren().clear();
        ajouterDistinction("Le plus chanceux", "fois le 6", Statistiques::getSix);
        ajouterDistinction("Le chasseur", "capture(s)", Statistiques::getCaptures);
        ajouterDistinction("Le souffre-douleur", "cheval(aux) perdu(s)", Statistiques::getChevauxPerdus);
        ajouterDistinction("Le marathonien", "cases parcourues", Statistiques::getCasesParcourues);
        ajouterDistinctionMoyenne();
        if (casesSpeciales()) {
            ajouterDistinction("L'aventurier", "case(s) spéciale(s)", Statistiques::getCasesSpeciales);
        }
    }

    private void ajouterDistinction(String titre, String unite, ToIntFunction<Statistiques> critere) {
        jeu.getJoueurs().stream()
                .max(Comparator.comparingInt(joueur -> critere.applyAsInt(joueur.getStatistiques())))
                .filter(joueur -> critere.applyAsInt(joueur.getStatistiques()) > 0)
                .ifPresent(joueur -> distinctions.getChildren().add(badge(titre, joueur,
                        critere.applyAsInt(joueur.getStatistiques()) + " " + unite)));
    }

    /**
     * Le joueur avec la plus faible moyenne au dé, s'il y a au moins deux joueurs à comparer.
     */
    private void ajouterDistinctionMoyenne() {
        ToDoubleFunction<Joueur> moyenne = joueur -> joueur.getStatistiques().getMoyenneDe();
        List<Joueur> ontLance = jeu.getJoueurs().stream()
                .filter(joueur -> joueur.getStatistiques().getLancers() > 0)
                .toList();
        if (ontLance.size() < 2) {
            return;
        }
        ontLance.stream()
                .min(Comparator.comparingDouble(moyenne))
                .ifPresent(joueur -> distinctions.getChildren().add(badge("Le malchanceux", joueur,
                        String.format(Locale.FRANCE, "%.2f de moyenne au dé", moyenne.applyAsDouble(joueur)))));
    }

    private VBox badge(String titre, Joueur joueur, String detail) {
        Label libelle = new Label(titre);
        libelle.setStyle("-fx-text-fill: #6b665e;");
        Label nom = new Label(joueur.getNom());
        nom.setStyle("-fx-font-weight: bold;");
        nom.setTextFill(Rendu.couleur(joueur.getCouleur()));
        VBox badge = new VBox(2, libelle, nom, new Label(detail));
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8 14 8 14;"
                + " -fx-border-color: " + Rendu.css(joueur.getCouleur()) + "; -fx-border-radius: 8;");
        return badge;
    }

    private static String formaterDuree(Duration duree) {
        long minutes = duree.toMinutes();
        long secondes = duree.toSecondsPart();
        return minutes == 0 ? secondes + " s" : minutes + " min " + secondes + " s";
    }

    /**
     * Relance une partie avec les mêmes joueurs humains (les bots sont tirés à nouveau).
     */
    @FXML
    private void onRejouer() {
        List<Joueur> humains = jeu.getJoueurs().stream()
                .filter(joueur -> !joueur.isBot())
                .map(joueur -> new Joueur(joueur.getNom(), joueur.getCouleur()))
                .toList();
        try {
            PetitsChevauxApp.afficherTirage(getStage(), new Jeu(humains, jeu.getMode()));
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible de relancer une partie", e);
        }
    }

    @FXML
    private void onRetourMenu() {
        try {
            PetitsChevauxApp.afficherMenu(getStage());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le menu", e);
        }
    }

    private Stage getStage() {
        return (Stage) titre.getScene().getWindow();
    }
}
