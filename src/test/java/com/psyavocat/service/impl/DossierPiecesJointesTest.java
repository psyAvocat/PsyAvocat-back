package com.psyavocat.service.impl;

import com.psyavocat.dto.dossier.PieceJointeDTO;
import com.psyavocat.entity.Client;
import com.psyavocat.entity.Dossier;
import com.psyavocat.entity.PieceJointe;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.SoumissionDossierService;
import com.psyavocat.service.support.AccesProfessionnel;
import com.psyavocat.storage.model.StoredDocument;
import com.psyavocat.storage.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

/**
 * Pièces jointes des dossiers juridiques : bucket privé, propriétaire uniquement.
 */
@ExtendWith(MockitoExtension.class)
class DossierPiecesJointesTest {

    @Mock private DossierRepository dossierRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private SoumissionDossierService soumissionDossierService;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private SpecialiteRepository specialiteRepository;
    @Mock private DocumentStorageService documentStorageService;

    private DossierServiceImpl service;
    private Dossier dossier;
    private final MockMultipartFile fichier =
            new MockMultipartFile("file", "contrat.pdf", "application/pdf", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        service = new DossierServiceImpl(dossierRepository, utilisateurRepository, soumissionDossierService,
                authenticationContext, new AccesProfessionnel(authenticationContext, utilisateurRepository),
                pieceJointeRepository, specialiteRepository, documentStorageService);
        Client proprietaire = new Client();
        proprietaire.setId("client-1");
        dossier = new Dossier();
        dossier.setId("dossier-1");
        dossier.setStatut("OUVERT");
        dossier.setJusticiable(proprietaire);
        dossier.setPiecesJointes(new ArrayList<>());
    }

    @Test
    @DisplayName("Le propriétaire dépose une pièce : clé générée par le serveur dans le bucket privé")
    void depot_proprietaire_ok() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(dossierRepository.findById("dossier-1")).thenReturn(Optional.of(dossier));
        when(pieceJointeRepository.findByDossierId("dossier-1")).thenReturn(Collections.emptyList());
        when(documentStorageService.uploadDocument(eq(fichier), startsWith("dossiers/dossier-1/")))
                .thenAnswer(i -> StoredDocument.builder().cleObjet(i.getArgument(1)).build());
        when(pieceJointeRepository.save(any(PieceJointe.class))).thenAnswer(i -> i.getArgument(0));

        PieceJointeDTO dto = service.ajouterPieceJointe("dossier-1", fichier);

        ArgumentCaptor<PieceJointe> captor = ArgumentCaptor.forClass(PieceJointe.class);
        verify(pieceJointeRepository).save(captor.capture());
        assertThat(captor.getValue().getCleObjet()).startsWith("dossiers/dossier-1/").endsWith(".pdf");
        assertThat(captor.getValue().getUrl()).startsWith("r2://");
        assertThat(dto.getNom()).isEqualTo("contrat.pdf");
    }

    @Test
    @DisplayName("Un tiers ne peut pas déposer de pièce sur le dossier d'autrui")
    void depot_tiers_refuse() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("autre-client");
        when(dossierRepository.findById("dossier-1")).thenReturn(Optional.of(dossier));

        assertThatThrownBy(() -> service.ajouterPieceJointe("dossier-1", fichier)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(documentStorageService);
    }

    @Test
    @DisplayName("Limite de pièces jointes par dossier respectée")
    void depot_limiteAtteinte() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(dossierRepository.findById("dossier-1")).thenReturn(Optional.of(dossier));
        List<PieceJointe> pleines = new ArrayList<>();
        for (int i = 0; i < DossierServiceImpl.MAX_PIECES_JOINTES; i++) {
            pleines.add(new PieceJointe());
        }
        when(pieceJointeRepository.findByDossierId("dossier-1")).thenReturn(pleines);

        assertThatThrownBy(() -> service.ajouterPieceJointe("dossier-1", fichier)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(documentStorageService);
    }

    @Test
    @DisplayName("Dossier pris en charge : les pièces ne peuvent plus être retirées")
    void suppression_dossierPrisEnCharge_refusee() {
        dossier.setStatut("PRIS_EN_CHARGE");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(dossierRepository.findById("dossier-1")).thenReturn(Optional.of(dossier));

        assertThatThrownBy(() -> service.supprimerPieceJointe("dossier-1", "pj-1")).isInstanceOf(BadRequestException.class);
        verify(documentStorageService, never()).deleteDocument(any());
    }
}
