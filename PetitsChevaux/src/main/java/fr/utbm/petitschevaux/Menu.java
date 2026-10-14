package fr.utbm.petitschevaux;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur du menu principal : choix des joueurs humains (les places libres
 * sont prises par des bots),
 * lancement d'une nouvelle partie ou chargement d'une partie sauvegardée.
 */
public class Menu {
    public static final int MIN_HUMAINS = 1;
    public static final int MAX_HUMAINS = Jeu.MAX_JOUEURS;

    @FXML
    private Spinner<Integer> nombreJoueurs;

    @FXML
    private VBox champsJoueurs;

    @FXML private RadioButton modeClassique;
    @FXML private RadioButton modeCasesSpeciales;
    @FXML private Label descriptionMode;
    private final ToggleGroup groupeMode = new ToggleGroup();

    private final List<HBox> lignesJoueurs = new ArrayList<>();
    private final List<TextField> nomsJoueurs = new ArrayList<>();

    @FXML
    private void initialize() {
        nombreJoueurs.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_HUMAINS, MAX_HUMAINS, 2));

        Couleur[] couleurs = Couleur.values();
        for (int i = 0; i < MAX_HUMAINS; i++) {
            Label couleur = new Label(couleurs[i].name());
            couleur.setPrefWidth(70);
            couleur.setTextFill(Rendu.couleur(couleurs[i]));
            TextField nom = new TextField("Joueur " + (i + 1));
            HBox ligne = new HBox(10, couleur, nom);
            nomsJoueurs.add(nom);
            lignesJoueurs.add(ligne);
            champsJoueurs.getChildren().add(ligne);
        }

        nombreJoueurs.valueProperty().addListener((obs, ancien, nouveau) -> afficherChampsJoueurs(nouveau));
        afficherChampsJoueurs(nombreJoueurs.getValue());

        modeClassique.setToggleGroup(groupeMode);
        modeCasesSpeciales.setToggleGroup(groupeMode);
        groupeMode.selectedToggleProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau == null) {
                // Un groupe de boutons radio ne doit jamais rester sans choix.
                ancien.setSelected(true);
            }
            descriptionMode.setText(modeChoisi().getDescription());
        });
        descriptionMode.setText(modeChoisi().getDescription());
    }

    private ModeJeu modeChoisi() {
        return modeCasesSpeciales.isSelected() ? ModeJeu.CASES_SPECIALES : ModeJeu.CLASSIQUE;
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
        Couleur[] couleurs = Couleur.values();
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 0; i < nombreJoueurs.getValue(); i++) {
            String nom = nomsJoueurs.get(i).getText().trim();
            joueurs.add(new Joueur(nom.isEmpty() ? "Joueur " + (i + 1) : nom, couleurs[i]));
        }
        try {
            PetitsChevauxApp.afficherTirage(getStage(), new Jeu(joueurs, modeChoisi()));
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le tirage", e);
        }
    }

    @FXML
    private void onChargerPartie() {
        FileChooser choix = new FileChooser();
        choix.setTitle("Charger une partie");
        choix.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Partie de petits chevaux", "*." + Sauvegarde.EXTENSION));
        File fichier = choix.showOpenDialog(getStage());
        if (fichier == null) {
            return;
        }
        try {
            PetitsChevauxApp.afficherPartie(getStage(), Sauvegarde.charger(fichier.toPath()));
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
