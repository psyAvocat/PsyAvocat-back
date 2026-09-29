# Algorithme d'Orientation Psychologique V1 — PsyAvocat

> [!IMPORTANT]
> **AVERTISSEMENT DÉONTOLOGIQUE ET LÉGAL** :
> Ce questionnaire est un **outil d'orientation et de mise en relation** vers des professionnels qualifiés (Psychologues).
> Il ne constitue en **AUCUN CAS un outil de diagnostic médical ou psychologique**.
> Aucun résultat produit ne peut se substituer à une consultation clinique approfondie réalisée par un professionnel de santé mentale agréé.

---

## 1. Origine des Données et Architecture

Toutes les données du questionnaire sont issues du dataset officiel :
`src/main/resources/data/questionnaires/questionnaire_psychologique_v1.json`

Le système suit une séparation stricte des responsabilités (SOLID / POO) :
- **Source de données** : Fichier JSON officiel versionné.
- **Seeding idempotent** : `QuestionnairePsychologiqueSeeder` synchronise en base MySQL les 8 catégories de besoin, le questionnaire, les 10 questions, les 80 réponses et les 80 lignes de pondération.
- **Entités JPA anémiques** : `com.psyavocat.entity.*` ne contiennent aucune logique de score ni de matching.
- **Couche Métier** : `OrientationServiceImpl` calcule le résultat à partir des pondérations dynamiques lues en base de données MySQL.
- **Matching des Professionnels** : Découplé dans `MatchingProfessionnelService` / `MatchingProfessionnelServiceImpl`.

---

## 2. Les 8 Catégories d'Orientation

| Code Catégorie | Libellé / Domaine | Description Métier |
| :--- | :--- | :--- |
| **`CLINIQUE`** | Psychologie clinique et bien-être émotionnel | Difficultés émotionnelles personnelles, stress, anxiété, estime de soi et périodes difficiles. |
| **`FAMILLE_COUPLE`** | Psychologie familiale et conjugale | Difficultés de couple, relations familiales, séparation, parentalité et conflits familiaux. |
| **`EDUCATION_DEVELOPPEMENT`** | Psychologie de l'éducation et du développement | Enfants, adolescents, apprentissage, études, développement et difficultés scolaires. |
| **`TRAVAIL`** | Psychologie du travail et des organisations | Stress professionnel, surcharge, relations au travail, transition et équilibre professionnel. |
| **`SANTE`** | Psychologie de la santé | Adaptation psychologique à une maladie, un traitement ou un changement lié à la santé. |
| **`SOCIAL_COMMUNAUTE`** | Psychologie sociale et communautaire | Isolement, relations sociales, adaptation à l’environnement social et vie communautaire. |
| **`HABITAT_CADRE_VIE`** | Habitat et cadre de vie | Impact du logement, du déménagement et du cadre de vie sur le bien-être psychologique. |
| **`NEUROPSYCHOLOGIE`** | Neuropsychologie | Mémoire, attention, concentration et difficultés cognitives. |

---

## 3. Structure du Questionnaire et Grille de Pondération

- **Nombre de questions** : 10 questions ordonnées (Q01 à Q10).
- **Nombre de réponses par question** : 8 réponses (une par catégorie cible).
- **Mode de sélection** : **Sélection unique obligatoire** (1 seule réponse par question).
- **Total des réponses** : 80 réponses uniques.
- **Poids attribués par question dans le dataset V1** :
  - **Q01** (Motif principal) : `poids = 10`
  - **Q02** (Contexte d'apparition) : `poids = 8`
  - **Q03** (Personne concernée) : `poids = 8`
  - **Q04** (Aspect le plus perturbé) : `poids = 7`
  - **Q05** (Situation actuelle) : `poids = 7`
  - **Q06** (Relation la plus préoccupante) : `poids = 8`
  - **Q07** (Changement récent) : `poids = 6`
  - **Q08** (Type d'accompagnement recherché) : `poids = 9`
  - **Q09** (Gêne quotidienne principale) : `poids = 7`
  - **Q10** (Amélioration prioritaire souhaitée) : `poids = 9`

---

## 4. Fonctionnement du Calcul du Score

Lorsqu'un utilisateur soumet ses réponses via `POST /api/orientation/evaluer` :

1. **Validation technique** :
   - Vérification de l'existence et de l'activation du questionnaire.
   - Contrôle d'unicité : 1 seule réponse autorisée par question.
   - Contrôle d'exhaustivité : toutes les questions obligatoires doivent avoir reçu une réponse.
2. **Agrégation des pondérations (`PonderationOrientation`)** :
   - Pour chaque identifiant de réponse sélectionné, récupération de sa pondération MySQL.
   - Sommation des poids par catégorie cible :
     $$\text{Score}(\text{Catégorie}_k) = \sum_{r \in \text{Sélection}} \text{Poids}(r, \text{Catégorie}_k)$$
3. **Sélection de la Catégorie Gagnante** :
   - La catégorie ayant obtenu le score maximal le plus élevé est retenue.

---

## 5. Stratégie de Résolution des Égalités (Tie-Breaking)

Si deux ou plusieurs catégories obtiennent exactement le même score maximal :
1. **Étape 1 (Priorité au motif principal)** : Le système analyse la réponse choisie à la question **Q01** (« *Quel est votre motif principal aujourd'hui ?* », dotée du poids le plus fort de 10 points). Si la catégorie ciblée par Q01 figure parmi les ex-aequo, elle est immédiatement déclarée gagnante. Le statut de départage retourné est `EGALITE_DEPARTAGEE_PAR_Q01`.
2. **Étape 2 (Départage déterministe documenté)** : Dans le cas exceptionnel où la réponse à Q01 ne correspondrait à aucune des catégories ex-aequo, le système applique un ordre alphabétique déterministe sur le code catégorie (`CLINIQUE`, `EDUCATION_DEVELOPPEMENT`, etc.), et le statut de départage retourné est `EGALITE_DEPARTAGEE_DETERMINISTE`.

---

## 6. Matching des Professionnels

Une fois la `CategorieBesoin` déterminée :
1. Le service délègue la recherche à `MatchingProfessionnelService`.
2. Les psychologues inscrits et validés (`statutValidation = 'VALIDE'`) correspondant aux spécialités de la catégorie sont recherchés dans MySQL.
3. Les 5 meilleurs professionnels qualifiés sont associés au `ResultatOrientation` et retournés dans le DTO pour affichage immédiat dans l'application.

---

## 7. Limites et Évolutivité

- **Non-médical** : L'algorithme a pour seule vocation de diriger vers la consultation adaptée la plus pertinente.
- **Données 100% dynamiques** : L'ajout de nouvelles questions ou l'ajustement des barèmes se fait en base de données ou dans le JSON sans modifier une seule ligne de code Java ou Flutter.
