package com.psyavocat.service.impl;

import com.psyavocat.dto.referentiel.TarifProfessionnelDTO;
import com.psyavocat.dto.referentiel.TarifRequest;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.TarifProfessionnel;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.TarifProfessionnelRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.support.AccesProfessionnel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarifServiceImplTest {

    @Mock private TarifProfessionnelRepository tarifRepository;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RealtimeGateway realtimeGateway;

    private TarifServiceImpl service;
    private Psychologue psy;

    @BeforeEach
    void setUp() {
        service = new TarifServiceImpl(tarifRepository, new AccesProfessionnel(authenticationContext, utilisateurRepository), realtimeGateway);
        psy = new Psychologue();
        psy.setId("psy-1");
        psy.setStatutValidation("APPROVED");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("psy-1");
        when(utilisateurRepository.findById("psy-1")).thenReturn(Optional.of(psy));
    }

    @Test
    @DisplayName("Création : montant saisi enregistré tel quel, devise FCFA, publié par défaut")
    void creation_enregistreLeMontantSaisi() {
        when(tarifRepository.save(any(TarifProfessionnel.class))).thenAnswer(i -> i.getArgument(0));

        TarifProfessionnelDTO dto = service.creer(TarifRequest.builder()
                .titre("Séance individuelle").montant(new BigDecimal("25000")).build());

        assertThat(dto.getMontant()).isEqualByComparingTo("25000");
        assertThat(dto.getDevise()).isEqualTo("XOF");
        assertThat(dto.getActif()).isTrue();
        verify(realtimeGateway).diffuser(eq("PROFESSIONNEL_MIS_A_JOUR"), any());
    }

    @Test
    @DisplayName("Un professionnel ne peut pas modifier le tarif d'un autre")
    void modification_tarifDAutrui_refusee() {
        Avocat autre = new Avocat();
        autre.setId("avo-9");
        TarifProfessionnel tarif = new TarifProfessionnel();
        tarif.setId("t-9");
        tarif.setProfessionnel(autre);
        when(tarifRepository.findById("t-9")).thenReturn(Optional.of(tarif));

        assertThatThrownBy(() -> service.modifier("t-9", TarifRequest.builder()
                .titre("x").montant(BigDecimal.TEN).build())).isInstanceOf(ForbiddenException.class);
        verify(tarifRepository, never()).save(any());
    }

    @Test
    @DisplayName("Compte suspendu : création de tarif refusée")
    void creation_compteSuspendu_refusee() {
        psy.setStatutValidation("SUSPENDED");

        assertThatThrownBy(() -> service.creer(TarifRequest.builder()
                .titre("Séance").montant(BigDecimal.TEN).build())).isInstanceOf(ForbiddenException.class);
    }
}
