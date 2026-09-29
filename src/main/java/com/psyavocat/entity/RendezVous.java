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

    private String mode; // EN_LIGNE (Visio), EN_CABINET

    private String motif;

    private Integer dureeMinutes = 45;

    private BigDecimal montantTotal;

    private BigDecimal montantAcompte; // 20% légal obligatoire

    @Column(length = 10)
    private String devise = "XOF";

    private String lienVisio;

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

    @OneToOne(mappedBy = "rendezVous", cascade = CascadeType.ALL)
    private Paiement paiement;

    // Rétrocompatibilité avec l'ancien nom de champ
    public Utilisateur getPatient() {
        return client;
    }

    public void setPatient(Utilisateur patient) {
        this.client = patient;
    }
}
