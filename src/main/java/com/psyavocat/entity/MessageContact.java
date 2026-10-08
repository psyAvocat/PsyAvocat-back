package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages_contact")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String objet;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    private LocalDateTime dateEnvoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id")
    private Utilisateur expediteur;

    @Column(name = "lu")
    private Boolean lu = false;
}
