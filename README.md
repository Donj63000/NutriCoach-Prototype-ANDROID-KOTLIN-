# NutriGain

NutriGain est une application Android privée conçue pour accompagner une
personne adulte dans une prise de poids progressive, avec des estimations
locales, des garde-fous de prudence et une gestion explicite des données
sensibles.

## Ce que fait la version actuelle

Le dépôt contient une tranche verticale Android fonctionnelle :

- application Jetpack Compose pilotée par des `ViewModel` ;
- logique métier déterministe pour l'IMC, l'estimation énergétique et le rythme
  de prise de poids ;
- onboarding en quatre étapes : profil, mode de vie, questionnaire de prudence,
  puis information sur les données et limites ;
- questionnaire médical à réponses explicites `Oui / Non / Incertain`, sans
  présélection silencieuse ;
- suspension automatique du repère calorique lorsqu'une situation demande une
  évaluation professionnelle ;
- révision périodique du questionnaire et recalcul transactionnel du plan ;
- base Room chiffrée avec SQLCipher et clé enveloppée par Android Keystore ;
- migration Room `1 → 2` non destructive ;
- tableau de bord avec provenance du calcul, échéance de révision, statut de
  prudence et suppression transactionnelle des données ;
- politique d'accès pour une future IA créée **désactivée par défaut** ;
- constructeur de contexte IA local qui applique consentement, périmètres,
  minimisation et refus de sécurité avant toute future transmission ;
- tests unitaires du domaine, tests du DAO et tests instrumentés de migration.

Il n'existe encore ni serveur distant, ni API métier, ni connecteur IA. Dans
ce projet, le « back-end » actuel correspond aux couches domaine, repository,
Room et chiffrement exécutées localement sur l'appareil.

## Architecture

Le flux principal reste unidirectionnel :

```text
Compose → ViewModel → Use case → Repository → Room / SQLCipher
                                      ↓
                                Flow vers l'UI
```

Room est la source de vérité locale. Les calculs nutritionnels sont
déterministes. Une future IA pourra expliquer des informations ou proposer des
idées dans le périmètre consenti, mais ne devra jamais modifier seule la cible
calorique enregistrée.

La frontière prévue pour l'IA est la suivante :

```text
UserSetup local
   ↓
BuildAiNutritionContextUseCase
   ├─ politique activée et consentement versionné valides
   ├─ questionnaire à jour
   ├─ niveau de prudence compatible
   ├─ catégories de données explicitement autorisées
   └─ exclusion de la date de naissance exacte, des identifiants internes
      et de la note médicale libre
   ↓
AiNutritionContext minimisé
```

## Lancer et vérifier le projet

Pré-requis :

- Android Studio compatible avec AGP 9.2 ;
- JDK 17 ou plus récent ;
- SDK Android 36 installé ;
- émulateur ou appareil API 26+ pour les tests instrumentés.

Commandes principales :

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Le premier lancement crée une phrase secrète SQLCipher aléatoire. Elle est
chiffrée avec une clé non exportable de l'Android Keystore et enregistrée dans
`noBackupFilesDir`. La sauvegarde automatique Android est désactivée afin
d'éviter la restauration d'une base sans sa clé correspondante.

Les schémas Room exportés dans `app/schemas/` font partie du code source et
doivent être conservés dans le contrôle de version avec chaque migration.

## Limites importantes

NutriGain ne pose aucun diagnostic, ne prescrit aucun traitement et ne
remplace pas un médecin ou un diététicien. Le questionnaire et les seuils
actuels sont des garde-fous techniques : ils exigent une validation clinique,
juridique et de protection des données avant diffusion publique.

Les repas, recettes, saisies de poids successives, graphiques de tendance,
rappels, export de données et rapports hebdomadaires ne sont pas encore
implémentés. Le connecteur IA distant, son authentification, sa politique de
rétention, son audit et la validation de ses sorties restent également à
concevoir.

Le diagnostic détaillé, les décisions d'architecture et l'ordre recommandé des
prochaines étapes sont décrits dans
[`docs/CODE_REVIEW_V2.md`](docs/CODE_REVIEW_V2.md).

## Licence

Copyright © 2026 Valentin GIDON.

Le code original est distribué sous
[GNU Affero General Public License v3.0 uniquement](LICENSE).

Consulter également :

- [Droits d'auteur](COPYRIGHT.md)
- [Composants tiers](THIRD_PARTY_NOTICES.md)
- [Politique de sécurité](SECURITY.md)
- [Règles de contribution](CONTRIBUTING.md)
