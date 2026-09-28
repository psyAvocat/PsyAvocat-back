# Matrice des Responsabilités des Méthodes et Règles Métier — PsyAvocat Backend

Ce document définit la répartition officielle des méthodes, cas d'utilisation et règles métier entre les différentes couches du backend Spring Boot de **PsyAvocat**.

---

## 1. Matrice des Responsabilités

| Fonctionnalité / Méthode | Entity | Service Interface | Service Impl | Repository | Controller |
|---|---|---|---|---|---|
| **Consulter profil courant** | Données uniquement (`Utilisateur`) | `ProfilService.getCurrentProfile()` | `ProfilServiceImpl` | `UtilisateurRepository` | `ProfilController.getCurrentProfile()` |
| **Créer profil Patient** | Données uniquement (`Patient`) | `ProfilService.createPatientProfile(...)` | `ProfilServiceImpl` | `PatientRepository` | `ProfilController.createPatientProfile(...)` |
| **Créer profil Justiciable** | Données uniquement (`Justiciable`) | `ProfilService.createJusticiableProfile(...)` | `ProfilServiceImpl` | `JusticiableRepository` | `ProfilController.createJusticiableProfile(...)` |
| **Créer profil Avocat** | Données uniquement (`Avocat`) | `ProfilService.createAvocatProfile(...)` | `ProfilServiceImpl` | `AvocatRepository` + `SpecialiteRepository` | `ProfilController.createAvocatProfile(...)` |
| **Créer profil Psychologue** | Données uniquement (`Psychologue`) | `ProfilService.createPsychologueProfile(...)` | `ProfilServiceImpl` | `PsychologueRepository` + `SpecialiteRepository` | `ProfilController.createPsychologueProfile(...)` |
| **Modifier profil** | Données uniquement (`Utilisateur`) | `ProfilService.updateProfile(...)` | `ProfilServiceImpl` | `UtilisateurRepository` | `ProfilController.updateProfile(...)` |
| **Rechercher professionnels** | Données uniquement (`Professionnel`) | `ProfessionnelService.searchProfessionnels(...)` | `ProfessionnelServiceImpl` | `ProfessionnelRepository` | `ProfessionnelController.searchProfessionnels(...)` |
| **Validation pro par admin** | Données uniquement (`Professionnel`) | `ProfessionnelService.updateStatutValidation(...)` | `ProfessionnelServiceImpl` | `ProfessionnelRepository` | `AdminController.updateStatutValidation(...)` |
| **Publier créneau dispo** | Données uniquement (`Disponibilite`) | `DisponibiliteService.createDisponibilite(...)` | `DisponibiliteServiceImpl` | `DisponibiliteRepository` | `DisponibiliteController.createDisponibilite(...)` |
| **Supprimer créneau dispo** | Données uniquement (`Disponibilite`) | `DisponibiliteService.deleteDisponibilite(...)` | `DisponibiliteServiceImpl` | `DisponibiliteRepository` | `DisponibiliteController.deleteDisponibilite(...)` |
| **Consulter créneaux libres** | Données uniquement (`Disponibilite`) | `DisponibiliteService.getDisponibilitesLibres(...)` | `DisponibiliteServiceImpl` | `DisponibiliteRepository` | `DisponibiliteController.getDisponibilitesLibres(...)` |
| **Prendre RDV Psychologue** | Données uniquement (`RendezVous`) | `RendezVousService.createRendezVousPsychologue(...)` | `RendezVousServiceImpl` | `RendezVousRepository` + `DisponibiliteRepository` | `RendezVousController.createRendezVousPsychologue(...)` |
| **Prendre RDV Avocat** | Données uniquement (`RendezVous`) | `RendezVousService.createRendezVousAvocat(...)` | `RendezVousServiceImpl` | `RendezVousRepository` + `DisponibiliteRepository` + `SoumissionDossierRepository` | `RendezVousController.createRendezVousAvocat(...)` |
| **Consulter mes RDV** | Données uniquement (`RendezVous`) | `RendezVousService.getMyRendezVous()` | `RendezVousServiceImpl` | `RendezVousRepository` | `RendezVousController.getMyRendezVous()` |
| **Annuler RDV** | Données uniquement (`RendezVous`) | `RendezVousService.annulerRendezVous(...)` | `RendezVousServiceImpl` | `RendezVousRepository` | `RendezVousController.annulerRendezVous(...)` |
| **Calculer acompte 20 %** | Données uniquement (`Paiement`) | `PaiementService.calculerAcompteRendezVous(...)` | `PaiementServiceImpl` | Aucun (calcul métier) | Indirectement via `RendezVousService` |
| **Traiter acompte simulé** | Données uniquement (`Paiement`) | `PaiementService.traiterAcompteRendezVous(...)` | `PaiementServiceImpl` | `PaiementRepository` | Indirectement via `RendezVousService` |
| **Créer dossier juridique** | Données uniquement (`Dossier`) | `DossierService.createDossier(...)` | `DossierServiceImpl` | `DossierRepository` + `JusticiableRepository` | `DossierController.createDossier(...)` |
| **Consulter mes dossiers** | Données uniquement (`Dossier`) | `DossierService.getMyDossiers()` | `DossierServiceImpl` | `DossierRepository` | `DossierController.getMyDossiers()` |
| **Soumettre dossier à avocat** | Données uniquement (`SoumissionDossier`) | `SoumissionDossierService.soumettreDossier(...)` | `SoumissionDossierServiceImpl` | `SoumissionDossierRepository` + `DossierRepository` + `AvocatRepository` | `DossierController.soumettreDossier(...)` |
| **Consulter soumissions avocat** | Données uniquement (`SoumissionDossier`) | `SoumissionDossierService.getSoumissionsPourAvocat(...)` | `SoumissionDossierServiceImpl` | `SoumissionDossierRepository` | `DossierController.getSoumissionsPourAvocat(...)` |
| **Répondre à soumission** | Données uniquement (`SoumissionDossier`) | `SoumissionDossierService.repondreSoumission(...)` | `SoumissionDossierServiceImpl` | `SoumissionDossierRepository` | `DossierController.repondreSoumission(...)` |
| **Évaluer questionnaire** | Données uniquement (`ResultatOrientation`) | `OrientationService.evaluerQuestionnaire(...)` | `OrientationServiceImpl` | `QuestionnaireRepository` + `ReponseRepository` + `ResultatOrientationRepository` | `OrientationController.evaluerQuestionnaire(...)` |
| **Créer conversation** | Données uniquement (`Conversation`) | `MessagerieService.createConversation(...)` | `MessagerieServiceImpl` | `ConversationRepository` + `MessageContactRepository` | `MessagerieController.createConversation(...)` |
| **Envoyer message** | Données uniquement (`MessageContact`) | `MessagerieService.sendMessage(...)` | `MessagerieServiceImpl` | `MessageContactRepository` + `ConversationRepository` | `MessagerieController.sendMessage(...)` |
| **Créer séance & fiche** | Données uniquement (`Seance`, `FichePatient`) | `SeanceService.createSeance(...)` | `SeanceServiceImpl` | `SeanceRepository` + `FichePatientRepository` | `SeanceController.createSeance(...)` |
| **Ajouter note de séance** | Données uniquement (`NoteSeance`) | `SeanceService.ajouterNoteSeance(...)` | `SeanceServiceImpl` | `NoteSeanceRepository` + `SeanceRepository` | `SeanceController.ajouterNoteSeance(...)` |
| **Envoyer push FCM** | Aucune entité | Non exposé (Infrastructure) | `FirebaseMessagingService` | Aucun | Déclenché par les services lors d'événements |

---

## 2. Workflow Rendez-Vous (Règle Métier Absolue)

### 2.1 Principe d'immédiateté (Modèle Direct)
Le système PsyAvocat **ne comporte aucune notion de "demande de rendez-vous en attente d'acceptation du professionnel"**.  
Le professionnel publie à l'avance ses créneaux de disponibilité. La prise de rendez-vous est directe et immédiate.

```text
1. Publication des créneaux
   Professionnel → DisponibiliteService.createDisponibilite() → Statut 'LIBRE'

2. Consultation
   Patient / Justiciable → DisponibiliteService.getDisponibilitesLibres()

3. Réservation directe
   Patient / Justiciable choisit le créneau disponible
   ↓
   RendezVousService :
     a. Vérifie que le créneau est toujours 'LIBRE'
     b. Verrouille immédiatement le créneau → Statut 'RESERVE'
     c. Calcule l'acompte obligatoire de 20 % via PaiementService
     d. Traite et enregistre le paiement simulé → Statut 'PAYE'
     e. Crée et confirme le rendez-vous → Statut 'CONFIRME'
```

### 2.2 Annulation de Rendez-Vous
- Un rendez-vous déjà planifié peut être annulé par le patient ou par le praticien.
- `RendezVousService.annulerRendezVous(id)` vérifie que l'utilisateur authentifié est bien l'une des parties prenantes du rendez-vous, puis bascule le statut à `ANNULE`.
- **Règle de non-invention** : Aucune règle arbitraire de pénalité, de délai limite (ex: 24h/48h) ou de pourcentage de remboursement n'est injectée tant que les règles juridiques et commerciales ne sont pas formellement spécifiées par le métier.

---

## 3. Workflow SoumissionDossier (Dossiers Juridiques)

### 3.1 Séparation Dossier vs SoumissionDossier
- Un `Dossier` appartient à un `Justiciable` et représente une affaire globale.
- Le justiciable peut soumettre ce même dossier à **plusieurs avocats différents**.
- Chaque avocat reçoit une `SoumissionDossier` distincte (relation `Dossier ↔ Avocat`).

```text
Justiciable
    ↓ (crée son dossier)
Dossier (statut 'OUVERT')
    ↓ (soumet à l'Avocat A et à l'Avocat B)
SoumissionDossier A ('EN_ATTENTE')        SoumissionDossier B ('EN_ATTENTE')
    ↓                                         ↓
Avocat A accepte avec tarif 200€          Avocat B décline (indisponible)
    ↓                                         ↓
Soumission A: 'ACCEPTEE'                  Soumission B: 'REFUSEE'
(Tarif proposé: 200 €)                    (Le dossier global reste 'OUVERT')
    ↓
Justiciable compare les réponses et consulte les disponibilités de l'Avocat A retenu
    ↓
Prise directe du RDV (20 % acompte = 40 €) → RDV 'CONFIRME'
    ↓
RÈGLE D'EXCLUSIVITÉ (SOLUTION 2) :
  1. Soumission A → 'RETENUE'
  2. Dossier → 'PRIS_EN_CHARGE'
  3. Toutes les autres soumissions (B, C...) passent automatiquement à 'CADUQUE'
  4. Disparition / Inaccessibilité : Les avocats concurrents n'ont plus accès au dossier (403 Forbidden)
```

### 3.2 Indépendance des réponses & Monopole de prise en charge
- L'acceptation ou le refus d'un avocat n'affecte en rien les soumissions adressées aux autres avocats pendant la phase d'évaluation.
- L'avocat accepte **la prise en charge du dossier** ; il n'accepte pas un "rendez-vous". Le rendez-vous est pris ensuite directement par le client.
- **Règle anti-doublon absolue (Solution 2)** :
  Dès que le client confirme son rendez-vous et verse l'acompte de 20 % chez l'avocat de son choix :
  - Le dossier est définitivement verrouillé (`PRIS_EN_CHARGE`).
  - Toutes les autres soumissions concurrentes deviennent immédiatement `CADUQUE`.
  - Le dossier disparaît du tableau de bord actif des autres avocats et toute tentative d'accès via `GET /api/dossiers/{id}` est bloquée avec un code `403 Forbidden` pour respecter le secret professionnel.
  - Tout nouvel avocat tentant de répondre à une soumission caduque reçoit une erreur `400 Bad Request`.

---

## 4. Règles Strictes sur les Entités JPA

Conformément à la décision d'architecture :
1. **Les entités ne contiennent aucune méthode métier**.
2. **Les entités ne contiennent aucun appel technique** (Firebase, FCM, Repositories, Services, WebSockets).
3. Les entités portent uniquement les attributs, les relations JPA (`@ManyToOne`, `@OneToMany`, `@ManyToMany`), les identifiants et les accesseurs Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`).
