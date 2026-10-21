package fr.utbm.petitschevaux;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Point d'entrée JavaFX et navigation entre les écrans :
 * menu → tirage du premier joueur → partie → victoire.
 */
public class PetitsChevauxApp extends Application {
    private static final double LARGEUR = 1100;
    private static final double HAUTEUR = 680;

    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Petits chevaux");
        URL icone = PetitsChevauxApp.class.getResource("petits-chevaux.png");
        if (icone == null) {
            throw new IOException("Icône de l'application introuvable : petits-chevaux.png");
        }
        stage.getIcons().add(new Image(icone.toExternalForm()));
        afficherMenu(stage);
        stage.show();
    }

    public static void afficherMenu(Stage stage) throws IOException {
        afficher(stage, charger("menu-view.fxml").load());
    }

    /**
     * Affiche le tirage au dé qui désigne le premier joueur d'une nouvelle partie.
     */
    public static void afficherTirage(Stage stage, Jeu jeu) throws IOException {
        FXMLLoader loader = charger("tirage-view.fxml");
        Parent racine = loader.load();
        loader.<TirageController>getController().setJeu(jeu);
        afficher(stage, racine);
    }

    /**
     * Affiche une nouvelle partie (pas encore sauvegardée).
     */
    public static void afficherPartie(Stage stage, Jeu jeu) throws IOException {
        afficherPartie(stage, jeu, null);
    }

    /**
     * Affiche une partie chargée : les prochaines sauvegardes mettront à jour son fichier.
     */
    public static void afficherPartie(Stage stage, Jeu jeu, Path fichierSauvegarde) throws IOException {
        afficherPartie(stage, jeu, fichierSauvegarde, null);
    }

    /**
     * Affiche une partie, éventuellement en ligne.
     *
     * @param session partie en ligne (hôte ou invité), ou {@code null} pour une partie locale
     */
    public static void afficherPartie(Stage stage, Jeu jeu, Path fichierSauvegarde, SessionEnLigne session)
            throws IOException {
        FXMLLoader loader = charger("partie-view.fxml");
        Parent racine = loader.load();
        loader.<PartieController>getController().setJeu(jeu, fichierSauvegarde, session);
        afficher(stage, racine);
    }

    /**
     * Affiche l'écran de fin de partie avec le classement et les statistiques.
     *
     * @param session partie en ligne, ou {@code null} pour une partie locale
     */
    public static void afficherVictoire(Stage stage, Jeu jeu, SessionEnLigne session) throws IOException {
        FXMLLoader loader = charger("victoire-view.fxml");
        Parent racine = loader.load();
        loader.<VictoireController>getController().setJeu(jeu, session);
        afficher(stage, racine);
    }

    /**
     * Affiche le salon d'une partie en ligne (hôte ou invité).
     */
    public static void afficherSalon(Stage stage, SessionEnLigne session) throws IOException {
        FXMLLoader loader = charger("salon-view.fxml");
        Parent racine = loader.load();
        loader.<SalonController>getController().setSession(session);
        afficher(stage, racine);
    }

    /**
     * Affiche le classement des joueurs et l'historique des parties.
     */
    public static void afficherScores(Stage stage) throws IOException {
        afficher(stage, charger("scores-view.fxml").load());
    }

    /**
     * Affiche une erreur à l'utilisateur.
     *
     * @param message ce qui n'a pas pu être fait
     * @param cause exception à l'origine de l'erreur
     */
    public static void afficherErreur(String message, Exception cause) {
        new Alert(Alert.AlertType.ERROR, message + " :\n" + cause.getMessage()).showAndWait();
    }

    private static FXMLLoader charger(String vue) {
        return new FXMLLoader(PetitsChevauxApp.class.getResource(vue));
    }

    private static void afficher(Stage stage, Parent racine) {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(racine, LARGEUR, HAUTEUR));
        } else {
            stage.getScene().setRoot(racine);
        }
    }
}
