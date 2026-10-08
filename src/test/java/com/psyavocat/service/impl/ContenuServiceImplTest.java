package com.psyavocat.service.impl;

import com.psyavocat.dto.contenu.ContenuRequest;
import com.psyavocat.dto.contenu.ContenuResponseDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.MediaUrlResolver;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.ContenuRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.storage.service.ImageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContenuServiceImplTest {

    @Mock private ContenuRepository contenuRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private SpecialiteRepository specialiteRepository;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private ImageStorageService imageStorageService;
    @Mock private MediaUrlResolver mediaUrlResolver;
    @Mock private RealtimeGateway realtimeGateway;

    @InjectMocks private ContenuServiceImpl service;

    private Avocat avocatValide() {
        Avocat avocat = new Avocat();
        avocat.setId("avocat-1");
        avocat.setStatutValidation("APPROVED");
        return avocat;
    }

    private ContenuRequest requete() {
        return new ContenuRequest("Vos droits en cas de litige", "Résumé", "Texte de l'article", null, true);
    }

    @Test
    @DisplayName("Un avocat validé publie : le type est forcé à ARTICLE")
    void avocat_publieUnArticle() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocatValide()));
        when(contenuRepository.save(any(Contenu.class))).thenAnswer(i -> {
            Contenu c = i.getArgument(0);
            c.setId("contenu-1");
            return c;
        });

        ContenuResponseDTO dto = service.creer(requete());

        assertThat(dto.getType()).isEqualTo("ARTICLE");
        verify(realtimeGateway).diffuser(eq("CONTENU_MIS_A_JOUR"), any());
    }

    @Test
    @DisplayName("Un psychologue validé publie : le type est forcé à CONSEIL")
    void psychologue_publieUnConseil() {
        Psychologue psy = new Psychologue();
        psy.setId("psy-1");
        psy.setStatutValidation("APPROVED");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("psy-1");
        when(utilisateurRepository.findById("psy-1")).thenReturn(Optional.of(psy));
        when(contenuRepository.save(any(Contenu.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.creer(requete()).getType()).isEqualTo("CONSEIL");
    }

    @Test
    @DisplayName("Un client ne peut pas publier")
    void client_nePeutPasPublier() {
        Client client = new Client();
        client.setId("client-1");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.creer(requete())).isInstanceOf(ForbiddenException.class);
        verify(contenuRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un avocat non validé ne peut pas publier")
    void avocatEnAttente_nePeutPasPublier() {
        Avocat avocat = avocatValide();
        avocat.setStatutValidation("PENDING");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));

        assertThatThrownBy(() -> service.creer(requete())).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Un avocat ne peut pas classer son article dans une spécialité psychologique")
    void specialiteDAutreUnivers_refusee() {
        Specialite anxiete = new Specialite();
        anxiete.setId("spe-anxiete");
        anxiete.setTypeProfessionnel("PSYCHOLOGUE");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocatValide()));
        when(specialiteRepository.findById("spe-anxiete")).thenReturn(Optional.of(anxiete));

        ContenuRequest request = requete();
        request.setSpecialiteId("spe-anxiete");

        assertThatThrownBy(() -> service.creer(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Un contenu désactivé n'est plus servi aux clients")
    void contenuDesactive_introuvablePourUnClient() {
        Contenu contenu = new Contenu();
        contenu.setId("contenu-1");
        contenu.setActif(false);
        contenu.setAuteur(avocatValide());
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(contenuRepository.findById("contenu-1")).thenReturn(Optional.of(contenu));

        assertThatThrownBy(() -> service.getContenu("contenu-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("n'est plus disponible");
    }

    @Test
    @DisplayName("Un avocat ne peut pas modifier l'article d'un confrère")
    void modificationParUnAutreAuteur_refusee() {
        Avocat autre = avocatValide();
        autre.setId("avocat-2");
        Contenu contenu = new Contenu();
        contenu.setId("contenu-1");
        contenu.setAuteur(autre);
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocatValide()));
        when(contenuRepository.findById("contenu-1")).thenReturn(Optional.of(contenu));

        assertThatThrownBy(() -> service.modifier("contenu-1", requete())).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Type de liste invalide refusé")
    void typeInvalide() {
        assertThatThrownBy(() -> service.rechercher("DOSSIER", null, null, null)).isInstanceOf(BadRequestException.class);
    }
}
