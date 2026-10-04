package com.psyavocat.service.impl;

import com.psyavocat.dto.admin.PointMensuelDTO;
import com.psyavocat.dto.admin.RepartitionDTO;
import com.psyavocat.dto.professionnel.ProfessionnelClientDTO;
import com.psyavocat.dto.professionnel.ProfessionnelStatistiquesDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.FichePatientRepository;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.repository.SoumissionDossierRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.ProfessionnelEspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProfessionnelEspaceServiceImpl implements ProfessionnelEspaceService {

    private final AuthenticationContext authenticationContext;
    private final UtilisateurRepository utilisateurRepository;
    private final RendezVousRepository rendezVousRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final FichePatientRepository fichePatientRepository;

    public ProfessionnelEspaceServiceImpl(
            AuthenticationContext authenticationContext,
            UtilisateurRepository utilisateurRepository,
            RendezVousRepository rendezVousRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            FichePatientRepository fichePatientRepository
    ) {
        this.authenticationContext = authenticationContext;
        this.utilisateurRepository = utilisateurRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.soumissionDossierRepository = soumissionDossierRepository;
        this.fichePatientRepository = fichePatientRepository;
    }

    private Professionnel getAuthenticatedProfessionnel() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur user = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + uid));

        if (!(user instanceof Professionnel pro)) {
            throw new ForbiddenException("Cet espace est réservé aux professionnels (Avocats / Psychologues).");
        }
        return pro;
    }

    @Override
    public List<ProfessionnelClientDTO> getMesClients() {
        Professionnel pro = getAuthenticatedProfessionnel();
        String proId = pro.getId();

        Map<String, ProfessionnelClientDTO> clientMap = new LinkedHashMap<>();

        // 1. Récupération des clients issus des rendez-vous
        List<RendezVous> rdvs = rendezVousRepository.findByProfessionnelIdOrderByDateHeureDesc(proId);
        for (RendezVous rdv : rdvs) {
            Utilisateur patient = rdv.getPatient();
            if (patient == null) continue;

            String patientId = patient.getId();
            ProfessionnelClientDTO dto = clientMap.computeIfAbsent(patientId, id -> ProfessionnelClientDTO.builder()
                    .id(patientId)
                    .nom(patient.getNom())
                    .prenom(patient.getPrenom())
                    .email(patient.getEmail())
                    .telephone(patient.getTelephone())
                    .nombreRendezVous(0)
                    .statut("ACTIF")
                    .dernierMode(rdv.getMode())
                    .dernierRendezVous(rdv.getDateHeure())
                    .build());

            dto.setNombreRendezVous(dto.getNombreRendezVous() + 1);
            if (dto.getDernierRendezVous() == null || (rdv.getDateHeure() != null && rdv.getDateHeure().isAfter(dto.getDernierRendezVous()))) {
                dto.setDernierRendezVous(rdv.getDateHeure());
                dto.setDernierMode(rdv.getMode());
            }
        }

        // 2. Si Avocat : enrichir avec les dossiers soumis
        if (pro instanceof Avocat) {
            List<SoumissionDossier> soumissions = soumissionDossierRepository.findByAvocatIdOrderByDateSoumissionDesc(proId);
            for (SoumissionDossier s : soumissions) {
                if (s.getDossier() == null) continue;
                Justiciable j = s.getDossier().getJusticiable();
                if (j == null) continue;

                ProfessionnelClientDTO dto = clientMap.computeIfAbsent(j.getId(), id -> ProfessionnelClientDTO.builder()
                        .id(j.getId())
                        .nom(j.getNom())
                        .prenom(j.getPrenom())
                        .email(j.getEmail())
                        .telephone(j.getTelephone())
                        .nombreRendezVous(0)
                        .statut("NOUVEAU")
                        .dossiersCount(0)
                        .build());

                dto.setDossiersCount((dto.getDossiersCount() != null ? dto.getDossiersCount() : 0) + 1);
            }
        }

        // 3. Si Psychologue : enrichir avec le décompte de séances
        if (pro instanceof Psychologue) {
            List<FichePatient> fiches = fichePatientRepository.findByPsychologueIdOrderByDateCreationDesc(proId);
            for (FichePatient f : fiches) {
                Patient p = f.getPatient();
                if (p == null) continue;

                ProfessionnelClientDTO dto = clientMap.computeIfAbsent(p.getId(), id -> ProfessionnelClientDTO.builder()
                        .id(p.getId())
                        .nom(p.getNom())
                        .prenom(p.getPrenom())
                        .email(p.getEmail())
                        .telephone(p.getTelephone())
                        .nombreRendezVous(0)
                        .statut("ACTIF")
                        .seancesCount(0)
                        .build());

                int seancesCount = f.getSeances() != null ? f.getSeances().size() : 0;
                dto.setSeancesCount((dto.getSeancesCount() != null ? dto.getSeancesCount() : 0) + seancesCount);
            }
        }

        List<ProfessionnelClientDTO> results = new ArrayList<>(clientMap.values());
        // Trier : clients avec RDV récent d'abord, puis par nom
        results.sort((a, b) -> {
            if (a.getDernierRendezVous() != null && b.getDernierRendezVous() != null) {
                return b.getDernierRendezVous().compareTo(a.getDernierRendezVous());
            }
            if (a.getDernierRendezVous() != null) return -1;
            if (b.getDernierRendezVous() != null) return 1;
            return Objects.toString(a.getNom(), "").compareToIgnoreCase(Objects.toString(b.getNom(), ""));
        });

        return results;
    }

    @Override
    public ProfessionnelStatistiquesDTO getMesStatistiques(int nbMois) {
        Professionnel pro = getAuthenticatedProfessionnel();
        String proId = pro.getId();

        int periode = Math.max(3, Math.min(nbMois, 24));
        YearMonth moisCourant = YearMonth.now();
        YearMonth moisDebut = moisCourant.minusMonths(periode - 1);

        LocalDateTime debut = moisDebut.atDay(1).atStartOfDay();
        LocalDateTime fin = moisCourant.plusMonths(1).atDay(1).atStartOfDay();

        // 1. Évolution des rendez-vous par mois
        Map<YearMonth, Long> rdvParMois = new HashMap<>();
        List<Object[]> rowsRdv = rendezVousRepository.compterRendezVousParMoisPourProfessionnel(proId, debut, fin);
        for (Object[] row : rowsRdv) {
            if (row[0] != null && row[1] != null && row[2] != null) {
                YearMonth ym = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
                rdvParMois.put(ym, ((Number) row[2]).longValue());
            }
        }

        List<PointMensuelDTO> evolutionRendezVous = new ArrayList<>();
        for (int i = 0; i < periode; i++) {
            YearMonth ym = moisDebut.plusMonths(i);
            long val = rdvParMois.getOrDefault(ym, 0L);
            evolutionRendezVous.add(new PointMensuelDTO(ym.toString(), val));
        }

        // 2. Répartition statuts RDV
        List<Object[]> rowsStatuts = rendezVousRepository.compterParStatutPourProfessionnel(proId);
        List<RepartitionDTO> repartitionStatutsRendezVous = construireRepartition(rowsStatuts);

        // 3. Répartition modes consultation
        List<Object[]> rowsModes = rendezVousRepository.compterParModePourProfessionnel(proId);
        List<RepartitionDTO> repartitionModesConsultation = construireRepartition(rowsModes);

        // 4. Si Avocat : répartition statuts dossiers
        List<RepartitionDTO> repartitionStatutsDossiers = Collections.emptyList();
        if (pro instanceof Avocat) {
            List<Object[]> rowsDossiers = soumissionDossierRepository.compterParStatutPourAvocat(proId);
            repartitionStatutsDossiers = construireRepartition(rowsDossiers);
        }

        // 5. Calcul totaux et honoraires réels
        List<RendezVous> allRdv = rendezVousRepository.findByProfessionnelIdOrderByDateHeureDesc(proId);
        long totalRdv = allRdv.size();
        long confirms = allRdv.stream().filter(r -> "CONFIRME".equalsIgnoreCase(r.getStatut())).count();
        long effectues = allRdv.stream().filter(r -> "EFFECTUE".equalsIgnoreCase(r.getStatut()) || "TERMINE".equalsIgnoreCase(r.getStatut())).count();
        long annules = allRdv.stream().filter(r -> "ANNULE".equalsIgnoreCase(r.getStatut())).count();
        long distinctClients = allRdv.stream().map(r -> r.getPatient() != null ? r.getPatient().getId() : null).filter(Objects::nonNull).distinct().count();

        BigDecimal honoraires = allRdv.stream()
                .filter(r -> "CONFIRME".equalsIgnoreCase(r.getStatut()) || "EFFECTUE".equalsIgnoreCase(r.getStatut()) || "TERMINE".equalsIgnoreCase(r.getStatut()))
                .map(r -> r.getMontantTotal() != null ? r.getMontantTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProfessionnelStatistiquesDTO.builder()
                .totalRendezVous(totalRdv)
                .rendezVousConfirmes(confirms)
                .rendezVousEffectues(effectues)
                .rendezVousAnnules(annules)
                .totalClients(distinctClients)
                .honorairesEstimes(honoraires)
                .evolutionRendezVous(evolutionRendezVous)
                .repartitionStatutsRendezVous(repartitionStatutsRendezVous)
                .repartitionModesConsultation(repartitionModesConsultation)
                .repartitionStatutsDossiers(repartitionStatutsDossiers)
                .build();
    }

    private List<RepartitionDTO> construireRepartition(List<Object[]> rows) {
        List<RepartitionDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                String label = row[0].toString();
                long val = ((Number) row[1]).longValue();
                result.add(new RepartitionDTO(label, val));
            }
        }
        return result;
    }
}
