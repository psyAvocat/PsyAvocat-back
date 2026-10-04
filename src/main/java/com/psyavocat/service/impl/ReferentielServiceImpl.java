package com.psyavocat.service.impl;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
import com.psyavocat.dto.referentiel.DomaineDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Domaine;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.DomaineRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.service.ReferentielService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class ReferentielServiceImpl implements ReferentielService {

    private final DomaineRepository domaineRepository;
    private final SpecialiteRepository specialiteRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;

    public ReferentielServiceImpl(
            DomaineRepository domaineRepository,
            SpecialiteRepository specialiteRepository,
            CategorieBesoinRepository categorieBesoinRepository
    ) {
        this.domaineRepository = domaineRepository;
        this.specialiteRepository = specialiteRepository;
        this.categorieBesoinRepository = categorieBesoinRepository;
    }

    // =========================================================================
    // DOMAINES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<DomaineDTO> getAllDomaines() {
        return domaineRepository.findAll().stream()
                .map(d -> DomaineDTO.builder()
                        .id(d.getId())
                        .nom(d.getNom())
                        .description(d.getDescription())
                        .build())
                .toList();
    }

    @Override
    public DomaineDTO createDomaine(DomaineDTO dto) {
        Domaine domaine = new Domaine();
        domaine.setNom(dto.getNom().trim());
        domaine.setDescription(dto.getDescription());
        Domaine saved = domaineRepository.save(domaine);
        return DomaineDTO.builder()
                .id(saved.getId())
                .nom(saved.getNom())
                .description(saved.getDescription())
                .build();
    }

    @Override
    public DomaineDTO updateDomaine(String id, DomaineDTO dto) {
        Domaine domaine = domaineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Domaine introuvable : " + id));
        domaine.setNom(dto.getNom().trim());
        domaine.setDescription(dto.getDescription());
        Domaine saved = domaineRepository.save(domaine);
        return DomaineDTO.builder()
                .id(saved.getId())
                .nom(saved.getNom())
                .description(saved.getDescription())
                .build();
    }

    @Override
    public void deleteDomaine(String id) {
        domaineRepository.deleteById(id);
    }

    // =========================================================================
    // SPÉCIALITÉS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SpecialiteDTO> getAllSpecialites() {
        return specialiteRepository.findAll().stream()
                .map(this::mapToSpecialiteDTO)
                .toList();
    }

    @Override
    public SpecialiteDTO createSpecialite(SpecialiteDTO dto) {
        Specialite specialite = new Specialite();
        specialite.setNom(dto.getNom().trim());
        specialite.setDescription(dto.getDescription());

        // Association optionnelle au Domaine
        if (StringUtils.hasText(dto.getDomaineId())) {
            domaineRepository.findById(dto.getDomaineId())
                    .ifPresent(specialite::setDomaine);
        }

        // Association optionnelle aux Catégories de besoin
        if (dto.getCategorieIds() != null && !dto.getCategorieIds().isEmpty()) {
            List<CategorieBesoin> categories = categorieBesoinRepository.findAllById(dto.getCategorieIds());
            specialite.setCategoriesBesoin(categories);
        }

        Specialite saved = specialiteRepository.save(specialite);
        return mapToSpecialiteDTO(saved);
    }

    @Override
    public SpecialiteDTO updateSpecialite(String id, SpecialiteDTO dto) {
        Specialite specialite = specialiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Spécialité introuvable : " + id));

        specialite.setNom(dto.getNom().trim());
        specialite.setDescription(dto.getDescription());

        if (StringUtils.hasText(dto.getDomaineId())) {
            domaineRepository.findById(dto.getDomaineId())
                    .ifPresent(specialite::setDomaine);
        } else {
            specialite.setDomaine(null);
        }

        if (dto.getCategorieIds() != null) {
            List<CategorieBesoin> categories = categorieBesoinRepository.findAllById(dto.getCategorieIds());
            specialite.setCategoriesBesoin(categories);
        }

        Specialite saved = specialiteRepository.save(specialite);
        return mapToSpecialiteDTO(saved);
    }

    @Override
    public void deleteSpecialite(String id) {
        specialiteRepository.deleteById(id);
    }

    private SpecialiteDTO mapToSpecialiteDTO(Specialite s) {
        SpecialiteDTO dto = new SpecialiteDTO();
        dto.setId(s.getId());
        dto.setNom(s.getNom());
        dto.setDescription(s.getDescription());

        if (s.getDomaine() != null) {
            dto.setDomaineId(s.getDomaine().getId());
            dto.setDomaineNom(s.getDomaine().getNom());
        }

        if (s.getCategoriesBesoin() != null && !s.getCategoriesBesoin().isEmpty()) {
            dto.setCategorieIds(s.getCategoriesBesoin().stream().map(CategorieBesoin::getId).toList());
            dto.setCategories(s.getCategoriesBesoin().stream()
                    .map(c -> new CategorieBesoinDTO(c.getId(), c.getNom(), c.getDescription(), c.getTypeProfessionnel(), c.getActif()))
                    .toList());
        } else {
            dto.setCategorieIds(Collections.emptyList());
            dto.setCategories(Collections.emptyList());
        }

        return dto;
    }

    // =========================================================================
    // CATÉGORIES DE BESOIN
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel) {
        return getCategoriesBesoin(typeProfessionnel, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel, boolean includeInactive) {
        List<CategorieBesoin> list;
        if (includeInactive) {
            if (typeProfessionnel != null && !typeProfessionnel.isBlank()) {
                list = categorieBesoinRepository.findAll().stream()
                        .filter(c -> typeProfessionnel.equalsIgnoreCase(c.getTypeProfessionnel()))
                        .toList();
            } else {
                list = categorieBesoinRepository.findAll();
            }
        } else {
            if (typeProfessionnel != null && !typeProfessionnel.isBlank()) {
                list = categorieBesoinRepository.findByActifTrueAndTypeProfessionnel(typeProfessionnel);
            } else {
                list = categorieBesoinRepository.findByActifTrue();
            }
        }
        return list.stream()
                .map(c -> new CategorieBesoinDTO(c.getId(), c.getNom(), c.getDescription(), c.getTypeProfessionnel(), c.getActif()))
                .toList();
    }

    @Override
    public CategorieBesoinDTO createCategorieBesoin(CategorieBesoinDTO dto) {
        CategorieBesoin categorie = new CategorieBesoin();
        categorie.setNom(dto.getNom().trim());
        categorie.setDescription(dto.getDescription());
        categorie.setTypeProfessionnel(dto.getTypeProfessionnel());
        categorie.setActif(dto.getActif() != null ? dto.getActif() : true);
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        return new CategorieBesoinDTO(saved.getId(), saved.getNom(), saved.getDescription(), saved.getTypeProfessionnel(), saved.getActif());
    }

    @Override
    public CategorieBesoinDTO updateCategorieBesoin(String id, CategorieBesoinDTO dto) {
        CategorieBesoin categorie = categorieBesoinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable : " + id));
        categorie.setNom(dto.getNom().trim());
        categorie.setDescription(dto.getDescription());
        categorie.setTypeProfessionnel(dto.getTypeProfessionnel());
        if (dto.getActif() != null) {
            categorie.setActif(dto.getActif());
        }
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        return new CategorieBesoinDTO(saved.getId(), saved.getNom(), saved.getDescription(), saved.getTypeProfessionnel(), saved.getActif());
    }

    @Override
    public CategorieBesoinDTO toggleActifCategorieBesoin(String id) {
        CategorieBesoin categorie = categorieBesoinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable : " + id));
        categorie.setActif(categorie.getActif() == null || !categorie.getActif());
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        return new CategorieBesoinDTO(saved.getId(), saved.getNom(), saved.getDescription(), saved.getTypeProfessionnel(), saved.getActif());
    }

    @Override
    public void deleteCategorieBesoin(String id) {
        CategorieBesoin categorie = categorieBesoinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable : " + id));
        categorie.setActif(false);
        categorieBesoinRepository.save(categorie);
    }
}
