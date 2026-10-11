package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pieces_jointes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String nom;

    /** Emplacement du document : {@code r2://<clé>} (bucket privé psyavocat-documents). */
    @Column(nullable = false)
    private String url;

    /** Clé d'objet dans le bucket privé : l'accès passe par une URL présignée temporaire. */
    @Column(name = "cle_objet")
    private String cleObjet;

    @Column(name = "type_mime")
    private String typeMime;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    private LocalDate dateAjout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
