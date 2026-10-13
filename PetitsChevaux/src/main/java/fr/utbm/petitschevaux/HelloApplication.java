package fr.utbm.petitschevaux;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    private static final double LARGEUR = 860;
    private static final double HAUTEUR = 680;

    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Petits chevaux");
        afficherMenu(stage);
        stage.show();
    }

    public static void afficherMenu(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("menu-view.fxml"));
        afficher(stage, fxmlLoader.load());
    }

    public static void afficherPartie(Stage stage, Jeu jeu) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Parent racine = fxmlLoader.load();
        PartieController controller = fxmlLoader.getController();
        controller.setJeu(jeu);
        afficher(stage, racine);
    }

    private static void afficher(Stage stage, Parent racine) {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(racine, LARGEUR, HAUTEUR));
        } else {
            stage.getScene().setRoot(racine);
        }
    }
}
