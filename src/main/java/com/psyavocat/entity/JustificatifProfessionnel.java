package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "justificatifs_professionnels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JustificatifProfessionnel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String nomFichier;
    
    @Column(nullable = false)
    private String typeDocument; // Ex: CARTE_PROFESSIONNELLE, DIPLOME, AGREMENT

    @Column(nullable = false)
    private String cheminStockage;

    @Column(nullable = false)
    private String statutValidation; // EN_ATTENTE, VALIDE, REJETE

    private LocalDate dateDepot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id", nullable = false)
    private Professionnel professionnel;
}
