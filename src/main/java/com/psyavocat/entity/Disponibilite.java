package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "disponibilites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Disponibilite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDate date;

    private LocalTime heureDebut;

    private LocalTime heureFin;

    private String statut; // DISPONIBLE, RESERVE, BLOQUE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id")
    private Professionnel professionnel;

    @OneToOne(mappedBy = "disponibilite")
    private RendezVous rendezVous;
}
