package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionSignalementRequest {
    private String action; // "SUSPENDRE" ou "CLASSER"
}
