package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Discussion supprimée par l'un de ses participants (« supprimer pour moi »).
 *
 * Elle disparaît de sa messagerie ; l'autre participant la conserve. Si une nouvelle
 * conversation reprend (nouveau message), seuls les messages postérieurs à
 * {@link #dateMasquage} lui sont affichés. Quand tous les participants l'ont supprimée,
 * la conversation est effacée de la base.
 */
@Entity
@Table(name = "conversations_masquees",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "utilisateur_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMasquee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private LocalDateTime dateMasquage;
}
