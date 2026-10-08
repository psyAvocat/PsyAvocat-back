package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profil client unique créé à l'inscription mobile.
 *
 * Décision produit : un seul profil, valable à la fois dans l'univers Avocat
 * (rôle justiciable) et dans l'univers Psychologue (rôle patient).
 * Les anciens profils {@link Patient} et {@link Justiciable} restent valides.
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client extends Utilisateur {

    @Embedded
    private PhotoProfil photo = new PhotoProfil();
}
