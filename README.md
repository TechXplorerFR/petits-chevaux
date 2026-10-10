# 🐴 Petits Chevaux 🎲

![GitHub stars](https://img.shields.io/github/stars/TechXplorerFR/petits-chevaux?style=social) ![GitHub watchers](https://img.shields.io/github/watchers/TechXplorerFR/petits-chevaux?style=social) ![GitHub repo size](https://img.shields.io/github/repo-size/TechXplorerFR/petits-chevaux) ![GitHub language count](https://img.shields.io/github/languages/count/TechXplorerFR/petits-chevaux) ![GitHub top language](https://img.shields.io/github/languages/top/TechXplorerFR/petits-chevaux) ![GitHub last commit](https://img.shields.io/github/last-commit/TechXplorerFR/petits-chevaux?color=red)

Les Petits Chevaux est une **version numérique du célèbre jeu de société** dans laquelle les joueurs doivent faire avancer leurs chevaux autour du plateau pour les ramener jusqu'à leur écurie d'arrivée. 🏇🎲

Développé en **Java avec JavaFX et Gradle**, ce projet vise à proposer une expérience de jeu conviviale et intuitive, tout en respectant les règles traditionnelles des Petits Chevaux.

Il s'inscrit dans le cadre du projet de l'UE PR70 (Programmation Orientée Objet) à l'UTBM.

## 🔑 Fonctionnalités principales

- 🎲 **Lancers de dés** pour déterminer les déplacements des chevaux.
- 🐎 **Déplacement des chevaux** sur le plateau de jeu.
- 🎨 **Interface graphique JavaFX** pour une expérience visuelle interactive.
- 👥 **Gestion des joueurs** et des tours de jeu.
- 🏁 **Gestion de la victoire** pour identifier le joueur ayant terminé le parcours avec tous ses chevaux.

<!-- TODO: Ajout image -->
![Aperçu du jeu Petits Chevaux](demo.png)

## 🚀 Installation

### Prérequis 🛠️

Assurez-vous d'avoir installé les outils suivants :

- **Java JDK** : version compatible avec celle configurée dans le projet.
- **Gradle** : facultatif si le projet contient le Gradle Wrapper (`gradlew`).
- **Git** : pour cloner le dépôt.

> [!TIP]
> Nous recommandons d'utiliser le Gradle Wrapper fourni avec le projet (`gradlew` ou `gradlew.bat`) afin d'utiliser automatiquement la version de Gradle prévue pour le projet.

### Installation depuis GitHub 🏗️

1. **Cloner le dépôt :**

   ```bash
   git clone https://github.com/OWNER/REPOSITORY.git
   cd REPOSITORY
   ```

2. **Compiler le projet :**

   Sous Linux/macOS :

   ```bash
   ./gradlew build
   ```

   Sous Windows (PowerShell) :

   ```powershell
   .\gradlew.bat build
   ```

3. **Lancer le jeu :**

   Sous Linux/macOS :

   ```bash
   ./gradlew run
   ```

   Sous Windows (PowerShell) :

   ```powershell
   .\gradlew.bat run
   ```

> [!NOTE]
> Ces commandes supposent que le projet configure la tâche Gradle `run` et les dépendances JavaFX nécessaires. Si votre projet utilise une configuration différente, adaptez les commandes en conséquence.

> [!CAUTION]
> La branche de développement peut contenir des fonctionnalités incomplètes ou instables. Utilisez-la avec précaution.

## 🎮 Comment jouer ?

Le but du jeu est d'être le premier joueur à ramener tous ses chevaux jusqu'à la zone d'arrivée.

1. **Choisissez votre couleur** parmi celles disponibles.
2. **Lancez le dé** pour déterminer le nombre de cases à parcourir.
3. **Déplacez vos chevaux** en respectant les règles du jeu.
4. **Anticipez les déplacements de vos adversaires** pour optimiser votre progression.
5. **Ramenez tous vos chevaux à l'arrivée** pour remporter la partie !

> [!TIP]
> Planifiez vos déplacements et choisissez judicieusement le cheval à faire avancer pour augmenter vos chances de victoire. 🧠

## 📚 Documentation

## 📜 Licence

Ce projet est distribué sous la licence [MIT](./LICENSE.md), si celle-ci est bien celle retenue pour le projet.

## 💬 Support

Vous avez rencontré un problème ou souhaitez proposer une amélioration ?

N'hésitez pas à ouvrir une issue dans la [section Issues](https://github.com/OWNER/REPOSITORY/issues) du dépôt.

Nous accueillons avec plaisir vos retours et suggestions afin d'améliorer le jeu. 💡

## 👥 Contribution

Vous souhaitez contribuer au projet ?

1. Forkez le dépôt.
2. Créez une branche dédiée à votre fonctionnalité.
3. Effectuez vos modifications.
4. Testez votre code.
5. Soumettez une pull request en décrivant vos changements.

## ✍️ Auteurs et remerciements

### Notre équipe 🚀

| Nom | Rôle | Liens |
| :--- | :--- | :--- |
| **Antoine PREVOST** | Développeur | [GitHub](https://github.com/TechXplorerFR) |
| **Alban GRATIA** | Développeur | [GitHub](https://github.com/Albtros26) |
| **Colin AUBERT** | Développeur | [GitHub](https://github.com/Cocolin67) |

---

**Amusez-vous bien avec les Petits Chevaux !** 🐴🎲🎉
