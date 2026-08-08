# Avis relatifs aux composants tiers

Ce document recense les composants tiers connus dans la version actuelle du projet. Il doit être mis à jour avant chaque ajout, remplacement ou mise à niveau d'une dépendance, d'un extrait de code ou d'une ressource externe.

Il ne remplace pas les textes de licence applicables. En cas de différence, la licence du composant tiers prévaut.

## Inventaire actuel

| Composant | Version déclarée | Titulaire ou projet | Licence | Source |
| --- | --- | --- | --- | --- |
| Android Gradle Plugin | 9.0.1 | Google LLC et contributeurs AOSP | Apache-2.0 | [Android tools/base](https://android.googlesource.com/platform/tools/base/) |
| Kotlin et plugin Kotlin Compose | 2.0.21 | JetBrains et contributeurs Kotlin | Apache-2.0 | [Kotlin](https://github.com/JetBrains/kotlin) |
| Gradle Wrapper et Gradle Build Tool | 9.1.0 | Gradle Inc. et contributeurs Gradle | Apache-2.0 | [Gradle](https://github.com/gradle/gradle) |
| AndroidX Core KTX | 1.19.0 | The Android Open Source Project | Apache-2.0 | [AndroidX](https://android.googlesource.com/platform/frameworks/support/) |
| AndroidX Lifecycle Runtime KTX | 2.10.0 | The Android Open Source Project | Apache-2.0 | [AndroidX](https://android.googlesource.com/platform/frameworks/support/) |
| AndroidX Activity Compose | 1.13.0 | The Android Open Source Project | Apache-2.0 | [AndroidX](https://android.googlesource.com/platform/frameworks/support/) |
| Jetpack Compose UI et Material 3 | BOM 2024.09.00 | The Android Open Source Project | Apache-2.0 | [AndroidX](https://android.googlesource.com/platform/frameworks/support/) |
| AndroidX Test et Espresso | 1.3.0 / 3.7.0 | The Android Open Source Project | Apache-2.0 | [AndroidX Test](https://android.googlesource.com/platform/frameworks/testing/) |
| JUnit 4 | 4.13.2 | JUnit Team | EPL-1.0 | [JUnit 4](https://github.com/junit-team/junit4/tree/r4.13.2) |
| Modèle de projet, exemples de test et ressources de lancement générés par Android Studio | Version de génération non enregistrée | Google LLC et contributeurs AOSP | Apache-2.0, sauf mention contraire dans la source | [Android tools/base](https://android.googlesource.com/platform/tools/base/) |

Les numéros de version déclarés proviennent de `gradle/libs.versions.toml` et de `gradle/wrapper/gradle-wrapper.properties`.

## Obligations de maintenance

- Vérifier la licence et sa compatibilité avant toute intégration.
- Ajouter le nom, la version, la source, le titulaire, la licence et les modifications éventuelles à ce fichier.
- Conserver les avis de copyright, fichiers `NOTICE` et textes de licence exigés par les titulaires tiers.
- Ne jamais intégrer un code, une image, une police, une icône, un modèle, un jeu de données ou un autre contenu dont l'origine ou les droits sont inconnus.
- Fournir les avis requis dans l'application et dans toute distribution binaire avant une publication destinée aux utilisatrices.

Les licences actuellement référencées sont disponibles dans :

- [Apache License 2.0](LICENSES/Apache-2.0.txt)
- [Eclipse Public License 1.0](LICENSES/EPL-1.0.txt)
- [Notice JUnit 4.13.2](LICENSES/JUnit-4.13.2-NOTICE.txt)
