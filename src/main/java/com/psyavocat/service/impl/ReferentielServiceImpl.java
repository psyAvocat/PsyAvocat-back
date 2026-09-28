package com.psyavocat.service.impl;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.service.ReferentielService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReferentielServiceImpl implements ReferentielService {

    private final SpecialiteRepository specialiteRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;

    public ReferentielServiceImpl(
            SpecialiteRepository specialiteRepository,
            CategorieBesoinRepository categorieBesoinRepository
    ) {
        this.specialiteRepository = specialiteRepository;
        this.categorieBesoinRepository = categorieBesoinRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecialiteDTO> getAllSpecialites() {
        return specialiteRepository.findAll().stream()
                .map(s -> new SpecialiteDTO(s.getId(), s.getNom(), s.getDescription()))
                .toList();
    }

    @Override
    public SpecialiteDTO createSpecialite(SpecialiteDTO dto) {
        Specialite specialite = new Specialite();
        specialite.setNom(dto.getNom());
        specialite.setDescription(dto.getDescription());
        Specialite saved = specialiteRepository.save(specialite);
        return new SpecialiteDTO(saved.getId(), saved.getNom(), saved.getDescription());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel) {
        List<CategorieBesoin> list;
        if (typeProfessionnel != null && !typeProfessionnel.isBlank()) {
            list = categorieBesoinRepository.findByActifTrueAndTypeProfessionnel(typeProfessionnel);
        } else {
            list = categorieBesoinRepository.findByActifTrue();
        }
        return list.stream()
                .map(c -> new CategorieBesoinDTO(c.getId(), c.getNom(), c.getDescription(), c.getTypeProfessionnel(), c.getActif()))
                .toList();
    }

    @Override
    public CategorieBesoinDTO createCategorieBesoin(CategorieBesoinDTO dto) {
        CategorieBesoin categorie = new CategorieBesoin();
        categorie.setNom(dto.getNom());
        categorie.setDescription(dto.getDescription());
        categorie.setTypeProfessionnel(dto.getTypeProfessionnel());
        categorie.setActif(dto.getActif() != null ? dto.getActif() : true);
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        return new CategorieBesoinDTO(saved.getId(), saved.getNom(), saved.getDescription(), saved.getTypeProfessionnel(), saved.getActif());
    }
}
