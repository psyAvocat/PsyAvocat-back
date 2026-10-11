package com.psyavocat.service.support;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Component;

/**
 * Point de contrôle unique du statut de vérification des professionnels.
 *
 * - {@code APPROVED} : fonctions métier professionnelles autorisées ;
 * - {@code PENDING} : uniquement le parcours de vérification (profil, justificatifs) ;
 * - {@code REJECTED} / {@code SUSPENDED} : aucune fonction métier.
 *
 * Les refus portent le code {@value #CODE_NON_APPROUVE} pour que les clients
 * affichent l'état du compte plutôt qu'une erreur générique.
 */
@Component
public class AccesProfessionnel {

    public static final String CODE_NON_APPROUVE = "PROFESSIONAL_NOT_APPROVED";

    public static final String STATUT_EN_ATTENTE = "PENDING";
    public static final String STATUT_APPROUVE = "APPROVED";
    public static final String STATUT_REFUSE = "REJECTED";
    public static final String STATUT_SUSPENDU = "SUSPENDED";

    private final AuthenticationContext authenticationContext;
    private final UtilisateurRepository utilisateurRepository;

    public AccesProfessionnel(AuthenticationContext authenticationContext,
                              UtilisateurRepository utilisateurRepository) {
        this.authenticationContext = authenticationContext;
        this.utilisateurRepository = utilisateurRepository;
    }

    /** Professionnel connecté, quel que soit son statut ; 403 pour tout autre compte. */
    public Professionnel professionnelConnecte() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = Entites.reel(utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable")));
        if (!(utilisateur instanceof Professionnel pro)) {
            throw new ForbiddenException("Cet espace est réservé aux professionnels (avocats et psychologues).");
        }
        return pro;
    }

    /** Professionnel connecté et approuvé : seul cas où les fonctions métier sont ouvertes. */
    public Professionnel exigerApprouve() {
        Professionnel pro = professionnelConnecte();
        verifierApprouve(pro);
        return pro;
    }

    /** Psychologue connecté et approuvé. */
    public Psychologue exigerPsychologueApprouve() {
        Professionnel pro = exigerApprouve();
        if (!(pro instanceof Psychologue psychologue)) {
            throw new ForbiddenException("Cette fonctionnalité est réservée aux psychologues.");
        }
        return psychologue;
    }

    /** Avocat connecté et approuvé. */
    public Avocat exigerAvocatApprouve() {
        Professionnel pro = exigerApprouve();
        if (!(pro instanceof Avocat avocat)) {
            throw new ForbiddenException("Cette fonctionnalité est réservée aux avocats.");
        }
        return avocat;
    }

    /**
     * Opérations du parcours de vérification (dépôt / retrait de justificatifs) :
     * ouvertes aux comptes en attente et approuvés, fermées aux comptes refusés ou suspendus.
     */
    public Professionnel exigerParcoursVerificationOuvert() {
        Professionnel pro = professionnelConnecte();
        String statut = statutDe(pro);
        if (!STATUT_EN_ATTENTE.equals(statut) && !STATUT_APPROUVE.equals(statut)) {
            throw new ForbiddenException(messageRefus(statut), CODE_NON_APPROUVE);
        }
        return pro;
    }

    /** Lève 403 si le professionnel n'est pas approuvé et actif. */
    public void verifierApprouve(Professionnel pro) {
        if (!estApprouve(pro)) {
            throw new ForbiddenException(messageRefus(statutDe(pro)), CODE_NON_APPROUVE);
        }
    }

    /** Approuvé et non désactivé : visible, réservable et autorisé à exercer. */
    public static boolean estApprouve(Professionnel pro) {
        return pro != null
                && STATUT_APPROUVE.equals(statutDe(pro))
                && !Boolean.FALSE.equals(pro.getActif());
    }

    private static String statutDe(Professionnel pro) {
        return pro.getStatutValidation() != null ? pro.getStatutValidation().trim().toUpperCase() : STATUT_EN_ATTENTE;
    }

    private static String messageRefus(String statut) {
        return switch (statut) {
            case STATUT_SUSPENDU -> "Votre compte professionnel est suspendu : cette action n'est pas disponible.";
            case STATUT_REFUSE -> "Votre dossier professionnel a été refusé : cette action n'est pas disponible.";
            case STATUT_APPROUVE -> "Votre compte professionnel est désactivé : cette action n'est pas disponible.";
            default -> "Votre compte est en cours de vérification : cette action sera disponible après validation.";
        };
    }
}
