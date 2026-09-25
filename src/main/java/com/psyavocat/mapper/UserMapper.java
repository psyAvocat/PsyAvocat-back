package com.psyavocat.mapper;

import com.psyavocat.dto.profil.UserProfileResponse;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .telephone(utilisateur.getTelephone())
                .dateInscription(utilisateur.getDateInscription());

        if (utilisateur instanceof Patient) {
            builder.typeUtilisateur("PATIENT");
        } else if (utilisateur instanceof Justiciable) {
            builder.typeUtilisateur("JUSTICIABLE");
        } else if (utilisateur instanceof Avocat avocat) {
            builder.typeUtilisateur("AVOCAT")
                    .numeroBarreau(avocat.getNumeroBarreau());
            mapProfessionnelFields(builder, avocat);
        } else if (utilisateur instanceof Psychologue psychologue) {
            builder.typeUtilisateur("PSYCHOLOGUE")
                    .numeroAgrement(psychologue.getNumeroAgrement());
            mapProfessionnelFields(builder, psychologue);
        } else if (utilisateur instanceof Administrateur) {
            builder.typeUtilisateur("ADMINISTRATEUR");
        }

        return builder.build();
    }

    private void mapProfessionnelFields(UserProfileResponse.UserProfileResponseBuilder builder, Professionnel pro) {
        builder.biographie(pro.getBiographie())
                .ville(pro.getVille())
                .adresse(pro.getAdresse())
                .modeConsultation(pro.getModeConsultation())
                .statutValidation(pro.getStatutValidation());

        if (pro.getSpecialites() != null) {
            List<SpecialiteDTO> specDtos = pro.getSpecialites().stream()
                    .map(s -> new SpecialiteDTO(s.getId(), s.getNom(), s.getDescription()))
                    .toList();
            builder.specialites(specDtos);
        } else {
            builder.specialites(Collections.emptyList());
        }
    }
}
