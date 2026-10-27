package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "avocats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Avocat extends Professionnel {

    /** Numéro de barreau, unique entre tous les avocats (enregistré en majuscules, sans espaces). */
    @Column(unique = true)
    private String numeroBarreau;

    @OneToMany(mappedBy = "avocat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoumissionDossier> soumissionsDossier = new ArrayList<>();
}
