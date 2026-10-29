package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Première lecture d'une publication par un client : un lecteur ne compte qu'une fois
 * dans {@link Contenu#getNombreVues()}, même s'il rouvre la publication.
 */
@Entity
@Table(name = "contenu_lectures",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contenu_id", "lecteur_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LectureContenu {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contenu_id", nullable = false)
    private Contenu contenu;

    /** Identifiant du client lecteur (sans clé étrangère : la lecture reste comptée). */
    @Column(name = "lecteur_id", nullable = false)
    private String lecteurId;

    @Column(nullable = false)
    private LocalDateTime dateLecture;
}
