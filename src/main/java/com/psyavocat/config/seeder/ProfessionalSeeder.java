package com.psyavocat.config.seeder;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Specialite;
import com.psyavocat.entity.TarifProfessionnel;
import com.psyavocat.repository.AvocatRepository;
import com.psyavocat.repository.PsychologueRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.repository.TarifProfessionnelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeder dédié aux Professionnels (Avocats, Psychologues) et à leurs grilles tarifaires (TarifProfessionnel).
 * 100% Idempotent : Vérifie l'existence par identifiant unique stable et titre de tarif.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfessionalSeeder {

    private final AvocatRepository avocatRepository;
    private final PsychologueRepository psychologueRepository;
    private final SpecialiteRepository specialiteRepository;
    private final TarifProfessionnelRepository tarifProfessionnelRepository;

    @Transactional
    public void seed() {
        log.info("  [ProfessionalSeeder] Initialisation des Professionnels (Avocats, Psychologues) et de leurs Tarifs...");

        seedAvocats();
        seedPsychologues();
    }

    private void seedAvocats() {
        // 1. Maître Mamadou Sangaré (Bamako, Mali)
        if (!avocatRepository.existsById("avocat-sangare")) {
            Avocat sangare = new Avocat();
            sangare.setId("avocat-sangare");
            sangare.setNom("Sangaré");
            sangare.setPrenom("Mamadou");
            sangare.setEmail("mamadou.sangare@psyavocat.com");
            sangare.setTelephone("+223 76 12 34 56");
            sangare.setVille("Bamako");
            sangare.setAdresse("Rue 14, Porte 205, Quartier Badalabougou");
            sangare.setNumeroBarreau("ML-BMK-2012-042");
            sangare.setBiographie("Avocat inscrit au Barreau du Mali depuis 2012. Ancien lauréat du concours d'éloquence, j'assure avec rigueur et combativité la défense des salariés, chefs d'entreprise et familles dans leurs litiges fonciers et sociaux.");
            sangare.setModeConsultation("HYBRIDE");
            sangare.setStatutValidation("VALIDE");
            sangare.setPhotoUrl("https://images.unsplash.com/photo-1556157382-97eda2d62296?auto=format&fit=crop&w=400&q=80");
            sangare.setNoteMoyenne(4.9);
            sangare.setNombreAvis(48);
            sangare.setEnLigne(true);
            sangare.setLangues("Français, Bambara, Anglais");
            sangare.setDateInscription(LocalDate.now().minusYears(3));
            sangare.setSpecialites(findSpecialitesByNoms(List.of(
                    "Droit du travail & relations sociales",
                    "Droit immobilier & contentieux foncier",
                    "Droit des affaires & contentieux commercial"
            )));
            avocatRepository.save(sangare);

            seedTarif(sangare, "Consultation juridique initiale", new BigDecimal("25000"), "XOF", "Évaluation de la situation et première analyse juridique personnalisée.", 45);
            seedTarif(sangare, "Audit complet & négociation", new BigDecimal("50000"), "XOF", "Examen approfondi des contrats ou pièces juridiques avec préconisations écrites.", 60);
            seedTarif(sangare, "Consultation express visio", new BigDecimal("15000"), "XOF", "Échange rapide de cadrage juridique par visioconférence.", 30);
            log.info("    + Avocat créé : Maître Mamadou Sangaré (Bamako)");
        }

        // 2. Maître Claire Dubois (Paris 8e, France)
        if (!avocatRepository.existsById("avocat-dubois")) {
            Avocat dubois = new Avocat();
            dubois.setId("avocat-dubois");
            dubois.setNom("Dubois");
            dubois.setPrenom("Claire");
            dubois.setEmail("claire.dubois@psyavocat.com");
            dubois.setTelephone("+33 1 42 68 00 00");
            dubois.setVille("Paris 8e");
            dubois.setAdresse("12 Avenue Montaigne, 75008 Paris");
            dubois.setNumeroBarreau("FR-PARIS-2014-089");
            dubois.setBiographie("Maître Claire Dubois exerce en droit patrimonial de la famille et successions internationales depuis plus de 10 ans. Elle privilégie une approche humaine et pacifique pour dénouer les conflits complexes.");
            dubois.setModeConsultation("HYBRIDE");
            dubois.setStatutValidation("VALIDE");
            dubois.setPhotoUrl("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80");
            dubois.setNoteMoyenne(4.8);
            dubois.setNombreAvis(34);
            dubois.setEnLigne(true);
            dubois.setLangues("Français, Anglais");
            dubois.setDateInscription(LocalDate.now().minusYears(2));
            dubois.setSpecialites(findSpecialitesByNoms(List.of(
                    "Droit de la famille & des personnes",
                    "Droit immobilier & contentieux foncier"
            )));
            avocatRepository.save(dubois);

            seedTarif(dubois, "Consultation d'orientation familiale", new BigDecimal("45000"), "XOF", "Analyse patrimoniale et stratégie en matière de divorce ou succession.", 45);
            seedTarif(dubois, "Étude approfondie de dossier", new BigDecimal("80000"), "XOF", "Stratégie judiciaire complète et rédaction d'actes préparatoires.", 60);
            log.info("    + Avocate créée : Maître Claire Dubois (Paris)");
        }

        // 3. Maître Cheick Oumar Traoré (Dakar / Bamako)
        if (!avocatRepository.existsById("avocat-traore")) {
            Avocat traore = new Avocat();
            traore.setId("avocat-traore");
            traore.setNom("Traoré");
            traore.setPrenom("Cheick Oumar");
            traore.setEmail("cheick.traore@psyavocat.com");
            traore.setTelephone("+221 77 654 32 10");
            traore.setVille("Dakar");
            traore.setAdresse("Immeuble Fahd, Boulevard Djily Mbaye, Plateau");
            traore.setNumeroBarreau("SN-DKR-2010-156");
            traore.setBiographie("Avocat pénaliste chevronné, intervenant en urgence dans les gardes à vue, les infractions économiques et la protection des droits fondamentaux dans l'espace UEMOA.");
            traore.setModeConsultation("HYBRIDE");
            traore.setStatutValidation("VALIDE");
            traore.setPhotoUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80");
            traore.setNoteMoyenne(4.9);
            traore.setNombreAvis(62);
            traore.setEnLigne(true);
            traore.setLangues("Français, Wolof, Bambara");
            traore.setDateInscription(LocalDate.now().minusYears(4));
            traore.setSpecialites(findSpecialitesByNoms(List.of(
                    "Droit pénal & défense criminelle",
                    "Droit immobilier & contentieux foncier"
            )));
            avocatRepository.save(traore);

            seedTarif(traore, "Consultation urgence pénale", new BigDecimal("50000"), "XOF", "Assistance immédiate et préparation de stratégie de défense en urgence.", 45);
            seedTarif(traore, "Consultation contentieux foncier", new BigDecimal("35000"), "XOF", "Sécurisation de transactions et défense devant les juridictions compétentes.", 45);
            log.info("    + Avocat créé : Maître Cheick Oumar Traoré (Dakar)");
        }
    }

    private void seedPsychologues() {
        // 4. Dr. Sophie Lambert (Lyon, France)
        if (!psychologueRepository.existsById("psy-lambert")) {
            Psychologue lambert = new Psychologue();
            lambert.setId("psy-lambert");
            lambert.setNom("Lambert");
            lambert.setPrenom("Sophie");
            lambert.setEmail("sophie.lambert@psyavocat.com");
            lambert.setTelephone("+33 4 72 00 00 00");
            lambert.setVille("Lyon");
            lambert.setAdresse("5 Place Bellecour, 69002 Lyon");
            lambert.setNumeroAgrement("ADELI-699314852");
            lambert.setBiographie("Psychologue clinicienne et psychothérapeute spécialisée dans l'accompagnement du burn-out, du stress chronique et des transitions de vie. Thérapies brèves orientées solutions.");
            lambert.setModeConsultation("HYBRIDE");
            lambert.setStatutValidation("VALIDE");
            lambert.setPhotoUrl("https://images.unsplash.com/photo-1594824813583-e18e8d5b1285?auto=format&fit=crop&w=400&q=80");
            lambert.setNoteMoyenne(4.9);
            lambert.setNombreAvis(56);
            lambert.setEnLigne(true);
            lambert.setLangues("Français, Anglais");
            lambert.setDateInscription(LocalDate.now().minusYears(2));
            lambert.setSpecialites(findSpecialitesByNoms(List.of(
                    "Gestion du stress, anxiété & burn-out",
                    "Psychothérapie clinique & troubles de l'humeur"
            )));
            psychologueRepository.save(lambert);

            seedTarif(lambert, "Séance de psychothérapie individuelle", new BigDecimal("25000"), "XOF", "Espace d'écoute bienveillant et confidentiel pour surmonter l'anxiété.", 45);
            seedTarif(lambert, "Bilan émotionnel & burn-out", new BigDecimal("35000"), "XOF", "Évaluation complète de l'épuisement et élaboration d'un plan de résilience.", 60);
            log.info("    + Psychologue créée : Dr. Sophie Lambert (Lyon)");
        }

        // 5. Dr. Aminata Touré (Abidjan, Côte d'Ivoire)
        if (!psychologueRepository.existsById("psy-toure")) {
            Psychologue toure = new Psychologue();
            toure.setId("psy-toure");
            toure.setNom("Touré");
            toure.setPrenom("Aminata");
            toure.setEmail("aminata.toure@psyavocat.com");
            toure.setTelephone("+225 07 89 01 23");
            toure.setVille("Abidjan");
            toure.setAdresse("Boulevard Latrille, Deux Plateaux Vallons, Cocody");
            toure.setNumeroAgrement("CI-ABJ-2016-033");
            toure.setBiographie("Psychologue clinicienne et thérapeute conjugale. J'accompagne les couples et familles en crise ainsi que les personnes confrontées à un deuil ou une séparation douloureuse.");
            toure.setModeConsultation("HYBRIDE");
            toure.setStatutValidation("VALIDE");
            toure.setPhotoUrl("https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&w=400&q=80");
            toure.setNoteMoyenne(4.8);
            toure.setNombreAvis(41);
            toure.setEnLigne(true);
            toure.setLangues("Français, Baoulé, Bambara");
            toure.setDateInscription(LocalDate.now().minusYears(3));
            toure.setSpecialites(findSpecialitesByNoms(List.of(
                    "Thérapie de couple & médiation familiale",
                    "Psychotraumatologie & gestion du deuil"
            )));
            psychologueRepository.save(toure);

            seedTarif(toure, "Consultation d'écoute & soutien", new BigDecimal("20000"), "XOF", "Séance individuelle pour faire le point sur vos émotions et blocages.", 45);
            seedTarif(toure, "Thérapie de couple & médiation", new BigDecimal("40000"), "XOF", "Séance conjointe pour rétablir le dialogue et dépasser les crises relationnelles.", 75);
            log.info("    + Psychologue créée : Dr. Aminata Touré (Abidjan)");
        }
    }

    private void seedTarif(Professionnel pro, String titre, BigDecimal montant, String devise, String desc, int duree) {
        if (!tarifProfessionnelRepository.existsByProfessionnelIdAndTitre(pro.getId(), titre)) {
            TarifProfessionnel t = new TarifProfessionnel();
            t.setTitre(titre);
            t.setMontant(montant);
            t.setDevise(devise);
            t.setDescription(desc);
            t.setDureeMinutes(duree);
            t.setActif(true);
            t.setProfessionnel(pro);
            tarifProfessionnelRepository.save(t);
        }
    }

    private List<Specialite> findSpecialitesByNoms(List<String> noms) {
        List<Specialite> list = new ArrayList<>();
        for (String nom : noms) {
            specialiteRepository.findByNomIgnoreCase(nom).ifPresent(list::add);
        }
        return list;
    }
}
