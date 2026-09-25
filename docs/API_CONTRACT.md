# PsyAvocat — Contrat d'API REST & Spécifications Métier

Ce document formalise les contrats d'API, les formats d'échange JSON (DTO), les codes HTTP et les règles de validation pour les échanges entre les clients (Frontend Flutter, Frontend Web Angular) et le backend Spring Boot.

---

## 1. Principes et Conventions Globales

* **Base URL** : `http://localhost:8080/api`
* **Format des données** : JSON (`Content-Type: application/json; charset=UTF-8`)
* **Authentification** : Bearer Token Firebase ID Token dans l'en-tête `Authorization: Bearer <firebase-id-token>`
* **Identifiant utilisateur** : Le Firebase UID correspond à l'identifiant primaire (`id`) de l'entité `Utilisateur` dans MySQL.
* **Codes HTTP standards** :
  * `200 OK` : Succès de lecture ou de mise à jour.
  * `201 Created` : Ressource créée avec succès.
  * `204 No Content` : Suppression ou action réussie sans corps de réponse.
  * `400 Bad Request` : Erreur de validation métier ou syntaxique.
  * `401 Unauthorized` : Token manquant, expiré ou invalide.
  * `403 Forbidden` : Rôle ou droits insuffisants pour accéder à la ressource.
  * `404 Not Found` : Ressource demandée inexistante.
  * `409 Conflict` : Conflit d'état ou doublon (ex. profil déjà créé, créneau déjà réservé).
  * `500 Internal Server Error` : Erreur interne non gérée.

---

## 2. Format Standard des Erreurs (ApiErrorResponse)

Toute erreur renvoie un corps JSON standard :

```json
{
  "timestamp": "2026-09-25T18:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Le créneau sélectionné n'est plus disponible",
  "path": "/api/rendez-vous/psychologue",
  "validationErrors": {
    "disponibiliteId": "Ne doit pas être vide"
  }
}
```

---

## 3. Module 1 : Authentification & Profil Utilisateur

### 3.1. Vérification de session (`GET /me`)
* **Endpoint** : `GET /me`
* **Sécurité** : Authentifié (tout utilisateur avec un token Firebase valide)
* **Description** : Renvoie les informations d'identité Firebase et la présence d'un profil métier MySQL.
* **Réponse 200 OK** :
```json
{
  "authenticated": true,
  "firebaseUid": "firebase-uid-123",
  "email": "user@example.com",
  "userId": "firebase-uid-123",
  "hasMetierProfile": true,
  "nom": "Dupont",
  "prenom": "Jean",
  "roles": ["ROLE_PATIENT", "ROLE_USER"]
}
```

### 3.2. Récupération du profil complet (`GET /api/profil`)
* **Endpoint** : `GET /api/profil`
* **Sécurité** : Authentifié avec profil métier existant
* **Réponse 200 OK** :
```json
{
  "id": "firebase-uid-123",
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "user@example.com",
  "telephone": "0601020304",
  "typeUtilisateur": "PATIENT",
  "dateInscription": "2026-09-25",
  "statutValidation": null,
  "biographie": null,
  "ville": null,
  "adresse": null,
  "modeConsultation": null,
  "numeroBarreau": null,
  "numeroAgrement": null,
  "specialiteIds": []
}
```

### 3.3. Initialisation du profil métier (Onboarding)
Lors de la première inscription Firebase, l'utilisateur choisit son rôle métier :

* `POST /api/profil/patient` : Création du profil Patient
* `POST /api/profil/justiciable` : Création du profil Justiciable
* `POST /api/profil/avocat` : Création du profil Avocat (statut initial `PENDING`)
* `POST /api/profil/psychologue` : Création du profil Psychologue (statut initial `PENDING`)

**Corps de requête exemple (Avocat)** :
```json
{
  "nom": "Martin",
  "prenom": "Sophie",
  "telephone": "0612345678",
  "biographie": "Avocate au barreau de Paris spécialisée en droit de la famille.",
  "ville": "Paris",
  "adresse": "12 rue de Rivoli",
  "modeConsultation": "CABINET_ET_VISIO",
  "numeroBarreau": "BAR-75001",
  "specialiteIds": ["spec-uuid-1", "spec-uuid-2"]
}
```

---

## 4. Module 2 : Référentiels (Spécialités & Catégories de Besoin)

* `GET /api/referentiels/specialites` : Liste de toutes les spécialités.
* `GET /api/referentiels/categories-besoin` : Liste des catégories de besoin actives (filtrable par `typeProfessionnel=AVOCAT|PSYCHOLOGUE`).

---

## 5. Module 3 : Professionnels & Validation Administrateur

### 5.1. Recherche de professionnels
* `GET /api/professionnels`
  * Paramètres de recherche optionnels :
    * `type` : `AVOCAT` ou `PSYCHOLOGUE`
    * `ville` : Nom de ville
    * `specialiteId` : UUID de la spécialité
    * `modeConsultation` : `CABINET`, `VISIO`, `CABINET_ET_VISIO`
  * Règle métier : Seuls les professionnels au statut `APPROVED` sont visibles pour les utilisateurs généraux.
* `GET /api/professionnels/{id}` : Fiche détaillée d'un professionnel.

### 5.2. Validation par un Administrateur (`ROLE_ADMINISTRATEUR`)
* `GET /api/admin/professionnels/en-attente` : Liste des professionnels en statut `PENDING`.
* `PATCH /api/admin/professionnels/{id}/statut` :
  ```json
  {
    "statut": "APPROVED" // PENDING, APPROVED, REJECTED, SUSPENDED
  }
  ```

---

## 6. Module 4 : Disponibilités & Rendez-vous

### 6.1. Disponibilités du Professionnel
* `POST /api/disponibilites` (`ROLE_PROFESSIONNEL`) : Créer un créneau de disponibilité.
  ```json
  {
    "date": "2026-10-01",
    "heureDebut": "14:00:00",
    "heureFin": "15:00:00"
  }
  ```
* `GET /api/disponibilites/me` : Liste des disponibilités du professionnel connecté.
* `DELETE /api/disponibilites/{id}` : Supprimer une disponibilité libre.
* `GET /api/professionnels/{id}/disponibilites` (Public/Connecté) : Créneaux disponibles (`statut = "LIBRE"`).

### 6.2. Prise de Rendez-vous
* **Psychologue (Direct)** : `POST /api/rendez-vous/psychologue`
  ```json
  {
    "psychologueId": "pro-uid-456",
    "disponibiliteId": "disp-uuid-789",
    "mode": "VISIO",
    "montantTotal": 80.00
  }
  ```
  * Règle métier : L'acompte simulé prélevé est de 20% (soit 16.00 €). Le créneau passe en `RESERVE` et le rendez-vous en `CONFIRME`.

* **Avocat (Lié à une soumission acceptée)** : `POST /api/rendez-vous/avocat`
  ```json
  {
    "soumissionId": "soumission-uuid-123",
    "disponibiliteId": "disp-uuid-456",
    "mode": "CABINET"
  }
  ```
  * Règle métier : Le rendez-vous ne peut être pris que si la soumission de dossier est en statut `ACCEPTEE`. L'acompte de 20% est calculé sur le `tarifPropose` de l'avocat.

* `GET /api/rendez-vous` : Liste des rendez-vous de l'utilisateur connecté (patient/justiciable ou professionnel).
* `PATCH /api/rendez-vous/{id}/annuler` : Annulation du rendez-vous (libère le créneau).

---

## 7. Module 5 : Dossiers Juridiques (Justiciable ↔ Avocat)

* `POST /api/dossiers` (`ROLE_JUSTICIABLE`) : Création d'un dossier.
  ```json
  {
    "titre": "Litige bail commercial",
    "description": "Contestation de renouvellement et indemnité d'éviction."
  }
  ```
* `GET /api/dossiers` : Mes dossiers créés.
* `GET /api/dossiers/{id}` : Détail avec pièces jointes, échéances et soumissions.
* `POST /api/dossiers/{id}/soumissions` : Soumettre le dossier à un avocat spécifique.
  ```json
  {
    "avocatId": "avocat-uid-789"
  }
  ```
* `GET /api/avocat/soumissions` (`ROLE_AVOCAT`) : Dossiers reçus par l'avocat connecté.
* `PATCH /api/avocat/soumissions/{id}/repondre` (`ROLE_AVOCAT`) :
  ```json
  {
    "statut": "ACCEPTEE", // ou "REFUSEE"
    "reponse": "Je prends en charge votre dossier. Honoraire prévisionnel fixé.",
    "tarifPropose": 350.00
  }
  ```

---

## 8. Module 6 : Séances & Fiches Patient (Psychologue)

* `GET /api/psychologue/fiches-patient` (`ROLE_PSYCHOLOGUE`) : Liste des patients suivis.
* `GET /api/psychologue/fiches-patient/{patientId}` : Fiche avec historique des séances.
* `POST /api/psychologue/seances` : Enregistrement d'une séance.
* `POST /api/psychologue/seances/{id}/notes` : Ajout d'une note confidentielle de séance.

---

## 9. Module 7 : Messagerie

* `GET /api/conversations` : Liste des conversations de l'utilisateur connecté.
* `POST /api/conversations` : Démarrer une conversation avec un destinataire.
* `GET /api/conversations/{id}/messages` : Historique des messages d'une conversation.
* `POST /api/conversations/{id}/messages` : Envoi d'un message.

---

## 10. Module 8 : Questionnaire d'Orientation

* `GET /api/orientation/questionnaires` : Liste des questionnaires actifs avec questions et réponses.
* `POST /api/orientation/soumettre` : Soumission des réponses -> Calcul du score et recommandation de la `CategorieBesoin`.
