package com.psyavocat.service;

import com.psyavocat.entity.Paiement;
import com.psyavocat.entity.Patient;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.repository.PaiementRepository;
import com.psyavocat.service.impl.PaiementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock
    private PaiementRepository paiementRepository;

    private PaiementServiceImpl paiementService;

    @BeforeEach
    void setUp() {
        paiementService = new PaiementServiceImpl(paiementRepository);
    }

    @Test
    @DisplayName("Calcul de l'acompte de 20 % pour un rendez-vous (règle métier)")
    void testCalculerAcompteRendezVous() {
        BigDecimal total100 = new BigDecimal("100.00");
        assertThat(paiementService.calculerAcompteRendezVous(total100))
                .isEqualByComparingTo(new BigDecimal("20.00"));

        BigDecimal total150 = new BigDecimal("150.00");
        assertThat(paiementService.calculerAcompteRendezVous(total150))
                .isEqualByComparingTo(new BigDecimal("30.00"));

        BigDecimal total75 = new BigDecimal("75.50");
        assertThat(paiementService.calculerAcompteRendezVous(total75))
                .isEqualByComparingTo(new BigDecimal("15.10"));
    }

    @Test
    @DisplayName("Calcul acompte rejeté si montant nul ou négatif")
    void testCalculerAcompteMontantInvalide() {
        assertThatThrownBy(() -> paiementService.calculerAcompteRendezVous(null))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> paiementService.calculerAcompteRendezVous(new BigDecimal("-10.00")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Traitement de l'acompte simulé d'un rendez-vous avec statut PAYE")
    void testTraiterAcompteRendezVous() {
        Utilisateur user = new Patient();
        user.setId("user-1");

        RendezVous rdv = new RendezVous();
        rdv.setId("rdv-1");

        BigDecimal acompte = new BigDecimal("20.00");

        when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Paiement resultat = paiementService.traiterAcompteRendezVous(user, rdv, acompte);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getMontant()).isEqualByComparingTo(acompte);
        assertThat(resultat.getStatut()).isEqualTo("PAYE");
        assertThat(resultat.getMethode()).isEqualTo("SIMULATION_CARTE");
        assertThat(resultat.getUtilisateur()).isEqualTo(user);
        assertThat(resultat.getRendezVous()).isEqualTo(rdv);
        assertThat(resultat.getDatePaiement()).isNotNull();

        verify(paiementRepository, times(1)).save(any(Paiement.class));
    }
}
