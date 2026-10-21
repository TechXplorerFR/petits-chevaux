package fr.utbm.petitschevaux;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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

    @FXML private RadioButton modeClassique;
    @FXML private RadioButton modeCasesSpeciales;
    @FXML private Label descriptionMode;
    private final ToggleGroup groupeMode = new ToggleGroup();

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
            PetitsChevauxApp.afficherTirage(getStage(), new Jeu(joueurs, modeChoisi()));
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

    /**
     * Héberge une partie en ligne : ouvre le serveur et affiche le salon. L'hôte joue avec
     * le nom et la couleur de la première ligne, et les règles choisies ci-dessus.
     */
    @FXML
    private void onHeberger() {
        try {
            SessionEnLigne session = SessionEnLigne.heberger(Platform::runLater, ServeurJeu.PORT_PAR_DEFAUT,
                    nomDuPremierJoueur(), couleursJoueurs.get(0).getValue(), modeChoisi());
            PetitsChevauxApp.afficherSalon(getStage(), session);
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'héberger une partie (le port "
                    + ServeurJeu.PORT_PAR_DEFAUT + " est peut-être déjà utilisé)", e);
        }
    }

    /**
     * Rejoint une partie en ligne : demande l'adresse de l'hôte (« 192.168.1.20 »,
     * ou « 192.168.1.20:5555 » pour un autre port), puis affiche le salon.
     */
    @FXML
    private void onRejoindre() {
        TextInputDialog demande = new TextInputDialog("localhost");
        demande.setTitle("Rejoindre une partie en ligne");
        demande.setHeaderText("Adresse de l'ordinateur qui héberge la partie\n"
                + "(affichée dans son salon, par exemple 192.168.1.20)");
        demande.setContentText("Adresse :");
        Optional<String> saisie = demande.showAndWait().map(String::strip).filter(texte -> !texte.isEmpty());
        if (saisie.isEmpty()) {
            return;
        }
        String adresse = saisie.get();
        int port = ServeurJeu.PORT_PAR_DEFAUT;
        int deuxPoints = adresse.lastIndexOf(':');
        if (deuxPoints > 0) {
            try {
                port = Integer.parseInt(adresse.substring(deuxPoints + 1));
                adresse = adresse.substring(0, deuxPoints);
            } catch (NumberFormatException e) {
                PetitsChevauxApp.afficherErreur("Adresse invalide", e);
                return;
            }
        }
        try {
            SessionEnLigne session = SessionEnLigne.rejoindre(Platform::runLater, adresse, port,
                    nomDuPremierJoueur(), couleursJoueurs.get(0).getValue());
            PetitsChevauxApp.afficherSalon(getStage(), session);
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible de rejoindre la partie sur " + adresse + ":" + port, e);
        }
    }

    @FXML
    private void onScores() {
        try {
            PetitsChevauxApp.afficherScores(getStage());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'afficher les scores", e);
        }
    }

    private String nomDuPremierJoueur() {
        String nom = nomsJoueurs.get(0).getText().strip();
        return nom.isEmpty() ? "Joueur 1" : nom;
    }

    @FXML
    private void onQuitter() {
        Platform.exit();
    }

    private Stage getStage() {
        return (Stage) champsJoueurs.getScene().getWindow();
    }
}
