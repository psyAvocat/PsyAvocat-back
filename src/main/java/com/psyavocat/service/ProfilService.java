package com.psyavocat.service;

import com.psyavocat.dto.profil.*;
import com.psyavocat.entity.*;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.UserMapper;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticatedUser;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProfilService {

    private final AuthenticationContext authenticationContext;
    private final UtilisateurRepository utilisateurRepository;
    private final PatientRepository patientRepository;
    private final JusticiableRepository justiciableRepository;
    private final AvocatRepository avocatRepository;
    private final PsychologueRepository psychologueRepository;
    private final SpecialiteRepository specialiteRepository;
    private final UserMapper userMapper;

    public ProfilService(
            AuthenticationContext authenticationContext,
            UtilisateurRepository utilisateurRepository,
            PatientRepository patientRepository,
            JusticiableRepository justiciableRepository,
            AvocatRepository avocatRepository,
            PsychologueRepository psychologueRepository,
            SpecialiteRepository specialiteRepository,
            UserMapper userMapper
    ) {
        this.authenticationContext = authenticationContext;
        this.utilisateurRepository = utilisateurRepository;
        this.patientRepository = patientRepository;
        this.justiciableRepository = justiciableRepository;
        this.avocatRepository = avocatRepository;
        this.psychologueRepository = psychologueRepository;
        this.specialiteRepository = specialiteRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable pour l'identifiant : " + uid));
        return userMapper.toProfileResponse(utilisateur);
    }

    public UserProfileResponse createPatientProfile(CreatePatientRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        checkIfProfileAlreadyExists(authUser.getFirebaseUid());

        Patient patient = new Patient();
        patient.setId(authUser.getFirebaseUid());
        patient.setEmail(authUser.getEmail());
        patient.setNom(request.getNom());
        patient.setPrenom(request.getPrenom());
        patient.setTelephone(request.getTelephone());
        patient.setDateInscription(LocalDate.now());

        Patient saved = patientRepository.save(patient);
        return userMapper.toProfileResponse(saved);
    }

    public UserProfileResponse createJusticiableProfile(CreateJusticiableRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        checkIfProfileAlreadyExists(authUser.getFirebaseUid());

        Justiciable justiciable = new Justiciable();
        justiciable.setId(authUser.getFirebaseUid());
        justiciable.setEmail(authUser.getEmail());
        justiciable.setNom(request.getNom());
        justiciable.setPrenom(request.getPrenom());
        justiciable.setTelephone(request.getTelephone());
        justiciable.setDateInscription(LocalDate.now());

        Justiciable saved = justiciableRepository.save(justiciable);
        return userMapper.toProfileResponse(saved);
    }

    public UserProfileResponse createAvocatProfile(CreateAvocatRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        checkIfProfileAlreadyExists(authUser.getFirebaseUid());

        Avocat avocat = new Avocat();
        avocat.setId(authUser.getFirebaseUid());
        avocat.setEmail(authUser.getEmail());
        avocat.setNom(request.getNom());
        avocat.setPrenom(request.getPrenom());
        avocat.setTelephone(request.getTelephone());
        avocat.setBiographie(request.getBiographie());
        avocat.setVille(request.getVille());
        avocat.setAdresse(request.getAdresse());
        avocat.setModeConsultation(request.getModeConsultation());
        avocat.setNumeroBarreau(request.getNumeroBarreau());
        avocat.setStatutValidation("PENDING");
        avocat.setDateInscription(LocalDate.now());

        if (request.getSpecialiteIds() != null && !request.getSpecialiteIds().isEmpty()) {
            List<Specialite> specialites = specialiteRepository.findAllById(request.getSpecialiteIds());
            avocat.setSpecialites(specialites);
        }

        Avocat saved = avocatRepository.save(avocat);
        return userMapper.toProfileResponse(saved);
    }

    public UserProfileResponse createPsychologueProfile(CreatePsychologueRequest request) {
        AuthenticatedUser authUser = getAuthenticatedUserOrThrow();
        checkIfProfileAlreadyExists(authUser.getFirebaseUid());

        Psychologue psychologue = new Psychologue();
        psychologue.setId(authUser.getFirebaseUid());
        psychologue.setEmail(authUser.getEmail());
        psychologue.setNom(request.getNom());
        psychologue.setPrenom(request.getPrenom());
        psychologue.setTelephone(request.getTelephone());
        psychologue.setBiographie(request.getBiographie());
        psychologue.setVille(request.getVille());
        psychologue.setAdresse(request.getAdresse());
        psychologue.setModeConsultation(request.getModeConsultation());
        psychologue.setNumeroAgrement(request.getNumeroAgrement());
        psychologue.setStatutValidation("PENDING");
        psychologue.setDateInscription(LocalDate.now());

        if (request.getSpecialiteIds() != null && !request.getSpecialiteIds().isEmpty()) {
            List<Specialite> specialites = specialiteRepository.findAllById(request.getSpecialiteIds());
            psychologue.setSpecialites(specialites);
        }

        Psychologue saved = psychologueRepository.save(psychologue);
        return userMapper.toProfileResponse(saved);
    }

    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        if (request.getNom() != null) utilisateur.setNom(request.getNom());
        if (request.getPrenom() != null) utilisateur.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) utilisateur.setTelephone(request.getTelephone());

        if (utilisateur instanceof Professionnel pro) {
            if (request.getBiographie() != null) pro.setBiographie(request.getBiographie());
            if (request.getVille() != null) pro.setVille(request.getVille());
            if (request.getAdresse() != null) pro.setAdresse(request.getAdresse());
            if (request.getModeConsultation() != null) pro.setModeConsultation(request.getModeConsultation());

            if (request.getSpecialiteIds() != null) {
                List<Specialite> specialites = specialiteRepository.findAllById(request.getSpecialiteIds());
                pro.setSpecialites(specialites);
            }
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return userMapper.toProfileResponse(saved);
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
