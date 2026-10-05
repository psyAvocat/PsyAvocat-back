package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    private LocalDateTime dateEnvoi;

    private String lienVisio;

    @Column(nullable = false)
    private boolean lu = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;
}
