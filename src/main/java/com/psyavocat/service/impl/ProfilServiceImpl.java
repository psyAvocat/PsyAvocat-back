package com.psyavocat.service.impl;

import com.psyavocat.dto.profil.*;
import com.psyavocat.entity.*;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.UserMapper;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticatedUser;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.ProfilService;
import com.psyavocat.service.support.ClientAccounts;
import com.psyavocat.service.support.PhoneNumbers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ProfilServiceImpl implements ProfilService {

    private final AuthenticationContext authenticationContext;
    private final UtilisateurRepository utilisateurRepository;
    private final PatientRepository patientRepository;
    private final JusticiableRepository justiciableRepository;
    private final AvocatRepository avocatRepository;
    private final PsychologueRepository psychologueRepository;
    private final SpecialiteRepository specialiteRepository;
    private final UserMapper userMapper;
    private final com.psyavocat.storage.service.ImageStorageService imageStorageService;
    private final AdministrateurRepository administrateurRepository;
    private final com.psyavocat.service.NotificationService notificationService;
    private final ClientRepository clientRepository;

    public ProfilServiceImpl(
            AuthenticationContext authenticationContext,
            UtilisateurRepository utilisateurRepository,
            PatientRepository patientRepository,
            JusticiableRepository justiciableRepository,
            AvocatRepository avocatRepository,
            PsychologueRepository psychologueRepository,
            SpecialiteRepository specialiteRepository,
            UserMapper userMapper,
            com.psyavocat.storage.service.ImageStorageService imageStorageService,
            AdministrateurRepository administrateurRepository,
            com.psyavocat.service.NotificationService notificationService,
            ClientRepository clientRepository
    ) {
        this.authenticationContext = authenticationContext;
        this.utilisateurRepository = utilisateurRepository;
        this.patientRepository = patientRepository;
        this.justiciableRepository = justiciableRepository;
        this.avocatRepository = avocatRepository;
        this.psychologueRepository = psychologueRepository;
        this.specialiteRepository = specialiteRepository;
        this.userMapper = userMapper;
        this.imageStorageService = imageStorageService;
        this.administrateurRepository = administrateurRepository;
        this.notificationService = notificationService;
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable pour l'identifiant : " + uid));
        return userMapper.toProfileResponse(utilisateur);
    }

    @Override
    public UserProfileResponse createClientProfile(CreateClientRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        checkIfProfileAlreadyExists(authUser.getFirebaseUid());

        Client client = new Client();
        client.setId(authUser.getFirebaseUid());
        client.setEmail(authUser.getEmail());
        client.setNom(request.getNom().trim());
        client.setPrenom(request.getPrenom().trim());
        client.setTelephone(validerTelephone(request.getTelephone(), authUser.getFirebaseUid(), true));
        client.setDateInscription(LocalDate.now());

        Client saved = clientRepository.save(client);
        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse createPatientProfile(CreatePatientRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        checkIfProfileAlreadyExists(authUser.getFirebaseUid());

        Patient patient = new Patient();
        patient.setId(authUser.getFirebaseUid());
        patient.setEmail(authUser.getEmail());
        patient.setNom(request.getNom());
        patient.setPrenom(request.getPrenom());
        patient.setTelephone(validerTelephone(request.getTelephone(), authUser.getFirebaseUid(), false));
        patient.setDateInscription(LocalDate.now());

        Patient saved = patientRepository.save(patient);
        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse createJusticiableProfile(CreateJusticiableRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        checkIfProfileAlreadyExists(authUser.getFirebaseUid());

        Justiciable justiciable = new Justiciable();
        justiciable.setId(authUser.getFirebaseUid());
        justiciable.setEmail(authUser.getEmail());
        justiciable.setNom(request.getNom());
        justiciable.setPrenom(request.getPrenom());
        justiciable.setTelephone(validerTelephone(request.getTelephone(), authUser.getFirebaseUid(), false));
        justiciable.setDateInscription(LocalDate.now());

        Justiciable saved = justiciableRepository.save(justiciable);
        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse createAvocatProfile(CreateAvocatRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        java.util.Optional<Utilisateur> existingOpt = utilisateurRepository.findById(authUser.getFirebaseUid());
        if (existingOpt.isPresent()) {
            Utilisateur existing = existingOpt.get();
            if (existing instanceof Avocat existingAvocat && "PENDING".equalsIgnoreCase(existingAvocat.getStatutValidation())) {
                existingAvocat.setNom(request.getNom());
                existingAvocat.setPrenom(request.getPrenom());
                existingAvocat.setTelephone(validerTelephone(request.getTelephone(), authUser.getFirebaseUid(), false));
                existingAvocat.setBiographie(request.getBiographie());
                existingAvocat.setVille(request.getVille());
                existingAvocat.setAdresse(request.getAdresse());
                existingAvocat.setModeConsultation(request.getModeConsultation());
                existingAvocat.setNumeroBarreau(validerNumeroBarreau(request.getNumeroBarreau(), authUser.getFirebaseUid()));
                if (request.getSpecialiteIds() != null && !request.getSpecialiteIds().isEmpty()) {
                    List<Specialite> specialites = specialiteRepository.findAllById(request.getSpecialiteIds());
                    for (Specialite s : specialites) {
                        String resolved = s.resolveTypeProfessionnel();
                        if ("PSYCHOLOGUE".equalsIgnoreCase(resolved)) {
                            throw new com.psyavocat.exception.BadRequestException("La spécialité '" + s.getNom() + "' n'est pas applicable à un avocat.");
                        }
                    }
                    existingAvocat.setSpecialites(specialites);
                }
                Avocat saved = avocatRepository.save(existingAvocat);
                return userMapper.toProfileResponse(saved);
            } else {
                throw new ConflictException("Un profil métier existe déjà pour cet utilisateur");
            }
        }

        Avocat avocat = new Avocat();
        avocat.setId(authUser.getFirebaseUid());
        avocat.setEmail(authUser.getEmail());
        avocat.setNom(request.getNom());
        avocat.setPrenom(request.getPrenom());
        avocat.setTelephone(validerTelephone(request.getTelephone(), authUser.getFirebaseUid(), false));
        avocat.setBiographie(request.getBiographie());
        avocat.setVille(request.getVille());
        avocat.setAdresse(request.getAdresse());
        avocat.setModeConsultation(request.getModeConsultation());
        avocat.setNumeroBarreau(validerNumeroBarreau(request.getNumeroBarreau(), authUser.getFirebaseUid()));
        avocat.setStatutValidation("PENDING");
        avocat.setDateInscription(LocalDate.now());

        if (request.getSpecialiteIds() != null && !request.getSpecialiteIds().isEmpty()) {
            List<Specialite> specialites = specialiteRepository.findAllById(request.getSpecialiteIds());
            for (Specialite s : specialites) {
                String resolved = s.resolveTypeProfessionnel();
                if ("PSYCHOLOGUE".equalsIgnoreCase(resolved)) {
                    throw new com.psyavocat.exception.BadRequestException("La spécialité '" + s.getNom() + "' n'est pas applicable à un avocat.");
                }
            }
            avocat.setSpecialites(specialites);
        }

        Avocat saved = avocatRepository.save(avocat);

        // Notification immédiate aux administrateurs
        List<Administrateur> admins = administrateurRepository.findAll();
        for (Administrateur admin : admins) {
            notificationService.sendNotification(
                admin.getId(),
                "INSCRIPTION_PRO",
                "Nouvelle demande d'inscription : " + saved.getPrenom() + " " + saved.getNom() + " (Avocat). Dossier en attente de vérification.",
                null
            );
        }

        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse createPsychologueProfile(CreatePsychologueRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        java.util.Optional<Utilisateur> existingOpt = utilisateurRepository.findById(authUser.getFirebaseUid());
        if (existingOpt.isPresent()) {
            Utilisateur existing = existingOpt.get();
            if (existing instanceof Psychologue existingPsychologue && "PENDING".equalsIgnoreCase(existingPsychologue.getStatutValidation())) {
                existingPsychologue.setNom(request.getNom());
                existingPsychologue.setPrenom(request.getPrenom());
                existingPsychologue.setTelephone(validerTelephone(request.getTelephone(), authUser.getFirebaseUid(), false));
                existingPsychologue.setBiographie(request.getBiographie());
                existingPsychologue.setVille(request.getVille());
                existingPsychologue.setAdresse(request.getAdresse());
                existingPsychologue.setModeConsultation(request.getModeConsultation());
                existingPsychologue.setNumeroAgrement(validerNumeroAgrement(request.getNumeroAgrement(), authUser.getFirebaseUid()));
                if (request.getSpecialiteIds() != null && !request.getSpecialiteIds().isEmpty()) {
                    List<Specialite> specialites = specialiteRepository.findAllById(request.getSpecialiteIds());
                    for (Specialite s : specialites) {
                        String resolved = s.resolveTypeProfessionnel();
                        if ("AVOCAT".equalsIgnoreCase(resolved)) {
                            throw new com.psyavocat.exception.BadRequestException("La spécialité '" + s.getNom() + "' n'est pas applicable à un psychologue.");
                        }
                    }
                    existingPsychologue.setSpecialites(specialites);
                }
                Psychologue saved = psychologueRepository.save(existingPsychologue);
                return userMapper.toProfileResponse(saved);
            } else {
                throw new ConflictException("Un profil métier existe déjà pour cet utilisateur");
            }
        }

        Psychologue psychologue = new Psychologue();
        psychologue.setId(authUser.getFirebaseUid());
        psychologue.setEmail(authUser.getEmail());
        psychologue.setNom(request.getNom());
        psychologue.setPrenom(request.getPrenom());
        psychologue.setTelephone(validerTelephone(request.getTelephone(), authUser.getFirebaseUid(), false));
        psychologue.setBiographie(request.getBiographie());
        psychologue.setVille(request.getVille());
        psychologue.setAdresse(request.getAdresse());
        psychologue.setModeConsultation(request.getModeConsultation());
        psychologue.setNumeroAgrement(validerNumeroAgrement(request.getNumeroAgrement(), authUser.getFirebaseUid()));
        psychologue.setStatutValidation("PENDING");
        psychologue.setDateInscription(LocalDate.now());

        if (request.getSpecialiteIds() != null && !request.getSpecialiteIds().isEmpty()) {
            List<Specialite> specialites = specialiteRepository.findAllById(request.getSpecialiteIds());
            for (Specialite s : specialites) {
                String resolved = s.resolveTypeProfessionnel();
                if ("AVOCAT".equalsIgnoreCase(resolved)) {
                    throw new com.psyavocat.exception.BadRequestException("La spécialité '" + s.getNom() + "' n'est pas applicable à un psychologue.");
                }
            }
            psychologue.setSpecialites(specialites);
        }

        Psychologue saved = psychologueRepository.save(psychologue);

        // Notification immédiate aux administrateurs
        List<Administrateur> admins = administrateurRepository.findAll();
        for (Administrateur admin : admins) {
            notificationService.sendNotification(
                admin.getId(),
                "INSCRIPTION_PRO",
                "Nouvelle demande d'inscription : " + saved.getPrenom() + " " + saved.getNom() + " (Psychologue). Dossier en attente de vérification.",
                null
            );
        }

        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        if (utilisateur instanceof Professionnel pro) {
            // Règle de conformité : Les professionnels ne peuvent modifier QUE leur numéro, adresse et email
            if (request.getTelephone() != null) {
                utilisateur.setTelephone(validerTelephone(request.getTelephone(), uid, false));
            }
            if (request.getAdresse() != null) {
                pro.setAdresse(request.getAdresse().trim());
            }
            if (request.getVille() != null) {
                pro.setVille(request.getVille().trim());
            }
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                String newEmail = request.getEmail().trim().toLowerCase();
                if (!newEmail.equalsIgnoreCase(utilisateur.getEmail())) {
                    if (utilisateurRepository.existsByEmail(newEmail)) {
                        throw new ConflictException("Cette adresse e-mail est déjà associée à un autre compte.");
                    }
                    utilisateur.setEmail(newEmail);
                }
            }
            // Nom, prénom, numéro de barreau / agrément sont certifiés par l'administration et verrouillés
        } else {
            // Client (profil unifié, patient ou justiciable)
            if (request.getNom() != null) {
                utilisateur.setNom(validerTexteObligatoire(request.getNom(), "Le nom"));
            }
            if (request.getPrenom() != null) {
                utilisateur.setPrenom(validerTexteObligatoire(request.getPrenom(), "Le prénom"));
            }
            if (request.getTelephone() != null) {
                utilisateur.setTelephone(validerTelephone(request.getTelephone(), uid, false));
            }
            // L'email est l'identifiant de connexion Firebase : le modifier ici
            // désynchroniserait MySQL et Firebase. Il n'est donc pas modifiable.
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()
                    && !request.getEmail().trim().equalsIgnoreCase(utilisateur.getEmail())) {
                throw new com.psyavocat.exception.BadRequestException(
                        "L'adresse e-mail est votre identifiant de connexion et ne peut pas être modifiée ici.");
            }
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse uploadPhotoProfil(org.springframework.web.multipart.MultipartFile file) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        if (utilisateur instanceof Professionnel pro) {
            supprimerImageSiPresente(pro.getPhotoObjectKey());
            com.psyavocat.storage.model.StoredImage stored = imageStorageService.uploadImage(file, "avatars", pro.getId());
            pro.setPhotoUrl(stored.getUrl());
            pro.setPhotoObjectKey(cleObjet(stored));
        } else if (ClientAccounts.isClient(utilisateur)) {
            PhotoProfil photo = ClientAccounts.photoOf(utilisateur);
            supprimerImageSiPresente(photo.getObjectKey());
            com.psyavocat.storage.model.StoredImage stored = imageStorageService.uploadImage(file, "avatars", utilisateur.getId());
            photo.setObjectKey(cleObjet(stored));
        } else {
            throw new com.psyavocat.exception.BadRequestException("Ce type de compte ne peut pas enregistrer de photo de profil.");
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse supprimerPhotoProfil() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        if (utilisateur instanceof Professionnel pro) {
            supprimerImageSiPresente(pro.getPhotoObjectKey());
            pro.setPhotoUrl(null);
            pro.setPhotoObjectKey(null);
        } else if (ClientAccounts.isClient(utilisateur)) {
            PhotoProfil photo = ClientAccounts.photoOf(utilisateur);
            supprimerImageSiPresente(photo.getObjectKey());
            photo.setObjectKey(null);
        } else {
            throw new com.psyavocat.exception.BadRequestException("Ce type de compte ne gère pas de photo de profil.");
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return userMapper.toProfileResponse(saved);
    }

    private void supprimerImageSiPresente(String objectKey) {
        if (objectKey != null && !objectKey.isBlank()) {
            imageStorageService.deleteImage(objectKey);
        }
    }

    private String cleObjet(com.psyavocat.storage.model.StoredImage stored) {
        return stored.getObjectKey() != null ? stored.getObjectKey() : stored.getPublicId();
    }

    // ------------------------------------------------------------------
    // Règles de cohérence des données de profil (communes à tous les comptes)
    // ------------------------------------------------------------------

    /**
     * Téléphone normalisé, au bon format et non utilisé par un autre compte.
     * @param obligatoire si {@code false}, un numéro vide est accepté (valeur {@code null})
     */
    private String validerTelephone(String brut, String uid, boolean obligatoire) {
        if (PhoneNumbers.isBlank(brut)) {
            if (obligatoire) {
                throw new com.psyavocat.exception.BadRequestException("Le numéro de téléphone est obligatoire.");
            }
            return null;
        }
        String normalise = PhoneNumbers.normalize(brut);
        if (normalise == null) {
            throw new com.psyavocat.exception.BadRequestException(PhoneNumbers.MESSAGE_INVALIDE);
        }
        boolean dejaUtilise = utilisateurRepository.findAllTelephones().stream()
                .filter(ligne -> !uid.equals(ligne[0]))
                .anyMatch(ligne -> normalise.equals(PhoneNumbers.normalize((String) ligne[1])));
        if (dejaUtilise) {
            throw new ConflictException("Ce numéro de téléphone est déjà associé à un autre compte.");
        }
        return normalise;
    }

    /** Numéro de barreau unique (insensible à la casse) entre tous les avocats. */
    private String validerNumeroBarreau(String brut, String uid) {
        String numero = validerTexteObligatoire(brut, "Le numéro de barreau");
        if (avocatRepository.existsByNumeroBarreauIgnoreCaseAndIdNot(numero, uid)) {
            throw new ConflictException("Ce numéro de barreau est déjà utilisé par un autre avocat.");
        }
        return numero;
    }

    /** Numéro d'agrément unique (insensible à la casse) entre tous les psychologues. */
    private String validerNumeroAgrement(String brut, String uid) {
        String numero = validerTexteObligatoire(brut, "Le numéro d'agrément");
        if (psychologueRepository.existsByNumeroAgrementIgnoreCaseAndIdNot(numero, uid)) {
            throw new ConflictException("Ce numéro d'agrément est déjà utilisé par un autre psychologue.");
        }
        return numero;
    }

    private String validerTexteObligatoire(String brut, String libelle) {
        if (brut == null || brut.isBlank()) {
            throw new com.psyavocat.exception.BadRequestException(libelle + " est obligatoire.");
        }
        return brut.trim();
    }

    private AuthenticatedUser getAuthenticatedUserOrThrow() {
        return authenticationContext.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Aucun utilisateur authentifié"));
    }

    private void checkIfProfileAlreadyExists(String uid) {
        if (utilisateurRepository.existsById(uid)) {
            throw new ConflictException("Un profil métier existe déjà pour cet utilisateur");
        }
    }
}
