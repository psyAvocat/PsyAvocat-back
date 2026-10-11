package com.psyavocat.service.impl;

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
    @Mock private RealtimeGateway realtimeGateway;

    private MatchingProfessionnelServiceImpl matching;
    private ReferentielServiceImpl referentiels;

    @BeforeEach
    void setUp() {
        matching = new MatchingProfessionnelServiceImpl(professionnelRepository);
        referentiels = new ReferentielServiceImpl(domaineRepository, specialiteRepository, categorieBesoinRepository,
                ponderationRepository, contenuRepository, dossierRepository, resultatOrientationRepository, realtimeGateway);
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
    @DisplayName("Spécialité : incohérence avec l'univers du domaine refusée")
    void specialite_domaineAutreUnivers_refusee() {
        Domaine domaine = new Domaine();
        domaine.setId("dom-1");
        domaine.setNom("Santé mentale");
        domaine.setTypeProfessionnel("PSYCHOLOGUE");
        when(domaineRepository.findById("dom-1")).thenReturn(Optional.of(domaine));
        SpecialiteDTO dto = new SpecialiteDTO();
        dto.setNom("Droit pénal");
        dto.setTypeProfessionnel("AVOCAT");
        dto.setDomaineId("dom-1");

        assertThatThrownBy(() -> referentiels.createSpecialite(dto)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Spécialité : univers hérité du domaine et enregistré")
    void specialite_universDuDomaine_enregistre() {
        Domaine domaine = new Domaine();
        domaine.setId("dom-2");
        domaine.setTypeProfessionnel("AVOCAT");
        when(domaineRepository.findById("dom-2")).thenReturn(Optional.of(domaine));
        when(specialiteRepository.save(any(Specialite.class))).thenAnswer(i -> i.getArgument(0));
        SpecialiteDTO dto = new SpecialiteDTO();
        dto.setNom("Droit immobilier");
        dto.setDomaineId("dom-2");

        SpecialiteDTO cree = referentiels.createSpecialite(dto);

        assertThat(cree.getTypeProfessionnel()).isEqualTo("AVOCAT");
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
