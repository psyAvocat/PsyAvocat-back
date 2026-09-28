# Architecture Métier du Backend PsyAvocat — Principes POO & SOLID

## 1. Philosophie Architecturale

Le backend Spring Boot de **PsyAvocat** est conçu selon les principes de l'**Inversion de Dépendance (DIP)**, de la **Séparation des Responsabilités (SRP)** et des patrons de conception logicielle éprouvés (**Clean Layered Architecture**).

### Chaîne de responsabilité stricte :

```text
HTTP Request (Flutter / Angular)
      ↓
Controller (REST API)
      ↓
Service Interface (Contrat métier)
      ↓
Service Implementation (Logique métier, orchestration & transactions)
      ↓
Repository Interface (Persistance Spring Data JPA)
      ↓
Entity (Modèle de données & mapping JPA)
      ↓
Base de données relationnelle MySQL
```

Pour les composants d'infrastructure externes :
```text
Service Implementation
      ↓
FirebaseMessagingService / Firebase SDK
      ↓
Firebase Cloud Messaging (FCM) / Firebase Authentication
```

---

## 2. Responsabilité de chaque couche

### 2.1 Couche Présentation : `controller`
- **Rôle unique** : Réceptionner les requêtes HTTP, valider les Request DTOs via Bean Validation (`@Valid`), router vers l'interface de service appropriée et renvoyer les Response DTOs / HTTP Status.
- **Règles strictes** :
  - **Dépendance exclusive aux interfaces de Service** : Les contrôleurs n'injectent jamais d'implémentations concrètes (`*ServiceImpl`) ni de `Repositories`.
  - **Aucune logique métier** : Aucun calcul de tarif, de statut, de validation d'état ou d'appel Firebase.
  - **Contrôleurs minces (Thin Controllers)**.

### 2.2 Couche Contrats Métier : `service` (Interfaces)
- **Rôle unique** : Définir les contrats fonctionnels et cas d'utilisation (Use Cases) indépendamment des détails d'implémentation.
- **Bénéfices** : Testabilité accrue (mocks faciles), respect du principe Open/Closed (OCP) et Inversion de Dépendance (DIP).

### 2.3 Couche Logique Métier : `service/impl` (Implémentations)
- **Rôle unique** : Orchestrer les règles métier, les vérifications d'autorisation contextuelle, les calculs (ex: 20 % d'acompte), les transitions d'état et les frontières transactionnelles (`@Transactional`).
- **Composants clés** :
  - `RendezVousServiceImpl` : Prise directe de rendez-vous sur créneau disponible, calcul d'acompte (20 %), confirmation immédiate, verrouillage exclusif du dossier juridique (`PRIS_EN_CHARGE`), bascule de la soumission choisie à `RETENUE` et auto-invalidation des soumissions concurrentes (`CADUQUE`).
  - `SoumissionDossierServiceImpl` : Soumission multi-avocats, réponses indépendantes avec tarif proposé, filtrage des soumissions caduques pour garantir la disparition du dossier chez les confrères non retenus.
  - `DossierServiceImpl` : Gestion du cycle de vie du dossier et contrôle d'accès strict (accès révoqué avec un `403 Forbidden` pour tout avocat dont la soumission est devenue caduque).
  - `PaiementServiceImpl` : Simulation des transactions financières et calcul des acomptes (20 %).
  - `DisponibiliteServiceImpl` : Publication et réservation des créneaux horaires.
  - `OrientationServiceImpl` : Évaluation des questionnaires et calcul de score.
  - `ProfilServiceImpl` & `ProfessionnelServiceImpl` : Gestion des profils et validations administratives.
  - `MessagerieServiceImpl` & `SeanceServiceImpl` : Conversations et consultations.

### 2.4 Couche Persistance : `repository`
- **Rôle unique** : Accès aux données MySQL via Spring Data JPA.
- **Règles strictes** : Contient uniquement les signatures de requêtes dérivées ou annotées (`@Query`). Ne contient aucune logique métier applicative.

### 2.5 Couche Modèle : `entity`
- **Rôle unique** : Objets de persistance JPA purs (champs, annotations JPA, clés étrangères, Lombok `@Getter` / `@Setter` / `@NoArgsConstructor`).
- **RÈGLE NON NÉGOCIABLE** :
  - **Aucune méthode métier dans les entités**.
  - **Aucun appel de service, repository, Firebase ou WebSocket dans les entités**.
  - Le modèle de domaine est volontairement un modèle de données persistant pour éviter la dispersion de la logique et garantir la testabilité pure des services.

### 2.6 Couche Contrats API & Mappers : `dto` & `mapper`
- `RequestDTO` & `ResponseDTO` : Découplent totalement la structure de la base de données de l'API REST exposée aux clients mobiles Flutter et web Angular.
- `mapper` : Centralise les conversions entre DTOs et Entités.

---

## 3. Application des Principes SOLID

1. **S - Single Responsibility Principle (SRP)** :
   - Découpage strict des services : `DossierService` gère le cycle de vie du dossier juridique ; `SoumissionDossierService` gère les propositions et réponses d'avocats ; `PaiementService` gère les simulations et acomptes.
2. **O - Open/Closed Principle (OCP)** :
   - Les contrôleurs dépendent d'abstractions (interfaces). De nouvelles implémentations de paiement ou d'orientation peuvent être ajoutées sans impacter la couche Web.
3. **L - Liskov Substitution Principle (LSP)** :
   - Les classes `Patient`, `Justiciable`, `Avocat` et `Psychologue` étendent la classe abstraite `Utilisateur` et respectent rigoureusement ses contrats.
4. **I - Interface Segregation Principle (ISP)** :
   - Interfaces ciblées et cohérentes (`RendezVousService`, `DisponibiliteService`, `SoumissionDossierService`) plutôt qu'un service monolithique `PsyAvocatService`.
5. **D - Dependency Inversion Principle (DIP)** :
   - Les contrôleurs dépendent d'interfaces de service (`RendezVousService`, `DisponibiliteService`), injectées par constructeur Spring.

---

## 4. Gestion de la Sécurité & Authentification

- **Firebase Authentication** reste la source unique de vérité pour l'authentification (tokens JWT Firebase vérifiés par `FirebaseAuthenticationFilter`).
- L'entité `Utilisateur` ne stocke aucun mot de passe en dur, aucun hash local et aucun mécanisme de session maison.
- Les identifiants (`id`) correspondent aux UIDs Firebase pour garantir une synchronisation fluide entre MySQL et Firebase.
