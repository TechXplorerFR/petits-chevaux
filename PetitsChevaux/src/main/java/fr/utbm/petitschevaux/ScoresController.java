package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
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

    /**
     * Initialise l'écran des scores en lisant les meilleurs joueurs et les dernières parties terminées dans la base de données.
     */
    @FXML
    private void initialize() {
        ScoresDAO scores = new ScoresDAO(BaseDeDonnees.parDefaut());
        try {
            List<Profil> profils = scores.meilleursJoueurs(NOMBRE_JOUEURS);
            List<ResumePartie> dernieres = scores.dernieresParties(NOMBRE_PARTIES);
            message.setText(profils.isEmpty()
                    ? "Aucune partie terminée pour l'instant : jouez jusqu'au bout pour apparaître ici."
                    : "Seules les parties terminées comptent. Les bots n'ont pas de classement.");
            afficherClassement(profils);
            afficherParties(dernieres);
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
        entetes(parties, "Date", "Gagnant", "Mode", "Durée", "Lancers");
        for (int i = 0; i < liste.size(); i++) {
            ResumePartie partie = liste.get(i);
            ligne(parties, i + 1, false,
                    partie.fin().format(FORMAT_DATE),
                    partie.vainqueur() + (partie.vainqueurBot() ? " (bot)" : ""),
                    partie.mode().getLibelle(),
                    formaterDuree(partie.duree()),
                    String.valueOf(partie.lancers()));
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

    /**
     * Ajoute une ligne de données dans la grille donnée.
     * @param texte les textes des cellules de la ligne
     * @param gras si les cellules doivent être en gras
     * @return la ligne ajoutée
     */
    private static Label cellule(String texte, boolean gras) {
        Label label = new Label(texte);
        label.setStyle("-fx-padding: 6 10 6 10;" + (gras ? " -fx-font-weight: bold;" : ""));
        return label;
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
