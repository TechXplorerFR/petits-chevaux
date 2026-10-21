package fr.utbm.petitschevaux;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Enregistre les parties terminées et fournit le classement des joueurs.
 * Seuls les joueurs humains ont un profil : les bots changent de nom à chaque partie.
 */
public class ScoresDAO {
    private final BaseDeDonnees base;

    public ScoresDAO(BaseDeDonnees base) {
        this.base = base;
    }

    /**
     * Enregistre le résultat d'une partie terminée : une ligne d'historique, et pour chaque
     * humain une partie de plus (et une victoire s'il a gagné). Une même partie n'est
     * comptée qu'une fois, même si elle est rechargée depuis une sauvegarde.
     *
     * @param jeu partie terminée
     * @return vrai si le résultat vient d'être enregistré, faux s'il l'était déjà
     *         ou si la partie n'est pas finie
     */
    public boolean enregistrerPartie(Jeu jeu) throws SQLException {
        if (!jeu.estTerminee() || jeu.isResultatEnregistre()) {
            return false;
        }
        try (Connection connexion = base.ouvrir()) {
            connexion.setAutoCommit(false);
            try {
                insererPartie(connexion, jeu);
                for (Joueur joueur : jeu.getJoueurs()) {
                    if (!joueur.isBot()) {
                        compterPartie(connexion, joueur.getNom(), joueur == jeu.getVainqueur());
                    }
                }
                connexion.commit();
            } catch (SQLException e) {
                connexion.rollback();
                throw e;
            }
        }
        jeu.marquerResultatEnregistre();
        return true;
    }

    /**
     * Enregistre une partie terminée dans la base de données.
     * @param connexion la connexion à la base de données
     * @param jeu la partie terminée
     * @throws SQLException si une erreur survient lors de l'accès à la base de données
     */
    private void insererPartie(Connection connexion, Jeu jeu) throws SQLException {
        String sql = "INSERT INTO partie (date_fin, mode, vainqueur, vainqueur_bot, duree_ms, lancers)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            requete.setString(2, jeu.getMode().name());
            requete.setString(3, jeu.getVainqueur().getNom());
            requete.setBoolean(4, jeu.getVainqueur().isBot());
            requete.setLong(5, jeu.getDuree().toMillis());
            requete.setInt(6, jeu.getNombreLancers());
            requete.executeUpdate();
        }
    }

    /**
     * Incrémente le nombre de parties jouées et de victoires pour un joueur donné.
     * Si le joueur n'existe pas encore dans la base de données, il est créé
     * @param connexion la connexion à la base de données
     * @param nom le nom du joueur
     * @param victoire vrai si le joueur a gagné la partie, faux sinon
     * @throws SQLException si une erreur survient lors de l'accès à la base de données
     */
    private void compterPartie(Connection connexion, String nom, boolean victoire) throws SQLException {
        int victoires = victoire ? 1 : 0;
        String miseAJour = "UPDATE profil SET parties = parties + 1, victoires = victoires + ? WHERE nom = ?";
        try (PreparedStatement requete = connexion.prepareStatement(miseAJour)) {
            requete.setInt(1, victoires);
            requete.setString(2, nom);
            if (requete.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement requete = connexion.prepareStatement(
                "INSERT INTO profil (nom, parties, victoires) VALUES (?, 1, ?)")) {
            requete.setString(1, nom);
            requete.setInt(2, victoires);
            requete.executeUpdate();
        }
    }

    /**
     * Retourne la liste des meilleurs joueurs selon leur nombre de victoires et leur ratio de victoires.
     * @param limite nombre maximum de profils
     * @return les meilleurs joueurs : plus de victoires d'abord, puis meilleur ratio, puis nom
     */
    public List<Profil> meilleursJoueurs(int limite) throws SQLException {
        String sql = "SELECT nom, parties, victoires FROM profil"
                + " ORDER BY victoires DESC, CAST(victoires AS DOUBLE) / parties DESC, nom LIMIT ?";
        List<Profil> profils = new ArrayList<>();
        try (Connection connexion = base.ouvrir();
                PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, limite);
            try (ResultSet ligne = requete.executeQuery()) {
                while (ligne.next()) {
                    profils.add(new Profil(ligne.getString("nom"), ligne.getInt("parties"),
                            ligne.getInt("victoires")));
                }
            }
        }
        return profils;
    }

    /**
     * Retourne le profil d'un joueur donné, ou un profil vide s'il n'a encore terminé aucune partie.
     * @param nom nom du joueur
     * @return son profil, ou un profil vide s'il n'a encore terminé aucune partie
     */
    public Profil profil(String nom) throws SQLException {
        try (Connection connexion = base.ouvrir();
                PreparedStatement requete = connexion.prepareStatement(
                        "SELECT parties, victoires FROM profil WHERE nom = ?")) {
            requete.setString(1, nom);
            try (ResultSet ligne = requete.executeQuery()) {
                return ligne.next()
                        ? new Profil(nom, ligne.getInt("parties"), ligne.getInt("victoires"))
                        : new Profil(nom, 0, 0);
            }
        }
    }

    /**
     * Retourne la liste des dernières parties terminées, de la plus récente à la plus ancienne.
     * @param limite nombre maximum de parties
     * @return les dernières parties terminées, de la plus récente à la plus ancienne
     */
    public List<ResumePartie> dernieresParties(int limite) throws SQLException {
        String sql = "SELECT date_fin, mode, vainqueur, vainqueur_bot, duree_ms, lancers FROM partie"
                + " ORDER BY date_fin DESC, id DESC LIMIT ?";
        List<ResumePartie> parties = new ArrayList<>();
        try (Connection connexion = base.ouvrir();
                PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, limite);
            try (ResultSet ligne = requete.executeQuery()) {
                while (ligne.next()) {
                    parties.add(new ResumePartie(
                            ligne.getTimestamp("date_fin").toLocalDateTime(),
                            ModeJeu.valueOf(ligne.getString("mode")),
                            ligne.getString("vainqueur"),
                            ligne.getBoolean("vainqueur_bot"),
                            Duration.ofMillis(ligne.getLong("duree_ms")),
                            ligne.getInt("lancers")));
                }
            }
        }
        return parties;
    }
}
