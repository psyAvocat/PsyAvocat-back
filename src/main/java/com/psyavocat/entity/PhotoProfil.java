package com.psyavocat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Photo de profil d'un client (patient / justiciable / client unifié),
 * stockée dans le bucket R2 privé {@code psyavocat-media}.
 *
 * Seule la clé d'objet est durable : l'URL d'accès (présignée, temporaire)
 * est générée à la lecture.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PhotoProfil {

    @Column(name = "photo_object_key")
    private String objectKey;
}
