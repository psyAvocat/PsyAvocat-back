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
    private String publicId;
    private String format;
    private Long bytes;
    private Integer width;
    private Integer height;
}
