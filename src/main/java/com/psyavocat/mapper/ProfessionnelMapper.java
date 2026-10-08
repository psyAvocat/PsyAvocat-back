package com.psyavocat.mapper;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.dto.referentiel.TarifProfessionnelDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProfessionnelMapper {

    private final MediaUrlResolver mediaUrlResolver;

    public ProfessionnelMapper(MediaUrlResolver mediaUrlResolver) {
        this.mediaUrlResolver = mediaUrlResolver;
    }

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
                .statutValidation(pro.getStatutValidation())
                .motifRefus(pro.getMotifRefus())
                .photoUrl(mediaUrlResolver.photoUrlOf(pro))

                .noteMoyenne(pro.getNoteMoyenne())
                .nombreAvis(pro.getNombreAvis())
                .enLigne(pro.getEnLigne())
                .langues(pro.getLangues());

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

        if (pro.getTarifs() != null) {
            List<TarifProfessionnelDTO> tarifDtos = pro.getTarifs().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActif()))
                    .map(t -> new TarifProfessionnelDTO(
                            t.getId(), t.getTitre(), t.getMontant(), t.getDevise(), t.getDescription(), t.getDureeMinutes()
                    ))
                    .toList();
            builder.tarifs(tarifDtos);
        } else {
            builder.tarifs(Collections.emptyList());
        }

        return builder.build();
    }
}
