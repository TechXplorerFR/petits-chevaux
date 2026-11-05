package fr.utbm.petitschevaux.controller;

import fr.utbm.petitschevaux.persistence.BaseDeDonnees;
import fr.utbm.petitschevaux.PetitsChevauxApp;
import fr.utbm.petitschevaux.model.BilanParties;
import fr.utbm.petitschevaux.model.ModeJeu;
import fr.utbm.petitschevaux.model.MoyennesStatistiques;
import fr.utbm.petitschevaux.model.Profil;
import fr.utbm.petitschevaux.model.ResumePartie;
import fr.utbm.petitschevaux.model.StatistiquesJoueur;
import fr.utbm.petitschevaux.persistence.ScoresDAO;
import fr.utbm.petitschevaux.view.Rendu;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Contrôleur de l'écran des scores : classement des joueurs, puis un onglet par mode de jeu
 * avec les moyennes depuis la première partie, la dernière partie détaillée et l'historique
 * des dernières parties.
 */
public class ScoresController {
    private static final int NOMBRE_JOUEURS = 10;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String STYLE_TITRE = "-fx-font-weight: bold; -fx-font-size: 15px;";
    private static final String STYLE_GRILLE = "-fx-background-color: white; -fx-background-radius: 8;";
    private static final String[] COLONNES_STATS = { "Arrivés", "Lancers", "Moyenne", "Six", "Tours passés",
            "Sorties", "Cases", "Captures", "Perdus", "Cases spéciales" };

    @FXML private Label message;
    @FXML private GridPane classement;
    @FXML private TabPane ongletsModes;

    private final ScoresDAO scores = new ScoresDAO(BaseDeDonnees.parDefaut());

    /**
     * Lit les scores dans la base de données et remplit l'écran.
     */
    @FXML
    private void initialize() {
        try {
            List<Profil> profils = scores.meilleursJoueurs(NOMBRE_JOUEURS);
            afficherClassement(profils);
            boolean aucunePartie = true;
            for (ModeJeu mode : ModeJeu.values()) {
                OngletMode onglet = new OngletMode(mode);
                aucunePartie &= onglet.remplir() == 0;
                ongletsModes.getTabs().add(onglet.tab);
            }
            message.setText(aucunePartie
                    ? "Aucune partie terminée pour l'instant : jouez jusqu'au bout pour apparaître ici."
                    : "Les moyennes portent sur toutes les parties depuis la première ; l'historique garde les "
                            + ScoresDAO.PARTIES_CONSERVEES + " dernières de chaque mode. Les bots n'ont pas de classement.");
        } catch (SQLException e) {
            message.setText("Impossible de lire les scores : " + e.getMessage());
        }
    }

    /**
     * Affiche le classement des meilleurs joueurs (tous modes confondus).
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
     * Onglet d'un mode de jeu : moyennes depuis la première partie, dernière partie
     * détaillée (ou celle choisie avec « Détails ») et dernières parties.
     */
    private final class OngletMode {
        private final ModeJeu mode;
        private final Tab tab;
        private final VBox contenu = new VBox(16);
        private final Label titreDetails = titre("");
        private final GridPane details = grille();
        private ResumePartie dernierePartie;

        OngletMode(ModeJeu mode) {
            this.mode = mode;
            contenu.setAlignment(Pos.TOP_CENTER);
            contenu.setPadding(new Insets(16, 0, 8, 0));
            tab = new Tab(mode.getLibelle(), contenu);
        }

        /**
         * Remplit l'onglet depuis la base.
         *
         * @return le nombre de parties terminées dans ce mode depuis le début
         */
        int remplir() throws SQLException {
            BilanParties bilan = scores.bilanParties(mode);
            List<ResumePartie> historique = scores.dernieresParties(mode, ScoresDAO.PARTIES_CONSERVEES);
            tab.setText(mode.getLibelle() + " (" + bilan.parties() + ")");
            if (bilan.parties() == 0 && historique.isEmpty()) {
                contenu.getChildren().add(new Label("Aucune partie terminée dans ce mode pour l'instant."));
                return 0;
            }
            afficherMoyennes(bilan, scores.moyennesStatistiques(mode));
            if (!historique.isEmpty()) {
                dernierePartie = historique.get(0);
                contenu.getChildren().add(section(titreDetails, details));
                afficherDetails(dernierePartie);
                afficherHistorique(historique);
            }
            return bilan.parties();
        }

        /** Bilan des parties et statistiques moyennes d'un joueur sur une partie, par groupe. */
        private void afficherMoyennes(BilanParties bilan, List<MoyennesStatistiques> lignes) {
            Label resume = new Label(bilan.parties() + (bilan.parties() == 1 ? " partie terminée" : " parties terminées")
                    + " depuis la première · durée moyenne " + formaterDuree(bilan.dureeMoyenne())
                    + " · " + nombre(bilan.lancersMoyens()) + " lancers par partie");
            resume.setStyle("-fx-text-fill: #6b665e;");
            GridPane moyennes = grille();
            String[] colonnes = new String[COLONNES_STATS.length + 1];
            colonnes[0] = "Par joueur et par partie";
            System.arraycopy(COLONNES_STATS, 0, colonnes, 1, COLONNES_STATS.length);
            entetes(moyennes, colonnes);
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
            retirerCasesSpecialesSiInutile(moyennes);
            contenu.getChildren().add(section(titre("Moyennes sur toutes les parties"), resume, moyennes));
        }

        /** Les dernières parties du mode, chacune avec son bouton « Détails ». */
        private void afficherHistorique(List<ResumePartie> historique) {
            GridPane parties = grille();
            entetes(parties, "Date", "Gagnant", "Durée", "Lancers", "");
            for (int i = 0; i < historique.size(); i++) {
                ResumePartie partie = historique.get(i);
                ligne(parties, i + 1, false,
                        partie.fin().format(FORMAT_DATE),
                        partie.vainqueur() + (partie.vainqueurBot() ? " (bot)" : ""),
                        formaterDuree(partie.duree()),
                        String.valueOf(partie.lancers()));
                Button voir = new Button("Détails");
                voir.setOnAction(event -> afficherDetails(partie));
                parties.add(voir, 4, i + 1);
            }
            contenu.getChildren().add(section(
                    titre(historique.size() + (historique.size() == 1 ? " dernière partie" : " dernières parties")),
                    parties));
        }

        /** Toutes les statistiques d'une partie : une ligne par joueur, du vainqueur au dernier. */
        private void afficherDetails(ResumePartie partie) {
            details.getChildren().clear();
            titreDetails.setText((partie.equals(dernierePartie) ? "Dernière partie, le " : "Partie du ")
                    + partie.fin().format(FORMAT_DATE) + " (" + formaterDuree(partie.duree()) + ", "
                    + partie.lancers() + " lancers)");
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
            String[] colonnes = new String[COLONNES_STATS.length + 2];
            colonnes[0] = "";
            colonnes[1] = "Joueur";
            System.arraycopy(COLONNES_STATS, 0, colonnes, 2, COLONNES_STATS.length);
            entetes(details, colonnes);
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
            retirerCasesSpecialesSiInutile(details);
        }

        /** En mode classique, la colonne « Cases spéciales » (toujours à 0) est retirée : c'est la dernière. */
        private void retirerCasesSpecialesSiInutile(GridPane grille) {
            if (mode == ModeJeu.CASES_SPECIALES) {
                return;
            }
            int derniere = grille.getChildren().stream()
                    .mapToInt(noeud -> GridPane.getColumnIndex(noeud) == null ? 0 : GridPane.getColumnIndex(noeud))
                    .max().orElse(0);
            grille.getChildren().removeIf(noeud -> {
                Integer colonne = GridPane.getColumnIndex(noeud);
                return colonne != null && colonne == derniere;
            });
        }
    }

    private static Label titre(String texte) {
        Label titre = new Label(texte);
        titre.setStyle(STYLE_TITRE);
        return titre;
    }

    private static GridPane grille() {
        GridPane grille = new GridPane();
        grille.setStyle(STYLE_GRILLE);
        grille.setMaxWidth(Region.USE_PREF_SIZE);
        return grille;
    }

    private static VBox section(javafx.scene.Node... noeuds) {
        VBox section = new VBox(8, noeuds);
        section.setAlignment(Pos.TOP_CENTER);
        return section;
    }

    /** Une moyenne arrondie à un chiffre après la virgule. */
    private static String nombre(double valeur) {
        return String.format(Locale.FRANCE, "%.1f", valeur);
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
