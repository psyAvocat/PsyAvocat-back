package com.psyavocat.mapper;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProfessionnelMapper {

    public ProfessionnelResponseDTO toDto(Professionnel pro) {
        if (pro == null) {
            return null;
        }

        ProfessionnelResponseDTO.ProfessionnelResponseDTOBuilder builder = ProfessionnelResponseDTO.builder()
                .id(pro.getId())
                .nom(pro.getNom())
                .prenom(pro.getPrenom())
                .email(pro.getEmail())
                .telephone(pro.getTelephone())
                .biographie(pro.getBiographie())
                .ville(pro.getVille())
                .adresse(pro.getAdresse())
                .modeConsultation(pro.getModeConsultation())
                .statutValidation(pro.getStatutValidation());

        if (pro instanceof Avocat avocat) {
            builder.type("AVOCAT")
                    .numeroBarreau(avocat.getNumeroBarreau());
        } else if (pro instanceof Psychologue psychologue) {
            builder.type("PSYCHOLOGUE")
                    .numeroAgrement(psychologue.getNumeroAgrement());
        }

        if (pro.getSpecialites() != null) {
            List<SpecialiteDTO> specDtos = pro.getSpecialites().stream()
                    .map(s -> new SpecialiteDTO(s.getId(), s.getNom(), s.getDescription()))
                    .toList();
            builder.specialites(specDtos);
        } else {
            builder.specialites(Collections.emptyList());
        }

        return builder.build();
    }
}
