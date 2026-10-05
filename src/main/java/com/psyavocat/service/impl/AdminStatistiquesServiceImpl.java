package com.psyavocat.service.impl;

import com.psyavocat.dto.admin.DashboardStatsDTO;
import com.psyavocat.dto.admin.PointMensuelDTO;
import com.psyavocat.dto.admin.RepartitionDTO;
import com.psyavocat.dto.admin.StatistiquesDetailleesDTO;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.repository.*;
import com.psyavocat.service.AdminStatistiquesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminStatistiquesServiceImpl implements AdminStatistiquesService {

    static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    static final String STATUT_VALIDE = "VALIDE";
    static final String STATUT_REJETE = "REJETE";
    static final String STATUT_SUSPENDU = "SUSPENDU";
    static final String STATUT_NON_RENSEIGNE = "NON_RENSEIGNE";
    static final String STATUT_AUTRE = "AUTRE";

    /** Statut utilisé par {@code AdminServiceImpl} pour les signalements non traités. */
    private static final String SIGNALEMENT_EN_ATTENTE = "EN_ATTENTE";

    private final UtilisateurRepository utilisateurRepository;
    private final ProfessionnelRepository professionnelRepository;
    private final RendezVousRepository rendezVousRepository;
    private final SignalementRepository signalementRepository;
    private final PatientRepository patientRepository;
    private final JusticiableRepository justiciableRepository;
    private final PsychologueRepository psychologueRepository;
    private final AvocatRepository avocatRepository;
    private final AdministrateurRepository administrateurRepository;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminStatistiquesServiceImpl(UtilisateurRepository utilisateurRepository,
                                        ProfessionnelRepository professionnelRepository,
                                        RendezVousRepository rendezVousRepository,
                                        SignalementRepository signalementRepository,
                                        PatientRepository patientRepository,
                                        JusticiableRepository justiciableRepository,
                                        PsychologueRepository psychologueRepository,
                                        AvocatRepository avocatRepository,
                                        AdministrateurRepository administrateurRepository) {
        this(utilisateurRepository, professionnelRepository, rendezVousRepository, signalementRepository,
                patientRepository, justiciableRepository, psychologueRepository, avocatRepository,
                administrateurRepository, Clock.systemDefaultZone());
    }

    /** Constructeur permettant d'injecter une horloge fixe dans les tests. */
    AdminStatistiquesServiceImpl(UtilisateurRepository utilisateurRepository,
                                 ProfessionnelRepository professionnelRepository,
                                 RendezVousRepository rendezVousRepository,
                                 SignalementRepository signalementRepository,
                                 PatientRepository patientRepository,
                                 JusticiableRepository justiciableRepository,
                                 PsychologueRepository psychologueRepository,
                                 AvocatRepository avocatRepository,
                                 AdministrateurRepository administrateurRepository,
                                 Clock clock) {
        this.utilisateurRepository = utilisateurRepository;
        this.professionnelRepository = professionnelRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.signalementRepository = signalementRepository;
        this.patientRepository = patientRepository;
        this.justiciableRepository = justiciableRepository;
        this.psychologueRepository = psychologueRepository;
        this.avocatRepository = avocatRepository;
        this.administrateurRepository = administrateurRepository;
        this.clock = clock;
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        Map<String, Long> statutsPros = normaliserStatutsProfessionnels(professionnelRepository.compterParStatutValidation());
        YearMonth moisCourant = YearMonth.now(clock);

        long rdvMoisCourant = rendezVousRepository.countByDateHeureGreaterThanEqualAndDateHeureLessThan(
                moisCourant.atDay(1).atStartOfDay(),
                moisCourant.plusMonths(1).atDay(1).atStartOfDay());

        long totalClients = patientRepository.count() + justiciableRepository.count();

        return DashboardStatsDTO.builder()
                .totalUtilisateurs(totalClients)
                .totalProfessionnels(professionnelRepository.count())
                .professionnelsEnAttente(statutsPros.getOrDefault(STATUT_EN_ATTENTE, 0L))
                .professionnelsValides(statutsPros.getOrDefault(STATUT_VALIDE, 0L))
                .rendezVousMoisCourant(rdvMoisCourant)
                .totalRendezVous(rendezVousRepository.count())
                .signalementsEnAttente(signalementRepository.countByStatut(SIGNALEMENT_EN_ATTENTE))
                .build();
    }


    @Override
    public StatistiquesDetailleesDTO getStatistiquesDetaillees(int periodeMois) {
        if (periodeMois < PERIODE_MIN_MOIS || periodeMois > PERIODE_MAX_MOIS) {
            throw new BadRequestException("La période doit être comprise entre "
                    + PERIODE_MIN_MOIS + " et " + PERIODE_MAX_MOIS + " mois.");
        }

        YearMonth moisFin = YearMonth.now(clock);
        YearMonth moisDebut = moisFin.minusMonths(periodeMois - 1L);
        LocalDate debut = moisDebut.atDay(1);
        LocalDate fin = moisFin.atEndOfMonth();

        List<PointMensuelDTO> inscriptions = completerSerie(moisDebut, moisFin,
                utilisateurRepository.compterInscriptionsParMois(debut, fin));
        List<PointMensuelDTO> rendezVous = completerSerie(moisDebut, moisFin,
                rendezVousRepository.compterRendezVousParMois(debut.atStartOfDay(), fin.plusDays(1).atStartOfDay()));

        return StatistiquesDetailleesDTO.builder()
                .periodeMois(periodeMois)
                .debut(debut)
                .fin(fin)
                .inscriptionsParMois(inscriptions)
                .rendezVousParMois(rendezVous)
                .utilisateursParProfil(repartitionProfils())
                .professionnelsParStatut(versRepartition(
                        normaliserStatutsProfessionnels(professionnelRepository.compterParStatutValidation())))
                .rendezVousParStatut(versRepartition(
                        normaliserStatutsBruts(rendezVousRepository.compterParStatut())))
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Produit un point par mois (0 si absent en base) pour garantir un axe temporel continu. */
    static List<PointMensuelDTO> completerSerie(YearMonth debut, YearMonth fin, List<Object[]> lignes) {
        Map<YearMonth, Long> valeurs = new HashMap<>();
        for (Object[] ligne : lignes) {
            if (ligne == null || ligne.length < 3 || ligne[0] == null || ligne[1] == null) continue;
            YearMonth ym = YearMonth.of(((Number) ligne[0]).intValue(), ((Number) ligne[1]).intValue());
            valeurs.merge(ym, ((Number) ligne[2]).longValue(), Long::sum);
        }
        List<PointMensuelDTO> serie = new ArrayList<>();
        for (YearMonth ym = debut; !ym.isAfter(fin); ym = ym.plusMonths(1)) {
            serie.add(PointMensuelDTO.builder().mois(ym.toString()).valeur(valeurs.getOrDefault(ym, 0L)).build());
        }
        return serie;
    }

    /**
     * Les statuts de validation historiques sont hétérogènes (PENDING / EN_ATTENTE, APPROVED / VALIDE, ...).
     * Ils sont regroupés ici en catégories stables pour l'affichage.
     */
    static Map<String, Long> normaliserStatutsProfessionnels(List<Object[]> lignes) {
        Map<String, Long> resultat = new LinkedHashMap<>();
        for (Object[] ligne : lignes) {
            String brut = ligne[0] == null ? "" : ligne[0].toString().trim().toUpperCase(Locale.ROOT);
            String cle = switch (brut) {
                case "PENDING", "EN_ATTENTE" -> STATUT_EN_ATTENTE;
                case "APPROVED", "VALIDE", "VALIDATED" -> STATUT_VALIDE;
                case "REJECTED", "REJETE" -> STATUT_REJETE;
                case "SUSPENDED", "SUSPENDU" -> STATUT_SUSPENDU;
                case "" -> STATUT_NON_RENSEIGNE;
                default -> STATUT_AUTRE;
            };
            resultat.merge(cle, ((Number) ligne[1]).longValue(), Long::sum);
        }
        return resultat;
    }

    static Map<String, Long> normaliserStatutsBruts(List<Object[]> lignes) {
        Map<String, Long> resultat = new LinkedHashMap<>();
        for (Object[] ligne : lignes) {
            String cle = ligne[0] == null || ligne[0].toString().isBlank()
                    ? STATUT_NON_RENSEIGNE
                    : ligne[0].toString().trim().toUpperCase(Locale.ROOT);
            resultat.merge(cle, ((Number) ligne[1]).longValue(), Long::sum);
        }
        return resultat;
    }

    private List<RepartitionDTO> repartitionProfils() {
        Map<String, Long> profils = new LinkedHashMap<>();
        profils.put("PATIENT", patientRepository.count());
        profils.put("JUSTICIABLE", justiciableRepository.count());
        profils.put("PSYCHOLOGUE", psychologueRepository.count());
        profils.put("AVOCAT", avocatRepository.count());
        // L'Administrateur est formellement exclu des statistiques utilisateurs (Règle 27)
        return versRepartition(profils);
    }


    /** Tri décroissant ; les catégories à 0 sont omises (pas de part vide dans les graphiques). */
    private static List<RepartitionDTO> versRepartition(Map<String, Long> valeurs) {
        return valeurs.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> RepartitionDTO.builder().cle(e.getKey()).valeur(e.getValue()).build())
                .toList();
    }
}
