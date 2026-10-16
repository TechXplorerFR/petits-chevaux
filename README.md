# Petits chevaux

![GitHub stars](https://img.shields.io/github/stars/TechXplorerFR/petits-chevaux?style=social) ![GitHub watchers](https://img.shields.io/github/watchers/TechXplorerFR/petits-chevaux?style=social) ![GitHub repo size](https://img.shields.io/github/repo-size/TechXplorerFR/petits-chevaux) ![GitHub language count](https://img.shields.io/github/languages/count/TechXplorerFR/petits-chevaux) ![GitHub top language](https://img.shields.io/github/languages/top/TechXplorerFR/petits-chevaux) ![GitHub last commit](https://img.shields.io/github/last-commit/TechXplorerFR/petits-chevaux?color=red)

Jeu de petits chevaux en JavaFX, réalisé dans le cadre du projet PR70 (UTBM).

De 1 à 4 joueurs humains, chacun choisit sa couleur ; les couleurs restantes sont jouées
par des bots. Les parties sont sauvegardées sans boîte de dialogue dans le dossier
`sauvegardes/` (créé là où le jeu est lancé, fichiers `.pcx`) et rechargées depuis le menu.

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

## Mode « cases spéciales »

À choisir dans le menu. 12 cases spéciales sont placées au hasard sur la piste (jamais sur une
case de départ ni au pied d'un escalier) : chaque effet une fois, plus 4 en double. L'effet se
déclenche quand un cheval **s'arrête** sur la case.

| Catégorie | Case | Effet |
|---|---|---|
| Bonus | Rejouer (↻) | Le joueur rejoue immédiatement. |
| Bonus | Turbo (+3) | Le cheval avance de 3 cases de plus. |
| Malus | Croc-en-jambe (-4) | Le cheval adverse le plus avancé sur la piste recule de 4 cases. |
| Malus | Dé maudit (-2) | Le prochain lancer de chaque adversaire perd 2 points (minimum 1). |
| Malus | Gel (❄) | Le joueur suivant passe son prochain tour. |
| Neutre | Coup du sort (?) | Un cheval au hasard sur la piste, peut-être celui du joueur, retourne à l'écurie. |
| Neutre | Tremblement de terre (↓) | Tous les chevaux sur la piste reculent de 2 cases. |
| Neutre | Vent arrière (↑) | Tous les chevaux sur la piste avancent de 2 cases. |

Les déplacements forcés restent sur la piste (jamais dans l'escalier) et capturent un
adversaire présent sur la case d'arrivée. Un cheval déplacé par un effet qui s'arrête sur une
autre case spéciale la déclenche à son tour, au nom de son propriétaire (réaction en chaîne).
Dans une même chaîne, chaque case ne se déclenche qu'une fois, pour éviter les boucles ;
« Rejouer » n'a d'effet que pour le joueur dont c'est le tour. Les bots visent les cases
bonus et malus et évitent les neutres.

## Organisation du code

Tout est dans le paquet `fr.utbm.petitschevaux` (`PetitsChevaux/src/main/java`).

| Rôle | Classes |
|---|---|
| Modèle (sans JavaFX) | `Jeu` (tours, bots, victoire), `Plateau` (règles de déplacement, cases spéciales), `Joueur`, `Cheval`, `De`, `Couleur`, `Statistiques`, `TiragePremierJoueur`, `ModeJeu`, `CaseSpeciale`, `EffetsSpeciaux` |
| Sauvegarde | `Sauvegarde` |
| Vues | `PlateauView`, `Quartier` (escaliers), `EcurieView`, `ChevalView`, `DeView`, `Rendu` (couleurs et dimensions communes) |
| Contrôleurs | `Menu` (`menu-view.fxml`), `TirageController` (`tirage-view.fxml`), `PartieController` (`partie-view.fxml`), `VictoireController` (`victoire-view.fxml`) |
| Lancement | `Launcher`, `PetitsChevauxApp` |

La position d'un cheval est relative à son joueur : 0 = écurie, 1 à 56 = piste
(1 = case de départ, 56 = pied de l'escalier), 57 à 62 = marches, 63 = centre.
`Plateau.positionAbsolue` la convertit en case réelle de la piste.
