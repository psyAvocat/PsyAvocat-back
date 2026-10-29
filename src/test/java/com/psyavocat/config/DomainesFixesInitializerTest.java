package com.psyavocat.config;

import com.psyavocat.entity.Domaine;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.DomaineRepository;
import com.psyavocat.repository.SpecialiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Les deux domaines fixes sont créés une seule fois et reçoivent les spécialités de leur univers. */
class DomainesFixesInitializerTest {

    private DomaineRepository domaineRepository;
    private SpecialiteRepository specialiteRepository;
    private DomainesFixesInitializer initializer;

    /** Base simulée : domaines persistés par identifiant. */
    private final Map<String, Domaine> base = new HashMap<>();
    private int sequence;

    @BeforeEach
    void setUp() {
        domaineRepository = mock(DomaineRepository.class);
        specialiteRepository = mock(SpecialiteRepository.class);
        initializer = new DomainesFixesInitializer(domaineRepository, specialiteRepository);

        when(domaineRepository.findAll()).thenAnswer(i -> new ArrayList<>(base.values()));
        when(domaineRepository.findByCode(any())).thenAnswer(i -> base.values().stream()
                .filter(d -> i.getArgument(0).equals(d.getCode())).findFirst());
        when(domaineRepository.findByNom(any())).thenAnswer(i -> base.values().stream()
                .filter(d -> i.getArgument(0).equals(d.getNom())).findFirst());
        when(domaineRepository.save(any(Domaine.class))).thenAnswer(i -> {
            Domaine d = i.getArgument(0);
            if (d.getId() == null) {
                d.setId("dom-" + (++sequence));
            }
            base.put(d.getId(), d);
            return d;
        });
        doAnswer(i -> base.remove(((Domaine) i.getArgument(0)).getId())).when(domaineRepository).delete(any(Domaine.class));
    }

    private Domaine domaine(String id, String nom, String type) {
        Domaine d = new Domaine();
        d.setId(id);
        d.setNom(nom);
        d.setTypeProfessionnel(type);
        base.put(id, d);
        return d;
    }

    private Specialite specialite(String type, Domaine domaine) {
        Specialite s = new Specialite();
        s.setId("spec-" + type);
        s.setTypeProfessionnel(type);
        s.setDomaine(domaine);
        return s;
    }

    @Test
    @DisplayName("Premier démarrage : création des domaines, rattachement des spécialités, ancien domaine retiré")
    void premierDemarrage() {
        Domaine ancien = domaine("old", "Santé mentale", "PSYCHOLOGUE");
        Specialite tcc = specialite("PSYCHOLOGUE", ancien);
        Specialite penal = specialite("AVOCAT", null);
        when(specialiteRepository.findAll()).thenReturn(List.of(tcc, penal));
        when(specialiteRepository.countByDomaineId(any())).thenReturn(0L);

        initializer.run(null);

        Domaine psy = domaineRepository.findByCode("PSYCHOLOGIQUE").orElseThrow();
        Domaine jur = domaineRepository.findByCode("JURIDIQUE").orElseThrow();
        assertThat(psy.getNom()).isEqualTo("Psychologique");
        assertThat(psy.getTypeProfessionnel()).isEqualTo("PSYCHOLOGUE");
        assertThat(jur.getNom()).isEqualTo("Juridique");
        assertThat(jur.getTypeProfessionnel()).isEqualTo("AVOCAT");
        assertThat(tcc.getDomaine()).isSameAs(psy);
        assertThat(penal.getDomaine()).isSameAs(jur);
        assertThat(base).doesNotContainKey("old");
        assertThat(base).hasSize(2);
    }

    @Test
    @DisplayName("Domaine existant de même nom : repris (identifiant conservé)")
    void repriseDuneDomaineExistant() {
        Domaine existant = domaine("jur-existant", "juridique", null);
        existant.setDescription("Description saisie");
        when(specialiteRepository.findAll()).thenReturn(List.of());

        initializer.run(null);

        Domaine jur = domaineRepository.findByCode("JURIDIQUE").orElseThrow();
        assertThat(jur.getId()).isEqualTo("jur-existant");
        assertThat(jur.getNom()).isEqualTo("Juridique");
        assertThat(jur.getDescription()).isEqualTo("Description saisie");
    }

    @Test
    @DisplayName("Démarrages suivants : rien n'est recréé ni modifié")
    void idempotent() {
        when(specialiteRepository.findAll()).thenReturn(List.of());
        initializer.run(null);
        Map<String, Domaine> apresPremier = new HashMap<>(base);
        clearInvocations(domaineRepository);

        initializer.run(null);

        assertThat(base).isEqualTo(apresPremier);
        verify(domaineRepository, never()).save(any());
        verify(domaineRepository, never()).delete(any());
        assertThat(Optional.ofNullable(base.get("dom-1"))).isPresent();
    }
}
