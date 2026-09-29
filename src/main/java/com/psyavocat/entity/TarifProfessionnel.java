package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tarifs_professionnel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarifProfessionnel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private BigDecimal montant;

    @Column(nullable = false, length = 10)
    private String devise = "XOF"; // Par défaut FCFA (XOF)

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer dureeMinutes = 45;

    private Boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id", nullable = false)
    private Professionnel professionnel;
}
