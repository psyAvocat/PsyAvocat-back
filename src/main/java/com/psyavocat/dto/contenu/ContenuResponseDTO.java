package com.psyavocat.dto.contenu;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Article (auteur avocat) ou Conseil (auteur psychologue). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContenuResponseDTO {
    private String id;
    /** ARTICLE ou CONSEIL. */
    private String type;
    private String titre;
    private String description;
    /** Texte complet (renvoyé uniquement par le détail). */
    private String contenu;
    private LocalDateTime datePublication;
    private LocalDateTime dateModification;
    private Boolean actif;
    private String auteurId;
    private String auteurNom;
    private String auteurPrenom;
    private String auteurPhotoUrl;
    private String specialiteId;
    private String specialiteNom;
    private String imageUrl;
    /** Estimation dérivée du texte (≈ 200 mots / minute). */
    private Integer tempsLectureMinutes;
    private Long nombreVues;
}
