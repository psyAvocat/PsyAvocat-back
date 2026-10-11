package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionAdministrativeDTO {
    private String id;
    private String type;
    private String cibleId;
    private String decision;
    private String motif;
    private String administrateurId;
    private LocalDateTime dateDecision;
}
