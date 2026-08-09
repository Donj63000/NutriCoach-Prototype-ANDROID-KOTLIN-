# Avis relatifs aux composants tiers

Ce document recense les composants tiers directement déclarés par le projet.
Il ne remplace pas les textes de licence applicables. En cas de différence,
la licence du composant tiers prévaut.

## Inventaire actuel

| Composant | Version déclarée | Projet | Licence |
| --- | ---: | --- | --- |
| Android Gradle Plugin | 9.2.1 | Google / Android Open Source Project | Apache-2.0 |
| Kotlin et plugin Compose | 2.3.10 | JetBrains / Kotlin | Apache-2.0 |
| Kotlin Symbol Processing | 2.3.10 | Google / KSP | Apache-2.0 |
| Gradle Wrapper | 9.4.1 | Gradle | Apache-2.0 |
| AndroidX Core KTX | 1.17.0 | AndroidX | Apache-2.0 |
| AndroidX Lifecycle | 2.10.0 | AndroidX | Apache-2.0 |
| AndroidX Activity Compose | 1.13.0 | AndroidX | Apache-2.0 |
| Jetpack Compose UI et Material 3 | BOM 2026.06.01 | AndroidX | Apache-2.0 |
| AndroidX Room | 2.8.4 | AndroidX | Apache-2.0 |
| AndroidX SQLite | 2.6.2 | AndroidX | Apache-2.0 |
| AndroidX Hilt Compose | 1.3.0 | AndroidX | Apache-2.0 |
| Dagger / Hilt | 2.60.1 | Google / Dagger | Apache-2.0 |
| Kotlin Coroutines | 1.10.2 | JetBrains | Apache-2.0 |
| SQLCipher for Android | 4.17.0 | Zetetic | BSD-3-Clause et avis applicables aux sources dérivées d'AOSP |
| AndroidX Test et Espresso | 1.7.0 / 1.3.0 / 3.7.0 | AndroidX Test | Apache-2.0 |
| JUnit 4 | 4.13.2 | JUnit Team | EPL-1.0 |

Les numéros de version sont définis dans `gradle/libs.versions.toml` et
`gradle/wrapper/gradle-wrapper.properties`.

## Sources principales

- AndroidX : <https://android.googlesource.com/platform/frameworks/support/>
- Android Gradle Plugin : <https://android.googlesource.com/platform/tools/base/>
- Kotlin : <https://github.com/JetBrains/kotlin>
- KSP : <https://github.com/google/ksp>
- Dagger / Hilt : <https://github.com/google/dagger>
- Kotlin Coroutines : <https://github.com/Kotlin/kotlinx.coroutines>
- SQLCipher for Android : <https://github.com/sqlcipher/sqlcipher-android>
- JUnit 4 : <https://github.com/junit-team/junit4/tree/r4.13.2>

## Textes de licence conservés

- [Apache License 2.0](LICENSES/Apache-2.0.txt)
- [BSD 3-Clause — SQLCipher](LICENSES/BSD-3-Clause.txt)
- [Eclipse Public License 1.0](LICENSES/EPL-1.0.txt)
- [Notice JUnit 4.13.2](LICENSES/JUnit-4.13.2-NOTICE.txt)

## Maintenance

Avant chaque ajout ou mise à niveau d'une dépendance :

1. vérifier sa provenance et sa licence ;
2. mettre à jour cet inventaire ;
3. conserver les avis imposés ;
4. vérifier les dépendances transitives dans l'artefact distribué ;
5. ne jamais intégrer une ressource ou une donnée dont les droits sont inconnus.
