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
    @Mock private com.psyavocat.repository.LectureContenuRepository lectureContenuRepository;

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
    @DisplayName("Un avocat ne classe son article que dans une de ses spécialités")
    void specialiteNonDeclaree_refusee() {
        Specialite famille = new Specialite();
        famille.setId("spe-famille");
        famille.setTypeProfessionnel("AVOCAT");
        Specialite travail = new Specialite();
        travail.setId("spe-travail");
        travail.setTypeProfessionnel("AVOCAT");
        Avocat avocat = avocatValide();
        avocat.getSpecialites().add(famille);
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));
        when(specialiteRepository.findById("spe-travail")).thenReturn(Optional.of(travail));

        ContenuRequest request = requete();
        request.setSpecialiteId("spe-travail");

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vos spécialités");
        verify(contenuRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un avocat publie dans une spécialité déclarée à son inscription")
    void specialiteDeclaree_acceptee() {
        Specialite famille = new Specialite();
        famille.setId("spe-famille");
        famille.setTypeProfessionnel("AVOCAT");
        Avocat avocat = avocatValide();
        avocat.getSpecialites().add(famille);
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));
        when(specialiteRepository.findById("spe-famille")).thenReturn(Optional.of(famille));
        when(contenuRepository.save(any(Contenu.class))).thenAnswer(i -> i.getArgument(0));

        ContenuRequest request = requete();
        request.setSpecialiteId("spe-famille");

        assertThat(service.creer(request).getSpecialiteId()).isEqualTo("spe-famille");
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
        connecterClient();
        assertThatThrownBy(() -> service.rechercher("DOSSIER", null, null, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Un client consulte la liste des articles publiés")
    void client_consulteLesArticles() {
        connecterClient();
        Contenu article = contenuPublie("contenu-1", avocatValide());
        when(contenuRepository.rechercherPublies("ARTICLE", null, null)).thenReturn(java.util.List.of(article));

        assertThat(service.rechercher("ARTICLE", null, null, "recent"))
                .extracting(ContenuResponseDTO::getId)
                .containsExactly("contenu-1");
    }

    @Test
    @DisplayName("Fiche d'un professionnel : seules ses publications sont listées")
    void client_consulteLesArticlesDUnAvocat() {
        connecterClient();
        Avocat autre = avocatValide();
        autre.setId("avocat-2");
        when(contenuRepository.rechercherPublies("ARTICLE", null, null)).thenReturn(java.util.List.of(
                contenuPublie("contenu-1", avocatValide()),
                contenuPublie("contenu-2", autre)));

        assertThat(service.rechercher("ARTICLE", null, null, "recent", "avocat-2"))
                .extracting(ContenuResponseDTO::getId)
                .containsExactly("contenu-2");
    }

    @Test
    @DisplayName("Un professionnel ne consulte pas la liste publique (réservée à l'app mobile)")
    void professionnel_nePeutPasListerLesPublications() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocatValide()));

        assertThatThrownBy(() -> service.rechercher("ARTICLE", null, null, null))
                .isInstanceOf(ForbiddenException.class);
        verify(contenuRepository, never()).rechercherPublies(any(), any(), any());
    }

    @Test
    @DisplayName("Un autre professionnel ne lit pas le détail d'une publication")
    void autreProfessionnel_nePeutPasLireLeDetail() {
        Contenu contenu = contenuPublie("contenu-1", avocatValide());
        Psychologue psy = new Psychologue();
        psy.setId("psy-1");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("psy-1");
        when(contenuRepository.findById("contenu-1")).thenReturn(Optional.of(contenu));
        when(utilisateurRepository.findById("psy-1")).thenReturn(Optional.of(psy));

        assertThatThrownBy(() -> service.getContenu("contenu-1")).isInstanceOf(ForbiddenException.class);
        verify(contenuRepository, never()).incrementerVues(any());
    }

    @Test
    @DisplayName("Un client lit le détail d'une publication et sa consultation est comptée")
    void client_litLeDetail() {
        connecterClient();
        Contenu contenu = contenuPublie("contenu-1", avocatValide());
        when(contenuRepository.findById("contenu-1")).thenReturn(Optional.of(contenu));
        when(lectureContenuRepository.enregistrerPremiereLecture(any(), eq("contenu-1"), eq("client-1"), any()))
                .thenReturn(1);

        assertThat(service.getContenu("contenu-1").getId()).isEqualTo("contenu-1");
        verify(contenuRepository).incrementerVues("contenu-1");
    }

    @Test
    @DisplayName("Un client qui relit une publication ne la rend pas plus populaire")
    void client_relitLeDetail_nonCompte() {
        connecterClient();
        Contenu contenu = contenuPublie("contenu-1", avocatValide());
        when(contenuRepository.findById("contenu-1")).thenReturn(Optional.of(contenu));
        when(lectureContenuRepository.enregistrerPremiereLecture(any(), eq("contenu-1"), eq("client-1"), any()))
                .thenReturn(0);

        service.getContenu("contenu-1");

        verify(contenuRepository, never()).incrementerVues(any());
    }

    @Test
    @DisplayName("Populaires : plus de lecteurs d'abord, puis la plus récente à égalité")
    void tri_populaire() {
        connecterClient();
        Contenu ancien = contenuPublie("ancien", avocatValide());
        ancien.setNombreVues(3L);
        ancien.setDatePublication(java.time.LocalDateTime.now().minusDays(30));
        Contenu recentEgal = contenuPublie("recent-egal", avocatValide());
        recentEgal.setNombreVues(3L);
        recentEgal.setDatePublication(java.time.LocalDateTime.now().minusDays(1));
        Contenu lePlusLu = contenuPublie("le-plus-lu", avocatValide());
        lePlusLu.setNombreVues(10L);
        lePlusLu.setDatePublication(java.time.LocalDateTime.now().minusDays(60));
        when(contenuRepository.rechercherPublies("ARTICLE", null, null))
                .thenReturn(java.util.List.of(ancien, lePlusLu, recentEgal));

        assertThat(service.rechercher("ARTICLE", null, null, "populaire"))
                .extracting(ContenuResponseDTO::getId)
                .containsExactly("le-plus-lu", "recent-egal", "ancien");
    }

    private void connecterClient() {
        Client client = new Client();
        client.setId("client-1");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));
    }

    private Contenu contenuPublie(String id, Professionnel auteur) {
        Contenu contenu = new Contenu();
        contenu.setId(id);
        contenu.setType("ARTICLE");
        contenu.setTitre("Vos droits en cas de litige");
        contenu.setActif(true);
        contenu.setAuteur(auteur);
        return contenu;
    }
}
