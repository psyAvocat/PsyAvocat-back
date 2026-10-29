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

    private Long tailleOctets;

    private String typeMime;

    private String cleObjet; // Clé Cloudflare R2: justificatifs/professionnel/{proId}/{uuid}.ext

    private LocalDate dateDepot;

    /**
     * Justificatif complémentaire validé que cette nouvelle version remplacera si l'administration
     * l'approuve (null pour un dépôt indépendant). Les documents obligatoires se remplacent par type.
     */
    @Column(name = "remplace_id")
    private String remplaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id", nullable = false)
    private Professionnel professionnel;
}
