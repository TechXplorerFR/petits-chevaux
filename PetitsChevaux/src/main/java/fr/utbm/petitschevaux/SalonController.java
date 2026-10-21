package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur du salon d'une partie en ligne : les joueurs attendent que l'hôte lance la partie.
 */
public class SalonController {
    @FXML private Label adresse;
    @FXML private Label regles;
    @FXML private VBox places;
    @FXML private StackPane zoneChat;
    @FXML private Label message;
    @FXML private Button boutonLancer;

    private SessionEnLigne session;
    private ChatView chat;

    public void setSession(SessionEnLigne session) {
        this.session = session;
        chat = new ChatView(session::envoyerChat);
        chat.afficher(session.getMessages());
        zoneChat.getChildren().setAll(chat.getRoot());

        if (session.estHote()) {
            adresse.setText("Les autres joueurs choisissent « Rejoindre une partie en ligne » et saisissent : "
                    + String.join(" ou ", SessionEnLigne.adressesLocales())
                    + " (port " + session.getPort() + ")");
            regles.setText("Règles : " + session.getMode().getLibelle());
            message.setText("Lancez la partie quand tout le monde est là.");
        } else {
            adresse.setText("Connecté. En attente du lancement de la partie par l'hôte…");
            regles.setText("");
            boutonLancer.setVisible(false);
            boutonLancer.setManaged(false);
        }

        session.surSalon(this::afficherPlaces);
        session.surChat(chat::ajouter);
        session.surDebutPartie(this::rejoindrePartie);
        session.surFin(this::revenirAuMenu);
        afficherPlaces(session.getPlaces());
    }

    private void afficherPlaces(List<Place> liste) {
        places.getChildren().clear();
        for (Place place : liste) {
            String precision = place.hote() ? " (hôte)" : "";
            if (place.couleur() == session.getCouleurLocale()) {
                precision += " — vous";
            }
            Label ligne = new Label(place.nom() + precision, new Circle(7, Rendu.couleur(place.couleur())));
            ligne.setStyle("-fx-font-size: 14px;");
            places.getChildren().add(ligne);
        }
        int bots = Jeu.MAX_JOUEURS - liste.size();
        if (session.estHote()) {
            boutonLancer.setText(bots == 0 ? "Lancer la partie" : "Lancer la partie (+" + bots + " bot"
                    + (bots > 1 ? "s" : "") + ")");
        }
    }

    /**
     * Hôte : crée la partie avec les joueurs du salon, tire au dé le premier joueur
     * (affiché dans le chat) et l'envoie aux invités.
     */
    @FXML
    private void onLancer() {
        if (!session.estHote()) {
            return;
        }
        List<Joueur> humains = session.getPlaces().stream()
                .map(place -> new Joueur(place.nom(), place.couleur()))
                .toList();
        Jeu jeu = new Jeu(humains, session.getMode());
        TiragePremierJoueur tirage = new TiragePremierJoueur(jeu.getJoueurs(), new De());
        while (!tirage.estTermine()) {
            tirage.lancer();
        }
        jeu.definirPremierJoueur(tirage.getGagnant());
        String scores = jeu.getJoueurs().stream()
                .map(joueur -> joueur.getNom() + " " + tirage.getResultat(joueur))
                .collect(Collectors.joining(", "));
        session.messagePartie("Tirage au dé (" + scores + ") : " + tirage.getGagnant().getNom() + " commence.");
        session.demarrerPartie(jeu);
        afficherPartie(jeu);
    }

    /** Invité : l'hôte a lancé la partie. */
    private void rejoindrePartie(String etat) {
        try {
            afficherPartie(Sauvegarde.depuisTexte(etat));
        } catch (IOException e) {
            session.fermer();
            PetitsChevauxApp.afficherErreur("Partie reçue illisible", e);
        }
    }

    private void afficherPartie(Jeu jeu) {
        try {
            PetitsChevauxApp.afficherPartie(getStage(), jeu, null, session);
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir la partie", e);
        }
    }

    @FXML
    private void onQuitter() {
        session.fermer();
        ouvrirMenu();
    }

    /** Session terminée par l'hôte ou le réseau : on prévient puis on revient au menu. */
    private void revenirAuMenu(String raison) {
        Alert alerte = new Alert(Alert.AlertType.INFORMATION, raison);
        alerte.setHeaderText("Partie en ligne terminée");
        alerte.show();
        ouvrirMenu();
    }

    private void ouvrirMenu() {
        try {
            PetitsChevauxApp.afficherMenu(getStage());
        } catch (IOException e) {
            PetitsChevauxApp.afficherErreur("Impossible d'ouvrir le menu", e);
        }
    }

    private Stage getStage() {
        return (Stage) places.getScene().getWindow();
    }
}
