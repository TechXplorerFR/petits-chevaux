package fr.utbm.petitschevaux.controller;

import fr.utbm.petitschevaux.persistence.BaseDeDonnees;
import fr.utbm.petitschevaux.PetitsChevauxApp;
import fr.utbm.petitschevaux.model.BilanParties;
import fr.utbm.petitschevaux.model.MoyennesStatistiques;
import fr.utbm.petitschevaux.model.Profil;
import fr.utbm.petitschevaux.model.ResumePartie;
import fr.utbm.petitschevaux.model.StatistiquesJoueur;
import fr.utbm.petitschevaux.persistence.ScoresDAO;
import fr.utbm.petitschevaux.view.Rendu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Contrôleur de l'écran des scores : classement des joueurs et dernières parties terminées.
 */
public class ScoresController {
    private static final int NOMBRE_JOUEURS = 10;
    private static final int NOMBRE_PARTIES = 10;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label message;
    @FXML private GridPane classement;
    @FXML private GridPane parties;
    @FXML private VBox zoneDetails;
    @FXML private Label titreDetails;
    @FXML private GridPane details;
    @FXML private VBox zoneMoyennes;
    @FXML private Label bilan;
    @FXML private GridPane moyennes;

    private final ScoresDAO scores = new ScoresDAO(BaseDeDonnees.parDefaut());
    /** Partie terminée la plus récente, ou {@code null} s'il n'y en a aucune. */
    private ResumePartie dernierePartie;

    /**
     * Initialise l'écran des scores en lisant les meilleurs joueurs et les dernières parties terminées dans la base de données.
     */
    @FXML
    private void initialize() {
        try {
            List<Profil> profils = scores.meilleursJoueurs(NOMBRE_JOUEURS);
            List<ResumePartie> dernieres = scores.dernieresParties(NOMBRE_PARTIES);
            message.setText(dernieres.isEmpty()
                    ? "Aucune partie terminée pour l'instant : jouez jusqu'au bout pour apparaître ici."
                    : "Seules les parties terminées comptent. Les bots n'ont pas de classement.");
            afficherClassement(profils);
            afficherParties(dernieres);
            // La dernière partie jouée est détaillée d'office ; « Détails » en affiche une autre.
            if (!dernieres.isEmpty()) {
                dernierePartie = dernieres.get(0);
                afficherDetails(dernierePartie);
            }
            afficherMoyennes(scores.bilanParties(), scores.moyennesStatistiques());
        } catch (SQLException e) {
            message.setText("Impossible de lire les scores : " + e.getMessage());
        }
    }

    /**
     * Affiche le classement des meilleurs joueurs dans la grille correspondante.
     * @param profils la liste des profils des meilleurs joueurs
     */
    private void afficherClassement(List<Profil> profils) {
        entetes(classement, "", "Joueur", "Victoires", "Parties", "Réussite");
        for (int rang = 0; rang < profils.size(); rang++) {
            Profil profil = profils.get(rang);
            ligne(classement, rang + 1, rang == 0,
                    (rang + 1) + (rang == 0 ? "er" : "e"),
                    profil.nom(),
                    String.valueOf(profil.victoires()),
                    String.valueOf(profil.parties()),
                    String.format(Locale.FRANCE, "%.0f %%", profil.ratioVictoires() * 100));
        }
    }

    /**
     * Affiche les dernières parties terminées dans la grille correspondante.
     * @param liste la liste des résulats des dernières parties
     */
    private void afficherParties(List<ResumePartie> liste) {
        entetes(parties, "Date", "Gagnant", "Mode", "Durée", "Lancers", "");
        for (int i = 0; i < liste.size(); i++) {
            ResumePartie partie = liste.get(i);
            ligne(parties, i + 1, false,
                    partie.fin().format(FORMAT_DATE),
                    partie.vainqueur() + (partie.vainqueurBot() ? " (bot)" : ""),
                    partie.mode().getLibelle(),
                    formaterDuree(partie.duree()),
                    String.valueOf(partie.lancers()));
            Button voir = new Button("Détails");
            voir.setOnAction(event -> afficherDetails(partie));
            parties.add(voir, 5, i + 1);
        }
    }

    /**
     * Affiche le bilan de toutes les parties et, par groupe de joueurs, les statistiques
     * moyennes d'un joueur sur une partie.
     */
    private void afficherMoyennes(BilanParties bilanParties, List<MoyennesStatistiques> lignes) {
        if (bilanParties.parties() == 0) {
            return;
        }
        zoneMoyennes.setVisible(true);
        zoneMoyennes.setManaged(true);
        bilan.setText(bilanParties.parties() + (bilanParties.parties() == 1 ? " partie terminée" : " parties terminées")
                + " · durée moyenne " + formaterDuree(bilanParties.dureeMoyenne())
                + " · " + nombre(bilanParties.lancersMoyens()) + " lancers par partie");
        if (lignes.isEmpty()) {
            return;
        }
        entetes(moyennes, "Par joueur et par partie", "Arrivés", "Lancers", "Moyenne", "Six", "Tours passés",
                "Sorties", "Cases", "Captures", "Perdus", "Cases spéciales");
        for (int i = 0; i < lignes.size(); i++) {
            MoyennesStatistiques m = lignes.get(i);
            ligne(moyennes, i + 1, i == 0,
                    m.groupe() + " (" + m.participations() + ")",
                    nombre(m.chevauxArrives()) + "/4",
                    nombre(m.lancers()),
                    String.format(Locale.FRANCE, "%.2f", m.moyenneDe()),
                    nombre(m.six()),
                    nombre(m.toursPasses()),
                    nombre(m.sorties()),
                    nombre(m.casesParcourues()),
                    nombre(m.captures()),
                    nombre(m.chevauxPerdus()),
                    nombre(m.casesSpeciales()));
        }
    }

    /** Une moyenne arrondie à un chiffre après la virgule. */
    private static String nombre(double valeur) {
        return String.format(Locale.FRANCE, "%.1f", valeur);
    }

    /**
     * Affiche toutes les statistiques enregistrées pour une partie : une ligne par joueur,
     * du vainqueur au dernier.
     */
    private void afficherDetails(ResumePartie partie) {
        details.getChildren().clear();
        zoneDetails.setVisible(true);
        zoneDetails.setManaged(true);
        titreDetails.setText((partie.equals(dernierePartie) ?"Dernière partie, le " : "Partie du ")
                + partie.fin().format(FORMAT_DATE) + " (" + partie.mode().getLibelle() + ", "
                + formaterDuree(partie.duree()) + ", " + partie.lancers() + " lancers)");
        List<StatistiquesJoueur> lignes;
        try {
            lignes = scores.statistiquesPartie(partie.id());
        } catch (SQLException e) {
            titreDetails.setText("Impossible de lire les statistiques : " + e.getMessage());
            return;
        }
        if (lignes.isEmpty()) {
            titreDetails.setText(titreDetails.getText() + " : statistiques détaillées non disponibles"
                    + " (partie enregistrée avant leur ajout).");
            return;
        }
        entetes(details, "", "Joueur", "Arrivés", "Lancers", "Moyenne", "Six", "Tours passés",
                "Sorties", "Cases", "Captures", "Perdus", "Cases spéciales");
        for (int i = 0; i < lignes.size(); i++) {
            StatistiquesJoueur stats = lignes.get(i);
            ligne(details, i + 1, stats.vainqueur(),
                    stats.rang() + (stats.rang() == 1 ? "er" : "e"),
                    null,
                    stats.chevauxArrives() + "/4",
                    String.valueOf(stats.lancers()),
                    String.format(Locale.FRANCE, "%.2f", stats.moyenneDe()),
                    String.valueOf(stats.six()),
                    String.valueOf(stats.toursPasses()),
                    String.valueOf(stats.sorties()),
                    String.valueOf(stats.casesParcourues()),
                    String.valueOf(stats.captures()),
                    String.valueOf(stats.chevauxPerdus()),
                    String.valueOf(stats.casesSpeciales()));
            Label nom = cellule(stats.nom() + (stats.bot() ? " (bot)" : ""), true);
            nom.setGraphic(Rendu.pion(stats.couleur(), 18));
            nom.setTextFill(Rendu.couleur(stats.couleur()));
            details.add(nom, 1, i + 1);
        }
    }

    /**
     * Ajoute les entêtes de colonnes dans la grille donnée.
     * @param grille la grille dans laquelle ajouter les entêtes
     * @param textes les textes des entêtes
     */
    private static void entetes(GridPane grille, String... textes) {
        for (int colonne = 0; colonne < textes.length; colonne++) {
            Label entete = cellule(textes[colonne], true);
            entete.setStyle(entete.getStyle() + " -fx-text-fill: #6b665e;");
            grille.add(entete, colonne, 0);
        }
    }

    private static void ligne(GridPane grille, int numero, boolean gras, String... textes) {
        for (int colonne = 0; colonne < textes.length; colonne++) {
            if (textes[colonne] != null) { // null : cellule remplie à part
                grille.add(cellule(textes[colonne], gras), colonne, numero);
            }
        }
    }

    private static Label cellule(String texte, boolean gras) {
        Label label = new Label(texte);
        label.setStyle("-fx-padding: 6 10 6 10;" + (gras ? " -fx-font-weight: bold;" : ""));
        return label;
    }

    private static String formaterDuree(Duration duree) {
        long minutes = duree.toMinutes();
        long secondes = duree.toSecondsPart();
        return minutes == 0 ? secondes + " s" : minutes + " min " + secondes + " s";
    }

    /**
     * Gère le clic sur le bouton "Retour au menu".
     */
    @FXML
    private void onRetourMenu() {
        try {
            PetitsChevauxApp.afficherMenu((Stage) message.getScene().getWindow());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le menu", e);
        }
    }
}
