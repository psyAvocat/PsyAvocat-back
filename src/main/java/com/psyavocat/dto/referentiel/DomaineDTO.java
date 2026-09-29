package com.psyavocat.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomaineDTO {
    private String id;
    private String nom;
    private String description;
    private List<SpecialiteDTO> specialites;
}
