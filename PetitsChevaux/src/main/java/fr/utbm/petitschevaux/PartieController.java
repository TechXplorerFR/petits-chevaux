package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class PartieController {
    private Jeu jeu;
    private PlateauView plateauView;

    @FXML
    private Pane zonePlateau;

    @FXML
    private Label information;

    @FXML private Label resultatDe;

    /**
     * Branche une partie (nouvelle ou chargée) sur la vue.
     * Appelé par {@link HelloApplication#afficherPartie} après le chargement du FXML.
     */
    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        plateauView = new PlateauView(jeu.getPlateau(), jeu.getJoueurs());
        zonePlateau.getChildren().setAll(plateauView.getRoot());
        actualiserVue();
    }

    @FXML
    public void onLancerDe() {
        jeu.jouerTour();
        actualiserVue();
    }

    @FXML
    public void onChoisirCheval() {
        // Le premier cheval déplaçable est choisi automatiquement dans cette version.
        actualiserVue();
    }

    @FXML
    public void onSauvegarder() {
        FileChooser choix = new FileChooser();
        choix.setTitle("Sauvegarder la partie");
        choix.setInitialFileName("partie." + Sauvegarde.EXTENSION);
        choix.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Partie de petits chevaux", "*." + Sauvegarde.EXTENSION));
        File fichier = choix.showSaveDialog(getStage());
        if (fichier == null) {
            return;
        }
        try {
            Sauvegarde.sauvegarder(jeu, fichier.toPath());
            information.setText("Partie sauvegardée.");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de sauvegarder :\n" + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void onRetourMenu() {
        try {
            HelloApplication.afficherMenu(getStage());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le menu :\n" + e.getMessage()).showAndWait();
        }
    }

    public void actualiserVue() {
        if (plateauView != null) {
            plateauView.dessinerPlateau();
        }
        if (information != null) {
            Joueur joueur = jeu.getJoueurs().get(jeu.getJoueurCourant());
            information.setText(joueur.getNom() + " - lancez le dé");
        }
        if (resultatDe != null) {
            int valeurDe = jeu.getDe().getValeur();
            resultatDe.setText("Résultat du dé : " + (valeurDe == 0 ? "-" : valeurDe));
        }
    }

    private Stage getStage() {
        return (Stage) zonePlateau.getScene().getWindow();
    }
}
