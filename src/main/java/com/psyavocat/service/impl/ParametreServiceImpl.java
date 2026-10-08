package com.psyavocat.service.impl;

import com.psyavocat.dto.admin.ParametrePlateformeDTO;
import com.psyavocat.entity.ParametrePlateforme;
import com.psyavocat.repository.ParametrePlateformeRepository;
import com.psyavocat.service.ParametreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParametreServiceImpl implements ParametreService {

    private static final String DEFAULT_ID = "GLOBAL_CONFIG";
    private final ParametrePlateformeRepository repository;

    public ParametreServiceImpl(ParametrePlateformeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ParametrePlateformeDTO getParametres() {
        ParametrePlateforme param = repository.findById(DEFAULT_ID)
                .orElseGet(this::initDefaultParametres);
        return toDto(param);
    }

    @Override
    public ParametrePlateformeDTO updateParametres(ParametrePlateformeDTO dto) {
        ParametrePlateforme param = repository.findById(DEFAULT_ID)
                .orElseGet(() -> ParametrePlateforme.builder().id(DEFAULT_ID).build());

        param.setEmailNotifications(dto.isEmailNotifications());
        param.setAutoValidation(dto.isAutoValidation());
        param.setDelaiValidationHeures(dto.getDelaiValidationHeures() > 0 ? dto.getDelaiValidationHeures() : 72);
        if (dto.getLangueDefaut() != null && !dto.getLangueDefaut().isBlank()) {
            param.setLangueDefaut(dto.getLangueDefaut());
        }
        param.setModeMaintenance(dto.isModeMaintenance());
        if (dto.getEmailSupport() != null && !dto.getEmailSupport().isBlank()) {
            param.setEmailSupport(dto.getEmailSupport());
        }
        if (dto.getVersionCgu() != null && !dto.getVersionCgu().isBlank()) {
            param.setVersionCgu(dto.getVersionCgu());
        }

        ParametrePlateforme saved = repository.save(param);
        return toDto(saved);
    }

    private ParametrePlateforme initDefaultParametres() {
        ParametrePlateforme defaults = ParametrePlateforme.builder()
                .id(DEFAULT_ID)
                .emailNotifications(true)
                .autoValidation(false)
                .delaiValidationHeures(72)
                .langueDefaut("fr")
                .modeMaintenance(false)
                .emailSupport("support@psyavocat.com")
                .versionCgu("v1.2")
                .build();
        return repository.save(defaults);
    }

    private ParametrePlateformeDTO toDto(ParametrePlateforme entity) {
        return ParametrePlateformeDTO.builder()
                .id(entity.getId())
                .emailNotifications(entity.isEmailNotifications())
                .autoValidation(entity.isAutoValidation())
                .delaiValidationHeures(entity.getDelaiValidationHeures())
                .langueDefaut(entity.getLangueDefaut())
                .modeMaintenance(entity.isModeMaintenance())
                .emailSupport(entity.getEmailSupport())
                .versionCgu(entity.getVersionCgu())
                .build();
    }
}
