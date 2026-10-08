package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Appareil enregistré pour recevoir les notifications push (FCM).
 *
 * Un utilisateur peut avoir plusieurs appareils. Le token FCM est un identifiant
 * technique d'appareil, jamais un identifiant métier.
 */
@Entity
@Table(name = "device_registrations")
@Getter
@Setter
@NoArgsConstructor
public class DeviceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /** ANDROID, IOS ou WEB. */
    @Column(nullable = false, length = 20)
    private String plateforme;

    @Column(nullable = false)
    private boolean actif = true;

    private LocalDateTime dateCreation;

    private LocalDateTime dateDerniereActivite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
}
