# NutriGain

NutriGain est une application Android privée conçue pour accompagner une
utilisatrice adulte dans une prise de poids progressive, avec un suivi local,
simple et non culpabilisant.

## État actuel

Le dépôt contient maintenant la première tranche verticale fonctionnelle :

- identité Android définitive `com.val.nutrigain` ;
- architecture Compose, ViewModel, use cases, repositories, Hilt et Room ;
- base locale chiffrée avec SQLCipher ;
- protection de la clé de base avec Android Keystore ;
- questionnaire initial en trois étapes ;
- contrôles de cohérence et garde-fous de prudence ;
- calcul local de l'IMC, de la dépense estimée et du repère énergétique ;
- désactivation de la cible automatique lorsque la situation demande un avis
  professionnel ;
- persistance transactionnelle du profil, du plan et du poids initial ;
- écran de synthèse ;
- tests unitaires du moteur métier et du parseur de saisie.

Les repas, recettes, graphiques, rappels et rapports hebdomadaires seront
ajoutés sur cette fondation. Aucun de ces futurs écrans n'est simulé dans la
version actuelle.

## Architecture

L'application suit un flux unidirectionnel :

```text
Compose → ViewModel → Use case → Repository → Room chiffré
                                      ↓
                                Flow vers l'UI
```

Room est la source de vérité locale. Les calculs nutritionnels restent
déterministes ; une IA ne devra jamais calculer ou modifier seule une cible.

## Lancer le projet

Pré-requis :

- Android Studio compatible avec AGP 9.2 ;
- JDK 17 ou plus récent ;
- SDK Android 36 installé.

Commandes principales :

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Le premier lancement crée une clé SQLCipher aléatoire. La clé est enveloppée
avec une clé non exportable de l'Android Keystore et son fichier est placé dans
`noBackupFilesDir`. La sauvegarde automatique Android est désactivée afin
d'éviter une restauration de base sans la clé correspondante.

## Limites importantes

NutriGain ne pose aucun diagnostic, ne prescrit aucun traitement et ne
remplace pas un médecin ou un diététicien. Les valeurs énergétiques affichées
sont des estimations de départ. Une perte de poids involontaire, une grossesse,
un antécédent de trouble alimentaire, certains symptômes, maladies ou
traitements suspendent volontairement le calcul automatique.

## Licence

Copyright © 2026 Valentin GIDON.

Le code original est distribué sous
[GNU Affero General Public License v3.0 uniquement](LICENSE).

Consulter également :

- [Droits d'auteur](COPYRIGHT.md)
- [Composants tiers](THIRD_PARTY_NOTICES.md)
- [Politique de sécurité](SECURITY.md)
- [Règles de contribution](CONTRIBUTING.md)
