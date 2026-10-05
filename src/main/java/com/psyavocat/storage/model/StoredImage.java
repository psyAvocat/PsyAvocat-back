package com.psyavocat.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredImage {
    private String url;
    private String publicId; // Conservé pour compatibilité
    private String objectKey; // Clé Cloudflare R2
    private String format;
    private Long bytes;
    private Integer width;
    private Integer height;

    public String getPublicId() {
        return publicId != null ? publicId : objectKey;
    }

    public String getObjectKey() {
        return objectKey != null ? objectKey : publicId;
    }
}
