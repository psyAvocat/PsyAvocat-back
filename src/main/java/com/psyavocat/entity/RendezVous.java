package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime dateHeure;

    private String statut; // CONFIRME, EN_ATTENTE, PASSE, ANNULE

    private String mode; // VISIO ou CABINET

    private String motif;

    private Integer dureeMinutes = 45;

    private BigDecimal montantTotal;

    private BigDecimal montantAcompte; // 20% légal obligatoire

    @Column(length = 10)
    private String devise = "XOF";

    /** Salle de visioconférence propre au rendez-vous (mode VISIO). */
    private String lienVisio;

    /** Rappels déjà envoyés : chacun part une seule fois, même après un redémarrage du serveur. */
    private Boolean rappelVeilleEnvoye;
    private Boolean rappelJourEnvoye;
    private Boolean rappelVisioEnvoye;

    private String adresseCabinet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Utilisateur client; // Accepte Patient ET Justiciable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id")
    private Professionnel professionnel;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disponibilite_id")
    private Disponibilite disponibilite;

    /** Rendez-vous avocat : soumission acceptée (donc dossier) dont il découle. Null pour un psychologue. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soumission_id")
    private SoumissionDossier soumission;

    @OneToOne(mappedBy = "rendezVous", cascade = CascadeType.ALL)
    private Paiement paiement;

    /** Rendez-vous déplacé : ses rappels repartent pour la nouvelle date. */
    public void reinitialiserRappels() {
        this.rappelVeilleEnvoye = null;
        this.rappelJourEnvoye = null;
        this.rappelVisioEnvoye = null;
    }

    // Rétrocompatibilité avec l'ancien nom de champ
    public Utilisateur getPatient() {
        return client;
    }

    public void setPatient(Utilisateur patient) {
        this.client = patient;
    }
}
