package fr.utbm.petitschevaux;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class PetitsChevauxApp extends Application {
    private static final double LARGEUR = 860;
    private static final double HAUTEUR = 680;

    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Petits chevaux");
        afficherMenu(stage);
        stage.show();
    }

    public static void afficherMenu(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PetitsChevauxApp.class.getResource("menu-view.fxml"));
        afficher(stage, fxmlLoader.load());
    }

    public static void afficherPartie(Stage stage, Jeu jeu) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PetitsChevauxApp.class.getResource("partie-view.fxml"));
        Parent racine = fxmlLoader.load();
        PartieController controller = fxmlLoader.getController();
        controller.setJeu(jeu);
        afficher(stage, racine);
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

    private static void afficher(Stage stage, Parent racine) {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(racine, LARGEUR, HAUTEUR));
        } else {
            stage.getScene().setRoot(racine);
        }
    }
}
