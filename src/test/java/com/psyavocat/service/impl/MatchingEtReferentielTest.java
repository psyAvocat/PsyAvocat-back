package com.psyavocat.service.impl;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Matching de l'orientation et intégrité des référentiels (type de professionnel explicite).
 */
@ExtendWith(MockitoExtension.class)
class MatchingEtReferentielTest {

    @Mock private ProfessionnelRepository professionnelRepository;
    @Mock private DomaineRepository domaineRepository;
    @Mock private SpecialiteRepository specialiteRepository;
    @Mock private CategorieBesoinRepository categorieBesoinRepository;
    @Mock private PonderationOrientationRepository ponderationRepository;
    @Mock private ContenuRepository contenuRepository;
    @Mock private DossierRepository dossierRepository;
    @Mock private ResultatOrientationRepository resultatOrientationRepository;
    @Mock private ResultatOrientationCategorieRepository resultatOrientationCategorieRepository;
    @Mock private RealtimeGateway realtimeGateway;

    private MatchingProfessionnelServiceImpl matching;
    private ReferentielServiceImpl referentiels;

    @BeforeEach
    void setUp() {
        matching = new MatchingProfessionnelServiceImpl(professionnelRepository);
        referentiels = new ReferentielServiceImpl(domaineRepository, specialiteRepository, categorieBesoinRepository,
                ponderationRepository, contenuRepository, dossierRepository, resultatOrientationRepository,
                resultatOrientationCategorieRepository, realtimeGateway);
    }

    // ------------------------------------------------------------------
    // Matching
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Matching psychologique : jamais d'avocat, même lié à la spécialité")
    void matching_neRecommandeQueDesPsychologues() {
        Specialite tcc = specialite("spec-tcc", "PSYCHOLOGUE");
        CategorieBesoin anxiete = categorie("PSYCHOLOGUE", tcc);
        Psychologue psy = new Psychologue();
        psy.setId("psy-1");
        psy.setNom("Diallo");
        psy.setSpecialites(new ArrayList<>(List.of(tcc)));
        Avocat avocatMalRattache = new Avocat();
        avocatMalRattache.setId("avo-1");
        avocatMalRattache.setNom("Traoré");
        avocatMalRattache.setSpecialites(new ArrayList<>(List.of(tcc)));
        when(professionnelRepository.findApprouvesParSpecialites(any())).thenReturn(List.of(avocatMalRattache, psy));

        List<Professionnel> resultat = matching.matcherParCategorieBesoin(anxiete, 5);

        assertThat(resultat).extracting(Professionnel::getId).containsExactly("psy-1");
    }

    @Test
    @DisplayName("Matching : spécialités d'un autre univers ignorées ; sans correspondance, liste vide (aucun repli)")
    void matching_sansCorrespondance_listeVide() {
        CategorieBesoin categorie = categorie("PSYCHOLOGUE", specialite("spec-droit", "AVOCAT"));

        assertThat(matching.matcherParCategorieBesoin(categorie, 5)).isEmpty();
        verifyNoInteractions(professionnelRepository);
    }

    @Test
    @DisplayName("Matching : catégorie désactivée ou sans univers → aucune recommandation")
    void matching_categorieInutilisable() {
        CategorieBesoin inactive = categorie("PSYCHOLOGUE", specialite("s1", "PSYCHOLOGUE"));
        inactive.setActif(false);
        CategorieBesoin sansType = categorie(null, specialite("s2", "PSYCHOLOGUE"));

        assertThat(matching.matcherParCategorieBesoin(inactive, 5)).isEmpty();
        assertThat(matching.matcherParCategorieBesoin(sansType, 5)).isEmpty();
        verifyNoInteractions(professionnelRepository);
    }

    // ------------------------------------------------------------------
    // Référentiels
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Spécialité : univers obligatoire (jamais déduit du libellé)")
    void specialite_sansUnivers_refusee() {
        SpecialiteDTO dto = new SpecialiteDTO();
        dto.setNom("Droit de la famille");

        assertThatThrownBy(() -> referentiels.createSpecialite(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("univers");
        verify(specialiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Spécialité : rattachée au domaine fixe de son univers, quel que soit le domaine envoyé")
    void specialite_domaineFixeDeLUnivers() {
        Domaine juridique = domaineFixe("dom-jur", "JURIDIQUE", "Juridique", "AVOCAT");
        when(domaineRepository.findByCode("JURIDIQUE")).thenReturn(Optional.of(juridique));
        when(specialiteRepository.save(any(Specialite.class))).thenAnswer(i -> i.getArgument(0));
        SpecialiteDTO dto = new SpecialiteDTO();
        dto.setNom("Droit pénal");
        dto.setTypeProfessionnel("AVOCAT");
        dto.setDomaineId("un-autre-domaine");

        SpecialiteDTO cree = referentiels.createSpecialite(dto);

        assertThat(cree.getTypeProfessionnel()).isEqualTo("AVOCAT");
        assertThat(cree.getDomaineId()).isEqualTo("dom-jur");
        assertThat(cree.getDomaineNom()).isEqualTo("Juridique");
    }

    @Test
    @DisplayName("Spécialité Avocat : aucune catégorie de besoin enregistrée")
    void specialiteAvocat_sansCategorie() {
        when(specialiteRepository.save(any(Specialite.class))).thenAnswer(i -> i.getArgument(0));
        SpecialiteDTO dto = new SpecialiteDTO();
        dto.setNom("Droit du travail");
        dto.setTypeProfessionnel("AVOCAT");
        dto.setCategorieIds(List.of("cat"));

        SpecialiteDTO cree = referentiels.createSpecialite(dto);

        assertThat(cree.getCategorieIds()).isEmpty();
        verify(categorieBesoinRepository, never()).findAllById(any());
    }

    // ------------------------------------------------------------------
    // Domaines fixes et catégories de besoin
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Domaine fixe : seule la description est modifiable")
    void domaine_seuleDescriptionModifiable() {
        Domaine psy = domaineFixe("dom-psy", "PSYCHOLOGIQUE", "Psychologique", "PSYCHOLOGUE");
        when(domaineRepository.findById("dom-psy")).thenReturn(Optional.of(psy));
        when(domaineRepository.save(any(Domaine.class))).thenAnswer(i -> i.getArgument(0));
        com.psyavocat.dto.referentiel.DomaineDTO dto = new com.psyavocat.dto.referentiel.DomaineDTO();
        dto.setNom("Autre nom");
        dto.setTypeProfessionnel("AVOCAT");
        dto.setDescription("Nouvelle description");

        var modifie = referentiels.updateDomaine("dom-psy", dto);

        assertThat(modifie.getNom()).isEqualTo("Psychologique");
        assertThat(modifie.getTypeProfessionnel()).isEqualTo("PSYCHOLOGUE");
        assertThat(modifie.getDescription()).isEqualTo("Nouvelle description");
    }

    @Test
    @DisplayName("Catégorie de besoin destinée aux avocats : refusée")
    void categorieAvocat_refusee() {
        CategorieBesoinDTO dto = new CategorieBesoinDTO();
        dto.setNom("Litige");
        dto.setTypeProfessionnel("AVOCAT");

        assertThatThrownBy(() -> referentiels.createCategorieBesoin(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Psychologue");
        verify(categorieBesoinRepository, never()).save(any());
    }

    @Test
    @DisplayName("Suppression d'une catégorie : retirée des spécialités et effacée de la base")
    void categorie_supprimeeDefinitivement() {
        Specialite tcc = specialite("spec-tcc", "PSYCHOLOGUE");
        CategorieBesoin anxiete = categorie("PSYCHOLOGUE", tcc);
        tcc.setCategoriesBesoin(new ArrayList<>(List.of(anxiete)));
        when(categorieBesoinRepository.findById("cat")).thenReturn(Optional.of(anxiete));

        referentiels.deleteCategorieBesoin("cat");

        assertThat(tcc.getCategoriesBesoin()).isEmpty();
        verify(categorieBesoinRepository).delete(anxiete);
    }

    @Test
    @DisplayName("Suppression d'une catégorie présente dans l'historique : masquée partout, jamais réaffichée")
    void categorie_historique_masquee() {
        CategorieBesoin anxiete = categorie("PSYCHOLOGUE");
        when(categorieBesoinRepository.findById("cat")).thenReturn(Optional.of(anxiete));
        when(resultatOrientationRepository.existsByCategorieBesoinId("cat")).thenReturn(true);
        when(categorieBesoinRepository.findAll()).thenReturn(List.of(anxiete));

        referentiels.deleteCategorieBesoin("cat");

        verify(categorieBesoinRepository, never()).delete(any());
        assertThat(anxiete.getSupprime()).isTrue();
        assertThat(anxiete.getActif()).isFalse();
        // « Inclure inactives » : la catégorie supprimée n'apparaît pas.
        assertThat(referentiels.getCategoriesBesoin(null, true)).isEmpty();
        assertThatThrownBy(() -> referentiels.toggleActifCategorieBesoin("cat"))
                .isInstanceOf(com.psyavocat.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Suppression d'une catégorie utilisée par le questionnaire : refusée")
    void categorie_utiliseeParPonderation_refusee() {
        CategorieBesoin anxiete = categorie("PSYCHOLOGUE");
        when(categorieBesoinRepository.findById("cat")).thenReturn(Optional.of(anxiete));
        when(ponderationRepository.countByCategorieBesoinId("cat")).thenReturn(2L);

        assertThatThrownBy(() -> referentiels.deleteCategorieBesoin("cat")).isInstanceOf(ConflictException.class);
        verify(categorieBesoinRepository, never()).delete(any());
        verify(categorieBesoinRepository, never()).save(any());
    }

    @Test
    @DisplayName("Spécialité utilisée par des professionnels : suppression refusée")
    void specialite_utilisee_nonSupprimable() {
        Specialite s = specialite("s-1", "AVOCAT");
        when(specialiteRepository.findById("s-1")).thenReturn(Optional.of(s));
        when(specialiteRepository.countProfessionnels("s-1")).thenReturn(3L);

        assertThatThrownBy(() -> referentiels.deleteSpecialite("s-1")).isInstanceOf(ConflictException.class);
        verify(specialiteRepository, never()).delete(any());
    }

    private Domaine domaineFixe(String id, String code, String nom, String type) {
        Domaine d = new Domaine();
        d.setId(id);
        d.setCode(code);
        d.setNom(nom);
        d.setTypeProfessionnel(type);
        return d;
    }

    private Specialite specialite(String id, String type) {
        Specialite s = new Specialite();
        s.setId(id);
        s.setNom(id);
        s.setTypeProfessionnel(type);
        return s;
    }

    private CategorieBesoin categorie(String type, Specialite... specialites) {
        CategorieBesoin c = new CategorieBesoin();
        c.setId("cat");
        c.setTypeProfessionnel(type);
        c.setActif(true);
        c.setSpecialites(new ArrayList<>(List.of(specialites)));
        return c;
    }
}
