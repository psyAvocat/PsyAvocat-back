package com.psyavocat.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredDocument {
    private String cleObjet;
    private String nomOriginal;
    private String typeMime;
    private Long tailleOctets;
    private String urlPresignee;
}
