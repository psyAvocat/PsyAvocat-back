package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametrePlateformeDTO {
    private String id;
    private boolean emailNotifications;
    private boolean autoValidation;
    private int delaiValidationHeures;
    private String langueDefaut;
    private boolean modeMaintenance;
    private String emailSupport;
    private String versionCgu;
}
