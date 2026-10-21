package fr.utbm.petitschevaux;

import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Zone de chat d'une partie en ligne : la liste des messages et un champ de saisie.
 * Utilisée dans le salon et pendant la partie.
 */
public class ChatView {
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final VBox racine = new VBox(6);
    private final ListView<Message> liste = new ListView<>();
    private final TextField saisie = new TextField();

    /**
     * @param envoi action appelée avec le texte saisi (Entrée ou bouton « Envoyer »)
     */
    public ChatView(Consumer<String> envoi) {
        liste.setCellFactory(vue -> new CelluleMessage());
        liste.setPlaceholder(new Text("Aucun message pour l'instant."));
        VBox.setVgrow(liste, Priority.ALWAYS);

        saisie.setPromptText("Votre message…");
        saisie.setOnAction(event -> envoyer(envoi));
        HBox.setHgrow(saisie, Priority.ALWAYS);
        Button bouton = new Button("Envoyer");
        bouton.setOnAction(event -> envoyer(envoi));

        racine.getChildren().addAll(liste, new HBox(6, saisie, bouton));
    }

    public VBox getRoot() {
        return racine;
    }

    /**
     * Remplace la liste par ces messages (en arrivant sur un nouvel écran).
     */
    public void afficher(List<Message> messages) {
        liste.getItems().setAll(messages);
        liste.scrollTo(liste.getItems().size() - 1);
    }

    /**
     * Ajoute un message en bas de la liste.
     */
    public void ajouter(Message message) {
        liste.getItems().add(message);
        liste.scrollTo(liste.getItems().size() - 1);
    }

    private void envoyer(Consumer<String> envoi) {
        String texte = saisie.getText().strip();
        if (!texte.isEmpty()) {
            envoi.accept(texte);
            saisie.clear();
        }
    }

    /** Affiche « [xx:xx] NAME : MESSAGE », avec le nom dans la couleur du joueur. */
    private static final class CelluleMessage extends ListCell<Message> {
        @Override
        protected void updateItem(Message message, boolean vide) {
            super.updateItem(message, vide);
            if (vide || message == null) {
                setGraphic(null);
                return;
            }
            Text heure = new Text("[" + message.heure().format(FORMAT_HEURE) + "] ");
            heure.setFill(Color.GRAY);
            Text auteur = new Text(message.auteur() + " : ");
            auteur.setStyle("-fx-font-weight: bold;");
            auteur.setFill(message.estSysteme() ? Rendu.TRAIT : Rendu.couleur(message.couleur()));
            Text texte = new Text(message.texte());
            if (message.estSysteme()) {
                texte.setStyle("-fx-font-style: italic;");
            }
            TextFlow ligne = new TextFlow(heure, auteur, texte);
            // Retour à la ligne dans la largeur de la liste.
            ligne.maxWidthProperty().bind(getListView().widthProperty().subtract(30));
            setGraphic(ligne);
        }
    }
}
