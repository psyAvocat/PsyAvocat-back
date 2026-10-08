package com.psyavocat.service.impl;

import com.psyavocat.dto.admin.DashboardStatsDTO;
import com.psyavocat.dto.admin.PointMensuelDTO;
import com.psyavocat.dto.admin.StatistiquesDetailleesDTO;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatistiquesServiceImplTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ProfessionnelRepository professionnelRepository;
    @Mock private RendezVousRepository rendezVousRepository;
    @Mock private SignalementRepository signalementRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private JusticiableRepository justiciableRepository;
    @Mock private PsychologueRepository psychologueRepository;
    @Mock private AvocatRepository avocatRepository;
    @Mock private AdministrateurRepository administrateurRepository;
    @Mock private ClientRepository clientRepository;

    private AdminStatistiquesServiceImpl statsService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-10-15T10:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        statsService = new AdminStatistiquesServiceImpl(
                utilisateurRepository,
                professionnelRepository,
                rendezVousRepository,
                signalementRepository,
                patientRepository,
                justiciableRepository,
                psychologueRepository,
                avocatRepository,
                administrateurRepository,
                clientRepository,
                fixedClock
        );
    }

    @Test
    @DisplayName("getDashboardStats - Calcule les métriques réelles sans données fictives")
    void testGetDashboardStats() {
        // « Utilisateurs » = clients uniquement (admin et professionnels exclus).
        when(clientRepository.count()).thenReturn(100L);
        when(patientRepository.count()).thenReturn(30L);
        when(justiciableRepository.count()).thenReturn(20L);
        when(professionnelRepository.count()).thenReturn(30L);
        when(professionnelRepository.compterParStatutValidation()).thenReturn(List.of(
                new Object[]{"APPROVED", 20L},
                new Object[]{"EN_ATTENTE", 8L},
                new Object[]{"REJECTED", 2L}
        ));
        when(rendezVousRepository.countByDateHeureGreaterThanEqualAndDateHeureLessThan(any(), any()))
                .thenReturn(45L);
        when(rendezVousRepository.count()).thenReturn(320L);
        when(signalementRepository.countByStatut("EN_ATTENTE")).thenReturn(3L);

        DashboardStatsDTO stats = statsService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(150L, stats.getTotalUtilisateurs());
        assertEquals(30L, stats.getTotalProfessionnels());
        assertEquals(8L, stats.getProfessionnelsEnAttente());
        assertEquals(20L, stats.getProfessionnelsValides());
        assertEquals(45L, stats.getRendezVousMoisCourant());
        assertEquals(320L, stats.getTotalRendezVous());
        assertEquals(3L, stats.getSignalementsEnAttente());
    }

    @Test
    @DisplayName("getStatistiquesDetaillees - Rejette une période hors limites (3 à 24 mois)")
    void testPeriodeInvalide() {
        assertThrows(BadRequestException.class, () -> statsService.getStatistiquesDetaillees(2));
        assertThrows(BadRequestException.class, () -> statsService.getStatistiquesDetaillees(25));
    }

    @Test
    @DisplayName("completerSerie - Complète les mois manquants avec valeur 0 pour un axe continu")
    void testCompleterSerie() {
        YearMonth debut = YearMonth.of(2026, 1);
        YearMonth fin = YearMonth.of(2026, 4);

        // Données en base seulement pour Janvier (5) et Mars (12)
        List<Object[]> raw = List.of(
                new Object[]{2026, 1, 5L},
                new Object[]{2026, 3, 12L}
        );

        List<PointMensuelDTO> points = AdminStatistiquesServiceImpl.completerSerie(debut, fin, raw);

        assertEquals(4, points.size());
        assertEquals("2026-01", points.get(0).getMois());
        assertEquals(5L, points.get(0).getValeur());

        assertEquals("2026-02", points.get(1).getMois());
        assertEquals(0L, points.get(1).getValeur()); // Mois sans activité = 0 réel

        assertEquals("2026-03", points.get(2).getMois());
        assertEquals(12L, points.get(2).getValeur());

        assertEquals("2026-04", points.get(3).getMois());
        assertEquals(0L, points.get(3).getValeur());
    }
}
