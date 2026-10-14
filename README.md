# Petits chevaux

![GitHub stars](https://img.shields.io/github/stars/TechXplorerFR/petits-chevaux?style=social) ![GitHub watchers](https://img.shields.io/github/watchers/TechXplorerFR/petits-chevaux?style=social) ![GitHub repo size](https://img.shields.io/github/repo-size/TechXplorerFR/petits-chevaux) ![GitHub language count](https://img.shields.io/github/languages/count/TechXplorerFR/petits-chevaux) ![GitHub top language](https://img.shields.io/github/languages/top/TechXplorerFR/petits-chevaux) ![GitHub last commit](https://img.shields.io/github/last-commit/TechXplorerFR/petits-chevaux?color=red)

Jeu de petits chevaux en JavaFX, réalisé dans le cadre du projet PR70 (UTBM).

De 1 à 4 joueurs humains ; les couleurs restantes sont jouées par des bots.
Les parties peuvent être sauvegardées et rechargées (fichiers `.pcx`).

## Lancer le jeu

Prérequis : un JDK 17 ou plus récent (`JAVA_HOME` doit pointer dessus).

```bash
cd PetitsChevaux
./mvnw javafx:run
```

Depuis IntelliJ : lancer la classe `fr.utbm.petitschevaux.Launcher`.

## Lancer les tests

```bash
cd PetitsChevaux
./mvnw test
```

## Règles implémentées

- Le premier joueur est tiré au dé (les ex æquo relancent), puis on joue dans le sens horaire :
  jaune, rouge, bleu, vert.
- Un **6** est nécessaire pour sortir un cheval de l'écurie, et un 6 fait rejouer.
- Un cheval fait le tour de la piste (56 cases) jusqu'au pied de son escalier.
  Il doit y arriver au compte exact : un dé trop fort le fait reculer d'autant.
- On ne peut **pas dépasser** un cheval, ni s'arrêter sur un de ses propres chevaux.
  S'arrêter sur un cheval adverse le **renvoie à son écurie**.
- L'escalier se monte marche par marche : il faut un 1 pour la première marche, un 2 pour la
  deuxième… jusqu'au 6, puis un dernier 6 pour entrer au centre.
- Le premier joueur qui ramène ses quatre chevaux au centre gagne.

Les bots capturent dès qu'ils le peuvent, sinon sortent un cheval, sinon avancent leur cheval
le plus proche de l'arrivée.

## Organisation du code

Tout est dans le paquet `fr.utbm.petitschevaux` (`PetitsChevaux/src/main/java`).

| Rôle | Classes |
|---|---|
| Modèle (sans JavaFX) | `Jeu` (tours, bots, victoire), `Plateau` (règles de déplacement), `Joueur`, `Cheval`, `De`, `Couleur` |
| Sauvegarde | `Sauvegarde` |
| Vues | `PlateauView`, `Quartier` (escaliers), `EcurieView`, `ChevalView`, `DeView`, `Rendu` (couleurs et dimensions communes) |
| Contrôleurs | `Menu` (`menu-view.fxml`), `PartieController` (`partie-view.fxml`) |
| Lancement | `Launcher`, `PetitsChevauxApp` |

La position d'un cheval est relative à son joueur : 0 = écurie, 1 à 56 = piste
(1 = case de départ, 56 = pied de l'escalier), 57 à 62 = marches, 63 = centre.
`Plateau.positionAbsolue` la convertit en case réelle de la piste.
