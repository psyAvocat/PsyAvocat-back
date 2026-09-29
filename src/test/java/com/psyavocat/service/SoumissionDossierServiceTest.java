package com.psyavocat.service;

import com.psyavocat.dto.dossier.RepondreSoumissionRequest;
import com.psyavocat.dto.dossier.SoumissionDossierRequest;
import com.psyavocat.dto.dossier.SoumissionDossierResponseDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Dossier;
import com.psyavocat.entity.Justiciable;
import com.psyavocat.entity.SoumissionDossier;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.repository.AvocatRepository;
import com.psyavocat.repository.DossierRepository;
import com.psyavocat.repository.SoumissionDossierRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.impl.SoumissionDossierServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoumissionDossierServiceTest {

    @Mock
    private DossierRepository dossierRepository;

    @Mock
    private SoumissionDossierRepository soumissionDossierRepository;

    @Mock
    private AvocatRepository avocatRepository;

    @Mock
    private AuthenticationContext authenticationContext;

    private SoumissionDossierServiceImpl soumissionDossierService;

    private Justiciable justiciable;
    private Dossier dossier;
    private Avocat avocatA;
    private Avocat avocatB;

    @BeforeEach
    void setUp() {
        soumissionDossierService = new SoumissionDossierServiceImpl(
                dossierRepository,
                soumissionDossierRepository,
                avocatRepository,
                authenticationContext
        );

        justiciable = new Justiciable();
        justiciable.setId("justiciable-uid-1");
        justiciable.setNom("Martin");
        justiciable.setPrenom("Julie");

        dossier = new Dossier();
        dossier.setId("dossier-1");
        dossier.setTitre("Affaire prud'homale");
        dossier.setJusticiable(justiciable);
        dossier.setSoumissions(new ArrayList<>());

        avocatA = new Avocat();
        avocatA.setId("avocat-uid-a");
        avocatA.setNom("Dupont");
        avocatA.setStatutValidation("APPROVED");

        avocatB = new Avocat();
        avocatB.setId("avocat-uid-b");
        avocatB.setNom("Durand");
        avocatB.setStatutValidation("APPROVED");
    }

    @Test
    @DisplayName("Soumission d'un dossier à un avocat : statut initial EN_ATTENTE")
    void testSoumettreDossier_Succes() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(justiciable.getId());
        when(dossierRepository.findById(dossier.getId())).thenReturn(Optional.of(dossier));
        when(avocatRepository.findById(avocatA.getId())).thenReturn(Optional.of(avocatA));

        when(soumissionDossierRepository.save(any(SoumissionDossier.class))).thenAnswer(invocation -> {
            SoumissionDossier s = invocation.getArgument(0);
            s.setId("soum-1");
            return s;
        });

        SoumissionDossierRequest request = new SoumissionDossierRequest();
        request.setAvocatId(avocatA.getId());

        SoumissionDossierResponseDTO response = soumissionDossierService.soumettreDossier(dossier.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getStatut()).isEqualTo("EN_ATTENTE");
        assertThat(response.getAvocatId()).isEqualTo(avocatA.getId());
        assertThat(response.getDossierId()).isEqualTo(dossier.getId());

        verify(soumissionDossierRepository).save(any(SoumissionDossier.class));
    }

    @Test
    @DisplayName("Refus de double soumission d'un même dossier au même avocat")
    void testSoumettreDossier_DoubleSoumission_Conflict() {
        SoumissionDossier existante = new SoumissionDossier();
        existante.setAvocat(avocatA);
        dossier.getSoumissions().add(existante);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(justiciable.getId());
        when(dossierRepository.findById(dossier.getId())).thenReturn(Optional.of(dossier));
        when(avocatRepository.findById(avocatA.getId())).thenReturn(Optional.of(avocatA));

        SoumissionDossierRequest request = new SoumissionDossierRequest();
        request.setAvocatId(avocatA.getId());

        assertThatThrownBy(() -> soumissionDossierService.soumettreDossier(dossier.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà été soumis à cet avocat");

        verify(soumissionDossierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Réponse d'un avocat A (ACCEPTEE avec tarif) indépendante de la soumission de l'avocat B")
    void testRepondreSoumission_AvocatAccepte_Succes() {
        SoumissionDossier soumissionA = new SoumissionDossier();
        soumissionA.setId("soum-a");
        soumissionA.setDossier(dossier);
        soumissionA.setAvocat(avocatA);
        soumissionA.setStatut("EN_ATTENTE");

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(avocatA.getId());
        when(soumissionDossierRepository.findById(soumissionA.getId())).thenReturn(Optional.of(soumissionA));
        when(soumissionDossierRepository.save(any(SoumissionDossier.class))).thenAnswer(i -> i.getArgument(0));

        RepondreSoumissionRequest request = new RepondreSoumissionRequest();
        request.setStatut("ACCEPTEE");
        request.setTarifPropose(new BigDecimal("300.00"));
        request.setReponse("Je prends en charge votre dossier");

        SoumissionDossierResponseDTO response = soumissionDossierService.repondreSoumission(soumissionA.getId(), request);

        assertThat(response.getStatut()).isEqualTo("ACCEPTEE");
        assertThat(response.getTarifPropose()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.getDateReponse()).isNotNull();
    }

    @Test
    @DisplayName("Réponse d'un avocat B (REFUSEE) sans impacter l'acceptation de l'avocat A")
    void testRepondreSoumission_AvocatRefuse_Succes() {
        SoumissionDossier soumissionB = new SoumissionDossier();
        soumissionB.setId("soum-b");
        soumissionB.setDossier(dossier);
        soumissionB.setAvocat(avocatB);
        soumissionB.setStatut("EN_ATTENTE");

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(avocatB.getId());
        when(soumissionDossierRepository.findById(soumissionB.getId())).thenReturn(Optional.of(soumissionB));
        when(soumissionDossierRepository.save(any(SoumissionDossier.class))).thenAnswer(i -> i.getArgument(0));

        RepondreSoumissionRequest request = new RepondreSoumissionRequest();
        request.setStatut("REFUSEE");
        request.setReponse("Indisponible pour ce type de contentieux");

        SoumissionDossierResponseDTO response = soumissionDossierService.repondreSoumission(soumissionB.getId(), request);

        assertThat(response.getStatut()).isEqualTo("REFUSEE");
    }

    @Test
    @DisplayName("Acceptation refusée si aucun tarif proposé n'est renseigné")
    void testRepondreSoumission_AcceptationSansTarif_Rejet() {
        SoumissionDossier soumissionA = new SoumissionDossier();
        soumissionA.setId("soum-a");
        soumissionA.setAvocat(avocatA);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(avocatA.getId());
        when(soumissionDossierRepository.findById(soumissionA.getId())).thenReturn(Optional.of(soumissionA));

        RepondreSoumissionRequest request = new RepondreSoumissionRequest();
        request.setStatut("ACCEPTEE");
        request.setTarifPropose(null); // Pas de tarif

        assertThatThrownBy(() -> soumissionDossierService.repondreSoumission(soumissionA.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tarif proposé supérieur à 0 est requis");
    }

    @Test
    @DisplayName("Refus si un avocat tente de répondre à la soumission d'un confrère")
    void testRepondreSoumission_ParUnAutreAvocat_Forbidden() {
        SoumissionDossier soumissionA = new SoumissionDossier();
        soumissionA.setId("soum-a");
        soumissionA.setAvocat(avocatA);

        // L'utilisateur connecté est l'avocat B, mais la soumission est pour l'avocat A
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(avocatB.getId());
        when(soumissionDossierRepository.findById(soumissionA.getId())).thenReturn(Optional.of(soumissionA));

        RepondreSoumissionRequest request = new RepondreSoumissionRequest();
        request.setStatut("ACCEPTEE");
        request.setTarifPropose(new BigDecimal("200.00"));

        assertThatThrownBy(() -> soumissionDossierService.repondreSoumission(soumissionA.getId(), request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("pas l'avocat destinataire");
    }

    @Test
    @DisplayName("Réponse rejetée si le dossier a déjà été pris en charge par un autre avocat (Solution 2)")
    void testRepondreSoumission_DossierDejaPrisEnCharge_BadRequest() {
        Dossier dossierPris = new Dossier();
        dossierPris.setId("dossier-pris-1");
        dossierPris.setStatut("PRIS_EN_CHARGE");

        SoumissionDossier soumission = new SoumissionDossier();
        soumission.setId("soum-1");
        soumission.setAvocat(avocatA);
        soumission.setDossier(dossierPris);
        soumission.setStatut("EN_ATTENTE");

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(avocatA.getId());
        when(soumissionDossierRepository.findById(soumission.getId())).thenReturn(Optional.of(soumission));

        RepondreSoumissionRequest request = new RepondreSoumissionRequest();
        request.setStatut("ACCEPTEE");
        request.setTarifPropose(new BigDecimal("250.00"));

        assertThatThrownBy(() -> soumissionDossierService.repondreSoumission(soumission.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà été confié et pris en charge");
    }

    @Test
    @DisplayName("La liste par défaut des soumissions avocat exclut les dossiers devenus CADUQUE")
    void testGetSoumissionsPourAvocat_ExclutCaduquesParDefaut() {
        SoumissionDossier sActive = new SoumissionDossier();
        sActive.setId("s-active");
        sActive.setStatut("EN_ATTENTE");

        SoumissionDossier sCaduque = new SoumissionDossier();
        sCaduque.setId("s-caduque");
        sCaduque.setStatut("CADUQUE");

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(avocatA.getId());
        when(soumissionDossierRepository.findByAvocatIdOrderByDateSoumissionDesc(avocatA.getId()))
                .thenReturn(List.of(sActive, sCaduque));

        List<SoumissionDossierResponseDTO> result = soumissionDossierService.getSoumissionsPourAvocat(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("s-active");
    }
}
