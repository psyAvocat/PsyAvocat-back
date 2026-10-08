package com.psyavocat.service.impl;

import com.psyavocat.dto.justificatif.JustificatifResponseDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.JustificatifProfessionnel;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.repository.JustificatifProfessionnelRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticatedUser;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.storage.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JustificatifStorageIntegrationTest {

    @Mock private JustificatifProfessionnelRepository justificatifRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private DocumentStorageService documentStorageService;

    private JustificatifServiceImpl justificatifService;

    private Avocat pro1;
    private Avocat pro2;
    private JustificatifProfessionnel docPro1;

    @BeforeEach
    void setUp() {
        justificatifService = new JustificatifServiceImpl(
                justificatifRepository,
                utilisateurRepository,
                authenticationContext,
                documentStorageService
        );

        pro1 = new Avocat();
        pro1.setId("pro_owner_123");
        pro1.setEmail("avocat1@psyavocat.fr");

        pro2 = new Avocat();
        pro2.setId("pro_stranger_456");
        pro2.setEmail("avocat2@psyavocat.fr");

        docPro1 = new JustificatifProfessionnel();
        docPro1.setId("doc_1");
        docPro1.setNomFichier("diplome_master.pdf");
        docPro1.setCleObjet("justificatifs/professionnel/pro_owner_123/uuid.pdf");
        docPro1.setCheminStockage("r2://justificatifs/professionnel/pro_owner_123/uuid.pdf");
        docPro1.setProfessionnel(pro1);
    }

    @Test
    @DisplayName("Sécurité R2 - Un professionnel ne peut pas accéder aux documents d'un autre praticien")
    void testAccesInterditAutreProfessionnel() {
        // Authentifié en tant que pro2
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(pro2.getId());
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_PROFESSIONNEL")))
                .when(authenticationContext).getCurrentAuthorities();
        when(justificatifRepository.findById("doc_1")).thenReturn(Optional.of(docPro1));

        assertThrows(ForbiddenException.class, () -> justificatifService.downloadJustificatif("doc_1"));
        assertThrows(ForbiddenException.class, () -> justificatifService.getPresignedUrl("doc_1"));
        assertThrows(ForbiddenException.class, () -> justificatifService.deleteJustificatif("doc_1"));
    }

    @Test
    @DisplayName("Sécurité R2 - Le propriétaire du document a le droit de le consulter et de récupérer son URL")
    void testAccesAutoriseProprietaire() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(pro1.getId());
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_PROFESSIONNEL")))
                .when(authenticationContext).getCurrentAuthorities();
        when(justificatifRepository.findById("doc_1")).thenReturn(Optional.of(docPro1));
        when(documentStorageService.isAvailable()).thenReturn(true);
        when(documentStorageService.generatePresignedDownloadUrl(eq(docPro1.getCleObjet()), any()))
                .thenReturn("https://r2.cloudflarestorage.com/presigned-url");

        String url = justificatifService.getPresignedUrl("doc_1");

        assertNotNull(url);
        assertEquals("https://r2.cloudflarestorage.com/presigned-url", url);
    }

    @Test
    @DisplayName("Sécurité R2 - Un administrateur a le droit d'examiner les documents de n'importe quel praticien")
    void testAccesAutoriseAdministrateur() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("admin_uid_789");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATEUR")))
                .when(authenticationContext).getCurrentAuthorities();
        when(justificatifRepository.findById("doc_1")).thenReturn(Optional.of(docPro1));
        when(documentStorageService.isAvailable()).thenReturn(true);
        when(documentStorageService.generatePresignedDownloadUrl(eq(docPro1.getCleObjet()), any()))
                .thenReturn("https://r2.cloudflarestorage.com/presigned-admin-url");

        String url = justificatifService.getPresignedUrl("doc_1");

        assertNotNull(url);
        assertEquals("https://r2.cloudflarestorage.com/presigned-admin-url", url);
    }
}
