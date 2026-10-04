package com.psyavocat.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "parametres_plateforme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametrePlateforme {

    @Id
    private String id;

    @Builder.Default
    private boolean emailNotifications = true;

    @Builder.Default
    private boolean autoValidation = false;

    @Builder.Default
    private int delaiValidationHeures = 72;

    @Builder.Default
    private String langueDefaut = "fr";

    @Builder.Default
    private boolean modeMaintenance = false;

    @Builder.Default
    private String emailSupport = "support@psyavocat.com";

    @Builder.Default
    private String versionCgu = "v1.2";
}
