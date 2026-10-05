package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "professionnels")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Professionnel extends Utilisateur {

    @Column(columnDefinition = "TEXT")
    private String biographie;

    private String ville;

    private String adresse;

    private String modeConsultation;

    private String statutValidation;

    @Column(columnDefinition = "TEXT")
    private String motifRefus;

    private String photoUrl;


    @Column(name = "photo_object_key")
    private String photoObjectKey;

    public String getPhotoPublicId() {
        return photoObjectKey;
    }

    public void setPhotoPublicId(String photoPublicId) {
        this.photoObjectKey = photoPublicId;
    }

    private Double noteMoyenne;

    private Integer nombreAvis;

    private Boolean enLigne = false;

    private String langues; // Ex: "Français, Bambara, Anglais"

    @ManyToMany
    @JoinTable(
            name = "professionnel_specialites",
            joinColumns = @JoinColumn(name = "professionnel_id"),
            inverseJoinColumns = @JoinColumn(name = "specialite_id")
    )
    private List<Specialite> specialites = new ArrayList<>();

    @OneToMany(mappedBy = "professionnel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Disponibilite> disponibilites = new ArrayList<>();

    @OneToMany(mappedBy = "professionnel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TarifProfessionnel> tarifs = new ArrayList<>();

    @OneToMany(mappedBy = "professionnel")
    private List<RendezVous> rendezVous = new ArrayList<>();
}
