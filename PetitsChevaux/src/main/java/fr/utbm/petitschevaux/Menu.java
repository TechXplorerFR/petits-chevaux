package fr.utbm.petitschevaux;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur du menu principal : choix des joueurs humains et de leur couleur (les places
 * libres sont prises par des bots), lancement d'une nouvelle partie ou chargement d'une
 * partie sauvegardée.
 */
public class Menu {
    public static final int MIN_HUMAINS = 1;
    public static final int MAX_HUMAINS = Jeu.MAX_JOUEURS;

    /** Une sauvegarde proposée au chargement, affichée par son nom de fichier. */
    private record EntreeSauvegarde(Path fichier) {
        @Override
        public String toString() {
            String nom = fichier.getFileName().toString();
            return nom.substring(0, nom.length() - Sauvegarde.EXTENSION.length() - 1);
        }
    }

    @FXML
    private Spinner<Integer> nombreJoueurs;

    @FXML
    private VBox champsJoueurs;

    private final List<HBox> lignesJoueurs = new ArrayList<>();
    private final List<TextField> nomsJoueurs = new ArrayList<>();
    private final List<ComboBox<Couleur>> couleursJoueurs = new ArrayList<>();

    @FXML
    private void initialize() {
        nombreJoueurs.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_HUMAINS, MAX_HUMAINS, 2));

        Couleur[] couleurs = Couleur.values();
        for (int i = 0; i < MAX_HUMAINS; i++) {
            ComboBox<Couleur> couleur = creerChoixCouleur(couleurs[i]);
            TextField nom = new TextField("Joueur " + (i + 1));
            HBox ligne = new HBox(10, couleur, nom);
            couleursJoueurs.add(couleur);
            nomsJoueurs.add(nom);
            lignesJoueurs.add(ligne);
            champsJoueurs.getChildren().add(ligne);
        }

        nombreJoueurs.valueProperty().addListener((obs, ancien, nouveau) -> afficherChampsJoueurs(nouveau));
        afficherChampsJoueurs(nombreJoueurs.getValue());
    }

    /**
     * Liste déroulante des couleurs. Choisir la couleur d'un autre joueur échange les deux
     * couleurs : les quatre lignes ont donc toujours des couleurs différentes.
     */
    private ComboBox<Couleur> creerChoixCouleur(Couleur initiale) {
        ComboBox<Couleur> choix = new ComboBox<>();
        choix.getItems().setAll(Couleur.values());
        choix.setValue(initiale);
        choix.setPrefWidth(110);
        choix.setCellFactory(liste -> new CelluleCouleur());
        choix.setButtonCell(new CelluleCouleur());
        choix.valueProperty().addListener((obs, ancienne, nouvelle) -> {
            for (ComboBox<Couleur> autre : couleursJoueurs) {
                if (autre != choix && autre.getValue() == nouvelle) {
                    autre.setValue(ancienne);
                }
            }
        });
        return choix;
    }

    /** Affiche une couleur avec une pastille et son nom écrit dans cette couleur. */
    private static final class CelluleCouleur extends ListCell<Couleur> {
        @Override
        protected void updateItem(Couleur couleur, boolean vide) {
            super.updateItem(couleur, vide);
            if (vide || couleur == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            String nom = couleur.name();
            setText(nom.charAt(0) + nom.substring(1).toLowerCase());
            setTextFill(Rendu.couleur(couleur));
            setGraphic(new Circle(6, Rendu.couleur(couleur)));
        }
    }

    private void afficherChampsJoueurs(int nombre) {
        for (int i = 0; i < lignesJoueurs.size(); i++) {
            HBox ligne = lignesJoueurs.get(i);
            ligne.setVisible(i < nombre);
            ligne.setManaged(i < nombre);
        }
    }

    @FXML
    private void onNouvellePartie() {
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 0; i < nombreJoueurs.getValue(); i++) {
            String nom = nomsJoueurs.get(i).getText().trim();
            joueurs.add(new Joueur(nom.isEmpty() ? "Joueur " + (i + 1) : nom,
                    couleursJoueurs.get(i).getValue()));
        }
        try {
            PetitsChevauxApp.afficherTirage(getStage(), new Jeu(joueurs));
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le tirage", e);
        }
    }

    /**
     * Propose les parties du dossier des sauvegardes, la plus récente en premier.
     */
    @FXML
    private void onChargerPartie() {
        List<EntreeSauvegarde> sauvegardes;
        try {
            sauvegardes = Sauvegarde.listerSauvegardes(Sauvegarde.DOSSIER).stream()
                    .map(EntreeSauvegarde::new)
                    .toList();
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible de lire le dossier des sauvegardes", e);
            return;
        }
        if (sauvegardes.isEmpty()) {
            Alert aucune = new Alert(Alert.AlertType.INFORMATION,
                    "Aucune partie sauvegardée dans " + Sauvegarde.DOSSIER + ".");
            aucune.setHeaderText("Pas encore de sauvegarde");
            aucune.showAndWait();
            return;
        }
        ChoiceDialog<EntreeSauvegarde> choix = new ChoiceDialog<>(sauvegardes.get(0), sauvegardes);
        choix.setTitle("Charger une partie");
        choix.setHeaderText("Choisissez une partie sauvegardée");
        choix.setContentText("Partie :");
        Optional<EntreeSauvegarde> choisie = choix.showAndWait();
        if (choisie.isEmpty()) {
            return;
        }
        Path fichier = choisie.get().fichier();
        try {
            PetitsChevauxApp.afficherPartie(getStage(), Sauvegarde.charger(fichier), fichier);
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible de charger la partie", e);
        }
    }

    @FXML
    private void onQuitter() {
        Platform.exit();
    }

    private Stage getStage() {
        return (Stage) champsJoueurs.getScene().getWindow();
    }
}
