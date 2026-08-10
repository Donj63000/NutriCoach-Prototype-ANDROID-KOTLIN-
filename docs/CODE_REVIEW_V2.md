# Revue de code avancée — NutriGain v2

## 1. Objet de l'application

NutriGain est actuellement un prototype Android « local-first » destiné à
établir un plan initial de prise de poids pour une personne adulte. Le parcours
collecte un profil minimal, un niveau d'activité, un rythme souhaité et un
questionnaire de prudence. Le domaine calcule ensuite un IMC indicatif, une
estimation de maintien et, lorsque les règles de sécurité le permettent, un
repère calorique de départ.

Le projet ne contient pas de back-end réseau. La partie assimilable au
back-end est locale :

```text
use cases métier
    ↓
repository
    ↓
DAO Room
    ↓
base SQLite chiffrée par SQLCipher
```

Aucune requête distante ni aucun appel de modèle d'IA n'est présent dans cette
version.

## 2. Niveau d'avancement avant cette revue

La fondation initiale était saine pour un prototype :

- séparation Compose / ViewModel / domaine / repository / Room ;
- injection Hilt ;
- calculs déterministes isolés ;
- écriture initiale transactionnelle ;
- base chiffrée et clé protégée par Android Keystore ;
- sauvegarde Android et trafic en clair désactivés ;
- premiers tests du calcul et du parseur.

Elle présentait néanmoins plusieurs risques avant une montée en production :

1. Les réponses médicales étaient des booléens initialisés à `false`. Une
   absence de réponse pouvait donc être confondue avec un « non » explicite.
2. Le questionnaire ne possédait ni version, ni date de réponse, ni échéance de
   révision, ni codes de justification auditables.
3. La cible énergétique et l'évaluation de prudence pouvaient évoluer sans
   contrat transactionnel dédié à leur révision.
4. Le schéma Room n'avait pas de migration vers un modèle médical plus riche.
5. Le tableau de bord ne permettait ni révision du questionnaire, ni suppression
   globale des données.
6. Aucune frontière de consentement et de minimisation n'existait pour une
   future IA.
7. Certains états d'interface avaient des valeurs implicites et les actions
   restaient navigables pendant des écritures.
8. L'état complet était assemblé depuis plusieurs sources observables, ce qui
   augmentait le risque d'un état transitoire partiel.

## 3. Modifications appliquées

### 3.1 Modèle médical explicite

Les réponses persistées utilisent désormais :

```kotlin
enum class HealthAnswer {
    YES,
    NO,
    UNSURE
}
```

L'absence de réponse reste uniquement un état de brouillon dans l'interface.
Un objet `HealthQuestionnaireAnswers` ne peut être construit que lorsque les
neuf questions ont reçu une réponse explicite.

Le questionnaire couvre désormais :

- perte de poids involontaire ;
- baisse d'appétit ;
- difficulté à avaler ou vomissements persistants ;
- grossesse ou allaitement ;
- antécédent ou suspicion de trouble du comportement alimentaire ;
- symptômes digestifs importants ou persistants ;
- maladie influençant le poids ou l'alimentation ;
- traitement influençant l'appétit, la glycémie ou le poids ;
- allergies ou intolérances alimentaires.

Le texte libre est facultatif, normalisé, limité à 500 caractères et présenté
comme une donnée locale à ne pas remplir avec des coordonnées ou documents.

### 3.2 Règles de prudence auditables

`SafetyRuleEngine` renvoie maintenant un `SafetyAssessment` composé d'un niveau
et d'un ensemble de `SafetyReason`. Les codes de raison sont enregistrés avec
les versions du questionnaire, des règles et de l'avertissement.

Les réponses positives ou incertaines aux signaux critiques suspendent le
repère automatique. Une allergie ou intolérance isolée active un niveau de
prudence sans rendre à elle seule l'estimation énergétique impossible.

Ce moteur est un triage technique conservateur, pas un dispositif médical et
pas un diagnostic. Ses libellés, seuils et comportements doivent être validés
par des professionnels compétents avant publication.

### 3.3 Révision périodique et cohérence transactionnelle

Chaque profil de prudence conserve :

- la version du questionnaire ;
- la version du moteur de règles ;
- la version de l'avertissement ;
- la date de création ;
- la date d'accusé de lecture ;
- la date des réponses ;
- la date limite de révision ;
- les raisons de la décision.

La révision est prévue six mois après la réponse et l'échéance est inclusive.
L'écran de révision exige un nouvel accusé de lecture. Le niveau de prudence et
le repère calorique sont mis à jour dans une seule transaction DAO : un nouveau
signal sensible ne peut pas laisser une ancienne cible active.

Le plan conserve aussi la provenance du calcul :

- poids utilisé ;
- date du calcul ;
- version de l'algorithme.

### 3.4 Migration Room 1 → 2

La migration est explicite et non destructive.

Pour le questionnaire historique :

- `1` devient `YES` ;
- `0` devient `NO` ;
- toute autre valeur devient `UNSURE` ;
- les nouvelles questions deviennent `UNSURE` ;
- une révision est exigée immédiatement ;
- le niveau devient `PROFESSIONAL_REVIEW_REQUIRED` jusqu'à cette révision.

Pour les objectifs historiques :

- les poids, dates, rythme et informations d'objectif sont conservés ;
- les anciennes calories sont neutralisées ;
- la provenance est ajoutée ;
- une version « review-required » signale que le calcul doit être réévalué.

La politique IA est créée désactivée uniquement lorsqu'un profil existe. Une
migration n'accorde jamais un consentement par déduction.

Les schémas `1.json` et `2.json` sont versionnés. Les tests instrumentés
couvrent une base peuplée, une valeur booléenne historique incohérente et une
base vide.

### 3.5 Lecture atomique de l'état

Le DAO expose une projection unique `StoredUserSetupEntity` issue d'une seule
requête. Le repository classe le résultat en :

- `Empty` lorsque les données fonctionnelles sont absentes ;
- `Incomplete` lorsqu'un ensemble partiel ou incohérent est détecté ;
- `Ready` uniquement lorsque toutes les composantes requises sont présentes.

Une donnée persistée inconnue n'est pas silencieusement remplacée par une
valeur par défaut : l'ouverture passe dans l'état d'erreur locale.

### 3.6 Gestion des données

Le tableau de bord permet maintenant de supprimer en une transaction :

- le profil ;
- le questionnaire ;
- les objectifs ;
- les mesures de poids ;
- la politique d'accès IA.

`PRAGMA secure_delete = ON` est appliqué à l'ouverture. Le fichier reste
chiffré par SQLCipher. La suppression actuelle garantit l'absence de lignes
fonctionnelles après le commit ; elle ne prétend pas être une procédure
forensique d'effacement de l'ensemble du fichier, de ses sauvegardes externes
ou d'un appareil compromis.

La phrase secrète SQLCipher générée est remise à zéro en mémoire si son
enveloppe ne peut pas être persistée. Lorsqu'une base existe mais que sa clé est
absente ou illisible, l'application échoue explicitement au lieu de générer une
nouvelle clé et de masquer une perte de données.

### 3.7 Front-end

Le parcours comporte quatre étapes. Les choix métaboliques, d'activité et de
rythme ne sont plus présélectionnés. Les neuf réponses médicales doivent être
explicites.

Le formulaire médical est partagé entre l'onboarding et la révision. Les
champs, boutons et retours système sont neutralisés pendant une écriture. Les
brouillons sensibles restent dans le `ViewModel` en mémoire et ne sont pas
copiés dans `SavedStateHandle` ou les journaux. Une destruction complète du
processus réinitialise donc volontairement un brouillon non enregistré.

Le tableau de bord présente :

- le niveau de prudence et l'échéance de révision ;
- le poids actuel, l'objectif et l'IMC indicatif ;
- le repère énergétique ou sa suspension ;
- la date et le poids ayant servi au calcul ;
- la date théorique lorsqu'elle reste raisonnable ;
- le statut d'accès IA ;
- les contrôles de suppression.

## 4. Frontière prévue pour la future IA

`AiDataAccessPolicy` est indépendante de l'accusé médical. Elle est désactivée
par défaut et contient des catégories de données autorisées, une version de
consentement, une date d'octroi, une éventuelle révocation et une date de mise
à jour.

`BuildAiNutritionContextUseCase` refuse la construction lorsque :

- la politique est désactivée ;
- l'identifiant ou la version du consentement est invalide ;
- les dates de consentement sont incohérentes ;
- aucune catégorie n'est autorisée ;
- le résumé de prudence n'est pas autorisé ;
- le questionnaire ou les règles ne sont plus à jour ;
- la révision est arrivée à échéance ;
- le profil sort de la plage d'âge du moteur ;
- une évaluation professionnelle est requise.

Le contexte construit n'inclut pas :

- l'identifiant Room ou métier ;
- la date de naissance exacte ;
- l'horodatage exact de la dernière pesée ;
- la note médicale libre.

Il transmet uniquement les sous-objets correspondant aux catégories
explicitement autorisées. Le contrat fixe
`calorieTargetMayBeModified = false`.

Cette couche est une frontière locale, pas une autorisation suffisante pour
déployer un service distant. Le futur connecteur devra encore ajouter un but
précis, un destinataire, une durée, une politique de rétention, une
authentification, une journalisation d'audit, une suppression côté serveur et
une validation des sorties.

## 5. Couverture de tests ajoutée

### Tests unitaires

- questionnaire complet et normalisation de la note ;
- échéance de révision inclusive ;
- tous les signaux critiques ;
- réponse incertaine explicable ;
- bornes d'IMC ;
- création d'un plan autorisé ou suspendu ;
- révision qui retire puis restaure une cible ;
- profil devenu hors plage d'âge ;
- consentement IA désactivé, incomplet, obsolète ou révoqué ;
- minimisation du contexte IA.

### Tests instrumentés

- projection Room vide ;
- enregistrement initial complet ;
- révision prudence + calories dans la même transaction ;
- suppression complète ;
- migration 1 → 2 sur base peuplée ;
- migration 1 → 2 sur base vide ;
- validation du schéma exporté.

## 6. Vérifications réalisées pendant la revue

Les contrôles suivants ont été exécutés sur le correctif :

- validation XML de toutes les ressources ;
- correspondance exhaustive entre `R.string.*` et `strings.xml` ;
- absence de ressources chaîne dupliquées ou orphelines ;
- contrôle des délimiteurs Kotlin hors chaînes et commentaires ;
- `git diff --check` ;
- reproduction du hash Room v1 puis calcul du hash v2 avec l'algorithme du
  compilateur Room ;
- exécution SQLite réelle de la migration sur base vide et peuplée ;
- comparaison des colonnes, affinités, nullabilités, clés et index avec le
  schéma Room v2 ;
- `PRAGMA integrity_check`.

Le build Gradle complet doit être rejoué dans un environnement Android doté du
SDK et des dépendances. L'environnement de revue ne disposait ni de la
distribution Gradle complète en cache, ni du SDK Android, et son accès réseau
de processus était bloqué. Cette limite ne remplace donc pas la CI demandée
ci-dessous.

## 7. Travail recommandé ensuite

### P0 — bloquant avant toute diffusion

1. Exécuter en CI :
   `testDebugUnitTest`, `lintDebug`, `assembleDebug` et
   `connectedDebugAndroidTest`.
2. Tester la migration sur un appareil avec la base réellement ouverte par
   SQLCipher, pas uniquement par SQLite de test.
3. Faire relire le questionnaire, les seuils et les textes d'urgence par un
   médecin ou diététicien qualifié.
4. Faire valider la notice, la base légale, les durées de conservation, les
   droits utilisateur et le futur consentement IA par un spécialiste de la
   protection des données.
5. Configurer signature de release, rétrécissement, règles R8, analyse de
   dépendances et politique de mise à jour.

### P1 — produit local utile

1. Ajouter la saisie de poids successive avec date, note facultative et
   validation.
2. Afficher historique et tendance robuste sur plusieurs mesures.
3. Recalculer le plan selon une règle explicite et testée, jamais sur une seule
   variation.
4. Ajouter modification du profil et de l'objectif avec historique.
5. Ajouter export portable et suppression renforcée incluant une stratégie de
   rotation ou destruction de clé.
6. Ajouter tests Compose des parcours, accessibilité, grands textes, rotation,
   process death et erreurs de stockage.

### P2 — préparation réelle de l'IA

1. Définir des cas d'usage étroits avant de choisir un modèle.
2. Ajouter une interface de consentement séparée montrant chaque catégorie,
   finalité, destinataire et durée.
3. Utiliser des autorisations courtes, révocables et vérifiées côté serveur.
4. Construire un back-end authentifié avec TLS, contrôle d'accès, audit,
   quotas, rétention minimale et suppression vérifiable.
5. Sérialiser uniquement `AiNutritionContext`, jamais `UserSetup`.
6. Valider les sorties contre des règles déterministes et interdire toute
   modification automatique d'une cible.
7. Prévoir refus, escalade professionnelle et tests adversariaux pour les
   contenus médicaux ou troubles alimentaires.

### P3 — fonctionnalités d'accompagnement

- repas et recettes tenant compte des allergies ;
- planification et liste de courses ;
- rappels opt-in ;
- rapports hebdomadaires ;
- observabilité technique sans données de santé ;
- stratégie hors ligne et synchronisation explicitement consentie.

## 8. Hypothèses et limites de cette évolution

- L'application reste réservée aux adultes de 18 à 100 ans pour l'équation
  actuelle.
- Un seul profil courant est géré localement.
- Il n'existe qu'un objectif actif.
- La date et l'heure de l'appareil sont considérées comme la référence locale.
- Les règles de prudence sont volontairement conservatrices.
- Aucun mécanisme de sauvegarde utilisateur chiffrée ou de récupération de clé
  n'est encore fourni.
- Aucun traitement IA n'est activé ni envoyé sur le réseau.
- Cette revue améliore la sécurité technique ; elle ne constitue ni une
  certification médicale, ni un audit juridique, ni une homologation de
  sécurité.
