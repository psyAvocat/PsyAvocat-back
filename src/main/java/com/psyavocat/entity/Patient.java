package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient extends Utilisateur {

    /** Photo de profil (R2) — voir {@link PhotoProfil}. */
    @Embedded
    private PhotoProfil photo = new PhotoProfil();
}
